package fi.bitrite.android.ws.persistence.db;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import fi.bitrite.android.ws.persistence.schema.SchemaDefinition;

/**
 * Forked from https://github.com/k9mail/k-9/blob/master/k9mail/src/main/java/com/fsck/k9/mailstore/LockableDatabase.java
 */
public class LockableDatabase {
    private final static String TAG = LockableDatabase.class.getName();

    /**
     * Callback interface for DB operations. Concept is similar to Spring
     * HibernateCallback.
     *
     * @param <T> Return value type for {@link #doDbWork(SQLiteDatabase)}
     */
    public interface DbCallback<T> {
        /**
         * @param db The locked database on which the work should occur.
         * @return Any relevant data.
         */
        @Nullable
        T doDbWork(@NonNull SQLiteDatabase db) throws SQLiteException;
    }

    private SQLiteDatabase mDb;

    /**
     * Reentrant read lock
     */
    private final Lock mReadLock;
    /**
     * Reentrant write lock (if you lock it 2x from the same thread, you have to
     * unlock it 2x to release it)
     */
    private final Lock mWriteLock;

    {
        final ReadWriteLock lock = new ReentrantReadWriteLock(true);
        mReadLock = lock.readLock();
        mWriteLock = lock.writeLock();
    }

    private Context mContext;

    /**
     * {@link ThreadLocal} to check whether a DB transaction is occurring in the
     * current {@link Thread}.
     *
     * @see #execute(boolean, DbCallback)
     */
    private ThreadLocal<Boolean> mInTransaction = new ThreadLocal<>();

    private SchemaDefinition mSchemaDefinition;

    public LockableDatabase(@NonNull final Context context,
                            @NonNull final SchemaDefinition schemaDefinition) {
        mContext = context;
        mSchemaDefinition = schemaDefinition;
    }

    /**
     * Lock the storage for shared operations (concurrent threads are allowed to
     * run simultaneously).
     *
     * <p>
     * You <strong>have to</strong> invoke {@link #unlockRead()} when you're
     * done with the storage.
     * </p>
     */
    private void lockRead() {
        mReadLock.lock();
    }

    private void unlockRead() {
        mReadLock.unlock();
    }

    /**
     * Lock the storage for exclusive access (other threads aren't allowed to
     * run simultaneously)
     *
     * <p>
     * You <strong>have to</strong> invoke {@link #unlockWrite()} when you're
     * done with the storage.
     * </p>
     */
    private void lockWrite() {
        mWriteLock.lock();
    }

    private void unlockWrite() {
        mWriteLock.unlock();
    }

    public <T> T executeTransactional(@NonNull final DbCallback<T> callback) {
        return execute(true, callback);
    }

    public <T> T executeNonTransactional(@NonNull final DbCallback<T> callback) {
        return execute(false, callback);
    }

    /**
     * Execute a DB callback in a shared context (doesn't prevent concurrent
     * shared executions), taking care of locking the DB storage.
     *
     * <p>
     * Can be instructed to start a transaction if none is currently active in
     * the current thread. Callback will participate in any active transaction (no
     * inner transaction created).
     * </p>
     *
     * @param transactional <code>true</code> the callback must be executed in a transactional context.
     * @return Whatever {@link DbCallback#doDbWork(SQLiteDatabase)} returns.
     */
    private <T> T execute(final boolean transactional, @NonNull final DbCallback<T> callback) {
        lockRead();
        final boolean doTransaction = transactional && mInTransaction.get() == null;
        try {
            if (mDb == null) {
                throw new IllegalStateException("Database is not open");
            }
            if (doTransaction) {
                mInTransaction.set(Boolean.TRUE);
                mDb.beginTransaction();
            }
            try {
                final T result = callback.doDbWork(mDb);
                if (doTransaction) {
                    mDb.setTransactionSuccessful();
                }
                return result;
            } finally {
                if (doTransaction) {
                    // Not doing endTransaction in the same 'finally' block of unlockRead() because
                    // endTransaction() may throw an exception.
                    mDb.endTransaction();
                }
            }
        } finally {
            if (doTransaction) {
                mInTransaction.set(null);
            }
            unlockRead();
        }
    }

    public void open(String dbName) {
        lockWrite();
        try {
            // Close the old connection.
            closeWithoutWriteLock();

            mDb = mContext.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null);

            if (mSchemaDefinition.getVersion() > mDb.getVersion()) {
                mSchemaDefinition.upgradeDatabase(mDb);
            } else if (mSchemaDefinition.getVersion() < mDb.getVersion()) {
                mSchemaDefinition.downgradeDatabase(mDb);
            }
        } finally {
            unlockWrite();
        }
    }

    public boolean isOpen() {
        return mDb != null && mDb.isOpen();
    }

    public void close() {
        lockWrite();
        try {
            closeWithoutWriteLock();
        } finally {
            unlockWrite();
        }
    }
    private void closeWithoutWriteLock() {
        if (!isOpen()) {
            return;
        }

        mDb.close();
        mDb = null;
    }

    /**
     * Delete the backing database.
     */
    public void delete() {
        delete(false);
    }

    public void recreate() {
        delete(true);
    }

    /**
     * @param recreate <code>true</code> if the DB should be recreated after delete
     */
    private void delete(final boolean recreate) {
        lockWrite();
        try {
            try {
                close();
            } catch (Exception e) {
                Log.d(TAG, "Exception caught in DB close", e);
            }

            File dbFile = new File(mDb.getPath());
            deleteDatabase(dbFile);

            if (recreate) {
                open(dbFile.getName());
            }
        } finally {
            unlockWrite();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void deleteDatabase(File database) {
        boolean deleted;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            deleted = SQLiteDatabase.deleteDatabase(database);
        } else {
            deleted = database.delete();
            deleted |= new File(database.getPath() + "-journal").delete();
        }
        if (!deleted) {
            Log.i(TAG, "deleteDatabase(): No files deleted.");
        }
    }
}
