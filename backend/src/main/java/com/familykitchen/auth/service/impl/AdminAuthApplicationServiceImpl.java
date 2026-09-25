package com.familykitchen.auth.service.impl;

import com.familykitchen.auth.model.dto.AdminLoginRequest;
import com.familykitchen.auth.model.dto.UserLoginRequest;
import com.familykitchen.auth.model.vo.LoginResponse;
import com.familykitchen.auth.service.AdminAuthApplicationService;
import com.familykitchen.auth.service.MemberAuthService;
import org.springframework.stereotype.Service;

/** PC 后台与小程序共用密码验证和实时身份解析。 */
@Service
public class AdminAuthApplicationServiceImpl implements AdminAuthApplicationService {
  private final MemberAuthService memberAuthService;

  public AdminAuthApplicationServiceImpl(MemberAuthService memberAuthService) {
    this.memberAuthService = memberAuthService;
  }

  @Override
  public LoginResponse login(AdminLoginRequest request) {
    return memberAuthService.loginAdmin(new UserLoginRequest(request.username(), request.password()));
  }
}
