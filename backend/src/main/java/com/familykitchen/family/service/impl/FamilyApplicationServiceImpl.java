package com.familykitchen.family.service.impl;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.model.vo.DishDetailView;
import com.familykitchen.dish.model.vo.DishView;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.model.dto.AddressRequest;
import com.familykitchen.family.model.entity.AddressEntity;
import com.familykitchen.family.model.entity.FamilyMemberRecord;
import com.familykitchen.family.model.entity.FamilyRecord;
import com.familykitchen.family.model.entity.MealSlotRecord;
import com.familykitchen.family.model.vo.AddressView;
import com.familykitchen.family.model.vo.FamilyHomeResponse;
import com.familykitchen.family.model.vo.FamilyMenuItemView;
import com.familykitchen.family.service.FamilyApplicationService;
import com.familykitchen.wallet.mapper.WalletPersistenceMapper;
import com.familykitchen.wallet.model.entity.WalletLedgerDO;
import com.familykitchen.wallet.model.enums.LedgerType;
import com.familykitchen.wallet.model.vo.WalletLedgerView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 家庭端首页、菜单、地址和钱包视图应用服务实现。
 *
 * <p>聚合家庭小程序日常使用的核心读写能力，包括首页概览、菜单浏览、菜品详情、
 * 地址维护与成员钱包流水查询。</p>
 */
@Service
public class FamilyApplicationServiceImpl implements FamilyApplicationService {

  private final FamilyMapper familyMapper;
  private final DishMapper dishMapper;
  private final WalletPersistenceMapper walletMapper;

  /**
   * 创建家庭实例。
   *
   * @param familyMapper 家庭Mapper
   * @param dishMapper 菜品Mapper
   * @param walletMapper 钱包Mapper
   */
  public FamilyApplicationServiceImpl(
      FamilyMapper familyMapper,
      DishMapper dishMapper,
      WalletPersistenceMapper walletMapper
  ) {
    this.familyMapper = familyMapper;
    this.dishMapper = dishMapper;
    this.walletMapper = walletMapper;
  }

  /**
   * 处理家庭。
   *
   * @param user 用户
   * @return 处理的结果
   */
  @Override
  public FamilyHomeResponse home(CurrentUserContext user) {
    FamilyRecord family = requireFamily(user.merchantId(), user.familyId());
    FamilyMemberRecord member = familyMapper.selectMember(user.memberId());
    List<FamilyHomeResponse.MealSlotView> slots = mealSlots(user);
    return new FamilyHomeResponse(
        new FamilyHomeResponse.FamilySummary(family.getFamilyId(), family.getFamilyName(), family.getMerchantName()),
        new FamilyHomeResponse.MemberSummary(member.getMemberId(), member.getName(), member.getRoleTemplate()),
        java.time.LocalDate.now(),
        familyMapper.selectFeaturedDish(user.familyId()),
        List.of(
            new FamilyHomeResponse.DashboardCard("menu", "可点菜品", String.valueOf(family.getActiveMenuCount())),
            new FamilyHomeResponse.DashboardCard("address", "地址数量", String.valueOf(family.getAddressCount()))
        ),
        slots,
        familyMapper.selectRecentOrders(user.familyId())
    );
  }

  /**
   * 处理家庭。
   *
   * @param user 用户
   * @return 处理的结果
   */
  @Override
  public List<AddressView> addresses(CurrentUserContext user) {
    return familyMapper.selectAddresses(user.familyId()).stream().map(FamilyApplicationServiceImpl::toAddressView).toList();
  }

  /**
   * 创建地址。
   *
   * @param user 用户
   * @param request 请求参数
   * @return 创建地址的结果
   */
  @Override
  @Transactional
  public AddressView createAddress(CurrentUserContext user, AddressRequest request) {
    if (request.defaultAddress()) {
      familyMapper.clearDefaultAddress(user.familyId());
    }
    AddressEntity entity = toAddressEntity(user.familyId(), null, request);
    familyMapper.insertAddress(entity);
    return toAddressView(entity);
  }

  /**
   * 更新地址。
   *
   * @param user 用户
   * @param addressId 地址标识
   * @param request 请求参数
   * @return 更新地址的结果
   */
  @Override
  @Transactional
  public AddressView updateAddress(CurrentUserContext user, Long addressId, AddressRequest request) {
    if (request.defaultAddress()) {
      familyMapper.clearDefaultAddress(user.familyId());
    }
    AddressEntity entity = toAddressEntity(user.familyId(), addressId, request);
    if (familyMapper.updateAddress(entity) == 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到地址");
    }
    return toAddressView(entity);
  }

  /**
   * 设置Default地址。
   *
   * @param user 用户
   * @param addressId 地址标识
   */
  @Override
  @Transactional
  public void setDefaultAddress(CurrentUserContext user, Long addressId) {
    familyMapper.clearDefaultAddress(user.familyId());
    if (familyMapper.setDefaultAddress(user.familyId(), addressId) == 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到地址");
    }
  }

  /**
   * 删除地址。
   *
   * @param user 用户
   * @param addressId 地址标识
   */
  @Override
  @Transactional
  public void deleteAddress(CurrentUserContext user, Long addressId) {
    if (familyMapper.deleteAddress(user.familyId(), addressId) == 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到地址");
    }
  }

  /**
   * 处理项目列表。
   *
   * @param user 用户
   * @param categoryId category标识
   * @param keyword keyword
   * @return 处理项目列表的结果
   */
  @Override
  public List<DishView> menuItems(CurrentUserContext user, Long categoryId, String keyword) {
    return familyMapper.selectFamilyMenuItems(user.merchantId(), user.familyId()).stream()
        // 家庭端只能看到商户为该家庭启用的菜品。
        .filter(FamilyMenuItemView::enabled)
        .filter(item -> categoryId == null || categoryId.equals(item.categoryId()))
        .filter(item -> keyword == null
            || keyword.isBlank()
            || item.dishName().toLowerCase().contains(keyword.toLowerCase())
            || (item.description() != null && item.description().toLowerCase().contains(keyword.toLowerCase())))
        // 家庭专属价优先于商户基础价，方便商户给不同家庭设置不同餐费。
        .map(item -> new DishView(
            item.dishId(),
            item.categoryId(),
            item.dishName(),
            item.description(),
            item.imageUrl(),
            item.familyFinalPrice() == null ? item.basePrice() : item.familyFinalPrice(),
            "active"
        ))
        .toList();
  }

  /**
   * 处理详情。
   *
   * @param user 用户
   * @param dishId 菜品标识
   * @return 处理详情的结果
   */
  @Override
  public DishDetailView dishDetail(CurrentUserContext user, Long dishId) {
    DishEntity dish = dishMapper.selectDish(user.merchantId(), dishId);
    if (dish == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到菜品");
    }
    return new DishDetailView(
        dish.getId(),
        dish.getCategoryId(),
        dish.getName(),
        dish.getDescription(),
        dish.getImageUrl(),
        money(dish.getBasePrice()),
        dish.getStatus(),
        dishMapper.selectDishIngredients(dish.getId()).stream()
            .map(item -> new DishDetailView.IngredientView(item.getIngredientName(), money(item.getQuantity()), item.getUnit(), item.getCalcType()))
            .toList(),
        List.of()
    );
  }

  /**
   * 处理Slots。
   *
   * @param user 用户
   * @return 处理Slots的结果
   */
  @Override
  public List<FamilyHomeResponse.MealSlotView> mealSlots(CurrentUserContext user) {
    return familyMapper.selectMealSlots(user.familyId()).stream()
        .map(FamilyApplicationServiceImpl::toMealSlotView)
        .toList();
  }

  /**
   * 处理Ledgers。
   *
   * @param user 用户
   * @return 处理Ledgers的结果
   */
  @Override
  public List<WalletLedgerView> walletLedgers(CurrentUserContext user) {
    return walletMapper.selectWalletLedgers(user.memberId()).stream()
        .map(FamilyApplicationServiceImpl::toWalletLedgerView)
        .toList();
  }

  private FamilyRecord requireFamily(Long merchantId, Long familyId) {
    FamilyRecord family = familyMapper.selectFamily(merchantId, familyId);
    if (family == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到家庭");
    }
    return family;
  }

  private static AddressEntity toAddressEntity(Long familyId, Long addressId, AddressRequest request) {
    AddressEntity entity = new AddressEntity();
    entity.setId(addressId);
    entity.setFamilyId(familyId);
    entity.setContactName(request.contactName());
    entity.setContactPhone(request.contactPhone());
    entity.setAddressText(request.addressText());
    entity.setDefaultAddress(request.defaultAddress());
    return entity;
  }

  private static AddressView toAddressView(AddressEntity entity) {
    return new AddressView(
        entity.getId(),
        entity.getContactName(),
        entity.getContactPhone(),
        entity.getAddressText(),
        Boolean.TRUE.equals(entity.getDefaultAddress())
    );
  }

  private static FamilyHomeResponse.MealSlotView toMealSlotView(MealSlotRecord row) {
    return new FamilyHomeResponse.MealSlotView(row.getMealSlotId(), row.getName(), row.getDisplayTime(), false);
  }

  private static WalletLedgerView toWalletLedgerView(WalletLedgerDO ledger) {
    return new WalletLedgerView(
        ledger.getId(),
        ledger.getMemberId(),
        LedgerType.valueOf(ledger.getType()),
        money(ledger.getAmount()),
        money(ledger.getBalanceBefore()),
        money(ledger.getBalanceAfter()),
        money(ledger.getFrozenBefore()),
        money(ledger.getFrozenAfter()),
        ledger.getRemark(),
        ledger.getCreatedAt()
    );
  }

  private static BigDecimal money(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
