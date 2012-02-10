package fi.bitrite.android.ws.model;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONObject;

import android.content.Context;
import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;

public class TestHostFactory {

	public static Host getHostFromJson() {
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
