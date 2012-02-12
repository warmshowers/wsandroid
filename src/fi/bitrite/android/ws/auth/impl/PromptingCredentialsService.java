package fi.bitrite.android.ws.auth.impl;

import roboguice.util.Strings;

import com.google.inject.Singleton;

import fi.bitrite.android.ws.auth.CredentialsProvider;
import fi.bitrite.android.ws.auth.CredentialsReceiver;
import fi.bitrite.android.ws.auth.CredentialsService;
import fi.bitrite.android.ws.auth.NoCredentialsException;

@Singleton
public class PromptingCredentialsService implements CredentialsService, CredentialsProvider {

	String username;
	
	String password;
	
	public void applyStoredCredentials(CredentialsReceiver receiver) {
		if (Strings.isEmpty(username) || Strings.isEmpty(password)) {
			throw new NoCredentialsException();
		}
		
		receiver.applyCredentials(this);
	}

	public void storeCredentials(CredentialsProvider credentials) {
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
