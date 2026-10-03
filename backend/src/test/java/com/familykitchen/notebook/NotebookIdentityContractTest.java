package com.familykitchen.notebook;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Contact lookup must not advertise unverified, non-unique mobile numbers. */
class NotebookIdentityContractTest {
  @Test void lookupAndNotebookContractSupportOnlyUsernameOrVerifiedEmail() throws Exception {
    String mapper;
    try (var stream = getClass().getResourceAsStream("/mapper/user/UserMapper.xml")) {
      mapper = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
    String query = mapper.substring(mapper.indexOf("<select id=\"findByLoginIdentifier\""));
    query = query.substring(0, query.indexOf("</select>"));
    assertThat(query).contains("username=#{identifier}", "email_verified=1").doesNotContain("mobile=");
    Path documentation = Path.of("docs", "api-spec.md");
    if (!Files.exists(documentation)) documentation = Path.of("..", "docs", "api-spec.md");
    String specification = Files.readString(documentation);
    String contacts = specification.substring(specification.indexOf("### 记事联系人与共享"));
    assertThat(contacts).contains("用户名", "已验证邮箱").doesNotContain("手机号");
  }
}
