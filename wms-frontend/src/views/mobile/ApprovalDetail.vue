<template>
  <div class="m-detail">
    <!-- ===== 沉浸式头部（状态色渐变 + 紧凑布局） ===== -->
    <div class="hero" :class="heroStatusClass">
      <!-- 安全区占位 -->
      <div class="hero-status"></div>

      <!-- 返回按钮行 -->
      <div class="hero-bar">
        <div class="circle-btn" @click="goBack">
          <van-icon name="arrow-left" size="18" />
        </div>
      </div>

      <!-- 顶部加载 -->
      <div v-if="loading" class="loading-inline">
        <van-loading type="spinner" color="#fff" size="24" />
      </div>

      <!-- 头部摘要（紧凑） -->
      <template v-else-if="order">
        <!-- 类型 + 状态合并 Pill -->
        <div class="hero-status-row">
          <span class="status-pill">
            <span class="pill-type">{{ typeText }}单</span>
            <span class="pill-dot"></span>
            <van-icon :name="statusIcon" size="13" />
            {{ statusText }}
          </span>
        </div>

        <!-- 金额大数字 -->
        <div class="hero-amount" v-if="order.totalAmount">
          <span class="amount-num">￥{{ formatAmount(order.totalAmount) }}</span>
          <span class="amount-label">{{ currentType.amountLabel }}</span>
        </div>

        <!-- 关键信息横排 -->
        <div class="hero-meta">
          <div class="meta-item">
            <div class="meta-label">{{ partyLabel }}</div>
            <div class="meta-value">{{ partyValue || '-' }}</div>
          </div>
          <div class="meta-divider" />
          <div class="meta-item">
            <div class="meta-label">种类</div>
            <div class="meta-value">{{ items.length }}</div>
          </div>
          <div class="meta-divider" />
          <div class="meta-item">
            <div class="meta-label">数量</div>
            <div class="meta-value">{{ order.totalQty || 0 }}</div>
          </div>
        </div>

        <!-- 单号 + 时间 -->
        <div class="hero-sub">
          <span class="hero-bill-no">{{ billNo }}</span>
          <span class="hero-time">{{ formatDate(order.createTime) }}</span>
        </div>
      </template>
    </div>

    <!-- ===== 内容区 ===== -->
    <template v-if="!loading && order">
      <!-- 金额构成卡（按类型动态渲染） -->
      <div class="section" v-if="amountRows.length > 0">
        <div class="amount-card">
          <div class="card-head">
            <span class="card-title">金额构成</span>
            <span class="card-badge">{{ currentType.text }}单</span>
          </div>
          <div class="amount-list">
            <div
              v-for="(row, idx) in amountRows"
              :key="idx"
              class="amount-row"
              :class="{ highlight: row.highlight, subtotal: row.subtotal }"
            >
              <span class="ar-label">{{ row.label }}</span>
              <span class="ar-value">
                <template v-if="row.suffix">{{ row.suffix }}</template>
                ￥{{ formatAmount(row.value) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 基本信息 -->
      <div class="section">
        <div class="section-title">
          <span class="title-dot"></span>基本信息
        </div>
        <div class="info-card">
          <div class="info-row" v-for="(row, idx) in basicRows" :key="idx">
            <span class="info-k">{{ row.label }}</span>
            <span class="info-v" :class="{ mono: row.mono && !row.value }">{{ row.value || '—' }}</span>
          </div>
        </div>
      </div>

      <!-- 明细 -->
      <div class="section" v-if="items.length > 0">
        <div class="section-title">
          <span class="title-dot"></span>明细
          <span class="section-sub">共 {{ items.length }} 项</span>
        </div>
        <div
          v-for="(item, idx) in items"
          :key="idx"
          class="item-card"
        >
          <div class="item-top">
            <span class="item-idx">#{{ idx + 1 }}</span>
            <span class="item-name">{{ getItemName(item) }}</span>
          </div>
          <div class="item-grid">
            <div class="grid-cell">
              <div class="gk">SKU</div>
              <div class="gv mono">{{ item.skuCode || '-' }}</div>
            </div>
            <div class="grid-cell">
              <div class="gk">规格</div>
              <div class="gv">{{ item.specText || item.spec || '-' }}</div>
            </div>
            <div class="grid-cell">
              <div class="gk">数量</div>
              <div class="gv">{{ item.quantity || item.qty || 0 }}</div>
            </div>
            <div class="grid-cell">
              <div class="gk">单价</div>
              <div class="gv">￥{{ formatAmount(item.unitPrice || item.price) }}</div>
            </div>
          </div>
          <div class="item-total">
            <span>小计</span>
            <span class="item-subtotal">￥{{ formatAmount(item.subtotal || (item.quantity * item.unitPrice)) }}</span>
          </div>
        </div>
      </div>
    </template>

    <van-empty v-else-if="!loading" description="单据不存在" />

    <!-- 底部操作栏 -->
    <div v-if="order && order.status === 1" class="action-bar">
      <button class="btn btn-reject" @click="openReject">
        <van-icon name="cross" size="16" />
        <span>驳回</span>
      </button>
      <button class="btn btn-approve" :disabled="submitting" @click="handleApprove">
        <van-loading v-if="submitting" size="16" color="#fff" />
        <template v-else>
          <van-icon name="success" size="16" />
        </template>
        <span>通过</span>
      </button>
    </div>

    <!-- 驳回弹窗 -->
    <van-dialog
      v-model:show="showRejectDialog"
      title="驳回原因"
      show-cancel-button
      confirm-button-text="确认驳回"
      cancel-button-text="取消"
      confirm-button-color="#ff4d4f"
      @confirm="handleReject"
    >
      <div class="reject-box">
        <textarea
          v-model="rejectRemark"
          class="reject-textarea"
          placeholder="请填写驳回原因（必填）"
          rows="4"
        />
      </div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showFailToast, showSuccessToast } from 'vant'
import {
  purchaseApi, stockInApi, stockOutApi, transferApi, lossApi, saleApi
} from '@/api'

defineOptions({ name: 'MobileApprovalDetail' })

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const order = ref<any>(null)
const items = ref<any[]>([])
const showRejectDialog = ref(false)
const rejectRemark = ref('')
const submitting = ref(false)

// ===== 金额明细行配置 =====
interface AmountRow {
  label: string       // 行标签
  field?: string      // 直接取 order 字段
  value?: number      // 或自定义值
  suffix?: string     // 前缀（如 "-" 折扣）
  highlight?: boolean // 高亮（最终金额）
  subtotal?: boolean  // 小计样式（商品小计）
}

// ===== 单据类型配置 =====
interface DetailConfig {
  key: string
  text: string
  color: string
  noField: string
  partyLabel: string
  partyField: string[]
  amountLabel: string
  amountRows: (o: any) => AmountRow[]
  getById: (id: string) => Promise<any>
  audit: (data: any) => Promise<any>
}

const typeConfigMap: Record<string, DetailConfig> = {
  purchase: {
    key: 'purchase', text: '采购', color: '#5a67d8',
    noField: 'purchaseNo', partyLabel: '供应商', partyField: ['supplierName'],
    amountLabel: '应付总金额',
    // 采购单：商品小计 + 运费 + 折扣 + 税率 + 税额 + 其他费用 + 最终金额
    amountRows: (o) => {
      const rows: AmountRow[] = []
      // 商品金额小计
      if (o.subtotal != null) rows.push({ label: '商品金额', field: 'subtotal', subtotal: true })
      // 运费
      if (o.freight != null && Number(o.freight) !== 0) rows.push({ label: '运费', field: 'freight' })
      // 折扣
      if (o.discountRate != null && Number(o.discountRate) !== 0) {
        const discountAmt = Number(o.subtotal || 0) * Number(o.discountRate || 0)
        rows.push({ label: `折扣 ${(Number(o.discountRate) * 100).toFixed(0)}%`, value: discountAmt, suffix: '-' })
      }
      // 税率 + 税额
      if (o.taxRate != null && Number(o.taxRate) !== 0) {
        rows.push({ label: `税率 ${(Number(o.taxRate) * 100).toFixed(0)}%`, field: 'taxAmount' })
      }
      // 其他费用
      if (o.otherAmount != null && Number(o.otherAmount) !== 0) rows.push({ label: '其他费用', field: 'otherAmount' })
      // 最终金额（高亮）
      if (o.totalAmount != null) rows.push({ label: '应付合计', field: 'totalAmount', highlight: true })
      return rows
    },
    getById: (id) => purchaseApi.getById(id),
    audit: (data) => purchaseApi.audit(data)
  },
  stockin: {
    key: 'stockin', text: '入库', color: '#52c41a',
    noField: 'stockInNo', partyLabel: '供应商', partyField: ['supplierName'],
    amountLabel: '入库总金额',
    amountRows: (o) => {
      const rows: AmountRow[] = []
      if (o.totalAmount != null) rows.push({ label: '入库合计', field: 'totalAmount', highlight: true })
      return rows
    },
    getById: (id) => stockInApi.getById(id),
    audit: (data) => stockInApi.audit(data)
  },
  stockout: {
    key: 'stockout', text: '出库', color: '#1890ff',
    noField: 'stockOutNo', partyLabel: '客户', partyField: ['customerName'],
    amountLabel: '出库总金额',
    amountRows: (o) => {
      const rows: AmountRow[] = []
      if (o.totalCost != null) rows.push({ label: '成本金额', field: 'totalCost', subtotal: true })
      if (o.saleAmount != null && Number(o.saleAmount) !== 0) rows.push({ label: '销售金额', field: 'saleAmount', highlight: true })
      else if (o.totalAmount != null) rows.push({ label: '出库合计', field: 'totalAmount', highlight: true })
      return rows
    },
    getById: (id) => stockOutApi.getById(id),
    audit: (data) => stockOutApi.audit(data)
  },
  transfer: {
    key: 'transfer', text: '调拨', color: '#f5576c',
    noField: 'transferNo', partyLabel: '仓库', partyField: ['outWarehouseName', 'inWarehouseName'],
    amountLabel: '调拨总金额',
    amountRows: (o) => {
      const rows: AmountRow[] = []
      if (o.totalAmount != null) rows.push({ label: '调拨合计', field: 'totalAmount', highlight: true })
      return rows
    },
    getById: (id) => transferApi.getById(id),
    audit: (data) => transferApi.audit(data)
  },
  loss: {
    key: 'loss', text: '报损', color: '#ff4d4f',
    noField: 'lossNo', partyLabel: '报损商品', partyField: ['skuName'],
    amountLabel: '报损总金额',
    amountRows: (o) => {
      const rows: AmountRow[] = []
      if (o.totalAmount != null) rows.push({ label: '报损合计', field: 'totalAmount', highlight: true })
      return rows
    },
    getById: (id) => lossApi.getById(id),
    audit: (data) => lossApi.audit(data)
  },
  sale: {
    key: 'sale', text: '销售', color: '#faad14',
    noField: 'saleNo', partyLabel: '客户', partyField: ['customerName'],
    amountLabel: '销售总金额',
    amountRows: (o) => {
      const rows: AmountRow[] = []
      if (o.goodsAmount != null) rows.push({ label: '商品金额', field: 'goodsAmount', subtotal: true })
      if (o.discountAmount != null && Number(o.discountAmount) !== 0) rows.push({ label: '折扣', field: 'discountAmount', suffix: '-' })
      if (o.saleAmount != null) rows.push({ label: '销售合计', field: 'saleAmount', highlight: true })
      if (o.receivedAmount != null && Number(o.receivedAmount) !== 0) rows.push({ label: '已收金额', field: 'receivedAmount' })
      return rows
    },
    getById: (id) => saleApi.getById(id),
    audit: (data) => saleApi.audit(data)
  }
}

// 当前类型配置
const currentType = computed<DetailConfig>(() => {
  const t = route.params.type as string
  return typeConfigMap[t] || typeConfigMap.purchase
})

const typeText = computed(() => currentType.value.text)
const typeColor = computed(() => currentType.value.color)
const partyLabel = computed(() => currentType.value.partyLabel)

const billNo = computed(() => {
  if (!order.value) return '-'
  return order.value[currentType.value.noField] || order.value.billNo || '-'
})

const partyValue = computed(() => {
  if (!order.value) return ''
  if (currentType.value.key === 'transfer') {
    return [order.value.outWarehouseName, order.value.inWarehouseName]
      .filter(Boolean).join(' → ')
  }
  for (const f of currentType.value.partyField) {
    if (order.value[f]) return order.value[f]
  }
  return ''
})

// 金额明细行（解析 field → 实际值）
const amountRows = computed(() => {
  if (!order.value) return []
  const cfg = currentType.value
  const raw = cfg.amountRows(order.value)
  return raw.map(r => {
    const val = r.value ?? (r.field ? order.value[r.field] : 0) ?? 0
    return { ...r, value: Number(val) }
  }).filter(r => r.value !== 0 || r.highlight) // 过滤零值行（保留高亮最终金额）
})

// 基本信息
const basicRows = computed(() => {
  if (!order.value) return []
  const rows = [
    { label: '创建人', value: order.value.createName || '-' },
    { label: '总数量', value: order.value.totalQty || 0 },
    { label: '备注', value: order.value.remark || '', mono: true }
  ]
  if (order.value.auditBy) {
    rows.push({ label: '审核人', value: order.value.auditName || '-' })
    rows.push({ label: '审核时间', value: formatDate(order.value.auditTime) })
  }
  if (currentType.value.key === 'transfer') {
    rows.splice(1, 0, { label: '调出仓库', value: order.value.outWarehouseName || '-' })
    rows.splice(2, 0, { label: '调入仓库', value: order.value.inWarehouseName || '-' })
  }
  return rows
})

// 状态文案
const statusText = computed(() => {
  const s = order.value?.status
  const map: Record<number, string> = {
    0: '草稿', 1: '待审核', 2: '已通过', 3: '部分到货', 4: '已完成', 5: '已作废'
  }
  return map[s] || `状态 ${s}`
})

const heroStatusClass = computed(() => {
  const s = order.value?.status
  if (s === 2 || s === 3 || s === 4) return 'hero-pass'
  if (s === 5) return 'hero-void'
  return 'hero-default'
})

const statusIcon = computed(() => {
  const s = order.value?.status
  if (s === 2 || s === 3 || s === 4) return 'passed'
  if (s === 5) return 'cross'
  return 'clock-o'
})

function getItemName(item: any): string {
  return item.skuName || item.goodsName || item.spuName || item.skuCode || '-'
}

// ===== 加载详情 =====
onMounted(async () => {
  const id = route.params.id as string
  if (!id) { loading.value = false; return }
  try {
    const res: any = await currentType.value.getById(id)
    const data = res.data || res
    order.value = data.order || data.bill || data
    items.value = data.items || data.billItems || []
  } catch (e: any) {
    showFailToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
})

// ===== 审核操作 =====
async function handleApprove() {
  submitting.value = true
  try {
    await currentType.value.audit({ id: route.params.id, pass: true })
    showSuccessToast('审核通过')
    setTimeout(() => router.replace('/mobile/approval'), 600)
  } catch (e: any) {
    showFailToast(e.message || '操作失败')
  } finally { submitting.value = false }
}

function openReject() { rejectRemark.value = ''; showRejectDialog.value = true }

async function handleReject() {
  if (!rejectRemark.value?.trim()) { showToast('请填写驳回原因'); return }
  submitting.value = true
  try {
    await currentType.value.audit({ id: route.params.id, pass: false, remark: rejectRemark.value.trim() })
    showSuccessToast('已驳回')
    showRejectDialog.value = false
    setTimeout(() => router.replace('/mobile/approval'), 600)
  } catch (e: any) {
    showFailToast(e.message || '操作失败')
  } finally { submitting.value = false }
}

function goBack() { router.back() }

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 16)
}

function formatAmount(amount: number) {
  if (amount == null || isNaN(Number(amount))) return '0.00'
  return Number(amount).toFixed(2)
}
</script>

<style scoped>
.m-detail {
  min-height: 100vh;
  background: #f4f5f9;
  padding-bottom: 90px;
}

/* ===== 沉浸式头部（紧凑版） ===== */
.hero {
  padding: 0 18px 24px;
  position: relative;
  color: #fff;
}

/* 三种状态渐变 */
.hero-default { background: linear-gradient(135deg, #667eea 0%, #5a67d8 50%, #764ba2 100%); }
.hero-pass    { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }
.hero-void    { background: linear-gradient(135deg, #8c8c8c 0%, #595959 100%); }

.hero-status {
  height: env(safe-area-inset-top);
}

.hero-bar {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.circle-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #fff;
  transition: transform 0.15s;
}

.circle-btn:active { transform: scale(0.92); }

/* 加载中 */
.loading-inline {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

/* ===== 状态 Pill（合并类型 + 状态） ===== */
.hero-status-row {
  margin-bottom: 12px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(8px);
  padding: 5px 12px;
  border-radius: 18px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.pill-type {
  font-weight: 700;
  opacity: 0.95;
}

.pill-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
  margin: 0 2px;
}

/* ===== 金额大数字（紧凑 + 标签在下方） ===== */
.hero-amount {
  margin-bottom: 14px;
}

.amount-num {
  font-size: 38px;
  font-weight: 800;
  letter-spacing: -0.5px;
  display: block;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  line-height: 1;
}

.amount-label {
  font-size: 12px;
  opacity: 0.75;
  display: block;
  margin-top: 4px;
}

/* ===== 关键信息横排（紧凑玻璃拟态） ===== */
.hero-meta {
  display: flex;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 10px 0;
  margin-bottom: 10px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.meta-item {
  flex: 1;
  text-align: center;
  padding: 0 2px;
}

.meta-label {
  font-size: 11px;
  opacity: 0.7;
  margin-bottom: 2px;
}

.meta-value {
  font-size: 13px;
  font-weight: 700;
  max-width: 80px;
  margin: 0 auto;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-divider {
  width: 1px;
  background: rgba(255, 255, 255, 0.25);
}

/* 单号 + 时间 */
.hero-sub {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  opacity: 0.8;
}

.hero-bill-no {
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-size: 11px;
  background: rgba(255, 255, 255, 0.18);
  padding: 2px 10px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

/* ===== 内容区（紧凑间距） ===== */
.section {
  padding: 4px 14px 0;
}

.section-title {
  padding: 12px 4px 6px;
  font-size: 14px;
  color: #1a1a2e;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-dot {
  width: 3px;
  height: 13px;
  border-radius: 2px;
  background: linear-gradient(180deg, #667eea, #764ba2);
}

.section-sub {
  font-size: 11px;
  color: #bbb;
  font-weight: normal;
  margin-left: auto;
}

/* ===== 金额构成卡 ===== */
.amount-card {
  background: #fff;
  border-radius: 14px;
  padding: 14px 16px 10px;
  box-shadow: 0 2px 14px rgba(0, 0, 0, 0.05);
}

.card-head {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #f0f0f0;
}

.card-title {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a2e;
}

.card-badge {
  margin-left: auto;
  font-size: 11px;
  color: #5a67d8;
  background: #eef2ff;
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 600;
}

.amount-list {
  display: flex;
  flex-direction: column;
}

.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 7px 0;
  font-size: 13px;
  color: #595959;
}

.amount-row.subtotal .ar-label {
  color: #1a1a2e;
  font-weight: 600;
}

.amount-row.subtotal .ar-value {
  color: #1a1a2e;
  font-weight: 700;
}

.amount-row.highlight {
  border-top: 1px solid #f0f0f0;
  margin-top: 4px;
  padding-top: 10px;
}

.amount-row.highlight .ar-label {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a2e;
}

.amount-row.highlight .ar-value {
  font-size: 20px;
  font-weight: 800;
  background: linear-gradient(135deg, #ff4d4f, #ff7a45);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}

.ar-label {
  color: #8c8c8c;
}

.ar-value {
  font-weight: 600;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}

/* ===== 基本信息卡 ===== */
.info-card {
  background: #fff;
  border-radius: 14px;
  padding: 2px 16px;
  box-shadow: 0 2px 14px rgba(0, 0, 0, 0.05);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 11px 0;
  border-bottom: 1px solid #f7f7f7;
  font-size: 13px;
}

.info-row:last-child { border-bottom: none; }

.info-k {
  color: #8c8c8c;
  flex-shrink: 0;
  margin-right: 12px;
}

.info-v {
  color: #1a1a2e;
  text-align: right;
  word-break: break-all;
  font-weight: 500;
}

.info-v.mono { color: #bbb; }

/* ===== 明细卡 ===== */
.item-card {
  background: #fff;
  border-radius: 14px;
  padding: 12px 14px;
  margin-bottom: 8px;
  box-shadow: 0 2px 14px rgba(0, 0, 0, 0.05);
  transition: transform 0.15s;
}

.item-card:active { transform: scale(0.99); }

.item-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.item-idx {
  min-width: 24px;
  height: 22px;
  padding: 0 6px;
  border-radius: 6px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.item-name {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a2e;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 14px;
  padding: 8px 0;
  border-top: 1px dashed #f0f0f0;
  border-bottom: 1px dashed #f0f0f0;
}

.grid-cell { font-size: 12px; }

.gk { color: #bfbfbf; margin-bottom: 2px; }

.gv { color: #1a1a2e; font-size: 13px; font-weight: 500; }

.mono {
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-size: 12px;
  color: #5a67d8;
}

.item-total {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #8c8c8c;
}

.item-subtotal {
  font-size: 16px;
  font-weight: 800;
  background: linear-gradient(135deg, #ff4d4f, #ff7a45);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* ===== 底部操作栏 ===== */
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 10px;
  padding: 10px 14px calc(env(safe-area-inset-bottom) + 10px);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  box-shadow: 0 -2px 20px rgba(0, 0, 0, 0.06);
  z-index: 30;
}

.btn {
  flex: 1;
  height: 46px;
  border: none;
  border-radius: 23px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: transform 0.15s;
}

.btn:active { transform: scale(0.98); }

.btn-reject {
  background: #fff;
  border: 1.5px solid #ff4d4f;
  color: #ff4d4f;
}

.btn-approve {
  background: linear-gradient(135deg, #52c41a, #389e0d);
  color: #fff;
  box-shadow: 0 4px 14px rgba(82, 196, 26, 0.3);
}

.btn-approve:disabled { opacity: 0.7; }

/* 驳回弹窗 */
.reject-box { padding: 10px 14px; }

.reject-textarea {
  width: 100%;
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 10px;
  font-size: 14px;
  outline: none;
  resize: none;
  box-sizing: border-box;
  font-family: inherit;
}

.reject-textarea:focus { border-color: #667eea; }
</style>
