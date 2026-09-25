package com.familykitchen.feedback;

import com.familykitchen.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises MVC exception resolution without a server or database. */
class FeedbackUploadLimitTest {
  @RestController
  static class OversizedUpload {
    @GetMapping("/oversized-upload")
    public void upload() { throw new MaxUploadSizeExceededException(4 * 1024 * 1024); }
  }
  @Test void parserSizeLimitReturnsReadableClientError() throws Exception {
    MockMvcBuilders.standaloneSetup(new OversizedUpload()).setControllerAdvice(new GlobalExceptionHandler()).build()
        .perform(get("/oversized-upload")).andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("message").value(org.hamcrest.Matchers.containsString("4 MiB")));
  }
}
