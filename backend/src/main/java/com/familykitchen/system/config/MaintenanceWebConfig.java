package com.familykitchen.system.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
/** 注册维护模式拦截器及必要白名单。 */
@Configuration
public class MaintenanceWebConfig implements WebMvcConfigurer {
  private final MaintenanceInterceptor interceptor;
  /**
   * 创建维护模式 MVC 配置。
   * @param interceptor 维护模式拦截器
   */
  public MaintenanceWebConfig(MaintenanceInterceptor interceptor){this.interceptor=interceptor;}
  /**
   * 注册全站维护拦截器，并保留配置管理、登录、健康检查与静态文件入口。
   * <p>这些白名单保证维护期间仍可登录后台关闭维护模式，并允许基础存活探测。</p>
   * @param registry MVC 拦截器注册表
   */
  @Override public void addInterceptors(InterceptorRegistry registry){
    registry.addInterceptor(interceptor).addPathPatterns("/**").excludePathPatterns(
        "/public/system-settings","/auth/admin/login","/admin/system-settings/**",
        "/uploads/**","/actuator/health");
  }
}
