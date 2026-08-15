package com.familykitchen.user.service.impl;
import com.familykitchen.auth.security.PasswordCodec;
import com.familykitchen.auth.service.SessionService;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.user.mapper.AccountLifecycleMapper;
import com.familykitchen.user.mapper.UserMapper;
import com.familykitchen.user.service.AccountLifecycleService;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/** 账号注销冷静期服务实现。 */
@Service public class AccountLifecycleServiceImpl implements AccountLifecycleService {
  private final AccountLifecycleMapper lifecycle;private final UserMapper users;private final PasswordCodec passwords;private final SessionService sessions;
  /**
   * 创建AccountLifecycle实例。
   *
   * @param lifecycle lifecycle
   * @param users users
   * @param passwords passwords
   * @param sessions sessions
   */
  public AccountLifecycleServiceImpl(AccountLifecycleMapper lifecycle,UserMapper users,PasswordCodec passwords,SessionService sessions){
    this.lifecycle=lifecycle;this.users=users;this.passwords=passwords;this.sessions=sessions;}
  /**
   * 处理AccountLifecycle。
   *
   * @param userId 用户标识
   * @param password 密码
   */
  @Override @Transactional public void request(Long userId,String password){var user=users.findById(userId);
    if(user==null||!passwords.matches(password,user.getPasswordHash()))throw new BusinessException(ErrorCode.UNAUTHORIZED,"密码验证失败");
    if(lifecycle.countActiveFamily(userId)>0)throw new BusinessException(ErrorCode.BUSINESS_INVALID,"请先退出或解散家庭");
    if(lifecycle.countPendingOrders(userId)>0)throw new BusinessException(ErrorCode.BUSINESS_INVALID,"存在未完成订单");
    if(lifecycle.countFrozenWallet(userId)>0)throw new BusinessException(ErrorCode.BUSINESS_INVALID,"存在冻结资金");
    if(lifecycle.countPendingRequest(userId)>0)throw new BusinessException(ErrorCode.STATE_CONFLICT,"注销申请已存在");
    lifecycle.insertRequest(userId);users.updateStatus(userId,"CANCELLING");sessions.revokeAll(userId);}
  /**
   * 取消AccountLifecycle。
   *
   * @param userId 用户标识
   */
  @Override @Transactional public void cancel(Long userId){if(lifecycle.cancelRequest(userId)==0)
    throw new BusinessException(ErrorCode.NOT_FOUND,"没有待撤销的注销申请");users.updateStatus(userId,"ACTIVE");}
  /**
   * 处理Due。
   */
  @Override @Scheduled(cron="${family-kitchen.account-cancellation.cron:0 */10 * * * *}") @Transactional
  public void processDue(){for(Long userId:lifecycle.selectDueUsers())complete(userId);}
  private void complete(Long userId){byte[] b=new byte[8];new SecureRandom().nextBytes(b);
    if(users.anonymize(userId,"cancelled_"+userId+"_"+HexFormat.of().formatHex(b))>0){lifecycle.complete(userId);sessions.revokeAll(userId);}}
}
