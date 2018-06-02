package fi.bitrite.android.ws.ui.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.clustering.StaticCluster;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class UserMarkerClusterer extends RadiusMarkerClusterer {
    public interface OnClusterClickListener{
        boolean onClusterClick(MapView mapView, StaticCluster cluster);
    }
    private OnClusterClickListener mOnClusterClickListener;

    private MarkerFactory mSingleLocationMarkerFactory;
    private MarkerFactory mMultiLocationMarkerFactory;

    public UserMarkerClusterer(Context ctx) {
        super(ctx);
    }

    /**
     * Sets the marker factory for when a cluster contains markers with different locations.
     */
    public void setmSingleLocationMarkerFactory(MarkerFactory markerFactory) {
        mSingleLocationMarkerFactory = markerFactory;
    }
    /**
     * Sets the marker factory for when a cluster contains markers with the same location.
     */
    public void setMultiLocationMarkerFactory(MarkerFactory markerFactory) {
        mMultiLocationMarkerFactory = markerFactory;
    }

    @Override
    public Marker buildClusterMarker(StaticCluster cluster, MapView mapView) {
        boolean isSingleLocationCluster = true;
        IGeoPoint firstPos = cluster.getItem(0).getPosition();
        for (int i = 1; i < cluster.getSize() && isSingleLocationCluster; ++i) {
            isSingleLocationCluster = firstPos.equals(cluster.getItem(i).getPosition());
        }

        MarkerFactory markerFactory = isSingleLocationCluster
                ? mSingleLocationMarkerFactory
                : mMultiLocationMarkerFactory;
        Marker marker = markerFactory.createMarker(
                mapView, cluster.getPosition(), Integer.toString(cluster.getSize()));

        marker.setOnMarkerClickListener((m, mv) ->
                mOnClusterClickListener != null
                && mOnClusterClickListener.onClusterClick(mv, cluster));
        return marker;
    }

    public void setOnClusterClickListener(OnClusterClickListener onClusterClickListener) {
        mOnClusterClickListener = onClusterClickListener;
    }

    public static class MarkerFactory {
        private float mAnchorH = Marker.ANCHOR_CENTER;
        private float mAnchorV = Marker.ANCHOR_CENTER;
        private float mTextAnchorH = Marker.ANCHOR_CENTER;
        private float mTextAnchorV = Marker.ANCHOR_CENTER;
        private int mTextPaddingX = 0;
        private int mTextPaddingY = 0;

        private Drawable mIconDrawable;

        public MarkerFactory(Drawable iconDrawable) {
            mIconDrawable = iconDrawable;
        }

        public void setMarkerAnchor(float anchorH, float anchorV) {
            mAnchorH = anchorH;
            mAnchorV = anchorV;
        }

        public void setTextAnchor(float textAnchorH, float textAnchorV) {
            mTextAnchorH = textAnchorH;
            mTextAnchorV = textAnchorV;
        }

        public void setTextPadding(int paddingX, int paddingY) {
            mTextPaddingX = paddingX;
            mTextPaddingY = paddingY;
        }

        Marker createMarker(MapView mapView, GeoPoint position, String text) {
            Marker marker = new Marker(mapView);
            marker.setPosition(position);
            marker.setInfoWindow(null);
            marker.setAnchor(mAnchorH, mAnchorV);

            Drawable textDrawable = new TextDrawable(mapView.getContext(), text);
            Drawable[] layers = { mIconDrawable, textDrawable };
            marker.setIcon(new LayerDrawable(layers));
            return marker;
        }

        private class TextDrawable extends Drawable {
            private final String mText;

            private final Paint mPaint;
            private final Rect mTextBounds = new Rect();

            TextDrawable(Context context, String text) {
                mText = text;

                mPaint = new Paint();
                mPaint.setColor(Color.WHITE);
                mPaint.setTextSize(15 * context.getResources().getDisplayMetrics().density);
                mPaint.setFakeBoldText(true);
                mPaint.setTextAlign(Paint.Align.LEFT);
                mPaint.setAntiAlias(true);
                mPaint.getTextBounds(mText, 0, mText.length(), mTextBounds);
            }

            @Override
            public void draw(@NonNull Canvas canvas) {
                final Rect bounds = getBounds();
                float x = bounds.left + mTextPaddingX
                          + mTextAnchorH * (bounds.width() - mTextBounds.width())
                          - mTextBounds.left;
                float y = bounds.top + mTextBounds.height() + mTextPaddingY
                          + mTextAnchorV*(bounds.height() - mTextBounds.height())
                          - mTextBounds.bottom;
                canvas.drawText(mText, x, y, mPaint);
            }

            @Override
            public int getIntrinsicWidth() {
                return mTextBounds.width();
            }
            @Override
            public int getIntrinsicHeight() {
                return mTextBounds.height();
            }
            @Override
            public void setAlpha(int alpha) {
            }
            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {
            }
            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        }
    }
}
