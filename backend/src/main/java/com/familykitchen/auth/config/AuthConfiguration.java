package com.familykitchen.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 认证相关配置入口。 */
@Configuration
@EnableConfigurationProperties({AuthBootstrapProperties.class, AuthTokenProperties.class})
public class AuthConfiguration {
}

