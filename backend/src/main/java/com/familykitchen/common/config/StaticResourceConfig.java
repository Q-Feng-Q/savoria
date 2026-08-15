package com.familykitchen.common.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 本地上传文件的静态资源映射配置，将公开 URL 限定到配置的上传根目录。 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
  private final String localRoot;

  /**
   * 创建静态资源配置。
   * @param localRoot 本地上传文件的根目录
   */
  public StaticResourceConfig(@Value("${family-kitchen.file-storage.local-root:./uploads}") String localRoot) {
    this.localRoot = localRoot;
  }

  /**
   * 将 {@code /uploads/**} 请求映射到规范化后的本地上传目录。
   * @param registry Spring MVC 资源处理器注册表
   */
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String uploadLocation = Path.of(localRoot).toAbsolutePath().normalize().toUri().toString();
    registry.addResourceHandler("/uploads/**")
        .addResourceLocations(uploadLocation.endsWith("/") ? uploadLocation : uploadLocation + "/");
  }
}

