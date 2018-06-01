package fi.bitrite.android.ws.persistence.schema.migrations.account;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;

import fi.bitrite.android.ws.di.account.AccountScope;
import fi.bitrite.android.ws.persistence.schema.migrations.app.MigrationTo4;
import io.reactivex.subjects.BehaviorSubject;

@AccountScope
public class MigrationTo1 {
    public static final BehaviorSubject<List<Integer>> savedFavoriteUserIds =
            BehaviorSubject.create();

    @Inject
    MigrationTo1() {
    }

    void recoverSavedFavoriteUserIds(@NonNull SQLiteDatabase db) {
        MigrationTo4.savedFavoriteUserIds.subscribe(favoriteUserIds -> {
            ContentValues cv = new ContentValues();
            for (Integer userId : favoriteUserIds) {
                cv.put("user_id", userId);
                db.insert("favorite_user", null, cv);
            }

            // Mark the savedFavoriteUserIds as completed s.t. no other account imports the
            // favorites.
            MigrationTo4.savedFavoriteUserIds.onComplete();
        });
    }
}
