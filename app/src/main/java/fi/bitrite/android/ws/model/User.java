package fi.bitrite.android.ws.model;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

import org.osmdroid.api.IGeoPoint;

import java.util.Date;

import fi.bitrite.android.ws.R;

public class User extends SimpleUser {

    public final String additionalAddress;

    public final String mobilePhone;
    public final String homePhone;
    public final String workPhone;

    public final String comments;
    public final String preferredNotice;

    public final int maximalCyclistCount;
    public final String distanceToMotel;
    public final String distanceToCampground;
    public final String distanceToBikeshop;
    public final boolean hasStorage;
    public final boolean hasShower;
    public final boolean hasKitchen;
    public final boolean hasLawnspace;
    public final boolean hasSag;
    public final boolean hasBed;
    public final boolean hasLaundry;
    public final boolean hasFood;
    public final String spokenLanguages;

    public User(int id, String username, String fullname, String street, String additionalAddress,
                String city, String province, String postalCode, String countryCode,
                IGeoPoint location, String mobilePhone, String homePhone, String workPhone,
                String comments, String preferredNotice, int maximalCyclistCount,
                String distanceToMotel, String distanceToCampground, String distanceToBikeshop,
                boolean hasStorage, boolean hasShower, boolean hasKitchen, boolean hasLawnspace,
                boolean hasSag, boolean hasBed, boolean hasLaundry, boolean hasFood,
                String spokenLanguages, boolean isCurrentlyAvailable, Picture profilePicture,
                Date created, Date lastAccess) {
        super(id, username, fullname, street, city, province, postalCode, countryCode, location,
                isCurrentlyAvailable, profilePicture, created, lastAccess);

        this.additionalAddress = additionalAddress;

        this.mobilePhone = mobilePhone;
        this.homePhone = homePhone;
        this.workPhone = workPhone;

        this.comments = comments;
        this.preferredNotice = preferredNotice;

        this.maximalCyclistCount = maximalCyclistCount;
        this.distanceToMotel = distanceToMotel;
        this.distanceToCampground = distanceToCampground;
        this.distanceToBikeshop = distanceToBikeshop;
        this.hasStorage = hasStorage;
        this.hasShower = hasShower;
        this.hasKitchen = hasKitchen;
        this.hasLawnspace = hasLawnspace;
        this.hasSag = hasSag;
        this.hasBed = hasBed;
        this.hasLaundry = hasLaundry;
        this.hasFood = hasFood;
        this.spokenLanguages = spokenLanguages;
    }

    @Override
    public String getFullAddress() {
        StringBuilder location = new StringBuilder();
        if (!TextUtils.isEmpty(street)) {
            location.append(street).append('\n');
        }

        if (!TextUtils.isEmpty(additionalAddress)) {
            location.append(additionalAddress).append('\n');
        }
        location.append(city).append(", ").append(province.toUpperCase());
        if (!TextUtils.isEmpty(postalCode)) {
            location.append(' ').append(postalCode);
        }

        if (!TextUtils.isEmpty(countryCode)) {
            location.append(", ").append(countryCode.toUpperCase());
        }

        return location.toString();
    }

    public String getNearbyServices(Context context) {
        Resources r = context.getResources();

        StringBuilder nearbyServices = new StringBuilder();
        if (!TextUtils.isEmpty(distanceToMotel)) {
            nearbyServices.append(r.getString(R.string.nearby_service_accommodation))
                    .append(": ").append(distanceToMotel).append(", ");
        }
        if (!TextUtils.isEmpty(distanceToBikeshop)) {
            nearbyServices.append(r.getString(R.string.nearby_service_bikeshop))
                    .append(": ").append(distanceToBikeshop).append(", ");
        }
        if (!TextUtils.isEmpty(distanceToCampground)) {
            nearbyServices.append(r.getString(R.string.nearby_service_campground))
                    .append(": ").append(distanceToCampground).append(", ");
        }

        return nearbyServices.toString();
    }
    public String getUserServices(Context context) {
        StringBuilder sb = new StringBuilder();
        Resources r = context.getResources();

        if (hasShower) {
            sb.append(r.getString(R.string.user_service_shower)).append(", ");
        }
        if (hasFood) {
            sb.append(r.getString(R.string.user_services_food)).append(", ");
        }
        if (hasBed) {
            sb.append(r.getString(R.string.user_services_bed)).append(", ");
        }
        if (hasLaundry) {
            sb.append(r.getString(R.string.user_service_laundry)).append(", ");
        }
        if (hasStorage) {
            sb.append(r.getString(R.string.user_service_storage)).append(", ");
        }
        if (hasKitchen) {
            sb.append(r.getString(R.string.user_service_kitchen)).append(", ");
        }
        if (hasLawnspace) {
            sb.append(r.getString(R.string.user_service_tentspace)).append(", ");
        }
        if (hasSag) {
            sb.append(context.getString(R.string.user_service_sag)).append(", ");
        }
        if (sb.length() > 2) {
            sb.delete(sb.length() - 2, sb.length()); // Remove last ", "
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof User)) {
            return false;
        }

        return id == ((User) other).id;
    }

    @Override
    public int hashCode() {
        return Integer.valueOf(id).hashCode();
    }
}
