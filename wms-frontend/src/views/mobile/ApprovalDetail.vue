<template>
  <div class="m-detail">
    <!-- ===== 沉浸式头部（状态色渐变） ===== -->
    <div class="hero" :class="heroStatusClass">
      <!-- 多层渐变光斑 -->
      <div class="hero-orb orb-a"></div>
      <div class="hero-orb orb-b"></div>
      <div class="hero-orb orb-c"></div>

      <!-- 状态栏占位 -->
      <div class="hero-status"></div>

      <!-- 操作按钮行 -->
      <div class="hero-bar">
        <div class="circle-btn" @click="goBack">
          <van-icon name="arrow-left" size="18" />
        </div>
        <div class="hero-type-tag">
          <span class="type-tag-text" :style="{ color: typeColor }">{{ typeText }}单</span>
        </div>
      </div>

      <!-- 顶部加载 -->
      <div v-if="loading" class="loading-inline">
        <van-loading type="spinner" color="#fff" size="24" />
      </div>

      <!-- 头部摘要 -->
      <template v-else-if="order">
        <!-- 状态 Pill -->
        <div class="hero-status-row">
          <span class="status-pill">
            <van-icon :name="statusIcon" size="14" />
            {{ statusText }}
          </span>
        </div>

        <!-- 金额大数字（渐变色文字 + 阴影） -->
        <div class="hero-amount" v-if="order.totalAmount">
          <span class="amount-label">{{ currentType.amountLabel }}</span>
          <span class="amount-num">￥{{ formatAmount(order.totalAmount) }}</span>
        </div>

        <!-- 关键信息横排（玻璃拟态） -->
        <div class="hero-meta">
          <div class="meta-item">
            <div class="meta-label">{{ partyLabel }}</div>
            <div class="meta-value">{{ partyValue || '-' }}</div>
          </div>
          <div class="meta-divider" />
          <div class="meta-item">
            <div class="meta-label">商品种类</div>
            <div class="meta-value">{{ items.length }}</div>
          </div>
          <div class="meta-divider" />
          <div class="meta-item">
            <div class="meta-label">总数量</div>
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
          <!-- 左侧色条 -->
          <div class="item-bar"></div>
          <div class="item-content">
            <div class="item-top">
              <span class="item-idx">#{{ idx + 1 }}</span>
              <span class="item-name">{{ getItemName(item) }}</span>
            </div>
            <div class="item-grid">
              <div class="grid-cell">
                <div class="gk">SKU编码</div>
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
              <span>金额小计</span>
              <span class="item-subtotal">￥{{ formatAmount(item.subtotal || (item.quantity * item.unitPrice)) }}</span>
            </div>
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

// ===== 单据类型配置（详情/审核适配器） =====
interface DetailConfig {
  key: string
  text: string
  color: string
  noField: string
  partyLabel: string
  partyField: string[]
  amountLabel: string
  getById: (id: string) => Promise<any>
  audit: (data: any) => Promise<any>
}

const typeConfigMap: Record<string, DetailConfig> = {
  purchase: {
    key: 'purchase', text: '采购', color: '#5a67d8',
    noField: 'purchaseNo', partyLabel: '供应商', partyField: ['supplierName'],
    amountLabel: '采购总金额',
    getById: (id) => purchaseApi.getById(id),
    audit: (data) => purchaseApi.audit(data)
  },
  stockin: {
    key: 'stockin', text: '入库', color: '#52c41a',
    noField: 'stockInNo', partyLabel: '供应商', partyField: ['supplierName'],
    amountLabel: '入库总金额',
    getById: (id) => stockInApi.getById(id),
    audit: (data) => stockInApi.audit(data)
  },
  stockout: {
    key: 'stockout', text: '出库', color: '#1890ff',
    noField: 'stockOutNo', partyLabel: '客户', partyField: ['customerName'],
    amountLabel: '出库总金额',
    getById: (id) => stockOutApi.getById(id),
    audit: (data) => stockOutApi.audit(data)
  },
  transfer: {
    key: 'transfer', text: '调拨', color: '#f5576c',
    noField: 'transferNo', partyLabel: '仓库', partyField: ['outWarehouseName', 'inWarehouseName'],
    amountLabel: '调拨总金额',
    getById: (id) => transferApi.getById(id),
    audit: (data) => transferApi.audit(data)
  },
  loss: {
    key: 'loss', text: '报损', color: '#ff4d4f',
    noField: 'lossNo', partyLabel: '报损商品', partyField: ['skuName'],
    amountLabel: '报损总金额',
    getById: (id) => lossApi.getById(id),
    audit: (data) => lossApi.audit(data)
  },
  sale: {
    key: 'sale', text: '销售', color: '#faad14',
    noField: 'saleNo', partyLabel: '客户', partyField: ['customerName'],
    amountLabel: '销售总金额',
    getById: (id) => saleApi.getById(id),
    audit: (data) => saleApi.audit(data)
  }
}

// 当前类型配置（默认采购，兜底）
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

// 基本信息（统一展示，按类型略作调整）
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
  // 调拨单额外信息
  if (currentType.value.key === 'transfer') {
    rows.splice(1, 0, {
      label: '调出仓库',
      value: order.value.outWarehouseName || '-'
    })
    rows.splice(2, 0, {
      label: '调入仓库',
      value: order.value.inWarehouseName || '-'
    })
  }
  return rows
})

// 状态文案
const statusText = computed(() => {
  const s = order.value?.status
  const map: Record<number, string> = {
    0: '草稿',
    1: '待审核',
    2: '已通过',
    3: '部分到货',
    4: '已完成',
    5: '已作废'
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

// 获取明细商品名（兼容多种字段）
function getItemName(item: any): string {
  return item.skuName || item.goodsName || item.spuName || item.skuCode || '-'
}

// ===== 加载详情 =====
onMounted(async () => {
  const id = route.params.id as string
  if (!id) {
    loading.value = false
    return
  }
  try {
    const res: any = await currentType.value.getById(id)
    const data = res.data || res
    // 兼容两种返回结构：{order, items} 或 flat
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
    await currentType.value.audit({
      id: route.params.id,
      pass: true
    })
    showSuccessToast('审核通过')
    setTimeout(() => router.replace('/mobile/approval'), 600)
  } catch (e: any) {
    showFailToast(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

function openReject() {
  rejectRemark.value = ''
  showRejectDialog.value = true
}

async function handleReject() {
  if (!rejectRemark.value?.trim()) {
    showToast('请填写驳回原因')
    return
  }
  submitting.value = true
  try {
    await currentType.value.audit({
      id: route.params.id,
      pass: false,
      remark: rejectRemark.value.trim()
    })
    showSuccessToast('已驳回')
    showRejectDialog.value = false
    setTimeout(() => router.replace('/mobile/approval'), 600)
  } catch (e: any) {
    showFailToast(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.back()
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 16)
}

function formatAmount(amount: number) {
  if (!amount) return '0.00'
  return Number(amount).toFixed(2)
}
</script>

<style scoped>
.m-detail {
  min-height: 100vh;
  background: #f4f5f9;
  padding-bottom: 90px;
}

/* ===== 沉浸式头部 ===== */
.hero {
  padding: 0 20px 70px;
  position: relative;
  color: #fff;
  overflow: hidden;
}

/* 三种状态渐变 */
.hero-default {
  background: linear-gradient(135deg, #667eea 0%, #5a67d8 50%, #764ba2 100%);
}

.hero-pass {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.hero-void {
  background: linear-gradient(135deg, #8c8c8c 0%, #595959 100%);
}

/* 多层渐变光斑 */
.hero-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(45px);
  pointer-events: none;
}

.orb-a {
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.2);
  top: -40px;
  right: -30px;
}

.orb-b {
  width: 160px;
  height: 160px;
  background: rgba(255, 255, 255, 0.15);
  bottom: 60px;
  left: -50px;
}

.orb-c {
  width: 120px;
  height: 120px;
  background: rgba(255, 255, 255, 0.12);
  top: 35%;
  right: 18%;
}

/* 底部波浪过渡 */
.hero::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 70px;
  background: #f4f5f9;
  border-radius: 35px 35px 0 0;
  z-index: 3;
}

.hero-status {
  height: 44px;
  position: relative;
  z-index: 2;
}

.hero-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  position: relative;
  z-index: 2;
}

.circle-btn {
  width: 38px;
  height: 38px;
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

.circle-btn:active {
  transform: scale(0.92);
}

.hero-type-tag {
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 16px;
  padding: 6px 14px;
}

.type-tag-text {
  font-size: 13px;
  font-weight: 700;
}

/* 加载中 */
.loading-inline {
  display: flex;
  justify-content: center;
  padding: 40px 0;
  position: relative;
  z-index: 2;
}

/* 状态 Pill */
.hero-status-row {
  margin-bottom: 16px;
  position: relative;
  z-index: 2;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(8px);
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

/* 金额大数字（渐变色文字 + 阴影） */
.hero-amount {
  margin-bottom: 18px;
  position: relative;
  z-index: 2;
}

.amount-label {
  font-size: 13px;
  opacity: 0.85;
  display: block;
  margin-bottom: 4px;
}

.amount-num {
  font-size: 42px;
  font-weight: 800;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #fff 0%, rgba(255, 255, 255, 0.7) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}

/* 关键信息横排（玻璃拟态） */
.hero-meta {
  display: flex;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(10px);
  border-radius: 14px;
  padding: 14px 0;
  margin-bottom: 14px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  position: relative;
  z-index: 2;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.08);
}

.meta-item {
  flex: 1;
  text-align: center;
  padding: 0 4px;
}

.meta-label {
  font-size: 12px;
  opacity: 0.75;
  margin-bottom: 4px;
}

.meta-value {
  font-size: 14px;
  font-weight: 700;
  max-width: 90px;
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
  font-size: 12px;
  opacity: 0.85;
  position: relative;
  z-index: 2;
}

.hero-bill-no {
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.18);
  padding: 4px 12px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

/* ===== 内容区 ===== */
.section {
  padding: 6px 0 0;
  position: relative;
  z-index: 4;
}

.section-title {
  padding: 14px 18px 8px;
  font-size: 15px;
  color: #1a1a2e;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-dot {
  width: 4px;
  height: 15px;
  border-radius: 2px;
  background: linear-gradient(180deg, #667eea, #764ba2);
}

.section-sub {
  font-size: 12px;
  color: #bbb;
  font-weight: normal;
  margin-left: auto;
}

/* 基本信息卡 */
.info-card {
  margin: 0 14px;
  background: #fff;
  border-radius: 16px;
  padding: 4px 18px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 14px 0;
  border-bottom: 1px solid #f7f7f7;
  font-size: 13px;
}

.info-row:last-child {
  border-bottom: none;
}

.info-k {
  color: #8c8c8c;
  flex-shrink: 0;
  margin-right: 16px;
}

.info-v {
  color: #1a1a2e;
  text-align: right;
  word-break: break-all;
  font-weight: 500;
}

.info-v.mono {
  color: #bbb;
}

/* 明细卡（现代卡片样式：左色条 + 序号 + 商品名 + grid） */
.item-card {
  margin: 0 14px 10px;
  background: #fff;
  border-radius: 16px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  transition: transform 0.15s;
}

.item-card:active {
  transform: scale(0.99);
}

.item-bar {
  width: 5px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
}

.item-content {
  flex: 1;
  padding: 14px 16px;
}

.item-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.item-idx {
  min-width: 28px;
  height: 24px;
  padding: 0 8px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.item-name {
  font-size: 15px;
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
  gap: 10px 16px;
  padding: 12px 0;
  border-top: 1px dashed #f0f0f0;
  border-bottom: 1px dashed #f0f0f0;
}

.grid-cell {
  font-size: 12px;
}

.gk {
  color: #bfbfbf;
  margin-bottom: 4px;
}

.gv {
  color: #1a1a2e;
  font-size: 13px;
  font-weight: 500;
}

.mono {
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-size: 12px;
  color: #5a67d8;
}

.item-total {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #8c8c8c;
}

.item-subtotal {
  font-size: 17px;
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
  gap: 12px;
  padding: 12px 16px calc(env(safe-area-inset-bottom) + 12px);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.08);
  z-index: 30;
}

.btn {
  flex: 1;
  height: 50px;
  border: none;
  border-radius: 25px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: transform 0.15s, box-shadow 0.2s;
  position: relative;
  overflow: hidden;
}

.btn:active {
  transform: scale(0.98);
}

.btn-reject {
  background: #fff;
  border: 1.5px solid #ff4d4f;
  color: #ff4d4f;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.15);
}

.btn-approve {
  background: linear-gradient(135deg, #52c41a, #389e0d);
  color: #fff;
  box-shadow: 0 6px 18px rgba(82, 196, 26, 0.35);
}

.btn-approve:disabled {
  opacity: 0.7;
}

/* 驳回弹窗 */
.reject-box {
  padding: 12px 16px;
}

.reject-textarea {
  width: 100%;
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 12px;
  font-size: 14px;
  outline: none;
  resize: none;
  box-sizing: border-box;
  font-family: inherit;
  transition: border-color 0.2s;
}

.reject-textarea:focus {
  border-color: #667eea;
}
</style>
