package yangFenTuoZi.demo;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.IBinder;

public class App extends Application {
    public IService iService;
    public MainActivity main;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @SuppressLint("SetTextI18n")
    public void onReceive(IBinder binder) {
        if (binder == null) {
            iService = null;
            main.runOnUiThread(() -> main.binding.status.setText("stopped"));
        } else {
            iService = IService.Stub.asInterface(binder);
            main.runOnUiThread(() -> main.binding.status.setText("running"));
        }
    }
}
