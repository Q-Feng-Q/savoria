package com.familykitchen.feedback;

import java.time.LocalDateTime;

/** Internal persistence record; never serialized directly to callers. */
public class FeedbackRow {
  Long id, ownerUserId, handledBy;
  String requestId, type, content, status, reply, ownerName;
  int version;
  LocalDateTime createdAt, updatedAt;
}
