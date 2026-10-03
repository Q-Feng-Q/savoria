package com.familykitchen.notebook.mapper;

import java.sql.PreparedStatement;
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

  private final JdbcTemplate sql;
  /** Connects image metadata persistence.
   * @param sql JDBC client */
  public NotebookImageMapper(JdbcTemplate sql) { this.sql = sql; }

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
