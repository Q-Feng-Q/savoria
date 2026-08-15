package com.familykitchen.dish.mapper;
import com.familykitchen.dish.model.entity.DishReviewSubmissionDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/** 菜品审核 Mapper。 */
@Mapper
public interface DishReviewMapper {
  /**
   * 新增菜品审核。
   *
   * @param entity 实体
   * @return 新增的结果
   */
  int insert(DishReviewSubmissionDO entity);
  /**
   * 查询By标识。
   *
   * @param id 标识
   * @return 查询By标识的结果
   */
  DishReviewSubmissionDO selectById(@Param("id") Long id);
  /**
   * 查询Pending。
   *
   * @return 查询Pending的结果
   */
  List<DishReviewSubmissionDO> selectPending();
  /**
   * 查询By商户。
   *
   * @param merchantId 商户标识
   * @return 查询By商户的结果
   */
  List<DishReviewSubmissionDO> selectByMerchant(@Param("merchantId") Long merchantId);
  /**
   * 处理菜品审核。
   *
   * @param id 标识
   * @param merchantId 商户标识
   * @return 处理的结果
   */
  int withdraw(@Param("id") Long id,@Param("merchantId") Long merchantId);
  /**
   * 批准菜品审核。
   *
   * @param id 标识
   * @param reviewedBy reviewedBy
   * @param reason 原因
   * @return 批准的结果
   */
  int approve(@Param("id") Long id,@Param("reviewedBy") Long reviewedBy,@Param("reason") String reason);
  /**
   * 拒绝菜品审核。
   *
   * @param id 标识
   * @param reviewedBy reviewedBy
   * @param reason 原因
   * @return 拒绝的结果
   */
  int reject(@Param("id") Long id,@Param("reviewedBy") Long reviewedBy,@Param("reason") String reason);
  /**
   * 统计Pending。
   *
   * @param merchantId 商户标识
   * @param dishId 菜品标识
   * @return 统计Pending的结果
   */
  int countPending(@Param("merchantId") Long merchantId,@Param("dishId") Long dishId);
}
