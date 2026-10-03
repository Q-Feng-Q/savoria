package com.familykitchen.notebook.mapper;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Private notebook image metadata and durable deletion queue. */
@Repository
public class NotebookImageMapper {
  /** Image metadata without a public URL or file content.
   * @param id image ID
   * @param recordId parent record ID
   * @param ownerUserId owning account ID
   * @param storageKey private file key
   * @param originalName upload display name
   * @param contentType validated media type
   * @param byteSize byte count */
  public record Image(long id, long recordId, long ownerUserId, String storageKey,
      String originalName, String contentType, long byteSize) {}

  /** Event-scoped image waiting for a record to bind it.
   * @param id staged row ID
   * @param eventId event ID
   * @param ownerUserId event owner
   * @param uploadedByUserId uploader
   * @param storageKey private file key
   * @param originalName display name
   * @param contentType MIME type
   * @param byteSize file size */
  public record Staged(long id, long eventId, long ownerUserId, long uploadedByUserId,
      String storageKey, String originalName, String contentType, long byteSize) {}

  private final JdbcTemplate sql;
  /** Connects image metadata persistence.
   * @param sql JDBC client */
  public NotebookImageMapper(JdbcTemplate sql) { this.sql = sql; }

  /** Locks an event row before writing a staged or attached image.
   * @param eventId event ID */
  public void lockEvent(long eventId) {
    if (sql.query("SELECT id FROM notebook_events WHERE id=? FOR UPDATE",
        (row, ignored) -> row.getLong(1), eventId).isEmpty()) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Notebook event not found");
    }
  }

  /** Adds metadata for an authorized record image.
   *
   * @param record record ID
   * @param owner owner ID
   * @param key private flat UUID key
   *
   * @param name original display name
   * @param type validated MIME
   * @param size byte count
   *
   * @return image ID */
  public long insert(long record, long owner, String key, String name, String type, long size) {
    var holder = new GeneratedKeyHolder();
    sql.update(connection -> {
      PreparedStatement statement = connection.prepareStatement("INSERT INTO notebook_images"
          + "(record_id,owner_user_id,storage_key,original_name,content_type,byte_size) "
          + "VALUES (?,?,?,?,?,?)", new String[] {"id"});
      statement.setLong(1, record); statement.setLong(2, owner); statement.setString(3, key);
      statement.setString(4, name); statement.setString(5, type); statement.setLong(6, size);
      return statement;
    }, holder);
    return holder.getKey().longValue();
  }

  /** Stores a temporary image until the uploader saves a record.
   * @param eventId event ID
   * @param owner event owner
   * @param uploader uploading account
   * @param key private file key
   * @param name display name
   * @param type MIME type
   * @param size file size
   * @param expiresAt expiry instant
   * @return staged image ID */
  public long stage(long eventId, long owner, long uploader, String key, String name,
      String type, long size, Instant expiresAt) {
    var holder = new GeneratedKeyHolder();
    sql.update(connection -> {
      PreparedStatement statement = connection.prepareStatement("INSERT INTO notebook_staged_images"
          + "(event_id,owner_user_id,uploaded_by_user_id,storage_key,original_name,content_type,byte_size,expires_at)"
          + " VALUES (?,?,?,?,?,?,?,?)", new String[] {"id"});
      statement.setLong(1, eventId); statement.setLong(2, owner); statement.setLong(3, uploader);
      statement.setString(4, key); statement.setString(5, name); statement.setString(6, type);
      statement.setLong(7, size); statement.setTimestamp(8, java.sql.Timestamp.from(expiresAt));
      return statement;
    }, holder);
    return holder.getKey().longValue();
  }

  /** Binds a staged key, or verifies an already attached key belongs to this record.
   * @param eventId event ID
   * @param recordId record ID
   * @param owner event owner
   * @param actor account writing the record
   * @param key submitted image key */
  public void requireAndBind(long eventId, long recordId, long owner, long actor, String key) {
    var attached = sql.query("SELECT id FROM notebook_images WHERE record_id=? AND owner_user_id=?"
        + " AND storage_key=? FOR UPDATE", (r, ignored) -> r.getLong(1), recordId, owner, key);
    if (!attached.isEmpty()) return;
    var pending = sql.query("SELECT id,event_id,owner_user_id,uploaded_by_user_id,storage_key,"
        + "original_name,content_type,byte_size FROM notebook_staged_images WHERE storage_key=?"
        + " AND expires_at>CURRENT_TIMESTAMP FOR UPDATE", (r, ignored) -> new Staged(r.getLong(1),
        r.getLong(2), r.getLong(3), r.getLong(4), r.getString(5), r.getString(6),
        r.getString(7), r.getLong(8)), key);
    if (pending.size() != 1 || pending.get(0).eventId() != eventId
        || pending.get(0).ownerUserId() != owner || pending.get(0).uploadedByUserId() != actor) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Image is not available for this record");
    }
    var staged = pending.get(0);
    insert(recordId, owner, key, staged.originalName(), staged.contentType(), staged.byteSize());
    sql.update("DELETE FROM notebook_staged_images WHERE id=?", staged.id());
  }

  /** Retrieves one expired stage batch for durable cleanup.
   * @param limit maximum batch size
   * @return expired rows */
  public List<Staged> expired(int limit) {
    return sql.query("SELECT id,event_id,owner_user_id,uploaded_by_user_id,storage_key,"
        + "original_name,content_type,byte_size FROM notebook_staged_images"
        + " WHERE expires_at<=CURRENT_TIMESTAMP ORDER BY id LIMIT ? FOR UPDATE",
        (r, ignored) -> new Staged(r.getLong(1), r.getLong(2), r.getLong(3), r.getLong(4),
            r.getString(5), r.getString(6), r.getString(7), r.getLong(8)), limit);
  }

  /** Moves one expired stage to the durable file-cleanup queue.
   * @param stage expired row */
  public void expire(Staged stage) {
    sql.update("INSERT INTO notebook_image_cleanup(event_id,owner_user_id,storage_key) VALUES (?,?,?)",
        stage.eventId(), stage.ownerUserId(), stage.storageKey());
    sql.update("DELETE FROM notebook_staged_images WHERE id=?", stage.id());
  }


  /** Gets one image without authorizing it.
   * @param id image ID
   * @return image or null */
  public Image find(long id) {
    return sql.query("SELECT id,record_id,owner_user_id,storage_key,original_name,content_type,byte_size "
        + "FROM notebook_images WHERE id=?", (r, ignored) -> new Image(r.getLong(1), r.getLong(2),
        r.getLong(3), r.getString(4), r.getString(5), r.getString(6), r.getLong(7)), id)
        .stream().findFirst().orElse(null);
  }

  /** Lists metadata for a record only.
   * @param record record ID
   * @return ordered metadata */
  public List<Image> forRecord(long record) {
    return sql.query("SELECT id,record_id,owner_user_id,storage_key,original_name,content_type,byte_size "
        + "FROM notebook_images WHERE record_id=? ORDER BY id", (r, ignored) ->
        new Image(r.getLong(1), r.getLong(2), r.getLong(3), r.getString(4), r.getString(5),
            r.getString(6), r.getLong(7)), record);
  }

  /** Queues private-file cleanup before metadata deletion.
   * @param image image snapshot
   *
   * @param event event snapshot */
  public void enqueueCleanup(Image image, long event) {
    sql.update("INSERT INTO notebook_image_cleanup(event_id,owner_user_id,storage_key) VALUES (?,?,?)",
        event, image.ownerUserId(), image.storageKey());
  }

  /** Deletes one metadata row.
   * @param id image ID
   * @return affected rows */
  public int delete(long id) { return sql.update("DELETE FROM notebook_images WHERE id=?", id); }
}
