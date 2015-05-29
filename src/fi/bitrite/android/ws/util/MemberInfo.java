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
    File mProfilePhotoFile;
    private static final String TAG = "MemberInfo";

    private static final String mWSAndroidDirName = Environment
            .getExternalStorageDirectory()
            .getAbsolutePath() + "/wsandroid/";
    private static File mWSAndroidDir;

    private MemberInfo(Host host) {
        if (host == null) {
            host = retrieveMemberInfo();
            if (host == null) {
                return;
            }
        }
        mHost = host;
        persistMemberInfo(host);

        // Get the member photo if it doesn't exist already
        mWSAndroidDir = new File(mWSAndroidDirName);
        if (!mWSAndroidDir.exists()) {
            mWSAndroidDir.mkdirs();
        }
        String profilePhotoPath = mWSAndroidDirName + "memberphoto";
        File profileImageFile = new File(profilePhotoPath);
        // If the file doesn't exist or is tiny, download it, otherwise use the one we have
        if (!profileImageFile.exists() || profileImageFile.length() < 1000) {
            // Download it
            new DownloadProfilePhotoTask(mHost.getProfilePictureSmall(), profilePhotoPath).execute();
        } else {
            mProfilePhotoFile = profileImageFile;
        }
    }

    private void persistMemberInfo(Host host) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WSAndroidApplication.getAppContext());
        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(host);
        prefsEditor.putString("member_info", json);
        prefsEditor.commit();
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WSAndroidApplication.getAppContext());
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.remove("member_info");
        prefsEditor.commit();
    }
    public static String getMemberPhotoFilePath() {
        if (instance != null && instance.mProfilePhotoFile != null && instance.mProfilePhotoFile.length() > 1000) {
            return instance.mProfilePhotoFile.getPath();
        }
        return null;
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
        if (instance != null && instance.mProfilePhotoFile != null) {
            boolean result = instance.mProfilePhotoFile.delete();
            instance.deleteMemberInfo();
            instance = null;
        }
    }

    private class DownloadProfilePhotoTask extends AsyncTask<Void, Void, File> {
        String mTargetFilePath;
        InputStream is;
        String mUrl;

        public DownloadProfilePhotoTask(String url, String targetFilePath) {
            mUrl = url;
            mTargetFilePath = targetFilePath;
        }

        @Override
        protected File doInBackground(Void... params)  {
            File targetFile = new File(mTargetFilePath);
            if (targetFile.exists()) {
                targetFile.delete();
            }
            HttpURLConnection c = null;
            FileOutputStream fos = null;
            BufferedOutputStream out = null;
            try {
                c = (HttpURLConnection) new URL(mUrl).openConnection();
                fos = new FileOutputStream(mTargetFilePath);
                out = new BufferedOutputStream(fos);

                InputStream in = c.getInputStream();
                byte[] buffer = new byte[16384];
                int len = 0;
                while (( len = in.read( buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();

            } catch (Exception e) {
                Log.i(TAG, "Error: " + e);
            }
            finally {
                try {
                    fos.getFD().sync();
                    out.close();
                    c.disconnect();
                } catch (Exception e) {
                    // Don't worry about these?
                }
            }
            return targetFile;
        }

        protected void onPostExecute(File profilePhoto) {
            if (profilePhoto != null && profilePhoto.length() > 1000) {
                mProfilePhotoFile = profilePhoto;
            }
        }

    }

}
