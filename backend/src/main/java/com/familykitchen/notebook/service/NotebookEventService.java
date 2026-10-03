package com.familykitchen.notebook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.notebook.mapper.NotebookEventMapper;
import com.familykitchen.notebook.model.NotebookDeleteImpact;
import com.familykitchen.notebook.model.NotebookEventCreate;
import com.familykitchen.notebook.model.NotebookEventPatch;
import com.familykitchen.notebook.model.NotebookEventView;
import com.familykitchen.notebook.model.NotebookField;
import com.familykitchen.notebook.model.NotebookTemplateRequest;
import com.familykitchen.notebook.model.NotebookTemplateView;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.LongFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Account scoped notebook event lifecycle and immutable template publishing. */
@Service
public class NotebookEventService {
  private static final Logger log = LoggerFactory.getLogger(NotebookEventService.class);
  private static final int IMAGE_BATCH_SIZE = 100;
  private final NotebookEventMapper mapper;
  private final NotebookTemplateValidator validator;
  private final ObjectMapper json;
  private final TransactionTemplate cleanupTransactions;
  @Value("${family-kitchen.notebook.private-root:./data/notebook-private}")
  private String privateRoot = "./data/notebook-private";
  @Value("${family-kitchen.file-storage.local-root:./uploads}")
  private String publicRoot = "./uploads";

  /** Creates the account-owned notebook application service.
   * @param mapper notebook SQL boundary
   * @param validator versioned field validator
   * @param json JSON codec
   * @param transactions transaction manager for post-commit cleanup */
  public NotebookEventService(NotebookEventMapper mapper, NotebookTemplateValidator validator,
      ObjectMapper json, PlatformTransactionManager transactions) {
    this.mapper = mapper; this.validator = validator; this.json = json;
    this.cleanupTransactions = new TransactionTemplate(transactions);
    this.cleanupTransactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /** Creates an event and first template in the same transaction.
   * @param owner authenticated account ID
   * @param request event and initial fields
   * @return created event */
  @Transactional
  public NotebookEventView create(long owner, NotebookEventCreate request) {
    if (request == null) throw bad("Event body is required");
    String name = name(request.name());
    String category = optional(request.category(), 80);
    String description = optional(request.description(), 5000);
    List<NotebookField> fields = validator.publish(request.fields(), List.of());
    long id = mapper.insertEvent(owner, name, category, description);
    mapper.insertTemplate(id, 1, encode(fields), owner);
    mapper.audit(owner, owner, id, "EVENT_CREATE");
    return get(owner, id);
  }

  /** Lists current account events without expanding private records.
   * @param owner authenticated account ID
   * @param includeArchived include archived events
   * @return ordered event summaries */
  public List<NotebookEventView> list(long owner, boolean includeArchived) {
    var results = mapper.listEvents(owner, includeArchived);
    for (var event : results) mapper.audit(owner, owner, event.id(), "EVENT_LIST_VIEW");
    return results;
  }

  /** Reads one event only for its owning account.
   * @param owner authenticated account ID
   * @param eventId event ID
   * @return owner event */
  public NotebookEventView get(long owner, long eventId) {
    var event = requireOwned(owner, mapper.findEvent(eventId));
    mapper.audit(owner, owner, eventId, "EVENT_VIEW");
    return event;
  }

  /** Changes owner-managed event fields without changing ownership.
   * @param owner authenticated account ID
   * @param eventId event ID
   * @param patch partial update
   * @return updated event */
  @Transactional
  public NotebookEventView update(long owner, long eventId, NotebookEventPatch patch) {
    NotebookEventView current = get(owner, eventId);
    if (patch == null) throw bad("Event update is required");
    var updated = new NotebookEventView(current.id(), owner,
        patch.name() == null ? current.name() : name(patch.name()),
        patch.category() == null ? current.category() : optional(patch.category(), 80),
        patch.description() == null ? current.description() : optional(patch.description(), 5000),
        patch.sortOrder() == null ? current.sortOrder() : order(patch.sortOrder()),
        patch.starred() == null ? current.starred() : patch.starred(),
        patch.archived() == null ? current.archived() : patch.archived(),
        current.currentTemplateVersion());
    if (mapper.updateEvent(updated) != 1) throw missing();
    mapper.audit(owner, owner, eventId, "EVENT_UPDATE");
    return get(owner, eventId);
  }

  /** Reorders exactly the supplied account events.
   * @param owner authenticated account ID
   * @param eventIds unique event IDs in desired order */
  @Transactional
  public void reorder(long owner, List<Long> eventIds) {
    if (eventIds == null || eventIds.isEmpty() || eventIds.size() > 200
        || eventIds.stream().anyMatch(id -> id == null || id < 1)
        || new HashSet<>(eventIds).size() != eventIds.size()) throw bad("Invalid event order");
    for (Long id : eventIds) get(owner, id);
    for (int index = 0; index < eventIds.size(); index++) {
      if (mapper.updateOrder(owner, eventIds.get(index), index) != 1) throw missing();
    }
  }

  /** Reads every historical template only after owner authorization.
   * @param owner authenticated account ID
   * @param eventId event ID
   * @return immutable versions in publication order */
  public List<NotebookTemplateView> templates(long owner, long eventId) {
    get(owner, eventId);
    return mapper.templates(eventId).stream().map(row ->
        new NotebookTemplateView(eventId, row.version(), decode(row.fieldsJson()))).toList();
  }

  /** Publishes a complete replacement template as the next immutable version.
   * @param owner authenticated account ID
   * @param eventId event ID
   * @param request replacement fields
   * @return newly published version */
  @Transactional
  public NotebookTemplateView publishTemplate(long owner, long eventId, NotebookTemplateRequest request) {
    NotebookEventView event = requireOwned(owner, mapper.lockEvent(eventId));
    if (request == null) throw bad("Template body is required");
    var previous = mapper.template(eventId, event.currentTemplateVersion());
    if (previous == null) throw new IllegalStateException("Current notebook template is missing");
    List<NotebookField> fields = validator.publish(request.fields(), decode(previous.fieldsJson()));
    int next = event.currentTemplateVersion() + 1;
    mapper.insertTemplate(eventId, next, encode(fields), owner);
    if (mapper.advanceVersion(owner, eventId, event.currentTemplateVersion(), next) != 1) {
      throw new BusinessException(ErrorCode.STATE_CONFLICT, "Template version changed; reload the event");
    }
    mapper.audit(owner, owner, eventId, "TEMPLATE_PUBLISH");
    return new NotebookTemplateView(eventId, next, fields);
  }

  /** Counts the rows affected by permanent removal for an owner confirmation screen.
   * @param owner authenticated account ID
   * @param eventId event ID
   * @return affected record, template and grant counts */
  public NotebookDeleteImpact deleteImpact(long owner, long eventId) {
    get(owner, eventId);
    return mapper.deleteImpact(eventId);
  }

  /** Permanently removes an owner event, revoking its grants and retaining metadata audit.
   * @param owner authenticated account ID
   * @param eventId event ID
   * @param confirmed whether the owner acknowledged the delete impact */
  @Transactional
  public void delete(long owner, long eventId, boolean confirmed) {
    requireOwned(owner, mapper.lockEvent(eventId));
    if (!confirmed) throw bad("Confirm permanent notebook event deletion");
    long lastImageId = 0;
    while (true) {
      var batch = mapper.imageKeysAfter(eventId, lastImageId, IMAGE_BATCH_SIZE);
      if (batch.isEmpty()) break;
      mapper.enqueueImageCleanup(eventId, owner, batch);
      lastImageId = batch.get(batch.size() - 1).id();
      if (batch.size() < IMAGE_BATCH_SIZE) break;
    }
    long lastStagedId = 0;
    while (true) {
      var batch = mapper.stagedImageKeysAfter(eventId, lastStagedId, IMAGE_BATCH_SIZE);
      if (batch.isEmpty()) break;
      mapper.enqueueImageCleanup(eventId, owner, batch);
      lastStagedId = batch.get(batch.size() - 1).id();
      if (batch.size() < IMAGE_BATCH_SIZE) break;
    }
    mapper.revokeGrants(eventId);
    if (mapper.deleteEvent(owner, eventId) != 1) throw missing();
    mapper.audit(owner, owner, eventId, "EVENT_DELETE");
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override public void afterCommit() { retryEventCleanup(eventId); }
      });
    } else retryEventCleanup(eventId);
  }

  /** Retries persisted private file cleanup in bounded batches.
   * Failed files retain queue rows for the next scheduled attempt. */
  @Scheduled(fixedDelayString = "${family-kitchen.notebook.cleanup-delay-ms:3600000}",
      initialDelayString = "${family-kitchen.notebook.cleanup-delay-ms:3600000}")
  public void cleanupPending() {
    try { processCleanup(cursor -> mapper.pendingCleanupAfter(cursor, IMAGE_BATCH_SIZE)); }
    catch (RuntimeException failure) { log.warn("Notebook private image cleanup will retry", failure); }
  }

  private void retryEventCleanup(long eventId) {
    try { processCleanup(cursor -> mapper.pendingCleanupForEvent(eventId, cursor, IMAGE_BATCH_SIZE), 1); }
    catch (RuntimeException failure) {
      log.warn("Notebook event image cleanup will retry eventId={}", eventId, failure);
    }
  }

  private void processCleanup(LongFunction<List<NotebookEventMapper.CleanupRow>> fetch) {
    processCleanup(fetch, Integer.MAX_VALUE);
  }

  private void processCleanup(LongFunction<List<NotebookEventMapper.CleanupRow>> fetch, int maxBatches) {
    long cursor = 0;
    for (int processed = 0; processed < maxBatches; processed++) {
      long afterId = cursor;
      List<NotebookEventMapper.CleanupRow> batch = cleanupTransactions.execute(
          status -> fetch.apply(afterId));
      if (batch == null || batch.isEmpty()) return;
      Path root = verifiedPrivateRoot();
      if (root == null) return;
      List<Long> cleaned = new ArrayList<>();
      for (var row : batch) if (cleanupImage(root, row.storageKey())) cleaned.add(row.id());
      if (!cleaned.isEmpty()) cleanupTransactions.executeWithoutResult(
          status -> mapper.removeCleanup(cleaned));
      cursor = batch.get(batch.size() - 1).id();
      if (batch.size() < IMAGE_BATCH_SIZE) return;
    }
  }

  private Path verifiedPrivateRoot() {
    Path root = Path.of(privateRoot).toAbsolutePath().normalize();
    Path publicFiles = Path.of(publicRoot).toAbsolutePath().normalize();
    try {
      if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return null;
      Path realRoot = root.toRealPath();
      Path realPublic = Files.exists(publicFiles, LinkOption.NOFOLLOW_LINKS)
          ? publicFiles.toRealPath() : publicFiles;
      if (!root.equals(realRoot) || realRoot.startsWith(realPublic)
          || realPublic.startsWith(realRoot)) {
        log.error("Notebook private image root overlaps public storage or is a symlink");
        return null;
      }
    } catch (IOException failure) {
      log.warn("Cannot verify notebook private image root", failure);
      return null;
    }
    return root;
  }

  private boolean cleanupImage(Path root, String key) {
    if (key == null || !key.matches("[a-fA-F0-9-]{32,36}\\.(png|jpg|webp)")) {
      log.warn("Skipping invalid private notebook image key");
      return true;
    }
    Path target = root.resolve(key).normalize();
    if (!target.getParent().equals(root)) return true;
    try {
      if (Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)
          && target.toRealPath().getParent().equals(root)) Files.delete(target);
      return true;
    } catch (IOException failure) {
      log.warn("Private notebook image cleanup failed for key={}", key, failure);
      return false;
    }
  }

  private NotebookEventView requireOwned(long owner, NotebookEventView event) {
    if (event == null || event.ownerUserId() != owner) throw missing();
    return event;
  }

  private String encode(List<NotebookField> fields) {
    try { return json.writeValueAsString(fields); }
    catch (JsonProcessingException failure) { throw new IllegalStateException("Cannot encode template", failure); }
  }

  private List<NotebookField> decode(String fieldsJson) {
    try { return List.copyOf(json.readValue(fieldsJson, new TypeReference<List<NotebookField>>() {})); }
    catch (JsonProcessingException failure) { throw new IllegalStateException("Cannot decode template", failure); }
  }

  private static String name(String value) {
    if (value == null || value.isBlank() || value.strip().length() > 120) throw bad("Invalid event name");
    return value.strip();
  }

  private static String optional(String value, int max) {
    if (value == null || value.isBlank()) return null;
    String result = value.strip();
    if (result.length() > max) throw bad("Event text is too long");
    return result;
  }

  private static int order(int value) {
    if (value < 0 || value > 1_000_000) throw bad("Invalid event order");
    return value;
  }

  private static BusinessException bad(String message) { return new BusinessException(ErrorCode.BAD_REQUEST, message); }
  private static BusinessException missing() { return new BusinessException(ErrorCode.NOT_FOUND, "Notebook event not found"); }
}
