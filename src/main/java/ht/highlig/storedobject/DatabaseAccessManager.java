package ht.highlig.storedobject;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by revant on 1/30/14.
 */
class DatabaseAccessManager extends SQLiteOpenHelper {
  private static DatabaseAccessManager mInstance = null;
  private final ReentrantReadWriteLock dbLock;
  private static final int DB_VERSION = 1;

  private AtomicInteger readers = new AtomicInteger(0);

  public static DatabaseAccessManager getInstance(Context context) {
    if (mInstance == null) {
      mInstance = new DatabaseAccessManager(context, DB_VERSION);
    }
    return mInstance;
  }

  private DatabaseAccessManager(Context context, int version) {
    super(context, DatabaseSchema.DB_NAME, null, version);
    this.dbLock = new ReentrantReadWriteLock();
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    DatabaseSchema.recreateDb(db);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    DatabaseSchema.recreateDb(db);
  }

  @Override
  public SQLiteDatabase getWritableDatabase() {
    SQLiteDatabase db = super.getWritableDatabase();
    if (!db.isOpen()) {
      // Sometimes for some reason the db returned could actually be
      // closed. For now, naively recall super#getWritableDatabase
      // to create or open the db in write mode.
      db = super.getWritableDatabase();
    }

    return db;
  }

  @Override
  public SQLiteDatabase getReadableDatabase() {
    readers.incrementAndGet();
    SQLiteDatabase db = super.getReadableDatabase();
    if (!db.isOpen()) {
      // Sometimes for some reason the db returned could actually be
      // closed. For now, naively recall super#getReadableDatabase
      // to create or open the db in read mode.
      db = super.getReadableDatabase();
    }

    return db;
  }

  public void lockDbForRead() {
    dbLock.readLock().lock();
  }

  private void unlockDbForRead() {
    dbLock.readLock().unlock();
  }

  public void lockDbForWrite() {
    dbLock.writeLock().lock();
  }

  public void unlockDbForWrite() {
    dbLock.writeLock().unlock();
  }

  public void unlockAndCloseDbAfterRead(SQLiteDatabase db) {
    if (readers.decrementAndGet() == 0 && db != null) {
      db.close();
    }

    unlockDbForRead();
  }

}
