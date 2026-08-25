ALTER TABLE dishes
  ADD COLUMN featured_at datetime(6) NULL COMMENT '商户推荐时间' AFTER source_template_id,
  ADD KEY idx_dishes_merchant_featured (merchant_id,featured_at);

CREATE TEMPORARY TABLE merchant_featured_dish_backfill (
  merchant_id bigint NOT NULL,
  dish_id bigint NOT NULL,
  PRIMARY KEY (merchant_id,dish_id)
) ENGINE=InnoDB;

INSERT INTO merchant_featured_dish_backfill (merchant_id,dish_id)
SELECT ranked.merchant_id,ranked.dish_id
FROM (
  SELECT candidate.merchant_id,
         candidate.dish_id,
         ROW_NUMBER() OVER (
           PARTITION BY candidate.merchant_id
           ORDER BY candidate.family_reference_count DESC, candidate.dish_id DESC
         ) AS featured_rank
  FROM (
    SELECT f.merchant_id,
           f.featured_dish_id AS dish_id,
           COUNT(DISTINCT f.id) AS family_reference_count
    FROM families f
    JOIN dishes d
      ON d.id = f.featured_dish_id
     AND d.merchant_id = f.merchant_id
     AND d.status = 'active'
    WHERE f.status = 'active'
      AND f.featured_dish_id IS NOT NULL
    GROUP BY f.merchant_id,f.featured_dish_id
  ) candidate
) ranked
WHERE ranked.featured_rank <= 5;

UPDATE dishes d
JOIN merchant_featured_dish_backfill backfill
  ON backfill.merchant_id = d.merchant_id
 AND backfill.dish_id = d.id
SET d.featured_at = CURRENT_TIMESTAMP(6);

UPDATE family_menu_items fmi
JOIN families f
  ON f.id = fmi.family_id
 AND f.status = 'active'
JOIN merchant_featured_dish_backfill backfill
  ON backfill.merchant_id = f.merchant_id
 AND backfill.dish_id = fmi.dish_id
SET fmi.enabled = 1;

INSERT INTO family_menu_items (family_id,dish_id,enabled,sort_order,final_price)
SELECT f.id,
       d.id,
       1,
       COALESCE(existing_tail.max_sort_order, -1)
         + ROW_NUMBER() OVER (
             PARTITION BY f.id
             ORDER BY d.featured_at DESC, d.id DESC
           ),
       d.base_price
FROM families f
JOIN merchant_featured_dish_backfill backfill
  ON backfill.merchant_id = f.merchant_id
JOIN dishes d
  ON d.id = backfill.dish_id
 AND d.merchant_id = backfill.merchant_id
LEFT JOIN (
  SELECT family_id,MAX(sort_order) AS max_sort_order
  FROM family_menu_items
  GROUP BY family_id
) existing_tail ON existing_tail.family_id = f.id
WHERE f.status = 'active'
  AND NOT EXISTS (
    SELECT 1
    FROM family_menu_items existing_item
    WHERE existing_item.family_id = f.id
      AND existing_item.dish_id = d.id
  );

DROP TEMPORARY TABLE merchant_featured_dish_backfill;
