package fi.bitrite.android.ws.ui.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.ColorInt;
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
    public void setSingleLocationMarkerFactory(MarkerFactory markerFactory) {
        mSingleLocationMarkerFactory = markerFactory;
    }
    /**
     * Sets the marker factory for when a cluster contains markers with the same location.
     */
    public void setMultiLocationMarkerFactory(MarkerFactory markerFactory) {
        mMultiLocationMarkerFactory = markerFactory;
    }

    public boolean remove(Marker marker){
        return mItems.remove(marker);
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
                mapView, cluster.getPosition(), cluster.getSize(), !isSingleLocationCluster);

        marker.setOnMarkerClickListener((m, mv) ->
                mOnClusterClickListener != null
                && mOnClusterClickListener.onClusterClick(mv, cluster));
        return marker;
    }

    public void setOnClusterClickListener(OnClusterClickListener onClusterClickListener) {
        mOnClusterClickListener = onClusterClickListener;
    }

    public static class MarkerFactory {
        private static final int[] BUCKETS = {10, 20, 50, 100, 200, 500, 1000};

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

        Marker createMarker(MapView mapView, GeoPoint position, int clusterSize, boolean useBuckets) {
            Marker marker = new Marker(mapView);
            marker.setPosition(position);
            marker.setInfoWindow(null);
            marker.setAnchor(mAnchorH, mAnchorV);

            String iconText;
            Drawable iconDrawable;
            if (useBuckets) {
                int bucket = getBucket(clusterSize);
                iconDrawable = mIconDrawable.getConstantState().newDrawable().mutate();
                iconDrawable.setColorFilter(getClusterColor(bucket), PorterDuff.Mode.SRC_ATOP);
                iconText = getClusterText(bucket);
            } else {
                iconText = Integer.toString(clusterSize);
                iconDrawable = mIconDrawable;
            }
            Drawable textDrawable = new TextDrawable(mapView.getContext(), iconText);
            Drawable[] layers = { iconDrawable, textDrawable };
            marker.setIcon(new LayerDrawable(layers));
            return marker;
        }

        /**
         * Gets the "bucket" for a particular cluster. By default, uses the number of points within
         * the cluster, bucketed to some set points.
         */
        private int getBucket(int size) {
            if (size <= BUCKETS[0]) {
                return size;
            }
            for (int i = 0; i < BUCKETS.length - 1; ++i) {
                if (size < BUCKETS[i + 1]) {
                    return BUCKETS[i];
                }
            }
            return BUCKETS[BUCKETS.length - 1];
        }

        @ColorInt
        private int getClusterColor(int clusterSize) {
            final float hueRange = 220;
            final float sizeRange = 300;
            final float size = Math.min(clusterSize, sizeRange);
            final float hue =
                    (sizeRange - size)*(sizeRange - size) / (sizeRange*sizeRange) * hueRange;
            return Color.HSVToColor(new float[]{ hue, 1f, .6f });
        }

        private String getClusterText(int bucket) {
            if (bucket < BUCKETS[0]) {
                return String.valueOf(bucket);
            }
            return String.valueOf(bucket) + "+";
        }

        private class TextDrawable extends Drawable {
            private static final int TEXT_PADDING = 32;

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
                return getIntrinsicSize();
            }
            @Override
            public int getIntrinsicHeight() {
                return getIntrinsicSize();
            }
            private int getIntrinsicSize() {
                return Math.max(mTextBounds.width(), mTextBounds.height()) + TEXT_PADDING;
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
