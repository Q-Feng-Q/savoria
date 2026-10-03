package com.familykitchen.notebook.mapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** SQL boundary for content-free notebook action history. */
@Repository
public class NotebookAuditMapper {
  /** Public audit projection: scalar IDs and action only.
   * @param id audit ID
   * @param actorUserId actor account ID
   * @param ownerUserId owning account ID
   * @param eventId event ID snapshot
   * @param recordId record ID snapshot
   * @param action action code
   * @param outcome result code
   * @param createdAt action instant */
  public record Entry(long id, Long actorUserId, Long ownerUserId, Long eventId, Long recordId,
      String action, String outcome, Instant createdAt) {}

  private final JdbcTemplate sql;

  /** Connects the audit SQL boundary.
   * @param sql JDBC client */
  public NotebookAuditMapper(JdbcTemplate sql) { this.sql = sql; }

  /** Persists only action metadata, never values or tokens.
   *
   * @param actor actor ID
   * @param owner owner ID
   * @param event event ID
   * @param record optional record ID
   *
   * @param action fixed action code */
  public void write(long actor, long owner, long event, Long record, String action) {
    sql.update("INSERT INTO notebook_audit(actor_user_id,owner_user_id,event_id,record_id,action,outcome) "
        + "VALUES (?,?,?,?,?,'SUCCESS')", actor, owner, event, record, action);
  }

  /** Reads one owner page in stable descending order.
   *
   * @param owner owner ID
   * @param event event ID
   * @param from inclusive instant
   *
   * @param to exclusive instant
   * @param limit bounded row count
   * @param offset page offset
   *
   * @return matching metadata */
  public List<Entry> page(long owner, long event, Instant from, Instant to, int limit, long offset) {
    return sql.query("SELECT id,actor_user_id,owner_user_id,event_id,record_id,action,outcome,created_at "
        + "FROM notebook_audit WHERE owner_user_id=? AND event_id=? AND created_at>=? "
        + "AND created_at<? ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?", (r, ignored) ->
        new Entry(r.getLong(1), (Long) r.getObject(2), (Long) r.getObject(3),
            (Long) r.getObject(4), (Long) r.getObject(5), r.getString(6), r.getString(7),
            r.getTimestamp(8).toInstant()), owner, event, Timestamp.from(from),
        Timestamp.from(to), limit, offset);
  }
}
