-- ============================================================
-- 智能补货分析功能
-- 日期：2026-09-29
-- 内容：1. 菜单数据（目录「智能分析」→ 页面「智能补货」+ 按钮权限）
--       2. admin 角色授权
-- 说明：脚本可重复执行（幂等，INSERT 前先 DELETE）
-- ============================================================

-- ------------------------------------------------------------
-- 1. 菜单数据
-- ------------------------------------------------------------
-- 目录
DELETE FROM sys_menu WHERE menu_id = 50;
INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, path, component, perms, icon, order_num, visible, status, create_time)
VALUES (50, 0, '智能分析', 'M', '/analysis', NULL, NULL, 'DataAnalysis', 14, 1, 0, NOW());

-- 页面
DELETE FROM sys_menu WHERE menu_id = 500;
INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, path, component, perms, icon, order_num, visible, status, create_time)
VALUES (500, 50, '智能补货', 'C', 'replenishment', 'analysis/replenishment', 'analysis:replenishment:list', NULL, 1, 1, 0, NOW());

-- 按钮权限
DELETE FROM sys_menu WHERE menu_id IN (5001, 5002);
INSERT INTO sys_menu (menu_id, parent_id, menu_name, menu_type, path, component, perms, icon, order_num, visible, status, create_time)
VALUES
 (5001, 500, '补货查询', 'F', '', NULL, 'analysis:replenishment:list',   NULL, 1, 1, 0, NOW()),
 (5002, 500, '补货导出', 'F', '', NULL, 'analysis:replenishment:export', NULL, 2, 1, 0, NOW());

-- ------------------------------------------------------------
-- 2. admin 角色授权（角色ID=1）
-- ------------------------------------------------------------
DELETE FROM sys_role_menu WHERE role_id = 1 AND menu_id IN (50, 500, 5001, 5002);
INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
 (1, 50),
 (1, 500),
 (1, 5001),
 (1, 5002);
