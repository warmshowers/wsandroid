package fi.bitrite.android.ws.api.response;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

import fi.bitrite.android.ws.model.SimpleUser;

public class UserSearchByLocationResponse {
    // {
    //    "status": {"delivered":800,"totalresults":"999","status":"complete"},
    //    "query_data":{
    //      "sql": "SELECT u.uid, u.created, u.access, u.login, u.name AS name, w.fullname AS fullname, street, city, province, postal_code, countryCode, latitude, longitude,\n    source, picture, notcurrentlyavailable,\n    DEGREES(ACOS(SIN(RADIANS(:centerlat)) * SIN(RADIANS(latitude))+COS(RADIANS(:centerlat)) * COS(RADIANS(latitude)) * COS(RADIANS(:centerlon - longitude)))) * 60 AS distance,\n    CONCAT(latitude, ',', longitude) AS position\n    FROM {users} u, {user_location} l, {wsuser} w\n    WHERE latitude > :minlat AND latitude < :maxlat AND longitude > :minlon AND longitude < :maxlon \n    AND u.uid = l.oid AND u.uid = w.uid AND u.status > 0\n    AND !notcurrentlyavailable\n    AND u.uid NOT IN (SELECT ur.uid FROM users_roles ur WHERE ur.rid = 9)\n\t\tORDER BY distance ASC",
    //      "centerlat":"47.000000",
    //      "centerlon":"8.000000",
    //      "minlat":"46.000000",
    //      "maxlat":"48.000000",
    //      "minlon":"7.000000",
    //      "maxlon":"9.000000",
    //      "limit":800
    //    },
    //    "accounts":[ { user1 }, { user2 }, ... ]
    // }

    /**
     * Unfortunately, the user we get in this response differs from the users we get from other API
     * calls. This should be fixed on the server side in the future.
     */
    public static class User {
        @SerializedName("uid") public int id;
        @SerializedName("created") public Date created;
        @SerializedName("access") public Date lastAccess;
        @SerializedName("login") public Date lastLogin;

        public String name;
        public String fullname;
        public String street;
        public String city;
        public String province;
        public String postalCode;
        @SerializedName("countryCode") public String countryCode;

        public int source;
        @SerializedName("picture") public int pictureId;

        public double latitude;
        public double longitude;
        @SerializedName("distance") public double distanceToCenter;

        @SerializedName("notcurrentlyavailable") public boolean notCurrentlyAvailable;

        @SerializedName("profile_image_profile_picture") public String profilePictureUrl_179x200;
        @SerializedName("profile_image_mobile_profile_photo_std") public String
                profilePictureUrl_400x400;
        @SerializedName("profile_image_mobile_photo_456") public String profilePictureUrl_456x342;
        @SerializedName("profile_image_map_infoWindow") public String profilePictureUrl_50x50;

        public SimpleUser toSimpleUser() {
            return new SimpleUser(id, name, fullname, street, city, province, postalCode,
                    countryCode, new LatLng(latitude, longitude), !notCurrentlyAvailable,
                    new SimpleUser.Picture(profilePictureUrl_179x200, profilePictureUrl_400x400),
                    created, lastAccess);
        }
    }

    public static class Status {
        @SerializedName("delivered") public int numDelivered;
        @SerializedName("totalresults") public int numTotalResults;
        @SerializedName("status") public String status; // Is there anything else than "complete"?
    }

    @SerializedName("status") public Status status;
    // @SerializedName("query_data") public QueryData queryData; // We are not interested in this.
    @SerializedName("accounts") public List<User> users;
}
