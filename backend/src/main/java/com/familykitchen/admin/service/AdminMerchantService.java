package com.familykitchen.admin.service;

import com.familykitchen.admin.mapper.AdminMerchantMapper;
import com.familykitchen.admin.model.dto.AdminMerchantRequest;
import com.familykitchen.admin.model.entity.AdminMerchantDO;
import com.familykitchen.admin.model.vo.AdminMerchantView;
import com.familykitchen.common.error.*;
import com.familykitchen.family.mapper.MerchantInvitationMapper;
import com.familykitchen.merchant.service.MerchantDefaultDataInitializer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 编排平台管理员对商户、负责人关系和私密邀请码的维护操作。 */
@Service
public class AdminMerchantService {
  private final AdminMerchantMapper mapper;
  private final MerchantInvitationMapper codes;
  private final MerchantDefaultDataInitializer defaults;

  /**
   * 创建商户管理服务。
   *
   * @param m 商户管理数据访问接口
   * @param c 商户邀请码数据访问接口
   * @param defaults 商户默认目录初始化器
   */
  public AdminMerchantService(AdminMerchantMapper m, MerchantInvitationMapper c,
      MerchantDefaultDataInitializer defaults) {
    mapper=m;
    codes=c;
    this.defaults=defaults;
  }

  /**
   * 按可选关键字查询商户。
   *
   * @param q 名称关键字
   * @return 商户列表
   */
  public List<AdminMerchantView> list(String q) { return mapper.list(q==null?null:q.trim()); }

  /**
   * 创建商户并建立负责人关系。
   *
   * @param r 商户资料
   * @return 新商户标识
   */
  @Transactional
  public Long create(AdminMerchantRequest r) { requireOwner(r.ownerUserId()); AdminMerchantDO e=new AdminMerchantDO(); e.setName(r.name().trim()); e.setStatus(r.status()==null?"active":r.status()); e.setContactName(r.contactName()); e.setContactPhone(r.contactPhone()); mapper.insert(e); defaults.initialize(e.getId()); mapper.insertOwner(e.getId(),r.ownerUserId()); return e.getId(); }

  /**
   * 更新商户资料并替换负责人关系。
   *
   * @param id 商户标识
   * @param r 商户资料
   */
  @Transactional
  public void update(Long id, AdminMerchantRequest r) { requireOwner(r.ownerUserId()); if(mapper.update(id,r.name().trim(),r.status(),r.contactName(),r.contactPhone())==0) throw new BusinessException(ErrorCode.NOT_FOUND,"商户不存在"); mapper.disableOwners(id); mapper.insertOwner(id,r.ownerUserId()); }

  /**
   * 逻辑删除商户并停用其负责人和邀请码。
   *
   * @param id 商户标识
   */
  @Transactional
  public void delete(Long id) { if(mapper.softDelete(id)==0) throw new BusinessException(ErrorCode.NOT_FOUND,"商户不存在或已停用"); mapper.disableOwners(id); codes.disableActive(id); }

  /**
   * 生成新邀请码并仅持久化其摘要。
   *
   * @param id 商户标识
   * @param operator 操作人标识
   * @return 仅本次返回的明文邀请码
   */
  @Transactional
  public String rotateCode(Long id, Long operator) { if(mapper.countActiveMerchant(id)==0) throw new BusinessException(ErrorCode.NOT_FOUND,"商户不存在或已停用"); codes.disableActive(id); byte[] b=new byte[9]; new SecureRandom().nextBytes(b); String raw=HexFormat.of().formatHex(b).toUpperCase(); codes.insert(id,hash(raw),operator); return raw; }

  /**
   * 停用商户当前有效的邀请码。
   *
   * @param id 商户标识
   */
  @Transactional
  public void disableCode(Long id) { codes.disableActive(id); }

  private void requireOwner(Long id) { if(mapper.countActiveUser(id)==0) throw new BusinessException(ErrorCode.BAD_REQUEST,"负责人用户不存在"); }
  private static String hash(String v) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8))); } catch(Exception e) { throw new IllegalStateException(e); } }
}
