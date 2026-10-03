package com.familykitchen.notebook.mapper;

import com.familykitchen.notebook.model.NotebookDeleteImpact;
import com.familykitchen.notebook.model.NotebookEventView;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** SQL boundary for account-owned events and immutable template versions. */
@Repository
public class NotebookEventMapper {
  private static final RowMapper<NotebookEventView> EVENT = (row, ignored) -> new NotebookEventView(
      row.getLong("id"), row.getLong("owner_user_id"), row.getString("name"),
      row.getString("category"), row.getString("description"), row.getInt("sort_order"),
      row.getBoolean("starred"), row.getBoolean("archived"), row.getInt("current_template_version"));
  private final JdbcTemplate sql;

  /** Connects the notebook event persistence boundary.
   * @param sql project data source client */
  public NotebookEventMapper(JdbcTemplate sql) { this.sql = sql; }

  /** Inserts an account-owned event.
   * @param owner account ID
   * @param name display name
   * @param category optional category
   * @param description optional description
   * @return generated event ID */
  public long insertEvent(long owner, String name, String category, String description) {
    var key = new GeneratedKeyHolder();
    sql.update(connection -> {
      PreparedStatement insert = connection.prepareStatement(
          "INSERT INTO notebook_events(owner_user_id,name,category,description) VALUES (?,?,?,?)",
          new String[] {"id"});
      insert.setLong(1, owner); insert.setString(2, name);
      insert.setString(3, category); insert.setString(4, description);
      return insert;
    }, key);
    if (key.getKey() == null) throw new IllegalStateException("Notebook event key was not generated");
    return key.getKey().longValue();
  }

  /** Reads one event without granting access by itself.
   * @param id event ID
   * @return event or null */
  public NotebookEventView findEvent(long id) {
    return sql.query("SELECT id,owner_user_id,name,category,description,sort_order,starred,archived,"
        + "current_template_version FROM notebook_events WHERE id=?", EVENT, id)
        .stream().findFirst().orElse(null);
  }

  /** Locks an event while a template version is published.
   * @param id event ID
   * @return event or null */
  public NotebookEventView lockEvent(long id) {
    return sql.query("SELECT id,owner_user_id,name,category,description,sort_order,starred,archived,"
        + "current_template_version FROM notebook_events WHERE id=? FOR UPDATE", EVENT, id)
        .stream().findFirst().orElse(null);
  }

  /** Lists only the owner's events in display order.
   * @param owner account ID
   * @param includeArchived whether archived events are included
   * @return matching events */
  public List<NotebookEventView> listEvents(long owner, boolean includeArchived) {
    return sql.query("SELECT id,owner_user_id,name,category,description,sort_order,starred,archived,"
        + "current_template_version FROM notebook_events WHERE owner_user_id=? AND (?=TRUE OR archived=FALSE) "
        + "ORDER BY starred DESC,sort_order,id", EVENT, owner, includeArchived);
  }

  /** Updates an event through an owner-scoped write.
   * @param event event state
   * @return affected rows */
  public int updateEvent(NotebookEventView event) {
    return sql.update("UPDATE notebook_events SET name=?,category=?,description=?,sort_order=?,"
        + "starred=?,archived=? WHERE id=? AND owner_user_id=?", event.name(), event.category(),
        event.description(), event.sortOrder(), event.starred(), event.archived(), event.id(),
        event.ownerUserId());
  }

  /** Updates one owner's event order.
   * @param owner account ID
   * @param id event ID
   * @param order zero-based order
   * @return affected rows */
  public int updateOrder(long owner, long id, int order) {
    return sql.update("UPDATE notebook_events SET sort_order=? WHERE id=? AND owner_user_id=?",
        order, id, owner);
  }

  /** Immutable database template row.
   * @param eventId parent event
   * @param version template version
   * @param fieldsJson ordered field definitions */
  public record TemplateRow(long eventId, int version, String fieldsJson) {}

  /** Inserts a published template version.
   * @param eventId event ID
   * @param version new version number
   * @param fieldsJson immutable JSON definition
   * @param publisher account ID
   * @return affected rows */
  public int insertTemplate(long eventId, int version, String fieldsJson, long publisher) {
    return sql.update("INSERT INTO notebook_template_versions(event_id,version,fields_json,"
        + "published_by_user_id) VALUES (?,?,?,?)", eventId, version, fieldsJson, publisher);
  }

  /** Lists all historical versions of one event.
   * @param eventId event ID
   * @return ordered immutable rows */
  public List<TemplateRow> templates(long eventId) {
    return sql.query("SELECT event_id,version,fields_json FROM notebook_template_versions "
        + "WHERE event_id=? ORDER BY version", (row, ignored) -> new TemplateRow(
        row.getLong("event_id"), row.getInt("version"), row.getString("fields_json")), eventId);
  }

  /** Reads the current version after event ownership is checked.
   * @param eventId event ID
   * @param version version number
   * @return immutable row or null */
  public TemplateRow template(long eventId, int version) {
    return sql.query("SELECT event_id,version,fields_json FROM notebook_template_versions "
        + "WHERE event_id=? AND version=?", (row, ignored) -> new TemplateRow(
        row.getLong("event_id"), row.getInt("version"), row.getString("fields_json")),
        eventId, version).stream().findFirst().orElse(null);
  }

  /** Advances the event's current version after insertion.
   * @param owner owning account
   * @param eventId event ID
   * @param prior prior version
   * @param next newly published version
   * @return affected rows */
  public int advanceVersion(long owner, long eventId, int prior, int next) {
    return sql.update("UPDATE notebook_events SET current_template_version=? WHERE id=? "
        + "AND owner_user_id=? AND current_template_version=?", next, eventId, owner, prior);
  }

  /** Counts content affected by owner-confirmed event deletion.
   * @param eventId event ID
   * @return record, version and grant counts */
  public NotebookDeleteImpact deleteImpact(long eventId) {
    long records = sql.queryForObject("SELECT COUNT(*) FROM notebook_records WHERE event_id=?", Long.class, eventId);
    long versions = sql.queryForObject("SELECT COUNT(*) FROM notebook_template_versions WHERE event_id=?",
        Long.class, eventId);
    long grants = sql.queryForObject("SELECT COUNT(*) FROM notebook_grants WHERE event_id=?", Long.class, eventId);
    return new NotebookDeleteImpact(records, versions, grants);
  }

  /** Image metadata copied into the durable cleanup queue before cascade deletion.
   * @param id image row ID
   * @param storageKey private object key */
  public record ImageKeyRow(long id, String storageKey) {}

  /** Reads one bounded image batch using a stable keyset cursor.
   * @param eventId event ID
   * @param afterId exclusive image row cursor
   * @param limit maximum rows
   * @return image metadata */
  public List<ImageKeyRow> imageKeysAfter(long eventId, long afterId, int limit) {
    return sql.query("SELECT image.id,image.storage_key FROM notebook_images image "
        + "JOIN notebook_records record ON record.id=image.record_id "
        + "WHERE record.event_id=? AND image.id>? ORDER BY image.id LIMIT ?",
        (row, ignored) -> new ImageKeyRow(row.getLong("id"), row.getString("storage_key")),
        eventId, afterId, limit);
  }

  /** Reads one bounded batch of staged image keys before event cascade deletion.
   * @param eventId event ID
   * @param afterId exclusive cursor
   * @param limit maximum rows
   * @return staged image keys */
  public List<ImageKeyRow> stagedImageKeysAfter(long eventId, long afterId, int limit) {
    return sql.query("SELECT id,storage_key FROM notebook_staged_images"
        + " WHERE event_id=? AND id>? ORDER BY id LIMIT ?",
        (row, ignored) -> new ImageKeyRow(row.getLong("id"), row.getString("storage_key")),
        eventId, afterId, limit);
  }

  /** Saves image keys in a durable queue that survives event deletion.
   * @param eventId event ID snapshot
   * @param owner account ID snapshot
   * @param images one bounded image batch */
  public void enqueueImageCleanup(long eventId, long owner, List<ImageKeyRow> images) {
    sql.batchUpdate("INSERT INTO notebook_image_cleanup(event_id,owner_user_id,storage_key) VALUES (?,?,?)",
        new BatchPreparedStatementSetter() {
          @Override public void setValues(PreparedStatement statement, int index) throws SQLException {
            statement.setLong(1, eventId);
            statement.setLong(2, owner);
            statement.setString(3, images.get(index).storageKey());
          }
          @Override public int getBatchSize() { return images.size(); }
        });
  }

  /** One durable cleanup entry.
   * @param id queue ID
   * @param storageKey private object key */
  public record CleanupRow(long id, String storageKey) {}

  /** Reads pending keys for one deleted event after commit.
   * @param eventId deleted event ID snapshot
   * @param afterId exclusive queue cursor
   * @param limit maximum rows
   * @return pending rows */
  public List<CleanupRow> pendingCleanupForEvent(long eventId, long afterId, int limit) {
    return sql.query("SELECT id,storage_key FROM notebook_image_cleanup WHERE event_id=? AND id>? "
        + "ORDER BY id LIMIT ?", (row, ignored) -> new CleanupRow(row.getLong("id"),
        row.getString("storage_key")), eventId, afterId, limit);
  }

  /** Reads pending keys across events for scheduled retry.
   * @param afterId exclusive queue cursor
   * @param limit maximum rows
   * @return pending rows */
  public List<CleanupRow> pendingCleanupAfter(long afterId, int limit) {
    return sql.query("SELECT id,storage_key FROM notebook_image_cleanup WHERE id>? ORDER BY id LIMIT ?",
        (row, ignored) -> new CleanupRow(row.getLong("id"), row.getString("storage_key")),
        afterId, limit);
  }

  /** Removes successfully handled cleanup entries.
   * @param ids queue IDs from one bounded batch */
  public void removeCleanup(List<Long> ids) {
    sql.batchUpdate("DELETE FROM notebook_image_cleanup WHERE id=?", new BatchPreparedStatementSetter() {
      @Override public void setValues(PreparedStatement statement, int index) throws SQLException {
        statement.setLong(1, ids.get(index));
      }
      @Override public int getBatchSize() { return ids.size(); }
    });
  }

  /** Revokes event grants before removing the event tree.
   * @param eventId event ID
   * @return affected grants */
  public int revokeGrants(long eventId) {
    return sql.update("UPDATE notebook_grants SET status='REVOKED' WHERE event_id=?", eventId);
  }

  /** Deletes the owner event and cascaded notebook content.
   * @param owner account ID
   * @param eventId event ID
   * @return affected events */
  public int deleteEvent(long owner, long eventId) {
    return sql.update("DELETE FROM notebook_events WHERE id=? AND owner_user_id=?", eventId, owner);
  }

  /** Records an action using scalar identifiers without notebook content.
   * @param actor acting account
   * @param owner owning account
   * @param eventId event ID
   * @param action action code
   * @return affected audit rows */
  public int audit(long actor, long owner, long eventId, String action) {
    return sql.update("INSERT INTO notebook_audit(actor_user_id,owner_user_id,event_id,action,outcome) "
        + "VALUES (?,?,?,?,'SUCCESS')", actor, owner, eventId, action);
  }
}
