-- Additive compatibility schema for family-shared carts and wallets.
-- Data movement and cutover are intentionally owned by the later migration runner.

ALTER TABLE carts
  MODIFY COLUMN user_id bigint NULL COMMENT '创建餐篮的用户ID（兼容旧流程）',
  MODIFY COLUMN meal_slot_id bigint NULL COMMENT '餐次ID（兼容旧流程）',
  MODIFY COLUMN service_date date NULL COMMENT '服务日期（兼容旧流程）',
  ADD COLUMN expected_meal_time datetime NULL COMMENT '期望用餐时间' AFTER service_date,
  ADD COLUMN version bigint NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER status;

ALTER TABLE orders
  MODIFY COLUMN meal_slot_id bigint NULL COMMENT '餐次ID（兼容旧流程）',
  MODIFY COLUMN service_date date NULL COMMENT '服务日期（兼容旧流程）',
  MODIFY COLUMN delivery_fee_payer_user_id bigint NULL COMMENT '配送费承担用户ID（兼容旧流程）',
  ADD COLUMN source_cart_id bigint NULL COMMENT '来源家庭餐篮ID' AFTER submitter_user_id,
  ADD COLUMN expected_meal_time datetime NULL COMMENT '期望用餐时间' AFTER service_date,
  ADD UNIQUE KEY uk_orders_source_cart (source_cart_id);

ALTER TABLE order_items
  MODIFY COLUMN owner_user_id bigint NULL COMMENT '点菜用户ID（兼容旧流程）';

CREATE TABLE cart_item_selections (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '餐篮项成员选择ID',
  cart_item_id bigint NOT NULL COMMENT '餐篮项ID',
  user_id bigint NOT NULL COMMENT '选择成员用户ID',
  quantity int NOT NULL COMMENT '成员选择份数',
  item_remark varchar(255) NULL COMMENT '成员单品备注',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_cart_item_selections_item_user (cart_item_id,user_id),
  KEY idx_cart_item_selections_user (user_id),
  CONSTRAINT ck_cart_item_selections_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='餐篮项成员选择表';

CREATE TABLE order_item_selections (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '订单项成员选择快照ID',
  order_item_id bigint NOT NULL COMMENT '订单项ID',
  user_id bigint NOT NULL COMMENT '选择成员用户ID',
  quantity int NOT NULL COMMENT '成员选择份数',
  member_name_snapshot varchar(100) NOT NULL COMMENT '成员名称快照',
  item_remark varchar(255) NULL COMMENT '成员单品备注快照',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_order_item_selections_item_user (order_item_id,user_id),
  KEY idx_order_item_selections_user (user_id),
  CONSTRAINT ck_order_item_selections_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单项成员选择快照表';

CREATE TABLE family_wallets (
  family_id bigint NOT NULL COMMENT '钱包所属家庭ID',
  available_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
  frozen_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '冻结余额',
  version bigint NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (family_id),
  CONSTRAINT ck_family_wallets_available CHECK (available_amount >= 0),
  CONSTRAINT ck_family_wallets_frozen CHECK (frozen_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭共享钱包';

CREATE TABLE family_wallet_ledgers (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '家庭钱包流水ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  scope_key varchar(100) NOT NULL COMMENT '规范化非空业务范围',
  business_type varchar(40) NOT NULL COMMENT '业务类型',
  business_key varchar(128) NOT NULL COMMENT '业务幂等键',
  amount decimal(18,2) NOT NULL COMMENT '变动金额绝对值',
  available_before decimal(18,2) NOT NULL COMMENT '变动前可用余额',
  available_after decimal(18,2) NOT NULL COMMENT '变动后可用余额',
  frozen_before decimal(18,2) NOT NULL COMMENT '变动前冻结余额',
  frozen_after decimal(18,2) NOT NULL COMMENT '变动后冻结余额',
  remark varchar(500) NULL COMMENT '流水备注',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_family_wallet_ledgers_business (business_type,business_key),
  KEY idx_family_wallet_ledgers_family_created (family_id,created_at),
  KEY idx_family_wallet_ledgers_scope (scope_key),
  CONSTRAINT ck_family_wallet_ledgers_amount CHECK (amount >= 0),
  CONSTRAINT ck_family_wallet_ledgers_available_before CHECK (available_before >= 0),
  CONSTRAINT ck_family_wallet_ledgers_available_after CHECK (available_after >= 0),
  CONSTRAINT ck_family_wallet_ledgers_frozen_before CHECK (frozen_before >= 0),
  CONSTRAINT ck_family_wallet_ledgers_frozen_after CHECK (frozen_after >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭共享钱包流水';

CREATE TABLE family_wallet_order_holds (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '订单资金冻结记录ID',
  order_id bigint NOT NULL COMMENT '订单ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  initial_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '初始冻结金额',
  additional_frozen_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '追加冻结金额',
  remaining_frozen_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '剩余冻结金额',
  captured_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '已扣款金额',
  released_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '已释放金额',
  refunded_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '已退款金额',
  status varchar(30) NOT NULL COMMENT '冻结状态',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_family_wallet_order_holds_order (order_id),
  KEY idx_family_wallet_order_holds_family_status (family_id,status),
  CONSTRAINT ck_family_wallet_order_holds_initial CHECK (initial_amount >= 0),
  CONSTRAINT ck_family_wallet_order_holds_additional CHECK (additional_frozen_amount >= 0),
  CONSTRAINT ck_family_wallet_order_holds_remaining CHECK (remaining_frozen_amount >= 0),
  CONSTRAINT ck_family_wallet_order_holds_captured CHECK (captured_amount >= 0),
  CONSTRAINT ck_family_wallet_order_holds_released CHECK (released_amount >= 0),
  CONSTRAINT ck_family_wallet_order_holds_refunded CHECK (refunded_amount >= 0),
  CONSTRAINT ck_family_wallet_order_holds_equation CHECK (initial_amount + additional_frozen_amount = remaining_frozen_amount + captured_amount + released_amount),
  CONSTRAINT ck_family_wallet_order_holds_refund CHECK (refunded_amount <= captured_amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭钱包订单冻结状态';

CREATE TABLE command_idempotency (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '命令幂等记录ID',
  actor_user_id bigint NOT NULL COMMENT '操作用户ID',
  family_id bigint NOT NULL COMMENT '规范化非空家庭范围ID',
  operation varchar(80) NOT NULL COMMENT '操作名称',
  request_id varchar(128) NOT NULL COMMENT '客户端请求ID',
  payload_hash varchar(128) NOT NULL COMMENT '请求载荷摘要',
  state varchar(30) NOT NULL COMMENT '处理状态',
  result_resource_type varchar(80) NULL COMMENT '结果资源类型',
  result_resource_id bigint NULL COMMENT '结果资源ID',
  result_body json NULL COMMENT '结果响应体',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_command_idempotency_scope (actor_user_id,family_id,operation,request_id),
  KEY idx_command_idempotency_state_updated (state,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='持久命令幂等记录';

CREATE TABLE family_wallet_migration_batches (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '迁移批次ID',
  phase varchar(30) NOT NULL COMMENT 'PRECHECK/QUIESCE/EXECUTE/VERIFY/FINALIZE',
  cutover_epoch bigint NOT NULL DEFAULT 0 COMMENT '切换纪元',
  drain_epoch bigint NOT NULL DEFAULT 0 COMMENT '排空纪元',
  source_available_total decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '源可用余额守恒总额',
  source_frozen_total decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '源冻结余额守恒总额',
  target_available_total decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '目标可用余额守恒总额',
  target_frozen_total decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '目标冻结余额守恒总额',
  status varchar(30) NOT NULL COMMENT '批次状态',
  started_at datetime NULL COMMENT '开始时间',
  completed_at datetime NULL COMMENT '完成时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_family_wallet_migration_batches_status (status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭钱包迁移批次';

CREATE TABLE family_wallet_migration_sources (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '钱包迁移源账户记录ID',
  batch_id bigint NOT NULL COMMENT '迁移批次ID',
  source_user_id bigint NOT NULL COMMENT '源成员钱包用户ID',
  family_id bigint NOT NULL COMMENT '目标家庭ID',
  source_available_amount decimal(18,2) NOT NULL COMMENT '源可用余额快照',
  source_frozen_amount decimal(18,2) NOT NULL COMMENT '源冻结余额快照',
  status varchar(30) NOT NULL COMMENT '迁移状态',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_family_wallet_migration_source (batch_id,source_user_id),
  KEY idx_family_wallet_migration_sources_family (batch_id,family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭钱包迁移源账户追踪';

CREATE TABLE family_cart_migration_sources (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '餐篮迁移源记录ID',
  batch_id bigint NOT NULL COMMENT '迁移批次ID',
  source_cart_id bigint NOT NULL COMMENT '源餐篮ID',
  target_cart_id bigint NULL COMMENT '目标餐篮ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  status varchar(30) NOT NULL COMMENT '迁移状态',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_family_cart_migration_source (batch_id,source_cart_id),
  KEY idx_family_cart_migration_sources_target (target_cart_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭餐篮迁移来源追踪';

CREATE TABLE family_wallet_migration_anomalies (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '迁移异常ID',
  batch_id bigint NOT NULL COMMENT '迁移批次ID',
  anomaly_type varchar(80) NOT NULL COMMENT '异常类型',
  source_type varchar(50) NULL COMMENT '源对象类型',
  source_id bigint NULL COMMENT '源对象ID',
  detail_json json NOT NULL COMMENT '异常或报告详情',
  resolved tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已处理',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_family_wallet_migration_anomalies_batch (batch_id,resolved,anomaly_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭钱包迁移异常与报告';

CREATE TABLE family_wallet_cutover_state (
  scope_key varchar(100) NOT NULL COMMENT '规范化切换范围',
  maintenance_enabled tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否开启维护模式',
  cutover_epoch bigint NOT NULL DEFAULT 0 COMMENT '切换纪元',
  drain_epoch bigint NOT NULL DEFAULT 0 COMMENT '排空纪元',
  state varchar(30) NOT NULL COMMENT '持久切换状态',
  updated_by varchar(128) NULL COMMENT '更新执行者',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (scope_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='持久切换与维护状态';

CREATE TABLE application_instance_leases (
  instance_id varchar(128) NOT NULL COMMENT '应用实例ID',
  build_version varchar(128) NOT NULL COMMENT '应用构建版本',
  lease_owner varchar(128) NOT NULL COMMENT '租约持有者',
  heartbeat_at datetime NOT NULL COMMENT '最近心跳时间',
  lease_expires_at datetime NOT NULL COMMENT '租约过期时间',
  started_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '实例启动时间',
  metadata_json json NULL COMMENT '实例元数据',
  PRIMARY KEY (instance_id),
  KEY idx_application_instance_leases_expiry (lease_expires_at),
  KEY idx_application_instance_leases_build (build_version,heartbeat_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用实例租约与构建心跳';
