package com.familykitchen.testsupport;

import java.util.Arrays;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;

/** Validates the final merged datasource properties before any application bean is created. */
public final class TestDatabaseFinalPropertyGuard
    implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

  private Environment environment;

  /** {@inheritDoc} */
  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  /** {@inheritDoc} */
  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
      throws BeansException {
    String profile = Arrays.stream(environment.getActiveProfiles())
        .filter(value -> value.startsWith("test-"))
        .findFirst()
        .orElse("");
    String url = environment.getProperty("spring.datasource.url", "");
    if ("test-container".equals(profile)) {
      String username = environment.getProperty("spring.datasource.username", "");
      String password = environment.getProperty("spring.datasource.password", "");
      TestDatabaseOwnership.Registration ownership =
          TestDatabaseOwnership.requireMatching(url, username, password);
      TestDatabaseUrlGuard.requireOwnedContainer(
          ownership, url, username, password, profile);
      return;
    }
    TestDatabaseEnvironmentPostProcessor.validate(profile, url,
        environment.getProperty("spring.autoconfigure.exclude", ""));
  }

  /** {@inheritDoc} */
  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
