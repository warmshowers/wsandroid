package fi.bitrite.android.ws.persistence.impl;

import java.util.Collections;
import java.util.List;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.persistence.StarredHostDao;

public class StarredHostDaoImpl implements StarredHostDao {

	private SQLiteDatabase database;
	private DbHelper dbHelper;
	
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}
	
	public void close() {
		database.close();
	}

	public Host get(int id) {
		Cursor cursor = database.query(DbHelper.TABLE_HOSTS, new String[] { DbHelper.COLUMN_DETAILS },
				DbHelper.COLUMN_ID + " = " + id, null, null, null, null);
		cursor.moveToFirst();
		return cursorToHost(cursor);
		
	}
	
	private Host cursorToHost(Cursor cursor) {
		Host host = new Host();
		
		return host;
		
	}

	public List<HostBriefInfo> getAll() {
//		HostBriefInfo [] starredHosts = {  new HostBriefInfo(123, testHost), new HostBriefInfo(123, testHost) };
//		return Arrays.asList(starredHosts);
		return Collections.emptyList();
	}

	public boolean isHostStarred(int id, String name) {
		return false;
	}
	
}
