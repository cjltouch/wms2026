# WMS 仓储管理系统 - 完整培训手册

> 版本：v1.0 | 更新日期：2026-08-26

---

## 目录

1. [系统概述](#1-系统概述)
2. [基础数据模块](#2-基础数据模块)
3. [采购业务流程](#3-采购业务流程)
4. [入库业务流程](#4-入库业务流程)
5. [出库业务流程](#5-出库业务流程)
6. [调拨业务流程](#6-调拨业务流程)
7. [销售业务流程](#7-销售业务流程)
8. [报损业务流程](#8-报损业务流程)
9. [盘点业务流程](#9-盘点业务流程)
10. [库存管理](#10-库存管理)
11. [系统权限与角色](#11-系统权限与角色)
12. [各部门培训要点](#12-各部门培训要点)

---

## 1. 系统概述

### 1.1 系统架构

WMS系统采用前后端分离架构：
- **后端**：Spring Boot + MyBatis Plus + MySQL
- **前端**：Vue3 + TypeScript + Vite
- **认证**：JWT Token 无状态认证

代码参考：
- 后端主入口：[WmsApplication.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/WmsApplication.java)
- 权限控制：[PreAuthorizeAspect.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/auth/PreAuthorizeAspect.java)

### 1.2 业务模块全景

```
┌─────────────────────────────────────────────────────────────┐
│                        WMS 系统全景                          │
├─────────────┬─────────────┬─────────────┬───────────────────┤
│  基础数据   │  采购入库   │  销售出库   │  库存管理         │
│  ─────────  │  ─────────  │  ─────────  │  ─────────        │
│  • 仓库     │  • 采购单   │  • 销售单   │  • 实时库存       │
│  • 库区     │  • 入库单   │  • 出库单   │  • 库存流水       │
│  • 库位     │             │             │  • 盘点单         │
│  • 商品     │  内部流转   │  异常处理   │  • 报损单         │
│  • 供应商   │  ─────────  │  ─────────  │                   │
│  • 客户     │  • 调拨单   │  • 报损单   │                   │
│  • 单位     │             │             │                   │
└─────────────┴─────────────┴─────────────┴───────────────────┘
```

### 1.3 核心单据状态通用规则

所有业务单据均遵循以下通用操作模式：
| 操作 | 说明 | 权限标识 |
|------|------|---------|
| 保存草稿 | 创建单据，不产生任何业务影响 | `*:add` |
| 修改 | 仅草稿/已提交状态可修改 | `*:edit` |
| 删除 | 仅草稿状态可删除 | `*:remove` |
| 提交 | 草稿 → 待审核，单据不可随意修改 | `*:submit` |
| 审核 | 审批通过，触发后续业务动作 | `*:audit` |
| 反审核 | 撤销审核（部分单据有时限） | `*:unaudit` |
| 作废 | 终止单据，作废后不可恢复 | `*:void` |

---

## 2. 基础数据模块

### 2.1 模块职责

基础数据是所有业务流转的前提，必须先维护好才能创建业务单据。

### 2.2 数据层级关系

```
仓库 (Warehouse)
  └── 库区 (Area)
       └── 库位 (Location)

商品分类 (Category)
  └── 商品品牌 (Brand)
       └── SPU (Standard Product Unit - 标准产品单元)
            └── SKU (Stock Keeping Unit - 库存量单位)
                 └── 库存 (Inventory) [按 仓库+SKU+库位+批次 维度]

合作伙伴：供应商 (Supplier) / 客户 (Customer)
计量单位：Unit
```

代码参考：
- 仓库实体：[WmsWarehouse.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/basedata/warehouse/entity/WmsWarehouse.java)
- 商品SKU实体：[WmsGoodsSku.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/basedata/goods/entity/WmsGoodsSku.java)

### 2.3 操作顺序

**正确顺序：**
```
仓库 → 库区 → 库位
         ↓
计量单位 → 商品分类 → 品牌 → SPU → SKU
         ↓
供应商 / 客户
```

**⚠️ 注意事项：**
- 没有库位就无法做入库上架操作
- 没有SKU就无法录入任何单据明细
- SKU一旦被使用，建议不要删除，改为停用

---

## 3. 采购业务流程

### 3.1 流程概述

采购流程用于管理从供应商订货到商品入库的全过程。

### 3.2 采购单状态机

```
         保存            提交            审核(通过)
[草稿 0] ────→ [已提交 1] ────→ [已审核 2]
   │              │                │
   │删除          │反审核(无到货)   │全部到货后
   ↓              ↓                ↓
 [删除]       [已提交 1]       [已完成 4]
                  │
                  │作废(无到货)
                  ↓
             [已作废 5]
```

状态说明（参考 [WmsPurchaseOrder.java#L66](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/purchase/entity/WmsPurchaseOrder.java#L66)）：
| 状态码 | 状态名 | 说明 | 可执行操作 |
|-------|-------|------|-----------|
| 0 | 草稿 | 刚创建未提交 | 修改、删除、提交 |
| 1 | 已提交 | 待审核 | 修改、反审核（退回草稿）、审核、作废 |
| 2 | 已审核 | 审批通过，等待到货 | 反审核（无到货时） |
| 4 | 已完成 | 商品全部到货入库 | —（终态） |
| 5 | 已作废 | 单据作废 | —（终态） |

### 3.3 详细操作步骤

| 步骤 | 操作人 | 操作 | 系统动作 | 数据影响 |
|------|-------|------|---------|---------|
| 1 | 采购员 | 新增采购单 | 保存草稿 | 写入采购单+明细，状态=0 |
| 2 | 采购员 | 提交审核 | 状态变更 | 0→1，记录状态日志 |
| 3 | 采购主管 | 审核通过 | 状态变更+乐观锁校验 | 1→2，记录审核人/时间 |
| 4 | 仓库员 | 【入库模块】根据采购单号创建入库单 | — | 见入库流程 |
| 5 | — | （入库上架后）自动回写 | 自动更新到货数量 | deliveredQty↑, unreceivedQty↓ |
| 6 | — | （全部到货后）自动完成 | 状态自动变更 | unreceivedQty=0 时，2→4 |

代码参考：
- 审核逻辑：[WmsPurchaseOrderServiceImpl.java#L162-L188](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/purchase/service/impl/WmsPurchaseOrderServiceImpl.java#L162-L188)
- 入库回写采购：[WmsStockInServiceImpl.java#L261-L296](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/stockin/service/impl/WmsStockInServiceImpl.java#L261-L296)

### 3.4 关键数据字段

- `purchaseNo`：采购单号（唯一）
- `supplierId`：供应商ID
- `warehouseId`：入仓仓库ID
- `totalQty`：采购总数量
- `deliveredQty`：已到货数量（入库单回写）
- `unreceivedQty`：未到货数量

### 3.5 ⚠️ 限制条件

- **删除**：仅草稿状态可删除
- **反审核**：仅已审核状态 且 `deliveredQty=0`（无到货记录）
- **作废**：仅未到货的单据可作废

---

## 4. 入库业务流程

### 4.1 流程概述

入库流程管理商品进入仓库的全过程，支持从采购单引入或手工新建。入库类型：
- `type=1`：采购入库（关联采购单）
- 其他类型：可扩展为退货入库、盘盈入库等

### 4.2 入库单状态机

```
         保存           提交           自动/手动         上架审核
[草稿 0] ────→ [已提交 1] ────→ [待上架 2] ─────────→ [已上架 3]
   │              │                │                     │
   │删除          │修改            │反审核(5分钟内)      │
   ↓              ↓                ↓                     ↓
 [删除]       [修改保存]       [待上架 2]            [已上架 3]
                                                          │
                                                          │作废(仅上架前可作废)
                                                          ↓
                                                    [已作废 4]
```

状态说明（参考 [WmsStockIn.java#L58](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/stockin/entity/WmsStockIn.java#L58)）：
| 状态码 | 状态名 | 说明 | 可执行操作 |
|-------|-------|------|-----------|
| 0 | 草稿 | 新建未提交 | 修改、删除、提交 |
| 1 | 已提交 | 已提交待收货 | 修改、提交(→待上架) |
| 2 | 待上架 | 商品已收货，等待上架 | 上架审核、自动分配库位 |
| 3 | 已上架 | 审核通过，已增加库存 | 反审核(5分钟内) |
| 4 | 已作废 | 单据作废 | —（终态） |

### 4.3 详细操作步骤

| 步骤 | 操作人 | 操作 | 系统动作 | 数据影响 |
|------|-------|------|---------|---------|
| 1 | 仓管员 | 方式一：输入采购单号→**从源单拉取**<br>方式二：手动新建 | 方式一：自动带出采购未到货明细 | 写入入库单草稿 |
| 2 | 仓管员 | 调整实到数量，录入批次/生产日期等 | 保存草稿/修改 | 状态=0或1 |
| 3 | 仓管员 | 提交 | 状态变更 | 0→1 |
| 4 | 仓管员 | 确认收货→状态变为待上架 | 状态变更 | 1→2 |
| 5 | 仓管员 | 自动分配库位 | 按策略填充库位信息 | 明细locationId更新 |
| 6 | 仓管员 | **上架审核** | ① 状态变更(乐观锁)<br>② 调用库存增加Handler<br>③ 若来源采购单则回写到货量 | 状态2→3<br>**库存↑**<br>采购单deliveredQty↑ |

代码参考：
- 从采购单拉取：[WmsStockInServiceImpl.java#L83-L131](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/stockin/service/impl/WmsStockInServiceImpl.java#L83-L131)
- 上架审核+库存增加：[WmsStockInServiceImpl.java#L217-L259](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/stockin/service/impl/WmsStockInServiceImpl.java#L217-L259)

### 4.4 数据流转图（入库→库存→采购回写）

```
┌────────────┐   采购单号拉取    ┌──────────────┐
│ 采购单(审核)│ ◀──────────── │ 入库单(草稿)   │
│ status=2   │                │ status=0       │
└─────┬──────┘                └──────┬─────────┘
      │                               │ 上架审核
      │ 回写deliveredQty              ▼
      │                        ┌──────────────┐
      │                        │ 入库单(已上架)│
      │                        │ status=3      │
      │                        └──────┬─────────┘
      │                               │ InventoryChangeHandler
      │                               │ STOCK_IN(2)
      ▼                               ▼
┌──────────────┐   insert/update   ┌───────────────┐
│ 采购明细     │ ◀────────────── │ WmsInventory   │
│ deliveredQty↑│                   │ quantity↑      │
│ unreceivedQty↓│                  │ availableQty↑  │
└──────────────┘                   │ totalAmount↑   │
     │                             └───────┬───────┘
     │ 全部到货=0时                         │ 记录流水
     ▼                                     ▼
┌──────────────┐                   ┌───────────────┐
│ 采购单(完成) │                   │ WmsInventoryLog│
│ status=4     │                   │ direction=1(入)│
└──────────────┘                   │ qtyChange=+N   │
                                   └───────────────┘
```

### 4.5 ⚠️ 限制条件

- **反审核**：仅已上架状态，且审核时间在 **5分钟内** 可反审核（防误操作保护）
- **作废**：仅未上架的入库单可作废（上架后库存已变动，不能作废）
- **源单回写**：类型=1（采购入库）且sourceBillNo存在时，才会回写采购单

---

## 5. 出库业务流程

### 5.1 流程概述

出库流程管理商品离开仓库的全过程，支持FIFO（先进先出）/FEFO（先到期先出）等分配策略。

### 5.2 出库单状态机

```
         保存           提交          锁定库存         拣货确认         出库审核
[草稿 0] ────→ [已提交 1] ────→ [已锁定 2] ────→ [已拣货 3] ────→ [已审核 4]
   │              │              │                │                │
   │删除          │预览分配       │                │反审核          │反审核
   ↓              ↓              ↓                ↓                ↓
 [删除]      查看分配建议    [已锁定 2]       [已拣货 3]       [已拣货 3]
                  │
                  │作废(仅审核前可作废)
                  ▼
             [已作废 5]
```

状态说明：
| 状态码 | 状态名 | 说明 | 可执行操作 |
|-------|-------|------|-----------|
| 0 | 草稿 | 新建未提交 | 修改、删除、提交 |
| 1 | 已提交 | 待分配库存 | 预览分配、锁定库存、作废 |
| 2 | 已锁定 | 库存已锁定（预留） | 拣货确认 |
| 3 | 已拣货 | 仓库已拣货完成 | 出库审核、反审核 |
| 4 | 已审核 | 出库完成，已扣减库存 | 反审核 |
| 5 | 已作废 | 单据作废 | —（终态） |

代码参考：[WmsStockOutController.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/stockout/controller/WmsStockOutController.java)

### 5.3 详细操作步骤

| 步骤 | 操作人 | 操作 | 系统动作 | 数据影响 |
|------|-------|------|---------|---------|
| 1 | 出库员 | 新建出库单，选择出库类型+商品+数量 | 保存草稿 | 状态=0 |
| 2 | 出库员 | 提交 | 校验明细非空 | 0→1 |
| 3 | 出库员 | **分配预览**（可选） | 按FIFO/FEFO规则返回建议批次/库位 | 仅查看，不锁库存 |
| 4 | 出库主管 | **锁定库存** | 乐观锁更新状态 | 1→2，**lockedQty↑**（预扣） |
| 5 | 拣货员 | **拣货确认** | 按拣货单拣货后确认 | 2→3 |
| 6 | 出库主管 | **出库审核** | ① 状态变更<br>② 调用出库确认Handler | 3→4，**quantity↓ lockedQty↓**<br>实际扣减库存 |

代码参考：
- 锁定库存：[WmsStockOutServiceImpl.java#L178-L198](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/stockout/service/impl/WmsStockOutServiceImpl.java#L178-L198)
- 拣货确认：[WmsStockOutServiceImpl.java#L201-L221](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/stockout/service/impl/WmsStockOutServiceImpl.java#L201-L221)
- 出库审核扣库存：[WmsStockOutServiceImpl.java#L224-L264](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/stockout/service/impl/WmsStockOutServiceImpl.java#L224-L264)

### 5.4 ⚠️ 限制条件

- **作废**：仅在已审核(status=4)之前可作废；已审核的出库单库存已扣减，不允许作废
- **分配策略**：`allocationRule` 1=FIFO先进先出, 2=FEFO先到期先出

---

## 6. 调拨业务流程

### 6.1 流程概述

调拨用于商品在**不同仓库之间**的转移，涉及两个库存变更动作：调出仓库扣减 + 调入仓库增加。

### 6.2 调拨单状态机

```
         保存           提交            审核           确认出库         确认入库
[草稿 0] ────→ [已提交 1] ────→ [已审核 2] ────→ [已出库 3] ────→ [已入库 4]
   │              │                │                │
   │删除          │                │反审核          │
   ↓              ↓                ↓                ↓
 [删除]       [已提交 1]       [已审核 2]       [已出库 3]
                  │
                  │作废
                  ▼
             [已作废 5]
```

状态说明：
| 状态码 | 状态名 | 说明 | 可执行操作 |
|-------|-------|------|-----------|
| 0 | 草稿 | 新建 | 修改、删除、提交 |
| 1 | 已提交 | 待审核 | 修改、审核、作废 |
| 2 | 已审核 | 审批通过，待调出仓库发货 | 反审核、确认出库 |
| 3 | 已出库 | 调出仓库已扣减库存，待调入仓库收货 | 确认入库 |
| 4 | 已入库 | 调入仓库已增加库存，调拨完成 | —（终态） |
| 5 | 已作废 | 作废 | —（终态） |

代码参考：[WmsTransferOrderController.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/transfer/controller/WmsTransferOrderController.java)

### 6.3 详细操作步骤（核心：两次库存变更）

| 步骤 | 操作人 | 操作 | 系统动作 | 数据影响 |
|------|-------|------|---------|---------|
| 1 | 调度员 | 新建调拨单：选择 调出仓库/调入仓库/商品 | 保存草稿 | 状态=0 |
| 2 | 调度员 | 提交审核 | 校验明细 | 0→1 |
| 3 | 调度主管 | 审核通过 | 乐观锁更新 | 1→2 |
| 4 | 调出仓管员 | **确认出库** | 调用 `TRANSFER_OUT` Handler | **调出仓库：quantity↓**<br>状态2→3 |
| 5 | 调入仓管员 | **确认入库** | 调用 `TRANSFER_IN` Handler | **调入仓库：quantity↑**<br>状态3→4 |

关键代码参考：[ChangeType.java#L9-L10](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/inventory/handler/ChangeType.java#L9-L10)

### 6.4 调拨数据流转图

```
   调出仓库 (Warehouse A)              调入仓库 (Warehouse B)
┌─────────────────────────┐        ┌─────────────────────────┐
│                         │        │                         │
│  WmsInventory           │        │  WmsInventory           │
│  ├─ warehouseId = A_id  │        │  ├─ warehouseId = B_id  │
│  ├─ quantity: 100 → 70  │        │  ├─ quantity: 0 → 30    │
│  └─ ...                 │        │  └─ ...                 │
│                         │        │                         │
│  InventoryLog           │        │  InventoryLog           │
│  ├─ billType=5(调出)    │        │  ├─ billType=6(调入)    │
│  ├─ direction=-1        │        │  ├─ direction=+1        │
│  └─ qtyChange=-30       │        │  └─ qtyChange=+30       │
└─────────────────────────┘        └─────────────────────────┘
          ▲                                   ▲
          │ TRANSFER_OUT                      │ TRANSFER_IN
          │ (确认出库时)                       │ (确认入库时)
   ┌──────┴───────────────────────────────────┴──────┐
   │              调拨单 (TransferOrder)              │
   │  status: 2(已审核) → 3(已出库) → 4(已入库)       │
   └─────────────────────────────────────────────────┘
```

---

## 7. 销售业务流程

### 7.1 流程概述

销售流程管理从客户下单到出库发货再到收款确认的全流程，覆盖**订单、出库、财务**三个环节。

### 7.2 销售单状态机

```
         保存           提交            审核           确认出库        确认收款        完成订单
[草稿 0] ────→ [已提交 1] ────→ [已审核 2] ────→ [已出库 3] ────→ [已收款 4] ────→ [已完成 5]
   │              │                │
   │删除          │                │作废
   ↓              ↓                ↓
 [删除]       [已提交 1]       [已作废 6]
```

状态说明：
| 状态码 | 状态名 | 说明 | 可执行操作 |
|-------|-------|------|-----------|
| 0 | 草稿 | 新建 | 修改、删除、提交 |
| 1 | 已提交 | 待审核 | 修改、审核、作废 |
| 2 | 已审核 | 审批通过，待发货 | 确认出库、作废 |
| 3 | 已出库 | 商品已发出，待收款 | 确认收款 |
| 4 | 已收款 | 财务已确认到账 | 完成订单 |
| 5 | 已完成 | 订单闭环 | —（终态） |
| 6 | 已作废 | 作废 | —（终态） |

代码参考：[WmsSaleOrderController.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/sale/controller/WmsSaleOrderController.java)

### 7.3 详细操作步骤（三部门协作）

| 步骤 | 部门/角色 | 操作 | 系统动作 | 数据影响 |
|------|----------|------|---------|---------|
| 1 | 销售部/业务员 | 新建销售单：客户+商品+单价+数量 | 保存草稿 | 状态=0 |
| 2 | 销售部 | 提交审核 | 校验明细 | 0→1 |
| 3 | 销售主管 | 审核通过 | 乐观锁更新 | 1→2 |
| 4 | 仓库部 | **确认出库** | ① 创建出库单/或关联出库<br>② 扣减库存 | 库存↓<br>状态2→3 |
| 5 | 财务部 | **确认收款** | 录入实收金额 | receivedAmount↑<br>状态3→4 |
| 6 | 销售部/系统 | **完成订单** | 关闭订单 | 状态4→5 |

### 7.4 销售利润分析

系统提供利润分析接口：`/api/wms/sale/profit-analysis`
- 收入：`totalSale`（销售总金额）
- 成本：`totalCost`（出库成本价 × 数量）
- 毛利 = 收入 - 成本

---

## 8. 报损业务流程

### 8.1 流程概述

报损用于处理商品损坏、过期、丢失等需要**主动扣减库存**的场景，需经审核后才执行库存扣减。

### 8.2 报损单状态机

```
         保存           提交            审核            处理(扣库存)
[草稿 0] ────→ [已提交 1] ────→ [已审核 2] ──────────→ [已处理 3]
   │              │                │
   │删除          │                │作废
   ↓              ↓                ↓
 [删除]       [已提交 1]       [已作废 4]
```

状态说明：
| 状态码 | 状态名 | 说明 | 可执行操作 |
|-------|-------|------|-----------|
| 0 | 草稿 | 新建 | 修改、删除、提交 |
| 1 | 已提交 | 待审核 | 修改、审核、作废 |
| 2 | 已审核 | 审批通过，待执行扣减 | 处理(扣库存)、作废 |
| 3 | 已处理 | 已扣减库存 | —（终态） |
| 4 | 已作废 | 作废 | —（终态） |

代码参考：[WmsLossOrderController.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/loss/controller/WmsLossOrderController.java)

### 8.3 详细操作步骤

| 步骤 | 操作人 | 操作 | 系统动作 | 数据影响 |
|------|-------|------|---------|---------|
| 1 | 仓管员 | 新建报损单：选择报损商品+数量+报损原因 | 保存草稿 | 状态=0 |
| 2 | 仓管员 | 提交审核 | 校验明细 | 0→1 |
| 3 | 仓库主管 | 审核通过 | 确认报损合理性 | 1→2 |
| 4 | 仓管员/系统 | **处理（扣减库存）** | 调用 `LOSS` Handler | **quantity↓**<br>状态2→3<br>生成库存流水 |

库存变更类型：`ChangeType.LOSS(7, "损耗")`，参考 [ChangeType.java#L11](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/inventory/handler/ChangeType.java#L11)

---

## 9. 盘点业务流程

### 9.1 流程概述

盘点用于核对**系统库存**与**实际库存**的差异，经审核后可将系统库存调整为实际值。

### 9.2 盘点单状态机

```
        保存/加载库存      开始盘点       录入实盘数据      完成盘点        审核         处理(调库存)
[草稿 0] ──────────→ [盘点中 1] ────────→ [盘点中 1] ──────→ [已完成 2] ────→ [已审核 3] ────→ [已处理 4]
   │                    │                                       │
   │删除                │                                       │作废
   ↓                    ↓                                       ↓
 [删除]            [盘点中 1]                               [已作废 5]
```

状态说明：
| 状态码 | 状态名 | 说明 | 可执行操作 |
|-------|-------|------|-----------|
| 0 | 草稿 | 新建，未加载库存 | 加载库存、开始盘点 |
| 1 | 盘点中 | 已加载系统库存，待录入实盘 | 录入实盘、完成盘点 |
| 2 | 盘点完成 | 实盘录入完毕，待审核 | 审核、作废 |
| 3 | 已审核 | 审核通过，待调整库存 | 处理(调库存) |
| 4 | 已处理 | 库存已按实盘调整 | —（终态） |
| 5 | 已作废 | 作废 | —（终态） |

代码参考：[WmsCheckOrderController.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/check/controller/WmsCheckOrderController.java)

### 9.3 详细操作步骤

| 步骤 | 操作人 | 操作 | 系统动作 | 数据影响 |
|------|-------|------|---------|---------|
| 1 | 仓管员 | 新建盘点单，可选指定仓库/库区 | 保存草稿 | 状态=0 |
| 2 | 仓管员 | **加载库存数据** | 查询库存表，自动生成盘点明细行（系统数量=systemQty） | 明细生成，可开始盘点 |
| 3 | 仓管员 | **开始盘点** | 状态变更，锁定盘点范围 | 0→1 |
| 4 | 仓管员 | **逐条录入实盘数据** | 填写 actualQty，系统自动计算 diffQty=actual-system | 每行差异实时计算 |
| 5 | 仓管员 | **完成盘点** | 汇总盘盈盘亏 | 1→2 |
| 6 | 仓库主管 | **审核** | 确认差异处理方式 | 2→3 |
| 7 | 系统/仓管员 | **处理（调整库存）** | 调用 `CHECK` Handler，盈则加，亏则减 | **按差异调整库存**<br>状态3→4 |

库存变更类型：`ChangeType.CHECK(8, "盘点")`

### 9.4 盘点差异处理逻辑

| 场景 | systemQty | actualQty | diffQty | 库存调整动作 |
|------|-----------|-----------|---------|------------|
| 盘盈 | 100 | 105 | +5 | quantity +5 |
| 盘亏 | 100 | 95 | -5 | quantity -5 |
| 无差异 | 100 | 100 | 0 | 不调整 |

---

## 10. 库存管理

### 10.1 库存核心数据结构

库存表 `wms_inventory`（参考 [WmsInventory.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/inventory/entity/WmsInventory.java)）：

| 字段 | 含义 | 说明 |
|------|------|------|
| `warehouseId` | 仓库ID | 第一维度 |
| `skuId` | 商品SKU ID | 第二维度 |
| `locationId` | 库位ID | 第三维度 |
| `batchNo` | 批次号 | 第四维度（同SKU不同批次分别存储） |
| `quantity` | 总数量 | 实际在库数量 |
| `lockedQty` | 锁定数量 | 已被出库单锁定预留的数量 |
| `availableQty` | 可用数量 | = quantity - lockedQty（前端展示关键） |
| `costPrice` | 成本价 | 用于核算 |
| `totalAmount` | 库存总值 | = quantity × costPrice |

### 10.2 库存变更类型总览

所有库存变动都通过 `InventoryChangeHandler` 处理，并写入 `wms_inventory_log` 流水表。

| 枚举值 | Code | 含义 | 库存方向 | 触发时机 |
|--------|------|------|---------|---------|
| PURCHASE_RETURN | 1 | 采购退货 | - | 采购退货审核 |
| **STOCK_IN** | **2** | **入库** | **+** | **入库单上架审核** |
| STOCK_OUT_LOCK | 3 | 出库锁定 | lockedQty+ | 出库单锁定库存 |
| **STOCK_OUT_CONFIRM** | **4** | **出库确认** | **- 且 lockedQty-** | **出库单审核** |
| **TRANSFER_OUT** | **5** | **调拨出库** | **-** | **调拨单确认出库** |
| **TRANSFER_IN** | **6** | **调拨入库** | **+** | **调拨单确认入库** |
| **LOSS** | **7** | **损耗** | **-** | **报损单处理** |
| **CHECK** | **8** | **盘点调整** | **+或-** | **盘点单处理** |

代码参考：[ChangeType.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/inventory/handler/ChangeType.java)

### 10.3 库存流水（wms_inventory_log）

**每一次**库存变动都会写一条流水记录，供审计追溯：

| 字段 | 含义 |
|------|------|
| `billId` / `billNo` | 关联的业务单据ID/单号（可追溯是哪张采购单/入库单导致的变动） |
| `billType` | 对应 ChangeType.code（1~8） |
| `direction` | 方向：+1=入库，-1=出库 |
| `qtyChange` | 变更数量（绝对值） |
| `beforeQty` → `afterQty` | 变更前数量 → 变更后数量（变更快照） |
| `unitPrice` / `amountChange` | 单价 / 金额变动 |
| `operateBy` / `operateTime` | 操作人 / 时间 |

代码参考：[WmsInventoryLog.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/inventory/entity/WmsInventoryLog.java)

### 10.4 单据与库存关系总图

```
                        ┌───────────────────┐
                        │  WmsInventory     │
                        │  (实时库存表)      │
                        │  quantity/locked  │
                        └─────────▲─────────┘
                                  │
                          InventoryChangeHandler
                    (8种ChangeType，统一入口)
                                  │
     ┌──────────┬──────────┬──────┴──────┬──────────┬──────────┐
     │ STOCK_IN │STOCK_OUT │ TRANSFER    │ LOSS     │ CHECK    │
     │ (入库)   │ (出库)   │ (调出/调入) │ (损耗)   │ (盘点)   │
     └─────▲────┴────▲────┴──────▲──────┴────▲─────┴────▲─────┘
           │         │           │            │          │
     ┌─────┴────┐ ┌──┴─────┐ ┌──┴──────┐ ┌───┴────┐ ┌──┴─────┐
     │ 入库单   │ │出库单  │ │调拨单    │ │报损单  │ │盘点单  │
     │ status=3 │ │status=4│ │status=3/4│ │status=3│ │status=4│
     └─────▲────┘ └────────┘ └──────────┘ └────────┘ └────────┘
           │
     ┌─────┴────┐
     │ 采购单   │ ←── 回写到货量
     │ status=2→4│
     └──────────┘
```

---

## 11. 系统权限与角色

### 11.1 权限注解机制

所有写操作接口都有双重保护：
1. **`@PreAuthorize(hasAuthority = "...")`**：Spring切面校验权限标识
2. **`@OperationLog`**：自动记录操作日志到 `sys_oper_log`

代码参考：
- 权限切面：[PreAuthorizeAspect.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/auth/PreAuthorizeAspect.java)
- 日志切面：[OperationLogAspect.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/auth/OperationLogAspect.java)

### 11.2 推荐角色权限矩阵

| 模块 \ 角色 | 系统管理员 | 采购主管 | 采购员 | 仓库主管 | 仓管员 | 销售主管 | 销售员 | 财务 |
|-----------|----------|---------|-------|---------|-------|---------|-------|-----|
| 基础数据 | ✅ 全部 | 👁️ | 👁️ | ✅ 全部 | 👁️ | 👁️ | 👁️ | 👁️ |
| 采购单 | ✅ | ✅ 审核/反审核/作废 | ✅ 增删改提交 | 👁️ | 👁️ | 👁️ | 👁️ | 👁️ |
| 入库单 | ✅ | 👁️ | 👁️ | ✅ 审核/作废 | ✅ 增删改提交上架 | 👁️ | 👁️ | 👁️ |
| 出库单 | ✅ | 👁️ | 👁️ | ✅ 审核/锁定/作废 | ✅ 增删改拣货 | 👁️ | ✅ 新建 | 👁️ |
| 调拨单 | ✅ | 👁️ | 👁️ | ✅ 审核/出入库 | ✅ 增删改提交 | 👁️ | 👁️ | 👁️ |
| 销售单 | ✅ | 👁️ | 👁️ | ✅ 确认出库 | 👁️ | ✅ 审核/作废/完成 | ✅ 增删改提交 | ✅ 确认收款 |
| 报损单 | ✅ | 👁️ | 👁️ | ✅ 审核/处理/作废 | ✅ 增删改提交 | 👁️ | 👁️ | 👁️ |
| 盘点单 | ✅ | 👁️ | 👁️ | ✅ 审核/处理/作废 | ✅ 新建/录入实盘 | 👁️ | 👁️ | 👁️ |
| 库存查询 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 库存流水 | ✅ | ✅ | 👁️ | ✅ | 👁️ | 👁️ | 👁️ | ✅ |
| 用户/角色/菜单 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

✅=可操作 👁️=仅查看 ❌=无权限

---

## 12. 各部门培训要点

### 12.1 采购部培训

**目标**：掌握"采购单 → 跟进到货"全流程

**培训内容**：
1. 采购单创建：
   - 单号规则自定义，必须唯一
   - 选择供应商、入仓仓库、采购员
   - 明细行：选SKU、录数量、单价（含税/不含税注意税率）
2. 提交与跟进：
   - 提交后通知主管审核
   - 关注 `unreceivedQty` 未到货数量
   - 供应商到货后通知仓库"按采购单号入库"
3. 异常处理：
   - 填错了：草稿→直接修改；已提交→让主管反审核
   - 想取消：无到货→作废；已有到货→不可作废

**演练题目**：
> 新建一张采购单向供应商A采购100件商品X，入仓一号仓库，单价10元。提交→主管审核→模拟仓库按该采购单号入库30件→查看采购单deliveredQty和unreceivedQty的变化。

---

### 12.2 仓库部培训（核心部门，需重点培训）

**目标**：掌握入库、出库、调拨、盘点、报损 5 大操作

**培训内容**：

#### 🔹 入库操作（必考）
```
[方式一：采购入库 - 推荐]
1. 点击"新增" → 输入/选择 采购单号
2. 系统自动带出未到货明细 → 确认实到数量
   ⚠️ 实际到了多少就填多少，可少于采购量（分批入库）
3. 录入 批次号/生产日期/过期日期（食品/医药行业必填）
4. 保存 → 提交 → 确认收货（待上架状态）
5. 点击"自动分配库位" → 检查库位是否正确
6. 点击"上架审核" → ✅ 完成！库存已增加

[方式二：手工入库]
1. 手动选择入库类型、仓库、供应商
2. 手动逐行添加商品明细
3. 后续步骤同上（提交→收货→分配库位→上架）
```

#### 🔹 出库操作（重点：锁定→拣货→审核三步骤）
```
1. 新增出库单 → 选出库类型、仓库、客户
2. 添加商品明细 → 保存 → 提交
3. [主管] 点击"预览分配" → 确认系统按FIFO推荐的批次
4. [主管] 点击"锁定库存" → 此时仓库总量不变，但可用量减少
5. [拣货员] 按系统推荐库位去拣货 → 拣完点"拣货确认"
6. [主管] 点击"出库审核" → ✅ 完成！库存已扣减
```

#### 🔹 调拨操作（两个仓库配合）
```
调度员：新建调拨单 → 选A仓(调出)和B仓(调入) → 提交 → 审核
A仓仓管员：确认出库 → A仓库存↓
B仓仓管员：确认入库 → B仓库存↑
```

#### 🔹 盘点操作（月末/季末必做）
```
1. 新建盘点单 → 可选"只盘某仓库/某库区"
2. 关键操作：点击"加载库存数据" → 系统自动拉出当前库存作为系统数量
3. 点击"开始盘点" → 进入实盘录入模式
4. 拿着PDA/打印单去仓库现场点数 → 回来录入"实际数量"
5. 完成盘点 → 系统自动计算差异（盘盈/盘亏）
6. 主管审核差异 → 点击"处理" → ✅ 库存自动对齐到实际值
```

#### 🔹 报损操作
```
1. 新建报损单 → 选报损商品+数量+原因
2. 提交 → 主管审核
3. 点击"处理" → ✅ 库存扣减完毕
```

**常见错误**：
- ❌ 入库上架后发现填错了 → 5分钟内可反审核，超过就不能改了（只能做调拨或报损调整）
- ❌ 出库单忘了点"锁定库存"就去拣货 → 可能被其他单子抢走库存
- ❌ 盘点时忘了点"加载库存"就填数 → 系统数量为空，差异计算错误

---

### 12.3 销售部培训

**目标**：掌握"销售下单→跟进发货"流程

**培训内容**：
1. 销售单创建：选客户、选商品+数量、填销售单价
2. 提交 → 等待主管审核
3. 审核通过后：
   - 通知仓库"按销售单号出库"（或系统自动生成出库单）
   - 关注状态：`已出库`=货已发，`已收款`=钱已到
4. 收款完成后，点击"完成订单"

---

### 12.4 财务部培训

**目标**：掌握收款确认、库存价值核算、流水审计

**培训内容**：
1. 每日/每周操作：
   - 销售单→筛选"已出库未收款"→跟催→确认收款
2. 月末操作：
   - 查看库存报表：库存总价值 = Σ(quantity × costPrice)
   - 导出库存流水：按时间段导出所有出入库明细做凭证
   - 利润分析：`/profit-analysis` 查看区间毛利
3. 审计追溯：
   - 任何库存变动 → wms_inventory_log 都有记录
   - 通过 billNo + billType 可反查到业务单据

---

### 12.5 系统管理员培训

**目标**：基础数据维护 + 用户权限 + 故障排查

**培训内容**：
1. 基础数据初始化顺序（**重要！顺序错了业务跑不起来**）：
```
① 计量单位 (Unit)
② 仓库 → 库区 → 库位 (Warehouse→Area→Location)
③ 商品分类 → 品牌 → SPU → SKU (Category→Brand→Spu→Sku)
④ 供应商 (Supplier) / 客户 (Customer)
```
2. 用户权限管理：
   - 新建角色 → 按"第11章权限矩阵"勾选权限标识
   - 新建用户 → 分配角色
3. 常见故障排查：
   - **问题**：入库时提示"库存不足"？→ 检查是不是选了"出库类型"搞反了
   - **问题**：提交后找不到"审核"按钮？→ 账号没分配 `*:audit` 权限
   - **问题**：反审核提示"超过5分钟"？→ 走盘点流程做调整
   - **问题**：库存对不上？→ 查 `wms_inventory_log` 看哪笔单据变动异常

---

## 附录A：所有单据状态速查表

| 单据 | 0 | 1 | 2 | 3 | 4 | 5 | 6 |
|------|---|---|---|---|---|---|---|
| **采购单** | 草稿 | 已提交 | 已审核 | - | 已完成 | 已作废 | - |
| **入库单** | 草稿 | 已提交 | 待上架 | 已上架 | 已作废 | - | - |
| **出库单** | 草稿 | 已提交 | 已锁定 | 已拣货 | 已审核 | 已作废 | - |
| **调拨单** | 草稿 | 已提交 | 已审核 | 已出库 | 已入库 | 已作废 | - |
| **销售单** | 草稿 | 已提交 | 已审核 | 已出库 | 已收款 | 已完成 | 已作废 |
| **报损单** | 草稿 | 已提交 | 已审核 | 已处理 | 已作废 | - | - |
| **盘点单** | 草稿 | 盘点中 | 盘点完成 | 已审核 | 已处理 | 已作废 | - |

---

## 附录B：培训验收自测题

### 判断题（每题10分）
1. ( ) 采购单已审核后，只要还没到货，就可以反审核。
2. ( ) 入库单上架审核10分钟后发现填错了，可以点击反审核修改。
3. ( ) 出库单点击"锁定库存"后，仓库的总数量(quantity)会减少。
4. ( ) 调拨单只需审核一次，审核通过后两个仓库的库存会同时自动变动。
5. ( ) 报损单审核通过后会立刻扣减库存。
6. ( ) 盘点单必须先点"加载库存数据"，否则系统数量为空。
7. ( ) 已审核的出库单可以直接作废。
8. ( ) 创建调拨单时，调出仓库和调入仓库可以是同一个仓库。
9. ( ) wms_inventory_log 中，direction=1 表示入库。
10. ( ) 采购入库时，入库明细可以少于采购单上的未到货数量（分批入库）。

### 答案：
1. ✓ 2. ✗（仅5分钟内）3. ✗（是lockedQty增加，quantity不变，审核时才真正扣）4. ✗（调出仓和调入仓需分别"确认出库/确认入库"）5. ✗（审核后还需点"处理"才扣减）6. ✓ 7. ✗（已审核库存已扣，不可作废）8. ✗（逻辑上无意义，系统一般做校验）9. ✓ 10. ✓

---

> 📌 **手册维护说明**：本手册基于代码逻辑自动梳理生成，核心流程参考：
> - 采购Service：[WmsPurchaseOrderServiceImpl.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/purchase/service/impl/WmsPurchaseOrderServiceImpl.java)
> - 入库Service：[WmsStockInServiceImpl.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/stockin/service/impl/WmsStockInServiceImpl.java)
> - 出库Service：[WmsStockOutServiceImpl.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/stockout/service/impl/WmsStockOutServiceImpl.java)
> - 调拨Service：[WmsTransferOrderServiceImpl.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/transfer/service/impl/WmsTransferOrderServiceImpl.java)
> - 库存Handler：[InventoryChangeHandler.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/inventory/handler/InventoryChangeHandler.java)
