<template>
  <div class="m-detail">
    <!-- ===== 沉浸式头部 ===== -->
    <div class="hero" :class="heroStatusClass">
      <!-- 状态栏占位 -->
      <div class="hero-status"></div>

      <!-- 操作按钮行 -->
      <div class="hero-bar">
        <div class="circle-btn" @click="goBack">
          <van-icon name="arrow-left" size="18" />
        </div>
        <div class="circle-btn ghost" @click="handleLogout" v-if="order?.status === 2">
          <van-icon name="share-o" size="16" />
        </div>
      </div>

      <!-- 顶部加载 -->
      <div v-if="loading" class="loading-inline">
        <van-loading type="spinner" color="#fff" size="24" />
      </div>

      <!-- 头部摘要（数据加载完才显示） -->
      <template v-else-if="order">
        <!-- 状态 Pill + 单号 -->
        <div class="hero-status-row">
          <span class="status-pill">
            <van-icon :name="statusIcon" size="14" />
            {{ statusText }}
          </span>
        </div>

        <!-- 金额大数字 -->
        <div class="hero-amount">
          <span class="amount-label">采购总金额</span>
          <span class="amount-num">￥{{ formatAmount(order.totalAmount) }}</span>
        </div>

        <!-- 关键信息横排 -->
        <div class="hero-meta">
          <div class="meta-item">
            <div class="meta-label">供应商</div>
            <div class="meta-value">{{ order.supplierName || '-' }}</div>
          </div>
          <div class="meta-divider" />
          <div class="meta-item">
            <div class="meta-label">入库仓库</div>
            <div class="meta-value">{{ order.warehouseName || '-' }}</div>
          </div>
          <div class="meta-divider" />
          <div class="meta-item">
            <div class="meta-label">商品种类</div>
            <div class="meta-value">{{ items.length }}</div>
          </div>
        </div>

        <!-- 单号 + 时间 -->
        <div class="hero-sub">
          <span class="hero-purchase-no">{{ order.purchaseNo }}</span>
          <span class="hero-time">{{ formatDate(order.createTime) }}</span>
        </div>
      </template>
    </div>

    <!-- ===== 内容区（在波浪下方） ===== -->
    <template v-if="!loading && order">
      <!-- 基本信息 -->
      <div class="section">
        <div class="section-title">
          <span class="title-dot"></span>基本信息
        </div>
        <div class="info-card">
          <div class="info-row">
            <span class="info-k">采购员</span>
            <span class="info-v">{{ order.purchaserName || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-k">创建人</span>
            <span class="info-v">{{ order.createName || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-k">总数量</span>
            <span class="info-v">{{ order.totalQty || 0 }}</span>
          </div>
          <div class="info-row">
            <span class="info-k">备注</span>
            <span class="info-v" :class="{ mono: !order.remark }">{{ order.remark || '—' }}</span>
          </div>
          <!-- 已审批的显示审核信息 -->
          <template v-if="order.auditBy">
            <div class="info-row">
              <span class="info-k">审核人</span>
              <span class="info-v">{{ order.auditName || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="info-k">审核时间</span>
              <span class="info-v">{{ formatDate(order.auditTime) }}</span>
            </div>
          </template>
        </div>
      </div>

      <!-- 采购明细 -->
      <div class="section">
        <div class="section-title">
          <span class="title-dot"></span>采购明细
          <span class="section-sub">共 {{ items.length }} 项</span>
        </div>
        <div
          v-for="(item, idx) in items"
          :key="idx"
          class="item-card"
        >
          <div class="item-top">
            <span class="item-idx">#{{ idx + 1 }}</span>
            <span class="item-name">{{ item.skuName || item.skuCode }}</span>
          </div>
          <div class="item-grid">
            <div class="grid-cell">
              <div class="gk">SKU编码</div>
              <div class="gv mono">{{ item.skuCode || '-' }}</div>
            </div>
            <div class="grid-cell">
              <div class="gk">规格</div>
              <div class="gv">{{ item.specText || '-' }}</div>
            </div>
            <div class="grid-cell">
              <div class="gk">数量</div>
              <div class="gv">{{ item.quantity }}</div>
            </div>
            <div class="grid-cell">
              <div class="gk">单价</div>
              <div class="gv">￥{{ formatAmount(item.purchasePrice) }}</div>
            </div>
          </div>
          <div class="item-total">
            <span>金额小计</span>
            <span class="item-subtotal">￥{{ formatAmount(item.subtotal) }}</span>
          </div>
        </div>
      </div>
    </template>

    <van-empty v-else-if="!loading" description="采购单不存在" />

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
import { purchaseApi } from '@/api'

defineOptions({ name: 'MobileApprovalDetail' })

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const order = ref<any>(null)
const items = ref<any[]>([])
const showRejectDialog = ref(false)
const rejectRemark = ref('')
const submitting = ref(false)

onMounted(async () => {
  const id = route.params.id as string
  try {
    const res: any = await purchaseApi.getById(id)
    const data = res.data || res
    order.value = data.order || data
    items.value = data.items || []
  } catch (e: any) {
    showFailToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
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

async function handleApprove() {
  submitting.value = true
  try {
    await purchaseApi.audit({ id: route.params.id, pass: true })
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
    await purchaseApi.audit({
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

function handleLogout() {
  router.replace('/mobile/approval')
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
  padding-bottom: 80px;
}

/* ===== 沉浸式头部 ===== */
.hero {
  padding: 0 20px 70px;
  position: relative;
  color: #fff;
}

/* 三种状态渐变 */
.hero-default {
  background: linear-gradient(135deg, #667eea 0%, #5a67d8 50%, #764ba2 100%);
}

.hero-pass {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.hero-void {
  background: linear-gradient(135deg, #666 0%, #999 100%);
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
}

.hero-status {
  height: 44px;
}

.hero-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.circle-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.25);
  color: #fff;
}

.circle-btn.ghost {
  background: transparent;
  border-color: rgba(255, 255, 255, 0.3);
}

/* 加载中 */
.loading-inline {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

/* 状态 Pill */
.hero-status-row {
  margin-bottom: 16px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(6px);
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid rgba(255, 255, 255, 0.25);
}

/* 金额大数字 */
.hero-amount {
  margin-bottom: 18px;
}

.amount-label {
  font-size: 13px;
  opacity: 0.85;
  display: block;
  margin-bottom: 4px;
}

.amount-num {
  font-size: 40px;
  font-weight: 800;
  letter-spacing: -0.5px;
}

/* 关键信息横排 */
.hero-meta {
  display: flex;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(6px);
  border-radius: 12px;
  padding: 12px 0;
  margin-bottom: 14px;
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.meta-item {
  flex: 1;
  text-align: center;
}

.meta-label {
  font-size: 12px;
  opacity: 0.75;
  margin-bottom: 2px;
}

.meta-value {
  font-size: 14px;
  font-weight: 600;
  max-width: 90px;
  margin: 0 auto;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-divider {
  width: 1px;
  background: rgba(255, 255, 255, 0.2);
}

/* 单号 + 时间 */
.hero-sub {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  opacity: 0.8;
}

.hero-purchase-no {
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.15);
  padding: 3px 10px;
  border-radius: 10px;
}

/* ===== 内容区 ===== */
.section {
  padding: 6px 0 0;
}

.section-title {
  padding: 14px 18px 8px;
  font-size: 14px;
  color: #666;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-dot {
  width: 4px;
  height: 14px;
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
  border-radius: 14px;
  padding: 4px 18px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 13px 0;
  border-bottom: 1px solid #f7f7f7;
  font-size: 13px;
}

.info-row:last-child {
  border-bottom: none;
}

.info-k {
  color: #999;
  flex-shrink: 0;
  margin-right: 16px;
}

.info-v {
  color: #333;
  text-align: right;
  word-break: break-all;
}

.info-v.mono {
  color: #bbb;
}

/* 明细卡 */
.item-card {
  margin: 0 14px 10px;
  background: #fff;
  border-radius: 14px;
  padding: 14px 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.item-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.item-idx {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.item-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
}

.item-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 16px;
  padding: 10px 0;
  border-top: 1px dashed #f0f0f0;
  border-bottom: 1px dashed #f0f0f0;
}

.grid-cell {
  font-size: 12px;
}

.gk {
  color: #bbb;
  margin-bottom: 3px;
}

.gv {
  color: #333;
  font-size: 13px;
}

.mono {
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-size: 12px;
  color: #667eea;
}

.item-total {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #999;
}

.item-subtotal {
  color: #ff4d4f;
  font-size: 16px;
  font-weight: 700;
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
  background: #fff;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.08);
  z-index: 30;
}

.btn {
  flex: 1;
  height: 48px;
  border: none;
  border-radius: 24px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: transform 0.15s, opacity 0.15s;
}

.btn:active {
  transform: scale(0.98);
}

.btn-reject {
  background: #fff;
  border: 1px solid #ff4d4f;
  color: #ff4d4f;
}

.btn-approve {
  background: linear-gradient(135deg, #52c41a, #389e0d);
  color: #fff;
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
}

.reject-textarea:focus {
  border-color: #667eea;
}
</style>
