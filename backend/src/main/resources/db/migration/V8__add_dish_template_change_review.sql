ALTER TABLE dish_templates
  ADD COLUMN version bigint NOT NULL DEFAULT 0 COMMENT '模板并发版本号' AFTER enabled;

CREATE TABLE dish_template_change_requests (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '模板菜品修改申请ID',
  merchant_id bigint NOT NULL COMMENT '提交商户ID',
  template_id bigint NOT NULL COMMENT '目标平台模板菜品ID',
  base_template_version bigint NOT NULL COMMENT '提交时模板并发版本号',
  base_snapshot_json json NOT NULL COMMENT '提交时模板完整业务快照',
  snapshot_json json NOT NULL COMMENT '申请覆盖后的完整业务快照',
  submit_note varchar(500) NULL COMMENT '商户提交说明',
  status varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING待审核、APPROVED已通过、REJECTED已驳回、WITHDRAWN已撤回',
  submitted_by bigint NOT NULL COMMENT '提交用户ID',
  reviewed_by bigint NULL COMMENT '审核平台管理员用户ID',
  review_reason varchar(500) NULL COMMENT '审核意见或驳回原因',
  submitted_at datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '提交时间',
  reviewed_at datetime(6) NULL COMMENT '审核完成时间',
  withdrawn_by bigint NULL COMMENT '撤回用户ID',
  withdrawn_at datetime(6) NULL COMMENT '撤回时间',
  result_notification_id bigint NULL COMMENT '审核结果站内通知ID',
  pending_marker tinyint GENERATED ALWAYS AS (CASE WHEN status='PENDING' THEN 1 ELSE NULL END) STORED COMMENT '待审核唯一标记',
  CONSTRAINT chk_dish_template_change_status CHECK (status IN ('PENDING','APPROVED','REJECTED','WITHDRAWN')),
  UNIQUE KEY uk_dish_template_change_pending (merchant_id,template_id,pending_marker),
  KEY idx_dish_template_change_status_time (status,submitted_at,id),
  KEY idx_dish_template_change_merchant_time (merchant_id,status,submitted_at,id),
  KEY idx_dish_template_change_template_time (template_id,status,submitted_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台模板菜品修改审核申请';
