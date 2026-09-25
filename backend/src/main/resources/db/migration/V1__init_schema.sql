-- 家庭厨房最终数据库结构。
-- 空库初始化专用；关系完整性由应用层维护，不创建物理外键。
-- 2026-09-23：已合并品牌配置和步骤图片字段，仅供全新空库使用。

SET NAMES utf8mb4;

CREATE TABLE user_feedback (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  owner_user_id bigint NOT NULL COMMENT '提交用户ID',
  request_id varchar(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '提交幂等请求标识',
  type varchar(20) NOT NULL COMMENT '反馈类型',
  content varchar(2000) NOT NULL COMMENT '反馈内容',
  status varchar(20) NOT NULL DEFAULT 'OPEN' COMMENT '处理状态',
  reply varchar(2000) NOT NULL DEFAULT '' COMMENT '最新回复',
  version int NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  handled_by bigint DEFAULT NULL COMMENT '处理管理员用户ID',
  created_at datetime(6) NOT NULL COMMENT '提交时间',
  updated_at datetime(6) NOT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_feedback_request (owner_user_id,request_id),
  KEY idx_feedback_owner_created (owner_user_id,created_at),
  KEY idx_feedback_filters (status,type,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户反馈表';

CREATE TABLE feedback_images (
  id varchar(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '图片标识',
  owner_user_id bigint NOT NULL COMMENT '上传用户ID',
  object_key varchar(80) NOT NULL COMMENT '私有存储对象键',
  mime varchar(30) NOT NULL COMMENT '图片媒体类型',
  width int NOT NULL COMMENT '图片宽度像素',
  height int NOT NULL COMMENT '图片高度像素',
  byte_size int NOT NULL COMMENT '图片文件字节数',
  feedback_id bigint DEFAULT NULL COMMENT '关联反馈ID',
  sort_order int NOT NULL DEFAULT 0 COMMENT '图片显示顺序',
  created_at datetime(6) NOT NULL COMMENT '上传时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_feedback_object (object_key),
  KEY idx_feedback_image_owner_created (owner_user_id,created_at),
  KEY idx_feedback_image_expired (feedback_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='反馈私有图片表';

CREATE TABLE feedback_history (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '处理记录ID',
  feedback_id bigint NOT NULL COMMENT '反馈ID',
  admin_id bigint NOT NULL COMMENT '操作管理员用户ID',
  from_status varchar(20) NOT NULL COMMENT '变更前状态',
  to_status varchar(20) NOT NULL COMMENT '变更后状态',
  reply varchar(2000) NOT NULL COMMENT '处理回复',
  created_at datetime(6) NOT NULL COMMENT '处理时间',
  PRIMARY KEY (id),
  KEY idx_feedback_history (feedback_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='反馈处理历史表';

CREATE TABLE account_cancellation_requests (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '账号注销申请ID',
  user_id bigint NOT NULL COMMENT '用户ID',
  status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '注销状态',
  requested_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  cooling_end_at datetime NOT NULL COMMENT '冷静期截止时间',
  cancelled_at datetime DEFAULT NULL COMMENT '撤销时间',
  completed_at datetime DEFAULT NULL COMMENT '完成时间',
  block_reason varchar(255) DEFAULT NULL COMMENT '阻断原因',
  pending_user_id bigint GENERATED ALWAYS AS ((case when (status = _utf8mb4'PENDING') then user_id else NULL end)) STORED COMMENT '待处理用户唯一键',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cancellation_pending_user (pending_user_id),
  KEY idx_cancellation_due (status,cooling_end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号注销申请表';

CREATE TABLE application_instance_leases (
  instance_id varchar(128) NOT NULL COMMENT '应用实例ID',
  build_version varchar(128) NOT NULL COMMENT '应用构建版本',
  lease_owner varchar(128) NOT NULL COMMENT '租约持有者',
  heartbeat_at datetime NOT NULL COMMENT '最近心跳时间',
  lease_expires_at datetime NOT NULL COMMENT '租约过期时间',
  started_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '实例启动时间',
  metadata_json json DEFAULT NULL COMMENT '实例元数据',
  PRIMARY KEY (instance_id),
  KEY idx_application_instance_leases_expiry (lease_expires_at),
  KEY idx_application_instance_leases_build (build_version,heartbeat_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用实例租约与构建心跳';

CREATE TABLE cart_item_selections (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '餐篮项成员选择ID',
  cart_item_id bigint NOT NULL COMMENT '餐篮项ID',
  user_id bigint NOT NULL COMMENT '选择成员用户ID',
  quantity int NOT NULL COMMENT '成员选择份数',
  item_remark varchar(255) DEFAULT NULL COMMENT '成员单品备注',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cart_item_selections_item_user (cart_item_id,user_id),
  KEY idx_cart_item_selections_user (user_id),
  CONSTRAINT ck_cart_item_selections_quantity CHECK ((quantity > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='餐篮项成员选择表';

CREATE TABLE cart_items (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '餐篮项ID',
  cart_id bigint NOT NULL COMMENT '所属餐篮ID',
  dish_id bigint NOT NULL COMMENT '菜品ID',
  dish_name_snapshot varchar(100) DEFAULT NULL COMMENT '菜品名称快照',
  price decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '单价快照',
  quantity int NOT NULL COMMENT '份数',
  item_remark varchar(255) DEFAULT NULL COMMENT '单品备注',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cart_items_cart_dish (cart_id,dish_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='餐篮菜品明细表';

CREATE TABLE carts (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '餐篮ID',
  merchant_id bigint NOT NULL COMMENT '商户ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  user_id bigint DEFAULT NULL COMMENT '创建餐篮的用户ID（兼容旧流程）',
  meal_slot_id bigint DEFAULT NULL COMMENT '餐次ID（兼容旧流程）',
  service_date date DEFAULT NULL COMMENT '服务日期（兼容旧流程）',
  expected_meal_time datetime DEFAULT NULL COMMENT '期望用餐时间',
  remark varchar(500) DEFAULT NULL COMMENT '整篮备注',
  status varchar(20) NOT NULL DEFAULT 'active' COMMENT '餐篮状态：active/submitted/archived',
  version bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  active_cart_key varchar(160) GENERATED ALWAYS AS ((case when (status = _utf8mb4'active') then concat(family_id,_utf8mb4':',user_id,_utf8mb4':',meal_slot_id,_utf8mb4':',service_date) else NULL end)) STORED COMMENT '有效餐篮唯一键',
  PRIMARY KEY (id),
  UNIQUE KEY uk_carts_active_cart (active_cart_key),
  KEY idx_carts_family_user_date_slot (family_id,user_id,service_date,meal_slot_id,status),
  KEY idx_carts_merchant_date (merchant_id,service_date,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户当日餐篮表';

CREATE TABLE command_idempotency (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '命令幂等记录ID',
  actor_user_id bigint NOT NULL COMMENT '操作用户ID',
  family_id bigint NOT NULL COMMENT '规范化非空家庭范围ID',
  operation varchar(80) NOT NULL COMMENT '操作名称',
  request_id varchar(128) NOT NULL COMMENT '客户端请求ID',
  payload_hash varchar(128) NOT NULL COMMENT '请求载荷摘要',
  state varchar(30) NOT NULL COMMENT '处理状态',
  result_resource_type varchar(80) DEFAULT NULL COMMENT '结果资源类型',
  result_resource_id bigint DEFAULT NULL COMMENT '结果资源ID',
  result_body json DEFAULT NULL COMMENT '结果响应体',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_command_idempotency_scope (actor_user_id,family_id,operation,request_id),
  KEY idx_command_idempotency_state_updated (state,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='持久命令幂等记录';

CREATE TABLE dish_categories (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '菜品分类ID',
  merchant_id bigint NOT NULL COMMENT '所属商户ID',
  name varchar(50) NOT NULL COMMENT '分类名称',
  sort_order int NOT NULL DEFAULT '0' COMMENT '排序值',
  enabled tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dish_categories_merchant_name (merchant_id,name),
  KEY idx_dish_categories_merchant_enabled_sort (merchant_id,enabled,sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户菜品分类表';

CREATE TABLE dish_cooking_steps (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '制作步骤ID',
  dish_id bigint NOT NULL COMMENT '所属菜品ID',
  step_no int NOT NULL COMMENT '步骤序号',
  title varchar(100) DEFAULT NULL COMMENT '步骤标题',
  content text COMMENT '步骤内容',
  duration_seconds int DEFAULT NULL COMMENT '制作持续秒数',
  temperature_text varchar(100) DEFAULT NULL COMMENT '制作温度说明',
  heat_level varchar(50) DEFAULT NULL COMMENT '制作火候说明',
  source_template_step_id bigint DEFAULT NULL COMMENT '来源平台模板步骤逻辑ID',
  component_template_id bigint DEFAULT NULL COMMENT '来源配料组件模板逻辑ID',
  source_note varchar(1000) DEFAULT NULL COMMENT '来源步骤简短追踪说明',
  image_urls json DEFAULT NULL COMMENT '有序步骤图片地址，最多五张',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dish_cooking_steps_dish_step (dish_id,step_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品制作步骤表';

CREATE TABLE dish_ingredients (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '菜品食材记录ID',
  dish_id bigint NOT NULL COMMENT '所属菜品ID',
  ingredient_name varchar(255) NOT NULL COMMENT '食材名称',
  quantity decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '计算数量',
  unit varchar(20) NOT NULL COMMENT '单位',
  calc_type varchar(20) NOT NULL COMMENT '计算方式：FIXED/PER_PERSON/NO_PURCHASE',
  PRIMARY KEY (id),
  KEY idx_dish_ingredients_dish (dish_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品食材明细表';

CREATE TABLE dish_review_submissions (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '菜品审核提交ID',
  merchant_id bigint NOT NULL COMMENT '商户ID',
  target_dish_id bigint DEFAULT NULL COMMENT '目标正式菜品ID',
  submission_type varchar(20) NOT NULL COMMENT '提交类型：CREATE/UPDATE',
  snapshot_json json NOT NULL COMMENT '菜品完整快照',
  status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/WITHDRAWN',
  submitted_by bigint NOT NULL COMMENT '提交用户ID',
  reviewed_by bigint DEFAULT NULL COMMENT '审核管理员用户ID',
  review_reason varchar(500) DEFAULT NULL COMMENT '审核原因或备注',
  submitted_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  reviewed_at datetime DEFAULT NULL COMMENT '审核时间',
  pending_target_key varchar(100) GENERATED ALWAYS AS ((case when ((status = _utf8mb4'PENDING') and (target_dish_id is not null)) then concat(merchant_id,_utf8mb4':',target_dish_id) else NULL end)) STORED COMMENT '待审核目标唯一键',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dish_review_pending_target (pending_target_key),
  KEY idx_dish_review_status_time (status,submitted_at),
  KEY idx_dish_review_merchant (merchant_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品审核提交表';

CREATE TABLE dish_template_categories (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '平台模板分类ID',
  code varchar(40) NOT NULL COMMENT '稳定分类编码',
  name varchar(50) NOT NULL COMMENT '分类名称',
  sort_order int NOT NULL DEFAULT '0' COMMENT '排序值',
  enabled tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dish_template_categories_code (code),
  UNIQUE KEY uk_dish_template_categories_name (name),
  KEY idx_dish_template_categories_enabled_sort (enabled,sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台菜品模板分类表';

CREATE TABLE dish_template_change_requests (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '模板菜品修改申请ID',
  merchant_id bigint NOT NULL COMMENT '提交商户ID',
  template_id bigint NOT NULL COMMENT '目标平台模板菜品ID',
  base_template_version bigint NOT NULL COMMENT '提交时模板并发版本号',
  base_snapshot_json json NOT NULL COMMENT '提交时模板完整业务快照',
  snapshot_json json NOT NULL COMMENT '申请覆盖后的完整业务快照',
  submit_note varchar(500) DEFAULT NULL COMMENT '商户提交说明',
  status varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING待审核、APPROVED已通过、REJECTED已驳回、WITHDRAWN已撤回',
  submitted_by bigint NOT NULL COMMENT '提交用户ID',
  reviewed_by bigint DEFAULT NULL COMMENT '审核平台管理员用户ID',
  review_reason varchar(500) DEFAULT NULL COMMENT '审核意见或驳回原因',
  submitted_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '提交时间',
  reviewed_at datetime(6) DEFAULT NULL COMMENT '审核完成时间',
  withdrawn_by bigint DEFAULT NULL COMMENT '撤回用户ID',
  withdrawn_at datetime(6) DEFAULT NULL COMMENT '撤回时间',
  result_notification_id bigint DEFAULT NULL COMMENT '审核结果站内通知ID',
  pending_marker tinyint GENERATED ALWAYS AS ((case when (status = _utf8mb4'PENDING') then 1 else NULL end)) STORED COMMENT '待审核唯一标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dish_template_change_pending (merchant_id,template_id,pending_marker),
  KEY idx_dish_template_change_status_time (status,submitted_at,id),
  KEY idx_dish_template_change_merchant_time (merchant_id,status,submitted_at,id),
  KEY idx_dish_template_change_template_time (template_id,status,submitted_at,id),
  CONSTRAINT chk_dish_template_change_status CHECK ((status in (_utf8mb4'PENDING',_utf8mb4'APPROVED',_utf8mb4'REJECTED',_utf8mb4'WITHDRAWN')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台模板菜品修改审核申请';

CREATE TABLE dish_template_cooking_steps (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '模板制作步骤ID',
  item_key varchar(500) DEFAULT NULL COMMENT '跨完整快照保持稳定的步骤项键',
  template_id bigint NOT NULL COMMENT '平台菜品模板逻辑ID',
  step_no int NOT NULL COMMENT '步骤序号',
  title varchar(100) DEFAULT NULL COMMENT '步骤标题',
  content text NOT NULL COMMENT '归纳后的完整制作操作',
  source_text varchar(1000) DEFAULT NULL COMMENT '简短来源定位和事实摘要',
  duration_seconds int DEFAULT NULL COMMENT '可可靠识别的持续秒数',
  temperature_text varchar(100) DEFAULT NULL COMMENT '可可靠识别的温度说明',
  heat_level varchar(50) DEFAULT NULL COMMENT '可可靠识别的火候说明',
  component_template_id bigint DEFAULT NULL COMMENT '当前步骤使用的配料组件逻辑ID',
  image_urls json DEFAULT NULL COMMENT '有序步骤图片地址，最多五张',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dish_template_cooking_steps_template_step (template_id,step_no),
  UNIQUE KEY uk_dish_template_cooking_steps_item_key (item_key),
  KEY idx_dish_template_cooking_steps_component (component_template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台菜品模板制作步骤表';

CREATE TABLE dish_template_image_assets (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '模板内部图片资产ID',
  template_id bigint NOT NULL COMMENT '平台菜品模板逻辑ID',
  source_record_id bigint NOT NULL COMMENT '模板来源记录逻辑ID',
  internal_storage_key varchar(500) NOT NULL COMMENT '服务端内部私有存储键',
  content_sha256 char(64) NOT NULL COMMENT '图片内容SHA-256',
  mime_type varchar(100) NOT NULL COMMENT '图片MIME类型',
  file_size bigint NOT NULL COMMENT '图片字节数',
  source_image_path varchar(1000) NOT NULL COMMENT '来源仓库图片相对路径',
  source_url varchar(1000) NOT NULL COMMENT '来源菜谱页面地址',
  source_revision varchar(64) NOT NULL COMMENT '来源仓库固定提交版本',
  asset_status varchar(30) NOT NULL DEFAULT 'INTERNAL_REVIEW' COMMENT '资产状态：INTERNAL_REVIEW、PUBLISHED、REJECTED',
  public_image_url varchar(500) DEFAULT NULL COMMENT '审核发布后的公共图片地址',
  image_author varchar(255) DEFAULT NULL COMMENT '审核确认的图片作者',
  image_license varchar(255) DEFAULT NULL COMMENT '审核确认的图片许可证',
  reviewed_by bigint DEFAULT NULL COMMENT '审核平台管理员用户ID',
  reviewed_at datetime DEFAULT NULL COMMENT '审核时间',
  rejection_reason varchar(500) DEFAULT NULL COMMENT '拒绝原因',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dish_template_image_assets_storage (internal_storage_key),
  UNIQUE KEY uk_dish_template_image_assets_source (template_id,source_revision,source_image_path(512),content_sha256),
  KEY idx_dish_template_image_assets_status (asset_status,id),
  CONSTRAINT chk_dish_template_image_asset_status CHECK ((asset_status in (_utf8mb4'INTERNAL_REVIEW',_utf8mb4'PUBLISHED',_utf8mb4'REJECTED'))),
  CONSTRAINT chk_template_image_asset_rejection CHECK ((((asset_status = _utf8mb4'REJECTED') and (rejection_reason is not null) and (char_length(trim(rejection_reason)) > 0)) or ((asset_status <> _utf8mb4'REJECTED') and (rejection_reason is null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台菜品模板内部图片审核资产表';

CREATE TABLE dish_template_ingredients (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '平台模板食材明细ID',
  template_id bigint NOT NULL COMMENT '平台菜品模板ID',
  ingredient_name varchar(255) NOT NULL COMMENT '食材名称',
  ingredient_category varchar(50) NOT NULL COMMENT '食材分类',
  quantity decimal(10,2) DEFAULT NULL COMMENT '经家庭化校验的采购数量',
  unit varchar(20) DEFAULT NULL COMMENT '经家庭化校验的计量单位',
  calc_type varchar(20) DEFAULT NULL COMMENT '计算方式：FIXED、PER_PERSON',
  source_text varchar(1000) DEFAULT NULL COMMENT '归一化后的来源原料行',
  source_quantity_text varchar(255) DEFAULT NULL COMMENT '来源批量用量原文',
  quantity_status varchar(30) NOT NULL DEFAULT 'VERIFIED' COMMENT '数量状态：VERIFIED、SOURCE_BATCH、MISSING、NOT_APPLICABLE',
  component_template_id bigint DEFAULT NULL COMMENT '引用配料组件模板的逻辑ID',
  source_line_key varchar(500) DEFAULT NULL COMMENT '来源文件和原料行组成的稳定键',
  component_occurrence_key varchar(500) DEFAULT NULL COMMENT '组件在当前菜谱中的引用出现键',
  component_multiplier decimal(12,4) DEFAULT NULL COMMENT '经验证的组件引用倍数',
  sort_order int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dish_template_ingredients_source_line (source_line_key),
  KEY idx_dish_template_ingredients_template_sort (template_id,sort_order),
  CONSTRAINT chk_template_ingredient_quantity_state CHECK ((((quantity_status = _utf8mb4'VERIFIED') and (quantity > 0) and (unit is not null) and (char_length(trim(unit)) > 0) and (calc_type in (_utf8mb4'FIXED',_utf8mb4'PER_PERSON'))) or ((quantity_status in (_utf8mb4'SOURCE_BATCH',_utf8mb4'MISSING')) and (quantity is null) and (unit is null) and (calc_type is null)) or ((quantity_status = _utf8mb4'NOT_APPLICABLE') and (quantity is null) and (unit is null) and (calc_type is null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台菜品模板食材明细表';

CREATE TABLE dish_template_name_aliases (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '模板名称别名ID',
  template_id bigint NOT NULL COMMENT '平台菜品模板逻辑ID',
  alias_name varchar(255) NOT NULL COMMENT '模板名称别名',
  normalized_alias_name varchar(255) NOT NULL COMMENT '规范化后的全局唯一别名',
  alias_type varchar(30) NOT NULL COMMENT '别名类型：LOCAL_PREVIOUS_NAME、SOURCE_TITLE_VARIANT、MANUAL',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dish_template_name_aliases_normalized (normalized_alias_name),
  KEY idx_dish_template_name_aliases_template (template_id,id),
  CONSTRAINT chk_dish_template_name_alias_type CHECK ((alias_type in (_utf8mb4'LOCAL_PREVIOUS_NAME',_utf8mb4'SOURCE_TITLE_VARIANT',_utf8mb4'MANUAL')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台菜品模板名称别名表';

CREATE TABLE dish_template_source_records (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '模板来源记录ID',
  template_id bigint NOT NULL COMMENT '平台菜品模板逻辑ID',
  source_key varchar(500) NOT NULL COMMENT '来源文件稳定规范化键',
  source_title varchar(255) NOT NULL COMMENT '来源菜谱标题',
  source_category varchar(100) NOT NULL COMMENT '来源仓库原始分类',
  source_path varchar(1000) NOT NULL COMMENT '来源仓库相对路径',
  source_url varchar(1000) NOT NULL COMMENT '来源菜谱页面地址',
  source_revision varchar(64) NOT NULL COMMENT '来源仓库固定提交版本',
  record_type varchar(30) NOT NULL COMMENT '记录类型：PRIMARY、SOURCE_ALIAS、PATH_RENAME',
  alias_reason varchar(500) DEFAULT NULL COMMENT '来源别名或路径重命名原因',
  content_sha256 char(64) NOT NULL COMMENT '规范化Markdown内容SHA-256',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dish_template_source_records_key (source_key),
  KEY idx_dish_template_source_records_template (template_id,record_type,id),
  CONSTRAINT chk_dish_template_source_record_type CHECK ((record_type in (_utf8mb4'PRIMARY',_utf8mb4'SOURCE_ALIAS',_utf8mb4'PATH_RENAME')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台菜品模板来源记录表';

CREATE TABLE dish_templates (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '平台菜品模板ID',
  template_code varchar(40) NOT NULL COMMENT '稳定模板编码',
  category_id bigint NOT NULL COMMENT '平台模板分类ID',
  name varchar(100) NOT NULL COMMENT '模板菜品名称',
  description varchar(255) DEFAULT NULL COMMENT '模板菜品简介',
  product_type varchar(20) NOT NULL DEFAULT 'NORMAL' COMMENT '商品类型：NORMAL/NOURISHMENT',
  nourishment_description varchar(1000) DEFAULT NULL COMMENT '滋补介绍（纯文本）',
  serving_advice varchar(1000) DEFAULT NULL COMMENT '食用建议（纯文本）',
  precautions varchar(1000) DEFAULT NULL COMMENT '注意事项（纯文本）',
  image_url varchar(500) DEFAULT NULL COMMENT '审核发布后的本地图片访问地址',
  image_source_url varchar(1000) DEFAULT NULL COMMENT '已声明授权图片的来源页面',
  image_author varchar(255) DEFAULT NULL COMMENT '已声明授权图片的作者或来源平台',
  image_license varchar(255) DEFAULT NULL COMMENT '已声明授权图片的许可证',
  reference_price decimal(10,2) DEFAULT NULL COMMENT '家庭私厨参考价',
  taste_tags json NOT NULL COMMENT '口味标签JSON',
  meal_tags json NOT NULL COMMENT '推荐餐次JSON',
  template_type varchar(20) NOT NULL DEFAULT 'DISH' COMMENT '模板类型：DISH成品菜、COMPONENT配料组件',
  source_type varchar(30) NOT NULL DEFAULT 'LOCAL_EXTENSION' COMMENT '来源类型：COOK_LIKE_HOC、LOCAL_EXTENSION',
  source_key varchar(500) DEFAULT NULL COMMENT '主来源文件的稳定规范化键',
  source_url varchar(1000) DEFAULT NULL COMMENT '来源菜谱页面地址',
  source_revision varchar(64) DEFAULT NULL COMMENT '来源仓库固定提交版本',
  source_category varchar(100) DEFAULT NULL COMMENT '来源仓库原始分类',
  source_yield_text varchar(255) DEFAULT NULL COMMENT '来源份数或批次说明原文',
  data_status varchar(30) NOT NULL DEFAULT 'READY' COMMENT '数据状态：READY、NEEDS_PURCHASE_DATA、NEEDS_PRICE、NEEDS_BOTH',
  procurement_ready tinyint(1) NOT NULL DEFAULT '1' COMMENT '采购用量是否完成家庭化校验',
  image_rights_status varchar(20) NOT NULL DEFAULT 'DECLARED' COMMENT '图片权利状态：DECLARED、UNDECLARED、NONE',
  sort_order int NOT NULL DEFAULT '0' COMMENT '分类内排序值',
  enabled tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否可导入',
  version bigint NOT NULL DEFAULT '0' COMMENT '模板并发版本号',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dish_templates_code (template_code),
  UNIQUE KEY uk_dish_templates_source_key (source_key),
  KEY idx_dish_templates_category_enabled_sort (category_id,enabled,sort_order),
  KEY idx_dish_templates_name (name),
  KEY idx_dish_templates_market (template_type,data_status,procurement_ready,enabled),
  CONSTRAINT chk_dish_template_data_status CHECK ((data_status in (_utf8mb4'READY',_utf8mb4'NEEDS_PURCHASE_DATA',_utf8mb4'NEEDS_PRICE',_utf8mb4'NEEDS_BOTH'))),
  CONSTRAINT chk_dish_template_source_type CHECK ((source_type in (_utf8mb4'COOK_LIKE_HOC',_utf8mb4'LOCAL_EXTENSION'))),
  CONSTRAINT chk_dish_template_type CHECK ((template_type in (_utf8mb4'DISH',_utf8mb4'COMPONENT'))),
  CONSTRAINT chk_template_image_rights CHECK ((((image_rights_status in (_utf8mb4'UNDECLARED',_utf8mb4'NONE')) and (image_url is null)) or ((image_rights_status = _utf8mb4'DECLARED') and (image_url is not null) and (image_source_url is not null) and (image_author is not null) and (image_license is not null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台菜品模板表';

CREATE TABLE dishes (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
  merchant_id bigint NOT NULL COMMENT '所属商户ID',
  category_id bigint NOT NULL COMMENT '菜品分类ID',
  name varchar(100) NOT NULL COMMENT '菜品名称',
  description varchar(255) DEFAULT NULL COMMENT '菜品简介',
  product_type varchar(20) NOT NULL DEFAULT 'NORMAL' COMMENT '商品类型：NORMAL/NOURISHMENT',
  nourishment_description varchar(1000) DEFAULT NULL COMMENT '滋补介绍（纯文本）',
  serving_advice varchar(1000) DEFAULT NULL COMMENT '食用建议（纯文本）',
  precautions varchar(1000) DEFAULT NULL COMMENT '注意事项（纯文本）',
  image_url varchar(500) DEFAULT NULL COMMENT '菜品图片地址',
  tags_json json DEFAULT NULL COMMENT '菜品标签JSON',
  base_price decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '基础售价',
  source_template_id bigint DEFAULT NULL COMMENT '来源平台模板菜品ID',
  featured_at datetime(6) DEFAULT NULL COMMENT '商户推荐时间',
  status varchar(20) NOT NULL DEFAULT 'active' COMMENT '菜品状态：active/inactive',
  deleted_at datetime(6) DEFAULT NULL COMMENT '逻辑删除时间',
  deleted_by bigint DEFAULT NULL COMMENT '执行逻辑删除的用户ID',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dishes_merchant_template (merchant_id,source_template_id),
  KEY idx_dishes_merchant_category_status (merchant_id,category_id,status),
  KEY idx_dishes_merchant_name (merchant_id,name),
  KEY idx_dishes_merchant_featured (merchant_id,featured_at),
  KEY idx_dishes_merchant_status_deleted (merchant_id,status,deleted_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户菜品主表';

CREATE TABLE email_verification_records (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '邮箱验证记录ID',
  user_id bigint NOT NULL COMMENT '用户ID',
  email varchar(190) NOT NULL COMMENT '待绑定邮箱',
  code_hash varchar(128) NOT NULL COMMENT '验证码摘要',
  status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '验证码状态',
  failed_attempts int NOT NULL DEFAULT '0' COMMENT '失败次数',
  expires_at datetime NOT NULL COMMENT '过期时间',
  used_at datetime DEFAULT NULL COMMENT '使用时间',
  request_ip varchar(64) DEFAULT NULL COMMENT '请求IP',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_email_verification_user (user_id,email,status,expires_at),
  KEY idx_email_verification_rate (email,request_ip,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮箱绑定验证码记录表';

CREATE TABLE families (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '家庭ID',
  merchant_id bigint NOT NULL COMMENT '所属商户ID',
  name varchar(100) NOT NULL COMMENT '家庭名称',
  note varchar(255) DEFAULT NULL COMMENT '家庭备注',
  contact_names_json json DEFAULT NULL COMMENT '家庭联系人摘要JSON数组',
  status varchar(20) NOT NULL DEFAULT 'active' COMMENT '家庭状态：active/inactive',
  delivery_enabled tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否支持配送',
  delivery_fee_default decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '默认配送费',
  delivery_fee_free tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否免配送费',
  featured_dish_id bigint DEFAULT NULL COMMENT '家庭首页显式推荐菜品ID',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_families_merchant_status (merchant_id,status),
  KEY idx_families_featured_dish (featured_dish_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭主表';

CREATE TABLE family_addresses (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  family_id bigint NOT NULL COMMENT '所属家庭ID',
  contact_name varchar(50) NOT NULL COMMENT '联系人姓名',
  contact_phone varchar(30) NOT NULL COMMENT '联系人电话',
  address_text varchar(255) NOT NULL COMMENT '详细地址',
  is_default tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否默认地址',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_family_addresses_family_default (family_id,is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭共享地址表';

CREATE TABLE family_applications (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '家庭创建申请ID',
  applicant_user_id bigint NOT NULL COMMENT '申请用户ID',
  merchant_id bigint DEFAULT NULL COMMENT '关联商户ID',
  family_name varchar(100) NOT NULL COMMENT '申请家庭名称',
  merchant_mode varchar(20) NOT NULL DEFAULT 'INVITATION' COMMENT '商户关联模式：INVITATION/JOINT_CREATE',
  merchant_invitation_id bigint DEFAULT NULL COMMENT '商户邀请码ID',
  proposed_merchant_name varchar(100) DEFAULT NULL COMMENT '拟创建商户名称',
  proposed_contact_name varchar(50) DEFAULT NULL COMMENT '拟创建商户联系人',
  proposed_contact_phone varchar(30) DEFAULT NULL COMMENT '拟创建商户联系电话',
  status varchar(20) NOT NULL DEFAULT 'pending' COMMENT '审核状态：pending/approved/rejected',
  review_remark varchar(255) DEFAULT NULL COMMENT '审核备注',
  reviewed_by bigint DEFAULT NULL COMMENT '审核人用户ID',
  reviewed_at datetime DEFAULT NULL COMMENT '审核时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_family_applications_status_time (status,created_at),
  KEY idx_family_applications_applicant (applicant_user_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭创建申请表';

CREATE TABLE family_cart_migration_sources (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '餐篮迁移源记录ID',
  batch_id bigint NOT NULL COMMENT '迁移批次ID',
  source_cart_id bigint NOT NULL COMMENT '源餐篮ID',
  target_cart_id bigint DEFAULT NULL COMMENT '目标餐篮ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  status varchar(30) NOT NULL COMMENT '迁移状态',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_family_cart_migration_source (batch_id,source_cart_id),
  KEY idx_family_cart_migration_sources_target (target_cart_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭餐篮迁移来源追踪';

CREATE TABLE family_invitation_codes (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '家庭安全邀请码ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  code_hash varchar(128) NOT NULL COMMENT '邀请码摘要',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  expires_at datetime NOT NULL COMMENT '过期时间',
  created_by bigint NOT NULL COMMENT '创建人用户ID',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  rotated_at datetime DEFAULT NULL COMMENT '轮换时间',
  active_family_id bigint GENERATED ALWAYS AS ((case when (status = _utf8mb4'ACTIVE') then family_id else NULL end)) STORED COMMENT '有效邀请码家庭唯一键',
  PRIMARY KEY (id),
  UNIQUE KEY uk_invitation_code_hash (code_hash),
  UNIQUE KEY uk_invitation_code_active_family (active_family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭安全邀请码表';

CREATE TABLE family_invitations (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '家庭邀请码记录ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  inviter_user_id bigint NOT NULL COMMENT '邀请人用户ID',
  code varchar(20) NOT NULL COMMENT '明文邀请码',
  status varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/used/expired',
  expire_at datetime NOT NULL COMMENT '过期时间',
  used_by_user_id bigint DEFAULT NULL COMMENT '使用人用户ID',
  used_at datetime DEFAULT NULL COMMENT '使用时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_family_invitations_code (code),
  KEY idx_family_invitations_family_status (family_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭明文邀请记录表';

CREATE TABLE family_membership_requests (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '家庭邀请或申请ID',
  request_type varchar(30) NOT NULL COMMENT 'DIRECT_INVITATION/CODE_APPLICATION',
  family_id bigint NOT NULL COMMENT '目标家庭ID',
  target_user_id bigint DEFAULT NULL COMMENT '定向邀请目标用户ID',
  applicant_user_id bigint DEFAULT NULL COMMENT '邀请码申请用户ID',
  invitation_code_id bigint DEFAULT NULL COMMENT '邀请码ID',
  status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED/REVOKED/INVALIDATED',
  created_by bigint NOT NULL COMMENT '创建人用户ID',
  reviewed_by bigint DEFAULT NULL COMMENT '处理人用户ID',
  reason varchar(500) DEFAULT NULL COMMENT '处理原因',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  handled_at datetime DEFAULT NULL COMMENT '处理时间',
  pending_direct_user bigint GENERATED ALWAYS AS ((case when ((status = _utf8mb4'PENDING') and (request_type = _utf8mb4'DIRECT_INVITATION')) then target_user_id else NULL end)) STORED COMMENT '待处理定向邀请用户唯一键',
  pending_code_user_family varchar(100) GENERATED ALWAYS AS ((case when ((status = _utf8mb4'PENDING') and (request_type = _utf8mb4'CODE_APPLICATION')) then concat(applicant_user_id,_utf8mb4':',family_id) else NULL end)) STORED COMMENT '待处理邀请码申请唯一键',
  PRIMARY KEY (id),
  UNIQUE KEY uk_pending_direct_user (pending_direct_user),
  UNIQUE KEY uk_pending_code_user_family (pending_code_user_family),
  KEY idx_membership_requests_family_status (family_id,status),
  KEY idx_membership_requests_target_status (target_user_id,status),
  KEY idx_membership_requests_applicant_status (applicant_user_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭邀请与加入申请表';

CREATE TABLE family_menu_items (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '家庭菜单项ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  dish_id bigint NOT NULL COMMENT '菜品ID',
  enabled tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  sort_order int NOT NULL DEFAULT '0' COMMENT '排序值',
  final_price decimal(10,2) DEFAULT NULL COMMENT '家庭专属价格',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_family_menu_family_dish (family_id,dish_id),
  KEY idx_family_menu_family_enabled_sort (family_id,enabled,sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭专属菜单配置表';

CREATE TABLE family_user_relations (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '家庭用户关系ID',
  user_id bigint NOT NULL COMMENT '用户ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  family_role varchar(20) NOT NULL COMMENT '家庭角色：OWNER/ADMIN/MEMBER',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/EXITED/REMOVED/DISSOLVED',
  join_source varchar(30) NOT NULL COMMENT '加入来源',
  invitation_id bigint DEFAULT NULL COMMENT '关联邀请或申请ID',
  joined_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  ended_at datetime DEFAULT NULL COMMENT '关系结束时间',
  created_by bigint DEFAULT NULL COMMENT '创建人用户ID',
  ended_by bigint DEFAULT NULL COMMENT '结束人用户ID',
  end_reason varchar(255) DEFAULT NULL COMMENT '结束原因',
  active_user_id bigint GENERATED ALWAYS AS ((case when (status = _utf8mb4'ACTIVE') then user_id else NULL end)) STORED COMMENT '有效关系用户唯一键',
  active_owner_family_id bigint GENERATED ALWAYS AS ((case when ((status = _utf8mb4'ACTIVE') and (family_role = _utf8mb4'OWNER')) then family_id else NULL end)) STORED COMMENT '有效户主家庭唯一键',
  PRIMARY KEY (id),
  UNIQUE KEY uk_family_relation_active_user (active_user_id),
  UNIQUE KEY uk_family_relation_active_owner (active_owner_family_id),
  KEY idx_family_relation_family_status (family_id,status),
  KEY idx_family_relation_user_history (user_id,joined_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭用户关系表';

CREATE TABLE family_wallet_cutover_state (
  scope_key varchar(100) NOT NULL COMMENT '规范化切换范围',
  maintenance_enabled tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否开启维护模式',
  cutover_epoch bigint NOT NULL DEFAULT '0' COMMENT '切换纪元',
  drain_epoch bigint NOT NULL DEFAULT '0' COMMENT '排空纪元',
  active_batch_id bigint DEFAULT NULL COMMENT '当前切换批次ID',
  state varchar(30) NOT NULL COMMENT '持久切换状态',
  updated_by varchar(128) DEFAULT NULL COMMENT '更新执行者',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (scope_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='持久切换与维护状态';

CREATE TABLE family_wallet_ledgers (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '家庭钱包流水ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  order_id bigint DEFAULT NULL COMMENT '关联订单ID',
  operator_user_id bigint DEFAULT NULL COMMENT '操作用户ID，系统操作时为空',
  scope_key varchar(100) NOT NULL COMMENT '规范化非空业务范围',
  business_type varchar(40) NOT NULL COMMENT '业务类型',
  business_key varchar(128) NOT NULL COMMENT '业务幂等键',
  amount decimal(18,2) NOT NULL COMMENT '变动金额绝对值',
  available_before decimal(18,2) NOT NULL COMMENT '变动前可用余额',
  available_after decimal(18,2) NOT NULL COMMENT '变动后可用余额',
  frozen_before decimal(18,2) NOT NULL COMMENT '变动前冻结余额',
  frozen_after decimal(18,2) NOT NULL COMMENT '变动后冻结余额',
  remark varchar(500) DEFAULT NULL COMMENT '流水备注',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_family_wallet_ledgers_business (business_type,business_key),
  KEY idx_family_wallet_ledgers_family_created (family_id,created_at),
  KEY idx_family_wallet_ledgers_family_order (family_id,order_id),
  KEY idx_family_wallet_ledgers_operator_created (operator_user_id,created_at),
  KEY idx_family_wallet_ledgers_scope (scope_key),
  CONSTRAINT ck_family_wallet_ledgers_amount CHECK ((amount >= 0)),
  CONSTRAINT ck_family_wallet_ledgers_available_after CHECK ((available_after >= 0)),
  CONSTRAINT ck_family_wallet_ledgers_available_before CHECK ((available_before >= 0)),
  CONSTRAINT ck_family_wallet_ledgers_frozen_after CHECK ((frozen_after >= 0)),
  CONSTRAINT ck_family_wallet_ledgers_frozen_before CHECK ((frozen_before >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭共享钱包流水';

CREATE TABLE family_wallet_migration_anomalies (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '迁移异常ID',
  batch_id bigint NOT NULL COMMENT '迁移批次ID',
  anomaly_type varchar(80) NOT NULL COMMENT '异常类型',
  source_type varchar(50) DEFAULT NULL COMMENT '源对象类型',
  source_id bigint DEFAULT NULL COMMENT '源对象ID',
  detail_json json NOT NULL COMMENT '异常或报告详情',
  resolved tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已处理',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_family_wallet_migration_anomalies_batch (batch_id,resolved,anomaly_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭钱包迁移异常与报告';

CREATE TABLE family_wallet_migration_batches (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '迁移批次ID',
  phase varchar(30) NOT NULL COMMENT 'PRECHECK/QUIESCE/EXECUTE/VERIFY/FINALIZE',
  cutover_epoch bigint NOT NULL DEFAULT '0' COMMENT '切换纪元',
  drain_epoch bigint NOT NULL DEFAULT '0' COMMENT '排空纪元',
  source_available_total decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '源可用余额守恒总额',
  source_frozen_total decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '源冻结余额守恒总额',
  target_available_total decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '目标可用余额守恒总额',
  target_frozen_total decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '目标冻结余额守恒总额',
  status varchar(30) NOT NULL COMMENT '批次状态',
  started_at datetime DEFAULT NULL COMMENT '开始时间',
  completed_at datetime DEFAULT NULL COMMENT '完成时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_family_wallet_migration_batches_status (status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭钱包迁移批次';

CREATE TABLE family_wallet_migration_families (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '迁移家庭进度ID',
  batch_id bigint NOT NULL COMMENT '迁移批次ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  status varchar(30) NOT NULL COMMENT 'PENDING/MIGRATED',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_family_wallet_migration_family (batch_id,family_id),
  KEY idx_family_wallet_migration_family_status (batch_id,status,family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭钱包迁移逐家庭进度';

CREATE TABLE family_wallet_migration_runner_lease (
  scope_key varchar(100) NOT NULL COMMENT '全局迁移运行器租约范围',
  owner_token varchar(128) NOT NULL COMMENT '不可猜测的运行器所有权令牌',
  batch_id bigint NOT NULL COMMENT '批次ID，初始预检使用0',
  drain_epoch bigint NOT NULL COMMENT '排空纪元，未排空使用0',
  mode varchar(30) NOT NULL COMMENT '当前运行模式',
  lease_expires_at datetime NOT NULL COMMENT '租约到期时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (scope_key),
  KEY idx_family_wallet_runner_lease_expiry (lease_expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭钱包迁移运行器排他租约';

CREATE TABLE family_wallet_migration_sources (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '钱包迁移源账户记录ID',
  batch_id bigint NOT NULL COMMENT '迁移批次ID',
  source_user_id bigint NOT NULL COMMENT '源成员钱包用户ID',
  family_id bigint NOT NULL COMMENT '目标家庭ID',
  source_available_amount decimal(18,2) NOT NULL COMMENT '源可用余额快照',
  source_frozen_amount decimal(18,2) NOT NULL COMMENT '源冻结余额快照',
  status varchar(30) NOT NULL COMMENT '迁移状态',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_family_wallet_migration_source (batch_id,source_user_id),
  KEY idx_family_wallet_migration_sources_family (batch_id,family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭钱包迁移源账户追踪';

CREATE TABLE family_wallet_order_holds (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '订单资金冻结记录ID',
  order_id bigint NOT NULL COMMENT '订单ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  initial_amount decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '初始冻结金额',
  additional_frozen_amount decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '追加冻结金额',
  remaining_frozen_amount decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '剩余冻结金额',
  captured_amount decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '已扣款金额',
  released_amount decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '已释放金额',
  refunded_amount decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '已退款金额',
  status varchar(30) NOT NULL COMMENT '冻结状态',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_family_wallet_order_holds_order (order_id),
  KEY idx_family_wallet_order_holds_family_status (family_id,status),
  CONSTRAINT ck_family_wallet_order_holds_additional CHECK ((additional_frozen_amount >= 0)),
  CONSTRAINT ck_family_wallet_order_holds_captured CHECK ((captured_amount >= 0)),
  CONSTRAINT ck_family_wallet_order_holds_equation CHECK (((initial_amount + additional_frozen_amount) = ((remaining_frozen_amount + captured_amount) + released_amount))),
  CONSTRAINT ck_family_wallet_order_holds_initial CHECK ((initial_amount >= 0)),
  CONSTRAINT ck_family_wallet_order_holds_refund CHECK ((refunded_amount <= captured_amount)),
  CONSTRAINT ck_family_wallet_order_holds_refunded CHECK ((refunded_amount >= 0)),
  CONSTRAINT ck_family_wallet_order_holds_released CHECK ((released_amount >= 0)),
  CONSTRAINT ck_family_wallet_order_holds_remaining CHECK ((remaining_frozen_amount >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭钱包订单冻结状态';

CREATE TABLE family_wallets (
  family_id bigint NOT NULL COMMENT '钱包所属家庭ID',
  available_amount decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '可用余额',
  frozen_amount decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '冻结余额',
  version bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (family_id),
  CONSTRAINT ck_family_wallets_available CHECK ((available_amount >= 0)),
  CONSTRAINT ck_family_wallets_frozen CHECK ((frozen_amount >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭共享钱包';

CREATE TABLE file_assets (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '文件资产ID',
  owner_type varchar(30) NOT NULL COMMENT '所属对象类型',
  owner_id bigint NOT NULL COMMENT '所属对象ID',
  file_type varchar(30) NOT NULL COMMENT '文件业务类型',
  storage_type varchar(30) NOT NULL COMMENT '存储类型：local/oss/cos',
  url varchar(500) NOT NULL COMMENT '访问地址',
  object_key varchar(255) DEFAULT NULL COMMENT '对象存储键或本地相对路径',
  mime_type varchar(100) DEFAULT NULL COMMENT 'MIME类型',
  size_bytes bigint DEFAULT NULL COMMENT '文件大小字节数',
  width int DEFAULT NULL COMMENT '图片宽度',
  height int DEFAULT NULL COMMENT '图片高度',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_file_assets_owner_type (owner_type,owner_id,file_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件资产表';

CREATE TABLE meal_slots (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '餐次ID',
  family_id bigint NOT NULL COMMENT '所属家庭ID',
  name varchar(50) NOT NULL COMMENT '餐次名称',
  display_time varchar(20) DEFAULT NULL COMMENT '展示时间',
  sort_order int NOT NULL DEFAULT '0' COMMENT '排序值',
  enabled tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_meal_slots_family_name (family_id,name),
  KEY idx_meal_slots_family_enabled_sort (family_id,enabled,sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭餐次配置表';

CREATE TABLE member_wallets (
  user_id bigint NOT NULL COMMENT '钱包所属用户ID',
  balance_amount decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '可用余额',
  frozen_amount decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '冻结金额',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户钱包账户表';

CREATE TABLE merchant_applications (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '商户申请ID',
  user_id bigint NOT NULL COMMENT '申请用户ID',
  merchant_name varchar(100) NOT NULL COMMENT '申请商户名称',
  status varchar(20) NOT NULL DEFAULT 'pending' COMMENT '申请状态：pending/approved/rejected',
  review_remark varchar(255) DEFAULT NULL COMMENT '审核备注',
  reviewed_by bigint DEFAULT NULL COMMENT '审核人用户ID',
  reviewed_at datetime DEFAULT NULL COMMENT '审核时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_merchant_applications_user_status (user_id,status),
  KEY idx_merchant_applications_status_time (status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户入驻申请表';

CREATE TABLE merchant_ingredients (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '商户食材字典ID',
  merchant_id bigint NOT NULL COMMENT '所属商户ID',
  name varchar(100) NOT NULL COMMENT '食材名称',
  category varchar(50) NOT NULL COMMENT '食材分类',
  unit varchar(20) NOT NULL COMMENT '默认单位',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_merchant_ingredients_merchant_name (merchant_id,name),
  KEY idx_merchant_ingredients_merchant_category (merchant_id,category,name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户食材字典表';

CREATE TABLE merchant_invitation_codes (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '商户邀请码ID',
  merchant_id bigint NOT NULL COMMENT '商户ID',
  code_hash varchar(64) NOT NULL COMMENT '邀请码摘要',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '邀请码状态',
  expires_at datetime NOT NULL COMMENT '过期时间',
  created_by bigint NOT NULL COMMENT '创建人用户ID',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  disabled_at datetime DEFAULT NULL COMMENT '失效时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_merchant_invitation_hash (code_hash),
  KEY idx_merchant_invitation_active (merchant_id,status,expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户邀请码表';

CREATE TABLE merchant_user_relations (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '商户用户关系ID',
  user_id bigint NOT NULL COMMENT '用户ID',
  merchant_id bigint NOT NULL COMMENT '商户ID',
  merchant_role varchar(40) NOT NULL COMMENT '商户角色',
  permission_scopes json DEFAULT NULL COMMENT '商户权限范围JSON',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '关系状态',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_merchant_user_role (merchant_id,user_id,merchant_role),
  KEY idx_merchant_user_user_status (user_id,status),
  KEY idx_merchant_user_merchant_status (merchant_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户用户关系表';

CREATE TABLE merchants (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '商户ID',
  name varchar(100) NOT NULL COMMENT '商户名称',
  status varchar(20) NOT NULL DEFAULT 'pending' COMMENT '商户状态：pending/active/rejected/inactive',
  contact_name varchar(50) DEFAULT NULL COMMENT '联系人姓名',
  contact_phone varchar(30) DEFAULT NULL COMMENT '联系人手机号',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_merchants_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户主表';

CREATE TABLE notification_dispatches (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '通知下发记录ID',
  notification_id bigint NOT NULL COMMENT '通知ID',
  channel varchar(20) NOT NULL COMMENT '发送渠道：wechat/sms/email/system',
  target varchar(100) DEFAULT NULL COMMENT '发送目标',
  send_status varchar(20) NOT NULL DEFAULT 'pending' COMMENT '发送状态：pending/success/failed',
  provider_message_id varchar(100) DEFAULT NULL COMMENT '渠道消息ID',
  fail_reason varchar(255) DEFAULT NULL COMMENT '失败原因',
  sent_at datetime DEFAULT NULL COMMENT '发送时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_notification_dispatches_notification_channel (notification_id,channel,send_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知下发记录表';

CREATE TABLE notifications (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  receiver_type varchar(20) NOT NULL COMMENT '接收方类型：user/merchant/platform',
  receiver_id bigint NOT NULL COMMENT '接收方业务ID',
  receiver_scope varchar(20) NOT NULL COMMENT '接收域：user/family/merchant/platform',
  category varchar(30) NOT NULL COMMENT '通知类别',
  title varchar(100) NOT NULL COMMENT '通知标题',
  content text COMMENT '通知正文',
  read_at datetime DEFAULT NULL COMMENT '已读时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_notifications_receiver_read (receiver_type,receiver_id,read_at),
  KEY idx_notifications_scope_receiver_read (receiver_scope,receiver_id,read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知主表';

CREATE TABLE order_delivery_snapshots (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '配送快照ID',
  order_id bigint NOT NULL COMMENT '所属订单ID',
  contact_name varchar(50) DEFAULT NULL COMMENT '配送联系人',
  contact_phone varchar(30) DEFAULT NULL COMMENT '配送联系电话',
  address_text varchar(255) DEFAULT NULL COMMENT '配送地址快照',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_delivery_snapshots_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单配送地址快照表';

CREATE TABLE order_item_selections (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '订单项成员选择快照ID',
  order_item_id bigint NOT NULL COMMENT '订单项ID',
  user_id bigint NOT NULL COMMENT '选择成员用户ID',
  quantity int NOT NULL COMMENT '成员选择份数',
  member_name_snapshot varchar(100) NOT NULL COMMENT '成员名称快照',
  item_remark varchar(255) DEFAULT NULL COMMENT '成员单品备注快照',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_item_selections_item_user (order_item_id,user_id),
  KEY idx_order_item_selections_user (user_id),
  CONSTRAINT ck_order_item_selections_quantity CHECK ((quantity > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单项成员选择快照表';

CREATE TABLE order_items (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
  order_id bigint NOT NULL COMMENT '所属订单ID',
  dish_id bigint NOT NULL COMMENT '菜品ID',
  owner_user_id bigint DEFAULT NULL COMMENT '点菜用户ID（兼容旧流程）',
  dish_name_snapshot varchar(100) DEFAULT NULL COMMENT '菜品名称快照',
  price decimal(10,2) NOT NULL COMMENT '菜品单价快照',
  quantity int NOT NULL COMMENT '份数',
  amount decimal(10,2) NOT NULL COMMENT '订单项金额',
  item_remark varchar(255) DEFAULT NULL COMMENT '订单项备注',
  PRIMARY KEY (id),
  KEY idx_order_items_order_user (order_id,owner_user_id),
  KEY idx_order_items_dish (dish_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单菜品明细表';

CREATE TABLE order_member_charges (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '订单用户费用分摊ID',
  order_id bigint NOT NULL COMMENT '所属订单ID',
  user_id bigint NOT NULL COMMENT '费用所属用户ID',
  dish_amount decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '菜品金额',
  delivery_fee_amount decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '配送费金额',
  total_amount decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '应付总额',
  frozen_amount decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '已冻结金额',
  settled_amount decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '已结算金额',
  released_amount decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '已释放金额',
  status varchar(20) NOT NULL COMMENT '分摊状态',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_user_charges (order_id,user_id),
  KEY idx_order_charges_user_status (user_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单用户分摊与结算表';

CREATE TABLE orders (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  merchant_id bigint NOT NULL COMMENT '商户ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  submitter_user_id bigint NOT NULL COMMENT '提交订单用户ID',
  source_cart_id bigint DEFAULT NULL COMMENT '来源家庭餐篮ID',
  meal_slot_id bigint DEFAULT NULL COMMENT '餐次ID（兼容旧流程）',
  service_date date DEFAULT NULL COMMENT '服务日期（兼容旧流程）',
  expected_meal_time datetime DEFAULT NULL COMMENT '期望用餐时间',
  delivery_mode varchar(20) NOT NULL COMMENT '配送方式：PICKUP/DELIVERY',
  delivery_fee decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '配送费',
  delivery_fee_payer_user_id bigint DEFAULT NULL COMMENT '配送费承担用户ID（兼容旧流程）',
  status varchar(20) NOT NULL COMMENT '订单状态',
  total_amount decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '订单总金额',
  remark varchar(500) DEFAULT NULL COMMENT '下单备注',
  cancel_reason varchar(500) DEFAULT NULL COMMENT '取消原因',
  cancelled_by_type varchar(20) DEFAULT NULL COMMENT '取消方类型',
  cancelled_by_id bigint DEFAULT NULL COMMENT '取消操作用户ID',
  cancelled_at datetime DEFAULT NULL COMMENT '取消时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_orders_source_cart (source_cart_id),
  KEY idx_orders_merchant_status_date (merchant_id,status,service_date),
  KEY idx_orders_family_date_status (family_id,service_date,status),
  KEY idx_orders_submitter_date (submitter_user_id,service_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭点餐订单表';

CREATE TABLE password_reset_records (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '密码重置记录ID',
  user_id bigint DEFAULT NULL COMMENT '用户ID',
  email varchar(190) NOT NULL COMMENT '目标邮箱',
  code_hash varchar(128) NOT NULL COMMENT '验证码摘要',
  status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '验证码状态',
  failed_attempts int NOT NULL DEFAULT '0' COMMENT '失败次数',
  expires_at datetime NOT NULL COMMENT '过期时间',
  used_at datetime DEFAULT NULL COMMENT '使用时间',
  request_ip varchar(64) DEFAULT NULL COMMENT '请求IP',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_password_reset_email (email,status,expires_at),
  KEY idx_password_reset_rate (request_ip,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码找回记录表';

CREATE TABLE purchase_list_items (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '采购项ID',
  purchase_list_id bigint NOT NULL COMMENT '所属采购单ID',
  ingredient_name varchar(255) NOT NULL COMMENT '采购食材名称',
  quantity decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '采购数量',
  unit varchar(20) NOT NULL COMMENT '单位',
  source_json json DEFAULT NULL COMMENT '来源明细JSON',
  source_status varchar(20) NOT NULL COMMENT '来源状态：ESTIMATED/CONFIRMED',
  checked tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已完成',
  temporary tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否临时补录',
  remark varchar(255) DEFAULT NULL COMMENT '备注',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_purchase_items_list_checked (purchase_list_id,checked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购单明细表';

CREATE TABLE purchase_lists (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '采购单ID',
  merchant_id bigint NOT NULL COMMENT '商户ID',
  service_date date NOT NULL COMMENT '服务日期',
  meal_slot_id bigint DEFAULT NULL COMMENT '餐次ID，空表示全天汇总',
  status varchar(20) NOT NULL DEFAULT 'draft' COMMENT '采购单状态：draft/confirmed/done',
  generated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_purchase_lists_merchant_date_slot (merchant_id,service_date,meal_slot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购汇总单表';

CREATE TABLE security_audit_logs (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '安全审计日志ID',
  user_id bigint DEFAULT NULL COMMENT '操作用户ID',
  action varchar(80) NOT NULL COMMENT '安全操作',
  result varchar(20) NOT NULL COMMENT '操作结果',
  client_ip varchar(64) DEFAULT NULL COMMENT '客户端IP',
  detail varchar(500) DEFAULT NULL COMMENT '脱敏详情',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_security_audit_user_time (user_id,created_at),
  KEY idx_security_audit_action_time (action,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='安全审计日志表';

CREATE TABLE system_settings (
  id bigint NOT NULL COMMENT '固定主键，始终为1',
  site_name varchar(100) NOT NULL COMMENT '站点名称',
  site_logo_url varchar(500) DEFAULT NULL COMMENT '站点Logo地址',
  site_logo_small_url varchar(500) DEFAULT NULL COMMENT '站点小尺寸Logo地址',
  site_logo_large_url varchar(500) DEFAULT NULL COMMENT '站点大尺寸Logo地址',
  site_favicon_url varchar(500) DEFAULT NULL COMMENT '站点浏览器图标地址',
  site_logo_small_size int DEFAULT NULL COMMENT '小尺寸Logo显示大小',
  site_logo_size int DEFAULT NULL COMMENT '默认Logo显示大小',
  site_logo_large_size int DEFAULT NULL COMMENT '大尺寸Logo显示大小',
  dish_review_enabled tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否开启菜品审核',
  maintenance_enabled tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否开启全站维护',
  maintenance_message varchar(500) NOT NULL DEFAULT '系统维护中，请稍后再试' COMMENT '维护提示',
  mobile_binding_enabled tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否允许绑定手机号',
  email_binding_enabled tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否允许绑定邮箱',
  wechat_binding_enabled tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否允许绑定微信',
  smtp_host varchar(255) DEFAULT NULL COMMENT 'SMTP服务器地址',
  smtp_port int DEFAULT NULL COMMENT 'SMTP端口',
  smtp_username varchar(255) DEFAULT NULL COMMENT 'SMTP用户名',
  smtp_password_ciphertext text COMMENT 'SMTP密码密文',
  smtp_tls_enabled tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用SMTP TLS',
  smtp_from varchar(255) DEFAULT NULL COMMENT '邮件发件人',
  updated_by bigint DEFAULT NULL COMMENT '最后操作管理员用户ID',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';

CREATE TABLE temp_purchase_items (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '临时采购项ID',
  merchant_id bigint NOT NULL COMMENT '商户ID',
  service_date date NOT NULL COMMENT '服务日期',
  meal_slot_id bigint DEFAULT NULL COMMENT '餐次ID',
  family_id bigint DEFAULT NULL COMMENT '关联家庭ID',
  ingredient_name varchar(255) NOT NULL COMMENT '临时采购食材名称',
  quantity decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '采购数量',
  unit varchar(20) NOT NULL COMMENT '单位',
  remark varchar(255) DEFAULT NULL COMMENT '备注',
  checked tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已完成',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_temp_purchase_items_merchant_date (merchant_id,service_date),
  KEY idx_temp_purchase_items_merchant_family (merchant_id,family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户临时采购项表';

CREATE TABLE user_role_relations (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '平台角色关系ID',
  user_id bigint NOT NULL COMMENT '用户ID',
  role_code varchar(40) NOT NULL COMMENT '平台角色编码',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '角色状态',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_platform_role (user_id,role_code),
  KEY idx_user_role_status (user_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户平台角色关系表';

CREATE TABLE user_sessions (
  id varchar(64) NOT NULL COMMENT '会话ID',
  user_id bigint NOT NULL COMMENT '用户ID',
  token_hash varchar(128) NOT NULL COMMENT '令牌摘要',
  token_version int NOT NULL DEFAULT '1' COMMENT '令牌版本',
  device_info varchar(255) DEFAULT NULL COMMENT '设备信息',
  client_ip varchar(64) DEFAULT NULL COMMENT '客户端IP',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态',
  expires_at datetime NOT NULL COMMENT '过期时间',
  revoked_at datetime DEFAULT NULL COMMENT '撤销时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_user_sessions_user_status (user_id,status),
  KEY idx_user_sessions_expire (status,expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户登录会话表';

CREATE TABLE users (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username varchar(64) NOT NULL COMMENT '登录用户名',
  username_changed tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已使用用户名修改机会',
  password_hash varchar(255) NOT NULL COMMENT '密码摘要',
  password_algorithm varchar(20) NOT NULL DEFAULT 'BCRYPT' COMMENT '密码算法',
  credential_status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '凭证状态：ACTIVE/CLAIM_REQUIRED',
  email varchar(190) DEFAULT NULL COMMENT '已绑定邮箱',
  email_verified tinyint(1) NOT NULL DEFAULT '0' COMMENT '邮箱是否已验证',
  wechat_open_id varchar(128) DEFAULT NULL COMMENT '微信OpenID',
  nickname varchar(80) NOT NULL COMMENT '用户昵称',
  avatar_url varchar(500) DEFAULT NULL COMMENT '头像地址',
  mobile varchar(32) DEFAULT NULL COMMENT '联系电话',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态：ACTIVE/FROZEN/CANCELLING/CANCELLED',
  password_changed_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '密码最近修改时间',
  last_login_at datetime DEFAULT NULL COMMENT '最近登录时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_wechat_open_id (wechat_open_id),
  KEY idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一用户表';

CREATE TABLE wallet_ledgers (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '钱包流水ID',
  user_id bigint NOT NULL COMMENT '流水所属用户ID',
  order_id bigint DEFAULT NULL COMMENT '关联订单ID',
  type varchar(30) NOT NULL COMMENT '流水类型',
  amount decimal(10,2) NOT NULL COMMENT '变动金额',
  balance_before decimal(10,2) NOT NULL COMMENT '变动前可用余额',
  balance_after decimal(10,2) NOT NULL COMMENT '变动后可用余额',
  frozen_before decimal(10,2) NOT NULL COMMENT '变动前冻结金额',
  frozen_after decimal(10,2) NOT NULL COMMENT '变动后冻结金额',
  remark varchar(255) DEFAULT NULL COMMENT '备注',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_wallet_ledgers_user_created (user_id,created_at),
  KEY idx_wallet_ledgers_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户钱包流水表';

CREATE TABLE wechat_authorization_records (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '微信授权记录ID',
  credential_hash varchar(128) NOT NULL COMMENT '短期凭证摘要',
  open_id_ciphertext varchar(500) NOT NULL COMMENT '加密微信OpenID',
  purpose varchar(30) NOT NULL COMMENT '凭证用途',
  status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  expires_at datetime NOT NULL COMMENT '过期时间',
  consumed_at datetime DEFAULT NULL COMMENT '消费时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wechat_credential_hash (credential_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='微信短期授权记录表';
