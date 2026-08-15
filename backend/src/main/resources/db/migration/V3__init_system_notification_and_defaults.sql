-- 系统配置、通知、文件资产和平台默认字典。
-- 初始化系统配置、平台管理员、基础商户及其实际菜品分类和食材目录。

CREATE TABLE system_settings (
  id bigint PRIMARY KEY COMMENT '固定主键，始终为1',
  site_name varchar(100) NOT NULL COMMENT '站点名称',
  site_logo_url varchar(500) NULL COMMENT '站点Logo地址',
  dish_review_enabled tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否开启菜品审核',
  maintenance_enabled tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否开启全站维护',
  maintenance_message varchar(500) NOT NULL DEFAULT '系统维护中，请稍后再试' COMMENT '维护提示',
  mobile_binding_enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否允许绑定手机号',
  email_binding_enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否允许绑定邮箱',
  wechat_binding_enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否允许绑定微信',
  smtp_host varchar(255) NULL COMMENT 'SMTP服务器地址',
  smtp_port int NULL COMMENT 'SMTP端口',
  smtp_username varchar(255) NULL COMMENT 'SMTP用户名',
  smtp_password_ciphertext text NULL COMMENT 'SMTP密码密文',
  smtp_tls_enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用SMTP TLS',
  smtp_from varchar(255) NULL COMMENT '邮件发件人',
  updated_by bigint NULL COMMENT '最后操作管理员用户ID',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';

CREATE TABLE notifications (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
  receiver_type varchar(20) NOT NULL COMMENT '接收方类型：user/merchant/platform',
  receiver_id bigint NOT NULL COMMENT '接收方业务ID',
  receiver_scope varchar(20) NOT NULL COMMENT '接收域：user/family/merchant/platform',
  category varchar(30) NOT NULL COMMENT '通知类别',
  title varchar(100) NOT NULL COMMENT '通知标题',
  content text NULL COMMENT '通知正文',
  read_at datetime NULL COMMENT '已读时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_notifications_receiver_read (receiver_type,receiver_id,read_at),
  KEY idx_notifications_scope_receiver_read (receiver_scope,receiver_id,read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知主表';

CREATE TABLE notification_dispatches (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '通知下发记录ID',
  notification_id bigint NOT NULL COMMENT '通知ID',
  channel varchar(20) NOT NULL COMMENT '发送渠道：wechat/sms/email/system',
  target varchar(100) NULL COMMENT '发送目标',
  send_status varchar(20) NOT NULL DEFAULT 'pending' COMMENT '发送状态：pending/success/failed',
  provider_message_id varchar(100) NULL COMMENT '渠道消息ID',
  fail_reason varchar(255) NULL COMMENT '失败原因',
  sent_at datetime NULL COMMENT '发送时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_notification_dispatches_notification_channel (notification_id,channel,send_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知下发记录表';

CREATE TABLE file_assets (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '文件资产ID',
  owner_type varchar(30) NOT NULL COMMENT '所属对象类型',
  owner_id bigint NOT NULL COMMENT '所属对象ID',
  file_type varchar(30) NOT NULL COMMENT '文件业务类型',
  storage_type varchar(30) NOT NULL COMMENT '存储类型：local/oss/cos',
  url varchar(500) NOT NULL COMMENT '访问地址',
  object_key varchar(255) NULL COMMENT '对象存储键或本地相对路径',
  mime_type varchar(100) NULL COMMENT 'MIME类型',
  size_bytes bigint NULL COMMENT '文件大小字节数',
  width int NULL COMMENT '图片宽度',
  height int NULL COMMENT '图片高度',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_file_assets_owner_type (owner_type,owner_id,file_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件资产表';

INSERT INTO system_settings (id,site_name,dish_review_enabled,maintenance_enabled,maintenance_message,mobile_binding_enabled,email_binding_enabled,wechat_binding_enabled,smtp_tls_enabled) VALUES (1,'家庭厨房',0,0,'系统维护中，请稍后再试',1,1,1,1);
INSERT INTO users (id,username,username_changed,password_hash,password_algorithm,credential_status,nickname,status) VALUES (1,'admin',0,'$2a$12$1yFJKeJ8ugS9D4JPt55tjOljDZ8JJIkLYALcGcT7imlXKZNow0JRG','BCRYPT','ACTIVE','系统管理员','ACTIVE');
INSERT INTO user_role_relations (id,user_id,role_code,status) VALUES (1,1,'PLATFORM_ADMIN','ACTIVE');
INSERT INTO merchants (id,name,status,contact_name) VALUES (1,'家庭厨房','active','系统管理员');
INSERT INTO merchant_user_relations (id,user_id,merchant_id,merchant_role,status) VALUES (1,1,1,'MERCHANT_ADMIN','ACTIVE');
INSERT INTO dish_categories (merchant_id,name,sort_order,enabled) VALUES (1,'家常热菜',10,1);
INSERT INTO dish_categories (merchant_id,name,sort_order,enabled) VALUES (1,'清爽凉菜',20,1);
INSERT INTO dish_categories (merchant_id,name,sort_order,enabled) VALUES (1,'营养汤羹',30,1);
INSERT INTO dish_categories (merchant_id,name,sort_order,enabled) VALUES (1,'主食面点',40,1);
INSERT INTO dish_categories (merchant_id,name,sort_order,enabled) VALUES (1,'早餐轻食',50,1);
INSERT INTO dish_categories (merchant_id,name,sort_order,enabled) VALUES (1,'儿童餐',60,1);
INSERT INTO dish_categories (merchant_id,name,sort_order,enabled) VALUES (1,'健康轻食',70,1);
INSERT INTO dish_categories (merchant_id,name,sort_order,enabled) VALUES (1,'甜品饮品',80,1);
INSERT INTO dish_categories (merchant_id,name,sort_order,enabled) VALUES (1,'特色小吃',90,1);
INSERT INTO dish_categories (merchant_id,name,sort_order,enabled) VALUES (1,'其他',999,1);
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'西红柿','蔬菜','个');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'土豆','蔬菜','个');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'胡萝卜','蔬菜','根');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'黄瓜','蔬菜','根');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'白萝卜','蔬菜','根');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'大白菜','蔬菜','颗');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'上海青','蔬菜','斤');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'菠菜','蔬菜','斤');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'生菜','蔬菜','颗');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'西兰花','蔬菜','颗');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'菜花','蔬菜','颗');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'茄子','蔬菜','根');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'青椒','蔬菜','个');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'洋葱','蔬菜','个');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'芹菜','蔬菜','斤');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'莲藕','蔬菜','节');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'冬瓜','蔬菜','斤');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'南瓜','蔬菜','斤');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'猪肉','肉禽','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'猪排骨','肉禽','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'五花肉','肉禽','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'牛肉','肉禽','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'羊肉','肉禽','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'鸡肉','肉禽','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'鸡翅','肉禽','个');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'鸭肉','肉禽','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'鸡蛋','蛋奶','个');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'鸭蛋','蛋奶','个');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'牛奶','蛋奶','毫升');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'黄油','蛋奶','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'奶酪','蛋奶','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'鲫鱼','水产','条');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'鲈鱼','水产','条');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'虾','水产','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'螃蟹','水产','只');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'鱿鱼','水产','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'蛤蜊','水产','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'北豆腐','豆制品','块');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'嫩豆腐','豆制品','盒');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'豆腐干','豆制品','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'腐竹','豆制品','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'豆芽','豆制品','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'香菇','菌菇','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'平菇','菌菇','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'金针菇','菌菇','包');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'木耳','菌菇','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'杏鲍菇','菌菇','个');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'大米','主食粮油','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'小米','主食粮油','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'面粉','主食粮油','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'面条','主食粮油','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'玉米','主食粮油','根');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'燕麦','主食粮油','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'食用油','主食粮油','毫升');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'香油','主食粮油','毫升');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'食盐','调味料','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'白砂糖','调味料','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'生抽','调味料','毫升');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'老抽','调味料','毫升');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'米醋','调味料','毫升');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'蚝油','调味料','毫升');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'料酒','调味料','毫升');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'淀粉','调味料','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'姜','调味料','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'大蒜','调味料','头');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'小葱','调味料','根');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'香菜','调味料','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'花椒','调味料','克');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'苹果','水果','个');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'香蕉','水果','根');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'橙子','水果','个');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'梨','水果','个');
INSERT INTO merchant_ingredients (merchant_id,name,category,unit) VALUES (1,'草莓','水果','克');
