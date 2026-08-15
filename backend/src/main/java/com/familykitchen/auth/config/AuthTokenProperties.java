package com.familykitchen.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 访问令牌配置属性。 *
 * <p>用于定义 HMAC Token 服务所需的签名密钥和过期时间。 
 * @param secret secret
 * @param expiresInSeconds expiresInSeconds
 */
@ConfigurationProperties(prefix = "family-kitchen.jwt")
public record AuthTokenProperties(
    String secret,
    long expiresInSeconds
) {
}

