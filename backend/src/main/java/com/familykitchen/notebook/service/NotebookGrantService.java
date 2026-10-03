package com.familykitchen.notebook.service;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.notebook.NotebookRangePolicy;
import com.familykitchen.notebook.mapper.NotebookContactMapper;
import com.familykitchen.notebook.mapper.NotebookEventMapper;
import com.familykitchen.notebook.mapper.NotebookGrantMapper;
import com.familykitchen.notebook.model.NotebookGrantRequest;
import com.familykitchen.notebook.model.NotebookGrantView;
import com.familykitchen.system.service.SystemSettingService;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owner-managed grants with independent data and validity intervals. */
@Service
public class NotebookGrantService {
  private final NotebookEventMapper events;
  private final NotebookContactMapper contacts;
  private final NotebookGrantMapper grants;
  private final NotebookRangePolicy ranges;

  /** Creates a grant service using the live platform month limit.
   * @param events event persistence
   * @param contacts confirmed contacts
   * @param grants grant persistence
   * @param settings platform settings */
  @Autowired
  public NotebookGrantService(NotebookEventMapper events, NotebookContactMapper contacts,
      NotebookGrantMapper grants, SystemSettingService settings) {
    this(events, contacts, grants, new NotebookRangePolicy(settings::notebookMaxQueryMonths));
  }

  /** Creates a grant service with an explicit range policy for isolated tests.
   * @param events event persistence
   * @param contacts confirmed contacts
   * @param grants grant persistence
   * @param ranges current month policy */
  public NotebookGrantService(NotebookEventMapper events, NotebookContactMapper contacts,
      NotebookGrantMapper grants, NotebookRangePolicy ranges) {
    this.events = events; this.contacts = contacts; this.grants = grants; this.ranges = ranges;
  }

  /** Lists grants only to the owning account.
   * @param actor authenticated owner
   * @param eventId event ID
   * @return grants */
  public List<NotebookGrantView> list(long actor, long eventId) {
    requireOwner(actor, eventId);
    return grants.forEvent(eventId);
  }

  /** Creates a grant for a confirmed account contact.
   * @param actor authenticated owner
   * @param eventId owner event ID
   * @param request complete grant draft
   * @return created grant */
  @Transactional
  public NotebookGrantView create(long actor, long eventId, NotebookGrantRequest request) {
    if (request == null || request.granteeUserId() < 1) throw bad("Invalid grant recipient");
    lockPair(actor, request.granteeUserId());
    requireOwner(actor, eventId);
    validate(actor, request);
    try {
      return grants.find(grants.insert(actor, eventId, normalized(request)));
    } catch (DuplicateKeyException duplicate) {
      throw new BusinessException(ErrorCode.STATE_CONFLICT, "Notebook grant already exists");
    }
  }

  /** Replaces one owner grant, including its independent permission switches.
   * @param actor authenticated owner
   * @param grantId grant ID
   * @param request complete replacement
   * @return updated grant */
  @Transactional
  public NotebookGrantView update(long actor, long grantId, NotebookGrantRequest request) {
    if (request == null || request.granteeUserId() < 1) throw bad("Invalid grant recipient");
    lockPair(actor, request.granteeUserId());
    var old = requireOwnedGrant(actor, grantId);
    if (request.granteeUserId() != old.granteeUserId()) throw bad("Grant recipient cannot change");
    validate(actor, request);
    if (grants.update(actor, grantId, normalized(request)) != 1) throw missing();
    return grants.find(grantId);
  }

  /** Revokes an owner grant immediately for subsequent requests.
   * @param actor authenticated owner
   * @param grantId grant ID */
  @Transactional
  public void revoke(long actor, long grantId) {
    requireOwnedGrant(actor, grantId);
    if (grants.revoke(actor, grantId) != 1) throw missing();
  }

  private void validate(long actor, NotebookGrantRequest request) {
    if (request == null || request.granteeUserId() == actor || request.granteeUserId() < 1) {
      throw bad("Invalid grant recipient");
    }
    try {
      if (!contacts.existsCurrent(actor, request.granteeUserId())
          || !contacts.existsCurrent(request.granteeUserId(), actor)) throw missing();
    } catch (CannotAcquireLockException concurrentRemoval) {
      throw new BusinessException(ErrorCode.STATE_CONFLICT, "Notebook contact changed; retry the grant");
    }
    ranges.requireAllowed(request.dataFrom(), request.dataTo());
    if (request.dataTimeZone() == null || !ZoneId.getAvailableZoneIds().contains(request.dataTimeZone())) {
      throw bad("Valid IANA grant dataTimeZone is required");
    }
    if (request.dataTo().equals(java.time.LocalDate.MAX)
        || request.validFrom() == null || request.validTo() == null
        || !request.validFrom().toInstant().isBefore(request.validTo().toInstant())
        || !request.validTo().toInstant().isAfter(Instant.now())) throw bad("Invalid grant validity period");
  }

  private void lockPair(long actor, long grantee) {
    try { contacts.lockPair(actor, grantee); }
    catch (EmptyResultDataAccessException unknownAccount) { throw missing(); }
    catch (CannotAcquireLockException concurrentChange) {
      throw new BusinessException(ErrorCode.STATE_CONFLICT, "Notebook contact changed; retry the grant");
    }
  }

  private NotebookGrantRequest normalized(NotebookGrantRequest request) {
    return new NotebookGrantRequest(request.granteeUserId(), request.dataFrom(), request.dataTo(),
        request.dataTimeZone(), request.validFrom(), request.validTo(),
        Boolean.TRUE.equals(request.canCreate()), Boolean.TRUE.equals(request.canEdit()),
        Boolean.TRUE.equals(request.canExport()));
  }

  private void requireOwner(long actor, long eventId) {
    var event = events.findEvent(eventId);
    if (event == null || event.ownerUserId() != actor) throw missing();
  }

  private NotebookGrantView requireOwnedGrant(long actor, long grantId) {
    var grant = grants.find(grantId);
    if (grant == null || grant.ownerUserId() != actor) throw missing();
    return grant;
  }

  private static BusinessException bad(String message) { return new BusinessException(ErrorCode.BAD_REQUEST, message); }
  private static BusinessException missing() { return new BusinessException(ErrorCode.NOT_FOUND, "Notebook grant not found"); }
}
