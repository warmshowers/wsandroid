package fi.bitrite.android.ws.model;


/**
 * Used for passing search results. More in-depth information is handled by the Host object.
 */
public class HostBriefInfo {

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

	public String getJson() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
