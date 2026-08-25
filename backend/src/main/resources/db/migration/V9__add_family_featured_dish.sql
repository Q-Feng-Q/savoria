ALTER TABLE families
  ADD COLUMN featured_dish_id bigint NULL COMMENT '家庭首页显式推荐菜品ID' AFTER delivery_fee_free,
  ADD KEY idx_families_featured_dish (featured_dish_id);
