package fi.bitrite.android.ws.auth.impl;

import roboguice.util.Strings;

import com.google.inject.Singleton;

import fi.bitrite.android.ws.auth.CredentialsProvider;
import fi.bitrite.android.ws.auth.CredentialsReceiver;
import fi.bitrite.android.ws.auth.CredentialsService;
import fi.bitrite.android.ws.auth.NoCredentialsException;

@Singleton
public class ExceptionalCredentialsService implements CredentialsService, CredentialsProvider {

	String username;
	
	String password;
	
	public void sendStoredCredentials(CredentialsReceiver receiver) {
		if (!hasStoredCredentials()) {
			throw new NoCredentialsException();
		}
		
		receiver.applyCredentials(this);
	}
	
	public boolean hasStoredCredentials() {
		return !(Strings.isEmpty(username) || Strings.isEmpty(password));
	}


	public void applyCredentials(CredentialsProvider credentials) {
		username = credentials.getUsername();
		password = credentials.getPassword();
	}
	
	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}
	
	
}
