package ht.highlig.storedobject;
/**
 * revant @ Mathcamp Inc
 * Date: 2/14/13
 */
enum ObjectsTableColumn implements DatabaseColumn {
  id(SqliteType.TEXT),
  type(SqliteType.TEXT),
  json(SqliteType.TEXT),
  ts(SqliteType.INTEGER);

  private final SqliteType sqliteType;

  private ObjectsTableColumn(SqliteType sqliteType) {
    this.sqliteType = sqliteType;
  }

  @Override
  public SqliteType getType() {
    return sqliteType;
  }

  @Override
  public String getNameAndType() {
    return name() + " " + getType();
  }
}
