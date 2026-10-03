package com.familykitchen.notebook.mapper;

import com.familykitchen.notebook.model.NotebookGrantRequest;
import com.familykitchen.notebook.model.NotebookGrantView;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** SQL boundary for event-scoped notebook grants. */
@Repository
public class NotebookGrantMapper {
  private static final String COLUMNS = "id,event_id,owner_user_id,grantee_user_id,data_from,data_to,"
      + "data_time_zone,valid_from,valid_to,can_create,can_edit,can_export,status";
  private static final RowMapper<NotebookGrantView> GRANT = (row, ignored) -> new NotebookGrantView(
      row.getLong("id"), row.getLong("event_id"), row.getLong("owner_user_id"),
      row.getLong("grantee_user_id"), row.getDate("data_from").toLocalDate(),
      row.getDate("data_to").toLocalDate(), row.getString("data_time_zone"),
      row.getTimestamp("valid_from").toInstant().atOffset(ZoneOffset.UTC),
      row.getTimestamp("valid_to").toInstant().atOffset(ZoneOffset.UTC),
      row.getBoolean("can_create"), row.getBoolean("can_edit"), row.getBoolean("can_export"),
      row.getString("status"));
  private final JdbcTemplate sql;

  /** Creates grant persistence.
   * @param sql project JDBC client */
  public NotebookGrantMapper(JdbcTemplate sql) { this.sql = sql; }

  /** Inserts a new event grant.
   * @param owner event owner
   * @param eventId event ID
   * @param request validated grant
   * @return generated ID */
  public long insert(long owner, long eventId, NotebookGrantRequest request) {
    var key = new GeneratedKeyHolder();
    sql.update(connection -> {
      PreparedStatement statement = connection.prepareStatement("INSERT INTO notebook_grants"
          + "(event_id,owner_user_id,grantee_user_id,data_from,data_to,data_time_zone,"
          + "valid_from,valid_to,can_create,can_edit,can_export) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
          new String[] {"id"});
      statement.setLong(1, eventId); statement.setLong(2, owner);
      statement.setLong(3, request.granteeUserId());
      statement.setDate(4, java.sql.Date.valueOf(request.dataFrom()));
      statement.setDate(5, java.sql.Date.valueOf(request.dataTo()));
      statement.setString(6, request.dataTimeZone());
      statement.setTimestamp(7, Timestamp.from(request.validFrom().toInstant()));
      statement.setTimestamp(8, Timestamp.from(request.validTo().toInstant()));
      statement.setBoolean(9, request.canCreate());
      statement.setBoolean(10, request.canEdit());
      statement.setBoolean(11, request.canExport());
      return statement;
    }, key);
    if (key.getKey() == null) throw new IllegalStateException("Notebook grant key missing");
    return key.getKey().longValue();
  }

  /** Reads a grant by ID, without granting access by itself.
   * @param id grant ID
   * @return grant or null */
  public NotebookGrantView find(long id) {
    return sql.query("SELECT " + COLUMNS + " FROM notebook_grants WHERE id=?", GRANT, id)
        .stream().findFirst().orElse(null);
  }

  /** Reads a grant for a grantee and event.
   * @param eventId event ID
   * @param grantee recipient account
   * @return grant or null */
  public NotebookGrantView forGrantee(long eventId, long grantee) {
    return sql.query("SELECT " + COLUMNS + " FROM notebook_grants WHERE event_id=? "
        + "AND grantee_user_id=?", GRANT, eventId, grantee).stream().findFirst().orElse(null);
  }

  /** Lists all grants managed by an event owner.
   * @param eventId event ID
   * @return grants */
  public List<NotebookGrantView> forEvent(long eventId) {
    return sql.query("SELECT " + COLUMNS + " FROM notebook_grants WHERE event_id=? ORDER BY id",
        GRANT, eventId);
  }

  /** Lists grants addressed to one account; policy checks validity and contacts.
   * @param grantee account
   * @return grant candidates */
  public List<NotebookGrantView> forAccount(long grantee) {
    return sql.query("SELECT " + COLUMNS + " FROM notebook_grants WHERE grantee_user_id=? "
        + "ORDER BY id", GRANT, grantee);
  }

  /** Replaces a grant while keeping event and recipients immutable.
   * @param owner event owner
   * @param id grant ID
   * @param request validated replacement
   * @return affected rows */
  public int update(long owner, long id, NotebookGrantRequest request) {
    return sql.update("UPDATE notebook_grants SET data_from=?,data_to=?,data_time_zone=?,"
        + "valid_from=?,valid_to=?,can_create=?,can_edit=?,can_export=?,status='ACTIVE' "
        + "WHERE id=? AND owner_user_id=? AND grantee_user_id=?",
        java.sql.Date.valueOf(request.dataFrom()), java.sql.Date.valueOf(request.dataTo()),
        request.dataTimeZone(), Timestamp.from(request.validFrom().toInstant()),
        Timestamp.from(request.validTo().toInstant()), request.canCreate(), request.canEdit(),
        request.canExport(), id, owner, request.granteeUserId());
  }

  /** Revokes an owner grant.
   * @param owner account
   * @param id grant ID
   * @return affected rows */
  public int revoke(long owner, long id) {
    return sql.update("UPDATE notebook_grants SET status='REVOKED' WHERE id=? AND owner_user_id=?",
        id, owner);
  }

  /** Revokes all grants between two accounts after contact removal.
   * @param user account
   * @param other former contact
   * @return affected rows */
  public int revokePair(long user, long other) {
    return sql.update("UPDATE notebook_grants SET status='REVOKED' WHERE "
        + "(owner_user_id=? AND grantee_user_id=?) OR (owner_user_id=? AND grantee_user_id=?)",
        user, other, other, user);
  }
}
