package fi.bitrite.android.ws.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Automatically generated Parcelable implementation for _Host.
 * DO NOT MODIFY THIS FILE MANUALLY! IT WILL BE OVERWRITTEN THE NEXT TIME
 * _Host's PARCELABLE DESCRIPTION IS CHANGED.
 */
/* package */ abstract class _Host implements Parcelable {

    protected int mId;
    protected String mName = "";
    protected String mFullname = "";
    protected String mStreet = "";
    protected String mAdditional = "";
    protected String mCity = "";
    protected String mProvince = "";
    protected String mPostalCode = "";
    protected String mCountry = "";
    protected String mMobilePhone = "";
    protected String mHomePhone = "";
    protected String mWorkPhone = "";
    protected String mComments = "";
    protected String mPreferredNotice = "";
    protected String mMaxCyclists = "";
    protected String mNotCurrentlyAvailable = "";
    protected String mBed = "";
    protected String mBikeshop = "";
    protected String mCampground = "";
    protected String mFood = "";
    protected String mKitchenUse = "";
    protected String mLaundry = "";
    protected String mLawnspace = "";
    protected String mMotel = "";
    protected String mSag = "";
    protected String mShower = "";
    protected String mStorage = "";
    protected String mLatitude = "";
    protected String mLongitude = "";
    protected String mLogin = "";
    protected String mCreated = "";
    protected String mLanguagesSpoken = "";
    protected String mPicture = "";
    protected String mProfilePictureSmall = "";
    protected String mProfilePictureLarge = "";

    protected _Host(int id, String name, String fullname, String street, String additional, String city, String province, String postalCode, String country, String mobilePhone, String homePhone, String workPhone, String comments, String preferredNotice, String maxCyclists, String notCurrentlyAvailable, String bed, String bikeshop, String campground, String food, String kitchenUse, String laundry, String lawnspace, String motel, String sag, String shower, String storage, String latitude, String longitude, String login, String created, String languagesSpoken, String picture, String profilePictureSmall, String profilePictureLarge) {
        this();
        mId = id;
        mName = name;
        mFullname = fullname;
        mStreet = street;
        mAdditional = additional;
        mCity = city;
        mProvince = province;
        mPostalCode = postalCode;
        mCountry = country;
        mMobilePhone = mobilePhone;
        mHomePhone = homePhone;
        mWorkPhone = workPhone;
        mComments = comments;
        mPreferredNotice = preferredNotice;
        mMaxCyclists = maxCyclists;
        mNotCurrentlyAvailable = notCurrentlyAvailable;
        mBed = bed;
        mBikeshop = bikeshop;
        mCampground = campground;
        mFood = food;
        mKitchenUse = kitchenUse;
        mLaundry = laundry;
        mLawnspace = lawnspace;
        mMotel = motel;
        mSag = sag;
        mShower = shower;
        mStorage = storage;
        mLatitude = latitude;
        mLongitude = longitude;
        mLogin = login;
        mCreated = created;
        mLanguagesSpoken = languagesSpoken;
        mPicture = picture;
        mProfilePictureSmall = profilePictureSmall;
        mProfilePictureLarge = profilePictureLarge;
    }

    protected _Host() {
        super();
    }

    public int getUid() {
        return getId();
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String getFullname() {
        return mFullname;
    }

    public String getStreet() {
        return mStreet;
    }

    public String getAdditional() {
        return mAdditional;
    }

    public String getCity() {
        return mCity;
    }

    public String getProvince() {
        return mProvince;
    }

    public String getPostalCode() {
        return mPostalCode;
    }

    public String getCountry() {
        return mCountry;
    }

    public String getMobilePhone() {
        return mMobilePhone;
    }

    public String getHomePhone() {
        return mHomePhone;
    }

    public String getWorkPhone() {
        return mWorkPhone;
    }

    public String getComments() {
        return mComments;
    }

    public String getPreferredNotice() {
        return mPreferredNotice;
    }

    public String getMaxCyclists() {
        return mMaxCyclists;
    }

    public String getNotCurrentlyAvailable() {
        return mNotCurrentlyAvailable;
    }

    public String getBed() {
        return mBed;
    }

    public String getBikeshop() {
        return mBikeshop;
    }

    public String getCampground() {
        return mCampground;
    }

    public String getFood() {
        return mFood;
    }

    public String getKitchenUse() {
        return mKitchenUse;
    }

    public String getLaundry() {
        return mLaundry;
    }

    public String getLawnspace() {
        return mLawnspace;
    }

    public String getMotel() {
        return mMotel;
    }

    public String getSag() {
        return mSag;
    }

    public String getShower() {
        return mShower;
    }

    public String getStorage() {
        return mStorage;
    }

    public String getLatitude() {
        return mLatitude;
    }

    public String getLongitude() {
        return mLongitude;
    }

    public String getLogin() {
        return mLogin;
    }

    public String getCreated() {
        return mCreated;
    }

    public String getPicture() { return mPicture; }

    public String getProfilePictureSmall() { return mProfilePictureSmall; }

    public String getProfilePictureLarge() { return mProfilePictureLarge; }

    public String getLanguagesSpoken() {
        return mLanguagesSpoken;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mName);
        parcel.writeString(mFullname);
        parcel.writeString(mStreet);
        parcel.writeString(mAdditional);
        parcel.writeString(mCity);
        parcel.writeString(mProvince);
        parcel.writeString(mPostalCode);
        parcel.writeString(mCountry);
        parcel.writeString(mMobilePhone);
        parcel.writeString(mHomePhone);
        parcel.writeString(mWorkPhone);
        parcel.writeString(mComments);
        parcel.writeString(mPreferredNotice);
        parcel.writeString(mMaxCyclists);
        parcel.writeString(mNotCurrentlyAvailable);
        parcel.writeString(mBed);
        parcel.writeString(mBikeshop);
        parcel.writeString(mCampground);
        parcel.writeString(mFood);
        parcel.writeString(mKitchenUse);
        parcel.writeString(mLaundry);
        parcel.writeString(mLawnspace);
        parcel.writeString(mMotel);
        parcel.writeString(mSag);
        parcel.writeString(mShower);
        parcel.writeString(mStorage);
        parcel.writeString(mLatitude);
        parcel.writeString(mLongitude);
        parcel.writeString(mLogin);
        parcel.writeString(mCreated);
        parcel.writeInt(mId);
        parcel.writeString(mLanguagesSpoken);
        parcel.writeString(mPicture);
        parcel.writeString(mProfilePictureSmall);
        parcel.writeString(mProfilePictureLarge);
    }

    public void readFromParcel(Parcel source) {
        mName = source.readString();
        mFullname = source.readString();
        mStreet = source.readString();
        mAdditional = source.readString();
        mCity = source.readString();
        mProvince = source.readString();
        mPostalCode = source.readString();
        mCountry = source.readString();
        mMobilePhone = source.readString();
        mHomePhone = source.readString();
        mWorkPhone = source.readString();
        mComments = source.readString();
        mPreferredNotice = source.readString();
        mMaxCyclists = source.readString();
        mNotCurrentlyAvailable = source.readString();
        mBed = source.readString();
        mBikeshop = source.readString();
        mCampground = source.readString();
        mFood = source.readString();
        mKitchenUse = source.readString();
        mLaundry = source.readString();
        mLawnspace = source.readString();
        mMotel = source.readString();
        mSag = source.readString();
        mShower = source.readString();
        mStorage = source.readString();
        mLatitude = source.readString();
        mLongitude = source.readString();
        mLogin = source.readString();
        mCreated = source.readString();
        mId = source.readInt();
        mLanguagesSpoken = source.readString();
        mPicture = source.readString();
        mProfilePictureSmall = source.readString();
        mProfilePictureLarge = source.readString();
    }

    public void readFromJson(JSONObject json) throws JSONException {
        mId = json.optInt("uid");
        if (!json.isNull("name")) {
            mName = json.optString("name");
        }
        if (!json.isNull("fullname")) {
            mFullname = json.optString("fullname");
        }
        if (!json.isNull("street")) {
            mStreet = json.optString("street");
        }
        if (!json.isNull("additional")) {
            mAdditional = json.optString("additional");
        }
        if (!json.isNull("city")) {
            mCity = json.optString("city");
        }
        if (!json.isNull("province")) {
            mProvince = json.optString("province");
        }
        if (!json.isNull("postal_code")) {
            mPostalCode = json.optString("postal_code");
        }
        if (!json.isNull("country")) {
            mCountry = json.optString("country");
        }
        if (!json.isNull("mobilephone")) {
            mMobilePhone = json.optString("mobilephone");
        }
        if (!json.isNull("homephone")) {
            mHomePhone = json.optString("homephone");
        }
        if (!json.isNull("workphone")) {
            mWorkPhone = json.optString("workphone");
        }
        if (!json.isNull("comments")) {
            mComments = json.optString("comments");
        }
        if (!json.isNull("preferred_notice")) {
            mPreferredNotice = json.optString("preferred_notice");
        }
        if (!json.isNull("maxcyclists")) {
            mMaxCyclists = json.optString("maxcyclists");
        }
        if (!json.isNull("notcurrentlyavailable")) {
            mNotCurrentlyAvailable = json.optString("notcurrentlyavailable");
        }
        if (!json.isNull("bed")) {
            mBed = json.optString("bed");
        }
        if (!json.isNull("bikeshop")) {
            mBikeshop = json.optString("bikeshop");
        }
        if (!json.isNull("campground")) {
            mCampground = json.optString("campground");
        }
        if (!json.isNull("food")) {
            mFood = json.optString("food");
        }
        if (!json.isNull("kitchenuse")) {
            mKitchenUse = json.optString("kitchenuse");
        }
        if (!json.isNull("laundry")) {
            mLaundry = json.optString("laundry");
        }
        if (!json.isNull("lawnspace")) {
            mLawnspace = json.optString("lawnspace");
        }
        if (!json.isNull("motel")) {
            mMotel = json.optString("motel");
        }
        if (!json.isNull("sag")) {
            mSag = json.optString("sag");
        }
        if (!json.isNull("shower")) {
            mShower = json.optString("shower");
        }
        if (!json.isNull("storage")) {
            mStorage = json.optString("storage");
        }
        if (!json.isNull("latitude")) {
            mLatitude = json.optString("latitude");
        }
        if (!json.isNull("longitude")) {
            mLongitude = json.optString("longitude");
        }
        if (!json.isNull("login")) {
            mLogin = json.optString("login");
        }
        if (!json.isNull("created")) {
            mCreated = json.optString("created");
        }
        if (!json.isNull("languagesspoken")) {
            mLanguagesSpoken = json.optString("languagesspoken");
        }
        if (!json.isNull("picture")) {
            mPicture = json.optString("picture");
        }
        if (!json.isNull("profile_image_profile_picture")) {
            mProfilePictureSmall = json.optString("profile_image_profile_picture");
        }
        if (!json.isNull("profile_image_mobile_profile_photo_std")) {
            mProfilePictureLarge = json.optString("profile_image_mobile_profile_photo_std");
        }
    }

}
