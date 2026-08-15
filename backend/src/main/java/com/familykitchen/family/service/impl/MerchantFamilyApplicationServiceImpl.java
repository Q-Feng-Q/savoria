package com.familykitchen.family.service.impl;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.model.dto.UpdateFamilyDeliveryPolicyRequest;
import com.familykitchen.family.model.dto.UpdateMerchantFamilyProfileRequest;
import com.familykitchen.family.model.entity.FamilyMemberRecord;
import com.familykitchen.family.model.entity.FamilyRecord;
import com.familykitchen.family.model.vo.MerchantFamilyDetailView;
import com.familykitchen.family.model.vo.MerchantFamilySummaryView;
import com.familykitchen.family.service.MerchantFamilyApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商户侧家庭资料应用服务实现。
 *
 * <p>用于商户后台查看已绑定家庭、读取家庭详情、维护家庭基础资料以及更新配送策略。</p>
 */
@Service
public class MerchantFamilyApplicationServiceImpl implements MerchantFamilyApplicationService {

  private final FamilyMapper familyMapper;
  private final ObjectMapper objectMapper;

  /**
   * 创建商户家庭实例。
   *
   * @param familyMapper 家庭Mapper
   * @param objectMapper objectMapper
   */
  public MerchantFamilyApplicationServiceImpl(FamilyMapper familyMapper, ObjectMapper objectMapper) {
    this.familyMapper = familyMapper;
    this.objectMapper = objectMapper;
  }

  /**
   * 列出Families。
   *
   * @param user 用户
   * @return 列出Families的结果
   */
  @Override
  public List<MerchantFamilySummaryView> listFamilies(CurrentUserContext user) {
    return familyMapper.selectMerchantFamilies(user.merchantId()).stream()
        .map(this::toSummaryView)
        .toList();
  }

  /**
   * 处理商户家庭。
   *
   * @param user 用户
   * @param familyId 家庭标识
   * @return 处理的结果
   */
  @Override
  public MerchantFamilyDetailView detail(CurrentUserContext user, Long familyId) {
    FamilyRecord family = requireFamily(user.merchantId(), familyId);
    return new MerchantFamilyDetailView(
        family.getFamilyId(),
        family.getFamilyName(),
        family.getMerchantId(),
        family.getMerchantName(),
        family.getNote(),
        contactNames(family.getContactNamesJson()),
        Boolean.TRUE.equals(family.getDeliveryEnabled()),
        money(family.getDeliveryFeeDefault()),
        Boolean.TRUE.equals(family.getDeliveryFree()),
        deliverySummary(family),
        familyMapper.selectAddresses(familyId).stream()
            .map(item -> new com.familykitchen.family.model.vo.AddressView(
                item.getId(),
                item.getContactName(),
                item.getContactPhone(),
                item.getAddressText(),
                Boolean.TRUE.equals(item.getDefaultAddress())
            ))
            .toList(),
        familyMapper.selectMembers(familyId).stream().map(MerchantFamilyApplicationServiceImpl::toMemberView).toList(),
        intValue(family.getActiveMenuCount())
    );
  }

  /**
   * 更新资料。
   *
   * @param user 用户
   * @param familyId 家庭标识
   * @param request 请求参数
   */
  @Override
  @Transactional
  public void updateProfile(CurrentUserContext user, Long familyId, UpdateMerchantFamilyProfileRequest request) {
    if (familyMapper.updateFamilyProfile(
        user.merchantId(),
        familyId,
        request.familyName(),
        request.note(),
        toJson(request.contactNames())
    ) == 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到家庭");
    }
  }

  /**
   * 更新配送Policy。
   *
   * @param user 用户
   * @param familyId 家庭标识
   * @param request 请求参数
   */
  @Override
  @Transactional
  public void updateDeliveryPolicy(CurrentUserContext user, Long familyId, UpdateFamilyDeliveryPolicyRequest request) {
    if (familyMapper.updateFamilyDeliveryPolicy(
        user.merchantId(),
        familyId,
        request.deliveryEnabled(),
        request.deliveryFeeDefault(),
        request.deliveryFree()
    ) == 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到家庭");
    }
  }

  /**
   * 校验家庭归属当前商户，并返回家庭档案实体。
   */
  private FamilyRecord requireFamily(Long merchantId, Long familyId) {
    FamilyRecord family = familyMapper.selectFamily(merchantId, familyId);
    if (family == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "未找到家庭");
    }
    return family;
  }

  /**
   * 将家庭档案记录转换为商户后台列表视图。
   */
  private MerchantFamilySummaryView toSummaryView(FamilyRecord family) {
    return new MerchantFamilySummaryView(
        family.getFamilyId(),
        family.getFamilyName(),
        family.getMerchantName(),
        family.getNote(),
        contactNames(family.getContactNamesJson()),
        Boolean.TRUE.equals(family.getDeliveryEnabled()),
        money(family.getDeliveryFeeDefault()),
        Boolean.TRUE.equals(family.getDeliveryFree()),
        deliverySummary(family),
        family.getDefaultAddressText(),
        intValue(family.getAddressCount()),
        intValue(family.getMemberCount()),
        intValue(family.getActiveMenuCount()),
        intValue(family.getLowBalanceMemberCount()),
        money(family.getFrozenBalanceTotal())
    );
  }

  /**
   * 解析联系人 JSON，解析失败时按空列表兜底，避免影响家庭档案展示。
   */
  private List<String> contactNames(String json) {
    try {
      if (json == null || json.isBlank()) {
        return List.of();
      }
      return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
      });
    } catch (Exception exception) {
      return List.of();
    }
  }

  /**
   * 序列化联系人列表，统一将空值写为空数组字符串。
   */
  private String toJson(List<String> values) {
    try {
      return objectMapper.writeValueAsString(values == null ? List.of() : values);
    } catch (Exception exception) {
      throw new IllegalStateException("联系人 JSON 序列化失败", exception);
    }
  }

  /**
   * 转换家庭成员视图，并根据可用余额标记低余额状态。
   */
  private static MerchantFamilyDetailView.MemberView toMemberView(FamilyMemberRecord member) {
    BigDecimal available = money(member.getAvailableBalance());
    return new MerchantFamilyDetailView.MemberView(
        member.getMemberId(),
        member.getName(),
        available,
        money(member.getFrozenBalance()),
        available.compareTo(new BigDecimal("20.00")) < 0
    );
  }

  /**
   * 生成商户后台可读的配送策略摘要文案。
   */
  private static String deliverySummary(FamilyRecord family) {
    if (!Boolean.TRUE.equals(family.getDeliveryEnabled())) {
      return "不配送";
    }
    if (Boolean.TRUE.equals(family.getDeliveryFree())) {
      return "免配送费";
    }
    return "配送费 " + money(family.getDeliveryFeeDefault());
  }

  private static int intValue(Integer value) {
    return value == null ? 0 : value;
  }

  private static BigDecimal money(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
