package fi.bitrite.android.ws.model;

import android.text.TextUtils;

import org.osmdroid.api.IGeoPoint;

import java.util.Date;

public class SimpleUser {
    public static class Picture {
        private final String smallUrl;
        private final String largeUrl;

        public Picture(String smallUrl, String largeUrl) {
            this.smallUrl = smallUrl;
            this.largeUrl = largeUrl;
        }

        public String getSmallUrl() {
            return smallUrl;
        }

        public String getLargeUrl() {
            return largeUrl;
        }
    }

    public final int id;
    public final String username;
    public final String fullname;

    public final String street;
    public final String city;
    public final String province;
    public final String postalCode;
    public final String countryCode;
    public final IGeoPoint location;

    public final boolean isCurrentlyAvailable;
    public final Picture profilePicture;

    public final Date created;
    public final Date lastAccess;

    public SimpleUser(int id, String username, String fullname, String street, String city,
                      String province, String postalCode, String countryCode, IGeoPoint location,
                      boolean isCurrentlyAvailable, Picture profilePicture, Date created,
                      Date lastAccess) {
        this.id = id;
        this.username = username;
        this.fullname = fullname;

        this.street = street;
        this.city = city;
        this.province = province;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
        this.location = location;

        this.isCurrentlyAvailable = isCurrentlyAvailable;
        this.profilePicture = profilePicture;

        this.created = created;
        this.lastAccess = lastAccess;
    }

    public String getName() {
        return TextUtils.isEmpty(fullname) ? username : fullname;
    }

    public String getStreetCityAddress() {
        StringBuilder result = new StringBuilder();
        if (!TextUtils.isEmpty(street)) {
            result.append(street).append(", ");
        }
        result.append(city).append(", ").append(province.toUpperCase());
        return result.toString();
    }

    public String getFullAddress() {
        StringBuilder location = new StringBuilder();
        if (!TextUtils.isEmpty(street)) {
            location.append(street).append('\n');
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
}
