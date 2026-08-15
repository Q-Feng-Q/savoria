package com.familykitchen.admin.service;

import com.familykitchen.admin.mapper.AdminUserMapper;
import com.familykitchen.admin.model.vo.AdminUserView;
import com.familykitchen.auth.service.SessionService;
import com.familykitchen.auth.security.PasswordCodec;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import java.util.List;
import java.math.BigDecimal;
import com.familykitchen.user.mapper.UserMapper;
import com.familykitchen.user.model.entity.UserDO;
import com.familykitchen.wallet.mapper.WalletPersistenceMapper;
import com.familykitchen.wallet.model.entity.WalletAccountDO;
import com.familykitchen.admin.model.dto.AdminUserCreateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 定义平台管理用户相关的应用服务能力与调用边界。
 */
@Service
public class AdminUserService {
  private final AdminUserMapper mapper;
  private final SessionService sessions;
  private final UserMapper users; private final PasswordCodec passwords; private final WalletPersistenceMapper wallets;
  /**
   * 创建平台管理用户实例。
   *
   * @param mapper mapper
   * @param sessions sessions
   * @param users users
   * @param passwords passwords
   * @param wallets wallets
   */
  public AdminUserService(AdminUserMapper mapper, SessionService sessions, UserMapper users, PasswordCodec passwords, WalletPersistenceMapper wallets) { this.mapper=mapper; this.sessions=sessions; this.users=users; this.passwords=passwords; this.wallets=wallets; }
  /**
   * 列出平台管理用户。
   *
   * @param keyword keyword
   * @return 列出结果后的结果
   */
  public List<AdminUserView> list(String keyword) { return mapper.list(keyword == null ? null : keyword.trim()); }
  /**
   * 创建平台管理用户。
   *
   * @param body 请求体
   * @return 创建结果后的结果
   */
  @Transactional public Long create(AdminUserCreateRequest body) {
    String username=body.username().trim().toLowerCase();
    if(users.findByUsername(username)!=null)throw new BusinessException(ErrorCode.STATE_CONFLICT,"用户名已被使用");
    UserDO user=new UserDO();user.setUsername(username);user.setUsernameChanged(false);user.setPasswordHash(passwords.encode(body.password()));user.setPasswordAlgorithm("BCRYPT");user.setCredentialStatus("ACTIVE");user.setNickname(body.name().trim());user.setMobile(normalizeNullable(body.mobile()));user.setStatus("ACTIVE");users.insert(user);
    WalletAccountDO wallet=new WalletAccountDO();wallet.setMemberId(user.getId());wallet.setBalanceAmount(BigDecimal.ZERO);wallet.setFrozenAmount(BigDecimal.ZERO);wallets.insertWalletAccount(wallet);
    if(body.platformAdmin())mapper.enablePlatformRole(user.getId());return user.getId();
  }
  private static String normalizeNullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
  /**
   * 更新状态。
   *
   * @param operatorId 操作人标识
   * @param userId 用户标识
   * @param status 状态
   */
  @Transactional public void updateStatus(Long operatorId, Long userId, String status) {
    if (operatorId.equals(userId) && !"ACTIVE".equals(status)) throw new BusinessException(ErrorCode.BUSINESS_INVALID,"不能停用当前登录账号");
    if (mapper.updateStatus(userId,status)==0) throw new BusinessException(ErrorCode.NOT_FOUND,"用户不存在");
    if (!"ACTIVE".equals(status)) sessions.revokeAll(userId);
  }
  /**
   * 更新Platform角色。
   *
   * @param operatorId 操作人标识
   * @param userId 用户标识
   * @param enabled 是否启用
   */
  @Transactional public void updatePlatformRole(Long operatorId, Long userId, boolean enabled) {
    if (operatorId.equals(userId) && !enabled) throw new BusinessException(ErrorCode.BUSINESS_INVALID,"不能移除自己的平台管理员角色");
    if (enabled) mapper.enablePlatformRole(userId); else mapper.disablePlatformRole(userId);
    sessions.revokeAll(userId);
  }
  /**
   * 删除平台管理用户。
   *
   * @param operatorId 操作人标识
   * @param userId 用户标识
   */
  @Transactional public void delete(Long operatorId, Long userId) {
    if (operatorId.equals(userId)) throw new BusinessException(ErrorCode.BUSINESS_INVALID,"不能删除当前登录账号");
    if (mapper.anonymizeUser(userId)==0) throw new BusinessException(ErrorCode.NOT_FOUND,"用户不存在或已删除");
    mapper.disablePlatformRoles(userId);
    mapper.disableFamilyRelations(userId);
    mapper.disableMerchantRelations(userId);
    sessions.revokeAll(userId);
  }
}
