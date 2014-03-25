package ht.highlig.storedobject;
/**
 * Created by revant on 1/30/14.
 */
/**
 * Represents a Database column.
 */
interface DatabaseColumn {
  SqliteType getType();
  String getNameAndType();
}