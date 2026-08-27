package com.familykitchen.common.idempotency;

import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persists transactional command claims and replay results. */
@Mapper
public interface CommandIdempotencyMapper {
  /** Claims a unique request.
   * @param actorId actor
   * @param familyId family
   * @param operation operation
   * @param requestId request identifier
   * @param payloadHash payload digest
   * @return one when claimed
   */
  int insertClaim(@Param("actorId") long actorId,@Param("familyId") long familyId,
      @Param("operation") String operation,@Param("requestId") String requestId,
      @Param("payloadHash") String payloadHash);
  /** Locks an existing claim.
   * @param actorId actor
   * @param familyId family
   * @param operation operation
   * @param requestId request identifier
   * @return claim row
   */
  Map<String,Object> lockClaim(@Param("actorId") long actorId,@Param("familyId") long familyId,
      @Param("operation") String operation,@Param("requestId") String requestId);
  /** Stores a completed result.
   * @param id claim identifier
   * @param resourceType resource type
   * @param resourceId resource identifier
   * @param resultBody serialized result
   * @return affected rows
   */
  int complete(@Param("id") long id,@Param("resourceType") String resourceType,
      @Param("resourceId") Long resourceId,@Param("resultBody") String resultBody);
}
