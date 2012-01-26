package fi.bitrite.android.ws.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Host implements Parcelable {

    public static final Parcelable.Creator<Host> CREATOR = new Parcelable.Creator<Host>() {
        public Host createFromParcel(Parcel in) {
            return new Host(in);
        }

        public Host[] newArray(int size) {
            return new Host[size];
        }
    };
	
	private String fullname;
	private String comments;

	public Host(String fullname, String comments) {
		this.fullname = fullname;
		this.comments = comments;
	}
	
	public Host(Parcel in) {
		this.fullname = in.readString();
		this.comments = in.readString();
	}
	
	public String getFullname() {
		return fullname;
	}
	public void setFullname(String fullname) {
		this.fullname = fullname;
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	
	public int describeContents() {
		return 0;
	}
	
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(fullname);
		dest.writeString(comments);
	}
}
