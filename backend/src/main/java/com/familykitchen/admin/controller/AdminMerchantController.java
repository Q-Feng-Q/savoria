package com.familykitchen.admin.controller;

import com.familykitchen.admin.model.dto.AdminMerchantRequest;
import com.familykitchen.admin.model.vo.AdminMerchantView;
import com.familykitchen.admin.service.AdminMerchantService;
import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.*;
import com.familykitchen.common.security.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** 提供平台管理员维护商户资料和私密邀请码的 HTTP 接口。 */
@RestController
@RequestMapping("/admin/merchants")
public class AdminMerchantController {
  private final CurrentUserProvider users;
  private final AdminMerchantService service;

  /**
   * 创建商户管理控制器。
   *
   * @param u 当前用户上下文提供器
   * @param s 商户管理服务
   */
  public AdminMerchantController(CurrentUserProvider u, AdminMerchantService s) { users = u; service = s; }

  /**
   * 查询商户列表。
   *
   * @param req 当前请求
   * @param keyword 名称关键字
   * @return 商户列表
   */
  @GetMapping
  public ApiResponse<List<AdminMerchantView>> list(HttpServletRequest req, @RequestParam(required=false) String keyword) { require(req); return ApiResponse.ok(service.list(keyword)); }

  /**
   * 创建商户并设置负责人。
   *
   * @param req 当前请求
   * @param body 商户资料
   * @return 新商户标识
   */
  @PostMapping
  public ApiResponse<Long> create(HttpServletRequest req, @Valid @RequestBody AdminMerchantRequest body) { require(req); return ApiResponse.ok(service.create(body)); }

  /**
   * 更新商户资料与负责人。
   *
   * @param req 当前请求
   * @param id 商户标识
   * @param body 商户资料
   * @return 空响应
   */
  @PutMapping("/{id}")
  public ApiResponse<Void> update(HttpServletRequest req, @PathVariable Long id, @Valid @RequestBody AdminMerchantRequest body) { require(req); service.update(id,body); return ApiResponse.ok(); }

  /**
   * 停用商户及其负责人关系。
   *
   * @param req 当前请求
   * @param id 商户标识
   * @return 空响应
   */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(HttpServletRequest req, @PathVariable Long id) { require(req); service.delete(id); return ApiResponse.ok(); }

  /**
   * 轮换商户邀请码并返回一次性明文。
   *
   * @param req 当前请求
   * @param id 商户标识
   * @return 新邀请码
   */
  @PostMapping("/{id}/invitation-code")
  public ApiResponse<String> code(HttpServletRequest req, @PathVariable Long id) { var u=require(req); return ApiResponse.ok(service.rotateCode(id,u.userId())); }

  /**
   * 停用商户当前有效的邀请码。
   *
   * @param req 当前请求
   * @param id 商户标识
   * @return 空响应
   */
  @DeleteMapping("/{id}/invitation-code")
  public ApiResponse<Void> disableCode(HttpServletRequest req, @PathVariable Long id) { require(req); service.disableCode(id); return ApiResponse.ok(); }

  private CurrentUserContext require(HttpServletRequest r) { var u=users.require(r); if(!u.hasPlatformBackendAccess()) throw new BusinessException(ErrorCode.FORBIDDEN,"无平台管理员权限"); return u; }
}
