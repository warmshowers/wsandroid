package fi.bitrite.android.ws.model;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;

import com.google.android.gms.maps.model.LatLng;
import com.yelp.parcelgen.JsonParser.DualCreator;
import org.json.JSONException;
import org.json.JSONObject;

import fi.bitrite.android.ws.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Host extends _Host {

    private long mUpdated;

    public static Host createFromBriefInfo(HostBriefInfo briefInfo) {
        Host host = new Host();
        host.mId = briefInfo.getId();
        host.mName = briefInfo.getName();
        host.mFullname = briefInfo.getFullname();
        host.mComments = briefInfo.getAboutMe();
        host.mLongitude = briefInfo.getLongitude();
        host.mLatitude = briefInfo.getLatitude();
        return host;
    }

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

    public String getLocation() {

        String location = "";
        if (!getStreet().isEmpty()) {
            location += getStreet() + "\n";
        }

        if (!getAdditional().isEmpty()) {
            location += getAdditional() + "\n";
        }
        location += getCity() + ", " + getProvince().toUpperCase();
        if (!getPostalCode().isEmpty()) {
            location += " " + getPostalCode();
        }

        if (!getCountry().isEmpty()) {
            location += ", " + getCountry().toUpperCase();
        }

        return location;
    }

    public String getNearbyServices(Context context) {
        Resources r = context.getResources();

        String nearbyServices = "";
        if (!getMotel().isEmpty()) {
            nearbyServices += r.getString(R.string.nearby_service_accommodation) + ": " + getMotel() + ", ";
        }
        if (!getBikeshop().isEmpty()) {
            nearbyServices += r.getString(R.string.nearby_service_bikeshop) + ": " +getBikeshop() + ", ";
        }
        if (!getCampground().isEmpty()) {
            nearbyServices += r.getString(R.string.nearby_service_campground) + ": " +getCampground() + ", ";
        }

        return nearbyServices;
    }
    public String getHostServices(Context context) {
        StringBuilder sb = new StringBuilder();
        Resources r = context.getResources();

        if (hasService(getShower()))
            sb.append(r.getString(R.string.host_service_shower) + ", ");
        if (hasService(getFood()))
            sb.append(r.getString(R.string.host_services_food) + ", ");
        if (hasService(getBed()))
            sb.append(r.getString(R.string.host_services_bed) + ", ");
        if (hasService(getLaundry()))
            sb.append(r.getString(R.string.host_service_laundry) + ", ");
        if (hasService(getStorage()))
            sb.append(r.getString(R.string.host_service_storage) + ", ");
        if (hasService(getKitchenUse()))
            sb.append(r.getString(R.string.host_service_kitchen) + ", ");
        if (hasService(getLawnspace()))
            sb.append(r.getString(R.string.host_service_tentspace) + ", ");
        if (hasService(getSag()))
            sb.append(context.getString(R.string.host_service_sag));

        return sb.toString();
    }

    private boolean hasService(String service) {
        return !service.isEmpty() && service.equals("1");
    }

    public boolean isNotCurrentlyAvailable() {
        return getNotCurrentlyAvailable().equals("1");
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

    public Date getCreatedAsDate() {
        return stringToDate(mCreated);
    }
    public Date getLastLoginAsDate() {
        return stringToDate(mLogin);
    }
    protected Date stringToDate(String s) {

        int intDate = 0;
        try {
            intDate = Integer.parseInt(s);
        } catch (Exception e) {
            // Ignore
        }
        Date d = new Date((long)intDate * 1000);
        return d;
    }


    private String formatDate(String timestamp) {
        if (timestamp.isEmpty()) {
            return "";
        }

        Date date = new Date(Long.parseLong(timestamp) * 1000);
        DateFormat dateFormat = SimpleDateFormat.getDateInstance();
        return dateFormat.format(date);
    }

    public Object getLatLng() {
        return new LatLng(Double.parseDouble(mLatitude), Double.parseDouble(mLongitude));
    }
}
