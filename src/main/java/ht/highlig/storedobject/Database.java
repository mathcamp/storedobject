package ht.highlig.storedobject;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

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
        public interface TYPE {
            public String getTypeName();
            public Class getTypeClass();
        }

        public TYPE getStoredObjectType();
        public String getStoredObjectId();
        public List<SearchableTagValuePair> getStoredObjectSearchableTags();
        public Long getStoredObjectTimestampMillis();
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
        List<StoredObject> list = new ArrayList<StoredObject>();
        list.add(object);
        saveObjects(list);
    }

    public void saveObjects(Collection<? extends StoredObject> objects) {
        SQLiteDatabase db = null;
        mDbAccessManager.lockDbForWrite();
        try {
            db = mDbAccessManager.getWritableDatabase();
            if (db != null) {
                db.beginTransaction();
            } else {
                throw new DatabaseException("Database can't be opened for writing");
            }

            ContentValues contentValues = new ContentValues();
            ContentValues tagCvs = new ContentValues();

            for (StoredObject object : objects) {
                setStringContentValue(contentValues, ObjectsTableColumn.id,
                        object.getStoredObjectId());

                setStringContentValue(contentValues, ObjectsTableColumn.type,
                        object.getStoredObjectType().getTypeName());

                setStringContentValue(contentValues, ObjectsTableColumn.json,
                        GSON.toJson(object));

                Long ts = object.getStoredObjectTimestampMillis();
                Long msTs = (ts == null || ts == 0) ? System.currentTimeMillis()
                        : ts;
                contentValues.put(ObjectsTableColumn.ts.name(), msTs);

                db.insertWithOnConflict(
                        DatabaseSchema.OBJECTS_TABLE, null, contentValues,
                        SQLiteDatabase.CONFLICT_REPLACE);

                //Delete old tags
                String[] whereArgs = new String[]{object.getStoredObjectType().getTypeName(),
                    object.getStoredObjectId()};
                db.delete(DatabaseSchema.TAGS_TABLE,
                        StringUtil.concat(TagsTableColumn.type, "= ?",
                        " AND ", TagsTableColumn.id, "= ?"),
                        whereArgs);
                //Add new tags
                List<SearchableTagValuePair> tags = object.getStoredObjectSearchableTags();
                if (tags != null && tags.size() > 0) {
                    for (SearchableTagValuePair pair: tags) {
                        setStringContentValue(tagCvs, TagsTableColumn.id, object.getStoredObjectId());
                        setStringContentValue(tagCvs, TagsTableColumn.type, object.getStoredObjectType().getTypeName());
                        setStringContentValue(tagCvs, TagsTableColumn.tag, pair.key);
                        setStringContentValue(tagCvs, TagsTableColumn.value, pair.value);
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
                                                          int limit, Long before, Long after) {
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

            StringBuilder selection = new StringBuilder(StringUtil.concat(ObjectsTableColumn.type, "=?"));
            LinkedList<String> selectionArgs = new LinkedList<String>();
            selectionArgs.add(type.getTypeName());
            if (before != null) {
                selection.append(" AND ").
                        append(ObjectsTableColumn.ts).
                        append("<=?");
                selectionArgs.add(before.toString());
            }
            if (after != null) {
                selection.append(" AND ").
                        append(ObjectsTableColumn.ts).
                        append(">=?");
                selectionArgs.add(after.toString());
            }
            cursor = db.query(
                    DatabaseSchema.OBJECTS_TABLE,
                    columns,
                    selection.toString(),
                    selectionArgs.toArray(new String[selectionArgs.size()]),
                    null,
                    null,
                    order,
                    limit > 0 ? StringUtil.concat("LIMIT ", limit) : null);

            List<T> storedObjects = new ArrayList<T>();
            if (cursor.moveToFirst()) {
                do {
                    String jsonString = cursor.getString(2);
                    storedObjects.add((T) GSON.fromJson(jsonString, type.getTypeClass()));
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
                selectionArgs[i*2 + 1] = type.getTypeName();
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
                    StoredObject storedObject = (T)GSON.fromJson(json, type.getTypeClass());
                    storedObjects.add((T) storedObject);
                } while (cursor.moveToNext());
            }
            return storedObjects;
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
            String typeSelection = StringUtil.concat(TagsTableColumn.type, "='", type, "' AND ");
            Set<String> finalIds = null;
            while (selectionIt.hasNext()) {
                if (finalIds != null && finalIds.size() == 0) break;
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

    public void deleteObjects(Collection<? extends StoredObject> objects) {
        if (objects == null || objects.size() == 0) return;
        StoredObject.TYPE[] types = new StoredObject.TYPE[objects.size()];
        String[] ids = new String[objects.size()];
        int i = 0;
        for (StoredObject object: objects) {
            types[i] = object.getStoredObjectType();
            ids[i] = object.getStoredObjectId();
            i++;
        }
        deleteObjects(types, ids);
    }

    public void deleteObjects(StoredObject.TYPE[] types, String[] ids) {
        if (types == null || ids == null || types.length == 0 || ids.length == 0) return;
        SQLiteDatabase db = null;
        mDbAccessManager.lockDbForWrite();
        try {
            db = mDbAccessManager.getWritableDatabase();
            if (db != null) {
                db.beginTransaction();
            } else {
                throw new DatabaseException("Database can't be opened for writing");
            }
            String selection = StringUtil.repeat(
                    StringUtil.concat(ObjectsTableColumn.type, "=?", " AND ", ObjectsTableColumn.id, " =? ")
            , " OR ", ids.length);
            String[] whereArgs = new String[ids.length * 2];
            for (int i=0; i < ids.length; i++) {
                whereArgs[i*2] = types[i].getTypeName();
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
            if (db != null) {
                db.beginTransaction();
            } else {
                throw new DatabaseException("Database can't be opened for writing");
            }
            String selection = StringUtil.concat(ObjectsTableColumn.type, " =? ");
            String[] whereArgs = new String[]{type.getTypeName()};
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
        Long before = null;
        Long after = null;

        public Request(StoredObject.TYPE type) {
            this.type = type;
        }

        public Request addIds(List<String> ids) {
            if (ids == null || ids.size() == 0) return this;
            if (this.ids == null) this.ids = new LinkedList<String>();
            this.ids.addAll(ids);
            return this;
        }

        public Request addId(String id) {
            if (this.ids == null) this.ids = new LinkedList<String>();
            this.ids.add(id);
            return this;
        }

        public Request tagEquals(String tag, String value) {
            return tagEquals(tag, value, SqliteType.TEXT);
        }

        public Request tagEquals(String tag, Object value, SqliteType valueType) {
            return tagWithOperator(tag, "=", value.toString(), valueType);
        }

        public Request tagGt(String tag, String value, SqliteType valueType) {
            return tagWithOperator(tag, ">", value, valueType);
        }

        public Request tagLt(String tag, String value, SqliteType valueType) {
            return tagWithOperator(tag, "<", value, valueType);
        }

        public Request tsGtEq(long timestampMs) {
            after = timestampMs;
            return this;
        }

        public Request tsLtEq(long timestampMs) {
            before = timestampMs;
            return this;
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
                retVal = loadObjects(type, order, limit, before, after);
            }
            if (truncate) {
                clearObjectsOfType(type);
                saveObjects(retVal);
            }
            return retVal;
        }

        public <T extends StoredObject> T getFirst() {
            List<T> list = execute();
            if (list == null || list.size() == 0) {
                Log.d(TAG, "No object of type: " + type + " found ");
                return null;
            } else {
                return list.get(0);
            }
        }

        @Override
        public String toString() {
            return getClass().getName() + "{" +
                    "type:" + type +
                    ",ids:" + ids +
                    ",tagSelects:" + tagSelects +
                    ",tagOrderBy:" + tagOrderBy +
                    ",orderByTagName:" + orderByTagName +
                    ",tsOrdering:" + tsOrdering +
                    ",limit:" + limit +
                    ",trucate:" + truncate +
                    ",before:" + before +
                    ",after:" + after;
        }
     }
}
