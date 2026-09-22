-- ============================================================
-- 用品登记功能（办公用品/消耗品出入库记录）
-- 日期：2026-09-22
-- 内容：1. 业务表 wms_office_record
--       2. 菜单数据（目录/页面/按钮权限）
-- 说明：脚本可重复执行（幂等）
-- ============================================================

-- ------------------------------------------------------------
-- 1. 业务表：办公用品/消耗品出入库登记表（仅作记录，不关联供应商/品牌/库存）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wms_office_record` (
  `record_id`   BIGINT       NOT NULL                COMMENT '记录ID',
  `record_date` DATE         NOT NULL                COMMENT '日期',
  `item_name`   VARCHAR(100) NOT NULL                COMMENT '名称',
  `type`        TINYINT      NOT NULL                COMMENT '类型：1入库 2领取 3报损',
  `unit`        VARCHAR(20)  DEFAULT NULL            COMMENT '单位',
  `quantity`    INT          NOT NULL DEFAULT 1      COMMENT '数量',
  `person_name` VARCHAR(50)  DEFAULT NULL            COMMENT '姓名',
  `spec`        VARCHAR(100) DEFAULT NULL            COMMENT '规格',
  `remark`      VARCHAR(255) DEFAULT NULL            COMMENT '备注',
  `create_by`   BIGINT       DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
  `update_by`   BIGINT       DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
  `version`     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `deleted`     INT          NOT NULL DEFAULT 0      COMMENT '逻辑删除：0正常 1删除',
  PRIMARY KEY (`record_id`),
  KEY `idx_record_date` (`record_date`),
  KEY `idx_item_name` (`item_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='办公用品/消耗品出入库登记表';

-- ------------------------------------------------------------
-- 2. 菜单数据：目录「用品管理」→ 页面「用品登记」+ 按钮权限
-- ------------------------------------------------------------
-- 目录
DELETE FROM sys_menu WHERE menu_id = 40;
INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, path, component, perms, icon, order_num, visible, status, create_time)
VALUES (40, 0, '用品管理', 'M', '/office', NULL, NULL, 'Collection', 13, 1, 0, NOW());

-- 页面
DELETE FROM sys_menu WHERE menu_id = 400;
INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, path, component, perms, icon, order_num, visible, status, create_time)
VALUES (400, 40, '用品登记', 'C', 'record', 'office/index', 'wms:office:list', NULL, 1, 1, 0, NOW());

-- 按钮权限
DELETE FROM sys_menu WHERE menu_id IN (4001, 4002, 4003, 4004);
INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, path, component, perms, icon, order_num, visible, status, create_time)
VALUES
 (4001, 400, '用品新增', 'F', '', NULL, 'wms:office:add',    NULL, 1, 1, 0, NOW()),
 (4002, 400, '用品修改', 'F', '', NULL, 'wms:office:edit',   NULL, 2, 1, 0, NOW()),
 (4003, 400, '用品删除', 'F', '', NULL, 'wms:office:remove', NULL, 3, 1, 0, NOW()),
 (4004, 400, '用品导出', 'F', '', NULL, 'wms:office:export', NULL, 4, 1, 0, NOW());
