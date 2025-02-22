package yangFenTuoZi.demo;

import static yangFenTuoZi.demo.Server.TAG;

import android.annotation.SuppressLint;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;

import yangFenTuoZi.demo.databinding.ActivityMainBinding;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {
    public ActivityMainBinding binding;
    public App mApp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApp = (App) getApplication();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.refreshStatus.setOnClickListener(v -> new Thread(() -> {
            try {
                LocalSocket socket = new LocalSocket();
                socket.connect(new LocalSocketAddress(TAG));
                OutputStream out = socket.getOutputStream();
                out.write("binder".getBytes());
                out.flush();
                out.close();
                socket.close();
            } catch (Throwable e) {
                Log.e("MainActivity", "refreshStatus.OnClickListener", e);
                runOnUiThread(() -> binding.status.setText("stopped"));
            }
        }).start());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mApp.main = this;
//        if (mApp.iService == null) binding.status.setText("stopped");
//        else
        try {
            binding.status.setText(mApp.iService.isRunning().equals("running") ? " running" : "stopped");
        } catch (Throwable e) {
            binding.status.setText("stopped");
        }
    }
}
