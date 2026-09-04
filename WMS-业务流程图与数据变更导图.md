# WMS 系统 - 业务流程图 & 数据变更导图

> 版本：v1.0 | 更新日期：2026-08-26
> 
> 说明：本文件使用 Mermaid 语法绘制流程图，支持在 Typora、VSCode(Mermaid插件)、GitHub、Notion 等环境直接渲染。

---

## 目录

1. [WMS 端到端全业务流程图](#1-wms-端到端全业务流程图)
2. [采购→入库 协同流程图](#2-采购入库-协同流程图)
3. [销售→出库 协同流程图](#3-销售出库-协同流程图)
4. [仓库调拨 双仓协同流程图](#4-仓库调拨-双仓协同流程图)
5. [盘点业务 详细流程图](#5-盘点业务-详细流程图)
6. [报损业务 流程图](#6-报损业务-流程图)
7. [全单据状态机 汇总图](#7-全单据状态机-汇总图)
8. [库存数据变更 核心导图（重中之重）](#8-库存数据变更-核心导图重中之重)
9. [库存流水追溯 关系图](#9-库存流水追溯-关系图)
10. [各部门职责与协作 泳道图](#10-各部门职责与协作-泳道图)

---

## 1. WMS 端到端全业务流程图

```mermaid
flowchart TD
    %% ===== 初始化阶段 =====
    A[基础数据初始化]:::init --> A1[仓库→库区→库位]
    A --> A2[分类→品牌→SPU→SKU]
    A --> A3[供应商/客户/单位]

    %% ===== 采购入库线 =====
    A --> B[采购部: 新建采购单]:::purchase
    B --> B1[保存草稿 status=0]
    B1 --> B2[提交审核 status=1]
    B2 --> B3{采购主管审核?}
    B3 -->|通过| B4[已审核 status=2]:::pass
    B3 -->|驳回| B1
    B4 --> C[仓库: 按采购单号拉取入库单]:::warehouse
    C --> C1[确认实到数量+批次]
    C1 --> C2[提交+收货 待上架 status=2]
    C2 --> C3[分配库位]
    C3 --> C4[上架审核 status=3]:::critical
    C4 -->|自动| C5[✔️ 库存增加 +\n采购单到货量回写]
    C5 --> C6{全部到货?}
    C6 -->|是| C7[采购单自动完成 status=4]:::done
    C6 -->|否| B4

    %% ===== 销售出库线 =====
    A --> D[销售部: 新建销售单]:::sale
    D --> D1[保存草稿 status=0]
    D1 --> D2[提交审核 status=1]
    D2 --> D3{销售主管审核?}
    D3 -->|通过| D4[已审核 status=2]:::pass
    D3 -->|驳回| D1
    D4 --> E[仓库: 执行出库]:::warehouse
    E --> E1[预览分配 FIFO/FEFO]
    E1 --> E2[锁定库存 status=2\n预留可用量↓]
    E2 --> E3[拣货确认 status=3]
    E3 --> E4[出库审核 status=4]:::critical
    E4 -->|自动| E5[✔️ 库存扣减 -\n销售单状态更新]
    E5 --> F[财务部: 确认收款]:::finance
    F --> F1[已收款 status=4]
    F1 --> F2[完成订单 status=5]:::done

    %% ===== 内部调拨线 =====
    A --> G[调度员: 新建调拨单]:::warehouse
    G --> G1[草稿→提交→审核 status=2]:::pass
    G1 --> H[调出仓: 确认出库]:::warehouse
    H --> H1[✔️ A仓库存扣减\nstatus=3]
    H1 --> I[调入仓: 确认入库]:::warehouse
    I --> I1[✔️ B仓库存增加\nstatus=4]:::done

    %% ===== 异常处理线 =====
    J[发现库存异常]:::exception --> K{差异类型?}
    K -->|商品损坏/过期| L[报损流程]:::exception
    K -->|账实不符| M[盘点流程]:::exception
    L --> L1[审核→处理 扣减库存]:::critical
    M --> M1[加载库存→录入实盘→审核→处理\n差异调整库存]:::critical

    %% ===== 样式 =====
    classDef init fill:#e1f5fe,stroke:#01579b,color:#000
    classDef purchase fill:#e8f5e9,stroke:#2e7d32,color:#000
    classDef warehouse fill:#fff3e0,stroke:#ef6c00,color:#000
    classDef sale fill:#fce4ec,stroke:#c2185b,color:#000
    classDef finance fill:#f3e5f5,stroke:#7b1fa2,color:#000
    classDef exception fill:#ffebee,stroke:#c62828,color:#000
    classDef critical fill:#ff5252,stroke:#b71c1c,color:#fff,stroke-width:2px
    classDef pass fill:#81c784,stroke:#2e7d32,color:#000
    classDef done fill:#4fc3f7,stroke:#01579b,color:#000
```

---

## 2. 采购→入库 协同流程图

```mermaid
sequenceDiagram
    participant P as 🧑‍💼 采购员
    participant PM as 👔 采购主管
    participant W as 📦 仓管员
    participant DB as 💾 WMS系统/DB

    Note over P,DB: ===== 阶段一：采购单创建与审核 =====
    P->>DB: 1. 新增采购单(选供应商/仓库/商品明细)
    DB-->>P: 返回采购单草稿 status=0
    P->>DB: 2. 提交审核
    DB-->>PM: 待审核提醒
    PM->>DB: 3. 审核通过（乐观锁防并发）
    DB->>DB: 写入状态日志: 1→2\n记录 auditBy/auditTime
    DB-->>PM: 审核成功 status=2
    Note over P,DB: 此时商品还未到仓，库存不变 ✅

    Note over P,DB: ===== 阶段二：入库单创建与上架 =====
    W->>DB: 4. 输入采购单号 → 拉取源单数据
    DB->>DB: 查询采购单未到货明细\n计算 remaining = 订购-已到货
    DB-->>W: 自动带出未到货商品
    W->>DB: 5. 录入实到数量、批次、生产日期
    DB-->>W: 入库单草稿 status=0
    W->>DB: 6. 提交 + 确认收货
    DB-->>W: status=2(待上架)
    W->>DB: 7. 自动分配库位 / 手动指定
    W->>DB: 8. ✨ 上架审核 ✨
    DB->>DB: 【关键动作1】调用 STOCK_IN Handler\n    WmsInventory.quantity += N\n    WmsInventory.availableQty += N
    DB->>DB: 【关键动作2】回写采购单\n    purchaseOrderItem.deliveredQty += 实际入库量\n    purchaseOrderItem.unreceivedQty -= 实际入库量
    DB->>DB: 【关键动作3】写库存流水\n    billType=2(入库), direction=+1
    alt 全部到货 unreceivedQty=0
        DB->>DB: 采购单状态自动更新 2→4 已完成
    end
    DB-->>W: 上架成功 status=3
```

---

## 3. 销售→出库 协同流程图

```mermaid
sequenceDiagram
    participant S as 🧑‍💼 销售员
    participant SM as 👔 销售主管
    participant WM as 👔 仓库主管
    participant PK as 🚚 拣货员
    participant F as 💰 财务
    participant DB as 💾 WMS系统/DB

    Note over S,DB: ===== 阶段一：销售单 =====
    S->>DB: 1. 新建销售单(客户+商品+单价)
    DB-->>S: 草稿 status=0
    S->>DB: 2. 提交审核
    SM->>DB: 3. 审核通过
    DB->>DB: 销售单 status=2
    DB-->>SM: 审核成功

    Note over S,DB: ===== 阶段二：出库执行（核心4步） =====
    WM->>DB: 4. 预览分配策略(FIFO/FEFO)
    DB-->>WM: 推荐批次和库位（仅查看，不锁库存）
    WM->>DB: 5. 🔒 锁定库存 🔒
    Note right of DB: ⚠️ 重要：\nquantity 不变\nlockedQty += N\navailableQty -= N\n【防止多人抢同一批货】
    DB->>DB: 出库单 status=2
    DB-->>WM: 锁定成功

    PK->>DB: 6. 按拣货单去仓库拣货
    PK->>DB: 7. ✅ 拣货确认
    DB->>DB: 出库单 status=3

    WM->>DB: 8. ✨ 出库审核 ✨
    Note right of DB: 🗑️ 真正扣减：\nquantity -= N\nlockedQty -= N\ntotalAmount -= N×costPrice
    DB->>DB: 【关键】调用 STOCK_OUT_CONFIRM Handler\n写库存流水 direction=-1
    DB->>DB: 销售单 status=3(已出库)
    DB-->>WM: 出库完成

    Note over S,DB: ===== 阶段三：财务收款 =====
    F->>DB: 9. 确认收到客户货款
    DB->>DB: receivedAmount += 实收金额\n销售单 status=4(已收款)
    S->>DB: 10. 完成订单
    DB->>DB: 销售单 status=5(已完成) ✅
```

---

## 4. 仓库调拨 双仓协同流程图

```mermaid
flowchart LR
    subgraph 调度中心
        D[调度员: 新建调拨单]:::s --> D1[提交审核]
        D1 --> D2[主管审核 status=2]:::s
    end

    subgraph 🏭 A仓库 - 调出方
        direction TB
        D2 --> AO[A仓仓管员看到待发货]:::a
        AO --> A1[核对商品]
        A1 --> A2[✅ 确认出库]:::critical
        A2 --> A3[调用 TRANSFER_OUT Handler]
        A3 --> A4[(A仓库存)]:::db
        A4 -. quantity↓ .-> A5[A仓库存扣减成功]
        A2 -->|status 2→3| TS[调拨单: 已出库]:::mid
    end

    subgraph 🏭 B仓库 - 调入方
        direction TB
        TS --> BO[B仓仓管员看到待收货]:::b
        BO --> B1[实物验收]
        B1 --> B2[✅ 确认入库]:::critical
        B2 --> B3[调用 TRANSFER_IN Handler]
        B3 --> B4[(B仓库存)]:::db
        B4 -. quantity↑ .-> B5[B仓库存增加成功]
        B2 -->|status 3→4| TD[调拨单: 已入库完成]:::done
    end

    classDef s fill:#f3e5f5,stroke:#7b1fa2
    classDef a fill:#ffebee,stroke:#c62828
    classDef b fill:#e8f5e9,stroke:#2e7d32
    classDef critical fill:#ff5252,stroke:#b71c1c,color:#fff,stroke-width:2px
    classDef db fill:#bbdefb,stroke:#0d47a1
    classDef mid fill:#fff9c4,stroke:#f57f17
    classDef done fill:#4fc3f7,stroke:#01579b,color:#fff
```

**关键设计点**：调拨不一步到位，必须两个仓库分别确认 → 防止"调出方没发货但调入方说收到了"的扯皮问题。

---

## 5. 盘点业务 详细流程图

```mermaid
flowchart TD
    Start([月末/季末盘点]):::start --> A[新建盘点单]
    A --> A1[可选: 指定仓库/库区范围]
    A1 --> B[⚠️ 关键步骤: 加载库存数据]:::warn
    B --> B1[系统查询 wms_inventory\n生成盘点明细行:\nsystemQty = 当前库存数]
    B1 --> C[点击 开始盘点]
    C -->|status 0→1| D[盘点中 status=1\n前往仓库现场点数]

    D --> E{逐SKU录入实际数量}
    E --> E1[inputActual 接口\n每行写 actualQty]
    E1 -->|自动计算| E2[diffQty = actualQty - systemQty]
    E2 --> F{全部SKU录完?}
    F -->|否| E
    F -->|是| G[点击 完成盘点 status 1→2]

    G --> H[盘点差异汇总]:::report
    H --> H1[盘盈: diff > 0 / 实际多了]
    H --> H2[盘亏: diff < 0 / 实际少了]
    H --> H3[无差异: diff = 0]

    H1 --> I{主管审核差异原因}
    H2 --> I
    H3 --> I
    I -->|合理/通过| J[审核通过 status 2→3]
    I -->|不合理,需重盘| D

    J --> K[✨ 处理: 调整库存 ✨]:::critical
    K -->|盘盈 +| K1[调用 CHECK Handler\nquantity += diff]
    K -->|盘亏 -| K2[调用 CHECK Handler\nquantity -= |diff|]
    K -->|无差异| K3[库存不变，仅关单]
    K1 & K2 & K3 --> L[写库存流水 billType=8]
    L --> M[status 3→4 盘点完成]:::done

    classDef start fill:#e1f5fe,stroke:#01579b
    classDef warn fill:#ff9800,stroke:#e65100,color:#fff,stroke-width:2px
    classDef critical fill:#ff5252,stroke:#b71c1c,color:#fff,stroke-width:2px
    classDef report fill:#fff9c4,stroke:#f57f17
    classDef done fill:#4fc3f7,stroke:#01579b,color:#fff
```

---

## 6. 报损业务 流程图

```mermaid
flowchart TD
    A([发现异常商品]):::start --> B{异常原因?}
    B -->|过期/损坏/丢失| C[新建报损单]:::warn
    C --> C1[选商品+报损数量+报损原因]
    C1 --> D[保存草稿 status=0]
    D --> E[提交审核 status=1]
    E --> F{主管审核}
    F -->|不合理 退回| D
    F -->|通过| G[审核通过 status=2]

    G --> H[✨ 处理(扣减库存) ✨]:::critical
    H --> H1[调用 LOSS Handler\nChangeType=7]
    H1 --> H2[WmsInventory.quantity -= 报损数量\navailableQty 同步扣减]
    H1 --> H3[写库存流水:\nbillType=7, direction=-1]
    H2 & H3 --> I[status 2→3 报损完成]:::done

    classDef start fill:#ffebee,stroke:#c62828
    classDef warn fill:#ff9800,stroke:#e65100,color:#fff
    classDef critical fill:#ff5252,stroke:#b71c1c,color:#fff,stroke-width:2px
    classDef done fill:#4fc3f7,stroke:#01579b,color:#fff
```

---

## 7. 全单据状态机 汇总图

```mermaid
stateDiagram-v2
    [*] --> 草稿0: 保存

    state "采购单 Purchase" as P {
        direction LR
        草稿0 --> 已提交1: 提交
        已提交1 --> 草稿0: 反审核
        已提交1 --> 已审核2: 审核
        已审核2 --> 已提交1: 反审核(无到货)
        已审核2 --> 已完成4: 全部到货入库(自动)
        已提交1 --> 已作废5: 作废
        已审核2 --> 已作废5: 作废(无到货)
    }

    state "入库单 StockIn" as SI {
        direction LR
        草稿0 --> 已提交1: 提交
        已提交1 --> 待上架2: 确认收货
        待上架2 --> 已上架3: 上架审核 ⭐
        已上架3 --> 待上架2: 反审核(5分钟内)
        待上架2 --> 已作废4: 作废
        已提交1 --> 已作废4: 作废
    }

    state "出库单 StockOut" as SO {
        direction LR
        草稿0 --> 已提交1: 提交
        已提交1 --> 已锁定2: 锁定库存 🔒
        已锁定2 --> 已拣货3: 拣货确认
        已拣货3 --> 已审核4: 出库审核 ⭐
        已审核4 --> 已拣货3: 反审核
        已提交1 --> 已作废5: 作废
    }

    state "调拨单 Transfer" as TF {
        direction LR
        草稿0 --> 已提交1: 提交
        已提交1 --> 已审核2: 审核
        已审核2 --> 已出库3: 调出仓确认出库 ⭐
        已出库3 --> 已入库4: 调入仓确认入库 ⭐
        已提交1 --> 已作废5: 作废
    }

    state "销售单 Sale" as SL {
        direction LR
        草稿0 --> 已提交1: 提交
        已提交1 --> 已审核2: 审核
        已审核2 --> 已出库3: 确认出库 ⭐
        已出库3 --> 已收款4: 财务确认收款
        已收款4 --> 已完成5: 完成订单
        已提交1 --> 已作废6: 作废
    }

    state "报损单 Loss" as LS {
        direction LR
        草稿0 --> 已提交1: 提交
        已提交1 --> 已审核2: 审核
        已审核2 --> 已处理3: 处理扣减 ⭐
        已提交1 --> 已作废4: 作废
    }

    state "盘点单 Check" as CK {
        direction LR
        草稿0 --> 盘点中1: 加载库存+开始盘点
        盘点中1 --> 盘点完成2: 完成盘点
        盘点完成2 --> 已审核3: 审核
        已审核3 --> 已处理4: 处理调库存 ⭐
        盘点完成2 --> 已作废5: 作废
    }

    草稿0 --> [*]: 删除(仅草稿可删)
```

⭐ = 此步骤触发库存变动

---

## 8. 库存数据变更 核心导图（重中之重）

> 所有业务最终都会落到这张图。培训时建议将此图打印出来贴在仓库办公室。

```mermaid
flowchart TB
    %% ===== 核心库存表 =====
    Inv[(📦 WmsInventory 实时库存表\n\nwarehouseId + skuId + locationId + batchNo → 唯一键\n────────────────\n📊 quantity       总数量\n🔒 lockedQty      锁定量\n✅ availableQty   可用量 (= Q - L)\n💰 costPrice      成本价\n💵 totalAmount    总值)]:::db

    %% ===== 8种变更入口 =====
    subgraph 所有业务单据审核/处理节点 ⭐会触发Handler
        direction TB
        SI[入库单上架审核\nstatus=2→3]:::in
        SOL[出库单锁定库存\nstatus=1→2]:::lock
        SO[出库单审核\nstatus=3→4]:::out
        TO[调拨确认出库\nstatus=2→3]:::out
        TI[调拨确认入库\nstatus=3→4]:::in
        L[报损单处理\nstatus=2→3]:::out
        C[盘点单处理\nstatus=3→4]:::diff
        PR[采购退货审核]:::out
    end

    %% ===== Factory + Handler 统一处理层 =====
    Fac[🏭 InventoryChangeHandlerFactory\n根据 ChangeType.code 路由]:::factory
    SI -->|ChangeType=2 STOCK_IN| Fac
    SOL -->|ChangeType=3 STOCK_OUT_LOCK| Fac
    SO -->|ChangeType=4 STOCK_OUT_CONFIRM| Fac
    TO -->|ChangeType=5 TRANSFER_OUT| Fac
    TI -->|ChangeType=6 TRANSFER_IN| Fac
    L -->|ChangeType=7 LOSS| Fac
    C -->|ChangeType=8 CHECK| Fac
    PR -->|ChangeType=1 PURCHASE_RETURN| Fac

    %% ===== Handler 执行计算 =====
    H[⚙️ InventoryChangeHandler\n执行库存计算 + 版本号乐观锁]:::handler
    Fac --> H

    H -->|① 计算变动前快照| S1(beforeQty 记录)
    H -->|② 按规则更新数量| Upd
    H -->|③ 计算变动后快照| S2(afterQty 记录)

    %% ===== 更新库存表 =====
    subgraph 更新逻辑 Upd
        direction LR
        U1[STOCK_IN/TRANSFER_IN:\nQ += N, A += N]
        U2[STOCK_OUT_LOCK:\nL += N, A -= N\n❗Q不变]
        U3[STOCK_OUT_CONFIRM:\nQ -= N, L -= N]
        U4[LOSS/TRANSFER_OUT/P_RETURN:\nQ -= N, A -= N]
        U5[盘点 CHECK:\ndiff>0? Q+diff : Q-|diff|]
    end
    Upd --> Inv

    %% ===== 写流水 =====
    S1 & S2 --> Log[(📝 WmsInventoryLog\n审计流水表 - 永远INSERT不UPDATE\n────────────────\n📋 billId/billNo 追溯业务单\n🔢 billType 1~8\n➡️ direction +1入/-1出\n📦 qtyChange 变动量\n🔜 beforeQty → afterQty\n👤 operateBy/operateTime\n💵 unitPrice/amountChange)]:::db
    Log -->|通过billNo+billType反查| Bill[任意业务单据\n(采购/入库/出库/调拨/销售/报损/盘点)]:::doc

    %% ===== 样式 =====
    classDef db fill:#1976d2,stroke:#0d47a1,color:#fff,stroke-width:2px
    classDef factory fill:#7b1fa2,stroke:#4a148c,color:#fff
    classDef handler fill:#e65100,stroke:#bf360c,color:#fff,stroke-width:2px
    classDef in fill:#4caf50,stroke:#1b5e20,color:#fff
    classDef out fill:#f44336,stroke:#b71c1c,color:#fff
    classDef lock fill:#ff9800,stroke:#e65100,color:#fff
    classDef diff fill:#2196f3,stroke:#0d47a1,color:#fff
    classDef doc fill:#fff9c4,stroke:#f57f17
```

**背诵口诀（仓管员必背）**：
> 🔼 **入库/调入**：Q↑ A↑  
> 🔽 **出库/调出/损耗/退货**：Q↓ A↓  
> 🔒 **锁定**：Q不变，L↑ A↓（先占位，后面出库审核才真扣）  
> 🧮 **盘点**：盘盈就加，盘亏就减  
> ✅ **availableQty = quantity - lockedQty**（这个公式是系统展示可用量的核心）

---

## 9. 库存流水追溯 关系图

> 财务/审计视角：任何库存变动都必须能追溯到业务单、操作人、操作时间。

```mermaid
erDiagram
    WMS_INVENTORY_LOG ||--o{ WMS_PURCHASE_ORDER : "billType=1追溯"
    WMS_INVENTORY_LOG ||--o{ WMS_STOCK_IN : "billType=2追溯"
    WMS_INVENTORY_LOG ||--o{ WMS_STOCK_OUT : "billType=3/4追溯"
    WMS_INVENTORY_LOG ||--o{ WMS_TRANSFER_ORDER : "billType=5/6追溯"
    WMS_INVENTORY_LOG ||--o{ WMS_LOSS_ORDER : "billType=7追溯"
    WMS_INVENTORY_LOG ||--o{ WMS_CHECK_ORDER : "billType=8追溯"

    SYS_USER ||--o{ WMS_INVENTORY_LOG : "操作人 operateBy"

    WMS_INVENTORY_LOG {
        bigint log_id PK "日志ID(雪花)"
        bigint bill_id FK "业务单ID"
        varchar bill_no "业务单号(核心追溯键)"
        int bill_type "1~8 对应ChangeType"
        bigint warehouse_id FK "仓库"
        bigint sku_id FK "商品SKU"
        bigint location_id FK "库位"
        varchar batch_no "批次号"
        int direction "方向 +1入/-1出"
        int qty_change "变动数量"
        int before_qty "变动前快照"
        int after_qty "变动后快照"
        decimal unit_price "单价"
        decimal amount_change "金额变动"
        bigint operate_by FK "→ sys_user.user_id"
        datetime operate_time "操作时间"
        varchar remark "备注"
    }

    WMS_STOCK_IN {
        bigint stock_in_id PK "入库单ID"
        varchar stock_in_no UK "入库单号 ← billNo"
        int status "3=已上架(触发库存)"
        bigint warehouse_id "入库仓库"
    }

    WMS_STOCK_OUT {
        bigint stock_out_id PK "出库单ID"
        varchar stock_out_no UK "出库单号 ← billNo"
        int status "4=已审核(触发库存)"
        bigint warehouse_id "出库仓库"
    }

    SYS_USER {
        bigint user_id PK "用户ID"
        varchar username "账号"
        varchar nickname "操作人姓名"
    }
```

**追溯用法示例**：
1. 财务发现某SKU本月库存少了100件 → 查询 `wms_inventory_log where sku_id=X and direction=-1`
2. 找到可疑记录 → 看 `bill_no` + `bill_type` → 比如 `bill_type=7 + bill_no=BS202608001`
3. 去报损单查 BS202608001 → 看操作人、审核人、报损原因 → 完成审计闭环

---

## 10. 各部门职责与协作 泳道图

```mermaid
flowchart LR
    subgraph 采购部
        direction TB
        P1[创建采购单] --> P2[跟进到货]
    end

    subgraph 销售部
        direction TB
        S1[创建销售单] --> S2[跟进发货/收款]
    end

    subgraph 仓库部
        direction TB
        W1[基础数据维护\n仓库/库区/库位]
        W2[入库作业\n按采购单收货上架]
        W3[出库作业\n拣货+打包+出库审核]
        W4[调拨作业\n调入/调出确认]
        W5[月末盘点\n+日常报损]
        W6[库存查询\n答复销售可发货量]
    end

    subgraph 财务部
        direction TB
        F1[销售收款确认]
        F2[采购对账]
        F3[库存成本核算]
        F4[审计库存流水]
    end

    subgraph 系统管理员
        direction TB
        A1[用户/角色/权限]
        A2[基础数据初始化\n商品分类/SPU/SKU/供应商/客户]
    end

    %% ===== 协作关系 =====
    P2 -->|到货通知| W2
    S1 -->|订单确认→| W3
    W3 -->|已出库通知| S2
    S2 -->|客户付款| F1
    P1 -->|采购合同| F2
    A2 -->|基础数据| P1 & S1 & W1 & W2 & W3
    A1 -->|权限分配| 所有部门
    W5 -->|盘点盈亏| F3
    W6 -.-> S1
    F4 -->|审计追溯| W2 & W3 & W4 & W5
```

---

## 附录：快速索引

| 业务问题 | 看哪张图 |
|---------|--------|
| 新人想快速了解系统全貌 | 图1 全业务流程图 |
| 采购问：为什么我的采购单显示已到货？ | 图2 采购→入库协同图 |
| 销售问：客户下单后仓库怎么一步步发货？ | 图3 销售→出库协同图 |
| 调拨时A仓说发了B仓说没收到？ | 图4 双仓调拨协同图 |
| 月末盘点流程老是走乱？ | 图5 盘点详细流程图 |
| 商品坏了怎么走流程？ | 图6 报损流程图 |
| 想知道某单据当前状态下一步该做啥？ | 图7 全单据状态机 |
| 库存变化原理，为什么有锁定量？ | 图8 库存数据变更核心导图（必背） |
| 审计要求查清楚每一笔库存变动的责任人 | 图9 库存流水追溯ER图 |
| 跨部门扯皮，界定谁该做什么？ | 图10 部门职责泳道图 |

---

> 📌 代码参考：
> - 状态机逻辑：各模块ServiceImpl，例如 [WmsStockInServiceImpl.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/stockin/service/impl/WmsStockInServiceImpl.java)
> - 库存Handler接口：[InventoryChangeHandler.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/inventory/handler/InventoryChangeHandler.java)
> - 8种变更类型：[ChangeType.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/inventory/handler/ChangeType.java)
> - 库存表结构：[WmsInventory.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/inventory/entity/WmsInventory.java)
> - 库存流水表：[WmsInventoryLog.java](file:///Users/caojinlong/Documents/trae_projects/wms-backend/src/main/java/com/example/wms/business/inventory/entity/WmsInventoryLog.java)
