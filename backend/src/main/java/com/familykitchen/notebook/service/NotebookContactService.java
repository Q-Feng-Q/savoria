package com.familykitchen.notebook.service;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.notebook.mapper.NotebookContactMapper;
import com.familykitchen.notebook.mapper.NotebookGrantMapper;
import com.familykitchen.notebook.model.NotebookContactView;
import com.familykitchen.notebook.model.NotebookInviteCreated;
import com.familykitchen.notebook.model.NotebookInviteView;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** One-time contact invitations independent of family membership. */
@Service
public class NotebookContactService {
  private static final SecureRandom RANDOM = new SecureRandom();
  private final NotebookContactMapper contacts;
  private final NotebookGrantMapper grants;
  private final NotebookIdentityLookup identities;

  /** Creates account-only contact management.
   * @param contacts contact persistence
   * @param grants grants to revoke on removal
   * @param identities narrow login identifier lookup */
  public NotebookContactService(NotebookContactMapper contacts, NotebookGrantMapper grants,
      NotebookIdentityLookup identities) {
    this.contacts = contacts; this.grants = grants; this.identities = identities;
  }

  /** Lists confirmed contacts of the account.
   * @param account authenticated account
   * @return confirmed contacts */
  public List<NotebookContactView> list(long account) { return contacts.list(account); }

  /** Lists received invites without token material.
   * @param account authenticated recipient
   * @return invite metadata */
  public List<NotebookInviteView> received(long account) { return contacts.received(account); }

  /** Creates a recipient-bound one-time invite and returns its secret once.
   * @param inviter authenticated sender
   * @param identifier recipient login identifier
   * @return created invite and plaintext token */
  public NotebookInviteCreated invite(long inviter, String identifier) {
    if (identifier == null || identifier.isBlank() || identifier.length() > 255) {
      throw bad("Invalid contact identifier");
    }
    Long invitee = identities.findUserId(identifier.strip().toLowerCase(Locale.ROOT));
    if (invitee == null) throw missing();
    if (invitee == inviter) throw bad("Cannot invite your own account");
    if (contacts.exists(inviter, invitee)) throw conflict();
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    Instant expires = Instant.now().plusSeconds(7 * 24 * 60 * 60);
    long id = contacts.invite(inviter, invitee, hash(token), expires);
    return new NotebookInviteCreated(id, invitee, token, expires.atOffset(ZoneOffset.UTC));
  }

  /** Accepts a recipient-bound, unexpired invite exactly once.
   * @param invitee authenticated recipient
   * @param id invite ID
   * @param token one-time secret */
  @Transactional
  public void accept(long invitee, long id, String token) {
    respond(invitee, id, token, "ACCEPTED");
    var invite = contacts.invite(id);
    if (invite == null) throw missing();
    if (!contacts.exists(invite.inviterUserId(), invitee)) {
      contacts.add(invite.inviterUserId(), invitee);
      contacts.add(invitee, invite.inviterUserId());
    }
  }

  /** Rejects a recipient-bound, unexpired invite exactly once.
   * @param invitee authenticated recipient
   * @param id invite ID
   * @param token one-time secret */
  @Transactional
  public void reject(long invitee, long id, String token) { respond(invitee, id, token, "REJECTED"); }

  /** Removes both contact directions and revokes their event grants.
   * @param account authenticated account
   * @param other former contact */
  @Transactional
  public void remove(long account, long other) {
    if (!contacts.exists(account, other)) throw missing();
    grants.revokePair(account, other);
    contacts.removePair(account, other);
  }

  private void respond(long invitee, long id, String token, String status) {
    if (token == null || token.isBlank() || token.length() > 256
        || contacts.respond(invitee, id, hash(token), status, Instant.now()) != 1) throw missing();
  }

  private static String hash(String token) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException failure) {
      throw new IllegalStateException("SHA-256 unavailable", failure);
    }
  }

  private static BusinessException bad(String message) { return new BusinessException(ErrorCode.BAD_REQUEST, message); }
  private static BusinessException missing() { return new BusinessException(ErrorCode.NOT_FOUND, "Notebook invite or contact not found"); }
  private static BusinessException conflict() { return new BusinessException(ErrorCode.STATE_CONFLICT, "Notebook contact already exists"); }
}
