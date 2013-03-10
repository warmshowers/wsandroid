package fi.bitrite.android.ws.view;

import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.location.Location;
import android.os.Handler;
import android.util.Log;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.activity.MapSearchTabActivity;


public class ScaleBarOverlay extends Overlay {

    // ===========================================================
    // Fields
    // ===========================================================

    // Defaults

    boolean enabled = true;

    float xOffset = 10;
    float yOffset = 10;
    float lineWidth = 2;
    int textSize = 20;

    boolean imperial = false;
    boolean nautical = false;

    boolean latitudeBar = true;
    boolean longitudeBar = false;

    // Internal

    protected final MapView mapView;

    private Context context;

    protected final Picture scaleBarPicture = new Picture();
    private final Matrix scaleBarMatrix = new Matrix();

    float xdpi;
    float ydpi;
    int screenWidth;
    int screenHeight;
    private final MapSearchTabActivity master;

    private Handler handler = new Handler();
    private int lastLonSpan = -1;

    // ===========================================================
    // Constructors
    // ===========================================================

    public ScaleBarOverlay(Context _context, MapSearchTabActivity master, MapView mapView) {
        super();

        this.master = master;
        this.context = _context;
        this.mapView = mapView;

        xdpi = this.context.getResources().getDisplayMetrics().xdpi;
        ydpi = this.context.getResources().getDisplayMetrics().ydpi;

        screenWidth = this.context.getResources().getDisplayMetrics().widthPixels;
        screenHeight = this.context.getResources().getDisplayMetrics().heightPixels;

    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the lineWidth
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * @param lineWidth the lineWidth to set
     */
    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * @return the imperial
     */
    public boolean isImperial() {
        return imperial;
    }

    public void setImperial() {
        this.imperial = true;
        this.nautical = false;
        createScaleBarPicture();
    }

    /**
     * @return the nautical
     */
    public boolean isNautical() {
        return nautical;
    }

    public void setNautical() {
        this.nautical = true;
        this.imperial = false;
        createScaleBarPicture();
    }

    public void setMetric() {
        this.nautical = false;
        this.imperial = false;
        createScaleBarPicture();
    }

    public void drawLatitudeScale(boolean latitude) {
        this.latitudeBar = latitude;
    }

    public void drawLongitudeScale(boolean longitude) {
        this.longitudeBar = longitude;
    }

    @Override
    public void draw(Canvas canvas, MapView localMapView, boolean shadow) {
        if (this.enabled) {
            if (shadow == false) {
                Projection projection = localMapView.getProjection();
                int lonSpan = projection.fromPixels(0,mapView.getHeight()/2).getLongitudeE6() -
                        projection.fromPixels(mapView.getWidth(),mapView.getHeight()/2).getLongitudeE6();

                if (lonSpan != lastLonSpan) {
                    createScaleBarPicture();
                    lastLonSpan = lonSpan;
                }

                this.scaleBarMatrix.setTranslate(-1 * (scaleBarPicture.getWidth() / 2 - 0.5f), -1 * (scaleBarPicture.getHeight() / 2 - 0.5f));
                float yPos = ydpi / 2 + canvas.getHeight() - 85;
                float xPos = xdpi / 2 + canvas.getWidth() - scaleBarPicture.getWidth() - 30;
                this.scaleBarMatrix.postTranslate(xPos, yPos);

                try {
                    canvas.save();
                    canvas.setMatrix(scaleBarMatrix);
                    canvas.drawPicture(scaleBarPicture);
                    canvas.restore();
                }

                catch (Exception e) {
                    Log.e(WSAndroidApplication.TAG, "Disabling scale bar due to error: " + e);
                    disableScaleBar();
                }
            }
        }
    }

    // ===========================================================
    // Methods
    // ===========================================================

    public void disableScaleBar() {
        this.enabled = false;
    }

    public boolean enableScaleBar() {
        return this.enabled = true;
    }

    private void createScaleBarPicture() {
        // We want the scale bar to be as long as the closest round-number miles/kilometers
        // to 1-inch at the latitude at the current center of the screen.

        Projection projection = mapView.getProjection();

        if (projection == null) {
            return;
        }

        Location locationP1 = new Location("ScaleBar location p1");
        Location locationP2 = new Location("ScaleBar location p2");

        // Two points, 1-inch apart in x/latitude, centered on screen
        GeoPoint p1 = projection.fromPixels((int) ((screenWidth / 2) - (xdpi / 2)), screenHeight / 2);
        GeoPoint p2 = projection.fromPixels((int) ((screenWidth / 2) + (xdpi / 2)), screenHeight / 2);

        locationP1.setLatitude(p1.getLatitudeE6() / 1E6);
        locationP2.setLatitude(p2.getLatitudeE6() / 1E6);
        locationP1.setLongitude(p1.getLongitudeE6() / 1E6);
        locationP2.setLongitude(p2.getLongitudeE6() / 1E6);

        float xMetersPerInch = locationP1.distanceTo(locationP2);

        p1 = projection.fromPixels(screenWidth / 2, (int) ((screenHeight / 2) - (ydpi / 2)));
        p2 = projection.fromPixels(screenWidth / 2, (int) ((screenHeight / 2) + (ydpi / 2)));

        locationP1.setLatitude(p1.getLatitudeE6() / 1E6);
        locationP2.setLatitude(p2.getLatitudeE6() / 1E6);
        locationP1.setLongitude(p1.getLongitudeE6() / 1E6);
        locationP2.setLongitude(p2.getLongitudeE6() / 1E6);

        float yMetersPerInch = locationP1.distanceTo(locationP2);

        final Paint barPaint = new Paint();
        barPaint.setColor(Color.BLACK);
        barPaint.setAntiAlias(true);
        barPaint.setStyle(Style.FILL);
        barPaint.setAlpha(255);

        final Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Style.FILL);
        textPaint.setAlpha(255);
        textPaint.setTextSize(textSize);

        final Canvas canvas = scaleBarPicture.beginRecording((int) xdpi, (int) ydpi);

        if (latitudeBar) {
            String xMsg = scaleBarLengthText(xMetersPerInch, imperial, nautical);
            Rect xTextRect = new Rect();
            textPaint.getTextBounds(xMsg, 0, xMsg.length(), xTextRect);

            int textSpacing = (int) (xTextRect.height() / 5.0);

            canvas.drawRect(xOffset, yOffset, xOffset + xdpi, yOffset + lineWidth, barPaint);
            canvas.drawRect(xOffset + xdpi, yOffset, xOffset + xdpi + lineWidth, yOffset + xTextRect.height() + lineWidth + textSpacing, barPaint);

            if (!longitudeBar) {
                canvas.drawRect(xOffset, yOffset, xOffset + lineWidth, yOffset + xTextRect.height() + lineWidth + textSpacing, barPaint);
            }
            canvas.drawText(xMsg, (xOffset + xdpi / 2 - xTextRect.width() / 2), (yOffset + xTextRect.height() + lineWidth + textSpacing), textPaint);
        }

        if (longitudeBar) {
            String yMsg = scaleBarLengthText(yMetersPerInch, imperial, nautical);
            Rect yTextRect = new Rect();
            textPaint.getTextBounds(yMsg, 0, yMsg.length(), yTextRect);

            int textSpacing = (int) (yTextRect.height() / 5.0);

            canvas.drawRect(xOffset, yOffset, xOffset + lineWidth, yOffset + ydpi, barPaint);
            canvas.drawRect(xOffset, yOffset + ydpi, xOffset + yTextRect.height() + lineWidth + textSpacing, yOffset + ydpi + lineWidth, barPaint);

            if (!latitudeBar) {
                canvas.drawRect(xOffset, yOffset, xOffset + yTextRect.height() + lineWidth + textSpacing, yOffset + lineWidth, barPaint);
            }

            float x = xOffset + yTextRect.height() + lineWidth + textSpacing;
            float y = yOffset + ydpi / 2 + yTextRect.width() / 2;

            canvas.rotate(-90, x, y);
            canvas.drawText(yMsg, x, y + textSpacing, textPaint);

        }

        scaleBarPicture.endRecording();
    }

    private String scaleBarLengthText(float meters, boolean imperial, boolean nautical) {
        float distance;

        if (this.imperial) {
            if (meters >= 1609.344) {
                return Math.round(meters / 1609.344) + " mi";
            } else if (meters >= 1609.344 / 10) {
                return Math.round((meters / 160.9344) / 10.0) + " mi";
            } else {
                return Math.round(meters * 3.2808399) + " ft";
            }
        } else if (this.nautical) {
            if (meters >= 1852) {
                return Math.round((meters / 1852)) + " nm";
            } else if (meters >= 1852 / 10) {
                return Math.round(((meters / 185.2)) / 10.0) + " nm";
            } else {
                return Math.round((meters * 3.2808399)) + " ft";
            }
        } else {
            if (meters >= 1000) {
                return Math.round((meters / 1000)) + " km";
            } else if (meters > 100) {
                return Math.round((meters / 100.0) / 10.0) + " km";
            } else {
                return Math.round(meters) + " m";
            }
        }
    }

    @Override
    public boolean onTap(GeoPoint point, MapView mapView) {
        // Do not react to screen taps.
        return false;
    }

}
