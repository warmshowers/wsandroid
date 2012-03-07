package fi.bitrite.android.ws.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.yelp.parcelgen.JsonParser.DualCreator;


/**
 * Used for passing search results. More in-depth information is handled by the Host object.
 */
public class HostBriefInfo implements Parcelable {

	private int id;
	private String name;
	private String fullname;
	private String location;
	private String comments;
	private String longitude;
	private String latitude;
	
	public HostBriefInfo(int id, String name, String fullname, String location, String comments) {
		this.id = id;
		this.name = name;
		this.fullname = fullname;
		this.location = location;
		this.comments = comments;
	}

	public HostBriefInfo(int id, Host host) {
		this.id = id;
		this.name = host.getName();
		this.fullname = host.getFullname();
		this.location = host.getCity() + ", " + host.getProvince() + ", " + host.getCountry().toUpperCase();
		this.comments = host.getComments();
	}
	
	public HostBriefInfo() {
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public String getFullname() {
		return fullname;
	}

	public String getLocation() {
		return location;
	}

	public String getComments() {
		return comments;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public int describeContents() {
		// TODO Auto-generated method stub
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
		dest.writeInt(id);
		dest.writeString(name);
		dest.writeString(fullname);
		dest.writeString(location);
		dest.writeString(comments);
		dest.writeString(longitude);
		dest.writeString(latitude);
	}
	
	public void readFromParcel(Parcel src) {
		id = src.readInt();
		name = src.readString();
		fullname = src.readString();
		location = src.readString();
		comments = src.readString();
		longitude = src.readString();
		latitude = src.readString();
	}	
}
