-- ============================================================
-- 库存表脏数据清理脚本（wms_inventory）
-- 日期：2026-09-22
-- 背景：库存表因早期代码 bug（lockStock/unlockStock 的 batch_no WHERE 条件
--       在 Java 传 null 时匹配到所有空 batch_no 行，导致跨批次批量操作）
--       出现以下脏数据：
--       1. batch_no 被误写入数值（cost_price / subtotal 等），应为 NULL
--       2. locked_qty 出现负数（-3），应为 ≥ 0
--       3. quantity 出现负数（-1），应为 ≥ 0
--       4. available_qty 与 quantity - locked_qty 计算不符
--       5. 同 (仓库+SKU+库位+批次) 重复多行
--
-- 执行要求：
--   · 先在测试环境执行，确认无误再上生产
--   · 执行前**务必备份**：CREATE TABLE wms_inventory_bak_YYYYMMDD AS SELECT * FROM wms_inventory;
--   · MySQL ≥ 5.7 支持全部 SQL
-- ============================================================


-- ============================================================
-- 阶段一：脏数据诊断（请先执行以下 SELECT，确认脏数据范围）
-- ============================================================
SELECT '=== 1. 批次号脏值 ===' AS info,
       COUNT(*) AS dirty_batch_no_rows
FROM wms_inventory
WHERE deleted = 0
  AND batch_no REGEXP '^-?[0-9]+\\.?[0-9]*$';

SELECT '=== 2. locked_qty 负数 ===' AS info,
       COUNT(*) AS negative_locked_rows
FROM wms_inventory WHERE deleted = 0 AND locked_qty < 0;

SELECT '=== 3. quantity 负数 ===' AS info,
       COUNT(*) AS negative_qty_rows
FROM wms_inventory WHERE deleted = 0 AND quantity < 0;

SELECT '=== 4. available_qty 计算不符 ===' AS info,
       COUNT(*) AS inconsistent_available_rows
FROM wms_inventory
WHERE deleted = 0
  AND (quantity - COALESCE(locked_qty, 0)) <> COALESCE(available_qty, 0);

SELECT '=== 5. 重复库存组 ===' AS info, warehouse_id, sku_id,
       COALESCE(location_id, -1)     AS loc_key,
       COALESCE(batch_no,   '__NULL__') AS batch_key,
       COUNT(*)                      AS cnt
FROM wms_inventory WHERE deleted = 0
GROUP BY warehouse_id, sku_id, COALESCE(location_id, -1), COALESCE(batch_no, '__NULL__')
HAVING COUNT(*) > 1;


-- ============================================================
-- 阶段二：清理脏数据（按顺序执行，MySQL 用派生表包裹避免 1093 报错）
-- ============================================================
START TRANSACTION;

-- ---------- 步骤1：修复 batch_no 脏值 ----------
UPDATE wms_inventory
SET batch_no = NULL
WHERE deleted = 0
  AND batch_no REGEXP '^-?[0-9]+\\.?[0-9]*$';

-- ---------- 步骤2：修复 quantity 负数 ----------
UPDATE wms_inventory SET quantity = 0 WHERE deleted = 0 AND quantity < 0;

-- ---------- 步骤3：修复 locked_qty 负数 ----------
UPDATE wms_inventory SET locked_qty = 0 WHERE deleted = 0 AND locked_qty < 0;

-- ---------- 步骤4：重新计算 available_qty ----------
UPDATE wms_inventory
SET available_qty = quantity - COALESCE(locked_qty, 0)
WHERE deleted = 0;

-- ---------- 步骤5：重新计算 total_amount ----------
UPDATE wms_inventory
SET total_amount = cost_price * quantity
WHERE deleted = 0;

-- ---------- 步骤6：合并重复库存行 ----------
-- MySQL 不允许 UPDATE 同表子查询，必须用派生表包一层：
-- (SELECT ... FROM wms_inventory ...) AS alias —— MySQL 会物化成临时表

-- 6.1 找出每组主行 inventory_id（最小的那条），先把要合并的数量算出来
CREATE TEMPORARY TABLE tmp_inv_merge_targets AS
SELECT MIN(inventory_id) AS keep_id,
       SUM(quantity)     AS new_qty,
       SUM(locked_qty)   AS new_locked,
       SUM(total_amount) AS new_amount
FROM (SELECT * FROM wms_inventory WHERE deleted = 0) AS t
GROUP BY warehouse_id, sku_id, COALESCE(location_id, -1), COALESCE(batch_no, '__NULL__')
HAVING COUNT(*) > 1;

-- 6.2 把合并后的数量写到主行
UPDATE wms_inventory tgt
JOIN tmp_inv_merge_targets t ON tgt.inventory_id = t.keep_id
SET tgt.quantity     = t.new_qty,
    tgt.locked_qty   = t.new_locked,
    tgt.total_amount = t.new_amount;

-- 6.3 重新计算主行的 available_qty
UPDATE wms_inventory tgt
JOIN tmp_inv_merge_targets t ON tgt.inventory_id = t.keep_id
SET tgt.available_qty = tgt.quantity - COALESCE(tgt.locked_qty, 0);

-- 6.4 把多余行逻辑删除（deleted = 1）
-- 先找每组要保留的主行 id，再找所有不在主行集合里的行
DELETE FROM tmp_inv_dup_ids;           -- 保险
CREATE TEMPORARY TABLE tmp_inv_dup_ids AS
SELECT dup.inventory_id
FROM wms_inventory dup
WHERE dup.deleted = 0
  AND dup.inventory_id NOT IN (SELECT * FROM (
      SELECT MIN(inventory_id) AS keep_id
      FROM (SELECT * FROM wms_inventory WHERE deleted = 0) AS t
      GROUP BY warehouse_id, sku_id, COALESCE(location_id, -1), COALESCE(batch_no, '__NULL__')
  ) AS keepers)
  AND (dup.warehouse_id, dup.sku_id,
       COALESCE(dup.location_id, -1),
       COALESCE(dup.batch_no, '__NULL__'))
      IN (SELECT warehouse_id, sku_id, COALESCE(location_id, -1), COALESCE(batch_no, '__NULL__')
          FROM (SELECT * FROM wms_inventory WHERE deleted = 0) AS t
          GROUP BY warehouse_id, sku_id, COALESCE(location_id, -1), COALESCE(batch_no, '__NULL__')
          HAVING COUNT(*) > 1);

UPDATE wms_inventory SET deleted = 1 WHERE inventory_id IN (SELECT * FROM tmp_inv_dup_ids);

-- 6.5 清理临时表
DROP TEMPORARY TABLE tmp_inv_merge_targets;
DROP TEMPORARY TABLE tmp_inv_dup_ids;

COMMIT;


-- ============================================================
-- 阶段三：清理后验证（重新执行阶段一的 SELECT，脏数据应为 0）
-- ============================================================
SELECT '=== 验证：剩余脏批次 ===' AS info,
       COUNT(*) AS remaining_dirty_batch_no
FROM wms_inventory WHERE deleted = 0 AND batch_no REGEXP '^-?[0-9]+\\.?[0-9]*$';

SELECT '=== 验证：剩余 locked_qty 负数 ===' AS info,
       COUNT(*) AS remaining_negative_locked
FROM wms_inventory WHERE deleted = 0 AND locked_qty < 0;

SELECT '=== 验证：剩余 quantity 负数 ===' AS info,
       COUNT(*) AS remaining_negative_qty
FROM wms_inventory WHERE deleted = 0 AND quantity < 0;

SELECT '=== 验证：剩余 available_qty 计算不符 ===' AS info,
       COUNT(*) AS remaining_inconsistent_available
FROM wms_inventory
WHERE deleted = 0
  AND (quantity - COALESCE(locked_qty, 0)) <> COALESCE(available_qty, 0);

SELECT '=== 验证：剩余重复行 ===' AS info, warehouse_id, sku_id,
       COALESCE(location_id, -1)     AS loc_key,
       COALESCE(batch_no,   '__NULL__') AS batch_key,
       COUNT(*)                      AS cnt
FROM wms_inventory WHERE deleted = 0
GROUP BY warehouse_id, sku_id, COALESCE(location_id, -1), COALESCE(batch_no, '__NULL__')
HAVING COUNT(*) > 1;

SELECT '=== 验证：清理后库存预览 ===' AS info,
       warehouse_id, sku_id, inventory_id, batch_no,
       quantity, locked_qty, available_qty, total_amount
FROM wms_inventory WHERE deleted = 0
ORDER BY sku_id, warehouse_id, inventory_id;


-- ============================================================
-- ✅ 清理完成后必须执行的代码修复（见 WmsInventoryMapper.java）
-- ============================================================
-- 1. lockStock / unlockStock / confirmLockStock / deductStock / restoreLockedStock / adjustStock
--    的 batch_no WHERE 条件在 Java 传 null 时必须用 IS NULL 精确匹配，
--    不能退化到 COALESCE(batch_no,'')='' 去匹配所有空批次行！
-- 2. confirmLockStock 的 total_amount 计算必须用 (quantity - #{qty})，
--    不能用 quantity（MySQL SET 里 quantity 是修改前的值，会算错）。
-- ============================================================
