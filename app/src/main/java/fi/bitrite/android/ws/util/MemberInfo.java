package fi.bitrite.android.ws.util;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.model.Host;

/**
 * Singleton pattern for our MemberInfo
 */
public class MemberInfo {
    private static MemberInfo instance;
    private Host mHost;
    private static final String TAG = "MemberInfo";

    public static final String mWSAndroidDirName = Environment
            .getExternalStorageDirectory()
            .getAbsolutePath() + "/wsandroid/";
    public static final String mProfilePhotoPath = mWSAndroidDirName + "memberphoto";

    private MemberInfo(Host host) {
        if (host == null) {
            host = retrieveMemberInfo();
            if (host == null) {
                return;
            }
        }
        mHost = host;
        persistMemberInfo(host);

        // Set up directory for photos if it doesn't exist
        File androidDir = new File(mWSAndroidDirName);
        if (!androidDir.exists()) {
            androidDir.mkdirs();
        }
    }

    private void persistMemberInfo(Host host) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WSAndroidApplication.getAppContext());
        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(host);
        prefsEditor.putString("member_info", json);
        prefsEditor.apply();
    }

    private Host retrieveMemberInfo() {
        Host host = null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WSAndroidApplication.getAppContext());
        Gson gson = new Gson();
        String json = prefs.getString("member_info", "");
        try {
            host = gson.fromJson(json, Host.class);
        } catch (JsonSyntaxException e) {
            // We failed, will return null
        }
        return host;
    }
    private void deleteMemberInfo() {
        File photo = new File(mProfilePhotoPath);
        boolean result = photo.delete();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WSAndroidApplication.getAppContext());
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.remove("member_info");
        prefsEditor.apply();
    }
    public static String getMemberPhotoFilePath() {
        return instance != null ? instance.mProfilePhotoPath : null;
    }

    public static void initInstance(Host host)
    {
        instance = new MemberInfo(host);
    }

    // We probably don't need this - just get of the host details
    public static MemberInfo getInstance() {
        // Check to see if initialized?
        return instance;
    }

    public static Host getMemberInfo() {
        if (instance != null) {
            return getInstance().mHost;
        }
        return null;
    }

    public static void doLogout() {
        if (instance != null) {
            instance.deleteMemberInfo();
            instance = null;
        }
    }

}
