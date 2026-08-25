package com.familykitchen.merchant.mapper;

import com.familykitchen.merchant.model.vo.MerchantProfileView;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 商户负责人自助资料的数据访问接口。 */
@Mapper
public interface MerchantProfileMapper {

  /**
   * 在实时校验负责人关系后读取商户资料。
   * @param userId 当前用户标识
   * @param merchantId 当前商户标识
   * @return 可编辑商户资料；权限或状态无效时返回空
   */
  @Select("""
      SELECT m.name, m.contact_name, m.contact_phone
      FROM merchants m
      JOIN merchant_user_relations mur ON mur.merchant_id=m.id
        AND mur.user_id=#{userId}
        AND mur.merchant_role='MERCHANT_ADMIN'
        AND mur.status='ACTIVE'
      WHERE m.id=#{merchantId}
        AND m.status='active'
      """)
  @ConstructorArgs({
      @Arg(column = "name", javaType = String.class),
      @Arg(column = "contact_name", javaType = String.class),
      @Arg(column = "contact_phone", javaType = String.class)
  })
  MerchantProfileView selectProfile(@Param("userId") Long userId,
      @Param("merchantId") Long merchantId);

  /**
   * 在同一条语句中校验负责人关系并更新商户资料。
   * @param userId 当前用户标识
   * @param merchantId 当前商户标识
   * @param name 商户名称
   * @param contactName 联系人姓名
   * @param contactPhone 联系电话
   * @return 更新行数
   */
  @Update("""
      UPDATE merchants m
      SET m.name=#{name}, m.contact_name=#{contactName}, m.contact_phone=#{contactPhone}
      WHERE m.id=#{merchantId}
        AND m.status='active'
        AND EXISTS (
          SELECT 1 FROM merchant_user_relations mur
          WHERE mur.merchant_id=m.id
            AND mur.user_id=#{userId}
            AND mur.merchant_role='MERCHANT_ADMIN'
            AND mur.status='ACTIVE'
        )
      """)
  int updateProfile(@Param("userId") Long userId, @Param("merchantId") Long merchantId,
      @Param("name") String name, @Param("contactName") String contactName,
      @Param("contactPhone") String contactPhone);
}
