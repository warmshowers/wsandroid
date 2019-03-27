package fi.bitrite.android.ws.persistence;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.AppScope;
import fi.bitrite.android.ws.model.SimpleUser;
import fi.bitrite.android.ws.model.User;
import fi.bitrite.android.ws.persistence.converters.DateConverter;
import fi.bitrite.android.ws.persistence.db.AppDatabase;

@AppScope
public class UserDao extends Dao {
    private final static String TABLE_NAME = "user";

    private final static String[] DEFAULT_COLUMNS = {
            "id", "name", "fullname", "street", "additional_address", "city", "province",
            "postal_code", "country_code", "mobile_phone", "home_phone", "work_phone", "comments",
            "preferred_notice", "max_cyclists_count", "distance_to_motel", "distance_to_campground",
            "distance_to_bikeshop", "has_storage", "has_shower", "has_kitchen", "has_lawnspace",
            "has_sag", "has_bed", "has_laundry", "has_food", "last_access", "created",
            "currently_available", "spoken_languages", "latitude", "longitude",
            "profile_picture_small", "profile_picture_large",
    };

    private final static int COL_IDX_ID = 0;
    private final static int COL_IDX_NAME = 1;
    private final static int COL_IDX_FULLNAME = 2;
    private final static int COL_IDX_STREET = 3;
    private final static int COL_IDX_ADDITIONAL_ADDRESS = 4;
    private final static int COL_IDX_CITY = 5;
    private final static int COL_IDX_PROVINCE = 6;
    private final static int COL_IDX_POSTAL_CODE = 7;
    private final static int COL_IDX_COUNTRY_CODE = 8;
    private final static int COL_IDX_MOBILE_PHONE = 9;
    private final static int COL_IDX_HOME_PHONE = 10;
    private final static int COL_IDX_WORK_PHONE = 11;
    private final static int COL_IDX_COMMENTS = 12;
    private final static int COL_IDX_PREFERRED_NOTICE = 13;
    private final static int COL_IDX_MAX_CYCLISTS_COUNT = 14;
    private final static int COL_IDX_DISTANCE_TO_MOTEL = 15;
    private final static int COL_IDX_DISTANCE_TO_CAMPGROUND = 16;
    private final static int COL_IDX_DISTANCE_TO_BIKESHOP = 17;
    private final static int COL_IDX_HAS_STORAGE = 18;
    private final static int COL_IDX_HAS_SHOWER = 19;
    private final static int COL_IDX_HAS_KITCHEN = 20;
    private final static int COL_IDX_HAS_LAWNSPACE = 21;
    private final static int COL_IDX_HAS_SAG = 22;
    private final static int COL_IDX_HAS_BED = 23;
    private final static int COL_IDX_HAS_LAUNDRY = 24;
    private final static int COL_IDX_HAS_FOOD = 25;
    private final static int COL_IDX_LAST_ACCESS = 26;
    private final static int COL_IDX_CREATED = 27;
    private final static int COL_IDX_CURRENTLY_AVAILABLE = 28;
    private final static int COL_IDX_SPOKEN_LANGUAGES = 29;
    private final static int COL_IDX_LATITUDE = 30;
    private final static int COL_IDX_LONGITUDE = 31;
    private final static int COL_IDX_PROFILE_PICTURE_SMALL = 32;
    private final static int COL_IDX_PROFILE_PICTURE_LARGE = 33;

    @Inject
    public UserDao(AppDatabase db) {
        super(db.getDatabase());
    }

    public User load(int userId) {
        return executeNonTransactional(db -> {
            try (Cursor cursor = db.query(TABLE_NAME, DEFAULT_COLUMNS, "id = ?", int2str(userId),
                    null, null, null, null)) {

                if (!cursor.moveToFirst()) {
                    return null;
                }

                return getUserFromCursor(cursor);
            }
        });

    }

    public List<User> loadAll() {
        return executeNonTransactional(db -> {
            List<User> users = new ArrayList<>();

            try (Cursor cursor = db.query(TABLE_NAME, DEFAULT_COLUMNS,
                    null, null, null, null, null, null)) {

                if (cursor.moveToFirst()) {
                    do {
                        User user = getUserFromCursor(cursor);
                        users.add(user);
                    } while (cursor.moveToNext());
                }
            }

            return users;
        });
    }

    public void save(User user) {
        executeNonTransactional(db -> {
            save(db, user);
            return null;
        });
    }
    public void save(SQLiteDatabase db, User user) {
        ContentValues cv = new ContentValues();
        cv.put("id", user.id);
        cv.put("name", user.username);
        cv.put("fullname", user.fullname);
        cv.put("street", user.street);
        cv.put("additional_address", user.additionalAddress);
        cv.put("city", user.city);
        cv.put("province", user.province);
        cv.put("postal_code", user.postalCode);
        cv.put("country_code", user.countryCode);
        cv.put("mobile_phone", user.mobilePhone);
        cv.put("home_phone", user.homePhone);
        cv.put("work_phone", user.workPhone);
        cv.put("comments", user.comments);
        cv.put("preferred_notice", user.preferredNotice);
        cv.put("max_cyclists_count", user.maximalCyclistCount);
        cv.put("distance_to_motel", user.distanceToMotel);
        cv.put("distance_to_campground", user.distanceToCampground);
        cv.put("distance_to_bikeshop", user.distanceToBikeshop);
        cv.put("has_storage", user.hasStorage);
        cv.put("has_shower", user.hasShower);
        cv.put("has_kitchen", user.hasKitchen);
        cv.put("has_lawnspace", user.hasLawnspace);
        cv.put("has_sag", user.hasSag);
        cv.put("has_bed", user.hasBed);
        cv.put("has_laundry", user.hasLaundry);
        cv.put("has_food", user.hasFood);
        cv.put("last_access", DateConverter.dateToLong(user.lastAccess));
        cv.put("created", DateConverter.dateToLong(user.created));
        cv.put("currently_available", user.isCurrentlyAvailable);
        cv.put("spoken_languages", user.spokenLanguages);
        cv.put("latitude", user.location.getLatitude());
        cv.put("longitude", user.location.getLongitude());
        cv.put("profile_picture_small", user.profilePicture.getSmallUrl());
        cv.put("profile_picture_large", user.profilePicture.getLargeUrl());

        insertOrUpdate(db, TABLE_NAME, cv, user.id);
    }

    public void delete(int userId) {
        executeNonTransactional(db -> {
            db.delete(TABLE_NAME, "id = ?", int2str(userId));
            return null;
        });
    }

    private static User getUserFromCursor(@NonNull Cursor c) {
        return new User(
                c.getInt(COL_IDX_ID),
                c.getString(COL_IDX_NAME),
                c.getString(COL_IDX_FULLNAME),
                c.getString(COL_IDX_STREET),
                c.getString(COL_IDX_ADDITIONAL_ADDRESS),
                c.getString(COL_IDX_CITY),
                c.getString(COL_IDX_PROVINCE),
                c.getString(COL_IDX_POSTAL_CODE),
                c.getString(COL_IDX_COUNTRY_CODE),
                new GeoPoint(c.getDouble(COL_IDX_LATITUDE), c.getDouble(COL_IDX_LONGITUDE)),
                c.getString(COL_IDX_MOBILE_PHONE),
                c.getString(COL_IDX_HOME_PHONE),
                c.getString(COL_IDX_WORK_PHONE),
                c.getString(COL_IDX_COMMENTS),
                c.getString(COL_IDX_PREFERRED_NOTICE),
                c.getInt(COL_IDX_MAX_CYCLISTS_COUNT),
                c.getString(COL_IDX_DISTANCE_TO_MOTEL),
                c.getString(COL_IDX_DISTANCE_TO_CAMPGROUND),
                c.getString(COL_IDX_DISTANCE_TO_BIKESHOP),
                getBool(c, COL_IDX_HAS_STORAGE),
                getBool(c, COL_IDX_HAS_SHOWER),
                getBool(c, COL_IDX_HAS_KITCHEN),
                getBool(c, COL_IDX_HAS_LAWNSPACE),
                getBool(c, COL_IDX_HAS_SAG),
                getBool(c, COL_IDX_HAS_BED),
                getBool(c, COL_IDX_HAS_LAUNDRY),
                getBool(c, COL_IDX_HAS_FOOD),
                c.getString(COL_IDX_SPOKEN_LANGUAGES),
                getBool(c, COL_IDX_CURRENTLY_AVAILABLE),
                new SimpleUser.Picture(c.getString(COL_IDX_PROFILE_PICTURE_SMALL),
                        c.getString(COL_IDX_PROFILE_PICTURE_LARGE)),

                DateConverter.longToDate(c.getLong(COL_IDX_CREATED)),
                DateConverter.longToDate(c.getLong(COL_IDX_LAST_ACCESS)));
    }

    private static boolean getBool(@NonNull Cursor c, int col) {
        return c.getInt(col) != 0;
    }
}
