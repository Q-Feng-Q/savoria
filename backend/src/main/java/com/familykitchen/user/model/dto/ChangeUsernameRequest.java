package com.familykitchen.user.model.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
/** 用户名唯一一次修改请求。 
 * @param username 用户名
 */
public record ChangeUsernameRequest(@NotBlank @Size(min=3,max=50)
  @Pattern(regexp="^[A-Za-z0-9_]+$") String username) {}
