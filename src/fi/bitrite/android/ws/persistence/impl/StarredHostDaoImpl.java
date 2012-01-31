package fi.bitrite.android.ws.persistence.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.persistence.StarredHostDao;

public class StarredHostDaoImpl implements StarredHostDao {

	private Host testHost = getHostFromJson();
	
	public List<Host> getAll() {
		Host [] starredHosts = {  testHost, testHost };
		return Arrays.asList(starredHosts);
	}

	public Host get() {
		return testHost;
	}

	private Host getHostFromJson() {
		Context context = WSAndroidApplication.getAppContext();
		InputStream inputStream = context.getResources().openRawResource(
				R.raw.jstaffans);

		InputStreamReader inputreader = new InputStreamReader(inputStream);
		BufferedReader buffreader = new BufferedReader(inputreader);
		
		String line;
		StringBuilder json = new StringBuilder();

		try {
			while ((line = buffreader.readLine()) != null) {
				json.append(line);
				json.append('\n');
			}
			return Host.CREATOR.parse(new JSONObject(json.toString()));
		} catch (Exception e) {
			return null;
		}
	}
}
