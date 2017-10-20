package fi.bitrite.android.ws.model;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
    protected String mHostingDateString;
    protected SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    protected _Feedback(String id, String uid, String fullname, String name, String body, String guestOrHost, String rating, long hostingDate) {
        this();
        mId = id;
        mUid = uid;
        mFullname = fullname;
        mName = name;
        mBody = body;
        mGuestOrHost = guestOrHost;
        mRating = rating;
        mHostingDateString = new Date(hostingDate).toString();
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

    public Date getHostingDate() {
        try {
            return formatter.parse(mHostingDateString);
        } catch (ParseException e) {
            return new Date();
        }
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
        parcel.writeLong(getHostingDate().getTime());
    }

    public void readFromParcel(Parcel source) {
        mId = source.readString();
        mUid = source.readString();
        mFullname = source.readString();
        mName = source.readString();
        mBody = source.readString();
        mGuestOrHost = source.readString();
        mRating = source.readString();
        mHostingDateString = new Date(source.readLong()).toString();
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
        if (!json.isNull("field_guest_or_host")) {
            mGuestOrHost = json.optString("field_guest_or_host");
        }
        if (!json.isNull("field_rating")) {
            mRating = json.optString("field_rating");
        }
        mHostingDateString = json.optString("field_hosting_date");
    }

}
