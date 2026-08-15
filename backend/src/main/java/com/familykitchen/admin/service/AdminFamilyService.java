package com.familykitchen.admin.service;

import com.familykitchen.admin.model.dto.AdminFamilyMemberUpdateRequest;
import com.familykitchen.admin.model.dto.AdminFamilyUpdateRequest;
import com.familykitchen.admin.model.vo.AdminFamilyDetailView;
import com.familykitchen.admin.model.vo.AdminFamilyOptionView;
import com.familykitchen.family.model.dto.AddressRequest;
import java.util.List;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.model.dto.SaveFamilyMenuRequest;
import com.familykitchen.family.model.vo.FamilyMenuItemView;
import com.familykitchen.wallet.model.dto.AdjustMemberBalanceRequest;
import com.familykitchen.wallet.model.vo.WalletLedgerView;

/**
 * 定义平台管理家庭相关的应用服务能力与调用边界。
 */
public interface AdminFamilyService {
  /**
   * 处理平台管理家庭。
   *
   * @return 处理结果
   */
  List<AdminFamilyOptionView> options();
  /**
   * 处理平台管理家庭。
   *
   * @param familyId 家庭标识
   * @return 处理结果
   */
  AdminFamilyDetailView detail(Long familyId);
  /**
   * 更新家庭。
   *
   * @param familyId 家庭标识
   * @param request 请求参数
   */
  void updateFamily(Long familyId, AdminFamilyUpdateRequest request);
  /**
   * 停用家庭。
   *
   * @param familyId 家庭标识
   */
  void disableFamily(Long familyId);
  /**
   * 更新成员。
   *
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @param request 请求参数
   */
  void updateMember(Long familyId, Long memberId, AdminFamilyMemberUpdateRequest request);
  /**
   * 停用成员。
   *
   * @param familyId 家庭标识
   * @param memberId 成员标识
   */
  void disableMember(Long familyId, Long memberId);
  /**
   * 创建地址。
   *
   * @param familyId 家庭标识
   * @param request 请求参数
   */
  void createAddress(Long familyId, AddressRequest request);
  /**
   * 更新地址。
   *
   * @param familyId 家庭标识
   * @param addressId 地址标识
   * @param request 请求参数
   */
  void updateAddress(Long familyId, Long addressId, AddressRequest request);
  /**
   * 删除地址。
   *
   * @param familyId 家庭标识
   * @param addressId 地址标识
   */
  void deleteAddress(Long familyId, Long addressId);
  /**
   * 设置Default地址。
   *
   * @param familyId 家庭标识
   * @param addressId 地址标识
   */
  void setDefaultAddress(Long familyId, Long addressId);
  /**
   * 处理平台管理家庭。
   *
   * @param user 用户
   * @param familyId 家庭标识
   * @return 处理结果
   */
  List<FamilyMenuItemView> menu(CurrentUserContext user, Long familyId);
  /**
   * 保存菜单。
   *
   * @param user 用户
   * @param familyId 家庭标识
   * @param request 请求参数
   */
  void saveMenu(CurrentUserContext user, Long familyId, SaveFamilyMenuRequest request);
  /**
   * 处理Ledgers。
   *
   * @param user 用户
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @return 处理Ledgers后的结果
   */
  List<WalletLedgerView> walletLedgers(CurrentUserContext user, Long familyId, Long memberId);
  /**
   * 处理余额。
   *
   * @param user 用户
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @param request 请求参数
   * @return 处理余额后的结果
   */
  WalletLedgerView adjustBalance(CurrentUserContext user, Long familyId, Long memberId, AdjustMemberBalanceRequest request);
}
