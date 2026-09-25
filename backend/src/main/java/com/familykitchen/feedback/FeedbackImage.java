package com.familykitchen.feedback;

import java.time.LocalDateTime;

/** Private attachment metadata, including a server-generated object key. */
public class FeedbackImage {
  String id, objectKey, mime;
  Long ownerUserId, feedbackId;
  int width, height, byteSize, sortOrder;
  LocalDateTime createdAt;
}
