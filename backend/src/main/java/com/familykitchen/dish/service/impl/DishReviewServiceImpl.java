package com.familykitchen.dish.service.impl;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.mapper.DishReviewMapper;
import com.familykitchen.dish.model.dto.DishRequest;
import com.familykitchen.dish.model.entity.DishCookingStepEntity;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.model.entity.DishIngredientEntity;
import com.familykitchen.dish.model.entity.DishReviewSubmissionDO;
import com.familykitchen.dish.service.DishReviewService;
import com.familykitchen.system.mapper.SystemAuditMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 菜品审核服务实现。 */
@Service
public class DishReviewServiceImpl implements DishReviewService {
  private final DishReviewMapper reviewMapper; private final DishMapper dishMapper; private final ObjectMapper json;
  private final SystemAuditMapper auditMapper;
  /**
   * 创建菜品审核实例。
   *
   * @param reviewMapper 审核Mapper
   * @param dishMapper 菜品Mapper
   * @param json json
   * @param auditMapper auditMapper
   */
  public DishReviewServiceImpl(DishReviewMapper reviewMapper,DishMapper dishMapper,ObjectMapper json,SystemAuditMapper auditMapper){
    this.reviewMapper=reviewMapper;this.dishMapper=dishMapper;this.json=json;this.auditMapper=auditMapper;}

  /**
   * 提交菜品审核。
   *
   * @param userId 用户标识
   * @param merchantId 商户标识
   * @param dishId 菜品标识
   * @param request 请求参数
   * @return 提交的结果
   */
  @Override @Transactional
  public DishReviewSubmissionDO submit(Long userId,Long merchantId,Long dishId,DishRequest request){
    if(dishId!=null && reviewMapper.countPending(merchantId,dishId)>0)throw new BusinessException(ErrorCode.STATE_CONFLICT,"该菜品已有待审核版本");
    if(dishId!=null){DishEntity current=dishMapper.selectDish(merchantId,dishId);
      if(current==null)throw new BusinessException(ErrorCode.NOT_FOUND,"菜品不存在");
      request=mergeCollections(dishId,request);}
    DishReviewSubmissionDO e=new DishReviewSubmissionDO();e.setMerchantId(merchantId);e.setTargetDishId(dishId);
    e.setSubmissionType(dishId==null?"CREATE":"UPDATE");e.setSubmittedBy(userId);
    try{e.setSnapshotJson(json.writeValueAsString(request));}catch(Exception ex){throw new BusinessException(ErrorCode.BAD_REQUEST,"菜品审核快照生成失败");}
    reviewMapper.insert(e);e.setStatus("PENDING");return e;
  }
  /**
   * 处理菜品审核。
   *
   * @return 处理的结果
   */
  @Override public List<DishReviewSubmissionDO> pending(){return reviewMapper.selectPending();}
  /**
   * 处理History。
   *
   * @param merchantId 商户标识
   * @return 处理History的结果
   */
  @Override public List<DishReviewSubmissionDO> merchantHistory(Long merchantId){return reviewMapper.selectByMerchant(merchantId);}
  /**
   * 处理详情。
   *
   * @param merchantId 商户标识
   * @param reviewId 审核标识
   * @return 处理详情的结果
   */
  @Override public DishReviewSubmissionDO merchantDetail(Long merchantId,Long reviewId){DishReviewSubmissionDO item=reviewMapper.selectById(reviewId);
    if(item==null||!merchantId.equals(item.getMerchantId()))throw new BusinessException(ErrorCode.NOT_FOUND,"审核记录不存在");return item;}
  /**
   * 处理菜品审核。
   *
   * @param merchantId 商户标识
   * @param reviewId 审核标识
   * @param userId 用户标识
   */
  @Override @Transactional public void withdraw(Long merchantId,Long reviewId,Long userId){
    if(reviewMapper.withdraw(reviewId,merchantId)==0)throw new BusinessException(ErrorCode.DISH_REVIEW_STATE_CONFLICT,"仅待审核记录可撤回");
    auditMapper.insert(userId,"DISH_REVIEW_WITHDRAW","审核记录="+reviewId);}

  /**
   * 批准菜品审核。
   *
   * @param id 标识
   * @param adminId 平台管理标识
   * @param reason 原因
   */
  @Override @Transactional
  public void approve(Long id,Long adminId,String reason){
    DishReviewSubmissionDO review=requirePending(id);DishRequest request=read(review.getSnapshotJson());
    if(dishMapper.countCategoryOwnership(review.getMerchantId(),request.categoryId())==0)
      throw new BusinessException(ErrorCode.BUSINESS_INVALID,"审核菜品的分类不存在或不属于当前商户");
    Long dishId=review.getTargetDishId();
    DishEntity dish=entity(review.getMerchantId(),dishId,request);
    if(dishId==null){dishMapper.insertDish(dish);dishId=dish.getId();}else if(dishMapper.updateDish(dish)==0){throw new BusinessException(ErrorCode.NOT_FOUND,"正式菜品不存在");}
    replaceIngredients(dishId,request.ingredients());replaceSteps(dishId,request.cookingSteps());
    if(reviewMapper.approve(id,adminId,reason)==0)throw new BusinessException(ErrorCode.DISH_REVIEW_STATE_CONFLICT,"审核状态已变化");
    auditMapper.insert(adminId,"DISH_REVIEW_APPROVE","审核记录="+id+"，正式菜品="+dishId);
  }
  /**
   * 拒绝菜品审核。
   *
   * @param id 标识
   * @param adminId 平台管理标识
   * @param reason 原因
   */
  @Override @Transactional
  public void reject(Long id,Long adminId,String reason){
    requirePending(id);if(reason==null||reason.isBlank())throw new BusinessException(ErrorCode.BAD_REQUEST,"拒绝原因必填");
    if(reviewMapper.reject(id,adminId,reason.trim())==0)throw new BusinessException(ErrorCode.DISH_REVIEW_STATE_CONFLICT,"审核状态已变化");
    auditMapper.insert(adminId,"DISH_REVIEW_REJECT","审核记录="+id+"，原因="+reason.trim());
  }
  private DishReviewSubmissionDO requirePending(Long id){DishReviewSubmissionDO e=reviewMapper.selectById(id);
    if(e==null)throw new BusinessException(ErrorCode.NOT_FOUND,"审核记录不存在");
    if(!"PENDING".equals(e.getStatus()))throw new BusinessException(ErrorCode.DISH_REVIEW_STATE_CONFLICT,"审核记录已处理");return e;}
  private DishRequest read(String value){try{return json.readValue(value,DishRequest.class);}catch(Exception e){throw new BusinessException(ErrorCode.SYSTEM_ERROR,"菜品审核快照无法读取");}}
  private static DishEntity entity(Long merchantId,Long id,DishRequest r){DishEntity e=new DishEntity();e.setId(id);e.setMerchantId(merchantId);
    e.setCategoryId(r.categoryId());e.setName(r.name());e.setDescription(r.description());e.setImageUrl(r.imageUrl());e.setBasePrice(r.basePrice());
    String status=r.status()==null?"inactive":r.status().trim().toLowerCase();
    if(!"active".equals(status)&&!"inactive".equals(status))throw new BusinessException(ErrorCode.BAD_REQUEST,"菜品状态仅支持 active 或 inactive");
    e.setStatus(status);return e;}
  private DishRequest mergeCollections(Long dishId,DishRequest r){
    List<DishRequest.IngredientRequest> ingredients=r.ingredients();
    if(ingredients==null)ingredients=dishMapper.selectDishIngredients(dishId).stream()
      .map(i->new DishRequest.IngredientRequest(i.getIngredientName(),i.getQuantity(),i.getUnit(),i.getCalcType())).toList();
    List<DishRequest.CookingStepRequest> steps=r.cookingSteps();
    if(steps==null)steps=dishMapper.selectCookingSteps(dishId).stream()
      .map(s->new DishRequest.CookingStepRequest(s.getStepNo(),s.getTitle(),s.getContent())).toList();
    return new DishRequest(r.name(),r.categoryId(),r.description(),r.imageUrl(),r.basePrice(),ingredients,steps,r.status());}
  private void replaceIngredients(Long dishId,List<DishRequest.IngredientRequest> items){dishMapper.deleteDishIngredients(dishId);if(items==null)return;
    for(var item:items){DishIngredientEntity e=new DishIngredientEntity();e.setDishId(dishId);e.setIngredientName(item.ingredientName());e.setQuantity(item.quantity());e.setUnit(item.unit());e.setCalcType(item.calcType());dishMapper.insertDishIngredient(e);}}
  private void replaceSteps(Long dishId,List<DishRequest.CookingStepRequest> items){dishMapper.deleteCookingSteps(dishId);if(items==null)return;
    for(var item:items){DishCookingStepEntity e=new DishCookingStepEntity();e.setDishId(dishId);e.setStepNo(item.stepNo());e.setTitle(item.title());e.setContent(item.content());dishMapper.insertCookingStep(e);}}
}
