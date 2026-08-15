-- 菜品、餐篮、订单、钱包和采购结构。
-- 所有关联ID均为逻辑引用，不创建数据库外键。

CREATE TABLE dish_categories (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '菜品分类ID',
  merchant_id bigint NOT NULL COMMENT '所属商户ID',
  name varchar(50) NOT NULL COMMENT '分类名称',
  sort_order int NOT NULL DEFAULT 0 COMMENT '排序值',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_dish_categories_merchant_name (merchant_id,name),
  KEY idx_dish_categories_merchant_enabled_sort (merchant_id,enabled,sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户菜品分类表';

CREATE TABLE dishes (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '菜品ID',
  merchant_id bigint NOT NULL COMMENT '所属商户ID',
  category_id bigint NOT NULL COMMENT '菜品分类ID',
  name varchar(100) NOT NULL COMMENT '菜品名称',
  description varchar(255) NULL COMMENT '菜品简介',
  image_url varchar(500) NULL COMMENT '菜品图片地址',
  tags_json json NULL COMMENT '菜品标签JSON',
  base_price decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '基础售价',
  source_template_id bigint NULL COMMENT '来源平台模板菜品ID',
  status varchar(20) NOT NULL DEFAULT 'active' COMMENT '菜品状态：active/inactive',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_dishes_merchant_category_status (merchant_id,category_id,status),
  KEY idx_dishes_merchant_name (merchant_id,name),
  UNIQUE KEY uk_dishes_merchant_template (merchant_id,source_template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户菜品主表';

CREATE TABLE dish_ingredients (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '菜品食材记录ID',
  dish_id bigint NOT NULL COMMENT '所属菜品ID',
  ingredient_name varchar(100) NOT NULL COMMENT '食材名称',
  quantity decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '计算数量',
  unit varchar(20) NOT NULL COMMENT '单位',
  calc_type varchar(20) NOT NULL COMMENT '计算方式：FIXED/PER_PERSON/NO_PURCHASE',
  KEY idx_dish_ingredients_dish (dish_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品食材明细表';

CREATE TABLE dish_cooking_steps (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '制作步骤ID',
  dish_id bigint NOT NULL COMMENT '所属菜品ID',
  step_no int NOT NULL COMMENT '步骤序号',
  title varchar(100) NULL COMMENT '步骤标题',
  content text NULL COMMENT '步骤内容',
  UNIQUE KEY uk_dish_cooking_steps_dish_step (dish_id,step_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品制作步骤表';

CREATE TABLE merchant_ingredients (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '商户食材字典ID',
  merchant_id bigint NOT NULL COMMENT '所属商户ID',
  name varchar(100) NOT NULL COMMENT '食材名称',
  category varchar(50) NOT NULL COMMENT '食材分类',
  unit varchar(20) NOT NULL COMMENT '默认单位',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_merchant_ingredients_merchant_name (merchant_id,name),
  KEY idx_merchant_ingredients_merchant_category (merchant_id,category,name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户食材字典表';

CREATE TABLE family_menu_items (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '家庭菜单项ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  dish_id bigint NOT NULL COMMENT '菜品ID',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  sort_order int NOT NULL DEFAULT 0 COMMENT '排序值',
  final_price decimal(10,2) NULL COMMENT '家庭专属价格',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_family_menu_family_dish (family_id,dish_id),
  KEY idx_family_menu_family_enabled_sort (family_id,enabled,sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭专属菜单配置表';

CREATE TABLE dish_review_submissions (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '菜品审核提交ID',
  merchant_id bigint NOT NULL COMMENT '商户ID',
  target_dish_id bigint NULL COMMENT '目标正式菜品ID',
  submission_type varchar(20) NOT NULL COMMENT '提交类型：CREATE/UPDATE',
  snapshot_json json NOT NULL COMMENT '菜品完整快照',
  status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/WITHDRAWN',
  submitted_by bigint NOT NULL COMMENT '提交用户ID',
  reviewed_by bigint NULL COMMENT '审核管理员用户ID',
  review_reason varchar(500) NULL COMMENT '审核原因或备注',
  submitted_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  reviewed_at datetime NULL COMMENT '审核时间',
  pending_target_key varchar(100) GENERATED ALWAYS AS (CASE WHEN status='PENDING' AND target_dish_id IS NOT NULL THEN CONCAT(merchant_id,':',target_dish_id) ELSE NULL END) STORED COMMENT '待审核目标唯一键',
  UNIQUE KEY uk_dish_review_pending_target (pending_target_key),
  KEY idx_dish_review_status_time (status,submitted_at),
  KEY idx_dish_review_merchant (merchant_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品审核提交表';

CREATE TABLE carts (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '餐篮ID',
  merchant_id bigint NOT NULL COMMENT '商户ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  user_id bigint NOT NULL COMMENT '创建餐篮的用户ID',
  meal_slot_id bigint NOT NULL COMMENT '餐次ID',
  service_date date NOT NULL COMMENT '服务日期',
  remark varchar(500) NULL COMMENT '整篮备注',
  status varchar(20) NOT NULL DEFAULT 'active' COMMENT '餐篮状态：active/submitted/archived',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  active_cart_key varchar(160) GENERATED ALWAYS AS (CASE WHEN status='active' THEN CONCAT(family_id,':',user_id,':',meal_slot_id,':',service_date) ELSE NULL END) STORED COMMENT '有效餐篮唯一键',
  UNIQUE KEY uk_carts_active_cart (active_cart_key),
  KEY idx_carts_family_user_date_slot (family_id,user_id,service_date,meal_slot_id,status),
  KEY idx_carts_merchant_date (merchant_id,service_date,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户当日餐篮表';

CREATE TABLE cart_items (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '餐篮项ID',
  cart_id bigint NOT NULL COMMENT '所属餐篮ID',
  dish_id bigint NOT NULL COMMENT '菜品ID',
  dish_name_snapshot varchar(100) NULL COMMENT '菜品名称快照',
  price decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '单价快照',
  quantity int NOT NULL COMMENT '份数',
  item_remark varchar(255) NULL COMMENT '单品备注',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_cart_items_cart_dish (cart_id,dish_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='餐篮菜品明细表';

CREATE TABLE orders (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
  merchant_id bigint NOT NULL COMMENT '商户ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  submitter_user_id bigint NOT NULL COMMENT '提交订单用户ID',
  meal_slot_id bigint NOT NULL COMMENT '餐次ID',
  service_date date NOT NULL COMMENT '服务日期',
  delivery_mode varchar(20) NOT NULL COMMENT '配送方式：PICKUP/DELIVERY',
  delivery_fee decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '配送费',
  delivery_fee_payer_user_id bigint NOT NULL COMMENT '配送费承担用户ID',
  status varchar(20) NOT NULL COMMENT '订单状态',
  total_amount decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
  remark varchar(500) NULL COMMENT '下单备注',
  cancel_reason varchar(500) NULL COMMENT '取消原因',
  cancelled_by_type varchar(20) NULL COMMENT '取消方类型',
  cancelled_by_id bigint NULL COMMENT '取消操作用户ID',
  cancelled_at datetime NULL COMMENT '取消时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_orders_merchant_status_date (merchant_id,status,service_date),
  KEY idx_orders_family_date_status (family_id,service_date,status),
  KEY idx_orders_submitter_date (submitter_user_id,service_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭点餐订单表';

CREATE TABLE order_items (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '订单项ID',
  order_id bigint NOT NULL COMMENT '所属订单ID',
  dish_id bigint NOT NULL COMMENT '菜品ID',
  owner_user_id bigint NOT NULL COMMENT '点菜用户ID',
  dish_name_snapshot varchar(100) NULL COMMENT '菜品名称快照',
  price decimal(10,2) NOT NULL COMMENT '菜品单价快照',
  quantity int NOT NULL COMMENT '份数',
  amount decimal(10,2) NOT NULL COMMENT '订单项金额',
  item_remark varchar(255) NULL COMMENT '订单项备注',
  KEY idx_order_items_order_user (order_id,owner_user_id),
  KEY idx_order_items_dish (dish_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单菜品明细表';

CREATE TABLE order_delivery_snapshots (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '配送快照ID',
  order_id bigint NOT NULL COMMENT '所属订单ID',
  contact_name varchar(50) NULL COMMENT '配送联系人',
  contact_phone varchar(30) NULL COMMENT '配送联系电话',
  address_text varchar(255) NULL COMMENT '配送地址快照',
  UNIQUE KEY uk_order_delivery_snapshots_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单配送地址快照表';

CREATE TABLE order_member_charges (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '订单用户费用分摊ID',
  order_id bigint NOT NULL COMMENT '所属订单ID',
  user_id bigint NOT NULL COMMENT '费用所属用户ID',
  dish_amount decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '菜品金额',
  delivery_fee_amount decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '配送费金额',
  total_amount decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '应付总额',
  frozen_amount decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '已冻结金额',
  settled_amount decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '已结算金额',
  released_amount decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '已释放金额',
  status varchar(20) NOT NULL COMMENT '分摊状态',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_order_user_charges (order_id,user_id),
  KEY idx_order_charges_user_status (user_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单用户分摊与结算表';

CREATE TABLE member_wallets (
  user_id bigint PRIMARY KEY COMMENT '钱包所属用户ID',
  balance_amount decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
  frozen_amount decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户钱包账户表';

CREATE TABLE wallet_ledgers (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '钱包流水ID',
  user_id bigint NOT NULL COMMENT '流水所属用户ID',
  order_id bigint NULL COMMENT '关联订单ID',
  type varchar(30) NOT NULL COMMENT '流水类型',
  amount decimal(10,2) NOT NULL COMMENT '变动金额',
  balance_before decimal(10,2) NOT NULL COMMENT '变动前可用余额',
  balance_after decimal(10,2) NOT NULL COMMENT '变动后可用余额',
  frozen_before decimal(10,2) NOT NULL COMMENT '变动前冻结金额',
  frozen_after decimal(10,2) NOT NULL COMMENT '变动后冻结金额',
  remark varchar(255) NULL COMMENT '备注',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_wallet_ledgers_user_created (user_id,created_at),
  KEY idx_wallet_ledgers_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户钱包流水表';

CREATE TABLE purchase_lists (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '采购单ID',
  merchant_id bigint NOT NULL COMMENT '商户ID',
  service_date date NOT NULL COMMENT '服务日期',
  meal_slot_id bigint NULL COMMENT '餐次ID，空表示全天汇总',
  status varchar(20) NOT NULL DEFAULT 'draft' COMMENT '采购单状态：draft/confirmed/done',
  generated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_purchase_lists_merchant_date_slot (merchant_id,service_date,meal_slot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购汇总单表';

CREATE TABLE purchase_list_items (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '采购项ID',
  purchase_list_id bigint NOT NULL COMMENT '所属采购单ID',
  ingredient_name varchar(100) NOT NULL COMMENT '食材名称',
  quantity decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '采购数量',
  unit varchar(20) NOT NULL COMMENT '单位',
  source_json json NULL COMMENT '来源明细JSON',
  source_status varchar(20) NOT NULL COMMENT '来源状态：ESTIMATED/CONFIRMED',
  checked tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已完成',
  temporary tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否临时补录',
  remark varchar(255) NULL COMMENT '备注',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_purchase_items_list_checked (purchase_list_id,checked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购单明细表';

CREATE TABLE temp_purchase_items (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '临时采购项ID',
  merchant_id bigint NOT NULL COMMENT '商户ID',
  service_date date NOT NULL COMMENT '服务日期',
  meal_slot_id bigint NULL COMMENT '餐次ID',
  family_id bigint NULL COMMENT '关联家庭ID',
  ingredient_name varchar(100) NOT NULL COMMENT '食材名称',
  quantity decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '采购数量',
  unit varchar(20) NOT NULL COMMENT '单位',
  remark varchar(255) NULL COMMENT '备注',
  checked tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已完成',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_temp_purchase_items_merchant_date (merchant_id,service_date),
  KEY idx_temp_purchase_items_merchant_family (merchant_id,family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户临时采购项表';
