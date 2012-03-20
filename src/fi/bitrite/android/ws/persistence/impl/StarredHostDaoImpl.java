package fi.bitrite.android.ws.persistence.impl;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;

import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.persistence.StarredHostDao;

public class StarredHostDaoImpl implements StarredHostDao {

	private SQLiteDatabase database;
	private DbHelper dbHelper;

	public void open() throws SQLException {
		if (dbHelper == null) {
			dbHelper = new DbHelper();
		}
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		database.close();
	}

	public void insert(int id, String name, Host host) {
		ContentValues values = new ContentValues();
		values.put(DbHelper.COLUMN_ID, id);
		values.put(DbHelper.COLUMN_NAME, name);
		values.put(DbHelper.COLUMN_UPDATED, DateFormat.getDateInstance().format(new Date()));

		Gson gson = new Gson();
		String details = gson.toJson(host);
		values.put(DbHelper.COLUMN_DETAILS, details);

		database.insert(DbHelper.TABLE_HOSTS, null, values);
	}

	public Host get(int id, String name) {
		Cursor cursor;
		
		if (id > 0) {
			cursor = database.query(DbHelper.TABLE_HOSTS, new String[] { DbHelper.COLUMN_ID, DbHelper.COLUMN_DETAILS,
					DbHelper.COLUMN_UPDATED }, DbHelper.COLUMN_ID + " = " + id, null, null, null, null);
		} else {
			cursor = database.query(DbHelper.TABLE_HOSTS, new String[] { DbHelper.COLUMN_NAME, DbHelper.COLUMN_DETAILS,
					DbHelper.COLUMN_UPDATED }, DbHelper.COLUMN_NAME + " = '" + name + "'", null, null, null, null);
		}

		if (cursor.getCount() == 0) {
			cursor.close();
			return null;
		}

		cursor.moveToFirst();
		Host host = cursorToHost(cursor);
		
		cursor.close();
		return host;
	}
	
	private Host cursorToHost(Cursor cursor) {
		String json = cursor.getString(1);
		Gson gson = new Gson();
		try {
			Host host = gson.fromJson(json, Host.class);
			host.setUpdated(cursor.getString(2));
			return host;
		}

		catch (Exception e) {
			throw new PersistenceException("Could not load starred host details");
		}
	}

	public List<HostBriefInfo> getAllBrief() {
		Cursor cursor = database.query(DbHelper.TABLE_HOSTS, new String[] { DbHelper.COLUMN_ID,
				DbHelper.COLUMN_DETAILS, DbHelper.COLUMN_UPDATED }, null, null, null, null, null);

		if (cursor.getCount() == 0) {
			cursor.close();
			return Collections.emptyList();
		}

		List<HostBriefInfo> hosts = new ArrayList<HostBriefInfo>();

		while (!cursor.isLast()) {
			cursor.moveToNext();
			Host host = cursorToHost(cursor);
			hosts.add(new HostBriefInfo(cursor.getInt(0), host));
		}

		cursor.close();
		
		return hosts;
	}

	public void delete(int id, String name) {
		if (id > 0) {
			database.delete(DbHelper.TABLE_HOSTS, DbHelper.COLUMN_ID + " = " + id, null);
		} else {
			database.delete(DbHelper.TABLE_HOSTS, DbHelper.COLUMN_NAME + " = '" + name + "'", null);
		}
	}

	public void update(int id, String name, Host host) {
		delete(id, name);
		insert(id, name, host);
	}
	
	public boolean isHostStarred(int id, String name) {
		return (get(id, name) != null);
	}
}
