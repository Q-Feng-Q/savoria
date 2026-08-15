package com.familykitchen.dish.controller;
import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.dish.model.dto.DishReviewDecisionRequest;
import com.familykitchen.dish.model.entity.DishReviewSubmissionDO;
import com.familykitchen.dish.service.DishReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/** 平台管理员菜品审核接口。 */
@RestController @RequestMapping("/admin/dish-reviews")
@Tag(name="后台-菜品审核",description="菜品待审列表、通过与拒绝")
public class AdminDishReviewController {
  private final CurrentUserProvider users;private final DishReviewService service;
  /**
   * 创建平台管理菜品审核实例。
   *
   * @param users users
   * @param service service
   */
  public AdminDishReviewController(CurrentUserProvider users,DishReviewService service){this.users=users;this.service=service;}
  /**
   * 处理平台管理菜品审核相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @return 处理的结果
   */
  @GetMapping @Operation(summary="查询待审核菜品")
  public ApiResponse<List<DishReviewSubmissionDO>> pending(HttpServletRequest request){requireAdmin(request);return ApiResponse.ok(service.pending());}
  /**
   * 处理平台管理菜品审核相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param id 标识
   * @param body 请求体
   * @return 批准的结果
   */
  @PostMapping("/{id}/approve") @Operation(summary="通过菜品审核")
  public ApiResponse<Void> approve(HttpServletRequest request,@PathVariable Long id,@RequestBody(required=false) DishReviewDecisionRequest body){
    CurrentUserContext user=requireAdmin(request);service.approve(id,user.userId(),body==null?null:body.reason());return ApiResponse.ok();}
  /**
   * 处理平台管理菜品审核相关的 HTTP 请求。
   *
   * @param request 请求参数
   * @param id 标识
   * @param body 请求体
   * @return 拒绝的结果
   */
  @PostMapping("/{id}/reject") @Operation(summary="拒绝菜品审核")
  public ApiResponse<Void> reject(HttpServletRequest request,@PathVariable Long id,@Valid @RequestBody DishReviewDecisionRequest body){
    CurrentUserContext user=requireAdmin(request);service.reject(id,user.userId(),body.reason());return ApiResponse.ok();}
  private CurrentUserContext requireAdmin(HttpServletRequest request){CurrentUserContext user=users.require(request);
    if(!user.hasPlatformBackendAccess())throw new BusinessException(ErrorCode.FORBIDDEN,"无平台管理员权限");return user;}
}
