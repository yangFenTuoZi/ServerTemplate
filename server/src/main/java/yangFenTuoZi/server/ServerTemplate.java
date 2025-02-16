package yangFenTuoZi.server;

import android.app.IActivityManager;
import android.content.pm.IPackageManager;
import android.ddm.DdmHandleAppName;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.system.Os;

import java.io.File;
import java.util.Arrays;

import yangFenTuoZi.server.fakecontext.FakeContext;

/**
 * 服务模板类，用于在Android上通过adb/shell..创建一个自己的“服务”
 * 这是一个抽象类，子类需要实现抽象方法以提供具体的配置和功能
 *
 * @author yangFenTuoZi
 * @version 1.0
 */
public abstract class ServerTemplate {
    /** 应用包管理器 */
    public IPackageManager mPackageManager;
    /** 活动管理器 */
    public IActivityManager mActivityManager;
    /** FakeContext */
    private FakeContext mContext;
    /** 日志记录器 */
    private final Logger mLogger;
    /** 主线程的Handler，用于在主线程执行任务 */
    private final Handler mHandler;
    /** 主线程实例， 用于判断当前是否在主线程 */
    private final Thread mainThread;

    /**
     * 构造函数，初始化服务
     * 包括设置主线程、权限检查、日志记录器初始化、异常处理等
     */
    public ServerTemplate() {
        // 切换到主线程
        if (Looper.getMainLooper() == null)
            Looper.prepareMainLooper();

        // 判断uid
        int uid = Os.getuid();
        if (Arrays.binarySearch(getUids(), uid) == -1) {
            System.err.printf("Insufficient permission! Need to be launched by %s, but your uid is %d.\n", Arrays.toString(getUids()), uid);
            System.exit(255);
        }
        // 设置程序名称
        DdmHandleAppName.setAppName(getServerName(), uid);
        // 如果启用Logger那么就设置为正常的Logger，否则就设置为空模板Logger
        mLogger = enableLogger() ? new Logger(getServerName(), getLogDir()) : new Logger();
        // jvm退出/异常处理
        Runtime.getRuntime().addShutdownHook(new Thread(this::onStop));
        Thread.setDefaultUncaughtExceptionHandler(this::onCrash);

        mPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        mActivityManager = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"));
        // 看情况创建FakeContext
        if (enableFakeContext()) createFakeContext(uid);
        // 创建一个Handler，为runOnMainThread奠定基础
        mHandler = new Handler();
        mainThread = Thread.currentThread();
        // onStart
        new Thread(this::onStart).start();
        // 主线程进入等待
        Looper.loop();
    }

    /**
     * 创建模拟上下文（FakeContext）
     *
     * @param uid 当前用户的UID
     */
    private void createFakeContext(int uid) {
        // 准备创建FakeContext
        mLogger.i("prepare to create FakeContext");
        // 获取当前uid对应的包名
        String packageName;
        if (uid == 0) packageName = "root";
        else {
            try {
                packageName = mPackageManager.getPackagesForUid(uid)[0];
            } catch (RemoteException e) {
                mLogger.e("cannot get the package name corresponding to this UID: %d", uid);
                return;
            }
        }
        if (packageName == null || packageName.isEmpty()) {
            mLogger.e("got an empty package name");
            return;
        }
        mLogger.i("create FakeContext { UID = %d, packageName = \"%s\"}", uid, packageName);
        // 创建FakeContext
        mContext = new FakeContext(uid, packageName);
    }

    /**
     * 服务启动时的回调方法
     * 子类可以重写此方法以实现自定义的启动逻辑<br/><br/>
     * 可以在这写监听app状态，等待发送Binder给app<br/>
     * 或者写socket服务与app通信
     */
    public void onStart() {
        // 服务启动
        mLogger.i("onStart");
    }

    /**
     * 服务关闭时的回调方法
     * 子类可以重写此方法以实现自定义的关闭逻辑
     */
    public void onStop() {
        // 服务关闭
        mLogger.i("onStop");
        mLogger.close();
    }

    /**
     * 崩溃处理回调方法
     *
     * @param t 崩溃发生的线程
     * @param e 崩溃的异常信息
     */
    private void onCrash(Thread t, Throwable e) {
        new Thread(() -> {
            if (mLogger != null)
                mLogger.e("""
                    Crashed !!!
                    currentUID: %d
                    ThreadID: %d
                    ThreadName: %s
                    Exception StackTrace: %s
                    """, Os.getuid(), t.getId(), t.getName(), Logger.getStackTraceString(e));
            exit(255);
        }).start();
    }

    /**
     * 手动退出程序
     *
     * @param status 退出状态码
     */
    public void exit(int status) {
        onStop();
        System.exit(status);
    }

    /**
     * 在主线程执行任务
     *
     * @param action 需要执行的任务
     */
    public void runOnMainThread(Runnable action) {
        if (Thread.currentThread() == mainThread) action.run();
        else mHandler.post(action);
    }

    /**
     * 获取虚假上下文（FakeContext）实例
     *
     * @return FakeContext实例
     */
    public FakeContext getContext() {
        return mContext;
    }

    /**
     * 获取日志记录器实例
     *
     * @return Logger实例
     */
    public Logger getLogger() {
        return mLogger;
    }

    /**
     * 获取允许运行的UID列表
     *
     * @return 允许运行的UID数组
     */
    abstract int[] getUids();

    /**
     * 获取服务名称
     *
     * @return 服务名称
     */
    abstract String getServerName();

    /**
     * 是否启用模拟上下文（FakeContext）
     *
     * @return 如果启用返回true，否则返回false
     */
    abstract boolean enableFakeContext();

    /**
     * 是否启用日志记录器
     *
     * @return 如果启用返回true，否则返回false
     */
    abstract boolean enableLogger();

    /**
     * 获取日志保存路径
     *
     * @return 日志保存路径的文件对象
     */
    abstract File getLogDir();
}