package com.familykitchen.common.idempotency;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes one command and persists its replay result in the same database transaction. */
@Service
public class CommandIdempotencyService {
  /** Normalized command scope.
   * @param actorId actor identifier
   * @param familyId family identifier
   * @param operation operation
   * @param requestId client request identifier
   * @param payload canonical payload
   */
  public record Command(long actorId,long familyId,String operation,String requestId,String payload) {}
  /** Persisted replay result.
   * @param resourceType resource type
   * @param resourceId resource identifier
   * @param body serialized body
   */
  public record Result(String resourceType,Long resourceId,String body) {}
  private final CommandIdempotencyMapper mapper;
  /** Creates the service.
   * @param mapper persistence mapper
   */
  public CommandIdempotencyService(CommandIdempotencyMapper mapper){this.mapper=mapper;}

  /** Executes or replays one command in its caller transaction.
   * @param command command scope
   * @param action business action
   * @return new or replayed result
   */
  @Transactional
  public Result execute(Command command,Supplier<Result> action){
    Command normalized=normalize(command);String hash=sha256(normalized.payload());
    int inserted=mapper.insertClaim(normalized.actorId(),normalized.familyId(),normalized.operation(),normalized.requestId(),hash);
    Map<String,Object> row=requireClaim(normalized);long id=((Number)row.get("id")).longValue();
    if(!hash.equals(text(row.get("payloadHash"))))throw new BusinessException(ErrorCode.STATE_CONFLICT,"相同请求号的请求内容不一致");
    if(inserted==0){
      if(!"COMPLETED".equals(text(row.get("state"))))throw new BusinessException(ErrorCode.STATE_CONFLICT,"请求仍在处理中");
      return new Result(nullableText(row.get("resourceType")),number(row.get("resourceId")),nullableText(row.get("resultBody")));
    }
    Result result=action.get();if(result==null)throw new BusinessException(ErrorCode.SYSTEM_ERROR,"幂等命令未返回结果");
    if(mapper.complete(id,blankToNull(result.resourceType()),result.resourceId(),blankToNull(result.body()))!=1)
      throw new BusinessException(ErrorCode.STATE_CONFLICT,"幂等命令状态已变化");
    return result;
  }

  private Map<String,Object> requireClaim(Command c){Map<String,Object> row=mapper.lockClaim(c.actorId(),c.familyId(),c.operation(),c.requestId());if(row==null)throw new BusinessException(ErrorCode.SYSTEM_ERROR,"幂等命令记录缺失");return row;}
  private static Command normalize(Command c){if(c==null||c.actorId()<=0||c.familyId()<=0)bad("幂等命令范围不合法");return new Command(c.actorId(),c.familyId(),required(c.operation(),80,"操作名称"),required(c.requestId(),128,"请求号"),c.payload()==null?"":c.payload());}
  private static String required(String value,int max,String name){if(value==null||value.isBlank()||value.trim().length()>max)bad(name+"不合法");return value.trim();}
  private static String sha256(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
  private static String text(Object v){return v==null?"":v.toString();} private static String nullableText(Object v){return v==null?null:v.toString();}
  private static Long number(Object v){return v instanceof Number n?n.longValue():null;} private static String blankToNull(String v){return v==null||v.isBlank()?null:v;}
  private static void bad(String message){throw new BusinessException(ErrorCode.BAD_REQUEST,message);}
}
