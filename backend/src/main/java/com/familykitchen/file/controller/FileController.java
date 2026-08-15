package com.familykitchen.file.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.file.model.vo.FileUploadResponse;
import com.familykitchen.file.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器。
 *
 * <p>负责接收文件上传请求、校验当前登录人是否具备后台上传权限，
 * 具体的文件校验与存储逻辑下沉到应用服务中处理。</p>
 */
@RestController
@RequestMapping("/files")
@Tag(name = "公共-文件上传", description = "后台图片上传接口")
public class FileController {

  private final CurrentUserProvider currentUserProvider;
  private final FileUploadService fileUploadService;

  /**
   * 创建文件上传控制器。
   * @param currentUserProvider 当前请求身份提供器
   * @param fileUploadService 文件上传应用服务
   */
  public FileController(CurrentUserProvider currentUserProvider, FileUploadService fileUploadService) {
    this.currentUserProvider = currentUserProvider;
    this.fileUploadService = fileUploadService;
  }

  /**
   * 校验后台访问权限后上传图片，并将资源归属到当前账号。
   * @param request 当前 HTTP 请求
   * @param file 待上传的图片文件
   * @return 文件访问地址及图片元数据
   */
  @PostMapping(path = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "上传图片", description = "支持 jpeg、png、webp 图片上传，单文件最大 4MB。")
  public ApiResponse<FileUploadResponse> uploadImage(
      HttpServletRequest request,
      @RequestPart("file") MultipartFile file
  ) {
    CurrentUserContext user = currentUserProvider.require(request);
    if (!user.hasMerchantBackendAccess() && !user.hasPlatformBackendAccess()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无文件上传权限");
    }
    return ApiResponse.ok(fileUploadService.uploadImage(user.userId(), file));
  }
}
