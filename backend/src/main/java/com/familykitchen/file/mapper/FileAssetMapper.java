package com.familykitchen.file.mapper;

import com.familykitchen.file.model.entity.FileAssetDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

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
}
