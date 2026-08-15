package com.familykitchen.file.model.entity;

/** 文件资产持久化实体，记录文件归属、存储定位信息及可查询的图片元数据。 */
public class FileAssetDO {
  /** 文件资产主键。 */
  private Long id;
  /** 归属主体类型，例如 {@code user}。 */
  private String ownerType;
  /** 归属主体标识。 */
  private Long ownerId;
  /** 文件业务类型，例如 {@code image}。 */
  private String fileType;
  /** 存储实现类型，例如 {@code local}。 */
  private String storageType;
  /** 对外访问路径。 */
  private String url;
  /** 存储系统中的对象键或相对路径。 */
  private String objectKey;
  /** 文件 MIME 类型。 */
  private String mimeType;
  /** 文件大小，单位为字节。 */
  private Long sizeBytes;
  /** 图片宽度，单位为像素。 */
  private Integer width;
  /** 图片高度，单位为像素。 */
  private Integer height;
  /**
   * 返回文件资产主键。
   *
   * @return 文件资产主键
   */
  public Long getId(){return id;}
  /**
   * 设置文件资产主键。
   *
   * @param value 文件资产主键
   */
  public void setId(Long value){id=value;}
  /**
   * 返回归属主体类型。
   *
   * @return 归属主体类型
   */
  public String getOwnerType(){return ownerType;}
  /**
   * 设置归属主体类型。
   *
   * @param value 归属主体类型
   */
  public void setOwnerType(String value){ownerType=value;}
  /**
   * 返回归属主体标识。
   *
   * @return 归属主体标识
   */
  public Long getOwnerId(){return ownerId;}
  /**
   * 设置归属主体标识。
   *
   * @param value 归属主体标识
   */
  public void setOwnerId(Long value){ownerId=value;}
  /**
   * 返回文件业务类型。
   *
   * @return 文件业务类型
   */
  public String getFileType(){return fileType;}
  /**
   * 设置文件业务类型。
   *
   * @param value 文件业务类型
   */
  public void setFileType(String value){fileType=value;}
  /**
   * 返回存储实现类型。
   *
   * @return 存储实现类型
   */
  public String getStorageType(){return storageType;}
  /**
   * 设置存储实现类型。
   *
   * @param value 存储实现类型
   */
  public void setStorageType(String value){storageType=value;}
  /**
   * 返回对外访问路径。
   *
   * @return 对外访问路径
   */
  public String getUrl(){return url;}
  /**
   * 设置对外访问路径。
   *
   * @param value 对外访问路径
   */
  public void setUrl(String value){url=value;}
  /**
   * 返回存储对象键。
   *
   * @return 存储对象键
   */
  public String getObjectKey(){return objectKey;}
  /**
   * 设置存储对象键。
   *
   * @param value 存储对象键
   */
  public void setObjectKey(String value){objectKey=value;}
  /**
   * 返回文件 MIME 类型。
   *
   * @return 文件 MIME 类型
   */
  public String getMimeType(){return mimeType;}
  /**
   * 设置文件 MIME 类型。
   *
   * @param value 文件 MIME 类型
   */
  public void setMimeType(String value){mimeType=value;}
  /**
   * 返回文件字节数。
   *
   * @return 文件字节数
   */
  public Long getSizeBytes(){return sizeBytes;}
  /**
   * 设置文件字节数。
   *
   * @param value 文件字节数
   */
  public void setSizeBytes(Long value){sizeBytes=value;}
  /**
   * 返回图片宽度。
   *
   * @return 图片宽度（像素）
   */
  public Integer getWidth(){return width;}
  /**
   * 设置图片宽度。
   *
   * @param value 图片宽度（像素）
   */
  public void setWidth(Integer value){width=value;}
  /**
   * 返回图片高度。
   *
   * @return 图片高度（像素）
   */
  public Integer getHeight(){return height;}
  /**
   * 设置图片高度。
   *
   * @param value 图片高度（像素）
   */
  public void setHeight(Integer value){height=value;}
}
