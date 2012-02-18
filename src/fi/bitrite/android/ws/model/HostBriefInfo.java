package fi.bitrite.android.ws.model;


/**
 * Used for passing search results. More in-depth information is handled by the Host object.
 */
public class HostBriefInfo {

	private String name;
	private String fullname;
	private String location;
	private String comments;
	
	public HostBriefInfo(String name, String fullname, String location, String comments) {
		this.name = name;
		this.fullname = fullname;
		this.location = location;
		this.comments = comments;
	}

	public HostBriefInfo(Host host) {
		this.name = host.getName();
		this.fullname = host.getFullname();
		this.location = host.getCity() + ", " + host.getProvince() + ", " + host.getCountry().toUpperCase();
		this.comments = host.getComments();
	}

	public Object getName() {
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
}
