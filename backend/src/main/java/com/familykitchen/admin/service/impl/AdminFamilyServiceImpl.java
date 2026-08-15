package com.familykitchen.admin.service.impl;

import com.familykitchen.admin.mapper.AdminFamilyMapper;
import com.familykitchen.admin.model.dto.AdminFamilyMemberUpdateRequest;
import com.familykitchen.admin.model.dto.AdminFamilyUpdateRequest;
import com.familykitchen.admin.model.vo.AdminFamilyDetailView;
import com.familykitchen.admin.model.vo.AdminFamilyOptionView;
import com.familykitchen.admin.service.AdminFamilyService;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.family.model.dto.AddressRequest;
import com.familykitchen.family.model.entity.AddressEntity;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.model.dto.SaveFamilyMenuRequest;
import com.familykitchen.family.model.vo.FamilyMenuItemView;
import com.familykitchen.family.service.MerchantFamilyMenuApplicationService;
import com.familykitchen.wallet.model.dto.AdjustMemberBalanceRequest;
import com.familykitchen.wallet.model.vo.WalletLedgerView;
import com.familykitchen.wallet.service.MerchantWalletApplicationService;

/**
 * 实现平台管理家庭相关业务编排，并协调校验与持久化操作。
 */
@Service
public class AdminFamilyServiceImpl implements AdminFamilyService {
  private final AdminFamilyMapper mapper;
  private final MerchantFamilyMenuApplicationService menuService;
  private final MerchantWalletApplicationService walletService;

  /**
   * 创建平台管理家庭实例。
   *
   * @param mapper mapper
   * @param menuService 菜单Service
   * @param walletService 钱包Service
   */
  public AdminFamilyServiceImpl(AdminFamilyMapper mapper, MerchantFamilyMenuApplicationService menuService,
                                MerchantWalletApplicationService walletService) {
    this.mapper = mapper;
    this.menuService = menuService;
    this.walletService = walletService;
  }

  /**
   * 处理平台管理家庭。
   *
   * @return 处理结果
   */
  public List<AdminFamilyOptionView> options() { return mapper.selectFamilyOptions(); }

  /**
   * 处理平台管理家庭。
   *
   * @param familyId 家庭标识
   * @return 处理结果
   */
  public AdminFamilyDetailView detail(Long familyId) {
    AdminFamilyOptionView family = requireFamily(familyId);
    return new AdminFamilyDetailView(family.familyId(), family.familyName(), family.merchantId(),
        family.merchantName(), family.status(), mapper.selectFamilyNote(familyId),
        mapper.selectDeliveryEnabled(familyId), mapper.selectDeliveryFee(familyId),
        mapper.selectDeliveryFree(familyId), mapper.selectMembers(familyId), mapper.selectAddresses(familyId));
  }

  /**
   * 更新家庭。
   *
   * @param familyId 家庭标识
   * @param request 请求参数
   */
  public void updateFamily(Long familyId, AdminFamilyUpdateRequest request) {
    requireFamily(familyId);
    mapper.updateFamily(familyId, request);
  }

  /**
   * 停用家庭。
   *
   * @param familyId 家庭标识
   */
  public void disableFamily(Long familyId) { mapper.disableFamily(familyId); }

  /**
   * 更新成员。
   *
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @param request 请求参数
   */
  public void updateMember(Long familyId, Long memberId, AdminFamilyMemberUpdateRequest request) {
    if (mapper.updateMember(familyId, memberId, request) == 0) throw notFound("家庭成员不存在");
  }

  /**
   * 停用成员。
   *
   * @param familyId 家庭标识
   * @param memberId 成员标识
   */
  public void disableMember(Long familyId, Long memberId) {
    if (mapper.disableMember(familyId, memberId) == 0) throw notFound("家庭成员不存在");
  }

  /**
   * 创建地址。
   *
   * @param familyId 家庭标识
   * @param request 请求参数
   */
  @Transactional
  public void createAddress(Long familyId, AddressRequest request) {
    requireFamily(familyId);
    if (request.defaultAddress()) mapper.clearDefaultAddress(familyId);
    mapper.insertAddress(toAddress(familyId, request));
  }

  /**
   * 更新地址。
   *
   * @param familyId 家庭标识
   * @param addressId 地址标识
   * @param request 请求参数
   */
  public void updateAddress(Long familyId, Long addressId, AddressRequest request) {
    if (request.defaultAddress()) mapper.clearDefaultAddress(familyId);
    if (mapper.updateAddress(familyId, addressId, toAddress(familyId, request)) == 0) throw notFound("家庭地址不存在");
    if (request.defaultAddress()) mapper.setDefaultAddress(familyId, addressId);
  }

  /**
   * 删除地址。
   *
   * @param familyId 家庭标识
   * @param addressId 地址标识
   */
  public void deleteAddress(Long familyId, Long addressId) {
    if (mapper.deleteAddress(familyId, addressId) == 0) throw notFound("家庭地址不存在");
  }

  /**
   * 设置Default地址。
   *
   * @param familyId 家庭标识
   * @param addressId 地址标识
   */
  @Transactional
  public void setDefaultAddress(Long familyId, Long addressId) {
    mapper.clearDefaultAddress(familyId);
    if (mapper.setDefaultAddress(familyId, addressId) == 0) throw notFound("家庭地址不存在");
  }

  /**
   * 处理平台管理家庭。
   *
   * @param user 用户
   * @param familyId 家庭标识
   * @return 处理结果
   */
  public List<FamilyMenuItemView> menu(CurrentUserContext user, Long familyId) {
    return menuService.menu(withFamilyMerchant(user, familyId), familyId);
  }

  /**
   * 保存菜单。
   *
   * @param user 用户
   * @param familyId 家庭标识
   * @param request 请求参数
   */
  public void saveMenu(CurrentUserContext user, Long familyId, SaveFamilyMenuRequest request) {
    menuService.saveMenu(withFamilyMerchant(user, familyId), familyId, request);
  }

  /**
   * 处理Ledgers。
   *
   * @param user 用户
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @return 处理Ledgers后的结果
   */
  public List<WalletLedgerView> walletLedgers(CurrentUserContext user, Long familyId, Long memberId) {
    requireMember(familyId, memberId);
    return walletService.walletLedgers(user, memberId);
  }

  /**
   * 处理余额。
   *
   * @param user 用户
   * @param familyId 家庭标识
   * @param memberId 成员标识
   * @param request 请求参数
   * @return 处理余额后的结果
   */
  public WalletLedgerView adjustBalance(CurrentUserContext user, Long familyId, Long memberId,
                                        AdjustMemberBalanceRequest request) {
    requireMember(familyId, memberId);
    return walletService.adjustBalance(user, memberId, request);
  }

  private CurrentUserContext withFamilyMerchant(CurrentUserContext user, Long familyId) {
    AdminFamilyOptionView family = requireFamily(familyId);
    return new CurrentUserContext(user.userId(), family.merchantId(), familyId, user.memberId(),
        user.roleTemplate(), user.backendRoles(), user.merchantAdminScopes());
  }

  private void requireMember(Long familyId, Long memberId) {
    if (mapper.countMember(familyId, memberId) == 0) throw notFound("家庭成员不存在");
  }

  private AdminFamilyOptionView requireFamily(Long familyId) {
    AdminFamilyOptionView family = mapper.selectFamilyOption(familyId);
    if (family == null) throw notFound("家庭不存在");
    return family;
  }

  private static AddressEntity toAddress(Long familyId, AddressRequest request) {
    AddressEntity entity = new AddressEntity();
    entity.setFamilyId(familyId);
    entity.setContactName(request.contactName());
    entity.setContactPhone(request.contactPhone());
    entity.setAddressText(request.addressText());
    entity.setDefaultAddress(request.defaultAddress());
    return entity;
  }

  private static BusinessException notFound(String message) {
    return new BusinessException(ErrorCode.NOT_FOUND, message);
  }
}
