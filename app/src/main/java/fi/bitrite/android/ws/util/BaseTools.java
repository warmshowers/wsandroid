package fi.bitrite.android.ws.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import fi.bitrite.android.ws.BuildConfig;
import fi.bitrite.android.ws.repository.SettingsRepository;

/**
 * General simple tools, mostly public methods.
 */
public class BaseTools {

    /**
     * Return distance between two points in km/miles
     *
     * @param l1 First location
     * @param l2 Second location
     * @param distanceUnit (mi or km)
     * @return Calculated distance between the two specified points of location
     */
    static public int calculateDistanceBetween(Location l1, Location l2,
                                               SettingsRepository.DistanceUnit distanceUnit) {
        double factor = distanceUnit == SettingsRepository.DistanceUnit.MILES ? 1609.34 : 1000;
        float meters = l1.distanceTo(l2);
        return (int) (meters / factor);
    }
    static public int calculateDistanceBetween(IGeoPoint l1, IGeoPoint l2,
                                               SettingsRepository.DistanceUnit distanceUnit) {
        return calculateDistanceBetween(latLngToLocation(l1), latLngToLocation(l2), distanceUnit);
    }

    static public GeoPoint locationToLatLng(Location location) {
        if (location == null) {
            return null;
        }
        return new GeoPoint(location.getLatitude(), location.getLongitude());
    }
    static public Location latLngToLocation(IGeoPoint latLng) {
        if (latLng == null) {
            return null;
        }
        Location location = new Location("fromlatlng");
        location.setLatitude(latLng.getLatitude());
        location.setLongitude(latLng.getLongitude());
        return location;
    }

    // TODO(saemy): Make reactive.
    static public boolean isNetworkConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null
                              && activeNetwork.isConnectedOrConnecting();

        if (BuildConfig.DEBUG) {
            // FIXME(saemy): Get from SettingsRepository
            boolean simulateDisconnected = PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("developer_no_network", false);
            if (simulateDisconnected) {
                return false;
            }
        }

        return isConnected;
    }

    /*
     * Scale an image to its view
     * From https://argillander.wordpress.com/2011/11/24/scale-image-into-imageview-then-resize-imageview-to-match-the-image/
     */
    static public void scaleImage(ImageView view, int boundBoxInDp) {
        // Get the ImageView and its bitmap
        Drawable drawing = view.getDrawable();
        Bitmap bitmap = ((BitmapDrawable) drawing).getBitmap();

        // Get current dimensions
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Determine how much to scale: the dimension requiring less scaling is
        // closer to the its side. This way the image always stays inside your
        // bounding box AND either x/y axis touches it.
        float xScale = ((float) boundBoxInDp) / width;
        float yScale = ((float) boundBoxInDp) / height;
        float scale = (xScale <= yScale) ? xScale : yScale;

        // Create a matrix for the scaling and add the scaling data
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        // Create a new bitmap and convert it to a format understood by the ImageView
        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        BitmapDrawable result = new BitmapDrawable(scaledBitmap);
        width = scaledBitmap.getWidth();
        height = scaledBitmap.getHeight();

        // Apply the scaled bitmap
        view.setImageDrawable(result);

        // Now change ImageView's dimensions to match the scaled image
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setLayoutParams(params);
    }

    /*
     * Get locale-sensitive date string in format Month Year
     */
    public static String getDateAsMY(Context context, long timeInMillis) {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
                    | DateUtils.FORMAT_NO_MONTH_DAY;
        return DateUtils.formatDateTime(context, timeInMillis, flags);
    }

    /**
     * Gets the specific locale for the current device configuration.
     * Defaults to default Locale ({@link Locale#getDefault}) if no context is specified.
     *
     * @param context Context used for retrieving the locale
     * @return The current Locale of the device
     */
    @NonNull
    public static Locale getLocale(@Nullable Context context) {
        if (context == null) {
            return Locale.getDefault();
        }

        final Resources resources = context.getResources();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return resources.getConfiguration().getLocales().get(0);
        } else {
            //noinspection deprecation
            return resources.getConfiguration().locale;
        }
    }

}
