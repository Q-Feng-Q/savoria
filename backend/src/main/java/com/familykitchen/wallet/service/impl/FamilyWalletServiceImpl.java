package com.familykitchen.wallet.service.impl;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.wallet.mapper.FamilyWalletMapper;
import com.familykitchen.wallet.model.bo.FamilyWalletAccount;
import com.familykitchen.wallet.model.entity.FamilyWalletAccountDO;
import com.familykitchen.wallet.model.entity.FamilyWalletLedgerDO;
import com.familykitchen.wallet.model.entity.FamilyWalletOrderHoldDO;
import com.familykitchen.wallet.service.FamilyWalletService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Default transaction coordinator for family-wallet mutations. */
@Service
public class FamilyWalletServiceImpl implements FamilyWalletService {
  private static final BigDecimal ZERO = new BigDecimal("0.00");
  private final FamilyWalletMapper mapper;
  /** Creates the service.
   * @param mapper persistence mapper
   */
  public FamilyWalletServiceImpl(FamilyWalletMapper mapper) { this.mapper=mapper; }

  /** {@inheritDoc} */
  @Override public FamilyWalletAccountDO get(long familyId) {
    FamilyWalletAccountDO row=mapper.selectAccount(familyId);
    if(row==null) throw new BusinessException(ErrorCode.NOT_FOUND,"家庭钱包不存在");
    return row;
  }

  /** {@inheritDoc} */
  @Override @Transactional
  public void freezeNewOrder(long familyId,long orderId,long actorId,BigDecimal amount,String key){
    FamilyWalletAccountDO row=lockAccount(familyId);
    if(mapper.lockHold(orderId)!=null) conflict("订单冻结单已存在");
    FamilyWalletAccount account=domain(row); BigDecimal beforeA=account.availableAmount(),beforeF=account.frozenAmount();
    account.freeze(amount);
    FamilyWalletOrderHoldDO hold=new FamilyWalletOrderHoldDO(); hold.orderId=orderId;hold.familyId=familyId;
    hold.initialAmount=FamilyWalletAccount.command(amount);hold.additionalFrozenAmount=ZERO;
    hold.remainingFrozenAmount=FamilyWalletAccount.command(amount);hold.capturedAmount=ZERO;hold.releasedAmount=ZERO;hold.refundedAmount=ZERO;hold.status="ACTIVE";
    mapper.insertHold(hold); persist(account); ledger(familyId,orderId,actorId,"ORDER_FREEZE",key,amount,beforeA,beforeF,account,"订单初始冻结");
  }

  /** {@inheritDoc} */
  @Override @Transactional
  public void appendFreeze(long familyId,long orderId,long actorId,BigDecimal amount,String key){
    FamilyWalletOrderHoldDO hold=lockHold(orderId,familyId); FamilyWalletAccount account=domain(lockAccount(familyId));
    BigDecimal value=FamilyWalletAccount.command(amount),beforeA=account.availableAmount(),beforeF=account.frozenAmount(); account.freeze(value);
    hold.additionalFrozenAmount=hold.additionalFrozenAmount.add(value);hold.remainingFrozenAmount=hold.remainingFrozenAmount.add(value);hold.status="ACTIVE";
    mapper.updateHold(hold);persist(account);ledger(familyId,orderId,actorId,"ORDER_APPEND_FREEZE",key,value,beforeA,beforeF,account,"订单追加冻结");
  }

  /** {@inheritDoc} */
  @Override @Transactional
  public void release(long familyId,long orderId,long actorId,BigDecimal amount,String key){
    FamilyWalletOrderHoldDO hold=lockHold(orderId,familyId); BigDecimal value=FamilyWalletAccount.command(amount);requireAtLeast(hold.remainingFrozenAmount,value,"释放金额超过订单剩余冻结");
    FamilyWalletAccount account=domain(lockAccount(familyId));BigDecimal beforeA=account.availableAmount(),beforeF=account.frozenAmount();account.release(value);
    hold.remainingFrozenAmount=hold.remainingFrozenAmount.subtract(value);hold.releasedAmount=hold.releasedAmount.add(value);hold.status=hold.remainingFrozenAmount.signum()==0?"RELEASED":"ACTIVE";
    mapper.updateHold(hold);persist(account);ledger(familyId,orderId,actorId,"ORDER_RELEASE",key,value,beforeA,beforeF,account,"订单释放冻结");
  }

  /** {@inheritDoc} */
  @Override @Transactional
  public void capture(long familyId,long orderId,long actorId,BigDecimal amount,String key){
    FamilyWalletOrderHoldDO hold=lockHold(orderId,familyId);BigDecimal value=FamilyWalletAccount.command(amount);requireAtLeast(hold.remainingFrozenAmount,value,"扣款金额超过订单剩余冻结");
    FamilyWalletAccount account=domain(lockAccount(familyId));BigDecimal beforeA=account.availableAmount(),beforeF=account.frozenAmount();account.capture(value);
    hold.remainingFrozenAmount=hold.remainingFrozenAmount.subtract(value);hold.capturedAmount=hold.capturedAmount.add(value);hold.status=hold.remainingFrozenAmount.signum()==0?"CAPTURED":"ACTIVE";
    mapper.updateHold(hold);persist(account);ledger(familyId,orderId,actorId,"ORDER_CAPTURE",key,value,beforeA,beforeF,account,"订单确认扣款");
  }

  /** {@inheritDoc} */
  @Override @Transactional
  public void refund(long familyId,long orderId,long actorId,BigDecimal amount,String key){
    FamilyWalletOrderHoldDO hold=lockHold(orderId,familyId);BigDecimal value=FamilyWalletAccount.command(amount);
    requireAtLeast(hold.capturedAmount.subtract(hold.refundedAmount),value,"退款金额超过订单已扣未退金额");
    FamilyWalletAccount account=domain(lockAccount(familyId));BigDecimal beforeA=account.availableAmount(),beforeF=account.frozenAmount();account.refund(value);
    hold.refundedAmount=hold.refundedAmount.add(value);hold.status=hold.refundedAmount.compareTo(hold.capturedAmount)==0?"REFUNDED":"PARTIALLY_REFUNDED";
    mapper.updateHold(hold);persist(account);ledger(familyId,orderId,actorId,"ORDER_REFUND",key,value,beforeA,beforeF,account,"订单退款");
  }

  /** {@inheritDoc} */
  @Override @Transactional public void manualCredit(long familyId,long actorId,BigDecimal amount,String key,String remark){adjust(familyId,actorId,amount,key,remark,true);}
  /** {@inheritDoc} */
  @Override @Transactional public void manualDebit(long familyId,long actorId,BigDecimal amount,String key,String remark){adjust(familyId,actorId,amount,key,remark,false);}

  private void adjust(long familyId,long actorId,BigDecimal amount,String key,String remark,boolean credit){
    FamilyWalletAccount account=domain(lockAccount(familyId));BigDecimal beforeA=account.availableAmount(),beforeF=account.frozenAmount();
    if(credit)account.manualCredit(amount);else account.manualDebit(amount);persist(account);
    ledger(familyId,null,actorId,credit?"MANUAL_CREDIT":"MANUAL_DEBIT",key,amount,beforeA,beforeF,account,remark);
  }
  private FamilyWalletAccountDO lockAccount(long id){FamilyWalletAccountDO row=mapper.lockAccount(id);if(row==null)throw new BusinessException(ErrorCode.NOT_FOUND,"家庭钱包不存在");return row;}
  private FamilyWalletOrderHoldDO lockHold(long orderId,long familyId){FamilyWalletOrderHoldDO h=mapper.lockHold(orderId);if(h==null)throw new BusinessException(ErrorCode.NOT_FOUND,"订单冻结单不存在");if(!Long.valueOf(familyId).equals(h.familyId))throw new BusinessException(ErrorCode.FORBIDDEN,"订单不属于当前家庭");return h;}
  private static FamilyWalletAccount domain(FamilyWalletAccountDO r){return new FamilyWalletAccount(r.familyId,r.availableAmount,r.frozenAmount);}
  private void persist(FamilyWalletAccount a){if(mapper.updateAccount(a.familyId(),a.availableAmount(),a.frozenAmount())!=1)conflict("家庭钱包并发更新失败");}
  private void ledger(long familyId,Long orderId,long actor,String type,String key,BigDecimal amount,BigDecimal beforeA,BigDecimal beforeF,FamilyWalletAccount after,String remark){
    if(key==null||key.isBlank())throw new BusinessException(ErrorCode.BAD_REQUEST,"业务幂等键不能为空");FamilyWalletLedgerDO l=new FamilyWalletLedgerDO();l.familyId=familyId;l.orderId=orderId;l.operatorUserId=actor;l.scopeKey="family:"+familyId;l.businessType=type;l.businessKey=key.trim();l.amount=FamilyWalletAccount.command(amount);l.availableBefore=beforeA;l.availableAfter=after.availableAmount();l.frozenBefore=beforeF;l.frozenAfter=after.frozenAmount();l.remark=remark;mapper.insertLedger(l);
  }
  private static void requireAtLeast(BigDecimal actual,BigDecimal wanted,String message){if(actual.compareTo(wanted)<0)throw new BusinessException(ErrorCode.BUSINESS_INVALID,message);}
  private static void conflict(String message){throw new BusinessException(ErrorCode.STATE_CONFLICT,message);}
}
