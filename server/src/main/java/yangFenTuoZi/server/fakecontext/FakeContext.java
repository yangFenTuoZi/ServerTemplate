package yangFenTuoZi.server.fakecontext;

import android.annotation.TargetApi;
import android.content.AttributionSource;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;

import androidx.annotation.NonNull;

public class FakeContext extends ContextWrapper {
    private String PACKAGE_NAME;
    private int UID;

    public FakeContext() {
        super(Workarounds.getSystemContext());
    }

    public void setPackageName(String PACKAGE_NAME) {
        this.PACKAGE_NAME = PACKAGE_NAME;
    }

    public void setUid(int UID) {
        this.UID = UID;
    }

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
