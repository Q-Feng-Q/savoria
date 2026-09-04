package com.familykitchen.dish.service;

import com.familykitchen.dish.model.entity.DishTemplateImageAssetEntity;
import java.nio.file.Path;
import org.springframework.core.io.Resource;

/** 在配置根目录内安全读取和发布模板图片。 */
public interface DishTemplateImageStorageService {
  /**
   * 解析内部审核文件，拒绝目录穿越和缺失文件。
   * @param asset 已从数据库读取的内部图片资产
   * @return 可安全流式输出的受控资源
   */
  PreviewResource preview(DishTemplateImageAssetEntity asset);
  /**
   * 按内容摘要生成公共文件名并以原子移动发布。
   * @param asset 已锁定的待审核图片资产
   * @return 公共地址和文件创建状态
   */
  PublishedResource publish(DishTemplateImageAssetEntity asset);
  /**
   * 数据库事务回滚时清理本次新建且尚未被引用的公共文件。
   * @param resource 本次发布产生的公共资源
   */
  void deleteNewFile(PublishedResource resource);

  /**
   * 受控预览资源。
   * @param resource Spring可读取资源
   * @param mimeType 图片媒体类型
   * @param contentLength 文件字节数
   */
  record PreviewResource(Resource resource, String mimeType, long contentLength) { }
  /**
   * 公共发布结果。
   * @param publicUrl 公共访问地址
   * @param path 公共目录内文件路径
   * @param newlyCreated 本次事务是否新建文件
   */
  record PublishedResource(String publicUrl, Path path, boolean newlyCreated) { }
}
