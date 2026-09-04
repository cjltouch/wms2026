# WMS 系统模块测试 Bug 整理文档

> 测试时间：2026-08-24
> 测试方式：后端 API 接口测试 + 前端 TypeScript 类型检查（vue-tsc）
> 测试账号：admin / admin123

## 一、用户上报的 Bug（已修复并验证）

| # | 模块 | 问题描述 | 根因 | 修复方案 | 验证结果 |
|---|------|----------|------|----------|----------|
| 1 | 库区管理 | 新增库区失败 | `wms_area` 表缺少 `manager`、`phone` 列；`AreaSaveReq` DTO 缺少对应字段 | 给 `wms_area` 表新增 `manager`、`phone` 列；DTO 补字段 | ✅ POST /api/wms/area 返回 200 |
| 2 | 商品 SPU | 编辑 SPU 保存时报错"数据已存在" | 编辑时旧 SKU 走逻辑删除（`deleted=1`），但 `sku_code` 唯一索引包含已逻辑删除记录，导致重新插入时唯一键冲突 | 新增 `WmsGoodsSkuMapper.physicalDeleteBySpuId`，编辑前对旧 SKU 物理删除再重新插入 | ✅ PUT /api/wms/goods/spu 返回 200 |

## 二、本次全模块测试发现的问题

### 2.1 后端 API 接口测试结果

对全部模块（商品/往来/仓库/订单/库存/系统）的列表、分页、新增、编辑、删除接口逐一测试：

- **商品模块**（brand/category/unit/spu/sku）：list-all / page / add / update / getById / delete 全部 200 ✅
- **往来模块**（customer/supplier）：list-all / page / add / update / delete 全部 200 ✅
- **仓库模块**（warehouse/area/location）：list-all / page / add / update / delete 全部 200 ✅
- **订单模块**（purchase/stockin/stockout/transfer/check/loss/sale）：POST /page 全部 200 ✅
- **库存模块**（inventory）：POST /page、POST /log/page 全部 200 ✅
- **系统模块**（user/role/menu/dept/tree、log/oper-page、log/login-page）：全部 200 ✅
- **仪表盘**（dashboard/summary）：200 ✅

> 后端接口层无残留 Bug。所有"参数类型错误"/"已存在"为测试用例自身使用了错误的路径或重复编码，非系统缺陷。

### 2.2 前端 TypeScript 类型检查残留 Bug（vue-tsc 报错）

`npx vue-tsc --noEmit` 发现 6 个视图文件存在 `el-tag :type` 类型不匹配错误，根因均为状态映射函数返回 `string` 而 Element Plus 的 `TagType` 仅接受 `'primary' | 'success' | 'warning' | 'info' | 'danger' | undefined`。

| # | 文件 | 报错行数 | 问题函数 | 错误信息 |
|---|------|----------|----------|----------|
| 1 | src/views/check/index.vue | 68, 222 | `statusTagType` | Type 'string' is not assignable to type TagType |
| 2 | src/views/loss/index.vue | 44, 54, 187, 193 | `lossTypeTagType`、`statusTagType` | 同上 |
| 3 | src/views/sale/index.vue | 58, 63, 252, 257 | `statusTagType`、`payStatusTagType` | 同上 |
| 4 | src/views/stockout/index.vue | 57, 184 | `statusTagType` | 同上 |
| 5 | src/views/transfer/index.vue | 54, 196 | `statusTagType` | 同上 |
| 6 | src/views/inventory/index.vue | 28 | `getSummaries` | `show-summary` 的 `summary-method` 期望 `(string \| VNode)[]`，函数返回 `(string \| number)[]` |

### 2.3 修复方案

- 对 #1~#5：参照已修复的 `purchase/index.vue` 模式，新增 `type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'`，将状态映射表的值类型由 `string` 改为 `TagType | undefined`，函数返回类型标注为 `TagType | undefined`。
- 对 #6：将 `getSummaries` 的 `sums` 数组类型改为 `string[]`，数值合计项用 `String()` 转换。

## 三、修复状态

- [x] BUG-1 库区新增失败（已修复）
- [x] BUG-2 SPU 编辑数据已存在（已修复）
- [x] BUG-3 check/index.vue el-tag 类型（已修复）
- [x] BUG-4 loss/index.vue el-tag 类型（已修复）
- [x] BUG-5 sale/index.vue el-tag 类型（已修复）
- [x] BUG-6 stockout/index.vue el-tag 类型（已修复）
- [x] BUG-7 transfer/index.vue el-tag 类型（已修复）
- [x] BUG-8 inventory/index.vue getSummaries 类型（已修复）

## 四、验证

- 后端：全模块 API（list-all / page / add / update / delete / POST /page）均返回 200
- 前端：`npx vue-tsc --noEmit` 退出码 0，0 类型错误

## 五、订单模块"下拉选择后不显示"Bug（2026-08-24 补充）

### 5.1 根因

采购/入库/出库/调拨/销售/报损/盘点/库存等订单模块的下拉选择（供应商/客户/仓库/SKU）使用 `:key="s.id"` / `:value="s.id"`，但后端 `list-all` 接口返回的实体主键是 `supplierId` / `customerId` / `warehouseId` / `skuId`（实体专用主键名，没有 `id` 字段）。导致 `:value` 绑定的值为 `undefined`，用户选中后 `el-select` 的 v-model 也变成 `undefined`，UI 上表现为"选择后不显示"。

附带影响：`row.id` 调用订单 API（getById/submit/audit/void 等）也是 `undefined`，导致后续查看/提交/审核/作废都无法进行。

### 5.2 修复范围

| # | 文件 | 下拉修复 | API id 修复 |
|---|------|----------|-------------|
| 1 | src/views/purchase/index.vue | 供应商、入库仓库、SKU（3处）+ onSkuSelect 查找 | row.id→row.purchaseId（5处）+ form.id→form.purchaseId |
| 2 | src/views/stockin/index.vue | 入库仓库、供应商、SKU（3处）+ onSkuSelect 查找 | row.id→row.stockInId（5处）+ form.id→form.stockInId |
| 3 | src/views/stockout/index.vue | 出库仓库（1处） | row.id→row.stockOutId（5处） |
| 4 | src/views/transfer/index.vue | 搜索×2、调出/调入仓库（4处） | row.id→row.transferId（7处） |
| 5 | src/views/loss/index.vue | 搜索、表单仓库（2处） | row.id→row.lossId（6处） |
| 6 | src/views/check/index.vue | 仓库（1处） | row.id→row.checkId（9处） |
| 7 | src/views/sale/index.vue | 客户、出库仓库（2处） | row.id→row.saleId（9处）+ payForm.id→payForm.saleId |
| 8 | src/views/inventory/index.vue | 仓库（1处） | — |

### 5.3 修复方案

- 下拉：`:key`/`:value` 由 `s.id`/`w.id`/`c.id` 改为对应的实体主键（`s.supplierId`/`w.warehouseId`/`c.customerId`/`s.skuId`）
- SKU 选中后查找：`skuOptions.value.find(s => s.id === row.skuId)` → `s.skuId === row.skuId`
- 订单 API 调用：`row.id` → 对应订单主键（purchaseId/stockInId/stockOutId/transferId/lossId/checkId/saleId），含 `form.id` / `payForm.id` 等表单字段
- 未触动正确的外键字段：`row.skuId`、`row.warehouseId`、`row.supplierId`、`row.customerId` 等保持不变

### 5.4 验证

- `npx vue-tsc --noEmit` 退出码 0，0 类型错误
- 全代码 grep：`":key="[scw]\.id"|:value="[scw]\.id"` 0 匹配
- 浏览器实测采购单新增：
  - 供应商下拉展开显示"金太阳/森马"，选中后正确显示"金太阳" ✅
  - 入库仓库下拉展开显示"测试仓库01/仓库02"，选中后正确显示"测试仓库01" ✅
