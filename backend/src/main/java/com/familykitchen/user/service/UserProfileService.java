package com.familykitchen.user.service;

import com.familykitchen.user.model.dto.BindEmailRequest;
import com.familykitchen.user.model.dto.BindWechatRequest;
import com.familykitchen.user.model.dto.ChangePasswordRequest;
import com.familykitchen.user.model.dto.UpdateProfileRequest;
import com.familykitchen.user.model.vo.UserProfileView;
import com.familykitchen.user.model.dto.ChangeUsernameRequest;
import com.familykitchen.user.model.vo.UserContextView;

/**
 * 用户公共账号能力服务。
 */
public interface UserProfileService {
  /**
   * 处理用户资料。
   *
   * @param userId 用户标识
   * @return 处理结果
   */
  UserProfileView profile(Long userId);

  /**
   * 更新资料。
   *
   * @param userId 用户标识
   * @param request 请求参数
   * @return 更新资料后的结果
   */
  UserProfileView updateProfile(Long userId, UpdateProfileRequest request);

  /**
   * 修改密码。
   *
   * @param userId 用户标识
   * @param request 请求参数
   */
  void changePassword(Long userId, ChangePasswordRequest request);

  /**
   * 发送邮箱编码。
   *
   * @param userId 用户标识
   * @param email 邮箱
   * @param requestIp 请求参数Ip
   */
  void sendEmailCode(Long userId, String email, String requestIp);

  /**
   * 绑定邮箱。
   *
   * @param userId 用户标识
   * @param request 请求参数
   */
  void bindEmail(Long userId, BindEmailRequest request);

  /**
   * 绑定微信。
   *
   * @param userId 用户标识
   * @param request 请求参数
   */
  void bindWechat(Long userId, BindWechatRequest request);

  /**
   * 解绑微信。
   *
   * @param userId 用户标识
   */
  void unbindWechat(Long userId);

  /**
   * 修改用户名。
   *
   * @param userId 用户标识
   * @param request 请求参数
   * @return 修改用户名后的结果
   */
  UserProfileView changeUsername(Long userId, ChangeUsernameRequest request);

  /**
   * 处理用户资料。
   *
   * @param userId 用户标识
   * @return 处理结果
   */
  UserContextView context(Long userId);
}
