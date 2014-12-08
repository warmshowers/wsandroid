package fi.bitrite.android.ws.model;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Automatically generated Parcelable implementation for _Feedback.
 * DO NOT MODIFY THIS FILE MANUALLY! IT WILL BE OVERWRITTEN THE NEXT TIME
 * _Feedback's PARCELABLE DESCRIPTION IS CHANGED.
 */
/* package */ abstract class _Feedback implements Parcelable {

    protected String mId;
    protected String mUid;
    protected String mFullname;
    protected String mName;
    protected String mBody;
    protected String mGuestOrHost;
    protected String mRating;
    protected long mHostingDate;

    protected _Feedback(String id, String uid, String fullname, String name, String body, String guestOrHost, String rating, long hostingDate) {
        this();
        mId = id;
        mUid = uid;
        mFullname = fullname;
        mName = name;
        mBody = body;
        mGuestOrHost = guestOrHost;
        mRating = rating;
        mHostingDate = hostingDate;
    }

    protected _Feedback() {
        super();
    }

    public String getId() {
        return mId;
    }

    public String getFullname() {
        return mFullname;
    }

    public String getName() {
        return mName;
    }

    public String getBody() {
        return mBody;
    }

    public String getGuestOrHost() {
        return mGuestOrHost;
    }

    public String getRating() {
        return mRating;
    }

    public long getHostingDate() {
        return mHostingDate;
    }


    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mId);
        parcel.writeString(mUid);
        parcel.writeString(mFullname);
        parcel.writeString(mName);
        parcel.writeString(mBody);
        parcel.writeString(mGuestOrHost);
        parcel.writeString(mRating);
        parcel.writeLong(mHostingDate);
    }

    public void readFromParcel(Parcel source) {
        mId = source.readString();
        mUid = source.readString();
        mFullname = source.readString();
        mName = source.readString();
        mBody = source.readString();
        mGuestOrHost = source.readString();
        mRating = source.readString();
        mHostingDate = source.readLong();
    }

    public void readFromJson(JSONObject json) throws JSONException {
        if (!json.isNull("nid")) {
            mId = json.optString("nid");
        }
        if (!json.isNull("uid_1")) {
            mUid = json.optString("uid_1");
        }
        if (!json.isNull("fullname")) {
            mFullname = json.optString("fullname");
        }
        if (!json.isNull("name")) {
            mName = json.optString("name");
        }
        if (!json.isNull("body")) {
            mBody = json.optString("body");
        }
        if (!json.isNull("field_guest_or_host_value")) {
            mGuestOrHost = json.optString("field_guest_or_host_value");
        }
        if (!json.isNull("field_rating_value")) {
            mRating = json.optString("field_rating_value");
        }
        mHostingDate = json.optLong("field_hosting_date_value");
    }

}
