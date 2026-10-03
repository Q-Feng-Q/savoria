package com.familykitchen.notebook.service;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.notebook.mapper.NotebookContactMapper;
import com.familykitchen.notebook.mapper.NotebookEventMapper;
import com.familykitchen.notebook.mapper.NotebookGrantMapper;
import com.familykitchen.notebook.model.NotebookGrantView;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Component;

/** Account- and grant-scoped access boundary for private notebook content. */
@Component
public class NotebookAccessPolicy {
  /** Authorized record interval bounds, or unbounded dates for an owner.
   * @param ownerUserId owning account
   * @param dataFrom inclusive instant, null for owner
   * @param dataToExclusive exclusive instant, null for owner
   * @param owner whether the actor owns the event */
  public record Scope(long ownerUserId, Instant dataFrom, Instant dataToExclusive, boolean owner) {
    /** Tests complete containment of one record occurrence.
     * @param from record start
     * @param to record end
     * @return true when the record is fully visible */
    public boolean contains(Instant from, Instant to) {
      return owner || !from.isBefore(dataFrom) && to.isBefore(dataToExclusive);
    }
  }

  private final NotebookEventMapper events;
  private final NotebookContactMapper contacts;
  private final NotebookGrantMapper grants;

  /** Creates the account and grant access boundary.
   * @param events owner event source
   * @param contacts confirmed contacts
   * @param grants active grants */
  public NotebookAccessPolicy(NotebookEventMapper events, NotebookContactMapper contacts,
      NotebookGrantMapper grants) {
    this.events = events; this.contacts = contacts; this.grants = grants;
  }

  /** Lists only currently valid, contact-confirmed shares for a recipient.
   * @param actor authenticated account
   * @return active grants */
  public List<NotebookGrantView> shared(long actor) {
    return grants.forAccount(actor).stream().filter(grant -> active(actor, grant, false)).toList();
  }

  /** Requires read access to an interval intersecting the requested local-date window.
   * @param actor authenticated account
   * @param eventId event ID
   * @param from inclusive requested date
   * @param to inclusive requested date
   * @param timeZone requested IANA calendar zone
   * @return owner or grant scope */
  public Scope requireRead(long actor, long eventId, LocalDate from, LocalDate to, String timeZone) {
    Scope scope = scope(actor, eventId, Action.READ);
    if (!scope.owner()) {
      ZoneId zone = zone(timeZone);
      Instant requestedFrom = from.atStartOfDay(zone).toInstant();
      Instant requestedTo = to.plusDays(1).atStartOfDay(zone).toInstant();
      if (!requestedFrom.isBefore(scope.dataToExclusive())
          || !requestedTo.isAfter(scope.dataFrom())) throw missing();
    }
    return scope;
  }

  /** Requires grant export permission independent of edit or create.
   * @param actor authenticated account
   * @param eventId event ID
   * @param from inclusive requested date
   * @param to inclusive requested date
   * @param timeZone requested IANA calendar zone
   * @return owner or export grant scope */
  public Scope requireExport(long actor, long eventId, LocalDate from, LocalDate to, String timeZone) {
    Scope scope = scope(actor, eventId, Action.EXPORT);
    if (!scope.owner()) {
      ZoneId zone = zone(timeZone);
      if (!from.atStartOfDay(zone).toInstant().isBefore(scope.dataToExclusive())
          || !to.plusDays(1).atStartOfDay(zone).toInstant().isAfter(scope.dataFrom())) throw missing();
    }
    return scope;
  }

  /** Requires permission to add a record fully inside granted data dates.
   * @param actor authenticated account
   * @param eventId event ID
   * @param from record start
   * @param to record end
   * @return owner or create grant scope */
  public Scope requireCreate(long actor, long eventId, OffsetDateTime from, OffsetDateTime to) {
    return contained(scope(actor, eventId, Action.CREATE), from, to);
  }

  /** Allows an event-scoped temporary upload before record dates are chosen.
   * @param actor uploading account
   * @param eventId event ID
   * @return owner or active create-grant scope */
  public Scope requireCreateCapability(long actor, long eventId) {
    return scope(actor, eventId, Action.CREATE);
  }

  /** Requires permission to edit a record fully inside granted data dates.
   * @param actor authenticated account
   * @param eventId event ID
   * @param from record start
   * @param to record end
   * @return owner or edit grant scope */
  public Scope requireEdit(long actor, long eventId, OffsetDateTime from, OffsetDateTime to) {
    return contained(scope(actor, eventId, Action.EDIT), from, to);
  }

  /** Requires visibility of an entire stored occurrence.
   * @param actor authenticated account
   * @param eventId event ID
   * @param from stored start instant
   * @param to stored end instant
   * @return owner or read grant scope */
  public Scope requireRecordRead(long actor, long eventId, Instant from, Instant to) {
    Scope scope = scope(actor, eventId, Action.READ);
    if (!scope.contains(from, to)) throw missing();
    return scope;
  }

  /** Requires owner-only management or deletion.
   * @param actor authenticated account
   * @param eventId event ID
   * @return owner scope */
  public Scope requireOwner(long actor, long eventId) {
    var event = events.findEvent(eventId);
    if (event == null || event.ownerUserId() != actor) throw missing();
    return new Scope(actor, null, null, true);
  }

  private Scope contained(Scope scope, OffsetDateTime from, OffsetDateTime to) {
    if (from == null || to == null || !scope.contains(from.toInstant(), to.toInstant())) throw missing();
    return scope;
  }

  private Scope scope(long actor, long eventId, Action action) {
    var event = events.findEvent(eventId);
    if (event == null) throw missing();
    boolean mutation = action == Action.CREATE || action == Action.EDIT;
    if (event.ownerUserId() == actor) {
      if (mutation && events.lockEvent(eventId) == null) throw missing();
      return new Scope(actor, null, null, true);
    }
    try {
      if (mutation) {
        contacts.lockPair(event.ownerUserId(), actor);
        if (events.lockEvent(eventId) == null) throw missing();
      }
      var grant = action == Action.READ ? grants.forGrantee(eventId, actor)
          : grants.forGranteeCurrent(eventId, actor);
      if (grant == null || !active(actor, grant, action != Action.READ)
          || action == Action.CREATE && !grant.canCreate()
          || action == Action.EDIT && !grant.canEdit()
          || action == Action.EXPORT && !grant.canExport()) throw missing();
      ZoneId zone = zone(grant.dataTimeZone());
      return new Scope(event.ownerUserId(), grant.dataFrom().atStartOfDay(zone).toInstant(),
          grant.dataTo().plusDays(1).atStartOfDay(zone).toInstant(), false);
    } catch (CannotAcquireLockException changed) {
      throw new BusinessException(ErrorCode.STATE_CONFLICT, "Notebook sharing changed; retry");
    }
  }

  private boolean active(long actor, NotebookGrantView grant, boolean current) {
    Instant now = Instant.now();
    return grant.granteeUserId() == actor && grant.status().equals("ACTIVE")
        && !now.isBefore(grant.validFrom().toInstant()) && now.isBefore(grant.validTo().toInstant())
        && (current ? contacts.existsCurrent(grant.ownerUserId(), actor)
            && contacts.existsCurrent(actor, grant.ownerUserId())
            : contacts.exists(grant.ownerUserId(), actor)
            && contacts.exists(actor, grant.ownerUserId()));
  }

  private static ZoneId zone(String value) {
    if (value == null || !ZoneId.getAvailableZoneIds().contains(value)) throw missing();
    return ZoneId.of(value);
  }

  /** Independent record capabilities checked by a share grant. */
  private enum Action {
    /** Read grant contents. */ READ,
    /** Create a new record. */ CREATE,
    /** Edit an existing record. */ EDIT,
    /** Copy or export grant contents. */ EXPORT
  }
  private static BusinessException missing() { return new BusinessException(ErrorCode.NOT_FOUND, "Notebook content not found"); }
}
