package fi.bitrite.android.ws.model;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.text.TextUtils;

import com.google.android.gms.maps.model.LatLng;
import com.yelp.parcelgen.JsonParser.DualCreator;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import fi.bitrite.android.ws.R;

public class Host extends _Host {

    public static final DualCreator<Host> CREATOR = new DualCreator<Host>() {

        public Host[] newArray(int size) {
            return new Host[size];
        }

        public Host createFromParcel(Parcel source) {
            Host object = new Host();
            object.readFromParcel(source);
            return object;
        }

        @Override
        public Host parse(JSONObject obj) throws JSONException {
            Host newInstance = new Host();
            newInstance.readFromJson(obj);
            return newInstance;
        }
    };
    private long mUpdated;

    public Host() {
        super();
    }

    public Host(int id, String name, String fullname, String street, String additional, String city,
                String province, String postalCode, String country, String mobilePhone,
                String homePhone, String workPhone, String comments, String preferredNotice,
                String maxCyclists, String notCurrentlyAvailable, String bed, String bikeshop,
                String campground, String food, String kitchenUse, String laundry, String lawnspace,
                String motel, String sag, String shower, String storage, String latitude,
                String longitude, String login, String created, String languagesSpoken,
                String picture, String profilePictureSmall, String profilePictureLarge) {
        super(id, name, fullname, street, additional, city, province, postalCode, country,
                mobilePhone, homePhone, workPhone, comments, preferredNotice, maxCyclists,
                notCurrentlyAvailable, bed, bikeshop, campground, food, kitchenUse, laundry,
                lawnspace, motel, sag, shower, storage, latitude, longitude, login, created,
                languagesSpoken, picture, profilePictureSmall, profilePictureLarge);
    }

    public String getLocation() {
        StringBuilder location = new StringBuilder();
        if (!TextUtils.isEmpty(getStreet())) {
            location.append(getStreet()).append('\n');
        }

        if (!TextUtils.isEmpty(getAdditional())) {
            location.append(getAdditional()).append('\n');
        }
        location.append(getCity()).append(", ").append(getProvince().toUpperCase());
        if (!TextUtils.isEmpty(getPostalCode())) {
            location.append(' ').append(getPostalCode());
        }

        if (!TextUtils.isEmpty(getCountry())) {
            location.append(", ").append(getCountry().toUpperCase());
        }

        return location.toString();
    }

    public String getNearbyServices(Context context) {
        Resources r = context.getResources();

        StringBuilder nearbyServices = new StringBuilder();
        if (!TextUtils.isEmpty(getMotel())) {
            nearbyServices.append(r.getString(R.string.nearby_service_accommodation))
                    .append(": ").append(getMotel()).append(", ");
        }
        if (!TextUtils.isEmpty(getBikeshop())) {
            nearbyServices.append(r.getString(R.string.nearby_service_bikeshop))
                    .append(": ").append(getBikeshop()).append(", ");
        }
        if (!TextUtils.isEmpty(getCampground())) {
            nearbyServices.append(r.getString(R.string.nearby_service_campground))
                    .append(": ").append(getCampground()).append(", ");
        }

        return nearbyServices.toString();
    }
    public String getHostServices(Context context) {
        StringBuilder sb = new StringBuilder();
        Resources r = context.getResources();

        if (hasService(getShower())) {
            sb.append(r.getString(R.string.host_service_shower)).append(", ");
        }
        if (hasService(getFood())) {
            sb.append(r.getString(R.string.host_services_food)).append(", ");
        }
        if (hasService(getBed())) {
            sb.append(r.getString(R.string.host_services_bed)).append(", ");
        }
        if (hasService(getLaundry())) {
            sb.append(r.getString(R.string.host_service_laundry)).append(", ");
        }
        if (hasService(getStorage())) {
            sb.append(r.getString(R.string.host_service_storage)).append(", ");
        }
        if (hasService(getKitchenUse())) {
            sb.append(r.getString(R.string.host_service_kitchen)).append(", ");
        }
        if (hasService(getLawnspace())) {
            sb.append(r.getString(R.string.host_service_tentspace)).append(", ");
        }
        if (hasService(getSag())) { sb.append(context.getString(R.string.host_service_sag)); }

        return sb.toString();
    }

    private boolean hasService(String service) {
        return !TextUtils.isEmpty(service) && service.equals("1");
    }

    public boolean isNotCurrentlyAvailable() {
        return hasService(mNotCurrentlyAvailable);
    }

    public long getUpdated() {
        return mUpdated;
    }

    public void setUpdated(long updated) {
        mUpdated = updated;
    }

    public String getMemberSince() {
        return formatDate(getCreated());
    }

    public String getLastLogin() {
        return getLogin();
    }

    public String getStreetCityAddress() {
        StringBuilder result = new StringBuilder();
        if (!TextUtils.isEmpty(mStreet)) {
            result.append(mStreet).append(", ");
        }
        result.append(mCity).append(", ").append(mProvince.toUpperCase());
        return result.toString();
    }

    public Date getCreatedAsDate() {
        return stringToDate(mCreated);
    }
    public Date getLastLoginAsDate() {
        return stringToDate(mLogin);
    }
    private Date stringToDate(String s) {
        int intDate = 0;
        try {
            intDate = Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }

        return new Date(intDate * 1000L);
    }


    private String formatDate(String timestamp) {
        if (timestamp.isEmpty()) {
            return "";
        }

        Date date = new Date(Long.parseLong(timestamp) * 1000);
        DateFormat dateFormat = SimpleDateFormat.getDateInstance();
        return dateFormat.format(date);
    }

    public LatLng getLatLng() {
        return new LatLng(Double.parseDouble(mLatitude), Double.parseDouble(mLongitude));
    }
}
