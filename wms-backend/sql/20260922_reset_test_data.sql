-- ============================================================
-- 重建库存表 + 清空全部业务测试数据
-- 日期：2026-09-22
-- 场景：本地测试数据乱了，清空重测
-- 内容：
--   1. 用实体类定义完整重建 wms_inventory 表（被误删时用）
--   2. 清空所有出入库/调拨/盘点/报损/采购/销售单据、明细、状态日志、库存流水
-- 注意：本脚本会清空全部业务数据，请确认后再执行！
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO';

-- ===== 1. 重建 wms_inventory 表（如果存在先删）=====
DROP TABLE IF EXISTS `wms_inventory`;
CREATE TABLE `wms_inventory` (
  `inventory_id`   BIGINT        NOT NULL   COMMENT '库存ID',
  `warehouse_id`   BIGINT        DEFAULT NULL COMMENT '仓库ID',
  `sku_id`         BIGINT        DEFAULT NULL COMMENT 'SKU ID',
  `location_id`    BIGINT        DEFAULT NULL COMMENT '库位ID',
  `batch_no`       VARCHAR(64)   DEFAULT NULL COMMENT '批次号',
  `produce_date`   DATE          DEFAULT NULL COMMENT '生产日期',
  `expire_date`    DATE          DEFAULT NULL COMMENT '过期日期',
  `supplier_batch` VARCHAR(64)   DEFAULT NULL COMMENT '供应商批次',
  `quantity`       INT           NOT NULL DEFAULT 0 COMMENT '库存数量',
  `locked_qty`     INT           NOT NULL DEFAULT 0 COMMENT '锁定数量',
  `available_qty`  INT           NOT NULL DEFAULT 0 COMMENT '可用数量',
  `cost_price`     DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '成本单价',
  `total_amount`   DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '库存总金额',
  `last_in_time`   DATETIME      DEFAULT NULL COMMENT '最后入库时间',
  `last_out_time`  DATETIME      DEFAULT NULL COMMENT '最后出库时间',
  `create_by`      BIGINT        DEFAULT NULL COMMENT '创建人',
  `create_time`    DATETIME      DEFAULT NULL COMMENT '创建时间',
  `update_by`      BIGINT        DEFAULT NULL COMMENT '更新人',
  `update_time`    DATETIME      DEFAULT NULL COMMENT '更新时间',
  `version`        INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
  PRIMARY KEY (`inventory_id`),
  KEY `idx_warehouse_sku` (`warehouse_id`, `sku_id`),
  KEY `idx_batch_no` (`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存表';


-- ===== 2. 清空所有业务单据（主表先清子表后清，避免外键约束）=====

-- 库存流水（与库存表无外键关联，单独 TRUNCATE）
TRUNCATE TABLE wms_inventory_log;

-- 入库
TRUNCATE TABLE wms_stock_in_status_log;
TRUNCATE TABLE wms_stock_in_item;
TRUNCATE TABLE wms_stock_in;

-- 出库
TRUNCATE TABLE wms_stock_out_status_log;
TRUNCATE TABLE wms_stock_out_item;
TRUNCATE TABLE wms_stock_out;

-- 调拨
TRUNCATE TABLE wms_transfer_status_log;
TRUNCATE TABLE wms_transfer_order_item;
TRUNCATE TABLE wms_transfer_order;

-- 盘点
TRUNCATE TABLE wms_check_status_log;
TRUNCATE TABLE wms_check_order_item;
TRUNCATE TABLE wms_check_order;

-- 报损
TRUNCATE TABLE wms_loss_status_log;
TRUNCATE TABLE wms_loss_order_item;
TRUNCATE TABLE wms_loss_order;

-- 采购
TRUNCATE TABLE wms_purchase_status_log;
TRUNCATE TABLE wms_purchase_order_item;
TRUNCATE TABLE wms_purchase_order;

-- 销售
TRUNCATE TABLE wms_sale_status_log;
TRUNCATE TABLE wms_sale_order_item;
TRUNCATE TABLE wms_sale_order;

SET FOREIGN_KEY_CHECKS = 1;

-- ===== 3. 验证 =====
SELECT '=== 清空后各业务表行数 ===' AS info;
SELECT '库存' AS t, COUNT(*) AS c FROM wms_inventory
UNION ALL SELECT '库存流水', COUNT(*) FROM wms_inventory_log
UNION ALL SELECT '入库单',   COUNT(*) FROM wms_stock_in
UNION ALL SELECT '出库单',   COUNT(*) FROM wms_stock_out
UNION ALL SELECT '调拨单',   COUNT(*) FROM wms_transfer_order
UNION ALL SELECT '盘点单',   COUNT(*) FROM wms_check_order
UNION ALL SELECT '报损单',   COUNT(*) FROM wms_loss_order
UNION ALL SELECT '采购单',   COUNT(*) FROM wms_purchase_order
UNION ALL SELECT '销售单',   COUNT(*) FROM wms_sale_order;

-- ============================================================
-- ⚠️ 重测前代码已同步修复（2026-09-22），不要再出现脏数据：
--
-- 修复 1：WmsInventoryMapper.java — batch_no WHERE 条件精确匹配
--   旧：batch_no = #{batchNo} OR (COALESCE(batch_no,'') = '' AND COALESCE(#{batchNo},'') = '')
--     → Java 传 null 时匹配所有空 batch_no 行，导致同 SKU+仓 多行一起被扣
--   新：batch_no = #{batchNo} OR (#{batchNo} IS NULL AND batch_no IS NULL)
--     → Java 传 null 时只匹配 DB 也为 NULL 的行
--   影响方法：deductStock / lockStock / confirmLockStock / unlockStock / restoreLockedStock
--
-- 修复 2：WmsInventoryMapper.java — confirmLockStock 的 total_amount 算错
--   旧：total_amount = cost_price * quantity  （quantity 是 SET 前的旧值！）
--   新：total_amount = cost_price * (quantity - #{qty})  （直接用新值计算）
--
-- 修复 3：WmsStockOutServiceImpl.java — 锁定/解锁/确认/反审核 均补库存流水
--   · lockItems    → direction=0, qtyChange=0, changeLocked=+qty  （预占）
--   · confirmLockedOut → direction=-1, qtyChange=-qty, changeLocked=-qty
--   · unlockItems  → direction=0, qtyChange=0, changeLocked=-qty  （释放）
--   · restoreLockedItems → direction=1, qtyChange=+qty, changeLocked=+qty （回补）
-- ============================================================
