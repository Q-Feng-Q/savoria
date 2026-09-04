package com.familykitchen.dish.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 模板图片私有审核区和公共发布区配置。 */
@Component
@ConfigurationProperties(prefix = "family-kitchen.dish-template-assets")
public class DishTemplateAssetProperties {
  /** 不对外映射的来源图片审核根目录。 */
  private Path privateRoot = Path.of("./data/dish-template-assets/private");
  /** 审核通过后图片写入的公共静态资源根目录。 */
  private Path publicRoot = Path.of("./uploads/dish-template-assets");
  /** 公共静态资源对外访问路径前缀。 */
  private String publicUrlPrefix = "/uploads/dish-template-assets";

  /**
   * 获取私有审核根目录。
   * @return 私有审核根目录
   */
  public Path getPrivateRoot() { return privateRoot; }
  /**
   * 设置私有审核根目录。
   * @param privateRoot 私有审核根目录
   */
  public void setPrivateRoot(Path privateRoot) { this.privateRoot = privateRoot; }
  /**
   * 获取公共发布根目录。
   * @return 公共发布根目录
   */
  public Path getPublicRoot() { return publicRoot; }
  /**
   * 设置公共发布根目录。
   * @param publicRoot 公共发布根目录
   */
  public void setPublicRoot(Path publicRoot) { this.publicRoot = publicRoot; }
  /**
   * 获取公共访问路径前缀。
   * @return 公共访问路径前缀
   */
  public String getPublicUrlPrefix() { return publicUrlPrefix; }
  /**
   * 设置公共访问路径前缀。
   * @param publicUrlPrefix 公共访问路径前缀
   */
  public void setPublicUrlPrefix(String publicUrlPrefix) { this.publicUrlPrefix = publicUrlPrefix; }
}
