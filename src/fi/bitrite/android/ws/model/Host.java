package fi.bitrite.android.ws.model;

import org.json.JSONException;
import org.json.JSONObject;

import roboguice.util.Strings;
import android.os.Parcel;

import com.yelp.parcelgen.JsonParser.DualCreator;

public class Host extends _Host {
	
	private String mUpdated;

	public static Host createFromBriefInfo(HostBriefInfo briefInfo) {
		Host host = new Host();
		host.mName = briefInfo.getName();
		host.mFullname = briefInfo.getFullname();
		host.mComments = briefInfo.getComments();
		host.mLongitude = briefInfo.getLongitude();
		host.mLatitude = briefInfo.getLatitude();
		return host;
	}

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

	public String getLocation() {
		StringBuilder sb = new StringBuilder();

		if (!Strings.isEmpty(getStreet())) {
			sb.append(getStreet()).append("\n");
		}

		if (!Strings.isEmpty(getAdditional())) {
			sb.append(getAdditional()).append("\n");
		}

		sb.append(getPostalCode()).append(", ").append(getCity()).append(", ").append(getProvince());
		
		if (!Strings.isEmpty(getCountry())) {
			sb.append(", ").append(getCountry().toUpperCase());
		}
		
		sb.append("\nLat: ").append(getLatitude()).append("\n");
		sb.append("Lon: ").append(getLongitude());
		
		return sb.toString();
	}

	public String getServices() {
		StringBuilder sb = new StringBuilder();

		if (hasService(getShower()))
			sb.append("Shower\n");
		if (hasService(getFood()))
			sb.append("Food\n");
		if (hasService(getBed()))
			sb.append("Bed\n");
		if (hasService(getLaundry()))
			sb.append("Laundry\n");
		if (hasService(getStorage()))
			sb.append("Storage\n");
		if (hasService(getKitchenUse()))
			sb.append("Use of kitchen\n");
		if (hasService(getLawnspace()))
			sb.append("Lawn space (for camping)\n");
		if (hasService(getSag()))
			sb.append("SAG (vehicle support)\n");

		return sb.toString();
	}

	private boolean hasService(String service) {
		return !Strings.isEmpty(service) && service.equals("1");
	}

	public boolean isNotCurrentlyAvailable() {
		return getNotCurrentlyAvailable().equals("1");
	}

	public String getUpdated() {
		return mUpdated;
	}

	public void setUpdated(String updated) {
		mUpdated = updated;
	}
}
