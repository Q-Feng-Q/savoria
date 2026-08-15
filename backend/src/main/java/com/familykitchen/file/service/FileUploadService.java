package com.familykitchen.file.service;

import com.familykitchen.file.model.vo.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传应用服务。
 *
 * <p>负责处理文件类型校验、大小校验、落盘存储以及上传结果返回，
 * Controller 只负责请求接入和权限判断。</p>
 */
public interface FileUploadService {

  /**
   * 上传图片文件并返回上传结果。
   *
   * @param ownerUserId 文件归属的账号标识
   * @param file 前端上传的图片文件
   * @return 上传完成后的文件信息
   */
  FileUploadResponse uploadImage(Long ownerUserId, MultipartFile file);
}
