package fi.bitrite.android.ws.model;

import android.os.Parcel;
import org.json.JSONException;
import org.json.JSONObject;
import com.yelp.parcelgen.JsonParser.DualCreator;


public class Feedback extends _Feedback {

	public static final DualCreator<Feedback> CREATOR = new DualCreator<Feedback>() {

		public Feedback[] newArray(int size) {
			return new Feedback[size];
		}

		public Feedback createFromParcel(Parcel source) {
			Feedback object = new Feedback();
			object.readFromParcel(source);
			return object;
		}

		@Override
		public Feedback parse(JSONObject obj) throws JSONException {
			Feedback newInstance = new Feedback();
			newInstance.readFromJson(obj);
			return newInstance;
		}
	};

}
