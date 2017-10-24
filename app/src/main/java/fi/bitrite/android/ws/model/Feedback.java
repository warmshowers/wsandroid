package fi.bitrite.android.ws.model;

import android.annotation.SuppressLint;
import android.os.Parcel;

import com.yelp.parcelgen.JsonParser.DualCreator;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Feedback extends _Feedback implements Comparable<Feedback> {

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

    @Override
    public int compareTo(Feedback other) {
        return (int) (other.getHostingDateStr().compareTo(this.getHostingDateStr()));
    }

    @SuppressLint("SimpleDateFormat")
    private static SimpleDateFormat feedbackDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public Date getHostingDate() {
        try {
            return feedbackDateFormat.parse(getHostingDateStr());
        } catch (ParseException e) {
            return null;
        }
    }
}
