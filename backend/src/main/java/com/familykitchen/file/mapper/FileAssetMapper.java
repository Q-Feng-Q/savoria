package com.familykitchen.file.mapper;

import com.familykitchen.file.model.entity.FileAssetDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 文件资产数据库访问接口。 */
@Mapper
public interface FileAssetMapper {
  /**
   * 保存文件资产元数据并回填生成的主键。
   * @param asset 待保存的文件资产
   * @return 受影响行数
   */
  @Insert("INSERT INTO file_assets(owner_type,owner_id,file_type,storage_type,url,object_key,mime_type,size_bytes,width,height) "
      + "VALUES(#{ownerType},#{ownerId},#{fileType},#{storageType},#{url},#{objectKey},#{mimeType},#{sizeBytes},#{width},#{height})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(FileAssetDO asset);

  /**
   * 查询当前账号拥有的图片资产，供需要引用已上传图片的业务做归属校验。
   * @param fileId 文件资产ID
   * @param ownerUserId 当前账号ID
   * @return 匹配的图片资产，不存在或不属于当前账号时为空
   */
  @Select("SELECT id,owner_type ownerType,owner_id ownerId,file_type fileType,storage_type storageType,"
      + "url,object_key objectKey,mime_type mimeType,size_bytes sizeBytes,width,height FROM file_assets "
      + "WHERE id=#{fileId} AND owner_type='user' AND owner_id=#{ownerUserId} AND file_type='image'")
  FileAssetDO selectOwnedImage(@Param("fileId") Long fileId, @Param("ownerUserId") Long ownerUserId);
}
