package yangFenTuoZi.server.fakecontext;

import android.annotation.TargetApi;
import android.content.AttributionSource;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;

import androidx.annotation.NonNull;

public class FakeContext extends ContextWrapper {
    public String PACKAGE_NAME;
    public int UID;

    public FakeContext(int uid, String packageName) {
        super(Workarounds.getSystemContext());
        PACKAGE_NAME = packageName;
        UID = uid;
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
