package fi.bitrite.android.ws.api.model;

import com.google.gson.annotations.SerializedName;

import org.osmdroid.util.GeoPoint;

import java.util.Date;

import fi.bitrite.android.ws.model.SimpleUser;
import fi.bitrite.android.ws.model.User;

public class ApiUser {

    public static class Picture {
        @SerializedName("fid") public int id;
        public String filename;
        public String uri; // s3://pictures/picture-193393-4829402949.jpg
        @SerializedName("filemime") public String mimeType;
        public int filesize;
        public int status;
        public Date timestamp;
        public String url; // https://warmshowers-files.s3-us-west-2.amazonaws.com/pictures/picture-193393-4829402949.jpg
    }

    public static class CommentNotifySettings {
        @SerializedName("node_notify") public boolean isNodeNotify;
        @SerializedName("comment_notify") public boolean isCommentNotify;
    }

    @SerializedName("uid") public int id;
    public String name;
    public String theme;
    public int signatureFormat;
    public Date created;
    @SerializedName("access") public Date lastAccess;
    @SerializedName("login") public Date lastLogin;
    public int status;
    public String timezone;
    public String language;
    public Picture picture;
    public String fullname;
    @SerializedName("notcurrentlyavailable") public boolean notCurrentlyAvailable;
    public String faxNumber;
    public String street;
    @SerializedName("additional") public String additionalAddress;
    public String city;
    public String province;
    public String postalCode;
    @SerializedName("country") public String countryCode;
    public double latitude;
    public double longitude;
    @SerializedName("mobilephone") public String mobilePhone;
    @SerializedName("workphone") public String workPhone;
    @SerializedName("homephone") public String homePhone;
    public String preferredNotice;
    @SerializedName("maxcyclists") public int maximalCyclistCount;
    @SerializedName("motel") public String distanceToMotel;
    @SerializedName("campground") public String distanceToCampground;
    @SerializedName("bikeshop") public String distanceToBikeshop;
    @SerializedName("storage") public boolean hasStorage;
    @SerializedName("shower") public boolean hasShower;
    @SerializedName("kitchenuse") public boolean hasKitchen;
    @SerializedName("lawnspace") public boolean hasLawnspace;
    @SerializedName("sag") public boolean hasSag;
    @SerializedName("bed") public boolean hasBed;
    @SerializedName("laundry") public boolean hasLaundry;
    @SerializedName("food") public boolean hasFood;
    public String comments;
    @SerializedName("languagesspoken") public String spokenLanguages;
    @SerializedName("URL") String url;
    @SerializedName("becomeavailable") public Date becameAvailable;
    public Date setUnavailableTimestamp;
    public Date setAvailableTimestamp;
    public Date lastUnavailabilityPester;
    public boolean hideDonationStatus;
    @SerializedName("email_opt_out") public boolean isEmailOptOut;
    public int source;
    public CommentNotifySettings commentNotifySettings;
    @SerializedName("privatemsg_disabled") public boolean isPrivateMessagingDisabled;
    @SerializedName("profile_image_profile_picture") public String profilePictureUrl_179x200;
    @SerializedName("profile_image_mobile_profile_photo_std") public String
            profilePictureUrl_400x400;
    @SerializedName("profile_image_mobile_photo_456") public String profilePictureUrl_456x342;
    @SerializedName("profile_image_map_infoWindow") public String profilePictureUrl_50x50;


    public User toUser() {
        return new User(id, name, fullname, street, additionalAddress, city, province, postalCode,
                countryCode, new GeoPoint(latitude, longitude), mobilePhone, homePhone, workPhone,
                comments, preferredNotice, maximalCyclistCount, distanceToMotel,
                distanceToCampground, distanceToBikeshop, hasStorage, hasShower, hasKitchen,
                hasLawnspace, hasSag, hasBed, hasLaundry, hasFood, spokenLanguages,
                !notCurrentlyAvailable,
                new SimpleUser.Picture(profilePictureUrl_179x200, profilePictureUrl_400x400),
                created, lastAccess);
    }
}
