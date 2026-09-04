-- =========================================================================================
-- WMS 仓库管理系统 数据库表结构设计 (核心业务模块：采购 + 入库 + 出库)
-- 目标数据库版本：MySQL 8.0+
-- 字符集：utf8mb4 / 排序规则：utf8mb4_general_ci
-- 引擎：InnoDB（支持事务 + 行锁 + 外键）
-- 设计依据：WMS-仓库管理系统-产品需求文档-v1.0
-- 生成日期：2026-08-24
-- =========================================================================================

-- =========================================================================================
-- 0. 数据库初始化
-- =========================================================================================
DROP DATABASE IF EXISTS `wms_db`;
CREATE DATABASE `wms_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `wms_db`;

-- -----------------------------------------------------------------------------------------
-- 统一约定：
-- 1. 主键全部使用 BIGINT 雪花ID（避免自增ID暴露数据量级）
-- 2. 通用字段：create_by / create_time / update_by / update_time / version / deleted
-- 3. 所有金额 DECIMAL(18,4)，数量 INT / DECIMAL(18,4) 根据精度需要
-- 4. 状态字段 TINYINT + COMMENT 明确枚举值
-- 5. 逻辑删除 deleted：0=未删除 1=已删除
-- 6. 乐观锁 version：初始 0，每次更新 +1
-- 7. 时间字段：create_time / update_time 使用 DATETIME，默认 CURRENT_TIMESTAMP / ON UPDATE
-- =========================================================================================

-- =========================================================================================
-- ========= 一、系统权限层（依赖顺序：部门→角色→用户→菜单→关联表→日志） ====================
-- =========================================================================================

-- 1.1 部门表
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `dept_id`       BIGINT        NOT NULL                    COMMENT '部门ID',
  `parent_id`     BIGINT        NOT NULL DEFAULT 0          COMMENT '父部门ID，0=顶级',
  `ancestors`     VARCHAR(512)  NOT NULL DEFAULT ''         COMMENT '祖级列表（逗号分隔，便于快速查子树）',
  `dept_name`     VARCHAR(64)   NOT NULL                    COMMENT '部门名称',
  `order_num`     INT           NOT NULL DEFAULT 0          COMMENT '显示顺序',
  `leader`        VARCHAR(32)   DEFAULT NULL                COMMENT '负责人',
  `phone`         VARCHAR(20)   DEFAULT NULL                COMMENT '联系电话',
  `email`         VARCHAR(128)  DEFAULT NULL                COMMENT '邮箱',
  `status`        TINYINT       NOT NULL DEFAULT 1          COMMENT '状态 0=停用 1=正常',
  `create_by`     BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`dept_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 1.2 角色表
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `role_id`             BIGINT        NOT NULL                    COMMENT '角色ID',
  `role_name`           VARCHAR(64)   NOT NULL                    COMMENT '角色名称',
  `role_key`            VARCHAR(64)   NOT NULL                    COMMENT '角色编码(唯一)，如 ROLE_ADMIN',
  `role_sort`           INT           NOT NULL DEFAULT 0          COMMENT '显示顺序',
  `data_scope`          TINYINT       NOT NULL DEFAULT 1          COMMENT '数据范围 1=全部 2=本部门 3=本部门及以下 4=自定义 5=仅本人',
  `status`              TINYINT       NOT NULL DEFAULT 1          COMMENT '状态 0=停用 1=正常',
  `remark`              VARCHAR(500)  DEFAULT NULL                COMMENT '备注',
  `create_by`           BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_key` (`role_key`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 1.3 用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `user_id`             BIGINT        NOT NULL                    COMMENT '用户ID',
  `dept_id`             BIGINT        DEFAULT NULL                COMMENT '部门ID',
  `username`            VARCHAR(64)   NOT NULL                    COMMENT '登录账号(唯一)',
  `password`            VARCHAR(255)  NOT NULL                    COMMENT '密码(BCrypt)',
  `real_name`           VARCHAR(64)   NOT NULL                    COMMENT '真实姓名',
  `nick_name`           VARCHAR(64)   DEFAULT NULL                COMMENT '昵称',
  `gender`              TINYINT       NOT NULL DEFAULT 0          COMMENT '性别 0=未知 1=男 2=女',
  `phone`               VARCHAR(20)   DEFAULT NULL                COMMENT '手机号',
  `email`               VARCHAR(128)  DEFAULT NULL                COMMENT '邮箱',
  `avatar`              VARCHAR(255)  DEFAULT NULL                COMMENT '头像URL',
  `warehouse_scope`     JSON          DEFAULT NULL                COMMENT '数据权限-可操作仓库ID数组',
  `status`              TINYINT       NOT NULL DEFAULT 1          COMMENT '状态 0=禁用 1=启用',
  `login_ip`            VARCHAR(64)   DEFAULT NULL                COMMENT '最后登录IP',
  `login_time`          DATETIME      DEFAULT NULL                COMMENT '最后登录时间',
  `pwd_modify_time`     DATETIME      DEFAULT NULL                COMMENT '密码最后修改时间',
  `remark`              VARCHAR(500)  DEFAULT NULL                COMMENT '备注',
  `create_by`           BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 1.4 菜单权限表
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `menu_id`       BIGINT        NOT NULL                    COMMENT '菜单ID',
  `parent_id`     BIGINT        NOT NULL DEFAULT 0          COMMENT '父菜单ID，0=顶级',
  `menu_name`     VARCHAR(64)   NOT NULL                    COMMENT '菜单名称',
  `menu_type`     CHAR(1)       NOT NULL DEFAULT 'M'        COMMENT '菜单类型 M=目录 C=菜单 F=按钮',
  `path`          VARCHAR(255)  DEFAULT NULL                COMMENT '路由路径',
  `component`     VARCHAR(255)  DEFAULT NULL                COMMENT '组件路径',
  `perms`         VARCHAR(128)  DEFAULT NULL                COMMENT '权限标识，如 system:user:list',
  `icon`          VARCHAR(64)   DEFAULT '#'                COMMENT '图标',
  `order_num`     INT           NOT NULL DEFAULT 0          COMMENT '显示顺序',
  `visible`       TINYINT       NOT NULL DEFAULT 1          COMMENT '显示状态 0=隐藏 1=显示',
  `status`        TINYINT       NOT NULL DEFAULT 1          COMMENT '状态 0=停用 1=正常',
  `remark`        VARCHAR(500)  DEFAULT ''                  COMMENT '备注',
  `create_by`     BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`menu_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- 1.5 用户-角色关联表
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id`   BIGINT    NOT NULL COMMENT '用户ID',
  `role_id`   BIGINT    NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 1.6 角色-菜单关联表
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `role_id`   BIGINT    NOT NULL COMMENT '角色ID',
  `menu_id`   BIGINT    NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 1.7 操作日志表（审计用）
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `oper_id`       BIGINT        NOT NULL                    COMMENT '日志主键',
  `title`         VARCHAR(64)   DEFAULT ''                  COMMENT '模块标题',
  `business_type` TINYINT       DEFAULT 0                   COMMENT '业务类型 0=其它 1=新增 2=修改 3=删除 4=授权 5=导出 6=导入 7=强退 8=生成代码 9=清空数据 10=审核',
  `method`        VARCHAR(255)  DEFAULT ''                  COMMENT '方法名称',
  `request_method` VARCHAR(16)  DEFAULT ''                  COMMENT 'HTTP方法',
  `operator_type` TINYINT       DEFAULT 1                   COMMENT '操作类别 0=其它 1=后台用户 2=手机端用户',
  `oper_name`     VARCHAR(64)   DEFAULT ''                  COMMENT '操作人员',
  `dept_name`     VARCHAR(64)   DEFAULT ''                  COMMENT '部门名称',
  `oper_url`      VARCHAR(255)  DEFAULT ''                  COMMENT '请求URL',
  `oper_ip`       VARCHAR(64)   DEFAULT ''                  COMMENT '操作IP',
  `oper_location` VARCHAR(128)  DEFAULT ''                  COMMENT '操作地点',
  `oper_param`    TEXT          DEFAULT NULL                COMMENT '请求参数',
  `json_result`   TEXT          DEFAULT NULL                COMMENT '返回参数',
  `status`        TINYINT       DEFAULT 1                   COMMENT '操作状态 0=失败 1=正常',
  `error_msg`     VARCHAR(2000) DEFAULT ''                  COMMENT '错误消息',
  `cost_time`     BIGINT        DEFAULT 0                   COMMENT '耗时(ms)',
  `oper_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`oper_id`),
  KEY `idx_oper_time` (`oper_time`),
  KEY `idx_business_type` (`business_type`),
  KEY `idx_oper_name` (`oper_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 1.8 登录日志表
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
  `login_id`      BIGINT        NOT NULL                    COMMENT '登录日志ID',
  `username`      VARCHAR(64)   DEFAULT ''                  COMMENT '登录账号',
  `login_ip`      VARCHAR(64)   DEFAULT ''                  COMMENT '登录IP',
  `login_location` VARCHAR(128) DEFAULT ''                  COMMENT '登录地点',
  `browser`       VARCHAR(64)   DEFAULT ''                  COMMENT '浏览器类型',
  `os`            VARCHAR(64)   DEFAULT ''                  COMMENT '操作系统',
  `status`        TINYINT       DEFAULT 1                   COMMENT '登录状态 0=失败 1=成功',
  `msg`           VARCHAR(255)  DEFAULT ''                  COMMENT '提示消息',
  `login_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`login_id`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- =========================================================================================
-- ========= 二、主数据层（分类/品牌/单位→SPU→SKU；供应商/客户；仓库→库区→库位） ===========
-- =========================================================================================

-- 2.1 商品分类表（多级树形，建议3级）
DROP TABLE IF EXISTS `wms_category`;
CREATE TABLE `wms_category` (
  `category_id`   BIGINT        NOT NULL                    COMMENT '分类ID',
  `parent_id`     BIGINT        NOT NULL DEFAULT 0          COMMENT '父分类ID，0=顶级',
  `ancestors`     VARCHAR(512)  NOT NULL DEFAULT ''         COMMENT '祖级列表（逗号分隔）',
  `category_name` VARCHAR(64)   NOT NULL                    COMMENT '分类名称',
  `category_code` VARCHAR(64)   DEFAULT NULL                COMMENT '分类编码',
  `order_num`     INT           NOT NULL DEFAULT 0          COMMENT '显示顺序',
  `icon`          VARCHAR(255)  DEFAULT NULL                COMMENT '分类图标',
  `status`        TINYINT       NOT NULL DEFAULT 1          COMMENT '状态 0=停用 1=正常',
  `remark`        VARCHAR(500)  DEFAULT NULL                COMMENT '备注',
  `create_by`     BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`category_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 2.2 品牌表
DROP TABLE IF EXISTS `wms_brand`;
CREATE TABLE `wms_brand` (
  `brand_id`      BIGINT        NOT NULL                    COMMENT '品牌ID',
  `brand_name`    VARCHAR(64)   NOT NULL                    COMMENT '品牌名称',
  `brand_logo`    VARCHAR(255)  DEFAULT NULL                COMMENT '品牌LOGO',
  `brand_desc`    VARCHAR(500)  DEFAULT NULL                COMMENT '品牌描述',
  `order_num`     INT           NOT NULL DEFAULT 0          COMMENT '显示顺序',
  `status`        TINYINT       NOT NULL DEFAULT 1          COMMENT '状态 0=停用 1=正常',
  `create_by`     BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`brand_id`),
  KEY `idx_brand_name` (`brand_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品牌表';

-- 2.3 计量单位表（支持辅助单位+换算）
DROP TABLE IF EXISTS `wms_unit`;
CREATE TABLE `wms_unit` (
  `unit_id`       BIGINT        NOT NULL                    COMMENT '单位ID',
  `unit_name`     VARCHAR(32)   NOT NULL                    COMMENT '单位名称，如 个/箱/瓶/公斤',
  `unit_code`     VARCHAR(32)   NOT NULL                    COMMENT '单位编码',
  `is_base`       TINYINT       NOT NULL DEFAULT 1          COMMENT '是否基准单位 0=否 1=是',
  `base_unit_id`  BIGINT        DEFAULT NULL                COMMENT '基准单位ID（非基准单位需关联）',
  `convert_rate`  DECIMAL(18,6) NOT NULL DEFAULT 1.000000   COMMENT '换算率（1个当前单位 = rate个基准单位），如 1箱=24瓶 → rate=24',
  `status`        TINYINT       NOT NULL DEFAULT 1          COMMENT '状态 0=停用 1=正常',
  `create_by`     BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`unit_id`),
  UNIQUE KEY `uk_unit_code` (`unit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计量单位表';

-- 2.4 商品SPU表（标准产品单元，不含规格）
DROP TABLE IF EXISTS `wms_goods_spu`;
CREATE TABLE `wms_goods_spu` (
  `spu_id`          BIGINT        NOT NULL                    COMMENT 'SPU ID',
  `spu_code`        VARCHAR(64)   NOT NULL                    COMMENT 'SPU编码',
  `spu_name`        VARCHAR(255)  NOT NULL                    COMMENT 'SPU名称',
  `category_id`     BIGINT        NOT NULL                    COMMENT '分类ID',
  `brand_id`        BIGINT        DEFAULT NULL                COMMENT '品牌ID',
  `unit_id`         BIGINT        DEFAULT NULL                COMMENT '主单位ID（冗余，便于列表展示）',
  `main_image`      VARCHAR(255)  DEFAULT NULL                COMMENT '主图URL',
  `images`          JSON          DEFAULT NULL                COMMENT '详情图URL列表JSON数组',
  `description`     TEXT          DEFAULT NULL                COMMENT '商品描述（富文本）',
  `enable_batch`    TINYINT       NOT NULL DEFAULT 0          COMMENT '是否启用批次管理 0=否 1=是',
  `enable_sn`       TINYINT       NOT NULL DEFAULT 0          COMMENT '是否启用序列号(SN)管理 0=否 1=是',
  `shelf_life_days` INT           DEFAULT NULL                COMMENT '保质期天数（启用批次时使用）',
  `warn_days`       INT           DEFAULT 0                   COMMENT '效期预警提前天数',
  `weight`          DECIMAL(10,3) DEFAULT NULL                COMMENT '单份重量(kg)',
  `volume`          DECIMAL(10,3) DEFAULT NULL                COMMENT '单份体积(m³)',
  `remark`          VARCHAR(500)  DEFAULT NULL                COMMENT '备注',
  `status`          TINYINT       NOT NULL DEFAULT 1          COMMENT '状态 0=下架 1=上架',
  `create_by`       BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`spu_id`),
  UNIQUE KEY `uk_spu_code` (`spu_code`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_brand_id` (`brand_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SPU表';

-- 2.5 商品SKU表（库存管理最小单位，含规格组合）
DROP TABLE IF EXISTS `wms_goods_sku`;
CREATE TABLE `wms_goods_sku` (
  `sku_id`          BIGINT        NOT NULL                    COMMENT 'SKU ID',
  `spu_id`          BIGINT        NOT NULL                    COMMENT 'SPU ID',
  `sku_code`        VARCHAR(64)   NOT NULL                    COMMENT 'SKU编码（唯一）',
  `sku_name`        VARCHAR(255)  NOT NULL                    COMMENT 'SKU名称（SPU名+规格）',
  `barcode`         VARCHAR(64)   DEFAULT NULL                COMMENT '条形码/二维码（EAN-13/69码）',
  `category_id`     BIGINT        NOT NULL                    COMMENT '分类ID（冗余便于查询）',
  `brand_id`        BIGINT        DEFAULT NULL                COMMENT '品牌ID（冗余）',
  `unit_id`         BIGINT        NOT NULL                    COMMENT '主单位ID',
  `spec_json`       JSON          DEFAULT NULL                COMMENT '规格属性JSON，如 {"颜色":"红","尺寸":"L"}',
  `spec_text`       VARCHAR(255)  DEFAULT NULL                COMMENT '规格文本拼接（冗余用于列表显示）',
  `cost_price`      DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '最新成本价（移动加权）',
  `purchase_price`  DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '默认采购价',
  `sale_price`      DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '销售价',
  `retail_price`    DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '零售价',
  `weight`          DECIMAL(10,3) DEFAULT NULL                COMMENT 'SKU重量(kg)，覆盖SPU',
  `volume`          DECIMAL(10,3) DEFAULT NULL                COMMENT 'SKU体积(m³)，覆盖SPU',
  `min_stock`       INT           NOT NULL DEFAULT 0          COMMENT '安全库存下限（预警）',
  `max_stock`       INT           NOT NULL DEFAULT 0          COMMENT '安全库存上限（积压预警）',
  `image`           VARCHAR(255)  DEFAULT NULL                COMMENT 'SKU图片URL',
  `remark`          VARCHAR(500)  DEFAULT NULL                COMMENT '备注',
  `status`          TINYINT       NOT NULL DEFAULT 1          COMMENT '状态 0=下架 1=上架',
  `create_by`       BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`sku_id`),
  UNIQUE KEY `uk_sku_code` (`sku_code`),
  UNIQUE KEY `uk_barcode` (`barcode`),
  KEY `idx_spu_id` (`spu_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_brand_id` (`brand_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

-- 2.6 供应商表
DROP TABLE IF EXISTS `wms_supplier`;
CREATE TABLE `wms_supplier` (
  `supplier_id`     BIGINT        NOT NULL                    COMMENT '供应商ID',
  `supplier_code`   VARCHAR(64)   NOT NULL                    COMMENT '供应商编码（唯一）',
  `supplier_name`   VARCHAR(255)  NOT NULL                    COMMENT '供应商全称',
  `short_name`      VARCHAR(64)   DEFAULT NULL                COMMENT '供应商简称',
  `category_id`     BIGINT        DEFAULT NULL                COMMENT '供应商分类ID',
  `credit_code`     VARCHAR(64)   DEFAULT NULL                COMMENT '统一社会信用代码',
  `legal_person`    VARCHAR(32)   DEFAULT NULL                COMMENT '法人代表',
  `level`           CHAR(1)       DEFAULT 'C'                 COMMENT '评级 A/B/C/D',
  `province`        VARCHAR(32)   DEFAULT NULL                COMMENT '省',
  `city`            VARCHAR(32)   DEFAULT NULL                COMMENT '市',
  `district`        VARCHAR(32)   DEFAULT NULL                COMMENT '区县',
  `address`         VARCHAR(500)  DEFAULT NULL                COMMENT '详细地址',
  `contact_name`    VARCHAR(32)   DEFAULT NULL                COMMENT '默认联系人',
  `contact_phone`   VARCHAR(20)   DEFAULT NULL                COMMENT '默认联系电话',
  `contact_email`   VARCHAR(128)  DEFAULT NULL                COMMENT '默认联系邮箱',
  `payment_term`    VARCHAR(64)   DEFAULT NULL                COMMENT '账期：月结30天/现款现货',
  `credit_limit`    DECIMAL(18,2) NOT NULL DEFAULT 0.00       COMMENT '信用额度',
  `used_credit`     DECIMAL(18,2) NOT NULL DEFAULT 0.00       COMMENT '已用额度（汇总采购未付款）',
  `tax_rate`        DECIMAL(5,2)  NOT NULL DEFAULT 0.00       COMMENT '税率%，如 13.00',
  `tax_no`          VARCHAR(64)   DEFAULT NULL                COMMENT '纳税人识别号',
  `bank_name`       VARCHAR(128)  DEFAULT NULL                COMMENT '开户银行',
  `bank_account`    VARCHAR(64)   DEFAULT NULL                COMMENT '银行账号',
  `status`          TINYINT       NOT NULL DEFAULT 1          COMMENT '状态 0=潜在 1=合作 2=暂停 3=黑名单',
  `remark`          VARCHAR(500)  DEFAULT NULL                COMMENT '备注',
  `create_by`       BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`supplier_id`),
  UNIQUE KEY `uk_supplier_code` (`supplier_code`),
  KEY `idx_supplier_name` (`supplier_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

-- 2.7 客户表（销售业务用）
DROP TABLE IF EXISTS `wms_customer`;
CREATE TABLE `wms_customer` (
  `customer_id`     BIGINT        NOT NULL                    COMMENT '客户ID',
  `customer_code`   VARCHAR(64)   NOT NULL                    COMMENT '客户编码（唯一）',
  `customer_name`   VARCHAR(255)  NOT NULL                    COMMENT '客户全称',
  `short_name`      VARCHAR(64)   DEFAULT NULL                COMMENT '客户简称',
  `category_id`     BIGINT        DEFAULT NULL                COMMENT '客户分类ID',
  `level`           CHAR(1)       DEFAULT 'C'                 COMMENT '客户等级 A/B/C/D',
  `price_level`     TINYINT       NOT NULL DEFAULT 1          COMMENT '价格等级 1=一级价 2=二级价 3=三级价 4=零售价',
  `province`        VARCHAR(32)   DEFAULT NULL                COMMENT '省',
  `city`            VARCHAR(32)   DEFAULT NULL                COMMENT '市',
  `district`        VARCHAR(32)   DEFAULT NULL                COMMENT '区县',
  `address`         VARCHAR(500)  DEFAULT NULL                COMMENT '详细地址',
  `contact_name`    VARCHAR(32)   DEFAULT NULL                COMMENT '默认联系人',
  `contact_phone`   VARCHAR(20)   DEFAULT NULL                COMMENT '默认联系电话',
  `contact_email`   VARCHAR(128)  DEFAULT NULL                COMMENT '默认联系邮箱',
  `payment_term`    VARCHAR(64)   DEFAULT NULL                COMMENT '账期',
  `credit_limit`    DECIMAL(18,2) NOT NULL DEFAULT 0.00       COMMENT '信用额度',
  `used_credit`     DECIMAL(18,2) NOT NULL DEFAULT 0.00       COMMENT '已用额度',
  `status`          TINYINT       NOT NULL DEFAULT 1          COMMENT '状态 0=停用 1=合作',
  `remark`          VARCHAR(500)  DEFAULT NULL                COMMENT '备注',
  `create_by`       BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`customer_id`),
  UNIQUE KEY `uk_customer_code` (`customer_code`),
  KEY `idx_customer_name` (`customer_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- 2.8 仓库表
DROP TABLE IF EXISTS `wms_warehouse`;
CREATE TABLE `wms_warehouse` (
  `warehouse_id`    BIGINT        NOT NULL                    COMMENT '仓库ID',
  `warehouse_code`  VARCHAR(64)   NOT NULL                    COMMENT '仓库编码（唯一），如 WH1',
  `warehouse_name`  VARCHAR(128)  NOT NULL                    COMMENT '仓库名称',
  `warehouse_type`  TINYINT       NOT NULL DEFAULT 1          COMMENT '仓库类型 1=正品仓 2=退件仓 3=残次仓 4=在途仓 5=赠品仓 9=其他',
  `province`        VARCHAR(32)   DEFAULT NULL                COMMENT '省',
  `city`            VARCHAR(32)   DEFAULT NULL                COMMENT '市',
  `district`        VARCHAR(32)   DEFAULT NULL                COMMENT '区县',
  `address`         VARCHAR(500)  DEFAULT NULL                COMMENT '详细地址',
  `area`            DECIMAL(12,2) DEFAULT NULL                COMMENT '仓库面积(m²)',
  `manager_id`      BIGINT        DEFAULT NULL                COMMENT '仓库主管用户ID',
  `manager_name`    VARCHAR(32)   DEFAULT NULL                COMMENT '主管姓名（冗余）',
  `manager_phone`   VARCHAR(20)   DEFAULT NULL                COMMENT '主管电话',
  `enable_location` TINYINT       NOT NULL DEFAULT 0          COMMENT '是否启用库位管理 0=否 1=是',
  `enable_check`    TINYINT       NOT NULL DEFAULT 0          COMMENT '出库是否需要复核 0=否 1=是',
  `status`          TINYINT       NOT NULL DEFAULT 1          COMMENT '状态 0=停用 1=正常',
  `remark`          VARCHAR(500)  DEFAULT NULL                COMMENT '备注',
  `create_by`       BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`warehouse_id`),
  UNIQUE KEY `uk_warehouse_code` (`warehouse_code`),
  KEY `idx_warehouse_name` (`warehouse_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库表';

-- 2.9 库区表（仓库下分区：收货区/存储区/拣货区/发货区/退货区/残次区）
DROP TABLE IF EXISTS `wms_area`;
CREATE TABLE `wms_area` (
  `area_id`         BIGINT        NOT NULL                    COMMENT '库区ID',
  `warehouse_id`    BIGINT        NOT NULL                    COMMENT '仓库ID',
  `area_code`       VARCHAR(64)   NOT NULL                    COMMENT '库区编码，如 A / B / RECEIVE / SHIP',
  `area_name`       VARCHAR(128)  NOT NULL                    COMMENT '库区名称',
  `area_type`       TINYINT       NOT NULL DEFAULT 0          COMMENT '库区类型 0=存储区 1=收货区 2=拣货区 3=打包区 4=发货区 5=退货区 6=残次区 7=待检区',
  `attribute`       TINYINT       NOT NULL DEFAULT 0          COMMENT '属性 0=普通 1=常温 2=冷藏 3=冷冻 4=贵重 5=危险品',
  `order_num`       INT           NOT NULL DEFAULT 0          COMMENT '显示顺序',
  `status`          TINYINT       NOT NULL DEFAULT 1          COMMENT '状态 0=停用 1=正常',
  `remark`          VARCHAR(500)  DEFAULT NULL                COMMENT '备注',
  `create_by`       BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`area_id`),
  UNIQUE KEY `uk_wh_area_code` (`warehouse_id`, `area_code`),
  KEY `idx_warehouse_id` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库区表';

-- 2.10 库位表（最小存储单元）
DROP TABLE IF EXISTS `wms_location`;
CREATE TABLE `wms_location` (
  `location_id`     BIGINT        NOT NULL                    COMMENT '库位ID',
  `warehouse_id`    BIGINT        NOT NULL                    COMMENT '仓库ID（冗余便于快速查询）',
  `area_id`         BIGINT        NOT NULL                    COMMENT '库区ID',
  `location_code`   VARCHAR(64)   NOT NULL                    COMMENT '库位编码，如 WH1-A-01-02-03 = 仓-区-架-层-位',
  `location_name`   VARCHAR(128)  DEFAULT NULL                COMMENT '库位名称',
  `rack_code`       VARCHAR(32)   DEFAULT NULL                COMMENT '货架编码',
  `row_no`          INT           DEFAULT NULL                COMMENT '排号',
  `column_no`       INT           DEFAULT NULL                COMMENT '列号',
  `level_no`        INT           DEFAULT NULL                COMMENT '层号',
  `max_weight`      DECIMAL(12,3) DEFAULT NULL                COMMENT '最大承重(kg)',
  `max_volume`      DECIMAL(12,3) DEFAULT NULL                COMMENT '最大容积(m³)',
  `max_sku`         INT           DEFAULT NULL                COMMENT '存放SKU最大种数',
  `attribute`       TINYINT       NOT NULL DEFAULT 0          COMMENT '属性 0=普通 1=常温 2=冷藏 3=冷冻 4=贵重 5=危险品 6=待检 7=不合格品',
  `status`          TINYINT       NOT NULL DEFAULT 1          COMMENT '状态 0=停用 1=空闲 2=占用中',
  `remark`          VARCHAR(500)  DEFAULT NULL                COMMENT '备注',
  `create_by`       BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`location_id`),
  UNIQUE KEY `uk_location_code` (`location_code`),
  KEY `idx_area_id` (`area_id`),
  KEY `idx_warehouse_id` (`warehouse_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库位表';

-- =========================================================================================
-- ========= 三、库存核心层（库存快照 + 库存流水） =========================================
-- =========================================================================================

-- 3.1 库存快照表（实时库存，维度：仓库+SKU+库位+批次）
--    说明：可用库存 = quantity - locked_qty
DROP TABLE IF EXISTS `wms_inventory`;
CREATE TABLE `wms_inventory` (
  `inventory_id`    BIGINT        NOT NULL                    COMMENT '库存ID',
  `warehouse_id`    BIGINT        NOT NULL                    COMMENT '仓库ID',
  `sku_id`          BIGINT        NOT NULL                    COMMENT 'SKU ID',
  `location_id`     BIGINT        DEFAULT NULL                COMMENT '库位ID（可空，未启用库位时为NULL）',
  `batch_no`        VARCHAR(64)   DEFAULT NULL                COMMENT '批次号（启用批次的SKU非空）',
  `produce_date`    DATE          DEFAULT NULL                COMMENT '生产日期',
  `expire_date`     DATE          DEFAULT NULL                COMMENT '有效期至',
  `supplier_batch`  VARCHAR(64)   DEFAULT NULL                COMMENT '供应商原始批次号',
  `quantity`        INT           NOT NULL DEFAULT 0          COMMENT '实存数量',
  `locked_qty`      INT           NOT NULL DEFAULT 0          COMMENT '锁定数量（已分配给未出库的出库单/调拨单）',
  `available_qty`   INT           NOT NULL DEFAULT 0          COMMENT '可用数量 = quantity - locked_qty（冗余便于查询，由触发器/代码维护）',
  `cost_price`      DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '移动加权平均成本单价',
  `total_amount`    DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '库存总金额 = quantity * cost_price（冗余）',
  `last_in_time`    DATETIME      DEFAULT NULL                COMMENT '最后入库时间',
  `last_out_time`   DATETIME      DEFAULT NULL                COMMENT '最后出库时间',
  `version`         INT           NOT NULL DEFAULT 0          COMMENT '乐观锁版本号（防并发超卖）',
  `create_by`       BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`inventory_id`),
  -- 核心唯一约束：同一仓库+SKU+库位+批次 只能有一条库存记录
  UNIQUE KEY `uk_wh_sku_loc_batch` (`warehouse_id`, `sku_id`, `location_id`, `batch_no`),
  KEY `idx_warehouse_id` (`warehouse_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_location_id` (`location_id`),
  KEY `idx_batch_no` (`batch_no`),
  KEY `idx_expire_date` (`expire_date`),
  KEY `idx_available_qty` (`available_qty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存快照表';

-- 3.2 库存流水表（每一笔库存变动都记录，用于追溯与对账）
DROP TABLE IF EXISTS `wms_inventory_log`;
CREATE TABLE `wms_inventory_log` (
  `log_id`          BIGINT        NOT NULL                    COMMENT '流水ID',
  `warehouse_id`    BIGINT        NOT NULL                    COMMENT '仓库ID',
  `sku_id`          BIGINT        NOT NULL                    COMMENT 'SKU ID',
  `location_id`     BIGINT        DEFAULT NULL                COMMENT '库位ID',
  `batch_no`        VARCHAR(64)   DEFAULT NULL                COMMENT '批次号',
  `bill_type`       VARCHAR(32)   NOT NULL                    COMMENT '关联单据类型：PURCHASE_IN / SALE_OUT / TRANSFER_OUT / TRANSFER_IN / DAMAGE_OUT / CHECK_PROFIT / CHECK_LOSS / OTHER_IN / OTHER_OUT',
  `bill_no`         VARCHAR(64)   NOT NULL                    COMMENT '关联单据号（如 RK202608240001）',
  `bill_item_id`    BIGINT        DEFAULT NULL                COMMENT '关联单据明细ID',
  `change_type`     TINYINT       NOT NULL                    COMMENT '变动类型 1=入库增加 2=出库减少 3=锁定(不改变实存) 4=解锁(不改变实存)',
  `before_qty`      INT           NOT NULL DEFAULT 0          COMMENT '变动前实存数量',
  `change_qty`      INT           NOT NULL                    COMMENT '变动数量（入库为正，出库为负，绝对值）',
  `after_qty`       INT           NOT NULL DEFAULT 0          COMMENT '变动后实存数量',
  `before_locked`   INT           NOT NULL DEFAULT 0          COMMENT '变动前锁定数量',
  `change_locked`   INT           NOT NULL DEFAULT 0          COMMENT '锁定变动数量',
  `after_locked`    INT           NOT NULL DEFAULT 0          COMMENT '变动后锁定数量',
  `cost_price`      DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '当时成本单价',
  `change_amount`   DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '变动金额',
  `remark`          VARCHAR(255)  DEFAULT NULL                COMMENT '备注',
  `operate_by`      BIGINT        DEFAULT NULL                COMMENT '操作人ID',
  `operate_name`    VARCHAR(32)   DEFAULT NULL                COMMENT '操作人姓名',
  `operate_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_bill` (`bill_type`, `bill_no`),
  KEY `idx_wh_sku` (`warehouse_id`, `sku_id`),
  KEY `idx_operate_time` (`operate_time`),
  KEY `idx_change_type` (`change_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';

-- =========================================================================================
-- ========= 四、采购管理模块（核心：采购订单 + 采购明细 + 状态日志） ======================
-- =========================================================================================

-- 4.1 采购订单主表
-- 状态流转：0=草稿 → 1=待审核 → 2=已审核(已下单) → 3=部分到货 → 4=全部到货 → 9=已完成 → 10=已取消
DROP TABLE IF EXISTS `wms_purchase_order`;
CREATE TABLE `wms_purchase_order` (
  `purchase_id`     BIGINT        NOT NULL                    COMMENT '采购单ID',
  `purchase_no`     VARCHAR(64)   NOT NULL                    COMMENT '采购单号，CG+yyyyMMdd+4位（唯一）',
  `supplier_id`     BIGINT        NOT NULL                    COMMENT '供应商ID',
  `supplier_name`   VARCHAR(255)  NOT NULL                    COMMENT '供应商名称（快照，防止供应商改名影响历史）',
  `warehouse_id`    BIGINT        NOT NULL                    COMMENT '到货仓库ID',
  `warehouse_name`  VARCHAR(128)  NOT NULL                    COMMENT '到货仓库名称（快照）',
  `purchaser_id`    BIGINT        DEFAULT NULL                COMMENT '采购员ID',
  `purchaser_name`  VARCHAR(32)   DEFAULT NULL                COMMENT '采购员姓名（快照）',
  `purchase_date`   DATE          NOT NULL                    COMMENT '采购日期',
  `expect_date`     DATE          DEFAULT NULL                COMMENT '期望到货日期',
  `currency`        CHAR(3)       NOT NULL DEFAULT 'CNY'      COMMENT '币种，默认CNY',
  `tax_rate`        DECIMAL(5,2)  NOT NULL DEFAULT 0.00       COMMENT '税率%',
  `discount_rate`   DECIMAL(5,2)  NOT NULL DEFAULT 0.00       COMMENT '整单折扣%，0=无折扣',
  `discount_amount` DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '整单折扣金额',
  `goods_amount`    DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '商品金额（各明细小计之和，税前）',
  `tax_amount`      DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '税额',
  `freight`         DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '运费',
  `other_fee`       DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '其他费用',
  `final_amount`    DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '应付总金额 = 商品金额 + 税 + 运费 + 其他 - 折扣',
  `paid_amount`     DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '已付款金额',
  `total_quantity`  INT           NOT NULL DEFAULT 0          COMMENT '商品总件数',
  `delivered_qty`   INT           NOT NULL DEFAULT 0          COMMENT '已到货件数（由入库单回写）',
  `unreceived_qty`  INT           NOT NULL DEFAULT 0          COMMENT '待到货件数（冗余 = 总件数-已到货）',
  `returned_qty`    INT           NOT NULL DEFAULT 0          COMMENT '已退货件数',
  `attachment`      JSON          DEFAULT NULL                COMMENT '附件URL列表',
  `remark`          VARCHAR(1000) DEFAULT NULL                COMMENT '整单备注',
  `status`          TINYINT       NOT NULL DEFAULT 0          COMMENT '状态 0=草稿 1=待审核 2=已审核 3=部分到货 4=全部到货 9=已完成 10=已取消',
  `audit_by`        BIGINT        DEFAULT NULL                COMMENT '审核人ID',
  `audit_name`      VARCHAR(32)   DEFAULT NULL                COMMENT '审核人姓名',
  `audit_time`      DATETIME      DEFAULT NULL                COMMENT '审核时间',
  `audit_remark`    VARCHAR(500)  DEFAULT NULL                COMMENT '审核意见',
  `create_by`       BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`purchase_id`),
  UNIQUE KEY `uk_purchase_no` (`purchase_no`),
  KEY `idx_supplier_id` (`supplier_id`),
  KEY `idx_warehouse_id` (`warehouse_id`),
  KEY `idx_purchase_date` (`purchase_date`),
  KEY `idx_status` (`status`),
  KEY `idx_purchaser_id` (`purchaser_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单主表';

-- 4.2 采购订单明细表（支持一批采购多个商品）
DROP TABLE IF EXISTS `wms_purchase_order_item`;
CREATE TABLE `wms_purchase_order_item` (
  `item_id`         BIGINT        NOT NULL                    COMMENT '采购明细ID',
  `purchase_id`     BIGINT        NOT NULL                    COMMENT '采购单ID',
  `purchase_no`     VARCHAR(64)   NOT NULL                    COMMENT '采购单号（冗余便于查询）',
  `line_no`         INT           NOT NULL                    COMMENT '行号，1/2/3...（排序用）',
  `sku_id`          BIGINT        NOT NULL                    COMMENT 'SKU ID',
  `sku_code`        VARCHAR(64)   NOT NULL                    COMMENT 'SKU编码（快照）',
  `sku_name`        VARCHAR(255)  NOT NULL                    COMMENT 'SKU名称（快照）',
  `spec_text`       VARCHAR(255)  DEFAULT NULL                COMMENT '规格文本（快照）',
  `unit_id`         BIGINT        DEFAULT NULL                COMMENT '单位ID',
  `unit_name`       VARCHAR(32)   DEFAULT NULL                COMMENT '单位名称（快照）',
  `quantity`        INT           NOT NULL                    COMMENT '采购数量',
  `purchase_price`  DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '采购单价（不含税）',
  `tax_rate`        DECIMAL(5,2)  NOT NULL DEFAULT 0.00       COMMENT '行税率%，默认取整单税率',
  `tax_amount`      DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '行税额',
  `subtotal`        DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '行小计 = quantity * purchase_price（不含税）',
  `delivered_qty`   INT           NOT NULL DEFAULT 0          COMMENT '已到货数量（由入库单明细回写）',
  `unreceived_qty`  INT           NOT NULL DEFAULT 0          COMMENT '待到货数量 = quantity - delivered_qty（冗余）',
  `returned_qty`    INT           NOT NULL DEFAULT 0          COMMENT '已退货数量',
  `expect_date`     DATE          DEFAULT NULL                COMMENT '行期望到货日（可覆盖整单）',
  `suggest_batch`   VARCHAR(64)   DEFAULT NULL                COMMENT '建议入库批次号',
  `remark`          VARCHAR(255)  DEFAULT NULL                COMMENT '行备注',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`item_id`),
  KEY `idx_purchase_id` (`purchase_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_purchase_no` (`purchase_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单明细表';

-- 4.3 采购单状态流转日志（审计用）
DROP TABLE IF EXISTS `wms_purchase_status_log`;
CREATE TABLE `wms_purchase_status_log` (
  `log_id`          BIGINT        NOT NULL                    COMMENT '日志ID',
  `purchase_id`     BIGINT        NOT NULL                    COMMENT '采购单ID',
  `purchase_no`     VARCHAR(64)   NOT NULL                    COMMENT '采购单号',
  `before_status`   TINYINT       DEFAULT NULL                COMMENT '变更前状态',
  `after_status`    TINYINT       NOT NULL                    COMMENT '变更后状态',
  `operate_type`    VARCHAR(32)   NOT NULL                    COMMENT '操作类型 CREATE/SUBMIT/AUDIT/REJECT/RECEIVE/FINISH/CANCEL',
  `operate_by`      BIGINT        DEFAULT NULL                COMMENT '操作人ID',
  `operate_name`    VARCHAR(32)   DEFAULT NULL                COMMENT '操作人姓名',
  `operate_remark`  VARCHAR(500)  DEFAULT NULL                COMMENT '操作备注/审批意见',
  `operate_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_purchase_id` (`purchase_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购单状态流转日志';

-- =========================================================================================
-- ========= 五、入库管理模块（6类入库：采购入库/调拨入库/销售退货/采购退货/盘盈/其他） ===
-- =========================================================================================

-- 5.1 入库单主表
-- 状态：0=草稿 → 1=待审核 → 2=已审核(入库生效) → 10=已作废
DROP TABLE IF EXISTS `wms_stock_in`;
CREATE TABLE `wms_stock_in` (
  `stock_in_id`     BIGINT        NOT NULL                    COMMENT '入库单ID',
  `stock_in_no`     VARCHAR(64)   NOT NULL                    COMMENT '入库单号，RK+yyyyMMdd+4位（唯一）',
  `in_type`         VARCHAR(32)   NOT NULL                    COMMENT '入库类型：IN_PURCHASE=采购入库 IN_TRANSFER=调拨入库 IN_SALE_RETURN=销售退货 IN_PURCHASE_RETURN=采购退货退库 IN_INVENTORY_PROFIT=盘盈入库 IN_OTHER=其他入库',
  `source_bill_type` VARCHAR(32)  DEFAULT NULL                COMMENT '源单类型，如 PURCHASE_ORDER / TRANSFER_ORDER / SALE_ORDER',
  `source_bill_id`  BIGINT        DEFAULT NULL                COMMENT '源单ID',
  `source_bill_no`  VARCHAR(64)   DEFAULT NULL                COMMENT '源单号（如采购单号CG001）',
  `warehouse_id`    BIGINT        NOT NULL                    COMMENT '入库仓库ID',
  `warehouse_name`  VARCHAR(128)  NOT NULL                    COMMENT '入库仓库名称（快照）',
  `supplier_id`     BIGINT        DEFAULT NULL                COMMENT '供应商ID（采购入库必填）',
  `supplier_name`   VARCHAR(255)  DEFAULT NULL                COMMENT '供应商名称（快照）',
  `customer_id`     BIGINT        DEFAULT NULL                COMMENT '客户ID（销售退货必填）',
  `customer_name`   VARCHAR(255)  DEFAULT NULL                COMMENT '客户名称（快照）',
  `transfer_id`     BIGINT        DEFAULT NULL                COMMENT '关联调拨单ID（调拨入库时）',
  `in_date`         DATE          NOT NULL                    COMMENT '入库日期',
  `total_quantity`  INT           NOT NULL DEFAULT 0          COMMENT '入库总件数',
  `goods_amount`    DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '入库商品总金额（成本总额）',
  `attachment`      JSON          DEFAULT NULL                COMMENT '附件URL列表（收货单照片等）',
  `remark`          VARCHAR(1000) DEFAULT NULL                COMMENT '整单备注',
  `status`          TINYINT       NOT NULL DEFAULT 0          COMMENT '状态 0=草稿 1=待审核 2=已审核(已入库) 10=已作废',
  `receiver_id`     BIGINT        DEFAULT NULL                COMMENT '收货人ID',
  `receiver_name`   VARCHAR(32)   DEFAULT NULL                COMMENT '收货人姓名',
  `audit_by`        BIGINT        DEFAULT NULL                COMMENT '审核人ID',
  `audit_name`      VARCHAR(32)   DEFAULT NULL                COMMENT '审核人姓名',
  `audit_time`      DATETIME      DEFAULT NULL                COMMENT '审核时间',
  `audit_remark`    VARCHAR(500)  DEFAULT NULL                COMMENT '审核意见',
  `create_by`       BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`stock_in_id`),
  UNIQUE KEY `uk_stock_in_no` (`stock_in_no`),
  KEY `idx_in_type` (`in_type`),
  KEY `idx_source_bill` (`source_bill_type`, `source_bill_no`),
  KEY `idx_warehouse_id` (`warehouse_id`),
  KEY `idx_supplier_id` (`supplier_id`),
  KEY `idx_in_date` (`in_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单主表';

-- 5.2 入库单明细表（支持一批入库多个商品）
DROP TABLE IF EXISTS `wms_stock_in_item`;
CREATE TABLE `wms_stock_in_item` (
  `item_id`         BIGINT        NOT NULL                    COMMENT '入库明细ID',
  `stock_in_id`     BIGINT        NOT NULL                    COMMENT '入库单ID',
  `stock_in_no`     VARCHAR(64)   NOT NULL                    COMMENT '入库单号（冗余）',
  `source_item_id`  BIGINT        DEFAULT NULL                COMMENT '源单明细ID（如采购明细ID，便于回写）',
  `line_no`         INT           NOT NULL                    COMMENT '行号',
  `sku_id`          BIGINT        NOT NULL                    COMMENT 'SKU ID',
  `sku_code`        VARCHAR(64)   NOT NULL                    COMMENT 'SKU编码（快照）',
  `sku_name`        VARCHAR(255)  NOT NULL                    COMMENT 'SKU名称（快照）',
  `spec_text`       VARCHAR(255)  DEFAULT NULL                COMMENT '规格文本（快照）',
  `unit_id`         BIGINT        DEFAULT NULL                COMMENT '单位ID',
  `unit_name`       VARCHAR(32)   DEFAULT NULL                COMMENT '单位名称（快照）',
  `expected_qty`    INT           NOT NULL DEFAULT 0          COMMENT '应收数量（如采购数/调拨数）',
  `actual_qty`      INT           NOT NULL                    COMMENT '实收数量（真实入库数，可多收/少收）',
  `diff_qty`        INT           NOT NULL DEFAULT 0          COMMENT '差异数量 = actual_qty - expected_qty（冗余）',
  `cost_price`      DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '入库成本单价',
  `subtotal`        DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '入库小记 = actual_qty * cost_price',
  `batch_no`        VARCHAR(64)   DEFAULT NULL                COMMENT '入库批次号（SKU启用批次时必填）',
  `produce_date`    DATE          DEFAULT NULL                COMMENT '生产日期',
  `expire_date`     DATE          DEFAULT NULL                COMMENT '有效期至',
  `supplier_batch`  VARCHAR(64)   DEFAULT NULL                COMMENT '供应商原始批次',
  `location_id`     BIGINT        DEFAULT NULL                COMMENT '上架库位ID（启用库位时使用）',
  `location_code`   VARCHAR(64)   DEFAULT NULL                COMMENT '库位编码（快照）',
  `sn_list`         JSON          DEFAULT NULL                COMMENT 'SN序列号列表JSON（SKU启用SN时必填，如 ["SN001","SN002"]）',
  `remark`          VARCHAR(255)  DEFAULT NULL                COMMENT '行备注',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`item_id`),
  KEY `idx_stock_in_id` (`stock_in_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_stock_in_no` (`stock_in_no`),
  KEY `idx_source_item_id` (`source_item_id`),
  KEY `idx_batch_no` (`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单明细表';

-- 5.3 入库单状态流转日志
DROP TABLE IF EXISTS `wms_stock_in_status_log`;
CREATE TABLE `wms_stock_in_status_log` (
  `log_id`          BIGINT        NOT NULL                    COMMENT '日志ID',
  `stock_in_id`     BIGINT        NOT NULL                    COMMENT '入库单ID',
  `stock_in_no`     VARCHAR(64)   NOT NULL                    COMMENT '入库单号',
  `before_status`   TINYINT       DEFAULT NULL                COMMENT '变更前状态',
  `after_status`    TINYINT       NOT NULL                    COMMENT '变更后状态',
  `operate_type`    VARCHAR(32)   NOT NULL                    COMMENT '操作类型 CREATE/SUBMIT/AUDIT/REJECT/VOID',
  `operate_by`      BIGINT        DEFAULT NULL                COMMENT '操作人ID',
  `operate_name`    VARCHAR(32)   DEFAULT NULL                COMMENT '操作人姓名',
  `operate_remark`  VARCHAR(500)  DEFAULT NULL                COMMENT '操作备注/审批意见',
  `operate_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_stock_in_id` (`stock_in_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单状态流转日志';

-- =========================================================================================
-- ========= 六、出库管理模块（6类出库：销售出库/调拨出库/报损出库/盘亏出库/采购退货/其他）
-- =========================================================================================

-- 6.1 出库单主表
-- 状态：0=草稿 → 1=待审核 → 2=已审核(锁定库存) → 3=拣货中 → 4=已复核 → 5=已出库(扣减生效) → 10=已作废
DROP TABLE IF EXISTS `wms_stock_out`;
CREATE TABLE `wms_stock_out` (
  `stock_out_id`    BIGINT        NOT NULL                    COMMENT '出库单ID',
  `stock_out_no`    VARCHAR(64)   NOT NULL                    COMMENT '出库单号，CK+yyyyMMdd+4位（唯一）',
  `out_type`        VARCHAR(32)   NOT NULL                    COMMENT '出库类型：OUT_SALE=销售出库 OUT_TRANSFER=调拨出库 OUT_DAMAGE=报损出库 OUT_INVENTORY_LOSS=盘亏出库 OUT_PURCHASE_RETURN=采购退货出库 OUT_OTHER=其他出库',
  `source_bill_type` VARCHAR(32)  DEFAULT NULL                COMMENT '源单类型，如 SALE_ORDER / TRANSFER_ORDER / DAMAGE_ORDER',
  `source_bill_id`  BIGINT        DEFAULT NULL                COMMENT '源单ID',
  `source_bill_no`  VARCHAR(64)   DEFAULT NULL                COMMENT '源单号（如销售单号XS001）',
  `warehouse_id`    BIGINT        NOT NULL                    COMMENT '出库仓库ID',
  `warehouse_name`  VARCHAR(128)  NOT NULL                    COMMENT '出库仓库名称（快照）',
  `customer_id`     BIGINT        DEFAULT NULL                COMMENT '客户ID（销售出库必填）',
  `customer_name`   VARCHAR(255)  DEFAULT NULL                COMMENT '客户名称（快照）',
  `supplier_id`     BIGINT        DEFAULT NULL                COMMENT '供应商ID（采购退货必填）',
  `supplier_name`   VARCHAR(255)  DEFAULT NULL                COMMENT '供应商名称（快照）',
  `transfer_id`     BIGINT        DEFAULT NULL                COMMENT '关联调拨单ID（调拨出库时）',
  `damage_id`       BIGINT        DEFAULT NULL                COMMENT '关联报损单ID（报损出库时）',
  `out_date`        DATE          NOT NULL                    COMMENT '出库日期',
  `total_quantity`  INT           NOT NULL DEFAULT 0          COMMENT '出库总件数',
  `goods_amount`    DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '出库商品总金额（成本总额）',
  `sale_amount`     DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '销售金额（销售出库时的收入金额）',
  -- 收货/物流信息
  `receiver_name`   VARCHAR(32)   DEFAULT NULL                COMMENT '收货人姓名',
  `receiver_phone`  VARCHAR(20)   DEFAULT NULL                COMMENT '收货人电话',
  `receiver_province` VARCHAR(32) DEFAULT NULL                COMMENT '收货省',
  `receiver_city`   VARCHAR(32)   DEFAULT NULL                COMMENT '收货市',
  `receiver_district` VARCHAR(32) DEFAULT NULL                COMMENT '收货区县',
  `receiver_address` VARCHAR(500) DEFAULT NULL                COMMENT '收货详细地址',
  `express_company` VARCHAR(64)   DEFAULT NULL                COMMENT '物流公司',
  `express_no`      VARCHAR(64)   DEFAULT NULL                COMMENT '物流运单号',
  `express_fee`     DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '运费',
  `attachment`      JSON          DEFAULT NULL                COMMENT '附件URL列表',
  `remark`          VARCHAR(1000) DEFAULT NULL                COMMENT '整单备注',
  `status`          TINYINT       NOT NULL DEFAULT 0          COMMENT '状态 0=草稿 1=待审核 2=已审核(锁定库存) 3=拣货中 4=已复核 5=已出库 10=已作废',
  `picker_id`       BIGINT        DEFAULT NULL                COMMENT '拣货人ID',
  `picker_name`     VARCHAR(32)   DEFAULT NULL                COMMENT '拣货人姓名',
  `picker_time`     DATETIME      DEFAULT NULL                COMMENT '拣货完成时间',
  `checker_id`      BIGINT        DEFAULT NULL                COMMENT '复核人ID',
  `checker_name`    VARCHAR(32)   DEFAULT NULL                COMMENT '复核人姓名',
  `check_time`      DATETIME      DEFAULT NULL                COMMENT '复核完成时间',
  `shipper_id`      BIGINT        DEFAULT NULL                COMMENT '发货人ID',
  `shipper_name`    VARCHAR(32)   DEFAULT NULL                COMMENT '发货人姓名',
  `ship_time`       DATETIME      DEFAULT NULL                COMMENT '实际发货时间',
  `audit_by`        BIGINT        DEFAULT NULL                COMMENT '审核人ID',
  `audit_name`      VARCHAR(32)   DEFAULT NULL                COMMENT '审核人姓名',
  `audit_time`      DATETIME      DEFAULT NULL                COMMENT '审核时间',
  `audit_remark`    VARCHAR(500)  DEFAULT NULL                COMMENT '审核意见',
  `create_by`       BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`stock_out_id`),
  UNIQUE KEY `uk_stock_out_no` (`stock_out_no`),
  KEY `idx_out_type` (`out_type`),
  KEY `idx_source_bill` (`source_bill_type`, `source_bill_no`),
  KEY `idx_warehouse_id` (`warehouse_id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_out_date` (`out_date`),
  KEY `idx_status` (`status`),
  KEY `idx_express_no` (`express_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单主表';

-- 6.2 出库单明细表（支持一批出库多个商品，FIFO/FEFO自动分配批次）
DROP TABLE IF EXISTS `wms_stock_out_item`;
CREATE TABLE `wms_stock_out_item` (
  `item_id`         BIGINT        NOT NULL                    COMMENT '出库明细ID',
  `stock_out_id`    BIGINT        NOT NULL                    COMMENT '出库单ID',
  `stock_out_no`    VARCHAR(64)   NOT NULL                    COMMENT '出库单号（冗余）',
  `source_item_id`  BIGINT        DEFAULT NULL                COMMENT '源单明细ID（如销售明细ID）',
  `line_no`         INT           NOT NULL                    COMMENT '行号',
  `sku_id`          BIGINT        NOT NULL                    COMMENT 'SKU ID',
  `sku_code`        VARCHAR(64)   NOT NULL                    COMMENT 'SKU编码（快照）',
  `sku_name`        VARCHAR(255)  NOT NULL                    COMMENT 'SKU名称（快照）',
  `spec_text`       VARCHAR(255)   DEFAULT NULL                COMMENT '规格文本（快照）',
  `unit_id`         BIGINT        DEFAULT NULL                COMMENT '单位ID',
  `unit_name`       VARCHAR(32)   DEFAULT NULL                COMMENT '单位名称（快照）',
  `expected_qty`    INT           NOT NULL DEFAULT 0          COMMENT '应出数量（如销售下单数）',
  `actual_qty`      INT           NOT NULL                    COMMENT '实出数量（拣货后的真实出库数）',
  `diff_qty`        INT           NOT NULL DEFAULT 0          COMMENT '差异 = actual_qty - expected_qty',
  `picked_qty`      INT           NOT NULL DEFAULT 0          COMMENT '已拣货数量',
  `cost_price`      DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '出库成本单价（移动加权快照）',
  `subtotal_cost`   DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '出库成本小计 = actual_qty * cost_price',
  `sale_price`      DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '销售单价（销售出库使用）',
  `subtotal_sale`   DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '销售小计 = actual_qty * sale_price',
  `discount_rate`   DECIMAL(5,2)  NOT NULL DEFAULT 0.00       COMMENT '行折扣%',
  -- 库存分配信息：按FIFO/FEFO拆成多条分配记录可放到子表，这里记录汇总，具体批次用JSON记录
  `allocation_rule` TINYINT       NOT NULL DEFAULT 1          COMMENT '分配策略 1=FIFO先入先出 2=FEFO先到期先出 3=手动指定批次',
  `allocation_json` JSON          DEFAULT NULL                COMMENT '批次分配明细JSON，数组格式 [{"batch_no":"A01","location_id":1,"qty":50},...]',
  `batch_no`        VARCHAR(64)   DEFAULT NULL                COMMENT '出库批次号（手动指定时或单批出库时使用）',
  `produce_date`    DATE          DEFAULT NULL                COMMENT '生产日期',
  `expire_date`     DATE          DEFAULT NULL                COMMENT '有效期',
  `location_id`     BIGINT        DEFAULT NULL                COMMENT '出库库位ID',
  `location_code`   VARCHAR(64)   DEFAULT NULL                COMMENT '库位编码（快照）',
  `sn_list`         JSON          DEFAULT NULL                COMMENT 'SN序列号列表JSON',
  `remark`          VARCHAR(255)  DEFAULT NULL                COMMENT '行备注',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`item_id`),
  KEY `idx_stock_out_id` (`stock_out_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_stock_out_no` (`stock_out_no`),
  KEY `idx_source_item_id` (`source_item_id`),
  KEY `idx_batch_no` (`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单明细表';

-- 6.3 出库单状态流转日志
DROP TABLE IF EXISTS `wms_stock_out_status_log`;
CREATE TABLE `wms_stock_out_status_log` (
  `log_id`          BIGINT        NOT NULL                    COMMENT '日志ID',
  `stock_out_id`    BIGINT        NOT NULL                    COMMENT '出库单ID',
  `stock_out_no`    VARCHAR(64)   NOT NULL                    COMMENT '出库单号',
  `before_status`   TINYINT       DEFAULT NULL                COMMENT '变更前状态',
  `after_status`    TINYINT       NOT NULL                    COMMENT '变更后状态',
  `operate_type`    VARCHAR(32)   NOT NULL                    COMMENT '操作类型 CREATE/SUBMIT/AUDIT/REJECT/PICK/CHECK/SHIP/VOID',
  `operate_by`      BIGINT        DEFAULT NULL                COMMENT '操作人ID',
  `operate_name`    VARCHAR(32)   DEFAULT NULL                COMMENT '操作人姓名',
  `operate_remark`  VARCHAR(500)  DEFAULT NULL                COMMENT '操作备注/审批意见',
  `operate_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_stock_out_id` (`stock_out_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单状态流转日志';

-- =========================================================================================
-- ========= 六(补)、报损单模块（报损单 + 明细 + 状态日志） ===============================
-- =========================================================================================

-- 6.4 报损单主表
DROP TABLE IF EXISTS `wms_loss_order`;
CREATE TABLE `wms_loss_order` (
  `loss_id`         BIGINT        NOT NULL                    COMMENT '报损单ID',
  `loss_no`         VARCHAR(64)   NOT NULL                    COMMENT '报损单号，BS+yyyyMMdd+4位（唯一）',
  `warehouse_id`    BIGINT        NOT NULL                    COMMENT '报损仓库ID',
  `warehouse_name`  VARCHAR(128)  NOT NULL                    COMMENT '仓库名称（快照）',
  `loss_type`       TINYINT       NOT NULL DEFAULT 0          COMMENT '报损类型 1=破损 2=过期 3=丢失 4=盘亏 9=其他',
  `total_qty`       INT           NOT NULL DEFAULT 0          COMMENT '报损总数量',
  `total_amount`    DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '报损总金额（成本总额）',
  `loss_date`       DATE          NOT NULL                    COMMENT '报损日期',
  `status`          TINYINT       NOT NULL DEFAULT 0          COMMENT '状态 0=草稿 1=待审核 2=已审核 3=已处理 5=已作废',
  `handle_by`       BIGINT        DEFAULT NULL                COMMENT '处理人ID',
  `handle_time`     DATETIME      DEFAULT NULL                COMMENT '处理时间',
  `audit_by`        BIGINT        DEFAULT NULL                COMMENT '审核人ID',
  `audit_time`      DATETIME      DEFAULT NULL                COMMENT '审核时间',
  `remark`          VARCHAR(1000) DEFAULT NULL                COMMENT '整单备注',
  `create_by`       BIGINT        DEFAULT NULL                COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       BIGINT        DEFAULT NULL                COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT           NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
  `deleted`         TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`loss_id`),
  UNIQUE KEY `uk_loss_no` (`loss_no`),
  KEY `idx_warehouse_id` (`warehouse_id`),
  KEY `idx_loss_date` (`loss_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报损单主表';

-- 6.5 报损单明细表
DROP TABLE IF EXISTS `wms_loss_order_item`;
CREATE TABLE `wms_loss_order_item` (
  `item_id`         BIGINT        NOT NULL                    COMMENT '报损明细ID',
  `loss_id`         BIGINT        NOT NULL                    COMMENT '报损单ID',
  `loss_no`         VARCHAR(64)   NOT NULL                    COMMENT '报损单号（冗余）',
  `line_no`         INT           NOT NULL                    COMMENT '行号',
  `sku_id`          BIGINT        NOT NULL                    COMMENT 'SKU ID',
  `sku_code`        VARCHAR(64)   NOT NULL                    COMMENT 'SKU编码（快照）',
  `sku_name`        VARCHAR(255)  NOT NULL                    COMMENT 'SKU名称（快照）',
  `spec_text`       VARCHAR(255)  DEFAULT NULL                COMMENT '规格文本（快照）',
  `unit_id`         BIGINT        DEFAULT NULL                COMMENT '单位ID',
  `unit_name`       VARCHAR(32)   DEFAULT NULL                COMMENT '单位名称（快照）',
  `loss_qty`        INT           NOT NULL DEFAULT 0          COMMENT '报损数量',
  `cost_price`      DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '成本单价（快照）',
  `subtotal`        DECIMAL(18,4) NOT NULL DEFAULT 0.0000     COMMENT '行小计 = loss_qty * cost_price',
  `batch_no`        VARCHAR(64)   DEFAULT NULL                COMMENT '批次号',
  `location_id`     BIGINT        DEFAULT NULL                COMMENT '库位ID',
  `location_code`   VARCHAR(64)   DEFAULT NULL                COMMENT '库位编码（快照）',
  `loss_reason`     VARCHAR(500)  DEFAULT NULL                COMMENT '报损原因',
  `remark`          VARCHAR(255)  DEFAULT NULL                COMMENT '行备注',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT       NOT NULL DEFAULT 0          COMMENT '逻辑删除 0=未删 1=已删',
  PRIMARY KEY (`item_id`),
  KEY `idx_loss_id` (`loss_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_loss_no` (`loss_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报损单明细表';

-- 6.6 报损单状态流转日志
DROP TABLE IF EXISTS `wms_loss_status_log`;
CREATE TABLE `wms_loss_status_log` (
  `log_id`          BIGINT        NOT NULL                    COMMENT '日志ID',
  `loss_id`         BIGINT        NOT NULL                    COMMENT '报损单ID',
  `loss_no`         VARCHAR(64)   NOT NULL                    COMMENT '报损单号',
  `before_status`   TINYINT       DEFAULT NULL                COMMENT '变更前状态',
  `after_status`    TINYINT       NOT NULL                    COMMENT '变更后状态',
  `operate_type`    VARCHAR(32)   NOT NULL                    COMMENT '操作类型 CREATE/SUBMIT/AUDIT/HANDLE/VOID',
  `operate_by`      BIGINT        DEFAULT NULL                COMMENT '操作人ID',
  `operate_name`    VARCHAR(32)   DEFAULT NULL                COMMENT '操作人姓名',
  `operate_remark`  VARCHAR(500)  DEFAULT NULL                COMMENT '操作备注/审批意见',
  `operate_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_loss_id` (`loss_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报损单状态流转日志';

-- =========================================================================================
-- ========= 七、视图（可选，便于报表查询使用） ===========================================
-- =========================================================================================

-- 7.1 实时库存宽表视图（多表关联方便列表展示）
DROP VIEW IF EXISTS `v_inventory_full`;
CREATE VIEW `v_inventory_full` AS
SELECT
  i.inventory_id,
  i.warehouse_id,
  w.warehouse_code,
  w.warehouse_name,
  i.sku_id,
  s.sku_code,
  s.sku_name,
  s.spec_text,
  s.barcode,
  c.category_id,
  c.category_name,
  b.brand_id,
  b.brand_name,
  u.unit_id,
  u.unit_name,
  i.location_id,
  l.location_code,
  i.batch_no,
  i.produce_date,
  i.expire_date,
  DATEDIFF(i.expire_date, CURDATE()) AS days_to_expire,
  i.quantity,
  i.locked_qty,
  i.available_qty,
  i.cost_price,
  i.total_amount,
  s.min_stock,
  s.max_stock,
  CASE
    WHEN i.available_qty < s.min_stock THEN 'LOW'
    WHEN i.available_qty > s.max_stock AND s.max_stock > 0 THEN 'HIGH'
    ELSE 'NORMAL'
  END AS stock_warn_level,
  i.last_in_time,
  i.last_out_time
FROM wms_inventory i
LEFT JOIN wms_warehouse w ON i.warehouse_id = w.warehouse_id
LEFT JOIN wms_goods_sku s ON i.sku_id = s.sku_id
LEFT JOIN wms_category c ON s.category_id = c.category_id
LEFT JOIN wms_brand b ON s.brand_id = b.brand_id
LEFT JOIN wms_unit u ON s.unit_id = u.unit_id
LEFT JOIN wms_location l ON i.location_id = l.location_id
WHERE i.quantity > 0;

-- 7.2 采购入库对账视图（采购单 vs 入库单）
DROP VIEW IF EXISTS `v_purchase_reconcile`;
CREATE VIEW `v_purchase_reconcile` AS
SELECT
  p.purchase_id,
  p.purchase_no,
  p.supplier_id,
  p.supplier_name,
  p.warehouse_id,
  p.warehouse_name,
  p.purchase_date,
  p.status AS purchase_status,
  pi.item_id,
  pi.sku_id,
  pi.sku_code,
  pi.sku_name,
  pi.spec_text,
  pi.unit_name,
  pi.quantity AS purchase_qty,
  pi.purchase_price,
  pi.subtotal AS purchase_amount,
  pi.delivered_qty,
  pi.unreceived_qty,
  pi.returned_qty,
  CASE
    WHEN pi.unreceived_qty = 0 AND pi.quantity > 0 THEN 'FULL'
    WHEN pi.delivered_qty = 0 THEN 'NONE'
    ELSE 'PARTIAL'
  END AS receive_status
FROM wms_purchase_order p
JOIN wms_purchase_order_item pi ON p.purchase_id = pi.purchase_id
WHERE p.deleted = 0;

-- =========================================================================================
-- ========= 八、附录：初始化数据（可选执行） ===============================================
-- =========================================================================================

-- 8.1 初始化超级管理员（用户名: admin / 密码: admin123，BCrypt需生成后填入，此处为示例占位）
-- 实际部署时请使用 PasswordEncoder.encode("admin123") 生成真实密码哈希
-- INSERT INTO `sys_user` (`user_id`, `dept_id`, `username`, `password`, `real_name`, `status`) VALUES
-- (1, 0, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 1);

-- 8.2 初始化预置角色
-- INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `status`) VALUES
-- (1, '系统管理员', 'ROLE_ADMIN',   1, 1, 1),
-- (2, '仓库主管',   'ROLE_WH_MANAGER', 2, 2, 1),
-- (3, '采购专员',   'ROLE_PURCHASER',  3, 2, 1),
-- (4, '仓管员',     'ROLE_KEEPER',     4, 2, 1),
-- (5, '销售专员',   'ROLE_SALES',      5, 2, 1),
-- (6, '财务人员',   'ROLE_FINANCE',    6, 1, 1),
-- (7, '普通查看者', 'ROLE_VIEWER',     7, 5, 1);

-- =========================================================================================
-- ========= 九、执行说明 ===================================================================
-- =========================================================================================
-- 1. 本脚本适用于 MySQL 8.0+，请确认数据库版本 SELECT VERSION();
-- 2. 执行方式：
--    mysql -uroot -p < wms_core_tables.sql
--    或在 Navicat / DataGrip 中打开后全选执行
-- 3. 表名前缀约定：
--    sys_*  系统权限层
--    wms_*  WMS业务层
-- 4. 核心单据状态枚举请参考各表 COMMENT 说明，避免随意插入数字
-- 5. 建议后续分库分表策略（当数据量>1000万）：
--    wms_inventory_log 按 operate_time 月分区
--    wms_stock_in / wms_stock_out 按 in_date/out_date 月分区
-- 6. 事务控制：入库审核 / 出库审核 必须包裹在一个事务中：
--    BEGIN;
--      更新库存快照（UPDATE ... WHERE version = ?）
--      插入库存流水（INSERT INTO wms_inventory_log）
--      更新入库单状态、回写源单
--    COMMIT;
-- =========================================================================================
