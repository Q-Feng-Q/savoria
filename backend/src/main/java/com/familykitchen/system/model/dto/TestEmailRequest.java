package com.familykitchen.system.model.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
/**
 * 平台 SMTP 配置测试请求。
 * @param recipient 接收测试邮件的有效邮箱地址
 */
public record TestEmailRequest(@NotBlank @Email String recipient) {}
