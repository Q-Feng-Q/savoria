ALTER TABLE system_settings
  ADD COLUMN brand_tagline varchar(500) DEFAULT NULL COMMENT '通用品牌标语',
  ADD COLUMN home_hero_tagline varchar(500) DEFAULT NULL COMMENT '家庭首页顶部标语',
  ADD COLUMN home_footer_message varchar(500) DEFAULT NULL COMMENT '家庭首页与订单页底部寄语',
  ADD COLUMN cart_hero_tagline varchar(500) DEFAULT NULL COMMENT '餐篮顶部标语',
  ADD COLUMN delivery_message varchar(500) DEFAULT NULL COMMENT '配送氛围提示',
  ADD COLUMN pickup_message varchar(500) DEFAULT NULL COMMENT '自取氛围提示',
  ADD COLUMN cart_footer_message varchar(500) DEFAULT NULL COMMENT '餐篮底部寄语',
  ADD COLUMN profile_welcome_message varchar(500) DEFAULT NULL COMMENT '个人中心欢迎语';
