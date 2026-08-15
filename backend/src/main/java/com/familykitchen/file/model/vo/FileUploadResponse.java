package com.familykitchen.file.model.vo;

/**
 * 图片上传成功后的响应视图。
 * @param fileId 文件资产标识
 * @param url 文件的公开访问路径
 * @param width 图片宽度，无法解析时为空
 * @param height 图片高度，无法解析时为空
 * @param size 文件大小，单位为字节
 */
public record FileUploadResponse(
    Long fileId,
    String url,
    Integer width,
    Integer height,
    long size
) {
}

