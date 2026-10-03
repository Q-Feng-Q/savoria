package com.familykitchen.notebook.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.notebook.model.NotebookGrantRequest;
import com.familykitchen.notebook.model.NotebookGrantView;
import com.familykitchen.notebook.service.NotebookAccessPolicy;
import com.familykitchen.notebook.service.NotebookGrantService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Owner grant management and recipient share discovery. */
@RestController
@RequestMapping("/notebook")
public class NotebookShareController {
  private final CurrentUserProvider users;
  private final NotebookGrantService grants;
  private final NotebookAccessPolicy access;

  /** Creates share routes.
   * @param users authenticated accounts
   * @param grants owner grant service
   * @param access recipient policy */
  public NotebookShareController(CurrentUserProvider users, NotebookGrantService grants,
      NotebookAccessPolicy access) {
    this.users = users; this.grants = grants; this.access = access;
  }

  /** Lists grants managed by the event owner.
   * @param request authenticated request
   * @param eventId event ID
   * @return event grants */
  @GetMapping("/events/{eventId}/grants")
  public ApiResponse<List<NotebookGrantView>> grants(HttpServletRequest request, @PathVariable long eventId) {
    return ApiResponse.ok(grants.list(users.require(request).userId(), eventId));
  }

  /** Creates an event grant for a confirmed contact.
   * @param request authenticated request
   * @param eventId event ID
   * @param body complete grant draft
   * @return created grant */
  @PostMapping("/events/{eventId}/grants")
  public ApiResponse<NotebookGrantView> create(HttpServletRequest request, @PathVariable long eventId,
      @RequestBody NotebookGrantRequest body) {
    return ApiResponse.ok(grants.create(users.require(request).userId(), eventId, body));
  }

  /** Replaces one owner grant.
   * @param request authenticated request
   * @param id grant ID
   * @param body complete replacement
   * @return updated grant */
  @PatchMapping("/grants/{id}")
  public ApiResponse<NotebookGrantView> update(HttpServletRequest request, @PathVariable long id,
      @RequestBody NotebookGrantRequest body) {
    return ApiResponse.ok(grants.update(users.require(request).userId(), id, body));
  }

  /** Revokes one owner grant.
   * @param request authenticated request
   * @param id grant ID
   * @return empty success */
  @DeleteMapping("/grants/{id}")
  public ApiResponse<Void> revoke(HttpServletRequest request, @PathVariable long id) {
    grants.revoke(users.require(request).userId(), id);
    return ApiResponse.ok();
  }

  /** Lists currently accessible shared grants, never private event contents.
   * @param request authenticated request
   * @return active shares */
  @GetMapping("/shared")
  public ApiResponse<List<NotebookGrantView>> shared(HttpServletRequest request) {
    return ApiResponse.ok(access.shared(users.require(request).userId()));
  }
}
