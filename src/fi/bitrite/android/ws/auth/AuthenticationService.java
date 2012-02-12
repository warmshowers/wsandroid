package fi.bitrite.android.ws.auth;

public interface AuthenticationService {
	
	public void authenticate(String username, String password);
	
	public boolean isAuthenticated();

}
