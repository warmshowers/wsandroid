package fi.bitrite.android.ws.model;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;

import com.yelp.parcelgen.JsonParser.DualCreator;


public class Host extends _Host {

	public static final DualCreator<Host> CREATOR = new DualCreator<Host>() {

		public Host[] newArray(int size) {
			return new Host[size];
		}

		public Host createFromParcel(Parcel source) {
			Host object = new Host();
			object.readFromParcel(source);
			return object;
		}

		@Override
		public Host parse(JSONObject obj) throws JSONException {
			Host newInstance = new Host();
			newInstance.readFromJson(obj);
			return newInstance;
		}
	};

}
