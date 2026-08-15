package com.familykitchen.family.service;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.model.vo.DishDetailView;
import com.familykitchen.dish.model.vo.DishView;
import com.familykitchen.family.model.dto.AddressRequest;
import com.familykitchen.family.model.vo.AddressView;
import com.familykitchen.family.model.vo.FamilyHomeResponse;
import com.familykitchen.wallet.model.vo.WalletLedgerView;
import java.util.List;

/**
 * 家庭端基础服务。
 *
 * <p>聚合家庭小程序首页、菜单浏览、地址维护、餐次配置和成员钱包流水等家庭端常用能力。
 * 所有方法都以当前登录家庭成员上下文作为数据隔离依据。</p>
 */
public interface FamilyApplicationService {

  /**
   * 查询家庭端首页聚合信息。
   *
   * @param user 当前登录用户上下文
   * @return 首页信息，包含家庭资料、推荐菜、餐次和订单概览
   */
  FamilyHomeResponse home(CurrentUserContext user);

  /**
   * 查询当前家庭的收餐地址列表。
   *
   * @param user 当前登录用户上下文
   * @return 地址列表
   */
  List<AddressView> addresses(CurrentUserContext user);

  /**
   * 新增当前家庭的收餐地址。
   *
   * @param user 当前登录用户上下文
   * @param request 地址联系人、手机号、详细地址和默认标记
   * @return 新增后的地址视图
   */
  AddressView createAddress(CurrentUserContext user, AddressRequest request);

  /**
   * 修改当前家庭的收餐地址。
   *
   * @param user 当前登录用户上下文
   * @param addressId 地址 ID
   * @param request 地址修改内容
   * @return 修改后的地址视图
   */
  AddressView updateAddress(CurrentUserContext user, Long addressId, AddressRequest request);

  /**
   * 将指定地址设置为当前家庭默认地址。
   *
   * @param user 当前登录用户上下文
   * @param addressId 地址 ID
   */
  void setDefaultAddress(CurrentUserContext user, Long addressId);

  /**
   * 删除当前家庭的收餐地址。
   *
   * @param user 当前登录用户上下文
   * @param addressId 地址 ID
   */
  void deleteAddress(CurrentUserContext user, Long addressId);

  /**
   * 查询家庭可点菜品。
   *
   * @param user 当前登录用户上下文
   * @param categoryId 分类 ID，可为空
   * @param keyword 菜名或描述关键字，可为空
   * @return 当前家庭已启用菜单中的菜品列表
   */
  List<DishView> menuItems(CurrentUserContext user, Long categoryId, String keyword);

  /**
   * 查询家庭端菜品详情。
   *
   * @param user 当前登录用户上下文
   * @param dishId 菜品 ID
   * @return 菜品详情；家庭端不暴露商户内部制作流程
   */
  DishDetailView dishDetail(CurrentUserContext user, Long dishId);

  /**
   * 查询当前家庭启用的餐次。
   *
   * @param user 当前登录用户上下文
   * @return 早餐、午餐、晚餐等餐次配置
   */
  List<FamilyHomeResponse.MealSlotView> mealSlots(CurrentUserContext user);

  /**
   * 查询当前成员的钱包流水。
   *
   * @param user 当前登录用户上下文
   * @return 钱包余额变动记录
   */
  List<WalletLedgerView> walletLedgers(CurrentUserContext user);
}
