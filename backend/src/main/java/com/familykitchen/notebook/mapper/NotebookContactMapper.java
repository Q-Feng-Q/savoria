package com.familykitchen.notebook.mapper;

import com.familykitchen.notebook.model.NotebookContactView;
import com.familykitchen.notebook.model.NotebookInviteView;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Account-only contact and one-time invitation persistence. */
@Repository
public class NotebookContactMapper {
  private final JdbcTemplate sql;

  /** Creates contact persistence.
   * @param sql project JDBC client */
  public NotebookContactMapper(JdbcTemplate sql) { this.sql = sql; }

  /** Checks a confirmed directed contact link.
   * @param user account
   * @param other target account
   * @return whether confirmed */
  public boolean exists(long user, long other) {
    Integer count = sql.queryForObject("SELECT COUNT(*) FROM notebook_contacts WHERE user_id=? "
        + "AND contact_user_id=?", Integer.class, user, other);
    return count != null && count > 0;
  }

  /** Checks a contact via a locking current read after ordered account locks.
   * Unlike a repeatable-read snapshot this observes a committed removal.
   * @param user account
   * @param other target account
   * @return whether the contact still exists */
  public boolean existsCurrent(long user, long other) {
    return !sql.query("SELECT id FROM notebook_contacts WHERE user_id=? AND contact_user_id=? "
        + "FOR UPDATE", (row, ignored) -> row.getLong(1), user, other).isEmpty();
  }

  /** Locks the two account rows in ascending ID order for contact/grant mutations.
   * Call inside one transaction; every paired operation uses this same order.
   * @param user first account
   * @param other second account */
  public void lockPair(long user, long other) {
    long first = Math.min(user, other);
    long second = Math.max(user, other);
    sql.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE", Long.class, first);
    if (second != first) {
      sql.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE", Long.class, second);
    }
  }

  /** Lists confirmed contacts of one account.
   * @param user account
   * @return contacts */
  public List<NotebookContactView> list(long user) {
    return sql.query("SELECT user_id,contact_user_id,source FROM notebook_contacts WHERE user_id=? "
        + "ORDER BY contact_user_id", (row, ignored) -> new NotebookContactView(row.getLong(1),
        row.getLong(2), row.getString(3)), user);
  }

  /** Inserts a token-hashed invitation.
   * @param inviter sender
   * @param invitee recipient
   * @param hash SHA-256 token hash
   * @param expires expiration instant
   * @return invitation ID */
  public long invite(long inviter, long invitee, String hash, Instant expires) {
    var key = new GeneratedKeyHolder();
    sql.update(connection -> {
      PreparedStatement statement = connection.prepareStatement("INSERT INTO notebook_contact_invites"
          + "(inviter_user_id,invitee_user_id,code_hash,status,expires_at) VALUES (?,?,?,'PENDING',?)",
          new String[] {"id"});
      statement.setLong(1, inviter); statement.setLong(2, invitee); statement.setString(3, hash);
      statement.setTimestamp(4, Timestamp.from(expires));
      return statement;
    }, key);
    if (key.getKey() == null) throw new IllegalStateException("Notebook invite key missing");
    return key.getKey().longValue();
  }

  /** Atomically consumes a pending, unexpired invite for its recipient.
   * @param invitee recipient
   * @param id invite ID
   * @param hash token hash
   * @param status accepted or rejected
   * @param now current instant
   * @return affected rows */
  public int respond(long invitee, long id, String hash, String status, Instant now) {
    return sql.update("UPDATE notebook_contact_invites SET status=?,responded_at=? WHERE id=? "
        + "AND invitee_user_id=? AND code_hash=? AND status='PENDING' AND expires_at>?",
        status, Timestamp.from(now), id, invitee, hash, Timestamp.from(now));
  }

  /** Reads invite metadata after a successful response.
   * @param id invite ID
   * @return invite or null */
  public NotebookInviteView invite(long id) {
    return sql.query("SELECT id,inviter_user_id,invitee_user_id,status,expires_at "
        + "FROM notebook_contact_invites WHERE id=?", (row, ignored) -> new NotebookInviteView(
        row.getLong(1), row.getLong(2), row.getLong(3), row.getString(4),
        row.getTimestamp(5).toInstant().atOffset(ZoneOffset.UTC)), id)
        .stream().findFirst().orElse(null);
  }

  /** Lists invitations received by one account without token material.
   * @param user recipient account
   * @return invitation metadata */
  public List<NotebookInviteView> received(long user) {
    return sql.query("SELECT id,inviter_user_id,invitee_user_id,status,expires_at "
        + "FROM notebook_contact_invites WHERE invitee_user_id=? ORDER BY id DESC",
        (row, ignored) -> new NotebookInviteView(row.getLong(1), row.getLong(2),
            row.getLong(3), row.getString(4), row.getTimestamp(5).toInstant().atOffset(ZoneOffset.UTC)),
        user);
  }

  /** Creates one directed confirmed contact link.
   * @param user account
   * @param other other account */
  public void add(long user, long other) {
    sql.update("INSERT INTO notebook_contacts(user_id,contact_user_id,source) VALUES (?,?,'LOGIN')",
        user, other);
  }

  /** Removes both directions of a contact pair.
   * @param user account
   * @param other other account
   * @return removed rows */
  public int removePair(long user, long other) {
    return sql.update("DELETE FROM notebook_contacts WHERE (user_id=? AND contact_user_id=?) "
        + "OR (user_id=? AND contact_user_id=?)", user, other, other, user);
  }

  /** Invalidates every pre-existing unconsumed invite in either direction.
   * @param user account
   * @param other former contact
   * @return invalidated invites */
  public int invalidatePendingPair(long user, long other) {
    return sql.update("UPDATE notebook_contact_invites SET status='REVOKED',responded_at=? "
        + "WHERE status='PENDING' AND ((inviter_user_id=? AND invitee_user_id=?) "
        + "OR (inviter_user_id=? AND invitee_user_id=?))", Timestamp.from(Instant.now()),
        user, other, other, user);
  }
}
