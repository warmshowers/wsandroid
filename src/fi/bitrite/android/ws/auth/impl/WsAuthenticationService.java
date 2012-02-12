package fi.bitrite.android.ws.auth.impl;

import fi.bitrite.android.ws.auth.AuthenticationService;

/**
 * Responsible for authenticating the user against the WarmShowers web service.
 * Authentication is done using a session cookie which is stored here in the service.
 */
public class WsAuthenticationService implements AuthenticationService {

	boolean authenticated;
	
	public boolean isAuthenticated() {
		return authenticated;
	}

	public void authenticate(String username, String password) {
		// TODO Auto-generated method stub
		
	}

}
