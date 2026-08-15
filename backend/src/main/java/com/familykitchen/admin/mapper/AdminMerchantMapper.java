package com.familykitchen.admin.mapper;
import com.familykitchen.admin.model.entity.AdminMerchantDO;import com.familykitchen.admin.model.vo.AdminMerchantView;import java.util.List;import org.apache.ibatis.annotations.*;
/**
 * 负责平台管理商户相关数据的数据库访问与持久化更新。
 */
@Mapper public interface AdminMerchantMapper {
 /**
  * 列出平台管理商户。
  *
  * @param keyword keyword
  * @return 列出结果后的结果
  */
 @Select("""
   SELECT m.id merchant_id,m.name,m.status,m.contact_name,m.contact_phone,
   (SELECT mur.user_id FROM merchant_user_relations mur WHERE mur.merchant_id=m.id AND mur.status='ACTIVE' ORDER BY mur.id LIMIT 1) owner_user_id,
   (SELECT u.username FROM merchant_user_relations mur JOIN users u ON u.id=mur.user_id WHERE mur.merchant_id=m.id AND mur.status='ACTIVE' ORDER BY mur.id LIMIT 1) owner_username,
   (SELECT COUNT(*) FROM families f WHERE f.merchant_id=m.id AND f.status='active') family_count,CAST(m.created_at AS CHAR) created_at
   FROM merchants m WHERE (#{keyword} IS NULL OR #{keyword}='' OR m.name LIKE CONCAT('%',#{keyword},'%') OR m.contact_name LIKE CONCAT('%',#{keyword},'%')) ORDER BY m.id DESC
   """) List<AdminMerchantView> list(String keyword);
 /**
  * 新增平台管理商户。
  *
  * @param e 数据实体
  * @return 新增结果后的结果
  */
 @Insert("INSERT INTO merchants(name,status,contact_name,contact_phone) VALUES(#{name},#{status},#{contactName},#{contactPhone})") @Options(useGeneratedKeys=true,keyProperty="id") int insert(AdminMerchantDO e);
 /**
  * 更新平台管理商户。
  *
  * @param id 标识
  * @param name 名称
  * @param status 状态
  * @param contactName 联系人名称
  * @param contactPhone 联系人联系电话
  * @return 更新结果后的结果
  */
 @Update("UPDATE merchants SET name=#{name},status=#{status},contact_name=#{contactName},contact_phone=#{contactPhone} WHERE id=#{id}") int update(@Param("id")Long id,@Param("name")String name,@Param("status")String status,@Param("contactName")String contactName,@Param("contactPhone")String contactPhone);
 /**
  * 停用Owners。
  *
  * @param merchantId 商户标识
  * @return 停用Owners后的结果
  */
 @Update("UPDATE merchant_user_relations SET status='INACTIVE' WHERE merchant_id=#{merchantId} AND status='ACTIVE'") int disableOwners(Long merchantId);
 /**
  * 新增负责人。
  *
  * @param merchantId 商户标识
  * @param userId 用户标识
  * @return 新增负责人后的结果
  */
 @Insert("INSERT INTO merchant_user_relations(user_id,merchant_id,merchant_role,status) VALUES(#{userId},#{merchantId},'MERCHANT_ADMIN','ACTIVE') ON DUPLICATE KEY UPDATE status='ACTIVE'") int insertOwner(@Param("merchantId")Long merchantId,@Param("userId")Long userId);
 /**
  * 处理Delete。
  *
  * @param id 标识
  * @return 处理Delete后的结果
  */
 @Update("UPDATE merchants SET status='inactive' WHERE id=#{id} AND status<>'inactive'") int softDelete(Long id);
 /**
  * 统计Active用户。
  *
  * @param id 标识
  * @return 统计Active用户后的结果
  */
 @Select("SELECT COUNT(*) FROM users WHERE id=#{id} AND status='ACTIVE'") int countActiveUser(Long id);
 /**
  * 统计Active商户。
  *
  * @param id 标识
  * @return 统计Active商户后的结果
  */
 @Select("SELECT COUNT(*) FROM merchants WHERE id=#{id} AND status<>'inactive'") int countActiveMerchant(Long id);
}
