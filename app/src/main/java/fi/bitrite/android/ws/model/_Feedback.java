package fi.bitrite.android.ws.model;

import android.os.Parcel;
import android.os.Parcelable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.json.JSONException;
import org.json.JSONObject;

/** Automatically generated Parcelable implementation for _Feedback.
 *    DO NOT MODIFY THIS FILE MANUALLY! IT WILL BE OVERWRITTEN THE NEXT TIME
 *    _Feedback's PARCELABLE DESCRIPTION IS CHANGED.
 */
/* package */ abstract class _Feedback implements Parcelable {

    protected String mId;
    protected String mUid;
    protected String mFullname;
    protected String mName;
    protected String mBody;
    protected String mGuestOrHostStr;
    protected String mHostingDateStr;
    protected String mRatingStr;

    protected _Feedback(String id, String uid, String fullname, String name, String body, String guestOrHostStr, String hostingDateStr, String ratingStr) {
        this();
        mId = id;
        mUid = uid;
        mFullname = fullname;
        mName = name;
        mBody = body;
        mGuestOrHostStr = guestOrHostStr;
        mHostingDateStr = hostingDateStr;
        mRatingStr = ratingStr;
    }

    protected _Feedback() {
        super();
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }

        if (object == this) {
            return true;
        }

        if (object.getClass() != getClass()) {
            return false;
        }

        _Feedback that = (_Feedback) object;

        return new EqualsBuilder()
                .append(this.mId, that.mId)
                .append(this.mUid, that.mUid)
                .append(this.mFullname, that.mFullname)
                .append(this.mName, that.mName)
                .append(this.mBody, that.mBody)
                .append(this.mGuestOrHostStr, that.mGuestOrHostStr)
                .append(this.mHostingDateStr, that.mHostingDateStr)
                .append(this.mRatingStr, that.mRatingStr)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(mId)
                .append(mUid)
                .append(mFullname)
                .append(mName)
                .append(mBody)
                .append(mGuestOrHostStr)
                .append(mHostingDateStr)
                .append(mRatingStr)
                .toHashCode();
    }

    public String getId() {
         return mId;
    }
    public String getUid() {
         return mUid;
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
    public String getGuestOrHostStr() {
         return mGuestOrHostStr;
    }
    public String getHostingDateStr() {
         return mHostingDateStr;
    }
    public String getRatingStr() {
         return mRatingStr;
    }


    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeValue(mId);
        parcel.writeValue(mUid);
        parcel.writeValue(mFullname);
        parcel.writeValue(mName);
        parcel.writeValue(mBody);
        parcel.writeValue(mGuestOrHostStr);
        parcel.writeValue(mHostingDateStr);
        parcel.writeValue(mRatingStr);
    }

    public void readFromParcel(Parcel source) {
        mId = (String) source.readValue(String.class.getClassLoader());
        mUid = (String) source.readValue(String.class.getClassLoader());
        mFullname = (String) source.readValue(String.class.getClassLoader());
        mName = (String) source.readValue(String.class.getClassLoader());
        mBody = (String) source.readValue(String.class.getClassLoader());
        mGuestOrHostStr = (String) source.readValue(String.class.getClassLoader());
        mHostingDateStr = (String) source.readValue(String.class.getClassLoader());
        mRatingStr = (String) source.readValue(String.class.getClassLoader());
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
            mGuestOrHostStr = json.optString("field_guest_or_host");
        }
        if (!json.isNull("field_hosting_date")) {
            mHostingDateStr = json.optString("field_hosting_date");
        }
        if (!json.isNull("field_rating")) {
            mRatingStr = json.optString("field_rating");
        }
    }

}
