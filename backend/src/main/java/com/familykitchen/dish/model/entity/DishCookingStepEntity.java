package com.familykitchen.dish.model.entity;

/**
 * 菜品制作步骤持久化实体。
 */
public class DishCookingStepEntity {

  /**
   * 标识。
   */
  private Long id;
  /**
   * 菜品标识。
   */
  private Long dishId;
  /**
   * stepNo。
   */
  private Integer stepNo;
  /**
   * title。
   */
  private String title;
  /**
   * content。
   */
  private String content;

  /**
   * 获取标识。
   *
   * @return 获取标识的结果
   */
  public Long getId() { return id; }
  /**
   * 标识。
   */
  /**
   * 设置标识。
   *
   * @param id 标识
   */
  public void setId(Long id) { this.id = id; }
  /**
   * 获取菜品标识。
   *
   * @return 获取菜品标识的结果
   */
  public Long getDishId() { return dishId; }
  /**
   * 菜品标识。
   */
  /**
   * 设置菜品标识。
   *
   * @param dishId 菜品标识
   */
  public void setDishId(Long dishId) { this.dishId = dishId; }
  /**
   * 获取StepNo。
   *
   * @return 获取StepNo的结果
   */
  public Integer getStepNo() { return stepNo; }
  /**
   * stepNo。
   */
  /**
   * 设置StepNo。
   *
   * @param stepNo stepNo
   */
  public void setStepNo(Integer stepNo) { this.stepNo = stepNo; }
  /**
   * 获取Title。
   *
   * @return 获取Title的结果
   */
  public String getTitle() { return title; }
  /**
   * title。
   */
  /**
   * 设置Title。
   *
   * @param title title
   */
  public void setTitle(String title) { this.title = title; }
  /**
   * 获取Content。
   *
   * @return 获取Content的结果
   */
  public String getContent() { return content; }
  /**
   * content。
   */
  /**
   * 设置Content。
   *
   * @param content content
   */
  public void setContent(String content) { this.content = content; }
}
