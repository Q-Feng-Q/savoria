package com.familykitchen.notebook.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.notebook.model.NotebookContactView;
import com.familykitchen.notebook.model.NotebookInviteCreated;
import com.familykitchen.notebook.model.NotebookInviteView;
import com.familykitchen.notebook.service.NotebookContactService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Confirmed account contact and invitation endpoints. */
@RestController
@RequestMapping("/notebook/contacts")
public class NotebookContactController {
  private final CurrentUserProvider users;
  private final NotebookContactService service;

  /** Creates contact routes.
   * @param users authenticated accounts
   * @param service contact service */
  public NotebookContactController(CurrentUserProvider users, NotebookContactService service) {
    this.users = users; this.service = service;
  }

  /** Lists confirmed account contacts.
   * @param request authenticated request
   * @return contacts */
  @GetMapping
  public ApiResponse<List<NotebookContactView>> list(HttpServletRequest request) {
    return ApiResponse.ok(service.list(users.require(request).userId()));
  }

  /** Lists invites received by the account without their secrets.
   * @param request authenticated request
   * @return invite metadata */
  @GetMapping("/invites")
  public ApiResponse<List<NotebookInviteView>> invites(HttpServletRequest request) {
    return ApiResponse.ok(service.received(users.require(request).userId()));
  }

  /** Invite target login identifier.
   * @param identifier username, mobile or email */
  public record InviteRequest(String identifier) {}

  /** Creates an invitation and returns its one-time token.
   * @param request authenticated request
   * @param body target account identifier
   * @return created invite */
  @PostMapping("/invites")
  public ApiResponse<NotebookInviteCreated> invite(HttpServletRequest request,
      @RequestBody InviteRequest body) {
    return ApiResponse.ok(service.invite(users.require(request).userId(), body.identifier()));
  }

  /** One-time invitation secret.
   * @param token recipient-bound secret */
  public record TokenRequest(String token) {}

  /** Accepts an invitation as its intended recipient.
   * @param request authenticated request
   * @param id invitation ID
   * @param body secret
   * @return empty success */
  @PostMapping("/invites/{id}/accept")
  public ApiResponse<Void> accept(HttpServletRequest request, @PathVariable long id,
      @RequestBody TokenRequest body) {
    service.accept(users.require(request).userId(), id, body.token());
    return ApiResponse.ok();
  }

  /** Rejects an invitation as its intended recipient.
   * @param request authenticated request
   * @param id invitation ID
   * @param body secret
   * @return empty success */
  @PostMapping("/invites/{id}/reject")
  public ApiResponse<Void> reject(HttpServletRequest request, @PathVariable long id,
      @RequestBody TokenRequest body) {
    service.reject(users.require(request).userId(), id, body.token());
    return ApiResponse.ok();
  }

  /** Removes a confirmed contact and revokes both parties' grants.
   * @param request authenticated request
   * @param id contact account ID
   * @return empty success */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> remove(HttpServletRequest request, @PathVariable long id) {
    service.remove(users.require(request).userId(), id);
    return ApiResponse.ok();
  }
}
