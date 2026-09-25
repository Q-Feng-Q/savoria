package com.familykitchen.feedback;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** SQL boundary for account-serialized feedback and locked private attachments. */
@Mapper
public interface FeedbackMapper {
  /**
   * Lock the account row until transaction completion.
   * @param owner account ID
   * @return database result or affected row count
   */
  Long lockUser(long owner);
  /**
   * Find feedback with submitter display name.
   * @param id resource ID
   * @return database result or affected row count
   */
  FeedbackRow find(long id);
  /**
   * Lock a feedback row for moderation.
   * @param id resource ID
   * @return database result or affected row count
   */
  FeedbackRow lockFeedback(long id);
  /**
   * Find the existing idempotent submission.
   * @param owner account ID
   * @param requestId client idempotency key
   * @return database result or affected row count
   */
  FeedbackRow byRequest(@Param("owner") long owner,@Param("requestId") String requestId);
  /**
   * Count successful submissions in the rolling quota window.
   * @param owner account ID
   * @param since inclusive quota cutoff
   * @return database result or affected row count
   */
  int countRecentFeedback(@Param("owner") long owner,@Param("since") LocalDateTime since);
  /**
   * Count registered uploads in the rolling quota window.
   * @param owner account ID
   * @param since inclusive quota cutoff
   * @return database result or affected row count
   */
  int countRecentImages(@Param("owner") long owner,@Param("since") LocalDateTime since);
  /**
   * Insert a feedback record and populate its generated ID.
   * @param row feedback record
   * @return database result or affected row count
   */
  int insert(FeedbackRow row);
  /**
   * Update moderation fields if the stored version still matches.
   * @param row feedback record
   * @return database result or affected row count
   */
  int update(FeedbackRow row);
  /**
   * Return filtered feedback in reverse chronological order.
   * @param owner account ID
   * @param type optional feedback type
   * @param status optional feedback status
   * @param offset zero-based row offset
   * @param limit maximum page length
   * @return database result or affected row count
   */
  List<FeedbackRow> list(@Param("owner") Long owner,@Param("type") String type,@Param("status") String status,@Param("offset") int offset,@Param("limit") int limit);
  /**
   * Count feedback using the same filters as the list.
   * @param owner account ID
   * @param type optional feedback type
   * @param status optional feedback status
   * @return database result or affected row count
   */
  long count(@Param("owner") Long owner,@Param("type") String type,@Param("status") String status);
  /**
   * Return bound images in client-selected order.
   * @param feedbackId feedback ID
   * @return database result or affected row count
   */
  List<FeedbackImage> images(long feedbackId);
  /**
   * Find private image metadata by opaque ID.
   * @param id resource ID
   * @return database result or affected row count
   */
  FeedbackImage image(String id);
  /**
   * Find registration for an orphan-cleanup object key.
   * @param objectKey server-generated private filename
   * @return database result or affected row count
   */
  FeedbackImage imageByObject(String objectKey);
  /**
   * Lock an image to coordinate binding and cleanup.
   * @param id resource ID
   * @return database result or affected row count
   */
  FeedbackImage lockImage(String id);
  /**
   * Register a successfully stored image.
   * @param image attachment metadata
   * @return database result or affected row count
   */
  int insertImage(FeedbackImage image);
  /**
   * Bind an image only if it remains unbound.
   * @param id resource ID
   * @param feedbackId feedback ID
   * @param sortOrder attachment order
   * @return database result or affected row count
   */
  int bind(@Param("id") String id,@Param("feedbackId") long feedbackId,@Param("sortOrder") int sortOrder);
  /**
   * Find a bounded batch of expired unbound attachment IDs.
   * @param before expiry cutoff
   * @return database result or affected row count
   */
  List<String> expired(LocalDateTime before);
  /**
   * Delete an unbound image registration after removing its file.
   * @param id resource ID
   * @return database result or affected row count
   */
  int deleteImage(String id);
  /**
   * Append one moderation audit entry.
   * @param feedbackId feedback ID
   * @param adminId acting platform administrator
   * @param fromStatus previous status
   * @param toStatus new status
   * @param reply public plain-text reply
   * @param now audit timestamp
   * @return database result or affected row count
   */
  int history(@Param("feedbackId") long feedbackId,@Param("adminId") long adminId,@Param("fromStatus") String fromStatus,@Param("toStatus") String toStatus,@Param("reply") String reply,@Param("now") LocalDateTime now);
  /**
   * Return chronological moderation audit entries.
   * @param feedbackId feedback ID
   * @return database result or affected row count
   */
  List<Map<String,Object>> histories(long feedbackId);
}
