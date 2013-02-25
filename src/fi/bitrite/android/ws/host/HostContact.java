package fi.bitrite.android.ws.host;

public interface HostContact {
    void send(int id, String subject, String message);
}
