package fi.bitrite.android.ws.util;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.subjects.BehaviorSubject;

public class LocationManager {
    private static final int MIN_TIME_BETWEEN_LOCATION_UPDATES_MS = 1000 * 20; // 20s
    private static final int TWO_MINUTES_MS = 1000 * 60 * 2;

    private android.location.LocationManager mAndroidLocationManager;

    private final BehaviorSubject<Location> mCurrentBestLocation = BehaviorSubject.create();
    private final BehaviorSubject<Boolean> mHasEnabledProviders =
            BehaviorSubject.createDefault(false);

    public LocationManager() {
    }

    public void start(android.location.LocationManager androidLocationManager) {
        mAndroidLocationManager = androidLocationManager;
        startProvider(android.location.LocationManager.NETWORK_PROVIDER);
        startProvider(android.location.LocationManager.GPS_PROVIDER);
    }
    public void stop() {
        // Guard against NPE when GPS is switched off between start and stop
        if (mAndroidLocationManager != null) {
            mAndroidLocationManager.removeUpdates(mLocationListener);
            mAndroidLocationManager = null;
        }
    }

    public BehaviorSubject<Location> getBestLocation() {
        return mCurrentBestLocation;
    }
    public BehaviorSubject<Boolean> getHasEnabledProviders() {
        return mHasEnabledProviders;
    }

    @SuppressLint("MissingPermission")
    private void startProvider(String provider) {
        if (mAndroidLocationManager.getProvider(provider) == null) {
            return;
        }
        updateLocationIfBetter(mAndroidLocationManager.getLastKnownLocation(provider));
        mAndroidLocationManager.requestLocationUpdates(provider,
                MIN_TIME_BETWEEN_LOCATION_UPDATES_MS, 0, mLocationListener);
        boolean isEnabled = mAndroidLocationManager.isProviderEnabled(provider);
        if (isEnabled) {
            mLocationListener.onProviderEnabled(provider);
        } else {
            mLocationListener.onProviderDisabled(provider);
        }
    }

    private final android.location.LocationListener mLocationListener =
            new android.location.LocationListener() {
                private Set<String> mEnabledProviders = new HashSet<>();

                @Override
                public void onLocationChanged(Location location) {
                    updateLocationIfBetter(location);
                }
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }
                @Override
                public void onProviderEnabled(String provider) {
                    boolean wasOff = mEnabledProviders.add(provider);
                    if (wasOff) {
                        mHasEnabledProviders.onNext(!mEnabledProviders.isEmpty());
                    }
                }
                @Override
                public void onProviderDisabled(String provider) {
                    boolean wasOn = mEnabledProviders.remove(provider);
                    if (wasOn) {
                        mHasEnabledProviders.onNext(!mEnabledProviders.isEmpty());
                    }
                }
            };

    private boolean updateLocationIfBetter(Location newLocation) {
        boolean isBetter = isBetterLocation(newLocation, mCurrentBestLocation.getValue());
        if (isBetter) {
            mCurrentBestLocation.onNext(newLocation);
        }
        return isBetter;
    }

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    private static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (location == null || (location.getLatitude() == 0 && location.getLongitude() == 0)) {
            return false;
        }
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES_MS;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES_MS;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider =
                isSameProvider(location.getProvider(), currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}
