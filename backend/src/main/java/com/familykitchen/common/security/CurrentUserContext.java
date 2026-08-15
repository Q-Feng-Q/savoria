package com.familykitchen.common.security;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
/**
 * 当前请求对应的实时用户与权限上下文，权限集合来自数据库而非令牌快照。
 * @param userId 当前账号标识
 * @param merchantId 当前账号关联的商户标识，未关联时为空
 * @param familyId 当前家庭标识，未加入家庭时为空
 * @param memberId 当前家庭成员标识；现阶段与账号标识一致
 * @param roleTemplate 当前用户在家庭中的角色模板
 * @param backendRoles 平台或商户后台角色集合
 * @param merchantAdminScopes 当前商户下的管理权限范围
 * @param sessionId 当前登录会话标识
 */
@Schema(description="当前登录用户上下文")
public record CurrentUserContext(Long userId,Long merchantId,Long familyId,Long memberId,
    String roleTemplate,Set<String> backendRoles,Set<String> merchantAdminScopes,String sessionId) {
  /**
   * 创建不携带会话标识的兼容权限上下文。
   * @param userId 当前账号标识
   * @param merchantId 关联商户标识
   * @param familyId 当前家庭标识
   * @param memberId 当前家庭成员标识
   * @param roleTemplate 家庭角色模板
   * @param backendRoles 后台角色集合
   * @param merchantAdminScopes 商户管理权限范围
   */
  public CurrentUserContext(Long userId,Long merchantId,Long familyId,Long memberId,String roleTemplate,
      Set<String> backendRoles,Set<String> merchantAdminScopes){
    this(userId,merchantId,familyId,memberId,roleTemplate,backendRoles,merchantAdminScopes,null);
  }
  /**
   * 判断用户是否具备当前商户的后台访问权限。
   * @return 是否具备当前商户的后台访问权限
   */
  public boolean hasMerchantBackendAccess(){return backendRoles.contains("MERCHANT_ADMIN")||
      backendRoles.contains("merchant_admin")||merchantAdminScopes.contains("MERCHANT_ADMIN")||
      merchantAdminScopes.contains("merchant");}
  /**
   * 判断用户是否具备平台后台访问权限。
   * @return 是否具备平台后台访问权限
   */
  public boolean hasPlatformBackendAccess(){return backendRoles.contains("PLATFORM_ADMIN")||
      backendRoles.contains("platform_admin");}
  /**
   * 判断用户是否为当前家庭的所有者或管理员。
   * @return 是否已在家庭中且具备家庭所有者或管理员权限
   */
  public boolean hasFamilyAdminAccess(){return ("owner".equalsIgnoreCase(roleTemplate)||
      "admin".equalsIgnoreCase(roleTemplate))&&familyId!=null;}
}
