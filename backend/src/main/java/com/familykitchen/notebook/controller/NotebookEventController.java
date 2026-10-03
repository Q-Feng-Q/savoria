package com.familykitchen.notebook.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.notebook.model.NotebookDeleteImpact;
import com.familykitchen.notebook.model.NotebookEventCreate;
import com.familykitchen.notebook.model.NotebookEventPatch;
import com.familykitchen.notebook.model.NotebookEventView;
import com.familykitchen.notebook.model.NotebookTemplateRequest;
import com.familykitchen.notebook.model.NotebookTemplateView;
import com.familykitchen.notebook.service.NotebookEventService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Account-owned notebook event and versioned template endpoints. */
@RestController
@RequestMapping("/notebook/events")
public class NotebookEventController {
  private final CurrentUserProvider users;
  private final NotebookEventService service;

  /** Creates notebook event routes.
   * @param users authenticated account provider
   * @param service account-owned event service */
  public NotebookEventController(CurrentUserProvider users, NotebookEventService service) {
    this.users = users; this.service = service;
  }

  /** Lists only events owned by the current account.
   * @param request authenticated request
   * @param includeArchived whether archived events are included
   * @return ordered owner events */
  @GetMapping
  public ApiResponse<List<NotebookEventView>> list(HttpServletRequest request,
      @RequestParam(defaultValue = "false") boolean includeArchived) {
    return ApiResponse.ok(service.list(users.require(request).userId(), includeArchived));
  }

  /** Creates an event and first template.
   * @param request authenticated request
   * @param body event draft
   * @return new owner event */
  @PostMapping
  public ApiResponse<NotebookEventView> create(HttpServletRequest request,
      @RequestBody NotebookEventCreate body) {
    return ApiResponse.ok(service.create(users.require(request).userId(), body));
  }

  /** Reads one owner event.
   * @param request authenticated request
   * @param id event ID
   * @return owner event */
  @GetMapping("/{id}")
  public ApiResponse<NotebookEventView> detail(HttpServletRequest request, @PathVariable long id) {
    return ApiResponse.ok(service.get(users.require(request).userId(), id));
  }

  /** Updates owner-managed event properties.
   * @param request authenticated request
   * @param id event ID
   * @param body partial update
   * @return updated event */
  @PatchMapping("/{id}")
  public ApiResponse<NotebookEventView> update(HttpServletRequest request, @PathVariable long id,
      @RequestBody NotebookEventPatch body) {
    return ApiResponse.ok(service.update(users.require(request).userId(), id, body));
  }

  /** Ordered event IDs supplied by the account owner.
   * @param eventIds event IDs in desired order */
  public record OrderRequest(List<Long> eventIds) {}

  /** Reorders the supplied owner events.
   * @param request authenticated request
   * @param body desired order
   * @return empty success response */
  @PutMapping("/order")
  public ApiResponse<Void> order(HttpServletRequest request, @RequestBody OrderRequest body) {
    service.reorder(users.require(request).userId(), body.eventIds());
    return ApiResponse.ok();
  }

  /** Counts data affected by permanent event deletion.
   * @param request authenticated request
   * @param id event ID
   * @return owner-visible impact counts */
  @GetMapping("/{id}/delete-impact")
  public ApiResponse<NotebookDeleteImpact> deleteImpact(HttpServletRequest request, @PathVariable long id) {
    return ApiResponse.ok(service.deleteImpact(users.require(request).userId(), id));
  }

  /** Deletes an event after an explicit owner confirmation.
   * @param request authenticated request
   * @param id event ID
   * @param confirm confirmation flag
   * @return empty success response */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable long id,
      @RequestParam(defaultValue = "false") boolean confirm) {
    service.delete(users.require(request).userId(), id, confirm);
    return ApiResponse.ok();
  }

  /** Lists every immutable template version for an owner event.
   * @param request authenticated request
   * @param id event ID
   * @return versions in publication order */
  @GetMapping("/{id}/templates")
  public ApiResponse<List<NotebookTemplateView>> templates(HttpServletRequest request, @PathVariable long id) {
    return ApiResponse.ok(service.templates(users.require(request).userId(), id));
  }

  /** Publishes a replacement template version for an owner event.
   * @param request authenticated request
   * @param id event ID
   * @param body complete replacement fields
   * @return new immutable version */
  @PostMapping("/{id}/templates")
  public ApiResponse<NotebookTemplateView> publish(HttpServletRequest request, @PathVariable long id,
      @RequestBody NotebookTemplateRequest body) {
    return ApiResponse.ok(service.publishTemplate(users.require(request).userId(), id, body));
  }
}
