package com.familykitchen.system.config;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.system.model.vo.SystemSettingView;
import com.familykitchen.system.service.SystemSettingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 在 MVC 入口执行全站维护状态检查的请求闸门。
 *
 * <p>维护开启时阻止进入业务控制器，并以统一 JSON 结构返回 HTTP 503；
 * 白名单路径由 Web 配置决定，不在此组件内重复维护。</p>
 */
@Component
public class MaintenanceInterceptor implements HandlerInterceptor {
  private final SystemSettingService service;
  private final ObjectMapper objectMapper;

  /**
   * 创建维护模式拦截器。
   * @param service 系统配置服务
   * @param objectMapper JSON 序列化器
   */
  public MaintenanceInterceptor(SystemSettingService service, ObjectMapper objectMapper) {
    this.service = service;
    this.objectMapper = objectMapper;
  }

  /**
   * 在维护模式下终止非白名单请求，并输出统一的 503 业务响应。
   * @param request 当前 HTTP 请求
   * @param response 当前 HTTP 响应
   * @param handler 即将执行的处理器
   * @return {@code true} 表示继续处理，{@code false} 表示已返回维护响应
   * @throws Exception 写入响应失败时向 MVC 调用链传播
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    SystemSettingView setting = service.current();
    if (!setting.maintenanceEnabled()) return true;
    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), ApiResponse.error(ErrorCode.SYSTEM_MAINTENANCE.code(),
      setting.maintenanceMessage()));
    return false;
  }
}
