package com.familykitchen.system.controller;
import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.system.model.dto.SystemSettingRequest;
import com.familykitchen.system.model.dto.TestEmailRequest;
import com.familykitchen.system.model.vo.SystemSettingView;
import com.familykitchen.system.service.SystemSettingService;
import com.familykitchen.system.service.PlatformMailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/** 平台管理员系统配置接口。 */
@RestController @RequestMapping("/admin/system-settings")
@Tag(name="后台-系统配置",description="网站信息、菜品审核和维护模式")
public class AdminSystemSettingController {
  private final CurrentUserProvider users; private final SystemSettingService service; private final PlatformMailService mail;
  /**
   * 创建平台系统配置控制器。
   * @param users 当前请求身份提供器
   * @param service 系统配置服务
   * @param mail 平台邮件服务
   */
  public AdminSystemSettingController(CurrentUserProvider users,SystemSettingService service,PlatformMailService mail){
    this.users=users;this.service=service;this.mail=mail;}
  /**
   * 查询平台管理员可见的系统配置。
   * @param request 当前 HTTP 请求
   * @return 脱敏后的完整系统配置
   */
  @GetMapping @Operation(summary="查询系统配置")
  public ApiResponse<SystemSettingView> current(HttpServletRequest request){requireAdmin(request);return ApiResponse.ok(service.current());}
  /**
   * 以平台管理员身份更新系统配置。
   * @param request 当前 HTTP 请求
   * @param body 已校验的配置内容
   * @return 更新后的脱敏配置
   */
  @PutMapping @Operation(summary="更新系统配置")
  public ApiResponse<SystemSettingView> update(HttpServletRequest request,@Valid @RequestBody SystemSettingRequest body){
    CurrentUserContext user=requireAdmin(request);return ApiResponse.ok(service.update(user.userId(),body));}
  /**
   * 使用当前平台 SMTP 配置发送测试邮件。
   * @param request 当前 HTTP 请求
   * @param body 测试收件地址
   * @return 空成功响应
   */
  @PostMapping("/test-email") @Operation(summary="发送 SMTP 测试邮件")
  public ApiResponse<Void> testEmail(HttpServletRequest request,@Valid @RequestBody TestEmailRequest body){
    requireAdmin(request);mail.sendTestMail(body.recipient());return ApiResponse.ok();}
  private CurrentUserContext requireAdmin(HttpServletRequest request){CurrentUserContext user=users.require(request);
    if(!user.hasPlatformBackendAccess())throw new BusinessException(ErrorCode.FORBIDDEN,"无平台管理员权限");return user;}
}
