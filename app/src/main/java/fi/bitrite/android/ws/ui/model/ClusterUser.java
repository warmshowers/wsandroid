package fi.bitrite.android.ws.ui.model;


import android.text.TextUtils;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import fi.bitrite.android.ws.api.response.UserSearchByLocationResponse;
import fi.bitrite.android.ws.model.User;

/**
 * This entity is used in the {@link fi.bitrite.android.ws.ui.MapFragment}. It acts as a compatible
 * type to all the different representations of a user that we are given when using the REST API.
 *
 * TODO(saemy): Eventually remove this or make it a child of User (while still implementing ClusterItem).
 */
public class ClusterUser implements ClusterItem {
    public final int id;
    public final String fullname;

    public final String street;
    public final String additionalAddress;
    public final String postalCode;
    public final String city;
    public final String province;
    public final String countryCode;
    public final LatLng latLng;

    public final boolean isCurrentlyAvailable;

    public ClusterUser(
            int id, String fullname, String street, String additionalAddress, String postalCode,
            String city, String province, String countryCode, LatLng latLng,
            boolean isCurrentlyAvailable) {
        this.id = id;
        this.fullname = fullname;
        this.street = street;
        this.additionalAddress = additionalAddress;
        this.postalCode = postalCode;
        this.city = city;
        this.province = province;
        this.countryCode = countryCode;
        this.latLng = latLng;
        this.isCurrentlyAvailable = isCurrentlyAvailable;
    }

    public static ClusterUser from(User user) {
        return new ClusterUser(
                user.id, user.fullname, user.street, user.additionalAddress, user.postalCode,
                user.city, user.province, user.countryCode, user.location,
                user.isCurrentlyAvailable);
    }
    public static ClusterUser from(UserSearchByLocationResponse.User user) {
        return new ClusterUser(
                user.id, user.fullname, user.street, "", user.postalCode, user.city, user.province,
                user.countryCode, new LatLng(user.latitude, user.longitude),
                !user.notCurrentlyAvailable);
    }

    public String getLocationStr() {
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

    public String getStreetCityAddressStr() {
        StringBuilder result = new StringBuilder();

        if (!TextUtils.isEmpty(street)) {
            result.append(street).append(", ");
        }
        result.append(city).append(", ").append(province.toUpperCase());

        return result.toString();
    }

    @Override
    public LatLng getPosition() {
        return latLng;
    }
}
