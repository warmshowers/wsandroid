package fi.bitrite.android.ws.auth;

public interface CredentialsService extends CredentialsProvider {

	void applyStoredCredentials(CredentialsReceiver receiver);

	void storeCredentials(CredentialsProvider provider);

}
