package com.familykitchen.system.mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/** 系统配置与审核操作审计 Mapper。 */
@Mapper
public interface SystemAuditMapper {
  /**
   * 记录一次成功的系统配置或审核操作。
   * @param userId 操作账号标识
   * @param action 操作类型
   * @param detail 审计详情
   * @return 受影响行数
   */
  @Insert("INSERT INTO security_audit_logs(user_id,action,result,detail) VALUES(#{userId},#{action},'SUCCESS',#{detail})")
  int insert(@Param("userId") Long userId,@Param("action") String action,@Param("detail") String detail);
}
