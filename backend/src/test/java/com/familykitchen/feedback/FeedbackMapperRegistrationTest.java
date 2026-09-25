package com.familykitchen.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.familykitchen.common.config.MybatisConfiguration;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Uses production mapper scanning and XML loading without connecting to a database. */
class FeedbackMapperRegistrationTest {
  @Test
  void productionScannerRegistersFeedbackMapperAndConstructsService() {
    DataSource dataSource = mock(DataSource.class);
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MybatisAutoConfiguration.class))
        .withUserConfiguration(MybatisConfiguration.class, FeedbackService.class)
        .withBean(DataSource.class, () -> dataSource)
        .withPropertyValues("mybatis.mapper-locations=classpath*:mapper/**/*.xml")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(FeedbackMapper.class);
          assertThat(context).hasSingleBean(FeedbackService.class);
          var configuration = context.getBean(SqlSessionFactory.class).getConfiguration();
          assertThat(configuration.hasMapper(FeedbackMapper.class)).isTrue();
          for (var method : FeedbackMapper.class.getDeclaredMethods()) {
            assertThat(configuration.hasStatement(FeedbackMapper.class.getName() + "." + method.getName()))
                .as("XML statement for %s", method.getName()).isTrue();
          }
          verifyNoInteractions(dataSource);
        });
  }
}
