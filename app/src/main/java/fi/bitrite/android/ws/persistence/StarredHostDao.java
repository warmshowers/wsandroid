package fi.bitrite.android.ws.persistence;

import fi.bitrite.android.ws.model.Feedback;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.HostBriefInfo;

import java.util.ArrayList;
import java.util.List;

public interface StarredHostDao {

    public void insert(int id, String name, Host host, List<Feedback> feedback);

    public Host getHost(int id);

    public ArrayList<Feedback> getFeedback(int id, String name);

    public List<HostBriefInfo> getAllBrief();

    public void delete(int id, String name);

    public void update(int id, String name, Host host, List<Feedback> feedback);

    public boolean isHostStarred(int id);

    public void open();

    public void close();
}
