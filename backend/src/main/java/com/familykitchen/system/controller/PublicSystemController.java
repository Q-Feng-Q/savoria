package com.familykitchen.system.controller;
import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.system.model.vo.PublicSystemSettingView;
import com.familykitchen.system.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/** 无需登录的站点配置接口。 */
@RestController @RequestMapping("/public/system-settings")
@Tag(name="公共-系统配置",description="网站名称、Logo和维护状态")
public class PublicSystemController {
  private final SystemSettingService service;
  /**
   * 创建公开系统配置控制器。
   * @param service 系统配置服务
   */
  public PublicSystemController(SystemSettingService service){this.service=service;}
  /**
   * 查询无需登录即可展示的站点与功能开关配置。
   * @return 不含 SMTP 凭据的公开系统配置
   */
  @GetMapping @Operation(summary="查询公开系统配置")
  public ApiResponse<PublicSystemSettingView> current(){return ApiResponse.ok(service.publicCurrent());}
}
