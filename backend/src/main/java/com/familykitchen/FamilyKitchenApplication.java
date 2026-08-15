package com.familykitchen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 家庭厨房后端的 Spring Boot 启动入口，负责启用组件扫描、自动配置与定时任务。 */
@SpringBootApplication
@EnableScheduling
public class FamilyKitchenApplication {

  /**
   * 启动家庭厨房后端应用。
   * @param args 命令行启动参数
   */
  public static void main(String[] args) {
    SpringApplication.run(FamilyKitchenApplication.class, args);
  }
}

