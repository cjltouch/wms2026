-- =====================================================================
-- WMS 业务数据清理脚本
-- 数据库: wms_db_pro
-- 用途: 清空所有业务单据、库存、审核记录，保留基础资料
-- 执行: mysql -uroot -p123456 wms_db_pro < clean-business-data.sql
-- 注意: 执行前会校验关键基础资料表非空，避免误删基础数据
-- =====================================================================

-- 安全检查：确认基础资料存在（若为 0 则停止，防止误操作）
SELECT
  IF((SELECT COUNT(*) FROM sys_user) > 0, 'OK', 'STOP') AS sys_user_check,
  IF((SELECT COUNT(*) FROM wms_warehouse) > 0, 'OK', 'STOP') AS warehouse_check,
  IF((SELECT COUNT(*) FROM wms_goods_spu) > 0, 'OK', 'STOP') AS spu_check;

-- 上述任一为 STOP 时，请人工确认后再继续。

-- =====================================================================
-- 清理范围
-- =====================================================================
START TRANSACTION;

-- 1. 单据明细（先删，避免主单残留引用）
DELETE FROM wms_purchase_order_item;        -- 采购单明细
DELETE FROM wms_stock_in_item;              -- 入库单明细
DELETE FROM wms_stock_out_item;             -- 出库单明细
DELETE FROM wms_transfer_order_item;        -- 调拨单明细
DELETE FROM wms_sale_order_item;            -- 销售单明细
DELETE FROM wms_loss_order_item;            -- 报损单明细

-- 2. 审核/状态日志
DELETE FROM wms_purchase_status_log;        -- 采购单审核记录
DELETE FROM wms_stock_in_status_log;        -- 入库单审核记录
DELETE FROM wms_stock_out_status_log;       -- 出库单审核记录
DELETE FROM wms_transfer_status_log;        -- 调拨单审核记录
DELETE FROM wms_sale_status_log;            -- 销售单审核记录
DELETE FROM wms_loss_status_log;            -- 报损单审核记录

-- 3. 单据主表
DELETE FROM wms_purchase_order;             -- 采购单
DELETE FROM wms_stock_in;                   -- 入库单
DELETE FROM wms_stock_out;                  -- 出库单
DELETE FROM wms_transfer_order;             -- 调拨单
DELETE FROM wms_sale_order;                 -- 销售单
DELETE FROM wms_loss_order;                 -- 报损单

-- 4. 库存记录
DELETE FROM wms_inventory_log;              -- 库存流水（先删）
DELETE FROM wms_inventory;                  -- 当前库存

COMMIT;

-- =====================================================================
-- 验证
-- =====================================================================
SELECT '=== 清理后业务表行数（应均为 0）===' AS result;
SELECT
  'wms_purchase_order' AS tbl, COUNT(*) AS cnt FROM wms_purchase_order
UNION ALL SELECT 'wms_purchase_order_item', COUNT(*) FROM wms_purchase_order_item
UNION ALL SELECT 'wms_purchase_status_log', COUNT(*) FROM wms_purchase_status_log
UNION ALL SELECT 'wms_stock_in', COUNT(*) FROM wms_stock_in
UNION ALL SELECT 'wms_stock_in_item', COUNT(*) FROM wms_stock_in_item
UNION ALL SELECT 'wms_stock_in_status_log', COUNT(*) FROM wms_stock_in_status_log
UNION ALL SELECT 'wms_stock_out', COUNT(*) FROM wms_stock_out
UNION ALL SELECT 'wms_stock_out_item', COUNT(*) FROM wms_stock_out_item
UNION ALL SELECT 'wms_stock_out_status_log', COUNT(*) FROM wms_stock_out_status_log
UNION ALL SELECT 'wms_transfer_order', COUNT(*) FROM wms_transfer_order
UNION ALL SELECT 'wms_transfer_order_item', COUNT(*) FROM wms_transfer_order_item
UNION ALL SELECT 'wms_transfer_status_log', COUNT(*) FROM wms_transfer_status_log
UNION ALL SELECT 'wms_sale_order', COUNT(*) FROM wms_sale_order
UNION ALL SELECT 'wms_sale_order_item', COUNT(*) FROM wms_sale_order_item
UNION ALL SELECT 'wms_sale_status_log', COUNT(*) FROM wms_sale_status_log
UNION ALL SELECT 'wms_loss_order', COUNT(*) FROM wms_loss_order
UNION ALL SELECT 'wms_loss_order_item', COUNT(*) FROM wms_loss_order_item
UNION ALL SELECT 'wms_loss_status_log', COUNT(*) FROM wms_loss_status_log
UNION ALL SELECT 'wms_inventory', COUNT(*) FROM wms_inventory
UNION ALL SELECT 'wms_inventory_log', COUNT(*) FROM wms_inventory_log;

SELECT '清理完成' AS final_result;
