package com.familykitchen.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

/**
 * 登录成功后的认证返回对象。
 
 * @param accessToken access令牌
 * @param userId 用户标识
 * @param merchantId 商户标识
 * @param familyId 家庭标识
 * @param memberId 成员标识
 * @param roleTemplate 角色Template
 * @param backendRoles backendRoles
 * @param merchantAdminScopes 商户平台管理Scopes
 */
@Schema(description = "登录成功后的认证返回对象")
public record LoginResponse(
    @Schema(description = "Bearer 访问令牌")
    String accessToken,
    @Schema(description = "当前用户 ID")
    Long userId,
    @Schema(description = "所属商户 ID")
    Long merchantId,
    @Schema(description = "所属家庭 ID")
    Long familyId,
    @Schema(description = "所属成员 ID")
    Long memberId,
    @Schema(description = "角色模板")
    String roleTemplate,
    @Schema(description = "后台角色列表")
    Set<String> backendRoles,
    @Schema(description = "商户后台权限范围")
    Set<String> merchantAdminScopes
) {
}
