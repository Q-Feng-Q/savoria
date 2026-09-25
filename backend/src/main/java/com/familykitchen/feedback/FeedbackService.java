package com.familykitchen.feedback;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.common.security.CurrentUserContext;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Feedback submission and moderation, independent of family membership. */
@Service
public class FeedbackService {
  private final FeedbackMapper mapper;
  /**
   * Create the feedback application service.
   * @param mapper persistence boundary
   */
  public FeedbackService(FeedbackMapper mapper) { this.mapper=mapper; }
  /**
   * A single logical feedback submission.
   * @param requestId client retry key
   * @param type BUG or SUGGESTION
   * @param content plain-text description
   * @param imageIds ordered private attachment IDs
   */
  public record Create(String requestId,String type,String content,List<String> imageIds) {}
  /**
   * Version-checked moderation command.
   * @param status target status
   * @param reply public reply
   * @param version last observed version
   */
  public record Update(String status,String reply,Integer version) {}
  /**
   * Build an invalid-input failure.
   * @param message readable explanation
   * @return validation error
   */
  static BusinessException bad(String message) { return new BusinessException(ErrorCode.BAD_REQUEST,message); }
  /**
   * Build a recoverable conflict.
   * @param message readable explanation
   * @return conflict error
   */
  static BusinessException conflict(String message) { return new BusinessException(ErrorCode.STATE_CONFLICT,message); }
  static void admin(CurrentUserContext user) {
    if (!user.hasPlatformBackendAccess()) throw new BusinessException(ErrorCode.FORBIDDEN,"无平台管理员权限");
  }
  static void type(String type) { if (!Set.of("BUG","SUGGESTION").contains(type==null?"":type)) throw bad("反馈类型无效"); }
  static void status(String status) { if (!Set.of("OPEN","PROCESSING","RESOLVED","CLOSED").contains(status==null?"":status)) throw bad("反馈状态无效"); }
  @Transactional
  /**
   * Serialize the quota check, idempotency lookup and binding in one transaction.
   * @param user authenticated account
   * @param body submission fields
   * @return owner-visible detail
   */
  public Map<String,Object> create(CurrentUserContext user,Create body) {
    type(body.type());String content=body.content()==null?"":body.content().strip();
    if(content.isEmpty()||content.length()>2000)throw bad("描述需为 1～2000 字符");
    if(body.requestId()==null||!body.requestId().matches("[A-Za-z0-9_-]{1,80}"))throw bad("requestId 无效");
    List<String> ids=body.imageIds()==null?List.of():body.imageIds();
    if(ids.size()>6||ids.stream().anyMatch(Objects::isNull)||new HashSet<>(ids).size()!=ids.size())throw bad("图片最多 6 张且不能重复");
    if(mapper.lockUser(user.userId())==null)throw new BusinessException(ErrorCode.UNAUTHORIZED,"账号不可用");
    FeedbackRow old=mapper.byRequest(user.userId(),body.requestId());
    if(old!=null) {
      if(!old.type.equals(body.type())||!old.content.equals(content)||!mapper.images(old.id).stream().map(i->i.id).toList().equals(ids))throw conflict("requestId 已用于不同的反馈内容");
      return view(old,false);
    }
    LocalDateTime now=LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    if(mapper.countRecentFeedback(user.userId(),now.minusHours(1))>=10)throw conflict("每小时最多提交 10 条反馈，请稍后重试");
    for(String id:ids.stream().sorted().toList()) {
      FeedbackImage image=mapper.lockImage(id);
      if(image==null||!user.userId().equals(image.ownerUserId)||image.feedbackId!=null||!image.createdAt.isAfter(now.minusHours(24)))throw bad("图片不可用或已过期，请重新上传");
    }
    FeedbackRow row=new FeedbackRow();row.ownerUserId=user.userId();row.requestId=body.requestId();row.type=body.type();row.content=content;
    row.status="OPEN";row.reply="";row.createdAt=now;row.updatedAt=now;mapper.insert(row);
    for(int n=0;n<ids.size();n++) if(mapper.bind(ids.get(n),row.id,n)!=1)throw conflict("图片已被绑定，请刷新");
    return view(row,false);
  }
  /**
   * Read feedback after ownership or platform authorization.
   * @param user authenticated caller
   * @param id feedback ID
   * @param isAdmin whether platform detail is requested
   * @return safe detail projection
   */
  public Map<String,Object> detail(CurrentUserContext user,long id,boolean isAdmin) {
    if(isAdmin)admin(user);FeedbackRow row=mapper.find(id);
    if(row==null||(!isAdmin&&!user.userId().equals(row.ownerUserId)))throw new BusinessException(ErrorCode.NOT_FOUND,"反馈不存在");
    return view(row,isAdmin);
  }
  @Transactional(readOnly=true)
  /**
   * Read a filtered page and its total in one transaction.
   * @param user authenticated caller
   * @param isAdmin whether platform-wide data is requested
   * @param type optional type filter
   * @param status optional status filter
   * @param page one-based page
   * @param pageSize page length from one to one hundred
   * @return items and total
   */
  public Map<String,Object> list(CurrentUserContext user,boolean isAdmin,String type,String status,int page,int pageSize) {
    if(isAdmin)admin(user);if(type!=null&&!type.isBlank())type(type);else type=null;
    if(status!=null&&!status.isBlank())status(status);else status=null;
    if(page<1||pageSize<1||pageSize>100||page>1000000)throw bad("分页参数无效");
    Long owner=isAdmin?null:user.userId();
    return Map.of("items",mapper.list(owner,type,status,(page-1)*pageSize,pageSize).stream().map(row->view(row,false,isAdmin)).toList(),"total",mapper.count(owner,type,status),"page",page,"pageSize",pageSize);
  }
  @Transactional
  /**
   * Atomically moderate with a version check and audit history.
   * @param user platform administrator
   * @param id feedback ID
   * @param body moderation fields
   * @return updated platform detail
   */
  public Map<String,Object> update(CurrentUserContext user,long id,Update body) {
    admin(user);status(body.status());String reply=body.reply()==null?"":body.reply().strip();
    if(reply.length()>2000||((body.status().equals("RESOLVED")||body.status().equals("CLOSED"))&&reply.isEmpty()))throw bad("已解决或已关闭需填写回复，最多 2000 字符");
    FeedbackRow row=mapper.lockFeedback(id);if(row==null)throw new BusinessException(ErrorCode.NOT_FOUND,"反馈不存在");
    if(body.version()==null||row.version!=body.version())throw conflict("反馈已被其他管理员修改，请刷新");
    String previous=row.status;row.status=body.status();row.reply=reply;row.handledBy=user.userId();row.updatedAt=LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    if(mapper.update(row)!=1)throw conflict("反馈已更新，请刷新");row.version++;
    mapper.history(id,user.userId(),previous,row.status,reply,row.updatedAt);return detail(user,id,true);
  }
  private Map<String,Object> view(FeedbackRow row,boolean admin) { return view(row,admin,admin); }
  private Map<String,Object> view(FeedbackRow row,boolean history,boolean admin) {
    Map<String,Object> out=new LinkedHashMap<>();out.put("feedbackId",row.id);out.put("type",row.type);out.put("content",row.content);
    out.put("status",row.status);out.put("reply",row.reply);out.put("version",row.version);out.put("createdAt",row.createdAt);out.put("updatedAt",row.updatedAt);
    out.put("images",mapper.images(row.id).stream().map(i->Map.of("imageId",i.id)).toList());
    if(admin){out.put("ownerUserId",row.ownerUserId);out.put("ownerName",row.ownerName);}
    if(history)out.put("history",mapper.histories(row.id));return out;
  }
}
