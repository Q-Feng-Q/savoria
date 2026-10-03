package com.familykitchen.notebook.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.notebook.service.NotebookAuditService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Owner-only notebook action history route. */
@RestController
@RequestMapping("/notebook/audit")
public class NotebookAuditController {
  private final CurrentUserProvider users;
  private final NotebookAuditService service;

  /** Creates audit route.
   * @param users current account
   * @param service audit operations */
  public NotebookAuditController(CurrentUserProvider users, NotebookAuditService service) {
    this.users = users; this.service = service;
  }

  /** Lists bounded action metadata for an owned event.
   *
   * @param request authenticated request
   * @param eventId event ID
   *
   * @param from first date
   * @param to last date
   * @param timeZone IANA zone
   *
   * @param page zero-based page
   * @param size page size
   * @return metadata page */
  @GetMapping
  public ApiResponse<NotebookAuditService.Page> list(HttpServletRequest request,
      @RequestParam long eventId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam String timeZone, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.ok(service.list(users.require(request).userId(), eventId, from, to,
        timeZone, page, size));
  }
}
