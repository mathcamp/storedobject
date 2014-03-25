package ht.highlig.storedobject;
import android.database.sqlite.SQLiteDatabase;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by revant on 1/30/14.
 */
  class DatabaseSchema {
    public static final String DB_NAME = "stored.objects.db";
    public static final String OBJECTS_TABLE = "objects";
    public static final String TAGS_TABLE = "tags";

    public static final List<String> COMMANDS = new LinkedList<String>() {{
        add(StringUtil.concat(
                "DROP TABLE IF EXISTS ", OBJECTS_TABLE, ";"));
        add(StringUtil.concat(
                "CREATE TABLE ", OBJECTS_TABLE, " (",
                ObjectsTableColumn.id.getNameAndType(), ",",
                ObjectsTableColumn.type.getNameAndType(), ",",
                ObjectsTableColumn.json.getNameAndType(), ",",
                ObjectsTableColumn.ts.getNameAndType(), ",",
                "PRIMARY KEY(", ObjectsTableColumn.type.name(), ", ",
                ObjectsTableColumn.id.name(), ")",
                ");"));
        add(StringUtil.concat(
                "CREATE INDEX ",
                OBJECTS_TABLE, "_", ObjectsTableColumn.type.name(), "_index",
                " ON ", OBJECTS_TABLE, " (", ObjectsTableColumn.type.name(),", ",
                ObjectsTableColumn.ts.name(),");"));

        //Tags table
        add(StringUtil.concat("DROP TABLE IF EXISTS ", TAGS_TABLE, ";"));
        add(StringUtil.concat(
                "CREATE TABLE ", TAGS_TABLE, " (",
                TagsTableColumn.id.getNameAndType(), ",",
                TagsTableColumn.tag.getNameAndType(), ",",
                TagsTableColumn.type.getNameAndType(), ",",
                TagsTableColumn.value.getNameAndType(), ",",
                "PRIMARY KEY(", TagsTableColumn.type.name(), ", ",
                TagsTableColumn.tag.name(), ", ", TagsTableColumn.value.name(),", ",
                TagsTableColumn.id.name(), ")",
                ");"));
        add(StringUtil.concat(
                "CREATE INDEX ",
                TAGS_TABLE, "_", TagsTableColumn.type.name(), "_index",
                " ON ", TAGS_TABLE, " (", TagsTableColumn.type.name(),", ",
                TagsTableColumn.id.name(),");"));

    }};

    public static void recreateDb(SQLiteDatabase db) {
        for (String command : COMMANDS) {
            db.execSQL(command);
        }
    }
}
