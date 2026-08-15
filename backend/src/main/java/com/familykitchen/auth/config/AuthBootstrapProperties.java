package com.familykitchen.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bootstrap configuration for the initial administrator account.
 *
 * <p>The application uses these values during startup to ensure that one administrator account
 * exists in a fresh environment.
 
 * @param username 用户名
 * @param displayName display名称
 * @param password 密码
 */
@ConfigurationProperties(prefix = "family-kitchen.auth.bootstrap-admin")
public record AuthBootstrapProperties(
    String username,
    String displayName,
    String password
) {
}

