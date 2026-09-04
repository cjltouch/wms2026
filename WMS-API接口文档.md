# WMS 仓库管理系统 - API 接口文档（v1.0）

> **文档版本**: v1.0  
> **编制日期**: 2026-08-24  
> **基础 URL**: `http://{host}:{port}/wms-api`（本地开发默认 `http://127.0.0.1:8080/wms-api`）  
> **鉴权方式**: 登录后返回 `access_token`，所有业务请求 Header 携带：`Authorization: Bearer <token>`  
> **请求 / 响应 Content-Type**: `application/json;charset=UTF-8`  
> **分页默认**: `pageNum=1, pageSize=20`，最大 `pageSize=500`（报表导出不限）  
> **ID 类型说明**: 全部 ID 为 Long（雪花算法），**JSON 序列化统一为字符串**，避免 JS `Number.MAX_SAFE_INTEGER` 精度丢失  
> **在线调试（Knife4j）**: 运行后端后访问 `http://127.0.0.1:8080/wms-api/doc.html`，无需手动翻阅本 MD

---

## 目录
- [0. 通用约定（响应体/分页/错误码/时间格式/枚举）](#0-通用约定)
- [1. 登录 / 鉴权模块](#1-登录--鉴权模块)
- [2. 用户管理](#2-用户管理)
- [3. 权限 / 角色 / 菜单 / 部门](#3-权限--角色--菜单--部门)
- [4. 商品管理（SPU/SKU/分类/品牌/单位）](#4-商品管理spuskucategorybrandunit)
- [5. 供应商 & 客户](#5-供应商--客户)
- [6. 仓库 / 区域 / 库位](#6-仓库--区域--库位)
- [7. 采购管理（批量商品）](#7-采购管理批量商品)
- [8. 入库管理（批量商品 + 批次/库位/SN）](#8-入库管理批量商品--批次库位sn)
- [9. 出库管理（批量商品 + 预扣库存 + FIFO/FEFO）](#9-出库管理批量商品--预扣库存--fifofefo)
- [10. 调拨管理（跨仓，批量商品）](#10-调拨管理跨仓批量商品)
- [11. 销售管理（批量商品 + 出库预扣）](#11-销售管理批量商品--出库预扣)
- [12. 报损管理（批量商品 + 原因）](#12-报损管理批量商品--原因)
- [13. 盘点管理（批量商品 + 盈亏调整）](#13-盘点管理批量商品--盈亏调整)
- [14. 库存查询 / 库存流水 / 效期预警](#14-库存查询--库存流水--效期预警)
- [15. 仓储统计（首页看板 / 汇总）](#15-仓储统计首页看板--汇总)
- [16. 报表导出（Excel）](#16-报表导出excel)
- [17. 操作日志 / 登录日志](#17-操作日志--登录日志)
- [附录 A：库存状态流转总览图](#附录-a库存状态流转总览图)
- [附录 B：13 个单据状态枚举](#附录-b13-个单据状态枚举)

---

## 0. 通用约定
### 0.1 统一响应体 `R<T>`
```json
{ "code": 200, "msg": "操作成功", "data": { /* T */ }, "timestamp": 1755974317000 }
```
| code | 含义 | 前端处理 |
|------|------|---------|
| `200` | 成功 | 取 `data` 渲染 |
| `400` | 参数错误（`msg` 附带字段名） | 表单校验提示 |
| `401` | 未登录 / Token 过期 / 密码修改后旧 Token 失效 | 跳转登录页，刷新 Token 接口 401 时再跳转 |
| `403` | 权限不足（具体接口级权限） | 按钮置灰 + Toast |
| `404` | 资源不存在（如单据 ID 无效） | 路由跳转 404 页 |
| `10001` | 库存不足 | 提示并回到单据明细 |
| `10002` | 单据当前状态不允许此操作 | 提示刷新 |
| `10003` | 单据明细为空 | 必须至少 1 条 |
| `10005` | SKU 启用批次但批次号为空 | 行内错误提示 |
| `10008` | 乐观锁并发冲突 | 提示"数据已被他人修改，请刷新重试" |
| `500` | 系统异常 | 友好提示 + 记录日志 |

### 0.2 分页响应 `PageRsp<T>`
```json
{
  "total": 273,
  "rows": [/* T 数组 */],
  "pageNum": 1,
  "pageSize": 20,
  "summary": { /* 可选：金额合计/数量合计行 */ }
}
```

### 0.3 日期格式
- 所有时间字段：`yyyy-MM-dd HH:mm:ss`（字符串）
- 纯日期字段（生产日期 / 效期 / 到货日）：`yyyy-MM-dd`

### 0.4 Decimal / Long 精度
- `amount / price / qty`：数字（字符串或 Number 都行，后端按 BigDecimal/Integer 解析）
- `id / *_id`：**一律字符串**（64bit Long > `2^53`）

### 0.5 列表查询通用筛选字段
除每个模块专属筛选外，所有列表接口都支持：
```
pageNum=1
pageSize=20
orderBy=create_time desc
dateRangeStart=2026-08-01 00:00:00
dateRangeEnd=2026-08-24 23:59:59
```

---

## 1. 登录 / 鉴权模块
> **模块前缀**: `/api/system/auth`  
> **权限**: 全部匿名（登录/刷新 Token/验证码）

### 1.1 账号密码登录
- **POST** `/api/system/auth/login`
- **请求体**:
  ```json
  { "username": "admin", "password": "123456", "captcha": "A3X9", "uuid": "xxx" }
  ```
  | 字段 | 类型 | 必填 | 说明 |
  |-----|------|------|------|
  | username | String | ✅ | `sys_user.username` 唯一索引 |
  | password | String | ✅ | 明文（HTTPS 传输；后端 BCrypt 比对） |
  | captcha | String | ✅ | 图形验证码，登录失败 ≥ 3 次启用；可配置关闭 |
  | uuid | String | ✅ | 对应验证码缓存 key（无 Redis 时用 JVM 本地 Caffeine 10 分钟过期） |
- **成功响应 data**:
  ```json
  {
    "accessToken":  "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9....",
    "refreshToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9....",
    "expiresIn": 43200,
    "tokenType": "Bearer"
  }
  ```

### 1.2 刷新 Token
- **POST** `/api/system/auth/refresh-token`
- **请求体**: `{ "refreshToken": "xxx" }`
- **成功响应**: 同登录，返回新的 access_token + refresh_token（滚动续签）
- **失败**：refresh 过期 → 401 跳转登录

### 1.3 获取当前登录人信息（含菜单树 + 按钮权限）
- **GET** `/api/system/auth/user-info`
- **Header**: `Authorization: Bearer <accessToken>`
- **成功响应 data**:
  ```json
  {
    "userId":   "1",
    "username": "admin",
    "nickname": "超级管理员",
    "avatar":   "/avatars/admin.png",
    "warehouseIds": ["1","2"],
    "dataScope": 1,
    "roles":    ["SUPER_ADMIN"],
    "perms":    ["wms:purchase:add","wms:purchase:audit","wms:stockin:*"],
    "routers": [
      { "path":"/wms/purchase", "name":"Purchase", "component":"wms/purchase/index",
        "meta":{ "title":"采购管理","icon":"shopping-cart" },
        "children":[ /* 嵌套菜单 */ ] }
    ]
  }
  ```

### 1.4 登出
- **POST** `/api/system/auth/logout`
- **响应**: 成功 200（无 Redis，JWT 自过期；可选写入"用户主动登出时间"作为 12.1 辅助判断）

### 1.5 图形验证码（可选）
- **GET** `/api/system/auth/captcha`
- **响应**: `{ "uuid": "xxx", "img": "data:image/svg+xml;base64,....", "enabled": true }`

---

## 2. 用户管理
> **前缀**: `/api/system/user` ｜ 权限前缀: `sys:user:*`

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| 2.1 | POST | `/page` | `sys:user:query` | 用户分页查询（支持用户名/手机号/状态/部门/角色/日期筛选） |
| 2.2 | GET  | `/{id}` | `sys:user:query` | 详情（含关联的 roleIds[] / deptId） |
| 2.3 | POST | `/` | `sys:user:add` | 新增（username 唯一；返回 id） |
| 2.4 | PUT  | `/` | `sys:user:edit` | 修改（支持重设 nickname / dept / roles / 状态；不可改 username） |
| 2.5 | DELETE | `/{id}` | `sys:user:remove` | 逻辑删除（保留操作日志外键） |
| 2.6 | POST | `/batch-delete` | `sys:user:remove` | 批量逻辑删除：`{ "ids": ["1","2","3"] }` |
| 2.7 | PUT  | `/reset-pwd` | `sys:user:resetPwd` | 管理员重置密码：`{ "userId":"1","password":"123456" }` |
| 2.8 | PUT  | `/update-pwd` | 登录即可 | 当前用户改自己密码：`{ "oldPwd":"","newPwd":"" }` |
| 2.9 | PUT  | `/change-status` | `sys:user:edit` | 启用/停用：`{ "userId":"1","status": 0 }` |
| 2.10| POST | `/import` | `sys:user:import` | Excel 批量导入（先预览 → 再确认） |
| 2.11| GET  | `/template` | - | 下载导入模板 xlsx |
| 2.12| GET  | `/export` | `sys:user:export` | 按筛选条件导出 xlsx |

**示例 2.1 用户分页请求**：
```json
{
  "pageNum": 1, "pageSize": 20,
  "deptId": "101",
  "roleId": "3",
  "username": "zhang",
  "mobile":   "13800138",
  "status":   1,
  "dateRangeStart": "2026-01-01 00:00:00",
  "dateRangeEnd":   "2026-08-24 23:59:59"
}
```

---

## 3. 权限 / 角色 / 菜单 / 部门
> **角色前缀**: `/api/system/role` `sys:role:*`  
> **菜单前缀**: `/api/system/menu` `sys:menu:*`  
> **部门前缀**: `/api/system/dept` `sys:dept:*`

### 3.1 角色（8 个接口）
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/role/page` | 角色分页（名称/编码/状态） |
| GET  | `/role/{id}` | 详情（含 menuIds[] / deptIds[] / dataScope） |
| POST | `/role` | 新增角色 + 分配菜单 + 分配数据权限范围（1~5） |
| PUT  | `/role` | 修改 |
| DELETE | `/role/{id}` | 逻辑删除 |
| POST | `/role/batch-delete` | 批量删除（已被用户引用 → 禁止并提示） |
| PUT  | `/role/{id}/data-scope` | 单独保存数据权限 + 部门（CUSTOM 模式下的 deptIds[]） |
| GET  | `/role/list-all` | 不分页（用户编辑页下拉框用） |

### 3.2 菜单（按钮级权限，树状）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | `/menu/tree` | 返回嵌套树（含按钮级 perms，角色编辑页勾选用） |
| GET  | `/menu/{id}` | 详情 |
| POST | `/menu` | 新增：type=M目录/C菜单/F按钮；菜单填 path/component/icon；按钮填 `perms` 字符串如 `wms:purchase:audit` |
| PUT  | `/menu` | 修改 |
| DELETE | `/menu/{id}` | 删除（有子节点/已被角色引用不允许删） |

### 3.3 部门
| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | `/dept/tree` | 部门树（用户管理页筛选 + 角色 CUSTOM 数据权限用） |
| GET  | `/dept/{id}` | 详情 |
| POST | `/dept` | 新增（parentId/leader/phone/email/orderNum） |
| PUT  | `/dept` | 修改 |
| DELETE | `/dept/{id}` | 删除（有子部门/已挂用户不允许） |

---

## 4. 商品管理（SPU/SKU/Category/Brand/Unit）
> **前缀**: `/api/wms/goods` `/api/wms/category` `/api/wms/brand` `/api/wms/unit`  
> **权限**: `wms:goods:*` 等

### 4.1 分类（树状）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | `/category/tree` | 嵌套树（SPU 新建页下拉） |
| POST | `/category` ｜ PUT ｜ DELETE `/{id}` | 增改删 |

### 4.2 品牌 & 单位
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/brand/page` ｜ `/unit/page` | 分页 |
| POST | `/brand` ｜ `/unit` ｜ PUT ｜ DELETE | 增改删；`uk_brand_code` `uk_unit_code` 唯一校验 |

### 4.3 商品 SPU + SKU（核心，一对多批量保存）
> 一张 SPU 下可多个 SKU（不同颜色/容量/规格）。
| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/goods/spu/page` | `wms:goods:query` | SPU 分页（SPU名/分类/品牌/状态/关键字模糊匹配 spu_name+spu_code） |
| GET  | `/goods/spu/{spuId}` | `wms:goods:query` | 详情：**SPU主表 + SKU数组**（用于编辑页回填） |
| POST | `/goods/spu` | `wms:goods:add` | 保存 SPU + 批量 SKU[]，结构见下 |
| PUT  | `/goods/spu` | `wms:goods:edit` | 编辑（删除原 SKU 再批量重插） |
| DELETE | `/goods/spu/{spuId}` | `wms:goods:remove` | 逻辑删除（存在库存/未完成单据 → 禁止） |
| POST | `/goods/spu/batch-change-status` | `wms:goods:edit` | 批量上下架：`{ "ids":["1","2"],"status":0 }` |
| POST | `/goods/spu/import` | `wms:goods:import` | Excel 批量导入 SPU+SKU（带校验报告） |
| GET  | `/goods/spu/export` | `wms:goods:export` | 导出 |
| GET  | `/goods/sku/search` | 登录即可 | 出库/入库/采购 选择 SKU 弹框：按 keyword/分类/仓库筛选，返回 `[{skuId, skuCode, skuName, barcode, specText, unitName, 可用库存}]` |
| GET  | `/goods/sku/{skuId}` | `wms:goods:query` | SKU 详情（含 SN/批次/ABC/效期 管理标识） |

**4.3 保存 SPU+批量 SKU 请求示例**：
```json
{
  "spuId": null,
  "spuCode": "SPU0000001",
  "spuName": "316L不锈钢保温杯",
  "categoryId": "1024",
  "brandId": "8",
  "unitId": "3",
  "origin": "浙江永康",
  "picUrls": ["https://xxx/1.jpg","https://xxx/2.jpg"],
  "abcLevel": "A",
  // ============ 批量 SKU ============
  "skuList": [
    { "skuId": null, "skuCode": "SKU0000001-1", "barcode": "6912345678001",
      "specText": "500ml 黑色", "weightG": 380, "volumeMl": 500, "color": "黑",
      "batchFlag": 1, "expireFlag": 0, "snFlag": 0, "shelfLifeDays": null,
      "defaultCost": 45.00, "defaultSale": 99.00 },
    { "skuId": null, "skuCode": "SKU0000001-2", "barcode": "6912345678002",
      "specText": "750ml 白色", "weightG": 520, "volumeMl": 750, "color": "白",
      "batchFlag": 1, "defaultCost": 55.00, "defaultSale": 129.00 }
  ]
}
```

---

## 5. 供应商 & 客户
> 供应商 `/api/wms/supplier` ｜ 客户 `/api/wms/customer`

| 方法 | （供应商/客户共用同一套 URI 模式） | 说明 |
|------|------|------|
| POST | `/page` | 分页（编码/名称/联系人/状态/信用等级） |
| GET  | `/{id}` | 详情（含联系人列表 JSON 或子表） |
| POST | `/` | 新增（`uk_supplier_code` / `uk_customer_code` 唯一） |
| PUT  | `/` | 修改 |
| DELETE | `/{id}` | 逻辑删除（未完成单据引用 → 禁止） |
| POST | `/batch-delete` | 批量逻辑删除 |
| GET  | `/list-all` | 下拉列表（采购单/入库单/销售单 用） |
| GET  | `/export` ｜ POST `/import` | 导入导出 Excel |

---

## 6. 仓库 / 区域 / 库位
> 仓库 `/api/wms/warehouse` ｜ 区域 `/api/wms/area` ｜ 库位 `/api/wms/location`

| 方法 | 说明 |
|------|------|
| POST `/warehouse/page` | 仓库分页（编码/名称/类型/状态） |
| GET `/warehouse/{id}` + POST/PUT/DELETE | 增改删；`uk_warehouse_code` 唯一 |
| POST `/area/page` | 区域分页（归属仓库 ID 必填；`uk_wh_area_code` 仓库+区域编码联合唯一） |
| POST `/area/tree` | 按仓库返回 "仓库→区域→库位" 三层树（库存页左侧导航） |
| POST `/location/page` | 库位分页（仓库/区域/库位类型/状态/关键字） |
| GET `/location/{id}` + POST/PUT/DELETE | 增改删；`uk_location_code` 全局唯一；库位类型=拣货/存储/退货/不良品/待检 |
| POST `/location/batch-generate` | **批量生成库位**（按模板：A01-01-01 ~ A10-10-05，返回条数） |
| POST `/location/import` | Excel 批量导入库位 |

---

## 7. 采购管理（批量商品）
> **前缀**: `/api/wms/purchase` ｜ 权限 `wms:purchase:*`  
> **状态枚举**: `0=草稿 1=已提交 2=已审核 3=部分到货 4=已完成 5=已作废`  
> **核心流程**: 保存草稿 → 提交 → 审核 → 到货（入库单回写）→ 自动状态流转 `2→3→4`  
> **对账视图**: `v_purchase_reconcile`（已在 MySQL 导入）

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| 7.1 | POST | `/page` | `wms:purchase:query` | 分页（单号/供应商/仓库/状态/日期/采购员） |
| 7.2 | GET  | `/{id}` | `wms:purchase:query` | 详情：**主表 + 明细数组 + 状态时间线** |
| 7.3 | GET  | `/by-no/{purchaseNo}` | `wms:purchase:query` | 按单号查（入库单引用源单） |
| 7.4 | POST | `/` | `wms:purchase:add` | 保存草稿（单头+多行明细），返回 id |
| 7.5 | PUT  | `/` | `wms:purchase:edit` | 修改（**仅草稿/已提交未审核**允许改） |
| 7.6 | DELETE | `/{id}` | `wms:purchase:remove` | 删除（仅草稿） |
| 7.7 | POST | `/submit` | `wms:purchase:edit` | 提交审核（校验明细非空 + 金额合计一致性） |
| 7.8 | POST | `/audit` | `wms:purchase:audit` | 审核通过（写状态日志；状态 `1→2`） |
| 7.9 | POST | `/unaudit` | `wms:purchase:audit` | 反审核（回退到草稿；仅已审核且未到货允许） |
| 7.10| POST | `/void` | `wms:purchase:void` | 作废（已作废后不可恢复；未到货才能作废） |
| 7.11| POST | `/batch-audit` | `wms:purchase:audit` | 批量审核：`{ "ids":["1","2"], "remark":"批量审核" }` |
| 7.12| POST | `/batch-void` | `wms:purchase:void` | 批量作废 |
| 7.13| GET  | `/reconcile/page` | `wms:purchase:reconcile` | 采购对账分页（基于 v_purchase_reconcile）：采购数/已到货/未到货/金额对账差 |
| 7.14| GET  | `/export` ｜ `/reconcile/export` | 导出 Excel |

**7.4 保存草稿请求示例（单头 + 批量明细）**：
```json
{
  "id": null,
  "purchaseNo": "PO202608240001",
  "supplierId": "12",
  "warehouseId": "3",
  "purchaseBy": "5",          // 采购员ID
  "expectDate": "2026-08-30",
  "taxRate": 13.00,
  "freight": 88.00,
  "discountRate": null,
  "otherAmount": 0,
  "remark": "首批采购，送货前致电",
  // ========== 批量商品（N 条） ==========
  "items": [
    { "skuId": "10001", "lineNo": 1, "quantity": 100, "purchasePrice": 45.00,
      "taxRate": 13, "expectDate": "2026-08-28", "suggestBatch": "P260828A",
      "remark": "外箱标签贴 SKU 条码" },
    { "skuId": "10002", "lineNo": 2, "quantity": 50,  "purchasePrice": 55.00,
      "suggestBatch": "P260828B" }
  ]
}
```

---

## 8. 入库管理（批量商品 + 批次/库位/SN）
> **前缀**: `/api/wms/stock-in` ｜ 权限 `wms:stock-in:*`  
> **入库类型**: `1=采购入库 2=调拨入库 3=生产入库 4=盘盈入库 5=其他入库 6=销售退货入库 7=采购退货拒收`  
> **状态**: `0=草稿 1=已提交 2=已验收 3=已上架(完成) 4=已作废`  
> **审核 = 库存生效（审核前允许任意改；审核后锁定 + 写库存 + 回写源单）**

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 8.1 | POST | `/page` | 分页（单号/类型/源单号/仓库/供应商/状态/日期） |
| 8.2 | GET  | `/{id}` | 详情：主表 + 明细（含批次/SN JSON/库位）+ 状态时间线 |
| 8.3 | POST | `/from-source` | **一键拉取源单**（如选了采购单号，自动带入未到货 SKU + Qty） |
| 8.4 | POST | `/` | 保存草稿（批量明细；每条明细含 expected/actual 两量） |
| 8.5 | PUT  | `/` | 修改（草稿态） |
| 8.6 | POST | `/submit` | 提交验收（expected_qty=actual_qty 警告允许） |
| 8.7 | POST | `/audit` | **审核（=上架）**：执行 3 件事：① 写库存（StockInInventoryHandler）② 写库存流水 ③ 回写源单（如采购单 delivered_qty） |
| 8.8 | POST | `/unaudit` | 反审核（5 分钟内允许，对应撤销入库三动作） |
| 8.9 | POST | `/void` | 作废（未审核） |
| 8.10| POST | `/batch-audit` | 批量审核 |
| 8.11| POST | `/allocate-location` | 自动分配库位（SKU 常规存储位推荐） |
| 8.12| GET  | `/export` | 导出入库明细 xlsx |

**8.4 保存请求（批量明细 + 批次 + 效期 + SN）**：
```json
{
  "id": null,
  "stockInNo": "SI2608240001",
  "type": 1,
  "sourceBillNo": "PO202608240001",
  "sourceItemId": null,
  "warehouseId": "3",
  "supplierId": "12",
  "inBy": "9",
  "remark": "",
  "items": [
    { "skuId": "10001", "lineNo": 1,
      "expectedQty": 100, "actualQty": 102, "diffQty": 2,
      "costPrice": 45.00, "subtotal": 4590.00,
      "batchNo": "P260824-A1", "produceDate": "2026-08-10", "expireDate": "2028-08-09",
      "supplierBatch": "SUP-88241",
      "locationId": "3014", "locationCode": "A01-02-05",
      "snList": null,
      "remark": "多送2个，属正常溢装" }
  ]
}
```

---

## 9. 出库管理（批量商品 + 预扣库存 + FIFO/FEFO）
> **前缀**: `/api/wms/stock-out` ｜ 权限 `wms:stock-out:*`  
> **出库类型**: `1=销售出库 2=调拨出库 3=领料出库 4=盘亏出库 5=报损出库 6=采购退货出库 7=其他出库`  
> **状态**: `0=草稿 1=已提交 2=已分配(锁定库存) 3=已拣货 4=已出库(完成) 5=已作废`  
> **分配策略 allocation_rule**: `1=FIFO 先入先出 2=FEFO 先到期先出 3=手动指定批次`

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 9.1 | POST | `/page` | 分页（单号/类型/源单号/仓库/客户/状态/日期） |
| 9.2 | GET  | `/{id}` | 详情：主表 + 明细 + 批次分配 JSON `allocation_json` + 时间线 |
| 9.3 | POST | `/from-source` | 拉取销售/调拨源单 |
| 9.4 | POST | `/preview-allocation` | **核心**：提交草稿后返回"按 FIFO/FEFO 计算的批次分配结果 + 库存不足 SKU 列表"，前端展示确认 |
| 9.5 | POST | `/` | 保存草稿（批量明细） |
| 9.6 | PUT  | `/` | 修改 |
| 9.7 | POST | `/lock-inventory` | **分配批次 → 锁定库存**（执行 StockOutLockInventoryHandler：`locked_qty+` `available_qty-`） |
| 9.8 | POST | `/pick-confirm` | 拣货确认（写入 picked_qty；可扫码逐条确认） |
| 9.9 | POST | `/audit` | **出库审核 = 实发**：StockOutConfirmInventoryHandler（`quantity-` `locked_qty-` + 写流水 + 回写源单发货数） |
| 9.10| POST | `/unaudit` | 反审核（同入库，5 分钟内） |
| 9.11| POST | `/void` | 作废（已分配状态需先释放锁定） |
| 9.12| POST | `/batch-audit` | 批量审核出库 |
| 9.13| GET  | `/export` | 出库明细导出 |

**9.4 预览分配（返回分配结果，用户确认后再锁定）**请求&响应：
- 请求：出库单草稿 id / 或直接传 items[] + warehouseId + strategy
- 响应：
  ```json
  {
    "ok": true,
    "shortageList": [
      { "skuId":"10002","skuName":"保温杯750ml","expected":50,"available":25,"short":25 }
    ],
    "allocationByLine": [
      {
        "lineNo": 1, "skuId":"10001","allocateQty":100,
        "batches": [
          { "batchNo":"P260501","locationId":"3014","qty":40,"produceDate":"2026-05-01","expireDate":"2028-04-30" },
          { "batchNo":"P260701","locationId":"3018","qty":60,"produceDate":"2026-07-01","expireDate":"2028-06-30" }
        ]
      }
    ]
  }
  ```

---

## 10. 调拨管理（跨仓，批量商品）
> **前缀**: `/api/wms/transfer` ｜ 权限 `wms:transfer:*`  
> **状态**: `0=草稿 1=已提交 2=已审核(调拨中) 3=已出库(出仓已发) 4=已完成(入仓上架) 5=已作废`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/page` | 分页（单号/出仓/入仓/状态/日期） |
| GET  | `/{id}` | 详情 + 明细 + 时间线 + 出仓操作日志/入仓操作日志 |
| POST | `/` ｜ PUT `/` | 草稿保存/修改（出仓必填 warehouse_out_id；入仓必填 warehouse_in_id） |
| POST | `/submit` / `/audit` | 提交 / 审核（通过后允许出仓） |
| POST | `/outbound-confirm` | **出仓确认**（出库仓扣减实存 + 写流水，状态 `2→3`） |
| POST | `/inbound-confirm` | **入仓确认**（入库仓增加实存 + 写流水，状态 `3→4`） |
| POST | `/unaudit` / `/void` | 反审核 / 作废 |
| GET  | `/export` | 调拨明细导出 |

---

## 11. 销售管理（批量商品 + 出库预扣）
> **前缀**: `/api/wms/sale` ｜ 权限 `wms:sale:*`  
> **状态**: `0=草稿 1=已提交 2=已审核 3=部分发货 4=已完成 5=已作废`  
> 销售审核后触发出库单 草稿 → 出库预扣（走 StockOutLock）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/page` | 分页（单号/客户/仓库/状态/日期/销售员） |
| GET  | `/{id}` | 详情 + 批量明细 + 折扣行 + 时间线 |
| POST | `/` ｜ PUT `/` | 草稿/修改（双金额：行小计 + 税后 + 整单折扣 + 运费） |
| POST | `/submit` / `/audit` / `/unaudit` | 审核流 |
| POST | `/create-stock-out/{saleId}` | 一键生成出库草稿（销售明细 → 出库明细；之后进入出库模块分配） |
| POST | `/void` ｜ `/batch-audit` | 作废 / 批量审核 |
| GET  | `/export` | 销售订单 / 销售明细导出 |

---

## 12. 报损管理（批量商品 + 原因）
> **前缀**: `/api/wms/loss` ｜ 权限 `wms:loss:*`  
> **状态**: `0=草稿 1=已提交 2=已审批 3=已出库(完成) 4=已作废`  
> **损因枚举**: `1=过期 2=破损 3=丢失 4=质量问题 5=其他`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/page` | 分页（单号/仓库/损因/状态/日期） |
| GET  | `/{id}` | 详情 + 批量明细（每条含损因/批次/库位/报损数量/金额） |
| POST | `/` ｜ PUT `/` | 保存草稿/修改 |
| POST | `/submit` / `/audit` | 审批：审核即扣实存（LossInventoryHandler） |
| POST | `/unaudit` / `/void` | 反审核（冲回库存）/作废 |
| GET  | `/export` | 报损单导出 |

---

## 13. 盘点管理（批量商品 + 盈亏调整）
> **前缀**: `/api/wms/check` ｜ 权限 `wms:check:*`  
> **盘点类型**: `1=全盘 2=抽盘 3=动碰盘 4=循环盘`  
> **状态**: `0=草稿 1=已下发 2=盘点中 3=已复盘 4=已审核 5=已作废`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/page` | 分页（单号/仓库/类型/状态/日期） |
| GET  | `/{id}` | 详情 + 明细（账面量 / 实盘量 / 差异量 / 盈亏金额） |
| POST | `/create-by-scope` | **按范围生成盘点任务**（仓库/区域/库位/分类/Brand/ABC；自动查账面，生成 N 条明细 actual_qty=0） |
| POST | `/` ｜ PUT `/` | 保存/修改 |
| POST | `/publish` | 下发（状态 `0→1`，生成 PDA 任务） |
| POST | `/count-enter` | 录入实盘数（批量）：`{ checkId, items:[{checkItemId, actualQty, remark}] }` |
| POST | `/recheck` | 复盘（对差异>0 的 SKU 二次确认） |
| POST | `/audit` | **审核**：盈亏自动调用 CheckInventoryHandler 写库存；状态 `3→4` |
| POST | `/unaudit` | 反审核（冲回） |
| POST | `/export-empty` | **导出空盘点表（打印给盘点员签字）**：实盘列置空 |
| GET  | `/export` | 导出盘点结果含盈亏 |

---

## 14. 库存查询 / 库存流水 / 效期预警
> **前缀**: `/api/wms/inventory` ｜ 权限 `wms:inventory:*`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/page` | 库存快照分页（`wms_inventory` + `v_inventory_full`），筛选：仓库/区域/库位/分类/品牌/关键字/available>0 仅看有货 |
| GET  | `/by-sku/{skuId}` | 某 SKU 分仓分批次全量库存 |
| POST | `/page-by-batch` | 按批次号检索库存（入库用批次号查找重复） |
| POST | `/log-page` | **库存流水分页**（`wms_inventory_log`）：按 SKU/仓库/单据类型/单号/日期；出入库 100% 可追溯 |
| POST | `/warning-expire` | 效期预警（默认剩 30 天；可调阈值 days=60）：返回 TOP 50 + 导出 |
| POST | `/warning-below-min` | 低于安全库存预警（SKU 安全库存字段） |
| POST | `/turnover-analytics` | 库存周转率（期间出库总成本 / 平均库存成本，SKU / 分类 / 品牌维度） |
| GET  | `/export` ｜ `/log/export` ｜ `/warning-expire/export` | 3 类导出 |

---

## 15. 仓储统计（首页看板 / 汇总）
> **前缀**: `/api/report/dashboard` ｜ 权限 `wms:dashboard:*`（默认全体登录用户只读）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | `/summary` | 6 张核心卡片：今日入库单数/出库单数/采购在途金额/库存SKU数/库存总金额/30天内到期批次 |
| POST | `/trend-30` | 最近 30 天出入库趋势（30 组 daily 数据：入库量/出库量/入库金额/出库金额） |
| POST | `/top-in-out-sku` | TOP10 入库 SKU / TOP10 出库 SKU（柱状图） |
| POST | `/warehouse-qty-pie` | 各仓库存占比（饼图） |
| POST | `/abnormal-summary` | 异常汇总卡片：库存不足 N 条 / 效期报警 N 条 / 待审采购 / 待入库 / 待出库 |
| POST | `/supplier-on-time` | 供应商准时到货率排行（用于采购考核） |

---

## 16. 报表导出（Excel）
> **前缀**: `/api/report/export` ｜ 权限：`wms:report:*`  
> **响应**: `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`；Header `Content-Disposition: attachment; filename=采购对账报表-20260824.xlsx`

| # | GET / POST | 路径 | 导出报表名 |
|---|-----------|------|-----------|
| 16.1 | POST | `/purchase-reconcile` | 采购对账报表（供应商/日期/单号/采购数/已到/未到/金额/税率） |
| 16.2 | POST | `/stock-in-detail` | 入库明细报表（全字段含批次/SN/库位/供应商） |
| 16.3 | POST | `/stock-out-detail` | 出库明细报表（含销售价/成本毛利分析） |
| 16.4 | POST | `/inventory-balance` | 库存余额表（SKU 维度三量 + 单价 + 金额） |
| 16.5 | POST | `/inventory-batch` | 库存批次效期表（批次号/生产日期/效期/剩余天数/数量/库位） |
| 16.6 | POST | `/expire-warning` | 效期预警（<=30 天，支持 15/30/60/90 档位） |
| 16.7 | POST | `/loss-detail` | 报损明细（期间 + 损因分析） |
| 16.8 | POST | `/check-profit-loss` | 盘点盈亏明细 |
| 16.9 | POST | `/sale-by-customer` | 销售报表：按客户/商品/期间汇总毛利 |
| 16.10| POST | `/oper-log` | 操作日志导出（见 17） |

---

## 17. 操作日志 / 登录日志
> **系统日志前缀**: `/api/system/log` ｜ 权限 `sys:log:*`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/oper/page` | 操作日志分页（模块/操作类型/操作人/状态/日期） |
| GET  | `/oper/{id}` | 详情：请求参数 JSON / 响应 JSON / 耗时 ms / 异常堆栈 |
| POST | `/oper/export` | 操作日志导出 xlsx |
| DELETE | `/oper/clean` | 清理 180 天前日志（管理员） |
| POST | `/login/page` | 登录日志（用户名/IP/浏览器/OS/登录状态/日期） |
| POST | `/login/export` | 登录日志导出 |

---

## 附录 A：库存状态流转总览图
```
采购到货(StockIn) ────► +实存 +可用 ──┐
调拨入库 ─────────────────► +实存 +可用 ─┤    实存 quantity
销售退货入库 ────────────► +实存 +可用 ─┤
盘点盘盈 ────────────────► +实存 +可用 ─┤   锁定 locked_qty
                                          │
                ┌─────可用 available_qty = quantity - locked_qty◄──┐
                │                                                   │
                │  销售审核/出库草稿分配（StockOutLock）            │
                │   └──► +锁定 -可用                                │
                │  调拨出库预扣                                       │
                │   └──► +锁定 -可用                                │
                │                                                   │
                ▼                                                   │
         出库审核发货（StockOutConfirm）◄───────────────────────────┘
           └──► -实存 -锁定
         调拨出库确认
           └──► -实存
         报损审核
           └──► -实存 -可用
         盘点盘亏
           └──► -实存 -可用
```

---

## 附录 B：13 个单据状态枚举

| 单据 | 状态码 → 文案 | 允许的下一步操作 |
|-----|--------------|----------------|
| **采购单** | 0草稿 → 1已提交 → 2已审核 → 3部分到货 → 4已完成 / 5已作废 | 审核后可反审核；未到货可作废；入库单回写自动流转 2→3→4 |
| **入库单** | 0草稿 → 1已提交 → 2已验收 → 3已上架 / 4作废 | 审核（=上架）为最终态 3；3 仅 5 分钟内可反审核撤销 |
| **出库单** | 0草稿 → 1已提交 → 2已分配 → 3已拣货 → 4已出库 / 5作废 | 2/3 之间可反复流转；4 为最终态 |
| **调拨单** | 0草稿 → 1已提交 → 2已审核 → 3已出库 → 4已完成 / 5作废 | 3→4 必须由"入仓确认" |
| **销售单** | 0草稿 → 1已提交 → 2已审核 → 3部分发货 → 4已完成 / 5作废 | 生成出库单；出库回写 2→3→4 |
| **报损单** | 0草稿 → 1已提交 → 2已审批 → 3已完成 / 4作废 | 审核后完成 |
| **盘点单** | 0草稿 → 1已下发 → 2盘点中 → 3已复盘 → 4已审核 / 5作废 | 审核后盈亏落库存 |
