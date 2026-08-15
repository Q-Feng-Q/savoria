package com.familykitchen.notification.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.api.PageResponse;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.notification.model.dto.ReadAllRequest;
import com.familykitchen.notification.service.NotificationApplicationService;
import com.familykitchen.notification.model.vo.NotificationView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知中心控制器。
 *
 * <p>提供通知列表读取、单条已读和全部已读等消息中心接口。</p>
 */
@RestController
@RequestMapping("/notifications")
@Tag(name = "公共-通知中心", description = "家庭端与商户端通知中心接口")
public class NotificationController {

  private final CurrentUserProvider currentUserProvider;
  private final NotificationApplicationService notificationApplicationService;

  /**
   * 创建通知实例。
   *
   * @param currentUserProvider 当前用户Provider
   * @param notificationApplicationService 通知申请Service
   */
  public NotificationController(
      CurrentUserProvider currentUserProvider,
      NotificationApplicationService notificationApplicationService
  ) {
    this.currentUserProvider = currentUserProvider;
    this.notificationApplicationService = notificationApplicationService;
  }

  /**
   * 处理通知相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param receiverScope receiverScope
   * @param readStatus read状态
   * @param category category
   * @param page 分页
   * @param pageSize 分页Size
   * @return 列出的结果
   */
  @GetMapping
  @Operation(summary = "查询通知列表", description = "按范围、已读状态和分类分页查询通知。")
  public ApiResponse<PageResponse<NotificationView>> list(
      HttpServletRequest request,
      @Parameter(description = "通知接收范围，可选 family/member/merchant/platform") @RequestParam(required = false)
      String receiverScope,
      @Parameter(description = "读取状态，可选 all/read/unread") @RequestParam(defaultValue = "all")
      String readStatus,
      @Parameter(description = "通知分类") @RequestParam(required = false) String category,
      @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
      @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    return ApiResponse.ok(notificationApplicationService.list(user, receiverScope, readStatus, category, page, pageSize));
  }

  /**
   * 处理通知相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param notificationId 通知标识
   * @return 处理的结果
   */
  @PostMapping("/{notificationId}/read")
  @Operation(summary = "标记已读", description = "将指定通知标记为已读。")
  public ApiResponse<Void> read(
      HttpServletRequest request,
      @Parameter(description = "通知 ID") @PathVariable Long notificationId
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    notificationApplicationService.read(user, notificationId);
    return ApiResponse.ok();
  }

  /**
   * 处理All相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param body 请求体
   * @return 处理All的结果
   */
  @PostMapping("/read-all")
  @Operation(summary = "全部标记已读", description = "按接收范围将通知全部标记为已读。")
  public ApiResponse<Void> readAll(HttpServletRequest request, @Valid @RequestBody ReadAllRequest body) {
    CurrentUserContext user = currentUserProvider.require(request);
    notificationApplicationService.readAll(user, body.receiverScope());
    return ApiResponse.ok();
  }
}
