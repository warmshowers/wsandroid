package fi.bitrite.android.ws.auth.http;

public interface HttpAuthenticationService {
	
	public void authenticate();
	
	public void authenticate(String username, String password);

	public boolean isAuthenticated();

}
