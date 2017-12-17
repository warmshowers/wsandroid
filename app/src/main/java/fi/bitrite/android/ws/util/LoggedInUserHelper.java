package fi.bitrite.android.ws.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.inject.Inject;
import javax.inject.Singleton;

import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.model.Host;
import io.reactivex.subjects.BehaviorSubject;

@Singleton
public class LoggedInUserHelper {
    public class Helper {
        @Nullable public final Host user;

        Helper(@Nullable Host user) {
            this.user = user;
        }

        public boolean hasUser() {
            return user != null;
        }
    }

    private final SharedPreferencesStore mStore;
    private final BehaviorSubject<Helper> mLoggedInUser;

    @Inject
    public LoggedInUserHelper() {
         mStore = new SharedPreferencesStore(WSAndroidApplication.getAppContext());

         Host loggedInUser = mStore.load();
         mLoggedInUser = BehaviorSubject.createDefault(new Helper(loggedInUser));
    }

    @Nullable
    public Host get() {
        return mLoggedInUser.getValue().user;
    }

    @NonNull
    public BehaviorSubject<Helper> getSubject() {
        return mLoggedInUser;
    }

    public void set(@Nullable Host loggedInUser) {
        mStore.save(loggedInUser);

        mLoggedInUser.onNext(new Helper(loggedInUser));
    }


    // TODO(saemy): Eventually remove this and store the user in the DB.
    static class SharedPreferencesStore {
        private final SharedPreferences mPrefs;

        SharedPreferencesStore(Context context) {
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        }

        Host load() {
            Gson gson = new Gson();
            String json = mPrefs.getString("member_info", "");
            try {
                return gson.fromJson(json, Host.class);
            } catch (JsonSyntaxException e) {
                // We failed, will return null
                return null;
            }
        }

        void save(Host user) {
            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            Gson gson = new Gson();
            String json = gson.toJson(user);
            prefsEditor.putString("member_info", json);
            prefsEditor.apply();
        }

        void remove() {
            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            prefsEditor.remove("member_info");
            prefsEditor.apply();
        }
    }
}
