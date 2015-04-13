package fi.bitrite.android.ws.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.HashMap;
import java.util.Locale;

import fi.bitrite.android.ws.R;
import fi.bitrite.android.ws.WSAndroidApplication;

/**
 * Uses a singleton design pattern to provide English (API) versions of array elements for translated
 * arrays
 */
public class ArrayTranslator {
    private static ArrayTranslator instance;
    Context mContext = WSAndroidApplication.getAppContext();
    HashMap<String, String> ratingTranslator = new HashMap<String, String>();
    HashMap<String, String> hostGuestTranslator = new HashMap<String, String>();
    String[] englishHostGuestOptions = getEnglishStringArray(R.array.feedback_how_we_met_options);
    String[] englishRatingOptions = getEnglishStringArray(R.array.feedback_overall_experience_options);

    // Private so can only be instantiated here
    private ArrayTranslator() {
        String[] localizedRatingOptions = mContext.getResources().getStringArray(R.array.feedback_overall_experience_options);

        if (localizedRatingOptions.length == englishRatingOptions.length && localizedRatingOptions.length > 0) {
            for (int i = 0; i < englishRatingOptions.length; i++) {
                String english = englishRatingOptions[i];
                String local = localizedRatingOptions[i];
                ratingTranslator.put(english, local);
            }
        }

        String[] localizedHostGuestOptions = mContext.getResources().getStringArray(R.array.feedback_how_we_met_options);

        if (localizedHostGuestOptions.length == englishHostGuestOptions.length && localizedHostGuestOptions.length > 0) {
            for (int i = 0; i < englishHostGuestOptions.length; i++) {
                String english = englishHostGuestOptions[i];
                String local = localizedHostGuestOptions[i];
                hostGuestTranslator.put(english, local);
            }
        }
    }

    private String[] getEnglishStringArray(int resId) {
        Resources res = mContext.getResources();
        Configuration conf = res.getConfiguration();
        Locale savedLocale = conf.locale;
        conf.locale = new Locale("en");
        res.updateConfiguration(conf, null); // second arg null means don't change
        String[] englishArray = res.getStringArray(resId);
        conf.locale = savedLocale;
        res.updateConfiguration(conf, null);
        return englishArray;
    }

    public static void initInstance()
    {
        if (instance == null)
        {
            // Create the instance
            instance = new ArrayTranslator();
        }
    }

    public static ArrayTranslator getInstance() {
        if (instance == null) {
            initInstance();
        }
        return instance;
    }

    public String translateRating(String rating) {
        if (ratingTranslator.containsKey(rating)) {
            return ratingTranslator.get(rating);
        }
        else {
            return rating;
        }
    }

    public String translateHostGuest(String hostOrGuest) {
        if (hostGuestTranslator.containsKey(hostOrGuest)) {
            return hostGuestTranslator.get(hostOrGuest);
        }
        else {
            return hostOrGuest;
        }
    }

    public String getEnglishRating(int position) {
        if (englishRatingOptions.length > position) {
            return englishRatingOptions[position];
        }
        return "";
    }

    public String getEnglishHostGuestOption(int position) {
        if (englishHostGuestOptions.length > position) {
            return englishHostGuestOptions[position];
        }
        return "";
    }

}
