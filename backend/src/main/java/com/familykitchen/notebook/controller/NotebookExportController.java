package com.familykitchen.notebook.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.notebook.service.NotebookExportService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Account-scoped notebook JSON export route. */
@RestController
@RequestMapping("/notebook/events")
public class NotebookExportController {
  /** Export request with explicit local-date zone and clipboard/file intent.
   * @param from first date
   * @param to last date
   * @param timeZone IANA zone
   * @param mode COPY or FILE */
  public record ExportRequest(LocalDate from, LocalDate to, String timeZone, String mode) {}
  private final CurrentUserProvider users;
  private final NotebookExportService service;

  /** Connects authenticated requests to export service.
   *
   * @param users current account
   * @param service export operations */
  public NotebookExportController(CurrentUserProvider users, NotebookExportService service) {
    this.users = users; this.service = service;
  }

  /** Exports one bounded, authorized JSON payload.
   *
   * @param request authenticated request
   * @param eventId event ID
   * @param body export selection
   *
   * @return complete v1 payload */
  @PostMapping("/{eventId}/export")
  public ApiResponse<Map<String, Object>> export(HttpServletRequest request, @PathVariable long eventId,
      @RequestBody ExportRequest body) {
    return ApiResponse.ok(service.export(users.require(request).userId(), eventId, body.from(),
        body.to(), body.timeZone(), body.mode()));
  }
}
