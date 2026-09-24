<template>
  <div class="m-list">
    <!-- ===== 沉浸式渐变头部 ===== -->
    <div class="hero">
      <!-- 多层渐变光斑 -->
      <div class="hero-orb orb-a"></div>
      <div class="hero-orb orb-b"></div>
      <div class="hero-orb orb-c"></div>

      <!-- 状态栏占位 -->
      <div class="hero-status"></div>

      <!-- 标题行 -->
      <div class="hero-head">
        <div class="hero-title">
          <span class="greeting">你好 👋</span>
          <span class="app-name">审批中心</span>
        </div>
        <div class="hero-avatar" @click="handleLogout">
          <van-icon name="user-o" size="18" />
        </div>
      </div>

      <!-- 待审批大数字卡（玻璃拟态） -->
      <div class="stat-card" @click="switchTab('all')">
        <div class="stat-ring">
          <van-icon name="clock-o" size="22" color="#fff" />
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ pendingTotal }}</div>
          <div class="stat-label">待我审批</div>
        </div>
        <div class="stat-tag" v-if="pendingTotal > 0">
          <van-icon name="fire" size="12" color="#fff" />
        </div>
      </div>

      <!-- 单据类型 Tab（玻璃拟态横向滚动 + 滑块） -->
      <div class="hero-tabs">
        <div class="tab-slider" :style="sliderStyle"></div>
        <div
          v-for="t in billTypes"
          :key="t.key"
          class="hero-tab"
          :class="{ active: activeTab === t.key }"
          :ref="(el) => setTabRef(t.key, el as HTMLElement)"
          @click="switchTab(t.key)"
        >
          <span>{{ t.text }}</span>
          <span v-if="getPendingByType(t.key) > 0 && t.key !== 'all'" class="dot" />
        </div>
      </div>
    </div>

    <!-- 状态子 Tab（待审批 / 已审批） -->
    <div class="sub-tabs">
      <div
        class="sub-tab"
        :class="{ active: statusTab === 0 }"
        @click="switchStatus(0)"
      >
        <span class="sub-text">待审批</span>
        <span class="sub-count" v-if="statusTab === 0">{{ list.length }}</span>
      </div>
      <div
        class="sub-tab"
        :class="{ active: statusTab === 1 }"
        @click="switchStatus(1)"
      >
        <span class="sub-text">已审批</span>
        <span class="sub-count" v-if="statusTab === 1">{{ list.length }}</span>
      </div>
    </div>

    <!-- 列表区 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh" class="pull-wrap">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="—— 没有更多了 ——"
        @load="onLoad"
      >
        <div
          v-for="(item, idx) in list"
          :key="`${item.type}-${item.id}-${idx}`"
          class="order-card"
          @click="goDetail(item)"
        >
          <!-- 左侧状态色渐变条 -->
          <div class="card-indicator" :class="statusTab === 0 ? 'wait' : 'done'"></div>
          <div class="card-body">
            <div class="card-head">
              <span class="type-tag" :style="{ color: getTypeColor(item.type), background: getTypeColor(item.type) + '1a', borderColor: getTypeColor(item.type) + '40' }">
                {{ getTypeText(item.type) }}
              </span>
              <span class="order-no">{{ item.no }}</span>
              <span v-if="item.amount" class="money">￥{{ formatAmount(item.amount) }}</span>
            </div>
            <div class="card-info">
              <div class="info-line">
                <van-icon name="shop-o" />
                <span>{{ item.party || '未指定' }}</span>
              </div>
              <div class="info-line">
                <van-icon name="contact" />
                <span>{{ item.createName || '-' }}</span>
              </div>
            </div>
            <div class="card-foot">
              <span class="time">{{ formatDate(item.time) }}</span>
              <span class="items-count">{{ item.qty || 0 }} 件 ›</span>
            </div>
          </div>
        </div>

        <van-empty
          v-if="finished && list.length === 0"
          :description="statusTab === 0 ? '暂无待审批单据 🎉' : '暂无已审批记录'"
          image="search"
        />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import {
  purchaseApi, stockInApi, stockOutApi, transferApi, lossApi, saleApi
} from '@/api'
import { useUserStore } from '@/store/user'

defineOptions({ name: 'MobileApprovalList' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// ===== 单据类型配置 =====
interface BillType {
  key: string
  text: string
  color: string
  noField: string
  partyField: string[]
  amountField: string
  qtyField: string
  createField: string
  timeField: string[]
  idField: string
  page: (params: any) => Promise<any>
}

const billTypeMap: Record<string, BillType> = {
  purchase: {
    key: 'purchase', text: '采购', color: '#5a67d8',
    noField: 'purchaseNo', partyField: ['supplierName'],
    amountField: 'totalAmount', qtyField: 'totalQty',
    createField: 'createName', timeField: ['auditTime', 'createTime'],
    idField: 'purchaseId', page: (p) => purchaseApi.page(p)
  },
  stockin: {
    key: 'stockin', text: '入库', color: '#52c41a',
    noField: 'stockInNo', partyField: ['supplierName'],
    amountField: 'totalAmount', qtyField: 'totalQty',
    createField: 'createName', timeField: ['auditTime', 'createTime'],
    idField: 'stockInId', page: (p) => stockInApi.page(p)
  },
  stockout: {
    key: 'stockout', text: '出库', color: '#1890ff',
    noField: 'stockOutNo', partyField: ['customerName'],
    amountField: 'totalAmount', qtyField: 'totalQty',
    createField: 'createName', timeField: ['auditTime', 'createTime'],
    idField: 'stockOutId', page: (p) => stockOutApi.page(p)
  },
  transfer: {
    key: 'transfer', text: '调拨', color: '#f5576c',
    noField: 'transferNo', partyField: ['outWarehouseName', 'inWarehouseName'],
    amountField: 'totalAmount', qtyField: 'totalQty',
    createField: 'createName', timeField: ['auditTime', 'createTime'],
    idField: 'transferId', page: (p) => transferApi.page(p)
  },
  loss: {
    key: 'loss', text: '报损', color: '#ff4d4f',
    noField: 'lossNo', partyField: ['skuName'],
    amountField: 'totalAmount', qtyField: 'totalQty',
    createField: 'createName', timeField: ['auditTime', 'createTime'],
    idField: 'lossId', page: (p) => lossApi.page(p)
  },
  sale: {
    key: 'sale', text: '销售', color: '#faad14',
    noField: 'saleNo', partyField: ['customerName'],
    amountField: 'totalAmount', qtyField: 'totalQty',
    createField: 'createName', timeField: ['auditTime', 'createTime'],
    idField: 'saleId', page: (p) => saleApi.page(p)
  }
}

const billTypes = [
  { key: 'all', text: '全部' },
  { key: 'purchase', text: '采购' },
  { key: 'stockin', text: '入库' },
  { key: 'stockout', text: '出库' },
  { key: 'transfer', text: '调拨' },
  { key: 'loss', text: '报损' },
  { key: 'sale', text: '销售' }
]

// ===== 列表状态 =====
const activeTab = ref<string>('all')  // 单据类型 Tab
const statusTab = ref<number>(0)      // 0=待审批(status=1), 1=已审批(status=2)
const list = ref<any[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const pageSize = 20

// 各类型待审批数量
const pendingByType = ref<Record<string, number>>({})
const pendingTotal = computed(() => {
  return Object.values(pendingByType.value).reduce((a, b) => a + b, 0)
})

function getPendingByType(key: string): number {
  return pendingByType.value[key] || 0
}

function getTypeText(type: string): string {
  return billTypeMap[type]?.text || type
}

function getTypeColor(type: string): string {
  return billTypeMap[type]?.color || '#8c8c8c'
}

// ===== Tab 滑块动画 =====
const tabRefs = ref<Record<string, HTMLElement | null>>({})
const sliderStyle = ref({ width: '0px', transform: 'translateX(0px)' })

function setTabRef(key: string, el: HTMLElement | null) {
  tabRefs.value[key] = el
}

async function updateSlider() {
  await nextTick()
  const el = tabRefs.value[activeTab.value]
  if (el) {
    sliderStyle.value = {
      width: el.offsetWidth + 'px',
      transform: `translateX(${el.offsetLeft}px)`
    }
  }
}

watch(activeTab, () => updateSlider())
onMounted(() => {
  setTimeout(updateSlider, 100)
})

// 把后端 row 标准化为统一结构
function normalizeBill(row: any, type: string): any {
  const cfg = billTypeMap[type]
  if (!cfg) return null
  // 对方名称
  let party = ''
  if (type === 'transfer') {
    party = [row.outWarehouseName, row.inWarehouseName].filter(Boolean).join(' → ')
  } else {
    for (const f of cfg.partyField) {
      if (row[f]) { party = row[f]; break }
    }
  }
  // 时间
  let time = ''
  for (const f of cfg.timeField) {
    if (row[f]) { time = row[f]; break }
  }
  return {
    type,
    id: row[cfg.idField] || row.id,
    no: row[cfg.noField] || '-',
    party,
    amount: row[cfg.amountField] || 0,
    qty: row[cfg.qtyField] || 0,
    createName: row[cfg.createField] || row.createName || '-',
    time,
    rawTime: time,
    status: row.status
  }
}

// ===== 拉取列表 =====
async function onLoad() {
  const status = statusTab.value === 0 ? 1 : 2
  try {
    if (activeTab.value === 'all') {
      // 全部类型：并发 6 个 API，合并按时间倒序
      const allItems: any[] = []
      const tasks = Object.values(billTypeMap).map(async (cfg) => {
        try {
          const res: any = await cfg.page({ status, pageNum: pageNum.value, pageSize })
          const rows = res.data?.rows || res.rows || []
          return rows.map((r: any) => normalizeBill(r, cfg.key)).filter(Boolean)
        } catch { return [] }
      })
      const results = await Promise.all(tasks)
      results.forEach(arr => allItems.push(...arr))
      allItems.sort((a, b) => (b.rawTime || '').localeCompare(a.rawTime || ''))
      // 全部模式只支持一页，避免分页复杂度
      list.value = allItems
      finished.value = true
    } else {
      // 单类型：标准后端分页
      const cfg = billTypeMap[activeTab.value]
      if (!cfg) { finished.value = true; return }
      const res: any = await cfg.page({ status, pageNum: pageNum.value, pageSize })
      const rows = res.data?.rows || res.rows || []
      const total = res.data?.total || res.total || 0
      const items = rows.map((r: any) => normalizeBill(r, cfg.key)).filter(Boolean)
      if (pageNum.value === 1) {
        list.value = items
      } else {
        list.value.push(...items)
      }
      if (list.value.length >= total) {
        finished.value = true
      } else {
        pageNum.value++
      }
    }
  } catch (e: any) {
    showToast(e.message || '加载失败')
    finished.value = true
  } finally {
    loading.value = false
  }
}

function onRefresh() {
  fetchPendingCount()
  pageNum.value = 1
  finished.value = false
  loading.value = true
  onLoad()
  refreshing.value = false
}

function switchTab(key: string) {
  activeTab.value = key
  // 同步到 URL query
  router.replace({ path: '/mobile/approval', query: key === 'all' ? {} : { type: key } })
  list.value = []
  pageNum.value = 1
  finished.value = false
  loading.value = true
  onLoad()
}

function switchStatus(idx: number) {
  statusTab.value = idx
  list.value = []
  pageNum.value = 1
  finished.value = false
  loading.value = true
  onLoad()
}

function goDetail(item: any) {
  router.push(`/mobile/approval/${item.type}/${item.id}`)
}

// ===== 待审批数量 =====
async function fetchPendingCount() {
  const entries = Object.entries(billTypeMap)
  const results = await Promise.allSettled(
    entries.map(async ([key, cfg]) => {
      const res: any = await cfg.page({ status: 1, pageNum: 1, pageSize: 1 })
      const total = res.data?.total ?? res.total ?? 0
      return [key, total] as [string, number]
    })
  )
  const map: Record<string, number> = {}
  results.forEach((r, i) => {
    if (r.status === 'fulfilled') {
      map[entries[i][0]] = r.value[1]
    }
  })
  pendingByType.value = map
}

function handleLogout() {
  showConfirmDialog({ title: '确认退出登录？' })
    .then(() => {
      userStore.resetState()
      router.replace('/mobile/login')
    })
    .catch(() => {})
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 16)
}

function formatAmount(amount: number) {
  if (!amount) return '0.00'
  return Number(amount).toFixed(2)
}

onMounted(() => {
  // 从 URL query 读取 type
  const queryType = route.query.type as string
  if (queryType && billTypeMap[queryType]) {
    activeTab.value = queryType
  }
  fetchPendingCount()
})
</script>

<style scoped>
.m-list {
  min-height: 100vh;
  background: #f4f5f9;
  display: flex;
  flex-direction: column;
}

/* ===== 头部渐变区 ===== */
.hero {
  background: linear-gradient(135deg, #667eea 0%, #5a67d8 50%, #764ba2 100%);
  padding: 0 20px 70px;
  position: relative;
  color: #fff;
  overflow: hidden;
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
  background: rgba(255, 154, 200, 0.4);
  top: -40px;
  right: -30px;
}

.orb-b {
  width: 160px;
  height: 160px;
  background: rgba(129, 196, 253, 0.45);
  bottom: 80px;
  left: -50px;
}

.orb-c {
  width: 120px;
  height: 120px;
  background: rgba(255, 255, 255, 0.25);
  top: 30%;
  right: 18%;
}

/* 底部波浪过渡 */
.hero::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 60px;
  background: #f4f5f9;
  border-radius: 30px 30px 0 0;
  z-index: 3;
}

.hero-status {
  height: 44px;
  position: relative;
  z-index: 2;
}

.hero-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
  position: relative;
  z-index: 2;
}

.hero-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.greeting {
  font-size: 13px;
  opacity: 0.85;
}

.app-name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.hero-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.3);
  flex-shrink: 0;
}

/* 待审批大数字卡（玻璃拟态） */
.stat-card {
  position: relative;
  z-index: 2;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-radius: 18px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 8px 28px rgba(45, 35, 110, 0.18);
  margin-bottom: 16px;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.3);
  transition: transform 0.15s;
}

.stat-card:active {
  transform: scale(0.98);
}

.stat-ring {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-num {
  font-size: 30px;
  font-weight: 800;
  color: #fff;
  line-height: 1.1;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.stat-label {
  font-size: 12px;
  opacity: 0.85;
  margin-top: 2px;
}

.stat-tag {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff9a56, #ff6a00);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(255, 106, 0, 0.4);
  flex-shrink: 0;
}

/* 单据类型 Tab（玻璃拟态 + 滑块动画） */
.hero-tabs {
  position: relative;
  z-index: 2;
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
  padding: 4px 0;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
  border-radius: 20px;
  padding: 6px;
}

.hero-tabs::-webkit-scrollbar {
  display: none;
}

/* 滑块 */
.tab-slider {
  position: absolute;
  top: 6px;
  left: 0;
  height: calc(100% - 12px);
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 0;
}

.hero-tab {
  flex-shrink: 0;
  padding: 7px 14px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color 0.25s;
  position: relative;
  z-index: 1;
}

.hero-tab.active {
  color: #5a67d8;
  font-weight: 700;
}

.dot {
  width: 6px;
  height: 6px;
  background: #ff4d4f;
  border-radius: 50%;
  box-shadow: 0 0 6px #ff4d4f;
}

.hero-tab.active .dot {
  background: #ff4d4f;
}

/* ===== 状态子 Tab ===== */
.sub-tabs {
  display: flex;
  background: #fff;
  padding: 10px 16px;
  gap: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  position: relative;
  z-index: 4;
}

.sub-tab {
  flex: 1;
  text-align: center;
  padding: 9px 0;
  font-size: 14px;
  color: #888;
  font-weight: 500;
  cursor: pointer;
  border-radius: 12px;
  background: #f7f7fa;
  transition: all 0.25s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.sub-tab.active {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.sub-count {
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.3);
  color: inherit;
}

.sub-tab:not(.active) .sub-count {
  background: #e8e8f0;
  color: #888;
}

/* ===== 列表区 ===== */
.pull-wrap {
  flex: 1;
  overflow-y: auto;
  padding-top: 10px;
}

.order-card {
  background: #fff;
  margin: 0 14px 12px;
  border-radius: 16px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.2s;
  position: relative;
}

.order-card:active {
  transform: scale(0.985);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}

.card-indicator {
  width: 5px;
  flex-shrink: 0;
  position: relative;
}

.card-indicator::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 0 4px 4px 0;
}

.card-indicator.wait::after {
  background: linear-gradient(180deg, #faad14 0%, #ff7a45 100%);
}

.card-indicator.done::after {
  background: linear-gradient(180deg, #52c41a 0%, #389e0d 100%);
}

.card-body {
  flex: 1;
  padding: 14px 16px;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.type-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 600;
  flex-shrink: 0;
  border: 1px solid transparent;
}

.order-no {
  flex: 1;
  font-size: 14px;
  font-weight: 700;
  color: #1a1a2e;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.money {
  font-size: 17px;
  font-weight: 800;
  background: linear-gradient(135deg, #ff4d4f, #ff7a45);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  flex-shrink: 0;
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-bottom: 10px;
}

.info-line {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666;
}

.info-line .van-icon {
  color: #bbb;
  font-size: 13px;
}

.card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px dashed #f0f0f0;
}

.time {
  font-size: 12px;
  color: #bbb;
}

.items-count {
  font-size: 12px;
  color: #667eea;
  font-weight: 600;
}

:deep(.van-empty) {
  padding: 60px 0;
}
</style>
