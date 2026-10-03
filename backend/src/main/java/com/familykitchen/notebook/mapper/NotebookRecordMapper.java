package com.familykitchen.notebook.mapper;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import com.familykitchen.notebook.mapper.NotebookEventMapper.ImageKeyRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** SQL boundary for account-owned notebook records and snapshots. */
@Repository
public class NotebookRecordMapper {
  /** Persisted record without expanded template fields.
   * @param id record ID
   * @param eventId parent event
   * @param ownerUserId owner account
   * @param createdByUserId author
   * @param updatedByUserId last editor
   * @param from UTC occurrence start
   * @param to UTC occurrence end
   * @param title title
   * @param note note
   * @param templateVersion bound template
   * @param valuesJson encoded values
   * @param lockVersion optimistic revision */
  public record Row(long id, long eventId, long ownerUserId, long createdByUserId,
      long updatedByUserId, Instant from, Instant to, String title, String note,
      int templateVersion, String valuesJson, int lockVersion) {}

  /** Minimal occurrence interval for calendar projection.
   * @param id stable keyset cursor
   * @param from UTC start
   * @param to UTC end */
  public record Interval(long id, Instant from, Instant to) {}

  private static final RowMapper<Row> RECORD = (result, ignored) -> new Row(
      result.getLong("id"), result.getLong("event_id"), result.getLong("owner_user_id"),
      result.getLong("created_by_user_id"), result.getLong("updated_by_user_id"),
      result.getTimestamp("occurred_from").toInstant(), result.getTimestamp("occurred_to").toInstant(),
      result.getString("title"), result.getString("note"), result.getInt("template_version"),
      result.getString("values_json"), result.getInt("lock_version"));
  private final JdbcTemplate sql;

  /** Connects notebook record persistence.
   * @param sql project JDBC client */
  public NotebookRecordMapper(JdbcTemplate sql) { this.sql = sql; }

  /** Inserts a record owned by its event owner.
   * @param eventId event ID
   * @param owner owner account
   * @param actor author account
   * @param from UTC start
   * @param to UTC end
   * @param title title
   * @param note note
   * @param templateVersion published version
   * @param valuesJson encoded values
   * @return generated record ID */
  public long insert(long eventId, long owner, long actor, Instant from, Instant to,
      String title, String note, int templateVersion, String valuesJson) {
    var key = new GeneratedKeyHolder();
    sql.update(connection -> {
      PreparedStatement insert = connection.prepareStatement("INSERT INTO notebook_records"
          + "(event_id,owner_user_id,created_by_user_id,updated_by_user_id,occurred_from,occurred_to,"
          + "title,note,template_version,values_json) VALUES (?,?,?,?,?,?,?,?,?,?)", new String[] {"id"});
      insert.setLong(1, eventId); insert.setLong(2, owner); insert.setLong(3, actor);
      insert.setLong(4, actor);
      insert.setTimestamp(5, Timestamp.from(from)); insert.setTimestamp(6, Timestamp.from(to));
      insert.setString(7, title); insert.setString(8, note); insert.setInt(9, templateVersion);
      insert.setString(10, valuesJson);
      return insert;
    }, key);
    if (key.getKey() == null) throw new IllegalStateException("Notebook record key was not generated");
    return key.getKey().longValue();
  }

  /** Reads an undeleted record for its owner only.
   * @param owner account ID
   * @param id record ID
   * @return row or null */
  public Row find(long owner, long id) {
    return sql.query("SELECT * FROM notebook_records WHERE id=? AND owner_user_id=? AND deleted=FALSE",
        RECORD, id, owner).stream().findFirst().orElse(null);
  }

  /** Reads an undeleted record before the access policy checks it.
   * @param id record ID
   * @return row or null */
  public Row findAny(long id) {
    return sql.query("SELECT * FROM notebook_records WHERE id=? AND deleted=FALSE", RECORD, id)
        .stream().findFirst().orElse(null);
  }

  /** Locks a record while deleting an attached image to prevent dangling field values.
   * @param id record ID
   * @return locked record or null */
  public Row lockAny(long id) {
    return sql.query("SELECT * FROM notebook_records WHERE id=? AND deleted=FALSE FOR UPDATE", RECORD, id)
        .stream().findFirst().orElse(null);
  }

  /** Applies a version-checked record edit.
   * @param row desired row state
   * @param expectedVersion previously read version
   * @return affected rows */
  public int update(Row row, int expectedVersion) {
    return sql.update("UPDATE notebook_records SET updated_by_user_id=?,occurred_from=?,occurred_to=?,"
        + "title=?,note=?,template_version=?,values_json=?,lock_version=lock_version+1 "
        + "WHERE id=? AND owner_user_id=? AND lock_version=? AND deleted=FALSE",
        row.updatedByUserId(), Timestamp.from(row.from()), Timestamp.from(row.to()), row.title(),
        row.note(), row.templateVersion(), row.valuesJson(), row.id(), row.ownerUserId(), expectedVersion);
  }

  /** Inserts an immutable snapshot of a record before template upgrade.
   * @param row prior record state
   * @param actor editor account */
  public void revision(Row row, long actor) {
    sql.update("INSERT INTO notebook_record_revisions(record_id,revision,template_version,title,note,"
        + "occurred_from,occurred_to,values_json,edited_by_user_id) VALUES (?,?,?,?,?,?,?,?,?)",
        row.id(), row.lockVersion() + 1, row.templateVersion(), row.title(), row.note(),
        Timestamp.from(row.from()), Timestamp.from(row.to()), row.valuesJson(), actor);
  }

  /** Deletes one owner record after its images are handled by the file boundary.
   * @param owner account ID
   * @param id record ID
   * @return affected rows */
  public int delete(long owner, long id) {
    return sql.update("DELETE FROM notebook_records WHERE id=? AND owner_user_id=?", id, owner);
  }

  /** Reads one private image batch before record cascade deletion.
   * @param recordId record ID
   * @param afterId exclusive image ID cursor
   * @param limit maximum rows
   * @return image keys */
  public List<ImageKeyRow> imageKeysAfter(long recordId, long afterId, int limit) {
    return sql.query("SELECT id,storage_key FROM notebook_images WHERE record_id=? AND id>? "
        + "ORDER BY id LIMIT ?", (row, ignored) -> new ImageKeyRow(row.getLong(1),
        row.getString(2)), recordId, afterId, limit);
  }

  /** Reads a deterministic page of owner records intersecting the requested interval.
   * @param owner account ID
   * @param eventId event ID
   * @param from inclusive UTC lower bound
   * @param toExclusive exclusive UTC upper bound
   * @param containedFrom optional inclusive grant bound
   * @param containedToExclusive optional exclusive grant bound
   * @param limit rows including one lookahead
   * @param offset page offset
   * @return matching rows */
  public List<Row> page(long owner, long eventId, Instant from, Instant toExclusive,
      Instant containedFrom, Instant containedToExclusive, int limit, long offset) {
    String base = "SELECT * FROM notebook_records WHERE owner_user_id=? AND event_id=? "
        + "AND deleted=FALSE AND occurred_to>=? AND occurred_from<? ";
    if (containedFrom == null) return sql.query(base + "ORDER BY occurred_from,id LIMIT ? OFFSET ?",
        RECORD, owner, eventId, Timestamp.from(from), Timestamp.from(toExclusive), limit, offset);
    return sql.query(base + "AND occurred_from>=? AND occurred_to<? "
        + "ORDER BY occurred_from,id LIMIT ? OFFSET ?", RECORD, owner, eventId,
        Timestamp.from(from), Timestamp.from(toExclusive), Timestamp.from(containedFrom),
        Timestamp.from(containedToExclusive), limit, offset);
  }

  /** Reads one bounded keyset page of occurrence intervals without body values.
   * @param owner account ID
   * @param eventId event ID
   * @param from inclusive UTC lower bound
   * @param toExclusive exclusive UTC upper bound
   * @param containedFrom optional inclusive grant bound
   * @param containedToExclusive optional exclusive grant bound
   * @param afterId exclusive record ID cursor
   * @param limit maximum rows
   * @return matching occurrence intervals */
  public List<Interval> intervalsAfter(long owner, long eventId, Instant from, Instant toExclusive,
      Instant containedFrom, Instant containedToExclusive, long afterId, int limit) {
    String base = "SELECT id,occurred_from,occurred_to FROM notebook_records WHERE owner_user_id=? "
        + "AND event_id=? AND deleted=FALSE AND occurred_to>=? AND occurred_from<? AND id>? ";
    RowMapper<Interval> interval = (result, ignored) -> new Interval(result.getLong(1),
        result.getTimestamp(2).toInstant(), result.getTimestamp(3).toInstant());
    if (containedFrom == null) return sql.query(base + "ORDER BY id LIMIT ?", interval, owner,
        eventId, Timestamp.from(from), Timestamp.from(toExclusive), afterId, limit);
    return sql.query(base + "AND occurred_from>=? AND occurred_to<? ORDER BY id LIMIT ?",
        interval, owner, eventId, Timestamp.from(from), Timestamp.from(toExclusive), afterId,
        Timestamp.from(containedFrom), Timestamp.from(containedToExclusive), limit);
  }

  /** Records an action without duplicating field content.
   * @param actor acting account
   * @param owner owner account
   * @param eventId event ID
   * @param recordId record ID snapshot
   * @param action action code */
  public void audit(long actor, long owner, long eventId, long recordId, String action) {
    sql.update("INSERT INTO notebook_audit(actor_user_id,owner_user_id,event_id,record_id,action,outcome) "
        + "VALUES (?,?,?,?,?,'SUCCESS')", actor, owner, eventId, recordId, action);
  }
}
