package com.familykitchen.family.mapper;
import com.familykitchen.family.model.entity.FamilyMembershipRequestDO;
import com.familykitchen.family.model.entity.FamilyRecord;
import com.familykitchen.family.model.entity.FamilyApplicationDO;
import java.util.List;
import com.familykitchen.family.model.vo.MerchantOptionView;
import org.apache.ibatis.annotations.*;
/** 家庭邀请、申请和负责人流程 Mapper。 */
@Mapper public interface FamilyWorkflowMapper {
  /**
   * 统计Active商户。
   *
   * @param merchantId 商户标识
   * @return 统计Active商户的结果
   */
  @Select("SELECT COUNT(*) FROM merchants WHERE id=#{merchantId} AND status='active'") int countActiveMerchant(Long merchantId);

  /**
   * 统计指定家庭是否处于有效状态。
   *
   * @param familyId 家庭ID
   * @return 有效家庭数量
   */
  @Select("SELECT COUNT(*) FROM families WHERE id=#{familyId} AND status='active'")
  int countActiveFamily(Long familyId);
  /**
   * 查询Active商户选项。
   *
   * @return 查询Active商户选项的结果
   */
  @Select("SELECT id,name FROM merchants WHERE status='active' ORDER BY name,id") List<MerchantOptionView> selectActiveMerchantOptions();
  /**
   * 新增家庭。
   *
   * @param family 家庭
   * @return 新增家庭的结果
   */
  @Insert("INSERT INTO families(merchant_id,name,status) VALUES(#{merchantId},#{familyName},'active')")
  @Options(useGeneratedKeys=true,keyProperty="familyId",keyColumn="id") int insertFamily(FamilyRecord family);
  /**
   * 新增商户。
   *
   * @param application 申请
   * @return 新增商户的结果
   */
  @Insert("INSERT INTO merchants(name,status,contact_name,contact_phone) VALUES(#{proposedMerchantName},'active',#{proposedContactName},#{proposedContactPhone})")
  @Options(useGeneratedKeys=true,keyProperty="merchantId",keyColumn="id") int insertMerchant(FamilyApplicationDO application);
  /**
   * 新增商户负责人。
   *
   * @param userId 用户标识
   * @param merchantId 商户标识
   * @return 新增商户负责人的结果
   */
  @Insert("INSERT INTO merchant_user_relations(user_id,merchant_id,merchant_role,status) VALUES(#{userId},#{merchantId},'MERCHANT_ADMIN','ACTIVE') ON DUPLICATE KEY UPDATE status='ACTIVE'")
  int insertMerchantOwner(@Param("userId")Long userId,@Param("merchantId")Long merchantId);
  /**
   * 新增请求参数。
   *
   * @param e 实体
   * @return 新增请求参数的结果
   */
  @Insert("INSERT INTO family_membership_requests(request_type,family_id,target_user_id,applicant_user_id,invitation_code_id,status,created_by) VALUES(#{requestType},#{familyId},#{targetUserId},#{applicantUserId},#{invitationCodeId},'PENDING',#{createdBy})")
  @Options(useGeneratedKeys=true,keyProperty="id") int insertRequest(FamilyMembershipRequestDO e);
  /**
   * 处理请求参数。
   *
   * @param id 标识
   * @return 处理请求参数的结果
   */
  @Select("SELECT * FROM family_membership_requests WHERE id=#{id} FOR UPDATE") FamilyMembershipRequestDO lockRequest(Long id);
  /**
   * 查询用户Invitations。
   *
   * @param userId 用户标识
   * @return 查询用户Invitations的结果
   */
  @Select("SELECT * FROM family_membership_requests WHERE target_user_id=#{userId} AND status='PENDING' ORDER BY id DESC")
  List<FamilyMembershipRequestDO> selectUserInvitations(Long userId);
  /**
   * 查询家庭Applications。
   *
   * @param familyId 家庭标识
   * @return 查询家庭Applications的结果
   */
  @Select("SELECT * FROM family_membership_requests WHERE family_id=#{familyId} AND request_type='CODE_APPLICATION' AND status='PENDING' ORDER BY id")
  List<FamilyMembershipRequestDO> selectFamilyApplications(Long familyId);
  /**
   * 查询Latest编码申请。
   *
   * @param userId 用户标识
   * @return 查询Latest编码申请的结果
   */
  @Select("SELECT * FROM family_membership_requests WHERE applicant_user_id=#{userId} AND request_type='CODE_APPLICATION' ORDER BY id DESC LIMIT 1")
  FamilyMembershipRequestDO findLatestCodeApplication(Long userId);
  /**
   * 处理家庭Workflow。
   *
   * @param id 标识
   * @param status 状态
   * @param userId 用户标识
   * @param reason 原因
   * @return 处理的结果
   */
  @Update("UPDATE family_membership_requests SET status=#{status},reviewed_by=#{userId},reason=#{reason},handled_at=NOW() WHERE id=#{id} AND status='PENDING'")
  int finish(@Param("id")Long id,@Param("status")String status,@Param("userId")Long userId,@Param("reason")String reason);
  /**
   * 停用Codes。
   *
   * @param familyId 家庭标识
   * @return 停用Codes的结果
   */
  @Update("UPDATE family_invitation_codes SET status='INACTIVE',rotated_at=NOW() WHERE family_id=#{familyId} AND status='ACTIVE'")
  int disableCodes(Long familyId);
  /**
   * 新增编码。
   *
   * @param familyId 家庭标识
   * @param hash hash
   * @param userId 用户标识
   * @return 新增编码的结果
   */
  @Insert("INSERT INTO family_invitation_codes(family_id,code_hash,status,expires_at,created_by) VALUES(#{familyId},#{hash},'ACTIVE',DATE_ADD(NOW(),INTERVAL 7 DAY),#{userId})")
  int insertCode(@Param("familyId")Long familyId,@Param("hash")String hash,@Param("userId")Long userId);
  /**
   * 查询编码标识。
   *
   * @param hash hash
   * @return 查询编码标识的结果
   */
  @Select("SELECT id FROM family_invitation_codes WHERE code_hash=#{hash} AND status='ACTIVE' AND expires_at>NOW() LIMIT 1")
  Long findCodeId(String hash);
  /**
   * 查询编码家庭。
   *
   * @param id 标识
   * @return 查询编码家庭的结果
   */
  @Select("SELECT family_id FROM family_invitation_codes WHERE id=#{id}") Long findCodeFamily(Long id);
  /**
   * 处理角色。
   *
   * @param userId 用户标识
   * @param familyId 家庭标识
   * @return 处理角色的结果
   */
  @Select("SELECT family_role FROM family_user_relations WHERE user_id=#{userId} AND family_id=#{familyId} AND status='ACTIVE' FOR UPDATE")
  String lockRole(@Param("userId")Long userId,@Param("familyId")Long familyId);
  /**
   * 更新角色。
   *
   * @param userId 用户标识
   * @param familyId 家庭标识
   * @param role 角色
   * @return 更新角色的结果
   */
  @Update("UPDATE family_user_relations SET family_role=#{role} WHERE user_id=#{userId} AND family_id=#{familyId} AND status='ACTIVE'")
  int updateRole(@Param("userId")Long userId,@Param("familyId")Long familyId,@Param("role")String role);
  /**
   * 处理Relations。
   *
   * @param familyId 家庭标识
   * @param userId 用户标识
   * @return 处理Relations的结果
   */
  @Update("UPDATE family_user_relations SET status='DISSOLVED',ended_at=NOW(),ended_by=#{userId},end_reason='家庭负责人解散家庭' WHERE family_id=#{familyId} AND status='ACTIVE'")
  int dissolveRelations(@Param("familyId")Long familyId,@Param("userId")Long userId);
  /**
   * 停用家庭。
   *
   * @param familyId 家庭标识
   * @return 停用家庭的结果
   */
  @Update("UPDATE families SET status='inactive' WHERE id=#{familyId}") int disableFamily(Long familyId);
  /**
   * 处理Requests。
   *
   * @param familyId 家庭标识
   * @return 处理Requests的结果
   */
  @Update("UPDATE family_membership_requests SET status='INVALIDATED',handled_at=NOW(),reason='家庭已解散' WHERE family_id=#{familyId} AND status='PENDING'")
  int invalidateRequests(Long familyId);
}
