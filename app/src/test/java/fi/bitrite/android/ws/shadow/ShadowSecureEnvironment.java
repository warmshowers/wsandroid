package fi.bitrite.android.ws.shadow;

import android.content.Context;

import com.u.securekeys.SecureEnvironment;

import org.robolectric.annotation.Implements;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

@Implements(SecureEnvironment.class)
public class ShadowSecureEnvironment {
    private final static Map<String, String> mMockSecureValues = new HashMap<>();

    static {
        setSecureKey("ws_api_userId", "1234");
        setSecureKey("ws_api_key", "mock_api_key");
        setSecureKey("ws_base_url", "https://localhost/");
    }

    private ShadowSecureEnvironment() throws IllegalAccessException {
        throw new IllegalAccessException("This object can't be instantiated");
    }

    public static void initialize(@NonNull Context context) {
    }

    public static void setSecureKey(@NonNull String key, @NonNull String value) {
        mMockSecureValues.put(key, value);
    }

    public static @NonNull String getString(@NonNull String key) {
        if (key.isEmpty()) {
            return "";
        }
        if (!mMockSecureValues.containsKey(key)) {
            throw new RuntimeException("Unknown key in ShadowSecureEnvironment");
            // The ShadowSecureEnvironment must know all the secrets.
        }
        String val = mMockSecureValues.get(key);
        return val == null ? "" : val;
    }

    public static long getLong(@NonNull String key) {
        String value = getString(key);
        if (value.isEmpty()) {
            return 0;
        }
        return Long.valueOf(value);
    }

    public static double getDouble(@NonNull String key) {
        String value = getString(key);
        if (value.isEmpty()) {
            return 0;
        }
        return Double.valueOf(value);
    }
}
