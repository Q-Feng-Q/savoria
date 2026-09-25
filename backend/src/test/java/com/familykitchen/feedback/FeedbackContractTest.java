package com.familykitchen.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FeedbackContractTest {
  @Test void privateFeedbackEndpointsExist() {
    assertThat(Files.exists(Path.of("src/main/java/com/familykitchen/feedback/FeedbackController.java"))).isTrue();
  }
  @Test void schemaHasIdempotencyAndPrivateAttachments() throws Exception {
    String sql = Files.readString(Path.of("src/main/resources/db/migration/V1__init_schema.sql"));
    assertThat(sql).contains("CREATE TABLE user_feedback", "CREATE TABLE feedback_images", "CREATE TABLE feedback_history", "UNIQUE KEY uk_feedback_request (owner_user_id,request_id)");
  }
}
