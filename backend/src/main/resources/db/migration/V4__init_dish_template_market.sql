-- 平台家常菜模板市场。
-- 所有关联ID均为逻辑引用，不创建数据库外键。

CREATE TABLE dish_template_categories (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '平台模板分类ID',
  code varchar(40) NOT NULL COMMENT '稳定分类编码',
  name varchar(50) NOT NULL COMMENT '分类名称',
  sort_order int NOT NULL DEFAULT 0 COMMENT '排序值',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_dish_template_categories_code (code),
  UNIQUE KEY uk_dish_template_categories_name (name),
  KEY idx_dish_template_categories_enabled_sort (enabled,sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台菜品模板分类表';

CREATE TABLE dish_templates (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '平台菜品模板ID',
  template_code varchar(40) NOT NULL COMMENT '稳定模板编码',
  category_id bigint NOT NULL COMMENT '平台模板分类ID',
  name varchar(100) NOT NULL COMMENT '模板菜品名称',
  description varchar(255) NOT NULL COMMENT '模板菜品简介',
  image_url varchar(500) NOT NULL COMMENT '本地图片访问地址',
  image_source_url varchar(1000) NOT NULL COMMENT '原始图片来源页面',
  image_author varchar(255) NOT NULL COMMENT '图片作者或来源平台',
  image_license varchar(255) NOT NULL COMMENT '图片授权类型',
  reference_price decimal(10,2) NOT NULL COMMENT '家庭私厨参考价',
  taste_tags json NOT NULL COMMENT '口味标签JSON',
  meal_tags json NOT NULL COMMENT '推荐餐次JSON',
  sort_order int NOT NULL DEFAULT 0 COMMENT '分类内排序值',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可导入',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_dish_templates_code (template_code),
  KEY idx_dish_templates_category_enabled_sort (category_id,enabled,sort_order),
  KEY idx_dish_templates_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台菜品模板表';

CREATE TABLE dish_template_ingredients (
  id bigint PRIMARY KEY AUTO_INCREMENT COMMENT '平台模板食材明细ID',
  template_id bigint NOT NULL COMMENT '平台菜品模板ID',
  ingredient_name varchar(100) NOT NULL COMMENT '食材名称',
  ingredient_category varchar(50) NOT NULL COMMENT '食材分类',
  quantity decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '默认计算数量',
  unit varchar(20) NOT NULL COMMENT '计量单位',
  calc_type varchar(20) NOT NULL COMMENT '计算方式：FIXED/PER_PERSON/NO_PURCHASE',
  sort_order int NOT NULL DEFAULT 0 COMMENT '显示顺序',
  KEY idx_dish_template_ingredients_template_sort (template_id,sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台菜品模板食材明细表';

INSERT INTO dish_template_categories (id,code,name,sort_order,enabled) VALUES (1,'HOME_HOT','家常热菜',10,1);
INSERT INTO dish_template_categories (id,code,name,sort_order,enabled) VALUES (2,'COLD','清爽凉菜',20,1);
INSERT INTO dish_template_categories (id,code,name,sort_order,enabled) VALUES (3,'SOUP','营养汤羹',30,1);
INSERT INTO dish_template_categories (id,code,name,sort_order,enabled) VALUES (4,'STAPLE','主食面点',40,1);
INSERT INTO dish_template_categories (id,code,name,sort_order,enabled) VALUES (5,'BREAKFAST','早餐轻食',50,1);
INSERT INTO dish_template_categories (id,code,name,sort_order,enabled) VALUES (6,'CHILDREN','儿童餐',60,1);
INSERT INTO dish_template_categories (id,code,name,sort_order,enabled) VALUES (7,'HEALTHY','健康轻食',70,1);
INSERT INTO dish_template_categories (id,code,name,sort_order,enabled) VALUES (8,'SNACK','特色小吃',80,1);
INSERT INTO dish_template_categories (id,code,name,sort_order,enabled) VALUES (9,'DESSERT','甜品饮品',90,1);

-- TEMPLATE DISH_001 红烧肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (1,'DISH_001',1,'红烧肉','红烧肉，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-001.jpg','https://commons.wikimedia.org/wiki/File%3A%E7%B4%85%E7%87%92%E8%82%89_Braised_pork_in_brown_sauce.jpg','FotoosVanRobin Photostream','CC BY 2.0',24.00,'["醇香"]','["LUNCH","DINNER"]',1,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (1,'五花肉','肉禽水产',400.00,'g','FIXED',1);
-- TEMPLATE DISH_002 糖醋排骨
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (2,'DISH_002',1,'糖醋排骨','糖醋排骨，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-002.jpg','https://commons.wikimedia.org/wiki/File%3AFlickr_-_Sweet_and_Sour_Pork.jpg','Alpha from Melbourne, Australia','CC BY-SA 2.0',26.00,'["酸甜"]','["LUNCH","DINNER"]',2,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (2,'猪肋排','蔬菜及其他',500.00,'g','FIXED',1);
-- TEMPLATE DISH_003 土豆烧牛腩
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (3,'DISH_003',1,'土豆烧牛腩','土豆烧牛腩，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-003.jpg','https://commons.wikimedia.org/wiki/File%3ADish_of_stewed_pork%2C_Romanesco_and_broccoli%2C_and_steamed_egg%2C_Singapore_-_20150129.jpg','Smuconlaw.','CC BY-SA 4.0',36.00,'["家常"]','["LUNCH","DINNER"]',3,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (3,'牛腩','蔬菜及其他',500.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (3,'土豆','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_004 番茄炒蛋
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (4,'DISH_004',1,'番茄炒蛋','番茄炒蛋，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-004.jpg','https://commons.wikimedia.org/wiki/File%3APip%C3%A9rade.jpg','FotoosVanRobin','CC BY-SA 2.0',22.00,'["家常"]','["LUNCH","DINNER"]',4,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (4,'番茄','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_005 青椒肉丝
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (5,'DISH_005',1,'青椒肉丝','青椒肉丝，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-005.jpg','https://commons.wikimedia.org/wiki/File%3APepper_steak.jpg','Bakkai撮影','CC BY-SA 3.0',32.00,'["家常"]','["LUNCH","DINNER"]',5,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (5,'猪里脊','蔬菜及其他',300.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (5,'青椒','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_006 鱼香肉丝
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (6,'DISH_006',1,'鱼香肉丝','鱼香肉丝，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-006.jpg','https://commons.wikimedia.org/wiki/File%3AFish_flavoured_sliced_pork_from_Melbourne.jpg','avlxyz on flickr','CC BY-SA 2.0',24.00,'["家常"]','["LUNCH","DINNER"]',6,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (6,'猪里脊','蔬菜及其他',300.00,'g','FIXED',1);
-- TEMPLATE DISH_007 宫保鸡丁
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (7,'DISH_007',1,'宫保鸡丁','宫保鸡丁，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-007.jpg','https://commons.wikimedia.org/wiki/File%3AKung_Pao_chicken_(western_version)_-1.jpg','Alexander Marks (aomarks)','Public domain',26.00,'["家常"]','["LUNCH","DINNER"]',7,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (7,'鸡腿肉','肉禽水产',400.00,'g','FIXED',1);
-- TEMPLATE DISH_008 麻婆豆腐
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (8,'DISH_008',1,'麻婆豆腐','麻婆豆腐，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-008.jpg','https://commons.wikimedia.org/wiki/File%3AMapo_doufu_by_kina3.jpg','kina3','CC BY 2.0',20.00,'["香辣"]','["LUNCH","DINNER"]',8,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (8,'北豆腐','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_009 回锅肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (9,'DISH_009',1,'回锅肉','回锅肉，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-009.jpg','https://commons.wikimedia.org/wiki/File%3ATwice_cooked_pork_and_rice%2C_Canard_Dor%C3%A9%2C_Paris_003.jpg','Guilhem Vellut from Paris, France','CC BY 2.0',30.00,'["家常"]','["LUNCH","DINNER"]',9,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (9,'五花肉','肉禽水产',400.00,'g','FIXED',1);
-- TEMPLATE DISH_010 木须肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (10,'DISH_010',1,'木须肉','木须肉，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-010.jpg','https://commons.wikimedia.org/wiki/File%3AMoo_Shu_Pork_Burritos.jpg','Mark Mitchell from Toledo, Ohio, United States of America','CC BY 2.0',32.00,'["家常"]','["LUNCH","DINNER"]',10,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (10,'时令蔬菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_011 京酱肉丝
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (11,'DISH_011',1,'京酱肉丝','京酱肉丝，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-011.jpg','https://commons.wikimedia.org/wiki/File%3AJing_Jiang_Rou_Si_01.jpg','gigijin','CC BY-SA 2.0',24.00,'["家常"]','["LUNCH","DINNER"]',11,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (11,'猪里脊','蔬菜及其他',300.00,'g','FIXED',1);
-- TEMPLATE DISH_012 尖椒炒肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (12,'DISH_012',1,'尖椒炒肉','尖椒炒肉，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-012.jpg','https://commons.wikimedia.org/wiki/File%3ADish_of_stewed_pork%2C_Romanesco_and_broccoli%2C_and_steamed_egg%2C_Singapore_-_20150129.jpg','Smuconlaw.','CC BY-SA 4.0',26.00,'["家常"]','["LUNCH","DINNER"]',12,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (12,'尖椒','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_013 蒜薹炒肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (13,'DISH_013',1,'蒜薹炒肉','蒜薹炒肉，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-013.jpg','https://commons.wikimedia.org/wiki/File%3ADish_of_stewed_pork%2C_Romanesco_and_broccoli%2C_and_steamed_egg%2C_Singapore_-_20150129.jpg','Smuconlaw.','CC BY-SA 4.0',28.00,'["家常"]','["LUNCH","DINNER"]',13,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (13,'蒜薹','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_014 芹菜炒肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (14,'DISH_014',1,'芹菜炒肉','芹菜炒肉，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-014.jpg','https://commons.wikimedia.org/wiki/File%3A2%E6%9C%885%E6%97%A5_%E8%A3%9C%E7%9A%84%E5%B9%B4%E5%A4%9C%E9%A3%AF.jpg','Singzy','CC BY 2.0',30.00,'["家常"]','["LUNCH","DINNER"]',14,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (14,'芹菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_015 洋葱炒肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (15,'DISH_015',1,'洋葱炒肉','洋葱炒肉，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-015.jpg','https://commons.wikimedia.org/wiki/File%3ADish_of_stewed_pork%2C_Romanesco_and_broccoli%2C_and_steamed_egg%2C_Singapore_-_20150129.jpg','Smuconlaw.','CC BY-SA 4.0',32.00,'["家常"]','["LUNCH","DINNER"]',15,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (15,'洋葱','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_016 莴笋炒肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (16,'DISH_016',1,'莴笋炒肉','莴笋炒肉，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-016.jpg','https://commons.wikimedia.org/wiki/File%3ADish_of_stewed_pork%2C_Romanesco_and_broccoli%2C_and_steamed_egg%2C_Singapore_-_20150129.jpg','Smuconlaw.','CC BY-SA 4.0',24.00,'["家常"]','["LUNCH","DINNER"]',16,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (16,'莴笋','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_017 白菜炒肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (17,'DISH_017',1,'白菜炒肉','白菜炒肉，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-017.jpg','https://commons.wikimedia.org/wiki/File%3AFood_%E8%B1%AC%E8%A1%80%E6%B9%AF%2C_%E9%AD%AF%E7%99%BD%E8%8F%9C%2C_%E9%AD%AF%E8%B1%86%E7%9A%AE%2C_%E9%AD%AF%E8%9B%8B%2C_%E8%85%BF%E5%BA%AB%E8%82%89%2C_%E9%AD%AF%E8%82%89%E9%A3%AF%2C_%E6%96%B0%E7%AB%B9%E7%82%92%E7%B1%B3%E7%B2%89%2C_%E5%A4%A7%E8%85%B8%E5%88%87%E7%9B%A4%2C_%E5%A4%A7%E9%BC%8E%E8%B1%AC%E8%A1%80%E6%B9%AF%2C_%E5%8F%B0%E5%8C%97_(13899976038).jpg','bryan... from Taipei, Taiwan','CC BY-SA 2.0',26.00,'["家常"]','["LUNCH","DINNER"]',17,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (17,'大白菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_018 香菇炒鸡
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (18,'DISH_018',1,'香菇炒鸡','香菇炒鸡，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-018.jpg','https://commons.wikimedia.org/wiki/File%3ADish_of_stewed_pork%2C_Romanesco_and_broccoli%2C_and_steamed_egg%2C_Singapore_-_20150129.jpg','Smuconlaw.','CC BY-SA 4.0',28.00,'["家常"]','["LUNCH","DINNER"]',18,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (18,'鸡腿肉','肉禽水产',400.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (18,'鲜香菇','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_019 黄焖鸡
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (19,'DISH_019',1,'黄焖鸡','黄焖鸡，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-019.jpg','https://commons.wikimedia.org/wiki/File%3ABraised_chicken.jpg','ZZArch','CC BY-SA 3.0',30.00,'["醇香"]','["LUNCH","DINNER"]',19,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (19,'鸡腿肉','肉禽水产',400.00,'g','FIXED',1);
-- TEMPLATE DISH_020 可乐鸡翅
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (20,'DISH_020',1,'可乐鸡翅','可乐鸡翅，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-020.jpg','https://commons.wikimedia.org/wiki/File%3A%E5%8F%AF%E4%B9%90%E9%B8%A1%E7%BF%85.jpg','Wuzhiya','CC BY-SA 4.0',32.00,'["家常"]','["LUNCH","DINNER"]',20,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (20,'鸡翅中','肉禽水产',8.00,'个','FIXED',1);
-- TEMPLATE DISH_021 红烧鸡翅
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (21,'DISH_021',1,'红烧鸡翅','红烧鸡翅，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-021.jpg','https://commons.wikimedia.org/wiki/File%3ADish_of_stewed_pork%2C_Romanesco_and_broccoli%2C_and_steamed_egg%2C_Singapore_-_20150129.jpg','Smuconlaw.','CC BY-SA 4.0',24.00,'["醇香"]','["LUNCH","DINNER"]',21,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (21,'鸡翅中','肉禽水产',8.00,'个','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (21,'鸡腿肉','肉禽水产',400.00,'g','FIXED',2);
-- TEMPLATE DISH_022 辣子鸡
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (22,'DISH_022',1,'辣子鸡','辣子鸡，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-022.jpg','https://commons.wikimedia.org/wiki/File%3ALa_Zi_Ji_(Chicken_with_Chiles)_(2269517013).jpg','FotoosVanRobin from Netherlands','CC BY-SA 2.0',26.00,'["香辣"]','["LUNCH","DINNER"]',22,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (22,'鸡腿肉','肉禽水产',400.00,'g','FIXED',1);
-- TEMPLATE DISH_023 三杯鸡
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (23,'DISH_023',1,'三杯鸡','三杯鸡，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-023.jpg','https://commons.wikimedia.org/wiki/File%3ARed_Columns_sanbeiji.jpg','sanbeiji','CC BY-SA 2.0',28.00,'["家常"]','["LUNCH","DINNER"]',23,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (23,'鸡腿肉','肉禽水产',400.00,'g','FIXED',1);
-- TEMPLATE DISH_024 葱油鸡
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (24,'DISH_024',1,'葱油鸡','葱油鸡，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-024.jpg','https://commons.wikimedia.org/wiki/File%3ADish_of_stewed_pork%2C_Romanesco_and_broccoli%2C_and_steamed_egg%2C_Singapore_-_20150129.jpg','Smuconlaw.','CC BY-SA 4.0',30.00,'["家常"]','["LUNCH","DINNER"]',24,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (24,'整鸡','肉禽水产',1.00,'只','FIXED',1);
-- TEMPLATE DISH_025 白切鸡
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (25,'DISH_025',1,'白切鸡','白切鸡，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-025.jpg','https://commons.wikimedia.org/wiki/File%3ABeiQieJi-WhiteCutChicken.jpg','Sjschen','CC BY-SA 3.0',32.00,'["清淡"]','["LUNCH","DINNER"]',25,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (25,'整鸡','肉禽水产',1.00,'只','FIXED',1);
-- TEMPLATE DISH_026 红烧排骨
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (26,'DISH_026',1,'红烧排骨','红烧排骨，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-026.jpg','https://commons.wikimedia.org/wiki/File%3AMianjin_hongshao_paigu_2009_03.jpg','Rolfmueller','CC BY-SA 3.0',24.00,'["醇香"]','["LUNCH","DINNER"]',26,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (26,'猪肋排','蔬菜及其他',500.00,'g','FIXED',1);
-- TEMPLATE DISH_027 蒜香排骨
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (27,'DISH_027',1,'蒜香排骨','蒜香排骨，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-027.jpg','https://commons.wikimedia.org/wiki/File%3ASmoked_country_style_pork_ribs.jpg','Dennis Brown','CC BY 3.0',26.00,'["家常"]','["LUNCH","DINNER"]',27,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (27,'猪肋排','蔬菜及其他',500.00,'g','FIXED',1);
-- TEMPLATE DISH_028 粉蒸排骨
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (28,'DISH_028',1,'粉蒸排骨','粉蒸排骨，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-028.jpg','https://commons.wikimedia.org/wiki/File%3ADish_of_stewed_pork%2C_Romanesco_and_broccoli%2C_and_steamed_egg%2C_Singapore_-_20150129.jpg','Smuconlaw.','CC BY-SA 4.0',28.00,'["清淡"]','["LUNCH","DINNER"]',28,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (28,'猪肋排','蔬菜及其他',500.00,'g','FIXED',1);
-- TEMPLATE DISH_029 梅菜扣肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (29,'DISH_029',1,'梅菜扣肉','梅菜扣肉，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-029.jpg','https://commons.wikimedia.org/wiki/File%3ACuisine_of_China_0074.JPG','Brücke-Osteuropa','Public domain',30.00,'["醇香"]','["LUNCH","DINNER"]',29,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (29,'五花肉','肉禽水产',400.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (29,'梅干菜','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_030 红烧狮子头
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (30,'DISH_030',1,'红烧狮子头','红烧狮子头，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-030.jpg','https://commons.wikimedia.org/wiki/File%3ALions_head_meatballs_in_brown_sauce.jpg','Jpatokal','CC BY-SA 4.0',24.00,'["醇香"]','["LUNCH","DINNER"]',30,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (30,'猪肉末','肉禽水产',300.00,'g','FIXED',1);
-- TEMPLATE DISH_031 四喜丸子
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (31,'DISH_031',1,'四喜丸子','四喜丸子，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-031.jpg','https://commons.wikimedia.org/wiki/File%3AXiaoSiXi_(Mahjong).JPG','The original uploader was Kowloonese at English Wikipedia.','CC BY-SA 3.0',16.00,'["家常"]','["LUNCH","DINNER"]',31,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (31,'猪肉末','肉禽水产',300.00,'g','FIXED',1);
-- TEMPLATE DISH_032 农家小炒肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (32,'DISH_032',1,'农家小炒肉','农家小炒肉，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-032.jpg','https://commons.wikimedia.org/wiki/File%3AFried_Pork_with_Pepper_20210630.jpg','Huangdan2060','CC BY 3.0',26.00,'["家常"]','["LUNCH","DINNER"]',32,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (32,'五花肉','肉禽水产',400.00,'g','FIXED',1);
-- TEMPLATE DISH_033 土豆炖排骨
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (33,'DISH_033',1,'土豆炖排骨','土豆炖排骨，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-033.jpg','https://commons.wikimedia.org/wiki/File%3A%E6%B9%98%E8%8F%9C%E9%A6%86%E4%B9%8B%E5%9C%9F%E8%B1%86%E7%82%96%E6%8E%92%E9%AA%A8.jpg','Liuxingy','CC BY-SA 4.0',28.00,'["醇香"]','["LUNCH","DINNER"]',33,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (33,'猪肋排','蔬菜及其他',500.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (33,'土豆','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_034 豆角炖排骨
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (34,'DISH_034',1,'豆角炖排骨','豆角炖排骨，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-034.jpg','https://commons.wikimedia.org/wiki/File%3AJiefang_Road%2C_Shenzhen_(254).jpg','Dinkun Chen','CC BY-SA 4.0',30.00,'["醇香"]','["LUNCH","DINNER"]',34,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (34,'猪肋排','蔬菜及其他',500.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (34,'四季豆','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_035 冬瓜烧肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (35,'DISH_035',1,'冬瓜烧肉','冬瓜烧肉，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-035.jpg','https://commons.wikimedia.org/wiki/File%3ADish_of_stewed_pork%2C_Romanesco_and_broccoli%2C_and_steamed_egg%2C_Singapore_-_20150129.jpg','Smuconlaw.','CC BY-SA 4.0',32.00,'["家常"]','["LUNCH","DINNER"]',35,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (35,'冬瓜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_036 鹌鹑蛋红烧肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (36,'DISH_036',1,'鹌鹑蛋红烧肉','鹌鹑蛋红烧肉，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-036.jpg','https://commons.wikimedia.org/wiki/File%3ADish_of_stewed_pork%2C_Romanesco_and_broccoli%2C_and_steamed_egg%2C_Singapore_-_20150129.jpg','Smuconlaw.','CC BY-SA 4.0',24.00,'["醇香"]','["LUNCH","DINNER"]',36,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (36,'五花肉','肉禽水产',400.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (36,'鹌鹑蛋','蛋奶',10.00,'个','FIXED',2);
-- TEMPLATE DISH_037 水煮肉片
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (37,'DISH_037',1,'水煮肉片','水煮肉片，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-037.jpg','https://commons.wikimedia.org/wiki/File%3A%E5%AE%B6%E5%BA%AD%E6%B0%B4%E7%85%AE%E8%82%89%E7%89%87.jpg','zhantongz','CC BY-SA 3.0',26.00,'["香辣"]','["LUNCH","DINNER"]',37,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (37,'猪里脊','蔬菜及其他',300.00,'g','FIXED',1);
-- TEMPLATE DISH_038 孜然牛肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (38,'DISH_038',1,'孜然牛肉','孜然牛肉，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-038.jpg','https://commons.wikimedia.org/wiki/File%3ACumin_beef_fried_rice_(1).jpg','Fumikas Sagisavas','CC0',28.00,'["家常"]','["LUNCH","DINNER"]',38,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (38,'牛肉','肉禽水产',300.00,'g','FIXED',1);
-- TEMPLATE DISH_039 黑椒牛柳
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (39,'DISH_039',1,'黑椒牛柳','黑椒牛柳，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-039.jpg','https://www.flickr.com/photos/10559879@N00/4003904363','avlxyz','BY-SA 2.0',30.00,'["家常"]','["LUNCH","DINNER"]',39,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (39,'牛里脊','蔬菜及其他',300.00,'g','FIXED',1);
-- TEMPLATE DISH_040 土豆烧鸡
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (40,'DISH_040',1,'土豆烧鸡','土豆烧鸡，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-040.jpg','https://www.flickr.com/photos/10559879@N00/2500744954','avlxyz','BY-SA 2.0',32.00,'["家常"]','["LUNCH","DINNER"]',40,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (40,'鸡腿肉','肉禽水产',400.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (40,'土豆','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_041 咖喱鸡块
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (41,'DISH_041',1,'咖喱鸡块','咖喱鸡块，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-041.jpg','https://www.flickr.com/photos/71635685@N00/2512709831','DavidPanChina','BY-SA 2.0',24.00,'["家常"]','["LUNCH","DINNER"]',41,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (41,'鸡腿肉','肉禽水产',400.00,'g','FIXED',1);
-- TEMPLATE DISH_042 板栗烧鸡
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (42,'DISH_042',1,'板栗烧鸡','板栗烧鸡，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-042.jpg','https://www.flickr.com/photos/10559879@N00/2500744954','avlxyz','BY-SA 2.0',26.00,'["家常"]','["LUNCH","DINNER"]',42,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (42,'鸡腿肉','肉禽水产',400.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (42,'板栗','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_043 香煎鸡胸
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (43,'DISH_043',1,'香煎鸡胸','香煎鸡胸，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-043.jpg','https://www.flickr.com/photos/10559879@N00/2500744954','avlxyz','BY-SA 2.0',28.00,'["家常"]','["LUNCH","DINNER"]',43,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (43,'鸡胸肉','肉禽水产',300.00,'g','FIXED',1);
-- TEMPLATE DISH_044 清蒸鲈鱼
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (44,'DISH_044',1,'清蒸鲈鱼','清蒸鲈鱼，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-044.jpg','https://www.flickr.com/photos/10559879@N00/2337447558','avlxyz','BY-SA 2.0',30.00,'["清淡"]','["LUNCH","DINNER"]',44,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (44,'鲈鱼','肉禽水产',1.00,'条','FIXED',1);
-- TEMPLATE DISH_045 红烧鲫鱼
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (45,'DISH_045',1,'红烧鲫鱼','红烧鲫鱼，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-045.jpg','https://www.flickr.com/photos/10559879@N00/2500744954','avlxyz','BY-SA 2.0',32.00,'["醇香"]','["LUNCH","DINNER"]',45,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (45,'鲫鱼','肉禽水产',1.00,'条','FIXED',1);
-- TEMPLATE DISH_046 糖醋鱼
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (46,'DISH_046',1,'糖醋鱼','糖醋鱼，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-046.jpg','https://www.flickr.com/photos/10559879@N00/3450251342','avlxyz','BY-SA 2.0',24.00,'["酸甜"]','["LUNCH","DINNER"]',46,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (46,'鲜鱼','肉禽水产',1.00,'条','FIXED',1);
-- TEMPLATE DISH_047 剁椒鱼头
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (47,'DISH_047',1,'剁椒鱼头','剁椒鱼头，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-047.jpg','https://www.flickr.com/photos/47038415@N00/216875875','Augapfel','BY 2.0',34.00,'["香辣"]','["LUNCH","DINNER"]',47,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (47,'花鲢鱼头','肉禽水产',1.00,'个','FIXED',1);
-- TEMPLATE DISH_048 酸菜鱼
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (48,'DISH_048',1,'酸菜鱼','酸菜鱼，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-048.jpg','https://www.flickr.com/photos/10559879@N00/4104355401','avlxyz','BY-SA 2.0',28.00,'["家常"]','["LUNCH","DINNER"]',48,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (48,'草鱼','肉禽水产',1.00,'条','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (48,'酸菜','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_049 水煮鱼
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (49,'DISH_049',1,'水煮鱼','水煮鱼，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-049.jpg','https://www.flickr.com/photos/10559879@N00/2952391629','avlxyz','BY-SA 2.0',30.00,'["香辣"]','["LUNCH","DINNER"]',49,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (49,'草鱼','肉禽水产',1.00,'条','FIXED',1);
-- TEMPLATE DISH_050 豉汁蒸鱼
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (50,'DISH_050',1,'豉汁蒸鱼','豉汁蒸鱼，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-050.jpg','https://www.flickr.com/photos/10559879@N00/2500744954','avlxyz','BY-SA 2.0',32.00,'["清淡"]','["LUNCH","DINNER"]',50,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (50,'鲜鱼','肉禽水产',1.00,'条','FIXED',1);
-- TEMPLATE DISH_051 蒜蓉粉丝虾
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (51,'DISH_051',1,'蒜蓉粉丝虾','蒜蓉粉丝虾，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-051.jpg','https://www.flickr.com/photos/10559879@N00/2500744954','avlxyz','BY-SA 2.0',24.00,'["家常"]','["LUNCH","DINNER"]',51,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (51,'时令蔬菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_052 油焖大虾
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (52,'DISH_052',1,'油焖大虾','油焖大虾，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-052.jpg','https://commons.wikimedia.org/wiki/File%3A%E4%B8%AD%E5%9B%BD%E5%AE%B6%E5%B8%B8%E8%8F%9C-%E6%B2%B9%E7%84%96%E5%A4%A7%E8%99%BE.jpg','Zsllong','CC BY-SA 4.0',34.00,'["醇香"]','["LUNCH","DINNER"]',52,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (52,'鲜虾','肉禽水产',400.00,'g','FIXED',1);
-- TEMPLATE DISH_053 香辣虾
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (53,'DISH_053',1,'香辣虾','香辣虾，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-053.jpg','https://www.flickr.com/photos/45390424@N02/14541535709','双核心飞鱼','BY 2.0',28.00,'["香辣"]','["LUNCH","DINNER"]',53,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (53,'鲜虾','肉禽水产',400.00,'g','FIXED',1);
-- TEMPLATE DISH_054 韭菜炒虾仁
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (54,'DISH_054',1,'韭菜炒虾仁','韭菜炒虾仁，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-054.jpg','https://www.flickr.com/photos/10559879@N00/2500744954','avlxyz','BY-SA 2.0',30.00,'["家常"]','["LUNCH","DINNER"]',54,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (54,'虾仁','肉禽水产',250.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (54,'韭菜','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_055 西兰花炒虾仁
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (55,'DISH_055',1,'西兰花炒虾仁','西兰花炒虾仁，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-055.jpg','https://www.flickr.com/photos/10559879@N00/2500744954','avlxyz','BY-SA 2.0',32.00,'["家常"]','["LUNCH","DINNER"]',55,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (55,'虾仁','肉禽水产',250.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (55,'西兰花','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_056 番茄炖豆腐
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (56,'DISH_056',1,'番茄炖豆腐','番茄炖豆腐，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-056.jpg','https://www.flickr.com/photos/10559879@N00/2500744954','avlxyz','BY-SA 2.0',16.00,'["醇香"]','["LUNCH","DINNER"]',56,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (56,'番茄','蔬菜及其他',200.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (56,'北豆腐','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_057 地三鲜
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (57,'DISH_057',1,'地三鲜','地三鲜，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-057.jpg','https://www.flickr.com/photos/10559879@N00/2403232046','avlxyz','BY-SA 2.0',18.00,'["家常"]','["LUNCH","DINNER"]',57,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (57,'时令蔬菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_058 干煸豆角
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (58,'DISH_058',1,'干煸豆角','干煸豆角，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-058.jpg','https://www.flickr.com/photos/90939609@N00/122780454','Pictlux','BY-SA 2.0',20.00,'["家常"]','["LUNCH","DINNER"]',58,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (58,'四季豆','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_059 醋溜白菜
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (59,'DISH_059',1,'醋溜白菜','醋溜白菜，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-059.jpg','https://www.flickr.com/photos/7898291@N03/4291831933','Oscar, give me five','BY-SA 2.0',22.00,'["家常"]','["LUNCH","DINNER"]',59,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (59,'大白菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_060 手撕包菜
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (60,'DISH_060',1,'手撕包菜','手撕包菜，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-060.jpg','https://commons.wikimedia.org/wiki/File%3AStarr_080103-1276_Brassica_oleracea_var._capitata.jpg','Forest & Kim Starr','CC BY 3.0',24.00,'["家常"]','["LUNCH","DINNER"]',60,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (60,'卷心菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_061 清炒西兰花
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (61,'DISH_061',1,'清炒西兰花','清炒西兰花，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-061.jpg','https://www.flickr.com/photos/65189805@N03/5935721220','incity007','BY-SA 2.0',16.00,'["清淡"]','["LUNCH","DINNER"]',61,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (61,'西兰花','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_062 蒜蓉油麦菜
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (62,'DISH_062',1,'蒜蓉油麦菜','蒜蓉油麦菜，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-062.jpg','https://www.flickr.com/photos/10559879@N00/2500744954','avlxyz','BY-SA 2.0',18.00,'["家常"]','["LUNCH","DINNER"]',62,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (62,'油麦菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_063 蒜蓉生菜
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (63,'DISH_063',1,'蒜蓉生菜','蒜蓉生菜，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-063.jpg','https://www.flickr.com/photos/76958594@N04/7625961994','D11xzj','BY 2.0',20.00,'["家常"]','["LUNCH","DINNER"]',63,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (63,'生菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_064 荷塘小炒
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (64,'DISH_064',1,'荷塘小炒','荷塘小炒，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-064.jpg','https://www.flickr.com/photos/10559879@N00/2500744954','avlxyz','BY-SA 2.0',22.00,'["家常"]','["LUNCH","DINNER"]',64,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (64,'时令蔬菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_065 香菇油菜
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (65,'DISH_065',1,'香菇油菜','香菇油菜，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-065.jpg','https://www.flickr.com/photos/7656598@N04/2847095853','DING *_^','BY 2.0',24.00,'["家常"]','["LUNCH","DINNER"]',65,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (65,'鲜香菇','蔬菜及其他',200.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (65,'上海青','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_066 蚝油杏鲍菇
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (66,'DISH_066',1,'蚝油杏鲍菇','蚝油杏鲍菇，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-066.jpg','https://commons.wikimedia.org/wiki/File%3ABeef_and_shrimp_set_meal_on_G65_(20170307121129).jpg','N509FZ','CC BY-SA 4.0',16.00,'["家常"]','["LUNCH","DINNER"]',66,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (66,'杏鲍菇','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_067 干锅花菜
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (67,'DISH_067',1,'干锅花菜','干锅花菜，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-067.jpg','https://www.flickr.com/photos/88718223@N00/13121053625','heiyo','BY-SA 2.0',18.00,'["家常"]','["LUNCH","DINNER"]',67,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (67,'花菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_068 红烧茄子
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (68,'DISH_068',1,'红烧茄子','红烧茄子，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-068.jpg','https://commons.wikimedia.org/wiki/File%3A%E7%BA%A2%E7%83%A7%E8%8C%84%E5%AD%90-%E4%BA%91%E5%8D%97%E6%98%86%E6%98%8E.jpg','tak.wing','CC BY-SA 2.0',20.00,'["醇香"]','["LUNCH","DINNER"]',68,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (68,'茄子','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_069 肉末茄子
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (69,'DISH_069',1,'肉末茄子','肉末茄子，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-069.jpg','https://www.flickr.com/photos/10559879@N00/2500744954','avlxyz','BY-SA 2.0',30.00,'["家常"]','["LUNCH","DINNER"]',69,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (69,'猪肉末','肉禽水产',300.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (69,'茄子','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_070 家常豆腐
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (70,'DISH_070',1,'家常豆腐','家常豆腐，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-070.jpg','https://www.flickr.com/photos/10559879@N00/2304041908','avlxyz','BY-SA 2.0',24.00,'["家常"]','["LUNCH","DINNER"]',70,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (70,'北豆腐','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_071 韭菜炒鸡蛋
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (71,'DISH_071',1,'韭菜炒鸡蛋','韭菜炒鸡蛋，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-071.jpg','https://www.flickr.com/photos/10559879@N00/2500744954','avlxyz','BY-SA 2.0',24.00,'["家常"]','["LUNCH","DINNER"]',71,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (71,'鸡腿肉','肉禽水产',400.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (71,'鸡蛋','肉禽水产',3.00,'个','FIXED',2);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (71,'韭菜','蔬菜及其他',200.00,'g','FIXED',3);
-- TEMPLATE DISH_072 西葫芦炒鸡蛋
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (72,'DISH_072',1,'西葫芦炒鸡蛋','西葫芦炒鸡蛋，经典家常做法，食材易采购，适合午餐或晚餐。','/images/dish-templates/dish-072.jpg','https://www.flickr.com/photos/10559879@N00/2500744954','avlxyz','BY-SA 2.0',26.00,'["家常"]','["LUNCH","DINNER"]',72,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (72,'鸡腿肉','肉禽水产',400.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (72,'鸡蛋','肉禽水产',3.00,'个','FIXED',2);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (72,'西葫芦','蔬菜及其他',200.00,'g','FIXED',3);
-- TEMPLATE DISH_073 拍黄瓜
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (73,'DISH_073',2,'拍黄瓜','拍黄瓜，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-073.jpg','https://www.flickr.com/photos/10559879@N00/3130197778','avlxyz','BY-SA 2.0',9.00,'["家常"]','["LUNCH","DINNER"]',73,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (73,'黄瓜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_074 凉拌木耳
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (74,'DISH_074',2,'凉拌木耳','凉拌木耳，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-074.jpg','https://www.flickr.com/photos/10559879@N00/2461262750','avlxyz','BY-SA 2.0',11.00,'["清淡"]','["LUNCH","DINNER"]',74,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (74,'黑木耳','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_075 凉拌海带丝
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (75,'DISH_075',2,'凉拌海带丝','凉拌海带丝，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-075.jpg','https://www.flickr.com/photos/10559879@N00/2461262750','avlxyz','BY-SA 2.0',13.00,'["清淡"]','["LUNCH","DINNER"]',75,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (75,'海带','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_076 凉拌腐竹
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (76,'DISH_076',2,'凉拌腐竹','凉拌腐竹，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-076.jpg','https://commons.wikimedia.org/wiki/File%3ATeochew_Dumplings_-_Chongqing_Hotpot_AUD3.80_small_(3496588212).jpg','Alpha from Melbourne, Australia','CC BY-SA 2.0',15.00,'["清淡"]','["LUNCH","DINNER"]',76,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (76,'腐竹','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_077 凉拌豆皮
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (77,'DISH_077',2,'凉拌豆皮','凉拌豆皮，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-077.jpg','https://www.flickr.com/photos/10559879@N00/2821584562','avlxyz','BY-SA 2.0',17.00,'["清淡"]','["LUNCH","DINNER"]',77,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (77,'豆腐皮','粮油主食',200.00,'g','FIXED',1);
-- TEMPLATE DISH_078 凉拌三丝
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (78,'DISH_078',2,'凉拌三丝','凉拌三丝，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-078.jpg','https://www.flickr.com/photos/10559879@N00/3495773419','avlxyz','BY-SA 2.0',9.00,'["清淡"]','["LUNCH","DINNER"]',78,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (78,'时令蔬菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_079 凉拌菠菜
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (79,'DISH_079',2,'凉拌菠菜','凉拌菠菜，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-079.jpg','https://www.flickr.com/photos/10559879@N00/2461262750','avlxyz','BY-SA 2.0',11.00,'["清淡"]','["LUNCH","DINNER"]',79,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (79,'菠菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_080 凉拌莴笋
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (80,'DISH_080',2,'凉拌莴笋','凉拌莴笋，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-080.jpg','https://www.flickr.com/photos/10559879@N00/2461262750','avlxyz','BY-SA 2.0',13.00,'["清淡"]','["LUNCH","DINNER"]',80,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (80,'莴笋','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_081 凉拌西兰花
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (81,'DISH_081',2,'凉拌西兰花','凉拌西兰花，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-081.jpg','https://www.flickr.com/photos/10559879@N00/2461262750','avlxyz','BY-SA 2.0',15.00,'["清淡"]','["LUNCH","DINNER"]',81,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (81,'西兰花','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_082 凉拌藕片
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (82,'DISH_082',2,'凉拌藕片','凉拌藕片，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-082.jpg','https://www.flickr.com/photos/10559879@N00/2461262750','avlxyz','BY-SA 2.0',17.00,'["清淡"]','["LUNCH","DINNER"]',82,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (82,'莲藕','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_083 凉拌土豆丝
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (83,'DISH_083',2,'凉拌土豆丝','凉拌土豆丝，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-083.jpg','https://www.flickr.com/photos/10559879@N00/2461262750','avlxyz','BY-SA 2.0',9.00,'["清淡"]','["LUNCH","DINNER"]',83,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (83,'土豆','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_084 凉拌金针菇
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (84,'DISH_084',2,'凉拌金针菇','凉拌金针菇，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-084.jpg','https://www.flickr.com/photos/47038415@N00/219115984','Augapfel','BY 2.0',11.00,'["清淡"]','["LUNCH","DINNER"]',84,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (84,'金针菇','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_085 皮蛋豆腐
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (85,'DISH_085',2,'皮蛋豆腐','皮蛋豆腐，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-085.jpg','https://www.flickr.com/photos/47038415@N00/248294003','Augapfel','BY 2.0',13.00,'["家常"]','["LUNCH","DINNER"]',85,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (85,'北豆腐','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_086 老醋花生
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (86,'DISH_086',2,'老醋花生','老醋花生，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-086.jpg','https://www.flickr.com/photos/10559879@N00/2461262750','avlxyz','BY-SA 2.0',15.00,'["家常"]','["LUNCH","DINNER"]',86,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (86,'花生米','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_087 凉拌鸡丝
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (87,'DISH_087',2,'凉拌鸡丝','凉拌鸡丝，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-087.jpg','https://www.flickr.com/photos/10559879@N00/2461262750','avlxyz','BY-SA 2.0',25.00,'["清淡"]','["LUNCH","DINNER"]',87,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (87,'鸡胸肉','肉禽水产',300.00,'g','FIXED',1);
-- TEMPLATE DISH_088 口水鸡
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (88,'DISH_088',2,'口水鸡','口水鸡，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-088.jpg','https://www.flickr.com/photos/10559879@N00/2952381609','avlxyz','BY-SA 2.0',17.00,'["家常"]','["LUNCH","DINNER"]',88,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (88,'鸡腿','肉禽水产',2.00,'个','FIXED',1);
-- TEMPLATE DISH_089 白切肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (89,'DISH_089',2,'白切肉','白切肉，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-089.jpg','https://www.flickr.com/photos/129851880@N07/26847459523','羽諾','BY-SA 2.0',19.00,'["清淡"]','["LUNCH","DINNER"]',89,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (89,'五花肉','肉禽水产',400.00,'g','FIXED',1);
-- TEMPLATE DISH_090 蒜泥白肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (90,'DISH_090',2,'蒜泥白肉','蒜泥白肉，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-090.jpg','https://www.flickr.com/photos/10559879@N00/2353320414','avlxyz','BY-SA 2.0',21.00,'["家常"]','["LUNCH","DINNER"]',90,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (90,'五花肉','肉禽水产',400.00,'g','FIXED',1);
-- TEMPLATE DISH_091 凉拌牛肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (91,'DISH_091',2,'凉拌牛肉','凉拌牛肉，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-091.jpg','https://commons.wikimedia.org/wiki/File%3A%E8%8A%A5%E8%8F%9C%E7%BE%8A%E8%82%89_Mustard_Green%2BLamb%2C_%E9%A6%99%E8%8F%87%E7%89%9B%E8%82%89_Mushroom%2BBeef%2C_%E7%B4%A0%E4%B8%89%E9%B2%9C_Vegetarian_%E9%A5%BA%E5%AD%90_Dumplings_-_%E8%80%81%E5%A4%A7%E5%A8%98%E6%B0%B4%E9%A5%BA%E5%BA%97_Laodaniang_Dumpling_Shop.jpg','avlxyz','CC BY-SA 2.0',23.00,'["清淡"]','["LUNCH","DINNER"]',91,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (91,'牛肉','肉禽水产',300.00,'g','FIXED',1);
-- TEMPLATE DISH_092 夫妻肺片
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (92,'DISH_092',2,'夫妻肺片','夫妻肺片，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-092.jpg','https://www.flickr.com/photos/91049143@N00/15953315094','bryan...','BY-SA 2.0',17.00,'["家常"]','["LUNCH","DINNER"]',92,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (92,'时令蔬菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_093 糖拌西红柿
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (93,'DISH_093',2,'糖拌西红柿','糖拌西红柿，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-093.jpg','https://www.flickr.com/photos/10559879@N00/2461262750','avlxyz','BY-SA 2.0',9.00,'["家常"]','["LUNCH","DINNER"]',93,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (93,'番茄','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_094 果仁菠菜
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (94,'DISH_094',2,'果仁菠菜','果仁菠菜，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-094.jpg','https://www.flickr.com/photos/10559879@N00/2461262750','avlxyz','BY-SA 2.0',11.00,'["家常"]','["LUNCH","DINNER"]',94,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (94,'菠菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_095 东北大拉皮
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (95,'DISH_095',2,'东北大拉皮','东北大拉皮，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-095.jpg','https://commons.wikimedia.org/wiki/File%3ADong_bei_da_la_pi.jpg','chengzhu','CC0',13.00,'["家常"]','["LUNCH","DINNER"]',95,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (95,'时令蔬菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_096 凉拌秋葵
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (96,'DISH_096',2,'凉拌秋葵','凉拌秋葵，清爽开胃，适合作为家庭餐桌配菜。','/images/dish-templates/dish-096.jpg','https://www.flickr.com/photos/10559879@N00/2461262750','avlxyz','BY-SA 2.0',15.00,'["清淡"]','["LUNCH","DINNER"]',96,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (96,'秋葵','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_097 紫菜蛋花汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (97,'DISH_097',3,'紫菜蛋花汤','紫菜蛋花汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-097.jpg','https://www.flickr.com/photos/76958594@N04/7762437314','D11xzj','BY 2.0',10.00,'["家常"]','["LUNCH","DINNER"]',97,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (97,'鸡蛋','肉禽水产',3.00,'个','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (97,'紫菜','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_098 西红柿鸡蛋汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (98,'DISH_098',3,'西红柿鸡蛋汤','西红柿鸡蛋汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-098.jpg','https://www.flickr.com/photos/10559879@N00/521426140','avlxyz','BY-SA 2.0',20.00,'["家常"]','["LUNCH","DINNER"]',98,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (98,'番茄','蔬菜及其他',200.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (98,'鸡蛋','肉禽水产',3.00,'个','FIXED',2);
-- TEMPLATE DISH_099 冬瓜排骨汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (99,'DISH_099',3,'冬瓜排骨汤','冬瓜排骨汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-099.jpg','https://www.flickr.com/photos/10559879@N00/521426140','avlxyz','BY-SA 2.0',22.00,'["家常"]','["LUNCH","DINNER"]',99,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (99,'猪肋排','蔬菜及其他',500.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (99,'冬瓜','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_100 玉米排骨汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (100,'DISH_100',3,'玉米排骨汤','玉米排骨汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-100.jpg','https://commons.wikimedia.org/w/index.php?curid=48631831','Yinsanhen','BY-SA 4.0',24.00,'["家常"]','["LUNCH","DINNER"]',100,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (100,'猪肋排','蔬菜及其他',500.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (100,'甜玉米','粮油主食',250.00,'g','FIXED',2);
-- TEMPLATE DISH_101 莲藕排骨汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (101,'DISH_101',3,'莲藕排骨汤','莲藕排骨汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-101.jpg','https://www.flickr.com/photos/71834709@N00/8546361421','Colin ZHU','BY-SA 2.0',26.00,'["家常"]','["LUNCH","DINNER"]',101,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (101,'猪肋排','蔬菜及其他',500.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (101,'莲藕','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_102 海带排骨汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (102,'DISH_102',3,'海带排骨汤','海带排骨汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-102.jpg','https://www.flickr.com/photos/10559879@N00/521426140','avlxyz','BY-SA 2.0',18.00,'["家常"]','["LUNCH","DINNER"]',102,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (102,'猪肋排','蔬菜及其他',500.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (102,'海带','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_103 山药排骨汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (103,'DISH_103',3,'山药排骨汤','山药排骨汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-103.jpg','https://www.flickr.com/photos/10559879@N00/521426140','avlxyz','BY-SA 2.0',20.00,'["家常"]','["LUNCH","DINNER"]',103,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (103,'猪肋排','蔬菜及其他',500.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (103,'山药','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_104 萝卜排骨汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (104,'DISH_104',3,'萝卜排骨汤','萝卜排骨汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-104.jpg','https://www.flickr.com/photos/76958594@N04/7275807388','D11xzj','BY 2.0',22.00,'["家常"]','["LUNCH","DINNER"]',104,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (104,'猪肋排','蔬菜及其他',500.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (104,'白萝卜','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_105 菌菇鸡汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (105,'DISH_105',3,'菌菇鸡汤','菌菇鸡汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-105.jpg','https://www.flickr.com/photos/10559879@N00/521426140','avlxyz','BY-SA 2.0',24.00,'["家常"]','["LUNCH","DINNER"]',105,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (105,'整鸡','肉禽水产',1.00,'只','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (105,'混合菌菇','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_106 香菇炖鸡汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (106,'DISH_106',3,'香菇炖鸡汤','香菇炖鸡汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-106.jpg','https://www.flickr.com/photos/10559879@N00/521426140','avlxyz','BY-SA 2.0',26.00,'["醇香"]','["LUNCH","DINNER"]',106,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (106,'整鸡','肉禽水产',1.00,'只','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (106,'鲜香菇','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_107 番茄牛腩汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (107,'DISH_107',3,'番茄牛腩汤','番茄牛腩汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-107.jpg','https://www.flickr.com/photos/10559879@N00/521426140','avlxyz','BY-SA 2.0',26.00,'["家常"]','["LUNCH","DINNER"]',107,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (107,'牛腩','蔬菜及其他',500.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (107,'番茄','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_108 萝卜牛肉汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (108,'DISH_108',3,'萝卜牛肉汤','萝卜牛肉汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-108.jpg','https://www.flickr.com/photos/10559879@N00/521426140','avlxyz','BY-SA 2.0',20.00,'["家常"]','["LUNCH","DINNER"]',108,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (108,'牛肉','肉禽水产',300.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (108,'白萝卜','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_109 鲫鱼豆腐汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (109,'DISH_109',3,'鲫鱼豆腐汤','鲫鱼豆腐汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-109.jpg','https://www.flickr.com/photos/10559879@N00/521426140','avlxyz','BY-SA 2.0',22.00,'["家常"]','["LUNCH","DINNER"]',109,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (109,'鲫鱼','肉禽水产',1.00,'条','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (109,'北豆腐','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_110 丝瓜蛋汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (110,'DISH_110',3,'丝瓜蛋汤','丝瓜蛋汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-110.jpg','https://www.flickr.com/photos/10559879@N00/885682730','avlxyz','BY-SA 2.0',16.00,'["家常"]','["LUNCH","DINNER"]',110,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (110,'丝瓜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_111 冬瓜虾皮汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (111,'DISH_111',3,'冬瓜虾皮汤','冬瓜虾皮汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-111.jpg','https://www.flickr.com/photos/10559879@N00/521426140','avlxyz','BY-SA 2.0',26.00,'["家常"]','["LUNCH","DINNER"]',111,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (111,'虾皮','肉禽水产',200.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (111,'冬瓜','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_112 榨菜肉丝汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (112,'DISH_112',3,'榨菜肉丝汤','榨菜肉丝汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-112.jpg','https://www.flickr.com/photos/10559879@N00/827971877','avlxyz','BY-SA 2.0',18.00,'["家常"]','["LUNCH","DINNER"]',112,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (112,'猪里脊','蔬菜及其他',300.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (112,'榨菜','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_113 酸辣汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (113,'DISH_113',3,'酸辣汤','酸辣汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-113.jpg','https://www.flickr.com/photos/10559879@N00/2416274752','avlxyz','BY-SA 2.0',12.00,'["香辣"]','["LUNCH","DINNER"]',113,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (113,'清水','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_114 家常豆腐羹
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (114,'DISH_114',3,'家常豆腐羹','家常豆腐羹，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-114.jpg','https://www.flickr.com/photos/10559879@N00/521426140','avlxyz','BY-SA 2.0',14.00,'["家常"]','["LUNCH","DINNER"]',114,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (114,'北豆腐','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_115 银耳莲子羹
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (115,'DISH_115',3,'银耳莲子羹','银耳莲子羹，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-115.jpg','https://www.flickr.com/photos/10559879@N00/521426140','avlxyz','BY-SA 2.0',16.00,'["家常"]','["LUNCH","DINNER"]',115,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (115,'银耳','蔬菜及其他',200.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (115,'莲子','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_116 绿豆汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (116,'DISH_116',3,'绿豆汤','绿豆汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-116.jpg','https://www.flickr.com/photos/33562248@N00/5922646361','Metaphox','BY 2.0',18.00,'["家常"]','["LUNCH","DINNER"]',116,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (116,'绿豆','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_117 红豆汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (117,'DISH_117',3,'红豆汤','红豆汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-117.jpg','https://www.flickr.com/photos/34353636@N00/5671801572','liewcf','BY-SA 2.0',10.00,'["家常"]','["LUNCH","DINNER"]',117,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (117,'红豆','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_118 羊肉萝卜汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (118,'DISH_118',3,'羊肉萝卜汤','羊肉萝卜汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-118.jpg','https://www.flickr.com/photos/10559879@N00/521426140','avlxyz','BY-SA 2.0',20.00,'["家常"]','["LUNCH","DINNER"]',118,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (118,'白萝卜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_119 西湖牛肉羹
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (119,'DISH_119',3,'西湖牛肉羹','西湖牛肉羹，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-119.jpg','https://www.flickr.com/photos/47038415@N00/370735171','Augapfel','BY 2.0',22.00,'["家常"]','["LUNCH","DINNER"]',119,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (119,'牛肉','肉禽水产',300.00,'g','FIXED',1);
-- TEMPLATE DISH_120 宋嫂鱼羹
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (120,'DISH_120',3,'宋嫂鱼羹','宋嫂鱼羹，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-120.jpg','https://www.flickr.com/photos/10559879@N00/521426140','avlxyz','BY-SA 2.0',24.00,'["家常"]','["LUNCH","DINNER"]',120,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (120,'鲈鱼','肉禽水产',1.00,'条','FIXED',1);
-- TEMPLATE DISH_121 白菜豆腐汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (121,'DISH_121',3,'白菜豆腐汤','白菜豆腐汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-121.jpg','https://www.flickr.com/photos/10559879@N00/76394568','avlxyz','BY-SA 2.0',18.00,'["家常"]','["LUNCH","DINNER"]',121,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (121,'北豆腐','蔬菜及其他',200.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (121,'大白菜','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_122 青菜肉丸汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (122,'DISH_122',3,'青菜肉丸汤','青菜肉丸汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-122.jpg','https://www.flickr.com/photos/10559879@N00/521426140','avlxyz','BY-SA 2.0',18.00,'["家常"]','["LUNCH","DINNER"]',122,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (122,'小青菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_123 海鲜菇豆腐汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (123,'DISH_123',3,'海鲜菇豆腐汤','海鲜菇豆腐汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-123.jpg','https://www.flickr.com/photos/10559879@N00/521426140','avlxyz','BY-SA 2.0',12.00,'["家常"]','["LUNCH","DINNER"]',123,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (123,'海鲜菇','蔬菜及其他',200.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (123,'北豆腐','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_124 罗宋汤
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (124,'DISH_124',3,'罗宋汤','罗宋汤，汤味温和，食材搭配适合日常家庭餐。','/images/dish-templates/dish-124.jpg','https://commons.wikimedia.org/wiki/File%3ABorscht_with_cream.jpg','Kelly Sue DeConnick','CC BY-SA 2.0',14.00,'["家常"]','["LUNCH","DINNER"]',124,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (124,'清水','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_125 扬州炒饭
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (125,'DISH_125',4,'扬州炒饭','扬州炒饭，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-125.jpg','https://commons.wikimedia.org/wiki/File%3AChinese_fried_rice_by_stu_spivack_in_Cleveland%2C_OH.jpg','stu_spivack from C & Y Chinese Restaurant in Cleveland''s Chinatown, Ohio','CC BY-SA 2.0',8.00,'["家常"]','["LUNCH","DINNER"]',125,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (125,'米饭','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_126 蛋炒饭
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (126,'DISH_126',4,'蛋炒饭','蛋炒饭，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-126.jpg','https://www.flickr.com/photos/34353636@N00/4437550956','liewcf','BY-SA 2.0',10.00,'["家常"]','["LUNCH","DINNER"]',126,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (126,'米饭','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_127 腊肠炒饭
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (127,'DISH_127',4,'腊肠炒饭','腊肠炒饭，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-127.jpg','https://www.flickr.com/photos/22292214@N00/5611724473','andrewarchy','BY 2.0',12.00,'["家常"]','["LUNCH","DINNER"]',127,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (127,'米饭','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_128 咖喱炒饭
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (128,'DISH_128',4,'咖喱炒饭','咖喱炒饭，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-128.jpg','https://www.flickr.com/photos/22292214@N00/5611724473','andrewarchy','BY 2.0',14.00,'["家常"]','["LUNCH","DINNER"]',128,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (128,'米饭','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_129 番茄鸡蛋面
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (129,'DISH_129',4,'番茄鸡蛋面','番茄鸡蛋面，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-129.jpg','https://commons.wikimedia.org/wiki/File%3ATomato_and_egg_noodles.jpg','Zhuo1221','CC BY-SA 4.0',24.00,'["家常"]','["LUNCH","DINNER"]',129,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (129,'番茄','蔬菜及其他',200.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (129,'鸡蛋','肉禽水产',3.00,'个','FIXED',2);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (129,'鲜面条','粮油主食',250.00,'g','FIXED',3);
-- TEMPLATE DISH_130 葱油拌面
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (130,'DISH_130',4,'葱油拌面','葱油拌面，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-130.jpg','https://commons.wikimedia.org/wiki/File%3A%E8%91%B1%E8%8A%B1%E8%91%B1%E6%B2%B9%E6%8B%8C%E9%9D%A2.jpg','Suginami','CC0',8.00,'["家常"]','["LUNCH","DINNER"]',130,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (130,'鲜面条','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_131 炸酱面
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (131,'DISH_131',4,'炸酱面','炸酱面，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-131.jpg','https://www.flickr.com/photos/10559879@N00/3012466496','avlxyz','BY-SA 2.0',10.00,'["家常"]','["LUNCH","DINNER"]',131,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (131,'鲜面条','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_132 阳春面
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (132,'DISH_132',4,'阳春面','阳春面，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-132.jpg','https://commons.wikimedia.org/wiki/File%3AChineseNoodles.jpg','Jun','CC BY 2.0',12.00,'["家常"]','["LUNCH","DINNER"]',132,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (132,'鲜面条','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_133 牛肉面
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (133,'DISH_133',4,'牛肉面','牛肉面，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-133.jpg','https://www.flickr.com/photos/10559879@N00/2990569537','avlxyz','BY-SA 2.0',22.00,'["家常"]','["LUNCH","DINNER"]',133,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (133,'牛肉','肉禽水产',300.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (133,'鲜面条','粮油主食',250.00,'g','FIXED',2);
-- TEMPLATE DISH_134 排骨面
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (134,'DISH_134',4,'排骨面','排骨面，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-134.jpg','https://www.flickr.com/photos/10559879@N00/3449436271','avlxyz','BY-SA 2.0',24.00,'["家常"]','["LUNCH","DINNER"]',134,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (134,'猪肋排','蔬菜及其他',500.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (134,'鲜面条','粮油主食',250.00,'g','FIXED',2);
-- TEMPLATE DISH_135 雪菜肉丝面
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (135,'DISH_135',4,'雪菜肉丝面','雪菜肉丝面，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-135.jpg','https://www.flickr.com/photos/87117631@N00/8492450995','Gary Soup','BY 2.0',16.00,'["家常"]','["LUNCH","DINNER"]',135,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (135,'猪里脊','蔬菜及其他',300.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (135,'大米','粮油主食',250.00,'g','FIXED',2);
-- TEMPLATE DISH_136 酸辣粉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (136,'DISH_136',4,'酸辣粉','酸辣粉，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-136.jpg','https://www.flickr.com/photos/25368895@N00/6822582224','ayustety','BY-SA 2.0',10.00,'["香辣"]','["LUNCH","DINNER"]',136,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (136,'红薯粉','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_137 肉末米粉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (137,'DISH_137',4,'肉末米粉','肉末米粉，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-137.jpg','https://www.flickr.com/photos/22292214@N00/5611724473','andrewarchy','BY 2.0',20.00,'["家常"]','["LUNCH","DINNER"]',137,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (137,'猪肉末','肉禽水产',300.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (137,'米粉','粮油主食',250.00,'g','FIXED',2);
-- TEMPLATE DISH_138 鸡蛋炒面
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (138,'DISH_138',4,'鸡蛋炒面','鸡蛋炒面，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-138.jpg','https://www.flickr.com/photos/10559879@N00/885682730','avlxyz','BY-SA 2.0',22.00,'["家常"]','["LUNCH","DINNER"]',138,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (138,'鸡蛋','肉禽水产',3.00,'个','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (138,'鲜面条','粮油主食',250.00,'g','FIXED',2);
-- TEMPLATE DISH_139 肉丝炒面
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (139,'DISH_139',4,'肉丝炒面','肉丝炒面，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-139.jpg','https://www.flickr.com/photos/10559879@N00/3042702705','avlxyz','BY-SA 2.0',24.00,'["家常"]','["LUNCH","DINNER"]',139,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (139,'猪里脊','蔬菜及其他',300.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (139,'鲜面条','粮油主食',250.00,'g','FIXED',2);
-- TEMPLATE DISH_140 韭菜鸡蛋饺子
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (140,'DISH_140',4,'韭菜鸡蛋饺子','韭菜鸡蛋饺子，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-140.jpg','https://www.flickr.com/photos/22292214@N00/5611724473','andrewarchy','BY 2.0',16.00,'["家常"]','["LUNCH","DINNER"]',140,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (140,'鸡蛋','肉禽水产',3.00,'个','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (140,'韭菜','蔬菜及其他',200.00,'g','FIXED',2);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (140,'饺子皮','粮油主食',200.00,'g','FIXED',3);
-- TEMPLATE DISH_141 猪肉白菜饺子
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (141,'DISH_141',4,'猪肉白菜饺子','猪肉白菜饺子，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-141.jpg','https://commons.wikimedia.org/wiki/File%3ADumplings_(jiaozhi)_(Pork_and_napa_cabbage).jpg','NeoBatfreak','CC BY-SA 4.0',18.00,'["家常"]','["LUNCH","DINNER"]',141,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (141,'大白菜','蔬菜及其他',200.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (141,'饺子皮','粮油主食',200.00,'g','FIXED',2);
-- TEMPLATE DISH_142 牛肉馅饺子
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (142,'DISH_142',4,'牛肉馅饺子','牛肉馅饺子，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-142.jpg','https://www.flickr.com/photos/22292214@N00/5611724473','andrewarchy','BY 2.0',20.00,'["家常"]','["LUNCH","DINNER"]',142,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (142,'牛肉','肉禽水产',300.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (142,'饺子皮','粮油主食',200.00,'g','FIXED',2);
-- TEMPLATE DISH_143 鲜肉馄饨
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (143,'DISH_143',4,'鲜肉馄饨','鲜肉馄饨，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-143.jpg','https://www.flickr.com/photos/32384318@N06/6568489863','DvYang','BY-SA 2.0',22.00,'["家常"]','["LUNCH","DINNER"]',143,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (143,'馄饨皮','粮油主食',200.00,'g','FIXED',1);
-- TEMPLATE DISH_144 猪肉包子
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (144,'DISH_144',4,'猪肉包子','猪肉包子，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-144.jpg','https://www.flickr.com/photos/22292214@N00/5611724473','andrewarchy','BY 2.0',24.00,'["家常"]','["LUNCH","DINNER"]',144,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (144,'中筋面粉','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_145 花卷
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (145,'DISH_145',4,'花卷','花卷，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-145.jpg','https://www.flickr.com/photos/52582306@N03/9207628982','阿橋花譜 KHQ Flower Guide','BY-SA 2.0',8.00,'["家常"]','["LUNCH","DINNER"]',145,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (145,'中筋面粉','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_146 葱油饼
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (146,'DISH_146',4,'葱油饼','葱油饼，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-146.jpg','https://www.flickr.com/photos/7527824@N04/5440935619','timquijano','BY 2.0',10.00,'["家常"]','["LUNCH","DINNER"]',146,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (146,'中筋面粉','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_147 南瓜馒头
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (147,'DISH_147',4,'南瓜馒头','南瓜馒头，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-147.jpg','https://commons.wikimedia.org/wiki/File%3AGlochidion_puberum_02.JPG','Vinayaraj','CC BY-SA 3.0',12.00,'["家常"]','["LUNCH","DINNER"]',147,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (147,'南瓜','蔬菜及其他',200.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (147,'中筋面粉','粮油主食',250.00,'g','FIXED',2);
-- TEMPLATE DISH_148 皮蛋瘦肉粥
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (148,'DISH_148',4,'皮蛋瘦肉粥','皮蛋瘦肉粥，饱腹实在，适合家庭日常主食。','/images/dish-templates/dish-148.jpg','https://www.flickr.com/photos/50642338@N00/245032510','sfllaw','BY-SA 2.0',22.00,'["清淡"]','["LUNCH","DINNER"]',148,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (148,'大米','粮油主食',250.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (148,'大米','粮油主食',250.00,'g','FIXED',2);
-- TEMPLATE DISH_149 小米粥
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (149,'DISH_149',5,'小米粥','小米粥，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-149.jpg','https://www.flickr.com/photos/76958594@N04/7356102222','D11xzj','BY 2.0',6.00,'["清淡"]','["BREAKFAST"]',149,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (149,'大米','粮油主食',250.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (149,'小米','粮油主食',250.00,'g','FIXED',2);
-- TEMPLATE DISH_150 南瓜粥
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (150,'DISH_150',5,'南瓜粥','南瓜粥，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-150.jpg','https://www.flickr.com/photos/13293168@N06/6839065964','caishin','BY-SA 2.0',8.00,'["清淡"]','["BREAKFAST"]',150,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (150,'南瓜','蔬菜及其他',200.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (150,'大米','粮油主食',250.00,'g','FIXED',2);
-- TEMPLATE DISH_151 八宝粥
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (151,'DISH_151',5,'八宝粥','八宝粥，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-151.jpg','https://www.flickr.com/photos/10559879@N00/2371077526','avlxyz','BY-SA 2.0',10.00,'["清淡"]','["BREAKFAST"]',151,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (151,'大米','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_152 燕麦牛奶粥
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (152,'DISH_152',5,'燕麦牛奶粥','燕麦牛奶粥，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-152.jpg','https://www.flickr.com/photos/48973657@N00/8520765973','See-ming Lee (SML)','BY 2.0',20.00,'["清淡"]','["BREAKFAST"]',152,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (152,'大米','粮油主食',250.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (152,'燕麦片','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_153 豆浆油条
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (153,'DISH_153',5,'豆浆油条','豆浆油条，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-153.jpg','https://www.flickr.com/photos/38766930@N05/33462761634','profernity','BY 2.0',14.00,'["家常"]','["BREAKFAST"]',153,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (153,'中筋面粉','粮油主食',250.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (153,'黄豆','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_154 茶叶蛋
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (154,'DISH_154',5,'茶叶蛋','茶叶蛋，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-154.jpg','https://www.flickr.com/photos/25698914@N05/7887334904','ahenobarbus','BY-SA 2.0',6.00,'["家常"]','["BREAKFAST"]',154,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (154,'鸡蛋','肉禽水产',3.00,'个','FIXED',1);
-- TEMPLATE DISH_155 煎鸡蛋
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (155,'DISH_155',5,'煎鸡蛋','煎鸡蛋，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-155.jpg','https://www.flickr.com/photos/13612227@N02/49923850563','Dana L. Brown','BY-SA 2.0',16.00,'["家常"]','["BREAKFAST"]',155,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (155,'鸡蛋','肉禽水产',3.00,'个','FIXED',1);
-- TEMPLATE DISH_156 火腿鸡蛋三明治
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (156,'DISH_156',5,'火腿鸡蛋三明治','火腿鸡蛋三明治，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-156.jpg','https://www.flickr.com/photos/48973657@N00/8520765973','See-ming Lee (SML)','BY 2.0',18.00,'["家常"]','["BREAKFAST"]',156,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (156,'鸡蛋','肉禽水产',3.00,'个','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (156,'吐司','蔬菜及其他',200.00,'g','FIXED',2);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (156,'火腿片','蔬菜及其他',200.00,'g','FIXED',3);
-- TEMPLATE DISH_157 牛油果鸡蛋吐司
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (157,'DISH_157',5,'牛油果鸡蛋吐司','牛油果鸡蛋吐司，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-157.jpg','https://www.flickr.com/photos/48973657@N00/8520765973','See-ming Lee (SML)','BY 2.0',20.00,'["家常"]','["BREAKFAST"]',157,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (157,'鸡蛋','肉禽水产',3.00,'个','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (157,'全麦吐司','蔬菜及其他',200.00,'g','FIXED',2);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (157,'牛油果','蔬菜及其他',200.00,'g','FIXED',3);
-- TEMPLATE DISH_158 鸡蛋灌饼
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (158,'DISH_158',5,'鸡蛋灌饼','鸡蛋灌饼，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-158.jpg','https://www.flickr.com/photos/28355684@N06/3558407996','大杨','BY 2.0',22.00,'["家常"]','["BREAKFAST"]',158,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (158,'鸡蛋','肉禽水产',3.00,'个','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (158,'中筋面粉','粮油主食',250.00,'g','FIXED',2);
-- TEMPLATE DISH_159 手抓饼
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (159,'DISH_159',5,'手抓饼','手抓饼，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-159.jpg','https://www.flickr.com/photos/32300607@N05/5311788306','jerryluo0520','BY 2.0',6.00,'["家常"]','["BREAKFAST"]',159,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (159,'中筋面粉','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_160 紫菜饭团
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (160,'DISH_160',5,'紫菜饭团','紫菜饭团，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-160.jpg','https://www.flickr.com/photos/42438955@N05/13885509804','KOREA.NET - Official page of the Republic of Korea','BY-SA 2.0',8.00,'["家常"]','["BREAKFAST"]',160,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (160,'紫菜','蔬菜及其他',200.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (160,'米饭','粮油主食',250.00,'g','FIXED',2);
-- TEMPLATE DISH_161 肉松饭团
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (161,'DISH_161',5,'肉松饭团','肉松饭团，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-161.jpg','https://www.flickr.com/photos/10559879@N00/3835982669','avlxyz','BY-SA 2.0',18.00,'["家常"]','["BREAKFAST"]',161,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (161,'米饭','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_162 玉米鸡蛋饼
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (162,'DISH_162',5,'玉米鸡蛋饼','玉米鸡蛋饼，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-162.jpg','https://www.flickr.com/photos/48973657@N00/8520765973','See-ming Lee (SML)','BY 2.0',20.00,'["家常"]','["BREAKFAST"]',162,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (162,'鸡蛋','肉禽水产',3.00,'个','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (162,'甜玉米','粮油主食',250.00,'g','FIXED',2);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (162,'中筋面粉','粮油主食',250.00,'g','FIXED',3);
-- TEMPLATE DISH_163 香蕉松饼
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (163,'DISH_163',5,'香蕉松饼','香蕉松饼，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-163.jpg','https://www.flickr.com/photos/48973657@N00/8520765973','See-ming Lee (SML)','BY 2.0',14.00,'["家常"]','["BREAKFAST"]',163,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (163,'中筋面粉','粮油主食',250.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (163,'香蕉','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_164 蒸红薯
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (164,'DISH_164',5,'蒸红薯','蒸红薯，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-164.jpg','https://commons.wikimedia.org/wiki/File%3ASteamed_dried_sweet_potato.jpg','Fumikas Sagisavas','CC0',6.00,'["清淡"]','["BREAKFAST"]',164,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (164,'红薯','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_165 蒸玉米
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (165,'DISH_165',5,'蒸玉米','蒸玉米，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-165.jpg','https://www.flickr.com/photos/91049143@N00/14859997855','bryan...','BY-SA 2.0',8.00,'["清淡"]','["BREAKFAST"]',165,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (165,'甜玉米','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_166 酒酿圆子
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (166,'DISH_166',5,'酒酿圆子','酒酿圆子，简单营养，适合作为家庭早餐。','/images/dish-templates/dish-166.jpg','https://www.flickr.com/photos/48973657@N00/8520765973','See-ming Lee (SML)','BY 2.0',10.00,'["家常"]','["BREAKFAST"]',166,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (166,'酒酿','蔬菜及其他',200.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (166,'糯米小圆子','粮油主食',250.00,'g','FIXED',2);
-- TEMPLATE DISH_167 虾仁蒸蛋
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (167,'DISH_167',6,'虾仁蒸蛋','虾仁蒸蛋，口感柔和，适合家庭儿童餐搭配。','/images/dish-templates/dish-167.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',20.00,'["清淡"]','["LUNCH","DINNER"]',167,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (167,'虾仁','肉禽水产',250.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (167,'鸡蛋','肉禽水产',3.00,'个','FIXED',2);
-- TEMPLATE DISH_168 番茄肉酱意面
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (168,'DISH_168',6,'番茄肉酱意面','番茄肉酱意面，口感柔和，适合家庭儿童餐搭配。','/images/dish-templates/dish-168.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',22.00,'["家常"]','["LUNCH","DINNER"]',168,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (168,'番茄','蔬菜及其他',200.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (168,'意大利面','粮油主食',250.00,'g','FIXED',2);
-- TEMPLATE DISH_169 奶香土豆泥
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (169,'DISH_169',6,'奶香土豆泥','奶香土豆泥，口感柔和，适合家庭儿童餐搭配。','/images/dish-templates/dish-169.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',16.00,'["家常"]','["LUNCH","DINNER"]',169,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (169,'土豆','蔬菜及其他',200.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (169,'纯牛奶','蛋奶',200.00,'g','FIXED',2);
-- TEMPLATE DISH_170 南瓜蒸蛋
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (170,'DISH_170',6,'南瓜蒸蛋','南瓜蒸蛋，口感柔和，适合家庭儿童餐搭配。','/images/dish-templates/dish-170.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',18.00,'["清淡"]','["LUNCH","DINNER"]',170,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (170,'鸡蛋','肉禽水产',3.00,'个','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (170,'南瓜','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_171 蔬菜鸡肉丸
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (171,'DISH_171',6,'蔬菜鸡肉丸','蔬菜鸡肉丸，口感柔和，适合家庭儿童餐搭配。','/images/dish-templates/dish-171.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',28.00,'["家常"]','["LUNCH","DINNER"]',171,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (171,'时令蔬菜','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_172 菠萝咕咾肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (172,'DISH_172',6,'菠萝咕咾肉','菠萝咕咾肉，口感柔和，适合家庭儿童餐搭配。','/images/dish-templates/dish-172.jpg','https://commons.wikimedia.org/wiki/File%3AUncooked_raw_sweet_and_sour_pork_with_pineapple.jpg','Fumikas Sagisavas','CC0',20.00,'["酸甜"]','["LUNCH","DINNER"]',172,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (172,'猪里脊','蔬菜及其他',300.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (172,'菠萝','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_173 彩椒牛肉粒
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (173,'DISH_173',6,'彩椒牛肉粒','彩椒牛肉粒，口感柔和，适合家庭儿童餐搭配。','/images/dish-templates/dish-173.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',22.00,'["家常"]','["LUNCH","DINNER"]',173,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (173,'牛肉','肉禽水产',300.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (173,'彩椒','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_174 可乐鸡腿
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (174,'DISH_174',6,'可乐鸡腿','可乐鸡腿，口感柔和，适合家庭儿童餐搭配。','/images/dish-templates/dish-174.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',24.00,'["家常"]','["LUNCH","DINNER"]',174,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (174,'鸡腿','肉禽水产',2.00,'个','FIXED',1);
-- TEMPLATE DISH_175 奶香玉米
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (175,'DISH_175',6,'奶香玉米','奶香玉米，口感柔和，适合家庭儿童餐搭配。','/images/dish-templates/dish-175.jpg','https://www.flickr.com/photos/91049143@N00/20435153748','bryan...','BY-SA 2.0',18.00,'["家常"]','["LUNCH","DINNER"]',175,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (175,'甜玉米','粮油主食',250.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (175,'纯牛奶','蛋奶',200.00,'g','FIXED',2);
-- TEMPLATE DISH_176 香煎鳕鱼
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (176,'DISH_176',6,'香煎鳕鱼','香煎鳕鱼，口感柔和，适合家庭儿童餐搭配。','/images/dish-templates/dish-176.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',28.00,'["家常"]','["LUNCH","DINNER"]',176,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (176,'鳕鱼','肉禽水产',300.00,'g','FIXED',1);
-- TEMPLATE DISH_177 鸡胸肉沙拉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (177,'DISH_177',7,'鸡胸肉沙拉','鸡胸肉沙拉，搭配均衡，适合清爽少负担的一餐。','/images/dish-templates/dish-177.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',22.00,'["清淡"]','["LUNCH","DINNER"]',177,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (177,'鸡胸肉','肉禽水产',300.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (177,'混合生菜','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_178 牛肉藜麦饭
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (178,'DISH_178',7,'牛肉藜麦饭','牛肉藜麦饭，搭配均衡，适合清爽少负担的一餐。','/images/dish-templates/dish-178.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',24.00,'["家常"]','["LUNCH","DINNER"]',178,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (178,'牛肉','肉禽水产',300.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (178,'藜麦','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_179 虾仁藜麦沙拉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (179,'DISH_179',7,'虾仁藜麦沙拉','虾仁藜麦沙拉，搭配均衡，适合清爽少负担的一餐。','/images/dish-templates/dish-179.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',26.00,'["清淡"]','["LUNCH","DINNER"]',179,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (179,'虾仁','肉禽水产',250.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (179,'混合生菜','蔬菜及其他',200.00,'g','FIXED',2);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (179,'藜麦','蔬菜及其他',200.00,'g','FIXED',3);
-- TEMPLATE DISH_180 西兰花鸡胸肉
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (180,'DISH_180',7,'西兰花鸡胸肉','西兰花鸡胸肉，搭配均衡，适合清爽少负担的一餐。','/images/dish-templates/dish-180.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',28.00,'["家常"]','["LUNCH","DINNER"]',180,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (180,'鸡胸肉','肉禽水产',300.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (180,'西兰花','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_181 清蒸鳕鱼
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (181,'DISH_181',7,'清蒸鳕鱼','清蒸鳕鱼，搭配均衡，适合清爽少负担的一餐。','/images/dish-templates/dish-181.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',30.00,'["清淡"]','["LUNCH","DINNER"]',181,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (181,'鳕鱼','肉禽水产',300.00,'g','FIXED',1);
-- TEMPLATE DISH_182 香煎三文鱼
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (182,'DISH_182',7,'香煎三文鱼','香煎三文鱼，搭配均衡，适合清爽少负担的一餐。','/images/dish-templates/dish-182.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',30.00,'["家常"]','["LUNCH","DINNER"]',182,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (182,'三文鱼','肉禽水产',300.00,'g','FIXED',1);
-- TEMPLATE DISH_183 蒸南瓜山药
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (183,'DISH_183',7,'蒸南瓜山药','蒸南瓜山药，搭配均衡，适合清爽少负担的一餐。','/images/dish-templates/dish-183.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',16.00,'["清淡"]','["LUNCH","DINNER"]',183,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (183,'山药','蔬菜及其他',200.00,'g','FIXED',1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (183,'南瓜','蔬菜及其他',200.00,'g','FIXED',2);
-- TEMPLATE DISH_184 蔬菜豆腐煲
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (184,'DISH_184',7,'蔬菜豆腐煲','蔬菜豆腐煲，搭配均衡，适合清爽少负担的一餐。','/images/dish-templates/dish-184.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',18.00,'["家常"]','["LUNCH","DINNER"]',184,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (184,'北豆腐','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_185 杂粮饭
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (185,'DISH_185',7,'杂粮饭','杂粮饭，搭配均衡，适合清爽少负担的一餐。','/images/dish-templates/dish-185.jpg','https://www.flickr.com/photos/10559879@N00/2780369515','avlxyz','BY-SA 2.0',20.00,'["家常"]','["LUNCH","DINNER"]',185,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (185,'杂粮米','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_186 凉拌荞麦面
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (186,'DISH_186',7,'凉拌荞麦面','凉拌荞麦面，搭配均衡，适合清爽少负担的一餐。','/images/dish-templates/dish-186.jpg','https://commons.wikimedia.org/wiki/File%3A%E8%8D%9E%E9%BA%A6%E5%87%89%E6%8B%8C%E9%9D%A2.jpg','XUEMEI SUN','CC BY-SA 4.0',22.00,'["清淡"]','["LUNCH","DINNER"]',186,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (186,'荞麦面','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_187 鲜肉锅贴
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (187,'DISH_187',8,'鲜肉锅贴','鲜肉锅贴，家常风味小吃，适合加餐或搭配正餐。','/images/dish-templates/dish-187.jpg','https://www.flickr.com/photos/79721788@N00/40693892792','D-Stanley','BY 2.0',14.00,'["家常"]','["LUNCH","DINNER"]',187,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (187,'饺子皮','粮油主食',200.00,'g','FIXED',1);
-- TEMPLATE DISH_188 上海生煎包
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (188,'DISH_188',8,'上海生煎包','上海生煎包，家常风味小吃，适合加餐或搭配正餐。','/images/dish-templates/dish-188.jpg','https://www.flickr.com/photos/91049143@N00/23590608472','bryan...','BY-SA 2.0',8.00,'["家常"]','["LUNCH","DINNER"]',188,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (188,'中筋面粉','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_189 红糖糍粑
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (189,'DISH_189',8,'红糖糍粑','红糖糍粑，家常风味小吃，适合加餐或搭配正餐。','/images/dish-templates/dish-189.jpg','https://commons.wikimedia.org/wiki/File%3AWalmart_Marketside_exploding_brown_sugar_glutinous_rice_cake.jpg','Fumikas Sagisavas','CC0',10.00,'["酸甜"]','["LUNCH","DINNER"]',189,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (189,'糯米','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_190 三丝春卷
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (190,'DISH_190',8,'三丝春卷','三丝春卷，家常风味小吃，适合加餐或搭配正餐。','/images/dish-templates/dish-190.jpg','https://www.flickr.com/photos/79721788@N00/40693892792','D-Stanley','BY 2.0',12.00,'["家常"]','["LUNCH","DINNER"]',190,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (190,'春卷皮','粮油主食',200.00,'g','FIXED',1);
-- TEMPLATE DISH_191 腊汁肉夹馍
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (191,'DISH_191',8,'腊汁肉夹馍','腊汁肉夹馍，家常风味小吃，适合加餐或搭配正餐。','/images/dish-templates/dish-191.jpg','https://commons.wikimedia.org/wiki/File%3A%E5%8F%A4%E5%9F%8E%E6%A8%8A%E6%B0%8F%E8%85%8A%E6%B1%81%E8%82%89%E5%A4%B9%E9%A6%8D.JPG','Steetsweet','CC BY-SA 4.0',22.00,'["家常"]','["LUNCH","DINNER"]',191,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (191,'猪前腿肉','肉禽水产',350.00,'g','FIXED',1);
-- TEMPLATE DISH_192 陕西凉皮
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (192,'DISH_192',8,'陕西凉皮','陕西凉皮，家常风味小吃，适合加餐或搭配正餐。','/images/dish-templates/dish-192.jpg','https://commons.wikimedia.org/wiki/File%3AShanxi_Liangpi_%E9%99%95%E8%A5%BF_%E5%87%89%E7%9A%AE.jpg','ウィキ太郎 ( Wiki Taro )','Public domain',6.00,'["香辣"]','["LUNCH","DINNER"]',192,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (192,'凉皮','粮油主食',200.00,'g','FIXED',1);
-- TEMPLATE DISH_193 糖油粑粑
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (193,'DISH_193',8,'糖油粑粑','糖油粑粑，家常风味小吃，适合加餐或搭配正餐。','/images/dish-templates/dish-193.jpg','https://commons.wikimedia.org/w/index.php?curid=48631831','Yinsanhen','BY-SA 4.0',8.00,'["酸甜"]','["LUNCH","DINNER"]',193,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (193,'糯米粉','粮油主食',250.00,'g','FIXED',1);
-- TEMPLATE DISH_194 香酥鸡柳
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (194,'DISH_194',8,'香酥鸡柳','香酥鸡柳，家常风味小吃，适合加餐或搭配正餐。','/images/dish-templates/dish-194.jpg','https://www.flickr.com/photos/79721788@N00/40693892792','D-Stanley','BY 2.0',18.00,'["家常"]','["LUNCH","DINNER"]',194,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (194,'鸡胸肉','肉禽水产',300.00,'g','FIXED',1);
-- TEMPLATE DISH_195 双皮奶
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (195,'DISH_195',9,'双皮奶','双皮奶，甜润适口，适合作为餐后甜品。','/images/dish-templates/dish-195.jpg','https://www.flickr.com/photos/9017514@N05/3423112618','West Zest','BY-SA 2.0',7.00,'["家常"]','["LUNCH","DINNER"]',195,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (195,'纯牛奶','蛋奶',200.00,'g','FIXED',1);
-- TEMPLATE DISH_196 杨枝甘露
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (196,'DISH_196',9,'杨枝甘露','杨枝甘露，甜润适口，适合作为餐后甜品。','/images/dish-templates/dish-196.jpg','https://commons.wikimedia.org/wiki/File%3AMango_pomelo_sago.jpg','relgar','CC BY 2.0',9.00,'["家常"]','["LUNCH","DINNER"]',196,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (196,'芒果','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_197 陈皮红豆沙
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (197,'DISH_197',9,'陈皮红豆沙','陈皮红豆沙，甜润适口，适合作为餐后甜品。','/images/dish-templates/dish-197.jpg','https://www.flickr.com/photos/71136117@N00/2508146881','chumsdock','BY-SA 2.0',11.00,'["家常"]','["LUNCH","DINNER"]',197,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (197,'红豆','蔬菜及其他',200.00,'g','FIXED',1);
-- TEMPLATE DISH_198 冰糖雪梨
INSERT INTO dish_templates (id,template_code,category_id,name,description,image_url,image_source_url,image_author,image_license,reference_price,taste_tags,meal_tags,sort_order,enabled) VALUES (198,'DISH_198',9,'冰糖雪梨','冰糖雪梨，甜润适口，适合作为餐后甜品。','/images/dish-templates/dish-198.jpg','https://www.flickr.com/photos/40394481@N03/9677644805','zoe.wang','BY 2.0',13.00,'["酸甜"]','["LUNCH","DINNER"]',198,1);
INSERT INTO dish_template_ingredients (template_id,ingredient_name,ingredient_category,quantity,unit,calc_type,sort_order) VALUES (198,'雪梨','蔬菜及其他',200.00,'g','FIXED',1);
