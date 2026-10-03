package com.familykitchen.notebook.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.notebook.model.NotebookCalendarSummary;
import com.familykitchen.notebook.model.NotebookRecordCreate;
import com.familykitchen.notebook.model.NotebookRecordPage;
import com.familykitchen.notebook.model.NotebookRecordPatch;
import com.familykitchen.notebook.model.NotebookRecordView;
import com.familykitchen.notebook.service.NotebookRecordService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Account-owned notebook record and bounded calendar endpoints. */
@RestController
@RequestMapping("/notebook")
public class NotebookRecordController {
  private final CurrentUserProvider users;
  private final NotebookRecordService service;

  /** Creates notebook record routes.
   * @param users authenticated account provider
   * @param service record application service */
  public NotebookRecordController(CurrentUserProvider users, NotebookRecordService service) {
    this.users = users; this.service = service;
  }

  /** Returns the current record form only to the owner or a live create collaborator.
   * @param request authenticated request
   * @param eventId event ID
   * @return current field definitions */
  @GetMapping("/events/{eventId}/record-template")
  public ApiResponse<NotebookRecordService.RecordTemplate> currentTemplate(HttpServletRequest request,
      @PathVariable long eventId) {
    return ApiResponse.ok(service.currentTemplate(users.require(request).userId(), eventId));
  }

  /** Lists a bounded, deterministic page of owner records.
   * @param request authenticated request
   * @param eventId event ID
   * @param from inclusive date
   * @param to inclusive date
   * @param timeZone IANA time-zone ID defining calendar days
   * @param page zero-based page
   * @param size page size
   * @return owner record page */
  @GetMapping("/events/{eventId}/records")
  public ApiResponse<NotebookRecordPage> list(HttpServletRequest request, @PathVariable long eventId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam String timeZone,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.ok(service.list(users.require(request).userId(), eventId, from, to,
        timeZone, page, size));
  }

  /** Returns nonempty date counts without record values.
   * @param request authenticated request
   * @param eventId event ID
   * @param from inclusive date
   * @param to inclusive date
   * @param timeZone IANA time-zone ID defining calendar days
   * @return ordered date summaries */
  @GetMapping("/events/{eventId}/calendar")
  public ApiResponse<List<NotebookCalendarSummary>> calendar(HttpServletRequest request,
      @PathVariable long eventId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam String timeZone) {
    return ApiResponse.ok(service.calendar(users.require(request).userId(), eventId, from, to,
        timeZone));
  }

  /** Creates a record under an owner event.
   * @param request authenticated request
   * @param eventId event ID
   * @param body record draft
   * @return created record */
  @PostMapping("/events/{eventId}/records")
  public ApiResponse<NotebookRecordView> create(HttpServletRequest request, @PathVariable long eventId,
      @RequestBody NotebookRecordCreate body) {
    return ApiResponse.ok(service.create(users.require(request).userId(), eventId, body));
  }

  /** Reads an owner record with its stored template fields.
   * @param request authenticated request
   * @param id record ID
   * @return record detail */
  @GetMapping("/records/{id}")
  public ApiResponse<NotebookRecordView> detail(HttpServletRequest request, @PathVariable long id) {
    return ApiResponse.ok(service.get(users.require(request).userId(), id));
  }

  /** Applies a version-checked owner record edit.
   * @param request authenticated request
   * @param id record ID
   * @param body record patch
   * @return edited record */
  @PatchMapping("/records/{id}")
  public ApiResponse<NotebookRecordView> update(HttpServletRequest request, @PathVariable long id,
      @RequestBody NotebookRecordPatch body) {
    return ApiResponse.ok(service.update(users.require(request).userId(), id, body));
  }

  /** Explicit template-upgrade request.
   * @param expectedVersion last observed record version
   * @param values complete current-template values */
  public record UpgradeRequest(Integer expectedVersion, Map<String, Object> values) {}

  /** Upgrades one record while snapshotting its previous template and values.
   * @param request authenticated request
   * @param id record ID
   * @param body upgrade request
   * @return upgraded record */
  @PostMapping("/records/{id}/upgrade-template")
  public ApiResponse<NotebookRecordView> upgrade(HttpServletRequest request, @PathVariable long id,
      @RequestBody UpgradeRequest body) {
    return ApiResponse.ok(service.upgradeTemplate(users.require(request).userId(), id,
        body.expectedVersion(), body.values()));
  }

  /** Permanently deletes one owner record.
   * @param request authenticated request
   * @param id record ID
   * @return empty success response */
  @DeleteMapping("/records/{id}")
  public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable long id) {
    service.delete(users.require(request).userId(), id);
    return ApiResponse.ok();
  }
}
