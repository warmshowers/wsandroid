package fi.bitrite.android.ws.persistence;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fi.bitrite.android.ws.model.Host;
import fi.bitrite.android.ws.persistence.converters.DateConverter;
import fi.bitrite.android.ws.persistence.db.AppDatabase;

@Singleton
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

    public Host load(int userId) {
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

    public List<Host> loadAll() {
        return executeNonTransactional(db -> {
            List<Host> users = new ArrayList<>();

            try (Cursor cursor = db.query(TABLE_NAME, DEFAULT_COLUMNS,
                    null, null, null, null, null, null)) {

                if (cursor.moveToFirst()) {
                    do {
                        Host user = getUserFromCursor(cursor);
                        users.add(user);
                    } while (cursor.moveToNext());
                }
            }

            return users;
        });
    }

    public void save(Host user) {
        executeNonTransactional(db -> {
            save(db, user);
            return null;
        });
    }
    public void save(SQLiteDatabase db, Host user) {
        ContentValues cv = new ContentValues();
        cv.put("id", user.getId());
        cv.put("name", user.getName());
        cv.put("fullname", user.getFullname());
        cv.put("street", user.getStreet());
        cv.put("additional_address", user.getAdditional());
        cv.put("city", user.getCity());
        cv.put("province", user.getProvince());
        cv.put("postal_code", user.getPostalCode());
        cv.put("country_code", user.getCountry());
        cv.put("mobile_phone", user.getMobilePhone());
        cv.put("home_phone", user.getHomePhone());
        cv.put("work_phone", user.getWorkPhone());
        cv.put("comments", user.getComments());
        cv.put("preferred_notice", user.getPreferredNotice());
        cv.put("max_cyclists_count", Integer.parseInt(user.getMaxCyclists()));
        cv.put("distance_to_motel", user.getMotel());
        cv.put("distance_to_campground", user.getCampground());
        cv.put("distance_to_bikeshop", user.getBikeshop());
        cv.put("has_storage", s2b(user.getStorage()));
        cv.put("has_shower", s2b(user.getShower()));
        cv.put("has_kitchen", s2b(user.getKitchenUse()));
        cv.put("has_lawnspace", s2b(user.getLawnspace()));
        cv.put("has_sag", s2b(user.getSag()));
        cv.put("has_bed", s2b(user.getBed()));
        cv.put("has_laundry", s2b(user.getLaundry()));
        cv.put("has_food", s2b(user.getFood()));
        cv.put("last_access", DateConverter.dateToLong(user.getLastLoginAsDate()));
        cv.put("created", DateConverter.dateToLong(user.getCreatedAsDate()));
        cv.put("currently_available",
                !s2b(user.getNotCurrentlyAvailable())); // NOT_currently_available
        cv.put("spoken_languages", user.getLanguagesSpoken());
        cv.put("latitude", Double.parseDouble(user.getLatitude()));
        cv.put("longitude", Double.parseDouble(user.getLongitude()));
        cv.put("profile_picture_small", user.getProfilePictureSmall());
        cv.put("profile_picture_large", user.getProfilePictureLarge());

        insertOrUpdate(db, TABLE_NAME, cv, user.getId());
    }

    public void delete(int userId) {
        executeNonTransactional(db -> {
            db.delete(TABLE_NAME, "id = ?", int2str(userId));
            return null;
        });
    }

    private static String i2s(int i) {
        return Integer.toString(i);
    }
    private static String b2s(int b) {
        return b == 0 ? "0" : "1";
    }
    private static String d2s(double d) {
        return Double.toString(d);
    }
    private static String dte2s(Date d) {
        return i2s((int) (d.getTime() / 1000));
    }

    private static boolean s2b(String s) {
        return "1".equals(s);
    }

    private static Host getUserFromCursor(@NonNull Cursor c) {
        return new Host(
                c.getInt(COL_IDX_ID), c.getString(COL_IDX_NAME), c.getString(COL_IDX_FULLNAME),
                c.getString(COL_IDX_STREET), c.getString(COL_IDX_ADDITIONAL_ADDRESS),
                c.getString(COL_IDX_CITY), c.getString(COL_IDX_PROVINCE),
                c.getString(COL_IDX_POSTAL_CODE), c.getString(COL_IDX_COUNTRY_CODE),
                c.getString(COL_IDX_MOBILE_PHONE), c.getString(COL_IDX_HOME_PHONE),
                c.getString(COL_IDX_WORK_PHONE), c.getString(COL_IDX_COMMENTS),
                c.getString(COL_IDX_PREFERRED_NOTICE),
                i2s(c.getInt(COL_IDX_MAX_CYCLISTS_COUNT)),
                b2s(1 - c.getInt(COL_IDX_CURRENTLY_AVAILABLE)), // It is NOT_currently_available in Host
                b2s(c.getInt(COL_IDX_HAS_BED)), c.getString(COL_IDX_DISTANCE_TO_BIKESHOP),
                c.getString(COL_IDX_DISTANCE_TO_CAMPGROUND), b2s(c.getInt(COL_IDX_HAS_FOOD)),
                b2s(c.getInt(COL_IDX_HAS_KITCHEN)), b2s(c.getInt(COL_IDX_HAS_LAUNDRY)),
                b2s(c.getInt(COL_IDX_HAS_LAWNSPACE)), c.getString(COL_IDX_DISTANCE_TO_MOTEL),
                b2s(c.getInt(COL_IDX_HAS_SAG)), b2s(c.getInt(COL_IDX_HAS_SHOWER)),
                b2s(c.getInt(COL_IDX_HAS_STORAGE)), d2s(c.getDouble(COL_IDX_LATITUDE)),
                d2s(c.getDouble(COL_IDX_LONGITUDE)),
                dte2s(DateConverter.longToDate(c.getLong(COL_IDX_LAST_ACCESS))),
                dte2s(DateConverter.longToDate(c.getLong(COL_IDX_CREATED))),
                c.getString(COL_IDX_SPOKEN_LANGUAGES),
                c.getString(COL_IDX_PROFILE_PICTURE_SMALL),
                c.getString(COL_IDX_PROFILE_PICTURE_SMALL),
                c.getString(COL_IDX_PROFILE_PICTURE_LARGE));
    }
}
