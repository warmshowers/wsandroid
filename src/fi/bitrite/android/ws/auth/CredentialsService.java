package fi.bitrite.android.ws.auth;

public interface CredentialsService extends CredentialsProvider, CredentialsReceiver {

	boolean hasStoredCredentials();
	
	void sendStoredCredentials(CredentialsReceiver receiver);

	void applyCredentials(CredentialsProvider provider);

}
