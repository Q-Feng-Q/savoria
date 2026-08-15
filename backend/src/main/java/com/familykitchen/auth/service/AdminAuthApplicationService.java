package com.familykitchen.auth.service;

import com.familykitchen.auth.model.dto.AdminLoginRequest;
import com.familykitchen.auth.model.vo.LoginResponse;

/**
 * 管理端认证服务。
 *
 * <p>用于后台账号登录，负责校验账号状态和密码，并生成后端接口访问令牌。</p>
 */
public interface AdminAuthApplicationService {

  /**
   * 使用后台账号密码登录。
   *
   * @param request 登录用户名和密码
   * @return 登录成功后的访问令牌、用户身份和后台权限范围
   */
  LoginResponse login(AdminLoginRequest request);
}
