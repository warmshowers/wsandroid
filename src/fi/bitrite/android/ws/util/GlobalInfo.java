package fi.bitrite.android.ws.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import fi.bitrite.android.ws.model.Host;

/**
 * Provide basic info that may not be accessible any other way.
 */
public class GlobalInfo {

    public static String warmshowersCookieDomain = ".warmshowers.org";
    public static String warmshowersBaseUrl = "https://www.warmshowers.org";

    public static void setWarmshowersBaseUrl(String url) {
        warmshowersBaseUrl = url;
    }
    public static void setWarmshowersCookieDomain(String cookieDomain) {
        warmshowersCookieDomain = cookieDomain;
    }
}
