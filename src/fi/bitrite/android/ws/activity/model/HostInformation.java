package fi.bitrite.android.ws.activity.model;

import android.content.Intent;
import android.os.Bundle;
import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.persistence.StarredHostDao;

import java.util.ArrayList;

/**
 * User: johannes
 * Date: 08.03.2013
 */
public class HostInformation {
    public static final int NO_ID = 0;

    private Host host;

    private final ArrayList<Feedback> feedback;
    private final int id;
    private boolean starred;

    public HostInformation(Host host, ArrayList<Feedback> feedback, int id, boolean starred) {
        this.feedback = feedback;
        this.host = host;
        this.id = id;
        this.starred = starred;
    }

    public static HostInformation fromSavedInstanceState(Bundle savedInstanceState, StarredHostDao dao) {
        final Host host = savedInstanceState.getParcelable("host");
        final ArrayList<Feedback> feedback = savedInstanceState.getParcelableArrayList("feedback");
        final int id = savedInstanceState.getInt("id");
        final boolean starred = dao.isHostStarred(id, host.getName());

        return new HostInformation(host, feedback, id, starred);
    }

    public static HostInformation fromIntent(Intent i, StarredHostDao dao) {
        final Host host = (Host) i.getParcelableExtra("host");
        final int id = i.getIntExtra("id", NO_ID);
        final ArrayList<Feedback> feedback = i.getParcelableArrayListExtra("feedback");
        final boolean starred = dao.isHostStarred(id, host.getName());

        return new HostInformation(host, feedback, id, starred);
    }

    public ArrayList<Feedback> getFeedback() {
        return (feedback == null) ? new ArrayList<Feedback>() : feedback;
    }

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public int getId() {
        return id;
    }

    public boolean isStarred() {
        return starred;
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("host", host);
        outState.putInt("id", id);
        outState.putParcelableArrayList("feedback", feedback);
    }

    public void toggleStarred() {
        starred = !starred;
    }
}
