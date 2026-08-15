package com.familykitchen.auth.service;

import com.familykitchen.auth.model.dto.RegisterRequest;
import com.familykitchen.auth.model.dto.UserLoginRequest;
import com.familykitchen.auth.model.dto.WechatLoginRequest;
import com.familykitchen.auth.model.vo.LoginResponse;

/**
 * 用户认证服务。
 */
public interface MemberAuthService {

  /**
   * 用户注册。
   
   * @param request 请求参数
   * @return 注册结果后的结果
   */
  LoginResponse register(RegisterRequest request);

  /**
   * 用户登录。
   
   * @param request 请求参数
   * @return 登录结果后的结果
   */
  LoginResponse login(UserLoginRequest request);

  /** 使用微信官方临时登录码登录已绑定账号。 
   * @param request 请求参数
   * @return 处理Login后的结果
   */
  LoginResponse wechatLogin(WechatLoginRequest request);
}
