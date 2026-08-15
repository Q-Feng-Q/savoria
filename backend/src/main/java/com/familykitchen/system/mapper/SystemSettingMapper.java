package com.familykitchen.system.mapper;
import com.familykitchen.system.model.entity.SystemSettingDO;
import org.apache.ibatis.annotations.Mapper;
/**
 * 持久化平台唯一的系统配置记录。
 *
 * <p>数据库约定配置固定使用主键 {@code id = 1}，因此查询不接收业务主键，
 * 更新操作也只面向该单例记录。</p>
 */
@Mapper
public interface SystemSettingMapper {
  /**
   * 查询当前平台系统配置。
   * @return 当前唯一一份平台系统配置
   */
  SystemSettingDO selectCurrent();
  /**
   * 更新平台系统配置。
   * @param setting 待持久化的配置
   * @return 受影响行数
   */
  int update(SystemSettingDO setting);
}
