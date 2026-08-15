package com.familykitchen.auth.mapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/** 密码找回记录 Mapper。 */
@Mapper
public interface PasswordResetMapper {
  /**
   * 新增密码Reset。
   *
   * @param userId 用户标识
   * @param email 邮箱
   * @param codeHash 编码Hash
   * @param requestIp 请求参数Ip
   * @return 新增结果后的结果
   */
  int insert(@Param("userId") Long userId, @Param("email") String email,
             @Param("codeHash") String codeHash, @Param("requestIp") String requestIp);
  /**
   * 处理Pending。
   *
   * @param email 邮箱
   * @return 处理Pending后的结果
   */
  int invalidatePending(@Param("email") String email);
  /**
   * 统计Recent。
   *
   * @param email 邮箱
   * @param requestIp 请求参数Ip
   * @return 统计Recent后的结果
   */
  int countRecent(@Param("email") String email,@Param("requestIp") String requestIp);
  /**
   * 查询Consumable。
   *
   * @param email 邮箱
   * @param codeHash 编码Hash
   * @return 查询Consumable后的结果
   */
  Long findConsumable(@Param("email") String email, @Param("codeHash") String codeHash);
  /**
   * 处理LatestFailure。
   *
   * @param email 邮箱
   * @return 处理LatestFailure后的结果
   */
  int incrementLatestFailure(@Param("email") String email);
  /**
   * 处理密码Reset。
   *
   * @param id 标识
   * @return 处理结果
   */
  int consume(@Param("id") Long id);
}
