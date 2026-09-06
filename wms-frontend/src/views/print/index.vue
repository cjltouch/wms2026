<template>
  <div class="print-page">
    <!-- 屏幕操作工具栏（打印时隐藏） -->
    <div class="toolbar no-print">
      <div class="toolbar-left">
        <span class="toolbar-label">打印联次：</span>
        <el-checkbox-group v-model="selectedCopies">
          <el-checkbox
            v-for="c in copies"
            :key="c.key"
            :value="c.key"
          >
            <span :style="{ color: c.color, fontWeight: 600 }">{{ c.name }}</span>
            <span class="copy-desc-inline">（{{ c.desc }}）</span>
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <div class="toolbar-right">
        <el-button @click="goBack">返回</el-button>
        <el-button type="primary" :loading="loading" :disabled="!activeCopies.length" @click="doPrint">打印（{{ activeCopies.length }}联）</el-button>
      </div>
    </div>

    <div v-loading="loading" class="bill-wrap">
      <!-- 一式多联：每个联次独立一页，打印时自动分页 -->
      <div
        v-for="(copy, idx) in activeCopies"
        :key="copy.key"
        class="copy-page"
      >
        <div class="bill">
          <!-- 标题行：左留白 / 中标题 / 右联次标签（正常文档流，不遮挡表格） -->
          <div class="bill-header">
            <div class="header-side"></div>
            <div class="bill-title">{{ title }}</div>
            <div class="header-side header-right">
              <div class="copy-badge" :style="{ color: copy.color, borderColor: copy.color }">
                <div class="badge-name">{{ copy.name }}</div>
                <div class="badge-no">第 {{ idx + 1 }} 联 / 共 {{ activeCopies.length }} 联</div>
              </div>
            </div>
          </div>

          <!-- 单据头信息 -->
          <table class="meta-table">
            <tbody>
              <tr v-for="(row, ri) in metaRows" :key="ri">
                <template v-for="(cell, ci) in row" :key="ci">
                  <td class="meta-label">{{ cell.label }}</td>
                  <td class="meta-value">{{ cell.value || ' ' }}</td>
                </template>
              </tr>
            </tbody>
          </table>

          <!-- 商品明细 -->
          <table class="items-table">
            <thead>
              <tr>
                <th
                  v-for="(col, ci) in columns"
                  :key="ci"
                  :style="{ width: col.width || 'auto', textAlign: col.align || 'left' }"
                >
                  {{ col.label }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, ii) in items" :key="ii">
                <td
                  v-for="(col, ci) in columns"
                  :key="ci"
                  :style="{ textAlign: col.align || 'left' }"
                >
                  {{ col.value(item, ii) }}
                </td>
              </tr>
              <tr v-if="!items.length">
                <td :colspan="columns.length" class="empty-tip">无明细数据</td>
              </tr>
            </tbody>
          </table>

          <!-- 合计 -->
          <div class="totals">
            <span v-for="(t, ti) in totals" :key="ti" class="total-item">
              <b>{{ t.label }}：</b>{{ t.value }}
            </span>
          </div>

          <!-- 备注 -->
          <div class="remark"><b>备注：</b>{{ remark || ' ' }}</div>

          <!-- 签字区整体（含单据编号，避免跨页时无法识别单据） -->
          <div class="sign-block">
            <!-- 单据编号：跨页时签字人可据此确认所属单据 -->
            <div class="bill-no-bar">
              <b>{{ billType === 'stock-in' ? '入库单号' : '出库单号' }}：{{ billNo }}</b>
            </div>

            <!-- 签字区 -->
            <div class="sign-area">
              <div v-for="(s, si) in signatures" :key="si" class="sign-item">
                <span class="sign-role">{{ s.role }}：</span>
                <span class="sign-line">{{ s.name || '' }}</span>
              </div>
              <div class="sign-item">
                <span class="sign-role">日期：</span>
                <span class="sign-line date-line"></span>年
                <span class="sign-line date-short"></span>月
                <span class="sign-line date-short"></span>日
              </div>
            </div>

            <!-- 联次用途与打印时间 -->
            <div class="copy-foot">
              <span>本联用途：<b :style="{ color: copy.color }">{{ copy.desc }}</b></span>
              <span>打印时间：{{ printTime }}</span>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && !activeCopies.length" description="请至少选择一个联次" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { stockInApi, stockOutApi, warehouseApi, supplierApi, customerApi } from '@/api'

type Align = 'left' | 'center' | 'right'
interface Col {
  label: string
  width?: string
  align?: Align
  value: (r: any, i: number) => any
}
interface CopyDef {
  key: string
  name: string
  color: string
  desc: string
}

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const billType = route.params.billType as string
const billId = route.params.id as string

const title = ref('')
const meta = ref<{ label: string; value: any }[]>([])
const columns = ref<Col[]>([])
const items = ref<any[]>([])
const totals = ref<{ label: string; value: any }[]>([])
const remark = ref('')
const billNo = ref('')
const signatures = ref<{ role: string; name?: string }[]>([])
const printTime = ref('')

/** 一式多联联次定义（第三联：出库单为客户联，入库单为供应商/送货方联） */
const copies = computed<CopyDef[]>(() => {
  const partner = billType === 'stock-in'
    ? { name: '供应商联', desc: '送货方留存' }
    : { name: '客户联', desc: '客户签收留存' }
  return [
    { key: 'stub', name: '存根联', color: '#303133', desc: '仓库留存备查' },
    { key: 'finance', name: '财务联', color: '#c0392b', desc: '财务记账凭证' },
    { key: 'partner', name: partner.name, color: '#b8860b', desc: partner.desc },
  ]
})

/** 当前勾选要打印的联次（默认三联全打） */
const selectedCopies = ref<string[]>(['stub', 'finance', 'partner'])
const activeCopies = computed<CopyDef[]>(() =>
  copies.value.filter((c) => selectedCopies.value.includes(c.key))
)

const metaRows = computed(() => {
  const rows: any[] = []
  for (let i = 0; i < meta.value.length; i += 3) {
    const cells = meta.value.slice(i, i + 3)
    while (cells.length < 3) cells.push({ label: ' ', value: ' ' })
    rows.push(cells)
  }
  return rows
})

const money = (v: any) => '¥' + Number(v || 0).toFixed(2)

function nowStr() {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function loadNames() {
  const [wh, sup, cus] = await Promise.all([
    warehouseApi.page({ pageNum: 1, pageSize: 200 }).catch(() => ({ data: { rows: [] } })),
    supplierApi.page({ pageNum: 1, pageSize: 200 }).catch(() => ({ data: { rows: [] } })),
    customerApi.page({ pageNum: 1, pageSize: 200 }).catch(() => ({ data: { rows: [] } })),
  ])
  return {
    warehouseName: (id: any) => (wh.data.rows || []).find((w: any) => w.warehouseId == id)?.warehouseName || '-',
    supplierName: (id: any) => (sup.data.rows || []).find((s: any) => s.supplierId == id)?.supplierName || '-',
    customerName: (id: any) => (cus.data.rows || []).find((c: any) => c.customerId == id)?.customerName || '-',
  }
}

async function loadData() {
  loading.value = true
  try {
    const names = await loadNames()
    if (billType === 'stock-in') {
      await buildStockIn(names)
    } else {
      await buildStockOut(names)
    }
    printTime.value = nowStr()
  } catch (e) {
    ElMessage.error('加载单据数据失败')
  } finally {
    loading.value = false
  }
}

async function buildStockIn(names: any) {
  const res: any = await stockInApi.getById(billId)
  const order = res.data?.order || {}
  const list = res.data?.items || []
  title.value = '入库单'
  billNo.value = order.stockInNo || '-'
  meta.value = [
    { label: '入库单号', value: order.stockInNo },
    { label: '入库类型', value: typeText(order.type) },
    { label: '单据状态', value: statusText(order.status) },
    { label: '入库仓库', value: order.warehouseName || names.warehouseName(order.warehouseId) },
    { label: '供应商', value: order.supplierName || names.supplierName(order.supplierId) },
    { label: '来源单号', value: order.sourceBillNo || '-' },
    { label: '入库人', value: order.inByName || order.inBy || '-' },
    { label: '创建时间', value: order.createTime },
    { label: '审核时间', value: order.auditTime || '-' },
  ]
  columns.value = [
    { label: '序号', width: '45px', align: 'center', value: (_r, i) => i + 1 },
    { label: '商品编码', width: '110px', value: (r) => r.skuCode },
    { label: '内部编码', width: '110px', value: (r) => r.innerCode || '-' },
    { label: '商品名称', value: (r) => r.skuName },
    { label: '供应商', width: '120px', value: (r) => r.supplierName || '-' },
    { label: '规格', width: '110px', value: (r) => r.specText || '-' },
    { label: '单位', width: '60px', value: (r) => r.unitName || '-' },
    { label: '应收数量', width: '75px', align: 'right', value: (r) => r.expectedQty ?? r.quantity ?? 0 },
    { label: '实收数量', width: '75px', align: 'right', value: (r) => r.actualQty ?? r.quantity ?? 0 },
    //{ label: '批次号', width: '105px', value: (r) => r.batchNo || '-' },
    //{ label: '库位', width: '90px', value: (r) => r.locationCode || '-' },
    //{ label: '成本单价', width: '90px', align: 'right', value: (r) => money(r.costPrice) },
  ]
  items.value = list
  totals.value = [
    { label: '总数量', value: order.totalQty ?? list.reduce((s: number, r: any) => s + Number(r.actualQty ?? r.quantity ?? 0), 0) },
    //{ label: '总金额', value: money(order.totalAmount ?? order.totalCost) },
  ]
  remark.value = order.remark
  signatures.value = [
    { role: '制单人' },
    { role: '送货人' },
    { role: '仓管员（验收上架）' },
    { role: '审核人' },
    { role: '收货人（入库人）', name: order.inByName || order.inBy },
  ]
}

async function buildStockOut(names: any) {
  const res: any = await stockOutApi.getById(billId)
  const order = res.data?.order || {}
  const list = res.data?.items || []
  title.value = '出库单'
  billNo.value = order.stockOutNo || '-'
  meta.value = [
    { label: '出库单号', value: order.stockOutNo },
    { label: '出库类型', value: outTypeText(order.type) },
    { label: '单据状态', value: outStatusText(order.status) },
    { label: '出库仓库', value: order.warehouseName || names.warehouseName(order.warehouseId) },
    { label: '客户名称', value: order.customerName || names.customerName(order.customerId) },
    { label: '来源单号', value: order.sourceBillNo || '-' },
    { label: '出库人', value: order.outByName || order.outBy || '-' },
    { label: '创建时间', value: order.createTime },
    { label: '审核时间', value: order.auditTime || '-' },
  ]
  columns.value = [
    { label: '序号', width: '45px', align: 'center', value: (_r, i) => i + 1 },
    { label: '商品编码', width: '110px', value: (r) => r.skuCode },
    { label: '内部编码', width: '110px', value: (r) => r.innerCode || '-' },
    { label: '商品名称', value: (r) => r.skuName },
    { label: '供应商', width: '120px', value: (r) => r.supplierName || '-' },
    { label: '规格', width: '110px', value: (r) => r.specText || '-' },
    { label: '单位', width: '60px', value: (r) => r.unitName || '-' },
    { label: '应出数量', width: '75px', align: 'right', value: (r) => r.expectedQty ?? r.quantity ?? 0 },
    { label: '实出数量', width: '75px', align: 'right', value: (r) => r.actualQty ?? r.quantity ?? 0 },
    //{ label: '批次号', width: '105px', value: (r) => r.batchNo || '-' },
    //{ label: '库位', width: '90px', value: (r) => r.locationCode || '-' },
    // { label: '销售单价', width: '90px', align: 'right', value: (r) => money(r.salePrice) },
    //{ label: '小计', width: '95px', align: 'right', value: (r) => money(r.subtotalSale ?? r.subtotal) },
  ]
  items.value = list
  totals.value = [
    { label: '总数量', value: order.totalQty ?? list.reduce((s: number, r: any) => s + Number(r.actualQty ?? r.quantity ?? 0), 0) },
    //{ label: '总金额', value: money(order.totalSale ?? order.totalAmount) },
  ]
  remark.value = order.remark
  signatures.value = [
    { role: '制单人' },
    { role: '出库人', name: order.outByName || order.outBy },
    { role: '仓管员（拣货复核）' },
    { role: '审核人' },
    { role: '提货人' },
  ]
}

function typeText(t: number) {
  return ({ 1: '采购入库', 2: '调拨入库', 3: '退货入库', 4: '其他入库' } as Record<number, string>)[t] || '-'
}
function statusText(s: number) {
  return ({ 0: '草稿', 1: '已提交', 2: '验收中', 3: '已入库', 4: '已审核', 5: '已作废' } as Record<number, string>)[s] || '-'
}
function outTypeText(t: number) {
  return ({ 1: '销售出库', 2: '调拨出库', 3: '退货出库', 4: '其他出库' } as Record<number, string>)[t] || '-'
}
function outStatusText(s: number) {
  return ({ 0: '草稿', 1: '已提交', 2: '已锁定', 3: '拣货完成', 4: '已审核', 5: '已作废' } as Record<number, string>)[s] || '-'
}

function doPrint() {
  if (!activeCopies.value.length) {
    ElMessage.warning('请至少选择一个联次')
    return
  }
  window.print()
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    window.close()
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.print-page {
  background: #f0f2f5;
  min-height: 100vh;
}

.toolbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-wrap: wrap;
}

.toolbar-label {
  font-weight: 600;
  margin-right: 4px;
}

.copy-desc-inline {
  color: #909399;
  font-size: 12px;
}

.bill-wrap {
  padding: 24px;
}

.copy-page {
  display: flex;
  justify-content: center;
  margin-bottom: 28px;
}

.bill {
  position: relative;
  width: 960px;
  max-width: 100%;
  background: #fff;
  padding: 32px 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  color: #000;
  font-size: 13px;
}

/* 标题行：三列栅格，左右等宽保证标题真正居中，联次标签在右列 */
.bill-header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  margin-bottom: 14px;
  min-height: 52px;
}

.header-side {
  display: flex;
  align-items: center;
}

.header-right {
  justify-content: flex-end;
}

/* 联次标识：标题右侧彩色边框标签（文档流内，不遮挡下方表格） */
.copy-badge {
  border: 2px solid;
  border-radius: 4px;
  padding: 2px 12px;
  text-align: center;
  line-height: 1.4;

  .badge-name {
    font-size: 16px;
    font-weight: 700;
    letter-spacing: 4px;
  }

  .badge-no {
    font-size: 11px;
    font-weight: 400;
  }
}

.bill-title {
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 6px;
}

.meta-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 12px;

  td {
    border: 1px solid #000;
    padding: 6px 8px;
  }

  .meta-label {
    background: #f5f5f5;
    font-weight: 600;
    white-space: nowrap;
    width: 90px;
  }

  .meta-value {
    width: 230px;
  }
}

.items-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 10px;

  th,
  td {
    border: 1px solid #000;
    padding: 6px 8px;
    word-break: break-all;
  }

  th {
    background: #f5f5f5;
    font-weight: 600;
  }

  .empty-tip {
    text-align: center;
  }
}

.totals {
  display: flex;
  justify-content: flex-end;
  gap: 28px;
  margin: 6px 0 14px;
  font-size: 14px;
}

.remark {
  border: 1px solid #000;
  padding: 8px;
  min-height: 48px;
  margin-bottom: 18px;
}

/* 单据编号栏：跨页时签字区若单独成页，可据此确认所属单据 */
.bill-no-bar {
  font-size: 13px;
  color: #000;
  margin-bottom: 12px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #999;
}

.sign-area {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 28px;
  margin: 10px 0 18px;
}

.sign-item {
  display: flex;
  align-items: flex-end;
  min-width: 180px;
}

.sign-role {
  font-weight: 600;
  white-space: nowrap;
}

.sign-line {
  display: inline-block;
  min-width: 110px;
  border-bottom: 1px solid #000;
  height: 22px;
  padding: 0 6px;
}

.date-line {
  min-width: 60px;
}

.date-short {
  min-width: 40px;
  margin: 0 4px;
}

.copy-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  color: #333;
  font-size: 12px;
}

/* 打印：隐藏工具栏，每联独占一页 */
@media print {
  .no-print {
    display: none !important;
  }

  .print-page {
    background: #fff;
  }

  .bill-wrap {
    padding: 0;
  }

  .copy-page {
    margin-bottom: 0;
    page-break-after: always;
    break-after: page;
  }

  .copy-page:last-child {
    page-break-after: auto;
    break-after: auto;
  }

  .bill {
    width: 100%;
    box-shadow: none;
    padding: 0;
  }

  /* 明细表格表头在跨页时重复显示 */
  .items-table thead {
    display: table-header-group;
  }

  /* 签字区整体（单号+签字+底部信息）尽量不被拆分到两页 */
  .sign-block {
    page-break-inside: avoid;
    break-inside: avoid;
  }
}
</style>
