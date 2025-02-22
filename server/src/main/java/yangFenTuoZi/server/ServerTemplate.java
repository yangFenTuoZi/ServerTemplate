package yangFenTuoZi.server;

import android.annotation.TargetApi;
import android.app.IActivityManager;
import android.content.AttributionSource;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.IPackageManager;
import android.ddm.DdmHandleAppName;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.system.Os;

import androidx.annotation.NonNull;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 服务模板类，用于在Android上通过adb/shell..创建一个自己的“服务”
 * 这是一个抽象类，子类需要实现抽象方法以提供具体的配置和功能
 *
 * @author yangFenTuoZi
 * @version 1.0
 */
public abstract class ServerTemplate extends ContextWrapper {
    /**
     * 记录器，防止二次崩溃
     */
    private boolean isCrashed = false;

    /**
     * 应用包管理器
     */
    public IPackageManager mPackageManager;
    /**
     * 活动管理器
     */
    public IActivityManager mActivityManager;
    /**
     * 日志记录器
     */
    private final Logger mLogger;
    /**
     * 主线程的Handler，用于在主线程执行任务
     */
    private final Handler mHandler;
    /**
     * 主线程实例， 用于判断当前是否在主线程
     */
    private final Thread mainThread;

    /**
     * 服务参数
     */
    private final Args mArgs;

    public static class Args {
        public final String serverName;
        public final File logDir;
        public final int[] uids;
        public final boolean enableLogger;

        private Args(Builder builder) {
            serverName = builder.serverName;
            logDir = builder.logDir;
            uids = builder.uids;
            enableLogger = builder.enableLogger;
        }

        public static class Builder {
            public String serverName;
            public File logDir;
            public int[] uids = new int[0];
            public boolean enableLogger = false;

            public Builder() {
            }

            public Args build() {
                return new Args(this);
            }
        }
    }

    /**
     * 构造函数，初始化服务
     * 包括设置主线程、权限检查、日志记录器初始化、异常处理等
     */
    public ServerTemplate(Args args) {
        super(getSystemContext());

        mArgs = args;
        // 切换到主线程
        if (Looper.getMainLooper() == null)
            Looper.prepareMainLooper();

        // 判断uid
        UID = Os.getuid();
        if (Arrays.binarySearch(mArgs.uids, UID) == -1) {
            System.err.printf("Insufficient permission! Need to be launched by %s, but your uid is %d.\n", Arrays.toString(mArgs.uids), UID);
            System.exit(255);
        }

        // onCreate
        onCreate();

        // 设置程序名称
        DdmHandleAppName.setAppName(mArgs.serverName, 0);

        // 如果启用Logger那么就设置为正常的Logger，否则就设置为空模板Logger
        mLogger = mArgs.enableLogger ? new Logger(mArgs.serverName, mArgs.logDir) : new Logger();

        // jvm退出/异常处理
        Runtime.getRuntime().addShutdownHook(new Thread(this::onStop));
        Thread.setDefaultUncaughtExceptionHandler(this::onCrash);

        mPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        mActivityManager = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"));

        // 初始化FakeContext
        initFakeContext();

        // 创建一个Handler，为runOnMainThread奠定基础
        mHandler = new Handler();
        mainThread = Thread.currentThread();

        // onStart
        new Thread(this::onStart).start();

        // 主线程进入等待
        Looper.loop();
    }

    /**
     * 初始化模拟上下文（FakeContext）
     */
    private void initFakeContext() {

        // 获取当前uid对应的包名
        String packageName;
        if (UID == 0) packageName = "root";
        else {
            try {
                packageName = mPackageManager.getPackagesForUid(UID)[0];
            } catch (RemoteException e) {
                mLogger.e("cannot get the package name corresponding to this UID: %d", UID);
                return;
            }
        }
        if (packageName == null || packageName.isEmpty()) {
            mLogger.e("got an empty package name");
            return;
        }
        mLogger.i("init FakeContext { UID = %d, PACKAGE_NAME = \"%s\"}", UID, packageName);

        // 设置FakeContext uid和包名
        PACKAGE_NAME = packageName;
    }

    /**
     * 服务创建时的回调方法
     * 可以在这写自己的启动条件判断逻辑<br/><br/>
     * 这个函数会在主线程执行，堆放太多代码可能导致阻塞<br/>
     * <b>注意：</b>此时的环境没有初始化FakeContext、Logger、IPackageManager、IActivityManager...
     */
    public void onCreate() {
        // 服务创建
    }

    /**
     * 服务启动时的回调方法
     * 子类可以重写此方法以实现自定义的启动逻辑<br/><br/>
     * 这个函数会在子线程执行，若要切换到主线程请使用<code>runOnMainThread(Runnable)</code>函数<br/>
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
    public void onCrash(Thread t, Throwable e) {
        if (isCrashed) System.exit(255);
        isCrashed = true;
        new Thread(() -> {
            if (mLogger != null)
                mLogger.e("""
                        ** Program Crashed ! **
                        at Thread-%s
                        UID: %d, PID: %d
                        
                        %s
                        """, t.getName(), Os.getuid(), Os.getpid(), Logger.getStackTraceString(e));
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
     * 获取日志记录器实例
     *
     * @return <code>Logger</code>实例
     */
    public Logger getLogger() {
        return mLogger;
    }

    // Workarounds

    private static final Class<?> ACTIVITY_THREAD_CLASS;
    private static final Object ACTIVITY_THREAD;

    static {
        if (Looper.getMainLooper() == null)
            Looper.prepareMainLooper();

        try {
            ACTIVITY_THREAD_CLASS = Class.forName("android.app.ActivityThread");
            Constructor<?> activityThreadConstructor = ACTIVITY_THREAD_CLASS.getDeclaredConstructor();
            activityThreadConstructor.setAccessible(true);
            ACTIVITY_THREAD = activityThreadConstructor.newInstance();

            Field sCurrentActivityThreadField = ACTIVITY_THREAD_CLASS.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            sCurrentActivityThreadField.set(null, ACTIVITY_THREAD);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    static Context getSystemContext() {
        try {
            Method getSystemContextMethod = ACTIVITY_THREAD_CLASS.getDeclaredMethod("getSystemContext");
            return (Context) getSystemContextMethod.invoke(ACTIVITY_THREAD);
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
            return null;
        }
    }
    // FakeContext

    private String PACKAGE_NAME;
    private final int UID;

    @Override
    public String getPackageName() {
        return PACKAGE_NAME;
    }

    @Override
    @NonNull
    public String getOpPackageName() {
        return PACKAGE_NAME;
    }

    @TargetApi(Build.VERSION_CODES.S)
    @Override
    @NonNull
    public AttributionSource getAttributionSource() {
        AttributionSource.Builder builder = new AttributionSource.Builder(UID);
        builder.setPackageName(PACKAGE_NAME);
        return builder.build();
    }

    // @Override to be added on SDK upgrade for Android 14
    @SuppressWarnings("unused")
    public int getDeviceId() {
        return 0;
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }
}