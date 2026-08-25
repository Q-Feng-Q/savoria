INSERT INTO meal_slots (family_id,name,display_time,sort_order,enabled)
SELECT f.id,'早餐','07:00',1,1
FROM families f
WHERE f.status='active'
  AND NOT EXISTS (SELECT 1 FROM meal_slots ms WHERE ms.family_id=f.id AND ms.name='早餐');

INSERT INTO meal_slots (family_id,name,display_time,sort_order,enabled)
SELECT f.id,'午餐','12:00',2,1
FROM families f
WHERE f.status='active'
  AND NOT EXISTS (SELECT 1 FROM meal_slots ms WHERE ms.family_id=f.id AND ms.name='午餐');

INSERT INTO meal_slots (family_id,name,display_time,sort_order,enabled)
SELECT f.id,'晚餐','18:30',3,1
FROM families f
WHERE f.status='active'
  AND NOT EXISTS (SELECT 1 FROM meal_slots ms WHERE ms.family_id=f.id AND ms.name='晚餐');
