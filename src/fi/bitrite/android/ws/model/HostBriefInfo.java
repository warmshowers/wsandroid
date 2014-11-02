package fi.bitrite.android.ws.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.yelp.parcelgen.JsonParser.DualCreator;


/**
 * Used for passing search results. More in-depth information is handled by the Host object.
 */
public class HostBriefInfo implements Parcelable, ClusterItem {

    private int mId;
    private String mUsername, mFullName, mStreet, mCity, mProvince, mCountry, mAboutMe, mLatitude, mLongitude;
    private String mUpdated;
    
    public HostBriefInfo(int id, String username, String fullName, String street, String city, String province, String country, String aboutMe) {
        mId = id;
        mUsername = username;
        mFullName = fullName;
        mStreet = street;
        mCity = city;
        mProvince = province;
        mCountry = country;
        mAboutMe = aboutMe;
    }

    public HostBriefInfo(int id, Host host) {
        mId = id;
        mUsername = host.getName();
        mFullName = host.getFullname();
        mStreet = host.getStreet();
        mCity = host.getCity();
        mProvince = host.getProvince();
        mCountry = host.getCountry();

        mAboutMe = host.getComments();
        mUpdated = host.getUpdated();
    }

    public HostBriefInfo() {
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mUsername;
    }
    
    public String getFullname() {
        return mFullName;
    }

    public String getLocation() {
        String location = mCity + ", " + mProvince.toUpperCase();
        if (mStreet != "" && mStreet.length() > 0) {
            location = mStreet + ", " + location;
        }
        return location;
    }

    public String getAboutMe() {
        return mAboutMe;
    }

    public String getLongitude() {
        return mLongitude;
    }

    public void setLongitude(String longitude) {
        this.mLongitude = longitude;
    }

    public String getLatitude() {
        return mLatitude;
    }

    public void setLatitude(String mLatitude) {
        this.mLatitude = mLatitude;
    }

    public String getUpdated() {
        return mUpdated;
    }

    public LatLng getLatLng() {
        return new LatLng(Double.parseDouble(mLatitude), Double.parseDouble(mLongitude));
    }
    
    public int describeContents() {
        return 0;
    }
    
    public static final DualCreator<HostBriefInfo> CREATOR = new DualCreator<HostBriefInfo>() {

        public HostBriefInfo[] newArray(int size) {
            return new HostBriefInfo[size];
        }

        public HostBriefInfo createFromParcel(Parcel source) {
            HostBriefInfo object = new HostBriefInfo();
            object.readFromParcel(source);
            return object;
        }
    };  
    
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeString(mUsername);
        dest.writeString(mFullName);
        dest.writeString(mStreet);
        dest.writeString(mAboutMe);
        dest.writeString(mLongitude);
        dest.writeString(mLatitude);
        dest.writeString(mCity);
        dest.writeString(mProvince);
        dest.writeString(mCountry);
    }
    
    public void readFromParcel(Parcel src) {
        mId = src.readInt();
        mUsername = src.readString();
        mFullName = src.readString();
        mStreet = src.readString();
        mAboutMe = src.readString();
        mLongitude = src.readString();
        mLatitude = src.readString();
        mCity = src.readString();
        mProvince = src.readString();
        mCountry = src.readString();
    }

    @Override
    public LatLng getPosition() {
        return new LatLng(Double.parseDouble(mLatitude), Double.parseDouble(mLongitude));
    }

    public String getStreet() {
        return mStreet;
    }

    public String getCity() {
        return mCity;
    }

    public String getProvince() {
        return mProvince;
    }
}
