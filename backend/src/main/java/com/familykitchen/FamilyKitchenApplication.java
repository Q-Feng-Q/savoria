package com.familykitchen;

import java.util.Locale;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.WebApplicationType;

/** 家庭厨房后端的 Spring Boot 启动入口，负责启用组件扫描、自动配置与定时任务。 */
@SpringBootApplication
public class FamilyKitchenApplication {

  /**
   * 启动家庭厨房后端应用。
   * @param args 命令行启动参数
   */
  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(FamilyKitchenApplication.class);
    if (isMigrationCommand(args)) {
      application.setWebApplicationType(WebApplicationType.NONE);
    }
    application.run(args);
  }

  static boolean isMigrationCommand(String[] args) {
    String mode = System.getProperty("family-kitchen.migration.mode");
    if (mode == null || mode.isBlank()) mode = System.getenv("FAMILY_KITCHEN_MIGRATION_MODE");
    for (String argument : args == null ? new String[0] : args) {
      String prefix = "--family-kitchen.migration.mode=";
      if (argument != null && argument.startsWith(prefix)) mode = argument.substring(prefix.length());
    }
    return mode != null && !mode.isBlank()
        && !"OFF".equals(mode.trim().toUpperCase(Locale.ROOT));
  }
}

