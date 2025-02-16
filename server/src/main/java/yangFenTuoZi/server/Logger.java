package yangFenTuoZi.server;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 自定义日志记录器类，支持多级别日志记录和每日日志文件分割
 */
public class Logger {
    // 日志级别常量
    private static final String VERBOSE = "V";
    private static final String DEBUG = "D";
    private static final String INFO = "I";
    private static final String WARN = "W";
    private static final String ERROR = "E";

    private final boolean disable; // 是否禁用日志功能
    private String TAG;            // 日志标签
    private LocalDate lastLogDate; // 最后记录日志的日期
    private FileWriter fileWriter; // 文件写入器
    private File logDir;           // 日志文件目录

    /**
     * 构造启用日志功能的记录器
     *
     * @param TAG    日志标签
     * @param logDir 日志文件存储目录
     */
    public Logger(String TAG, File logDir) {
        disable = false;
        lastLogDate = LocalDate.now();
        try {
            // 检查并创建日志目录
            if (logDir.isFile()) return;
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            this.TAG = TAG;
            this.logDir = logDir;
            // 创建当日日志文件
            File logFile = new File(logDir + "/" + lastLogDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".log");
            changeLogFile(logFile);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 构造禁用日志功能的记录器
     */
    public Logger() {
        disable = true;
    }

    // 不同日志级别的基础记录方法
    public void v(String message) {
        writeLog(VERBOSE, message);
    }

    public void d(String message) {
        writeLog(DEBUG, message);
    }

    public void i(String message) {
        writeLog(INFO, message);
    }

    public void w(String message) {
        writeLog(WARN, message);
    }

    public void e(String message) {
        writeLog(ERROR, message);
    }

    // 支持格式化字符串的日志记录方法
    public void v(String message, Object... args) {
        v(String.format(message, args));
    }

    public void d(String message, Object... args) {
        d(String.format(message, args));
    }

    public void i(String message, Object... args) {
        i(String.format(message, args));
    }

    public void w(String message, Object... args) {
        w(String.format(message, args));
    }

    public void e(String message, Object... args) {
        e(String.format(message, args));
    }

    /**
     * 核心日志写入方法
     *
     * @param priority 日志级别
     * @param message  日志信息
     */
    private void writeLog(String priority, String message) {
        if (disable) return;

        try {
            // 输出到Android系统日志
            switch (priority) {
                case VERBOSE -> Log.v(TAG, message);
                case DEBUG -> Log.d(TAG, message);
                case INFO -> Log.i(TAG, message);
                case WARN -> Log.w(TAG, message);
                case ERROR -> Log.e(TAG, message);
            }

            LocalDate date = LocalDate.now();
            // 检查日期变化，自动切换日志文件
            if (!Objects.equals(lastLogDate.format(DateTimeFormatter.ofPattern("dd")),
                    date.format(DateTimeFormatter.ofPattern("dd")))) {
                changeLogFile(new File(logDir, date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".log"));
            }

            if (isOpen()) {
                // 构造日志格式：[时间] [标签] [级别] 信息
                String log = "[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] ["
                        + TAG + "] [" + priority + "] " + message;
                fileWriter.write(log + "\n");
                fileWriter.flush();
                System.out.println(log);  // 同时输出到控制台
                lastLogDate = date;
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 直接写入原始信息（不包含元数据）
     *
     * @param message 要写入的原始信息
     */
    public void print(String message) {
        if (disable) return;

        try {
            LocalDate date = LocalDate.now();
            if (!Objects.equals(lastLogDate.format(DateTimeFormatter.ofPattern("dd")),
                    date.format(DateTimeFormatter.ofPattern("dd")))) {
                changeLogFile(new File(logDir, date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".log"));
            }
            if (isOpen()) {
                fileWriter.write(message);
                fileWriter.flush();
                System.out.printf(message);  // 输出到控制台
                lastLogDate = date;
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 关闭文件写入器
     */
    public void close() {
        if (disable) return;

        try {
            if (fileWriter != null) {
                fileWriter.close();
                fileWriter = null;
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 检查文件写入器是否打开
     */
    public boolean isOpen() {
        return fileWriter != null;
    }

    /**
     * 切换日志文件
     *
     * @param file 新的日志文件对象
     */
    private void changeLogFile(File file) throws IOException {
        if (disable) return;

        if (fileWriter != null) close();
        if (!file.exists())
            file.createNewFile();
        fileWriter = new FileWriter(file, true); // 追加模式打开文件
    }

    /**
     * 获取异常堆栈字符串
     *
     * @param tr 异常对象
     * @return 格式化后的堆栈跟踪字符串
     */
    public static String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }
}
