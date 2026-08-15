package com.familykitchen.dish.service;
import com.familykitchen.dish.model.dto.DishRequest;
import com.familykitchen.dish.model.entity.DishReviewSubmissionDO;
import java.util.List;
/** 菜品审核服务。 */
public interface DishReviewService {
  /**
   * 提交菜品审核。
   *
   * @param userId 用户标识
   * @param merchantId 商户标识
   * @param dishId 菜品标识
   * @param request 请求参数
   * @return 提交的结果
   */
  DishReviewSubmissionDO submit(Long userId,Long merchantId,Long dishId,DishRequest request);
  /**
   * 处理菜品审核。
   *
   * @return 处理的结果
   */
  List<DishReviewSubmissionDO> pending();
  /**
   * 处理History。
   *
   * @param merchantId 商户标识
   * @return 处理History的结果
   */
  List<DishReviewSubmissionDO> merchantHistory(Long merchantId);
  /**
   * 处理详情。
   *
   * @param merchantId 商户标识
   * @param reviewId 审核标识
   * @return 处理详情的结果
   */
  DishReviewSubmissionDO merchantDetail(Long merchantId,Long reviewId);
  /**
   * 处理菜品审核。
   *
   * @param merchantId 商户标识
   * @param reviewId 审核标识
   * @param userId 用户标识
   */
  void withdraw(Long merchantId,Long reviewId,Long userId);
  /**
   * 批准菜品审核。
   *
   * @param reviewId 审核标识
   * @param adminUserId 平台管理用户标识
   * @param reason 原因
   */
  void approve(Long reviewId,Long adminUserId,String reason);
  /**
   * 拒绝菜品审核。
   *
   * @param reviewId 审核标识
   * @param adminUserId 平台管理用户标识
   * @param reason 原因
   */
  void reject(Long reviewId,Long adminUserId,String reason);
}
