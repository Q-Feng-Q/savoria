package com.familykitchen.notebook.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.system.service.SystemSettingService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Account-authenticated notebook limits shared by all client views. */
@RestController
@RequestMapping("/notebook")
public class NotebookConfigController {
  private final CurrentUserProvider users;
  private final SystemSettingService settings;

  /** Creates the notebook configuration endpoint.
   * @param users current account provider
   * @param settings live platform settings */
  public NotebookConfigController(CurrentUserProvider users, SystemSettingService settings) {
    this.users = users; this.settings = settings;
  }

  /** Returns the effective bounded query limit without exposing notebook data.
   * @param request authenticated request
   * @return effective maximum touched calendar months */
  @GetMapping("/config")
  public ApiResponse<Map<String, Integer>> config(HttpServletRequest request) {
    users.require(request);
    return ApiResponse.ok(Map.of("maxQueryMonths", settings.notebookMaxQueryMonths()));
  }
}
