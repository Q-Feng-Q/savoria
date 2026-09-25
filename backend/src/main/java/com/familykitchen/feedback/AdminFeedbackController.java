package com.familykitchen.feedback;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Platform-only feedback moderation with optimistic version checks and audit history. */
@RestController
@RequestMapping("/admin/feedback")
public class AdminFeedbackController {
  private final CurrentUserProvider users;
  private final FeedbackService service;
  /**
   * Create platform moderation endpoints.
   * @param users identity provider
   * @param service feedback application service
   */
  public AdminFeedbackController(CurrentUserProvider users,FeedbackService service){this.users=users;this.service=service;}
  @GetMapping
  /**
   * List feedback after platform authorization.
   * @param request authenticated request
   * @param type optional type filter
   * @param status optional status filter
   * @param page one-based page
   * @param pageSize page length
   * @return filtered page envelope
   */
  public ApiResponse<Map<String,Object>> list(HttpServletRequest request,@RequestParam(required=false) String type,@RequestParam(required=false) String status,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize){return ApiResponse.ok(service.list(users.require(request),true,type,status,page,pageSize));}
  @GetMapping("/{id}")
  /**
   * Read platform detail including audit history.
   * @param request authenticated request
   * @param id feedback ID
   * @return platform detail envelope
   */
  public ApiResponse<Map<String,Object>> detail(HttpServletRequest request,@PathVariable long id){return ApiResponse.ok(service.detail(users.require(request),id,true));}
  @PutMapping("/{id}")
  /**
   * Apply a version-checked moderation change.
   * @param request authenticated request
   * @param id feedback ID
   * @param body moderation fields
   * @return updated platform detail envelope
   */
  public ApiResponse<Map<String,Object>> update(HttpServletRequest request,@PathVariable long id,@RequestBody FeedbackService.Update body){return ApiResponse.ok(service.update(users.require(request),id,body));}
}
