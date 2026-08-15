-- 家庭厨房全新空库初始化：用户、商户、家庭及账号安全。
-- 本项目不使用数据库外键；跨表引用由 Service 事务和归属校验保证。

CREATE TABLE users (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username varchar(64) NOT NULL COMMENT '登录用户名',
  username_changed tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已使用用户名修改机会',
  password_hash varchar(255) NOT NULL COMMENT '密码摘要',
  password_algorithm varchar(20) NOT NULL DEFAULT 'BCRYPT' COMMENT '密码算法',
  credential_status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '凭证状态：ACTIVE/CLAIM_REQUIRED',
  email varchar(190) NULL COMMENT '已绑定邮箱',
  email_verified tinyint(1) NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证',
  wechat_open_id varchar(128) NULL COMMENT '微信OpenID',
  nickname varchar(80) NOT NULL COMMENT '用户昵称',
  avatar_url varchar(500) NULL COMMENT '头像地址',
  mobile varchar(32) NULL COMMENT '联系电话',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态：ACTIVE/FROZEN/CANCELLING/CANCELLED',
  password_changed_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '密码最近修改时间',
  last_login_at datetime NULL COMMENT '最近登录时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_wechat_open_id (wechat_open_id),
  KEY idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一用户表';

CREATE TABLE merchants (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '商户ID',
  name varchar(100) NOT NULL COMMENT '商户名称',
  status varchar(20) NOT NULL DEFAULT 'pending' COMMENT '商户状态：pending/active/rejected/inactive',
  contact_name varchar(50) NULL COMMENT '联系人姓名',
  contact_phone varchar(30) NULL COMMENT '联系人手机号',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_merchants_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户主表';

CREATE TABLE merchant_applications (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '商户申请ID',
  user_id bigint NOT NULL COMMENT '申请用户ID',
  merchant_name varchar(100) NOT NULL COMMENT '申请商户名称',
  status varchar(20) NOT NULL DEFAULT 'pending' COMMENT '申请状态：pending/approved/rejected',
  review_remark varchar(255) NULL COMMENT '审核备注',
  reviewed_by bigint NULL COMMENT '审核人用户ID',
  reviewed_at datetime NULL COMMENT '审核时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_merchant_applications_user_status (user_id,status),
  KEY idx_merchant_applications_status_time (status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户入驻申请表';

CREATE TABLE merchant_user_relations (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '商户用户关系ID',
  user_id bigint NOT NULL COMMENT '用户ID',
  merchant_id bigint NOT NULL COMMENT '商户ID',
  merchant_role varchar(40) NOT NULL COMMENT '商户角色',
  permission_scopes json NULL COMMENT '商户权限范围JSON',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '关系状态',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_merchant_user_role (merchant_id,user_id,merchant_role),
  KEY idx_merchant_user_user_status (user_id,status),
  KEY idx_merchant_user_merchant_status (merchant_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户用户关系表';

CREATE TABLE merchant_invitation_codes (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '商户邀请码ID',
  merchant_id bigint NOT NULL COMMENT '商户ID',
  code_hash varchar(64) NOT NULL COMMENT '邀请码摘要',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '邀请码状态',
  expires_at datetime NOT NULL COMMENT '过期时间',
  created_by bigint NOT NULL COMMENT '创建人用户ID',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  disabled_at datetime NULL COMMENT '失效时间',
  UNIQUE KEY uk_merchant_invitation_hash (code_hash),
  KEY idx_merchant_invitation_active (merchant_id,status,expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户邀请码表';

CREATE TABLE user_role_relations (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '平台角色关系ID',
  user_id bigint NOT NULL COMMENT '用户ID',
  role_code varchar(40) NOT NULL COMMENT '平台角色编码',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '角色状态',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_user_platform_role (user_id,role_code),
  KEY idx_user_role_status (user_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户平台角色关系表';

CREATE TABLE families (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '家庭ID',
  merchant_id bigint NOT NULL COMMENT '所属商户ID',
  name varchar(100) NOT NULL COMMENT '家庭名称',
  note varchar(255) NULL COMMENT '家庭备注',
  contact_names_json json NULL COMMENT '家庭联系人摘要JSON数组',
  status varchar(20) NOT NULL DEFAULT 'active' COMMENT '家庭状态：active/inactive',
  delivery_enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否支持配送',
  delivery_fee_default decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '默认配送费',
  delivery_fee_free tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否免配送费',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_families_merchant_status (merchant_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭主表';

CREATE TABLE family_user_relations (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '家庭用户关系ID',
  user_id bigint NOT NULL COMMENT '用户ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  family_role varchar(20) NOT NULL COMMENT '家庭角色：OWNER/ADMIN/MEMBER',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/EXITED/REMOVED/DISSOLVED',
  join_source varchar(30) NOT NULL COMMENT '加入来源',
  invitation_id bigint NULL COMMENT '关联邀请或申请ID',
  joined_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  ended_at datetime NULL COMMENT '关系结束时间',
  created_by bigint NULL COMMENT '创建人用户ID',
  ended_by bigint NULL COMMENT '结束人用户ID',
  end_reason varchar(255) NULL COMMENT '结束原因',
  active_user_id bigint GENERATED ALWAYS AS (CASE WHEN status='ACTIVE' THEN user_id ELSE NULL END) STORED COMMENT '有效关系用户唯一键',
  active_owner_family_id bigint GENERATED ALWAYS AS (CASE WHEN status='ACTIVE' AND family_role='OWNER' THEN family_id ELSE NULL END) STORED COMMENT '有效户主家庭唯一键',
  UNIQUE KEY uk_family_relation_active_user (active_user_id),
  UNIQUE KEY uk_family_relation_active_owner (active_owner_family_id),
  KEY idx_family_relation_family_status (family_id,status),
  KEY idx_family_relation_user_history (user_id,joined_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭用户关系表';

CREATE TABLE family_addresses (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '地址ID',
  family_id bigint NOT NULL COMMENT '所属家庭ID',
  contact_name varchar(50) NOT NULL COMMENT '联系人姓名',
  contact_phone varchar(30) NOT NULL COMMENT '联系人电话',
  address_text varchar(255) NOT NULL COMMENT '详细地址',
  is_default tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否默认地址',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_family_addresses_family_default (family_id,is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭共享地址表';

CREATE TABLE meal_slots (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '餐次ID',
  family_id bigint NOT NULL COMMENT '所属家庭ID',
  name varchar(50) NOT NULL COMMENT '餐次名称',
  display_time varchar(20) NULL COMMENT '展示时间',
  sort_order int NOT NULL DEFAULT 0 COMMENT '排序值',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_meal_slots_family_name (family_id,name),
  KEY idx_meal_slots_family_enabled_sort (family_id,enabled,sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭餐次配置表';

CREATE TABLE family_applications (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '家庭创建申请ID',
  applicant_user_id bigint NOT NULL COMMENT '申请用户ID',
  merchant_id bigint NULL COMMENT '关联商户ID',
  family_name varchar(100) NOT NULL COMMENT '申请家庭名称',
  merchant_mode varchar(20) NOT NULL DEFAULT 'INVITATION' COMMENT '商户关联模式：INVITATION/JOINT_CREATE',
  merchant_invitation_id bigint NULL COMMENT '商户邀请码ID',
  proposed_merchant_name varchar(100) NULL COMMENT '拟创建商户名称',
  proposed_contact_name varchar(50) NULL COMMENT '拟创建商户联系人',
  proposed_contact_phone varchar(30) NULL COMMENT '拟创建商户联系电话',
  status varchar(20) NOT NULL DEFAULT 'pending' COMMENT '审核状态：pending/approved/rejected',
  review_remark varchar(255) NULL COMMENT '审核备注',
  reviewed_by bigint NULL COMMENT '审核人用户ID',
  reviewed_at datetime NULL COMMENT '审核时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_family_applications_status_time (status,created_at),
  KEY idx_family_applications_applicant (applicant_user_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭创建申请表';

CREATE TABLE family_invitations (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '家庭邀请码记录ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  inviter_user_id bigint NOT NULL COMMENT '邀请人用户ID',
  code varchar(20) NOT NULL COMMENT '明文邀请码',
  status varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/used/expired',
  expire_at datetime NOT NULL COMMENT '过期时间',
  used_by_user_id bigint NULL COMMENT '使用人用户ID',
  used_at datetime NULL COMMENT '使用时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_family_invitations_code (code),
  KEY idx_family_invitations_family_status (family_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭明文邀请记录表';

CREATE TABLE family_invitation_codes (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '家庭安全邀请码ID',
  family_id bigint NOT NULL COMMENT '家庭ID',
  code_hash varchar(128) NOT NULL COMMENT '邀请码摘要',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  expires_at datetime NOT NULL COMMENT '过期时间',
  created_by bigint NOT NULL COMMENT '创建人用户ID',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  rotated_at datetime NULL COMMENT '轮换时间',
  active_family_id bigint GENERATED ALWAYS AS (CASE WHEN status='ACTIVE' THEN family_id ELSE NULL END) STORED COMMENT '有效邀请码家庭唯一键',
  UNIQUE KEY uk_invitation_code_active_family (active_family_id),
  UNIQUE KEY uk_invitation_code_hash (code_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭安全邀请码表';

CREATE TABLE family_membership_requests (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '家庭邀请或申请ID',
  request_type varchar(30) NOT NULL COMMENT 'DIRECT_INVITATION/CODE_APPLICATION',
  family_id bigint NOT NULL COMMENT '目标家庭ID',
  target_user_id bigint NULL COMMENT '定向邀请目标用户ID',
  applicant_user_id bigint NULL COMMENT '邀请码申请用户ID',
  invitation_code_id bigint NULL COMMENT '邀请码ID',
  status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED/REVOKED/INVALIDATED',
  created_by bigint NOT NULL COMMENT '创建人用户ID',
  reviewed_by bigint NULL COMMENT '处理人用户ID',
  reason varchar(500) NULL COMMENT '处理原因',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  handled_at datetime NULL COMMENT '处理时间',
  pending_direct_user bigint GENERATED ALWAYS AS (CASE WHEN status='PENDING' AND request_type='DIRECT_INVITATION' THEN target_user_id ELSE NULL END) STORED COMMENT '待处理定向邀请用户唯一键',
  pending_code_user_family varchar(100) GENERATED ALWAYS AS (CASE WHEN status='PENDING' AND request_type='CODE_APPLICATION' THEN CONCAT(applicant_user_id,':',family_id) ELSE NULL END) STORED COMMENT '待处理邀请码申请唯一键',
  UNIQUE KEY uk_pending_direct_user (pending_direct_user),
  UNIQUE KEY uk_pending_code_user_family (pending_code_user_family),
  KEY idx_membership_requests_family_status (family_id,status),
  KEY idx_membership_requests_target_status (target_user_id,status),
  KEY idx_membership_requests_applicant_status (applicant_user_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭邀请与加入申请表';

CREATE TABLE user_sessions (
  id varchar(64) PRIMARY KEY COMMENT '会话ID',
  user_id bigint NOT NULL COMMENT '用户ID',
  token_hash varchar(128) NOT NULL COMMENT '令牌摘要',
  token_version int NOT NULL DEFAULT 1 COMMENT '令牌版本',
  device_info varchar(255) NULL COMMENT '设备信息',
  client_ip varchar(64) NULL COMMENT '客户端IP',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态',
  expires_at datetime NOT NULL COMMENT '过期时间',
  revoked_at datetime NULL COMMENT '撤销时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_user_sessions_user_status (user_id,status),
  KEY idx_user_sessions_expire (status,expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户登录会话表';

CREATE TABLE password_reset_records (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '密码重置记录ID',
  user_id bigint NULL COMMENT '用户ID',
  email varchar(190) NOT NULL COMMENT '目标邮箱',
  code_hash varchar(128) NOT NULL COMMENT '验证码摘要',
  status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '验证码状态',
  failed_attempts int NOT NULL DEFAULT 0 COMMENT '失败次数',
  expires_at datetime NOT NULL COMMENT '过期时间',
  used_at datetime NULL COMMENT '使用时间',
  request_ip varchar(64) NULL COMMENT '请求IP',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_password_reset_email (email,status,expires_at),
  KEY idx_password_reset_rate (request_ip,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码找回记录表';

CREATE TABLE email_verification_records (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '邮箱验证记录ID',
  user_id bigint NOT NULL COMMENT '用户ID',
  email varchar(190) NOT NULL COMMENT '待绑定邮箱',
  code_hash varchar(128) NOT NULL COMMENT '验证码摘要',
  status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '验证码状态',
  failed_attempts int NOT NULL DEFAULT 0 COMMENT '失败次数',
  expires_at datetime NOT NULL COMMENT '过期时间',
  used_at datetime NULL COMMENT '使用时间',
  request_ip varchar(64) NULL COMMENT '请求IP',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_email_verification_user (user_id,email,status,expires_at),
  KEY idx_email_verification_rate (email,request_ip,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮箱绑定验证码记录表';

CREATE TABLE wechat_authorization_records (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '微信授权记录ID',
  credential_hash varchar(128) NOT NULL COMMENT '短期凭证摘要',
  open_id_ciphertext varchar(500) NOT NULL COMMENT '加密微信OpenID',
  purpose varchar(30) NOT NULL COMMENT '凭证用途',
  status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  expires_at datetime NOT NULL COMMENT '过期时间',
  consumed_at datetime NULL COMMENT '消费时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_wechat_credential_hash (credential_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='微信短期授权记录表';

CREATE TABLE account_cancellation_requests (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '账号注销申请ID',
  user_id bigint NOT NULL COMMENT '用户ID',
  status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '注销状态',
  requested_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  cooling_end_at datetime NOT NULL COMMENT '冷静期截止时间',
  cancelled_at datetime NULL COMMENT '撤销时间',
  completed_at datetime NULL COMMENT '完成时间',
  block_reason varchar(255) NULL COMMENT '阻断原因',
  pending_user_id bigint GENERATED ALWAYS AS (CASE WHEN status='PENDING' THEN user_id ELSE NULL END) STORED COMMENT '待处理用户唯一键',
  UNIQUE KEY uk_cancellation_pending_user (pending_user_id),
  KEY idx_cancellation_due (status,cooling_end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号注销申请表';

CREATE TABLE security_audit_logs (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '安全审计日志ID',
  user_id bigint NULL COMMENT '操作用户ID',
  action varchar(80) NOT NULL COMMENT '安全操作',
  result varchar(20) NOT NULL COMMENT '操作结果',
  client_ip varchar(64) NULL COMMENT '客户端IP',
  detail varchar(500) NULL COMMENT '脱敏详情',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_security_audit_user_time (user_id,created_at),
  KEY idx_security_audit_action_time (action,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='安全审计日志表';
