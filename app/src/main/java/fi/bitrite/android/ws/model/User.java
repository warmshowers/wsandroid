package fi.bitrite.android.ws.model;

import android.text.TextUtils;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

public class User {

    public static class Location {
        private final double latitude;
        private final double longitude;

        public Location(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public LatLng toLatLng() {
            return new LatLng(latitude, longitude);
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }

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

    private final int id;
    private final String name;
    private final String fullname;

    private final String street;
    private final String additionalAddress;
    private final String city;
    private final String province;
    private final String postalCode;
    private final String countryCode;

    private final String mobilePhone;
    private final String homePhone;
    private final String workPhone;

    private final String comments;

    private final String preferredNotice;

    private int maximalCyclistCount;
    private String distanceToMotel;
    private String distanceToCampground;
    private String distanceToBikeshop;
    private boolean hasStorage;
    private boolean hasShower;
    private boolean hasKitchen;
    private boolean hasLawnspace;
    private boolean hasSag;
    private boolean hasBed;
    private boolean hasLaundry;
    private boolean hasFood;

    private final Date lastAccess;
    private final Date created;
    private final boolean isCurrentlyAvailable;

    private final Location location;
    private final String spokenLanguages;
    private final Picture profilePicture;

    // A user is starred, if it has been marked as favorite by the human using the app. It should
    // then show up in the list of favorites. However, users are also stored in the database if
    // they appear in a conversation.
    private boolean isStarred;

    public User(int id, String name, String fullname, String street, String additionalAddress,
                String city, String province, String postalCode, String countryCode,
                String mobilePhone, String homePhone, String workPhone, String comments,
                String preferredNotice, int maximalCyclistCount, String distanceToMotel,
                String distanceToCampground, String distanceToBikeshop, boolean hasStorage,
                boolean hasShower, boolean hasKitchen, boolean hasLawnspace, boolean hasSag,
                boolean hasBed, boolean hasLaundry, boolean hasFood, Date lastAccess, Date created,
                boolean isCurrentlyAvailable, Location location, String spokenLanguages,
                Picture profilePicture, boolean isStarred) {
        this.id = id;
        this.name = name;
        this.fullname = fullname;
        this.street = street;
        this.additionalAddress = additionalAddress;
        this.city = city;
        this.province = province;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
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
        this.lastAccess = lastAccess;
        this.created = created;
        this.isCurrentlyAvailable = isCurrentlyAvailable;
        this.location = location;
        this.spokenLanguages = spokenLanguages;
        this.profilePicture = profilePicture;
        this.isStarred = isStarred;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFullname() {
        return fullname;
    }

    public String getStreet() {
        return street;
    }

    public String getAdditionalAddress() {
        return additionalAddress;
    }

    public String getCity() {
        return city;
    }

    public String getProvince() {
        return province;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public String getHomePhone() {
        return homePhone;
    }

    public String getWorkPhone() {
        return workPhone;
    }

    public String getComments() {
        return comments;
    }

    public String getPreferredNotice() {
        return preferredNotice;
    }

    public int getMaximalCyclistCount() {
        return maximalCyclistCount;
    }

    public String getDistanceToMotel() {
        return distanceToMotel;
    }

    public String getDistanceToCampground() {
        return distanceToCampground;
    }

    public String getDistanceToBikeshop() {
        return distanceToBikeshop;
    }

    public boolean hasStorage() {
        return hasStorage;
    }

    public boolean hasShower() {
        return hasShower;
    }

    public boolean hasKitchen() {
        return hasKitchen;
    }

    public boolean hasLawnspace() {
        return hasLawnspace;
    }

    public boolean hasSag() {
        return hasSag;
    }

    public boolean hasBed() {
        return hasBed;
    }

    public boolean hasLaundry() {
        return hasLaundry;
    }

    public boolean hasFood() {
        return hasFood;
    }

    public Date getLastAccess() {
        return lastAccess;
    }

    public Date getCreated() {
        return created;
    }

    public boolean isCurrentlyAvailable() {
        return isCurrentlyAvailable;
    }

    public Location getLocation() {
        return location;
    }

    public String getSpokenLanguages() {
        return spokenLanguages;
    }

    public Picture getProfilePicture() {
        return profilePicture;
    }

    public boolean isStarred() {
        return isStarred;
    }

    public void setStarred(boolean isStarred) {
        this.isStarred = isStarred;
    }

    public String getStringLocation() {
        StringBuilder location = new StringBuilder();

        location.append(city).append(", ").append(province.toUpperCase());
        if (!TextUtils.isEmpty(street)) {
            location.append(street).append(", ").append(location);
        }
        return location.toString();
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
