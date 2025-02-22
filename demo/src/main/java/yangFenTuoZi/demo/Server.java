package yangFenTuoZi.demo;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import java.io.File;

import yangFenTuoZi.server.Logger;
import yangFenTuoZi.server.ServerTemplate;

public class Server extends ServerTemplate {
    public static final String TAG = "demo_server";
    public static final String ACTION_SERVER_RUNNING = "server_template.demo.intent.action.SERVER_RUNNING";
    public static final String ACTION_SERVER_STOPPED = "server_template.demo.intent.action.SERVER_STOPPED";

    private Logger mLogger;
    private boolean isStop = false;

    @SuppressLint("SdCardPath")
    public static void main(String[] __) {
        Args.Builder builder = new Args.Builder();
        builder.serverName = TAG;
        builder.enableLogger = true;
        builder.logDir = new File("/sdcard/logs");
        builder.uids = new int[]{0};
        new Server(builder.build());
    }

    public Server(Args args) {
        super(args);
    }

    public void onStart() {
        super.onCreate();
        mLogger = getLogger();
//        new Thread(() -> {
//            try {
//                mLogger.d("create LocalServerSocket");
//                LocalServerSocket server = new LocalServerSocket(TAG);
//
//                while (!isStop) {
//                    LocalSocket socket = server.accept();
//                    mLogger.d("accepted LocalSocket");
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                    String line;
//                    while ((line = reader.readLine()) != null) {
//                        if (line.equals("binder")) {
//                            Bundle data = new Bundle();
//                            data.putBinder("binder", getBinder());
//
//                            sendBroadcast(new Intent(Server.ACTION_SERVER_RUNNING)
//                                    .setPackage(BuildConfig.APPLICATION_ID)
//                                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
//                                    .putExtra("data", data));
//                        }
//                    }
//                    reader.close();
//                    socket.close();
//                    mLogger.d("close LocalSocket");
//                }
//            } catch (Throwable e) {
//                mLogger.e(Logger.getStackTraceString(e));
//            }
//        }).start();

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        }, new IntentFilter());
    }

    private IBinder getBinder() {

        return new IService.Stub() {
            @Override
            public String isRunning() {
                return "running";
            }

            @Override
            public void stop() {
                isStop = true;
                exit(0);
            }
        };
    }

    @Override
    public void onStop() {
        super.onStop();
        sendBroadcast(new Intent(Server.ACTION_SERVER_STOPPED)
                .setPackage(BuildConfig.APPLICATION_ID)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));
    }
}
