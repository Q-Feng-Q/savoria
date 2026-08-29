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
import com.familykitchen.family.model.dto.UpdateFamilyInfoRequest;
import com.familykitchen.family.model.entity.AddressEntity;
import com.familykitchen.family.model.entity.FamilyMemberRecord;
import com.familykitchen.family.model.entity.FamilyRecord;
import com.familykitchen.family.model.vo.AddressView;
import com.familykitchen.family.model.vo.FamilyHomeResponse;
import com.familykitchen.family.model.vo.FamilyInfoView;
import com.familykitchen.family.model.vo.FamilyMenuItemView;
import com.familykitchen.family.service.FamilyApplicationService;
import com.familykitchen.wallet.model.vo.FamilyWalletSummaryView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  private final ObjectMapper objectMapper;

  /**
   * 创建家庭实例。
   *
   * @param familyMapper 家庭Mapper
   * @param dishMapper   菜品Mapper
   * @param objectMapper JSON序列化
   */
  public FamilyApplicationServiceImpl(
      FamilyMapper familyMapper,
      DishMapper dishMapper,
      ObjectMapper objectMapper
  ) {
    this.familyMapper = familyMapper;
    this.dishMapper = dishMapper;
    this.objectMapper = objectMapper;
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
    List<FamilyMemberRecord> members = familyMapper.selectMembers(user.familyId());
    FamilyHomeResponse.CrewSummary crew = buildCrew(family, members);
    List<FamilyHomeResponse.FeaturedDish> featuredDishes =
      familyMapper.selectFeaturedDishes(user.familyId(), 5);
    if (featuredDishes == null || featuredDishes.isEmpty()) {
      FamilyHomeResponse.FeaturedDish fallback = familyMapper.selectFallbackDish(user.familyId());
      featuredDishes = fallback == null ? List.of() : List.of(fallback);
    }
    return new FamilyHomeResponse(
      new FamilyHomeResponse.FamilySummary(family.getFamilyId(), family.getFamilyName(), family.getMerchantName()),
      new FamilyHomeResponse.MemberSummary(member.getMemberId(), member.getName(), member.getRoleTemplate()),
      crew,
      java.time.LocalDate.now(),
      featuredDishes.isEmpty() ? null : featuredDishes.get(0),
      featuredDishes,
      new FamilyWalletSummaryView(family.getFamilyId(), money(family.getFamilyWalletAvailable()),
          money(family.getFamilyWalletFrozen()),
          money(family.getFamilyWalletAvailable()).add(money(family.getFamilyWalletFrozen())),
          family.getFamilyWalletVersion(), null),
      List.of(
        new FamilyHomeResponse.DashboardCard("menu", "可点菜品", String.valueOf(family.getActiveMenuCount())),
        new FamilyHomeResponse.DashboardCard("address", "地址数量", String.valueOf(family.getAddressCount()))
      ),
      familyMapper.selectRecentOrders(user.familyId())
    );
  }

  private static FamilyHomeResponse.CrewSummary buildCrew(
      FamilyRecord family, List<FamilyMemberRecord> members) {
    List<FamilyMemberRecord> safeMembers = members == null ? List.of() : members;
    FamilyMemberRecord administrator = safeMembers.stream()
        .filter(FamilyApplicationServiceImpl::isFamilyAdministrator)
        .sorted((left, right) -> Integer.compare(rolePriority(left), rolePriority(right)))
        .findFirst()
        .orElse(null);
    FamilyMemberRecord ordinaryMember = safeMembers.stream()
        .filter(item -> "MEMBER".equals(normalizedRole(item)))
        .findFirst()
        .orElse(null);
    String chefName = displayName(family.getMerchantResponsibleName(), family.getMerchantName());
    String helperName = administrator == null ? "无帮厨" : displayName(administrator.getName(), "无帮厨");
    String tasterName = ordinaryMember != null
        ? displayName(ordinaryMember.getName(), administrator == null ? "无试吃员" : helperName)
        : (administrator == null ? "无试吃员" : helperName);
    return new FamilyHomeResponse.CrewSummary(chefName, helperName, tasterName);
  }

  private static boolean isFamilyAdministrator(FamilyMemberRecord member) {
    String role = normalizedRole(member);
    return "OWNER".equals(role) || "ADMIN".equals(role);
  }

  private static int rolePriority(FamilyMemberRecord member) {
    return "OWNER".equals(normalizedRole(member)) ? 0 : 1;
  }

  private static String normalizedRole(FamilyMemberRecord member) {
    return member == null || member.getRoleTemplate() == null
        ? "" : member.getRoleTemplate().trim().toUpperCase(Locale.ROOT);
  }

  private static String displayName(String value, String fallback) {
    if (value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim())) {
      return fallback;
    }
    return value.trim();
  }

  /**
   * 查询当前家庭业务资料。
   *
   * @param user 当前用户
   * @return 家庭业务资料
   */
  @Override
  public FamilyInfoView familyInfo(CurrentUserContext user) {
    FamilyRecord family = requireFamily(null, user.familyId());
    return new FamilyInfoView(
        family.getFamilyName(),
        family.getNote(),
        family.getMerchantName(),
        Boolean.TRUE.equals(family.getDeliveryEnabled()),
        money(family.getDeliveryFeeDefault()),
        Boolean.TRUE.equals(family.getDeliveryFree()));
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
   * @param user    用户
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
   * @param user      用户
   * @param addressId 地址标识
   * @param request   请求参数
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
   * @param user      用户
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
   * @param user      用户
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
   * @param user       用户
   * @param categoryId category标识
   * @param keyword    keyword
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
        "active",
        null,
        false,
        item.featuredAt(),
        item.featured()
      ))
      .toList();
  }

  /**
   * 处理详情。
   *
   * @param user   用户
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
      null,
      false,
      dishMapper.selectDishIngredients(dish.getId()).stream()
        .map(item -> new DishDetailView.IngredientView(item.getIngredientName(), money(item.getQuantity()), item.getUnit(), item.getCalcType()))
        .toList(),
      List.of()
    );
  }

  /**
   * 更新当前家庭名称与备注。
   *
   * @param user 当前用户
   * @param request 更新请求
   */
  @Override
  @Transactional
  public void updateFamilyInfo(CurrentUserContext user, UpdateFamilyInfoRequest request) {
    if (!user.hasFamilyAdminAccess()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "仅家庭管理员可修改家庭资料");
    }
    String familyName = request.familyName() == null ? "" : request.familyName().trim();
    if (familyName.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "家庭名称不能为空");
    }
    String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
    if (familyMapper.updateFamilyInfo(user.familyId(), familyName, note) == 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到家庭");
    }
  }

  private String toJson(List<String> values) {
    try {
      return objectMapper.writeValueAsString(values == null ? List.of() : values);
    } catch (Exception exception) {
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "联系人 JSON 序列化失败");
    }
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

  private static BigDecimal money(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
