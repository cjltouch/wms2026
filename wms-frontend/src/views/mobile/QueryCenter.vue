<template>
  <div class="m-query">
    <!-- ===== 沉浸式渐变头部 ===== -->
    <div class="hero">
      <div class="hero-orb orb-a"></div>
      <div class="hero-orb orb-b"></div>
      <div class="hero-orb orb-c"></div>

      <div class="hero-status"></div>

      <div class="hero-head">
        <div class="hero-back" @click="goBack">
          <van-icon name="arrow-left" size="18" />
        </div>
        <div class="hero-title">
          <span class="greeting">快捷查询 👋</span>
          <span class="app-name">查询中心</span>
        </div>
        <div class="hero-avatar" @click="handleLogout">
          <van-icon name="user-o" size="18" />
        </div>
      </div>

      <!-- 搜索框（玻璃拟态） -->
      <div class="search-card">
        <van-search
          v-model="keyword"
          placeholder="单据号 / 供应商 / 客户 / 仓库"
          shape="round"
          :clearable="true"
          @search="onSearch"
          @clear="onClearKeyword"
          class="glass-search"
        />
      </div>

      <!-- 单据类型 Tab -->
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
        </div>
      </div>
    </div>

    <!-- 状态子 Tab -->
    <div class="sub-tabs">
      <div
        v-for="s in statusOptions"
        :key="s.value"
        class="sub-tab"
        :class="{ active: activeStatus === s.value }"
        @click="switchStatus(s.value)"
      >
        <span class="sub-text">{{ s.label }}</span>
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
          <div class="card-indicator" :style="{ background: getTypeColor(item.type) }"></div>
          <div class="card-body">
            <div class="card-head">
              <span class="type-tag" :style="typeTagStyle(item.type)">
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
              <span class="status-badge" :class="statusClass(item.status)">{{ item.statusText }}</span>
              <span class="time">{{ formatDate(item.time) }}</span>
              <span class="items-count">{{ item.qty || 0 }} 件 ›</span>
            </div>
          </div>
        </div>

        <van-empty
          v-if="finished && list.length === 0"
          description="暂无查询结果"
          image="search"
        />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import {
  purchaseApi, stockInApi, stockOutApi, transferApi, lossApi, saleApi
} from '@/api'
import { useUserStore } from '@/store/user'

defineOptions({ name: 'MobileQueryCenter' })

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

// 状态选项（-1 表示全部）
const statusOptions = [
  { value: -1, label: '全部' },
  { value: 0, label: '草稿' },
  { value: 1, label: '已提交' },
  { value: 2, label: '已审核' },
  { value: 5, label: '已作废' }
]

const statusTextMap: Record<number, string> = {
  0: '草稿',
  1: '已提交',
  2: '已审核',
  3: '部分到货',
  4: '已完成',
  5: '已作废'
}

function statusText(s: number): string {
  return statusTextMap[s] || `状态 ${s}`
}

function statusClass(s: number): string {
  if (s === 0) return 'draft'
  if (s === 1) return 'submitted'
  if (s === 2 || s === 3 || s === 4) return 'approved'
  if (s === 5) return 'void'
  return 'draft'
}

// ===== 列表状态 =====
const keyword = ref('')
const activeTab = ref<string>('all')
const activeStatus = ref<number>(-1)
const list = ref<any[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const pageSize = 20

function getTypeText(type: string): string {
  return billTypeMap[type]?.text || type
}

function getTypeColor(type: string): string {
  return billTypeMap[type]?.color || '#8c8c8c'
}

function typeTagStyle(type: string) {
  const c = getTypeColor(type)
  return {
    color: c,
    background: c + '1a',
    borderColor: c + '40'
  }
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
  let party = ''
  if (type === 'transfer') {
    party = [row.outWarehouseName, row.inWarehouseName].filter(Boolean).join(' → ')
  } else {
    for (const f of cfg.partyField) {
      if (row[f]) { party = row[f]; break }
    }
  }
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
    status: row.status,
    statusText: statusText(row.status)
  }
}

// ===== 拉取列表 =====
async function onLoad() {
  try {
    const params: any = {
      keyword: keyword.value,
      page: pageNum.value,
      size: pageSize
    }
    if (activeStatus.value !== -1) {
      params.status = activeStatus.value
    }

    if (activeTab.value === 'all') {
      // 全部类型：并发 6 个 API，合并按时间倒序
      const allItems: any[] = []
      const tasks = Object.values(billTypeMap).map(async (cfg) => {
        try {
          const res: any = await cfg.page(params)
          const rows = res.data?.rows || res.rows || []
          return rows.map((r: any) => normalizeBill(r, cfg.key)).filter(Boolean)
        } catch { return [] }
      })
      const results = await Promise.all(tasks)
      results.forEach(arr => allItems.push(...arr))
      allItems.sort((a, b) => (b.rawTime || '').localeCompare(a.rawTime || ''))
      list.value = allItems
      finished.value = true
    } else {
      const cfg = billTypeMap[activeTab.value]
      if (!cfg) { finished.value = true; return }
      const res: any = await cfg.page(params)
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
    showToast(e.message || '查询失败')
    finished.value = true
  } finally {
    loading.value = false
  }
}

function onRefresh() {
  pageNum.value = 1
  finished.value = false
  loading.value = true
  onLoad()
  refreshing.value = false
}

function onSearch() {
  list.value = []
  pageNum.value = 1
  finished.value = false
  loading.value = true
  onLoad()
}

function onClearKeyword() {
  keyword.value = ''
  onSearch()
}

function switchTab(key: string) {
  activeTab.value = key
  list.value = []
  pageNum.value = 1
  finished.value = false
  loading.value = true
  onLoad()
}

function switchStatus(val: number) {
  activeStatus.value = val
  list.value = []
  pageNum.value = 1
  finished.value = false
  loading.value = true
  onLoad()
}

function goDetail(item: any) {
  router.push(`/mobile/approval/${item.type}/${item.id}`)
}

function goBack() {
  router.back()
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
  const queryType = route.query.type as string
  if (queryType && billTypeMap[queryType]) {
    activeTab.value = queryType
  }
})
</script>

<style scoped>
.m-query {
  min-height: 100vh;
  background: #f4f5f9;
  display: flex;
  flex-direction: column;
}

/* ===== 头部渐变区 ===== */
.hero {
  background: linear-gradient(135deg, #1890ff 0%, #0050b3 50%, #003a8c 100%);
  padding: 0 20px 70px;
  position: relative;
  color: #fff;
  overflow: hidden;
}

.hero-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(45px);
  pointer-events: none;
}

.orb-a {
  width: 200px;
  height: 200px;
  background: rgba(255, 154, 200, 0.35);
  top: -40px;
  right: -30px;
}

.orb-b {
  width: 160px;
  height: 160px;
  background: rgba(129, 196, 253, 0.4);
  bottom: 80px;
  left: -50px;
}

.orb-c {
  width: 120px;
  height: 120px;
  background: rgba(255, 255, 255, 0.2);
  top: 30%;
  right: 18%;
}

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
  margin-bottom: 14px;
  position: relative;
  z-index: 2;
}

.hero-back {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
}

.hero-title {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-left: 12px;
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

/* 搜索框（玻璃拟态） */
.search-card {
  position: relative;
  z-index: 2;
  margin-bottom: 14px;
}

.glass-search {
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(14px);
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  overflow: hidden;
}

:deep(.glass-search .van-search__content) {
  background: transparent;
}

:deep(.glass-search .van-field__control) {
  color: #fff;
}

:deep(.glass-search .van-field__control::placeholder) {
  color: rgba(255, 255, 255, 0.6);
}

:deep(.glass-search .van-search) {
  background: transparent;
}

/* 单据类型 Tab */
.hero-tabs {
  position: relative;
  z-index: 2;
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
  border-radius: 20px;
  padding: 6px;
}

.hero-tabs::-webkit-scrollbar {
  display: none;
}

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
  transition: color 0.25s;
  position: relative;
  z-index: 1;
}

.hero-tab.active {
  color: #0050b3;
  font-weight: 700;
}

/* ===== 状态子 Tab ===== */
.sub-tabs {
  display: flex;
  background: #fff;
  padding: 10px 12px;
  gap: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  position: relative;
  z-index: 4;
  overflow-x: auto;
  scrollbar-width: none;
}

.sub-tabs::-webkit-scrollbar {
  display: none;
}

.sub-tab {
  flex-shrink: 0;
  text-align: center;
  padding: 7px 14px;
  font-size: 13px;
  color: #888;
  font-weight: 500;
  cursor: pointer;
  border-radius: 10px;
  background: #f7f7fa;
  transition: all 0.25s;
}

.sub-tab.active {
  background: linear-gradient(135deg, #1890ff, #0050b3);
  color: #fff;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
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
  gap: 8px;
  padding-top: 8px;
  border-top: 1px dashed #f0f0f0;
}

.status-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 600;
  flex-shrink: 0;
}

.status-badge.draft {
  color: #8c8c8c;
  background: #f5f5f5;
}

.status-badge.submitted {
  color: #faad14;
  background: #fff7e6;
}

.status-badge.approved {
  color: #52c41a;
  background: #f6ffed;
}

.status-badge.void {
  color: #ff4d4f;
  background: #fff2f0;
}

.time {
  font-size: 12px;
  color: #bbb;
  flex: 1;
  text-align: right;
}

.items-count {
  font-size: 12px;
  color: #1890ff;
  font-weight: 600;
  flex-shrink: 0;
}

:deep(.van-empty) {
  padding: 60px 0;
}
</style>
