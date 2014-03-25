package ht.highlig.storedobject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
/**
 * Created by revant on 1/30/14.
 */
public class Database {
    public static final String TAG = Database.class.getName();
    public static final Gson GSON = new Gson();


  public interface StoredObject {
        //This should be implemented by an enum
        public interface TYPE {
          public String getName();
          public Class getStoredObjectClass();
        }

        TYPE getStoredObjectType();
        String getStoredObjectId();
        List<Pair<String, String>> getStoredObjectSearchableTags();
        // Timestamp in seconds
        Double getStoredObjectTimestamp();
    }

    public enum SORT_ORDER {
        ASC,
        DESC
    }

    private DatabaseAccessManager mDbAccessManager;
    private static Database mInstance;

    private Database(Context context) {
        this.mDbAccessManager = DatabaseAccessManager.getInstance(context);
    }

    public static Database with(Context context) {
        if (mInstance == null) {
            mInstance = new Database(context);
        }
        return mInstance;
    }

    public void saveObject(StoredObject object) {
        List<StoredObject> objects = new LinkedList<StoredObject>();
        objects.add(object);
        saveObjects(objects);
    }

    public void saveObjects(Collection<? extends StoredObject> objects) {
        SQLiteDatabase db = null;
        mDbAccessManager.lockDbForWrite();
        try {
            db = mDbAccessManager.getWritableDatabase();
            db.beginTransaction();

            ContentValues contentValues = new ContentValues();
            ContentValues tagCvs = new ContentValues();

            for (StoredObject object : objects) {
                setStringContentValue(contentValues, ObjectsTableColumn.id,
                        object.getStoredObjectId());

                setStringContentValue(contentValues, ObjectsTableColumn.type,
                        object.getStoredObjectType().getName());

                setStringContentValue(contentValues, ObjectsTableColumn.json,
                        GSON.toJson(object));

                Double ts = object.getStoredObjectTimestamp();
                Long msTs = (ts == null || ts == 0) ? System.currentTimeMillis()
                        : (long)(ts * 1000);
                contentValues.put(ObjectsTableColumn.ts.name(), msTs);

                db.insertWithOnConflict(
                        DatabaseSchema.OBJECTS_TABLE, null, contentValues,
                        SQLiteDatabase.CONFLICT_REPLACE);

                //Delete old tags
                String[] whereArgs = new String[]{object.getStoredObjectType().getName(),
                    object.getStoredObjectId()};
                db.delete(DatabaseSchema.TAGS_TABLE,
                        StringUtil.concat(TagsTableColumn.type, "= ?",
                        " AND ", TagsTableColumn.id, "= ?"),
                        whereArgs);
                //Add new tags
                List<Pair<String, String>> tags = object.getStoredObjectSearchableTags();
                if (tags != null && tags.size() > 0) {
                    for (Pair<String, String> pair: tags) {
                        setStringContentValue(tagCvs, TagsTableColumn.id, object.getStoredObjectId());
                        setStringContentValue(tagCvs, TagsTableColumn.type, object.getStoredObjectType().getName());
                        setStringContentValue(tagCvs, TagsTableColumn.tag, pair.first);
                        setStringContentValue(tagCvs, TagsTableColumn.value, pair.second);
                        db.insertWithOnConflict(DatabaseSchema.TAGS_TABLE, null, tagCvs,
                                SQLiteDatabase.CONFLICT_REPLACE);
                    }
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error when storing object:\n" + e.getMessage());
        } finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
            mDbAccessManager.unlockDbForWrite();
        }
    }

    /**
     * Load objects in descending order by timestamp
     * @param type
     * @param <T>
     * @return
     */
    private  <T extends StoredObject> List<T> loadObjects(StoredObject.TYPE type, String order,
                                                          int limit) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        mDbAccessManager.lockDbForRead();
        try {
            db = mDbAccessManager.getReadableDatabase();
            if (db == null) return null;
            String[] columns = new String[]{
                    ObjectsTableColumn.id.name(),
                    ObjectsTableColumn.type.name(),
                    ObjectsTableColumn.json.name(),
                    ObjectsTableColumn.ts.name()};

            String selection = StringUtil.concat(ObjectsTableColumn.type, "=?");
            String[] selectionArgs = new String[]{type.getName()};
            cursor = db.query(
                    DatabaseSchema.OBJECTS_TABLE,
                    columns,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    order,
                    limit > 0 ? StringUtil.concat("LIMIT ", limit) : null);

            List<T> storedObjects = new ArrayList<T>();
            if (cursor.moveToFirst()) {
                do {
                    String jsonString = cursor.getString(2);
                    storedObjects.add((T) GSON.fromJson(jsonString, type.getStoredObjectClass()));
                } while (cursor.moveToNext());
            }
            return storedObjects;

        } catch (Exception e) {
            Log.e(TAG, "Error when fetching stored objects " + e.getMessage());
            return new ArrayList<T>();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            mDbAccessManager.unlockAndCloseDbAfterRead(db);
        }
    }

    private  <T extends StoredObject> List<T> loadObjects(StoredObject.TYPE type, Collection<String> ids,
                                                        String orderBy) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        mDbAccessManager.lockDbForRead();
        try {
            db = mDbAccessManager.getReadableDatabase();
            if (db == null) return null;
            String singleSelect = StringUtil.concat(ObjectsTableColumn.id.name(),
                    "=? AND ", ObjectsTableColumn.type.name(), "=?");
            String selection = StringUtil.repeat(singleSelect, " OR ", ids.size());
            String[] columns = new String[]{
                    ObjectsTableColumn.id.name(),
                    ObjectsTableColumn.type.name(),
                    ObjectsTableColumn.json.name(),
                    ObjectsTableColumn.ts.name()};
            String[] selectionArgs = new String[ids.size()*2];
            int i = 0;
            for (String id : ids) {
                selectionArgs[i*2] = id;
                selectionArgs[i*2 + 1] = type.getName();
                i++;
            }
            cursor = db.query(
                    DatabaseSchema.OBJECTS_TABLE,
                    columns,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    orderBy
            );

            List<T> storedObjects = new ArrayList<T>();
            if (cursor.moveToFirst()){
                do {
                    String objectType = cursor.getString(1);
                    String json = cursor.getString(2);
                    StoredObject storedObject = (T)GSON.fromJson(json, type.getStoredObjectClass());
                    storedObjects.add((T) storedObject);
                } while (cursor.moveToNext());
            }
            return storedObjects;
        } catch (Exception e) {
            Log.e(TAG, "Unable to fetch stored object: " + e.getMessage());
            return new ArrayList<T>();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            mDbAccessManager.unlockAndCloseDbAfterRead(db);
        }
    }

    private Set<String> getIdsFromSelection(StoredObject.TYPE type, List<String> selections,
                                             List<String> args, int limit) {
        if (selections == null || selections.size() == 0 || selections.size() != args.size()) return null;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        mDbAccessManager.lockDbForRead();
        try {
            db = mDbAccessManager.getReadableDatabase();
            if (db == null) return null;
            String[] columns = new String[]{TagsTableColumn.id.name()};

            ListIterator<String> selectionIt = selections.listIterator();
            ListIterator<String> argsIt = args.listIterator();
            String typeSelection = StringUtil.concat(TagsTableColumn.type, "=", type.getName(), " AND ");
            Set<String> finalIds = null;
            while (selectionIt.hasNext()) {
                if (finalIds != null && finalIds.size() == 0) break;
                selectionIt.next();
                cursor = db.query(true,
                        DatabaseSchema.TAGS_TABLE,
                        columns,
                        StringUtil.concat(typeSelection, selectionIt.next()),
                        new String[]{argsIt.next()},
                        null,
                        null,
                        null,
                        limit > 0 ? StringUtil.concat("LIMIT ", limit) : null);
                if (cursor.getCount() == 0) break;

                Set currentIds = new HashSet(cursor.getCount());
                if (cursor.moveToFirst()){
                    do {
                        currentIds.add(cursor.getString(0));
                    } while (cursor.moveToNext());
                }
                if (finalIds == null)  {
                    finalIds = new HashSet<String>(currentIds);
                } else {
                    finalIds.retainAll(currentIds);
                }
            }
            return finalIds;
        } catch (Exception e) {
            Log.e(TAG, "Unable to fetch stored object: " + e.getMessage());
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            mDbAccessManager.unlockAndCloseDbAfterRead(db);
        }
    }

    public void deleteObjects(StoredObject.TYPE[] types, String[] ids) {
        if (types == null || ids == null || types.length == 0 || ids.length == 0) return;
        SQLiteDatabase db = null;
        mDbAccessManager.lockDbForWrite();
        try {
            db = mDbAccessManager.getWritableDatabase();
            db.beginTransaction();
            String selection = StringUtil.repeat(
                    StringUtil.concat(ObjectsTableColumn.type, "=?", " AND ", ObjectsTableColumn.id, " =? ")
            , " OR ", ids.length);
            String[] whereArgs = new String[ids.length * 2];
            for (int i=0; i < ids.length; i++) {
                whereArgs[i*2] = types[i].getName();
                whereArgs[i*2 + 1] = ids[i];
            }
            db.delete(DatabaseSchema.OBJECTS_TABLE, selection, whereArgs);
            db.delete(DatabaseSchema.TAGS_TABLE, selection, whereArgs);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error when storing object:\n" + e.getMessage());
        } finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
            mDbAccessManager.unlockDbForWrite();
        }
    }

    public void deleteObject(StoredObject object) {
        if (object == null || object.getStoredObjectId() == null || object.getStoredObjectType() == null) return;
        deleteObjects(new StoredObject.TYPE[]{object.getStoredObjectType()},
                new String[]{object.getStoredObjectId()});
    }

    public void clearObjectsOfType(StoredObject.TYPE type) {
        SQLiteDatabase db = null;
        mDbAccessManager.lockDbForWrite();
        try {
            db = mDbAccessManager.getWritableDatabase();
            db.beginTransaction();
            String selection = StringUtil.concat(ObjectsTableColumn.type, " =? ");
            String[] whereArgs = new String[]{type.getName()};
            db.delete(DatabaseSchema.OBJECTS_TABLE, selection, whereArgs);
            db.delete(DatabaseSchema.TAGS_TABLE, selection, whereArgs);
        } catch (Exception e) {
            Log.e(TAG, "Error when storing object:\n" + e.getMessage());
        } finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
            mDbAccessManager.unlockDbForWrite();
        }
    }

    public void setStringContentValue(
            ContentValues contentValues, Enum column, String value) {
        if (value != null) {
            contentValues.put(column.name(), value);
        }
    }

    public Request load(StoredObject.TYPE type) {
        return new Request(type);
    }

    public class Request {
        StoredObject.TYPE type;
        List<String> ids;
        List<String> tagSelects;
        List<String> tagSelectionArgs;
        String tagOrderBy;
        String orderByTagName;
        SORT_ORDER tsOrdering;
        int limit;
        boolean truncate;

        public Request(StoredObject.TYPE type) {
            this.type = type;
        }

        public Request withIds(List<String> ids) {
            if (ids == null || ids.size() == 0) return this;
            if (this.ids == null) this.ids = new LinkedList<String>();
            this.ids.addAll(ids);
            return this;
        }

        public Request withId(String id) {
            if (this.ids == null) this.ids = new LinkedList<String>();
            this.ids.add(id);
            return this;
        }

        public Request tagEquals(String tag, String value) {
            return tagWithOperator(tag, "=", value, SqliteType.TEXT);
        }

        public Request tagGt(String tag, String value, SqliteType valueType) {
            return tagWithOperator(tag, ">", value, valueType);
        }

        public Request tagLt(String tag, String value, SqliteType valueType) {
            return tagWithOperator(tag, "<", value, valueType);
        }

        /**
         * Add a tag with a specific operator for the given value
         * @param tag tag name
         * @param op Sqllite operation: <=, >=, NOT, LIKE etc.
         * @param value value
         * @return
         */
        public Request tagWithOperator(String tag, String op, String value, SqliteType valueType) {
            if (ids != null && ids.size() > 0) {
                throw new UnsupportedOperationException("Can't have both tags and ids");
            }
            if (tagSelects == null) {
                tagSelects = new LinkedList<String>();
                tagSelectionArgs = new LinkedList<String>();
            }
            tagSelects.add(StringUtil.concat(TagsTableColumn.tag, "= '", tag, "' AND ",
                    "CAST(", TagsTableColumn.value, " AS ", valueType.name(), ") ", op, " ? "));
            tagSelectionArgs.add(value);
            return this;
        }

        public Request orderByTs(SORT_ORDER order) {
            if (tagOrderBy != null) {
                throw new UnsupportedOperationException("Only supports one ordering");
            }
            this.tsOrdering = order;
            return this;
        }

        /** Truncates everything else of this type except for things fetched in this request **/
        public Request truncateRest() {
            truncate = true;
            return this;
        }

        /*
        TODO: complete implementation
        public Request orderByTag(String tag, SORT_ORDER order, SqliteType valueType) {
            if (tsOrdering != null || tagOrderBy != null) {
                throw new UnsupportedOperationException("Only supports one ordering");
            }
            orderByTagName = tag;
            tagOrderBy = StringUtil.concat("CAST(", tag, " AS ", valueType.name(), ") ", order.name());
            return this;
        } */

        public Request limit(int l) {
            limit = l;
            return this;
        }

        public <T extends StoredObject> List<T> execute() {
            String order = (tsOrdering == null) ? null :
                    StringUtil.concat(ObjectsTableColumn.ts, " ", tsOrdering);
            List<T> retVal = null;
            if (ids != null && ids.size() > 0) {
                retVal =  loadObjects(type, ids, order);
            } else if (tagSelects != null && tagSelects.size() > 0) {
                Set<String> ids = getIdsFromSelection(type, tagSelects, tagSelectionArgs, limit);
                retVal = loadObjects(type, ids, order);
            } else {
                retVal = loadObjects(type, order, limit);
            }
            if (truncate) {
                clearObjectsOfType(type);
                saveObjects(retVal);
            }
            return retVal;
        }
     }

}
