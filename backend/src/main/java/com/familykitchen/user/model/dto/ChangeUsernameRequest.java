package com.familykitchen.user.model.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
/** 用户名唯一一次修改请求。 
 * @param username 用户名
 */
public record ChangeUsernameRequest(@NotBlank @Size(min=2,max=50,message="长度需为 {min}-{max} 个字符")
  @Pattern(regexp="^[A-Za-z0-9_]+$",message="只能使用英文字母、数字或下划线") String username) {}
