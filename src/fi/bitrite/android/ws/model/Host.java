package fi.bitrite.android.ws.model;

public class Host {

	private String fullname;
	private String comments;

	public Host(String fullname, String comments) {
		this.fullname = fullname;
		this.comments = comments;
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
	
}
