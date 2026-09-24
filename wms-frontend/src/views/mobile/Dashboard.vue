<template>
  <div class="m-dashboard">
    <!-- ===== 沉浸式渐变头部 ===== -->
    <div class="hero">
      <!-- 状态栏占位 -->
      <div class="hero-status"></div>

      <!-- 顶部标题行 -->
      <div class="hero-head">
        <div class="hero-title">
          <span class="greeting">{{ greeting }} 👋</span>
          <span class="user-name">{{ displayName }}</span>
          <span class="user-role">{{ roleText }}</span>
        </div>
        <div class="hero-avatar" @click="handleLogout">
          <van-icon name="user-o" size="18" />
        </div>
      </div>

      <!-- 待办统计卡（白底悬浮） -->
      <div class="todo-card">
        <div class="todo-head">
          <div class="todo-title">
            <van-icon name="clock-o" size="16" color="#5a67d8" />
            <span>待我处理</span>
          </div>
          <span class="todo-total">{{ todoTotal }} 项</span>
        </div>
        <div class="todo-grid">
          <div class="todo-cell" @click="goApproval('purchase')">
            <div class="cell-num" :class="{ 'num-warn': todo.approval > 0 }">{{ todo.approval }}</div>
            <div class="cell-label">待审批</div>
          </div>
          <div class="todo-cell" @click="goWork('stockin')">
            <div class="cell-num" :class="{ 'num-warn': todo.stockin > 0 }">{{ todo.stockin }}</div>
            <div class="cell-label">待上架</div>
          </div>
          <div class="todo-cell" @click="goWork('stockout')">
            <div class="cell-num" :class="{ 'num-warn': todo.stockout > 0 }">{{ todo.stockout }}</div>
            <div class="cell-label">待拣货</div>
          </div>
          <div class="todo-cell" @click="goWork('check')">
            <div class="cell-num" :class="{ 'num-warn': todo.check > 0 }">{{ todo.check }}</div>
            <div class="cell-label">待盘点</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 快捷入口九宫格 ===== -->
    <div class="section">
      <div class="section-title">
        <span class="title-dot"></span>快捷入口
      </div>
      <div class="quick-grid">
        <div
          v-for="it in quickEntries"
          :key="it.key"
          class="quick-cell"
          @click="goQuick(it)"
        >
          <div class="quick-icon" :style="{ background: it.bg }">
            <van-icon :name="it.icon" size="22" color="#fff" />
          </div>
          <div class="quick-text">{{ it.text }}</div>
        </div>
      </div>
    </div>

    <!-- ===== 今日动态 ===== -->
    <div class="section">
      <div class="section-title">
        <span class="title-dot"></span>今日动态
        <span class="section-sub">最近 10 条</span>
      </div>
      <div class="feed-list">
        <div
          v-for="(it, idx) in feedList"
          :key="idx"
          class="feed-item"
          @click="goFeedDetail(it)"
        >
          <div class="feed-dot" :style="{ background: it.color }"></div>
          <div class="feed-body">
            <div class="feed-head">
              <span class="feed-type-tag" :style="{ color: it.color, background: it.color + '1a' }">{{ it.typeText }}</span>
              <span class="feed-no">{{ it.no }}</span>
              <span class="feed-status" :style="{ color: it.color }">{{ it.statusText }}</span>
            </div>
            <div class="feed-meta">
              <span class="feed-party">{{ it.party || '-' }}</span>
              <span class="feed-time">{{ it.time }}</span>
            </div>
          </div>
          <van-icon name="arrow" size="12" color="#bbb" />
        </div>
        <van-empty
          v-if="feedList.length === 0 && !feedLoading"
          description="今日暂无动态"
          image="search"
        />
      </div>
    </div>

    <!-- 底部退出按钮 -->
    <div class="footer-actions">
      <button class="btn-logout" @click="handleLogout">
        <van-icon name="revoke" size="14" />
        <span>退出登录</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import {
  purchaseApi, stockInApi, stockOutApi, checkApi,
  transferApi, saleApi, lossApi
} from '@/api'
import { useUserStore } from '@/store/user'

defineOptions({ name: 'MobileDashboard' })

const router = useRouter()
const userStore = useUserStore()

// ===== 问候语 =====
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 11) return '早上好'
  if (h < 13) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

// 用户展示名：realName → username → 默认
const displayName = computed(() => userStore.realName || userStore.username || 'WMS 用户')

// 角色文案
const roleText = computed(() => {
  const roles = userStore.roles || []
  if (roles.includes('admin')) return '管理员 · 全仓'
  if (roles.includes('manager') || roles.includes('主管')) return '主管 · 审批中心'
  if (roles.includes('purchaser') || roles.includes('采购')) return '采购员'
  if (roles.includes('warehouse') || roles.includes('仓管')) return '仓管员 · 上海仓'
  if (roles.length) return roles[0]
  return 'WMS 用户'
})

// ===== 待办统计 =====
const todo = reactive({
  approval: 0,  // 待审批（采购单 status=1）
  stockin: 0,    // 待上架（入库单 status=2 已审待上架）
  stockout: 0,  // 待拣货（出库单 status=2 已审待拣货）
  check: 0      // 待盘点（盘点单 status=2 待执行）
})
const todoTotal = computed(() => todo.approval + todo.stockin + todo.stockout + todo.check)

async function fetchTodos() {
  // 并发调用 4 个分页接口，只取 total
  const tasks: { key: string; fn: () => Promise<any> }[] = [
    { key: 'approval', fn: () => purchaseApi.page({ status: 1, pageNum: 1, pageSize: 1 }) },
    { key: 'stockin', fn: () => stockInApi.page({ status: 2, pageNum: 1, pageSize: 1 }) },
    { key: 'stockout', fn: () => stockOutApi.page({ status: 2, pageNum: 1, pageSize: 1 }) },
    { key: 'check', fn: () => checkApi.page({ status: 2, pageNum: 1, pageSize: 1 }) }
  ]
  const results = await Promise.allSettled(tasks.map(t => t.fn()))
  results.forEach((r, i) => {
    if (r.status !== 'fulfilled') return
    const res: any = r.value
    const total = res.data?.total ?? res.total ?? 0
    todo[tasks[i].key as keyof typeof todo] = total
  })
}

// ===== 快捷入口九宫格 =====
const quickEntries = [
  { key: 'stockin', icon: 'after-sale', text: '入库上架', bg: 'linear-gradient(135deg,#11998e,#38ef7d)' },
  { key: 'stockout', icon: 'logistics', text: '出库拣货', bg: 'linear-gradient(135deg,#667eea,#764ba2)' },
  { key: 'transfer', icon: 'exchange', text: '调拨搬运', bg: 'linear-gradient(135deg,#f093fb,#f5576c)' },
  { key: 'check', icon: 'records', text: '盘点作业', bg: 'linear-gradient(135deg,#faad14,#ff7a45)' },
  { key: 'loss', icon: 'warning-o', text: '报损录入', bg: 'linear-gradient(135deg,#ff4d4f,#cf1322)' },
  { key: 'approval', icon: 'passed', text: '审批中心', bg: 'linear-gradient(135deg,#5a67d8,#764ba2)' },
  { key: 'query', icon: 'search', text: '查询中心', bg: 'linear-gradient(135deg,#1890ff,#0050b3)' },
  { key: 'inventory', icon: 'points', text: '库存查询', bg: 'linear-gradient(135deg,#36d1dc,#5b86e5)' },
  { key: 'office', icon: 'notes-o', text: '用品登记', bg: 'linear-gradient(135deg,#834d9b,#d04ed6)' }
] as const

function goQuick(it: { key: string; text: string }) {
  switch (it.key) {
    case 'approval':
      router.push('/mobile/approval')
      break
    case 'stockin':
    case 'stockout':
    case 'transfer':
    case 'check':
    case 'loss':
      // 作业页待 Phase 3 实现，临时提示
      showToast(`${it.text} 作业页即将上线`)
      break
    case 'query':
    case 'inventory':
    case 'office':
      showToast(`${it.text} 即将上线`)
      break
    default:
      showToast(it.text)
  }
}

// 待办卡跳转
function goApproval(type: string) {
  router.push({ path: '/mobile/approval', query: { type } })
}
function goWork(_type: string) {
  showToast('作业页即将上线')
}

// ===== 今日动态 =====
interface FeedItem {
  type: string
  typeText: string
  no: string
  status: number
  statusText: string
  color: string
  party: string
  time: string
  rawTime: string
}

const feedList = ref<FeedItem[]>([])
const feedLoading = ref(true)

// 颜色映射
const colorMap: Record<string, string> = {
  purchase: '#5a67d8',
  stockin: '#52c41a',
  stockout: '#1890ff',
  transfer: '#f5576c',
  sale: '#faad14',
  loss: '#ff4d4f'
}

// 状态文案映射
const statusTextMap: Record<number, string> = {
  2: '已审核',
  3: '部分到货',
  4: '已完成',
  5: '已作废'
}

// 单号字段回退（不同模块字段名不一）
function pickNo(row: any): string {
  return row.purchaseNo || row.stockInNo || row.stockOutNo
    || row.transferNo || row.saleNo || row.lossNo || row.billNo || '-'
}

// 对方名称回退（供应商 / 客户 / 仓库）
function pickParty(row: any, type: string): string {
  if (type === 'transfer') {
    return [row.outWarehouseName, row.inWarehouseName].filter(Boolean).join(' → ') || '-'
  }
  return row.supplierName || row.customerName || row.skuName || '-'
}

async function fetchFeed() {
  feedLoading.value = true
  const sources = [
    { type: 'purchase', typeText: '采购单', fn: () => purchaseApi.page({ status: 2, pageNum: 1, pageSize: 3 }) },
    { type: 'stockin', typeText: '入库单', fn: () => stockInApi.page({ status: 2, pageNum: 1, pageSize: 3 }) },
    { type: 'stockout', typeText: '出库单', fn: () => stockOutApi.page({ status: 2, pageNum: 1, pageSize: 3 }) },
    { type: 'transfer', typeText: '调拨单', fn: () => transferApi.page({ status: 2, pageNum: 1, pageSize: 3 }) },
    { type: 'sale', typeText: '销售单', fn: () => saleApi.page({ status: 2, pageNum: 1, pageSize: 3 }) },
    { type: 'loss', typeText: '报损单', fn: () => lossApi.page({ status: 2, pageNum: 1, pageSize: 3 }) }
  ]
  const results = await Promise.allSettled(sources.map(s => s.fn()))
  const all: FeedItem[] = []
  results.forEach((r, i) => {
    if (r.status !== 'fulfilled') return
    const res: any = r.value
    const rows = res.data?.rows || res.rows || []
    rows.forEach((row: any) => {
      const rawTime = row.auditTime || row.updateTime || row.createTime || ''
      all.push({
        type: sources[i].type,
        typeText: sources[i].typeText,
        no: pickNo(row),
        status: row.status,
        statusText: statusTextMap[row.status] || `状态 ${row.status}`,
        color: colorMap[sources[i].type] || '#8c8c8c',
        party: pickParty(row, sources[i].type),
        time: formatDate(rawTime),
        rawTime
      })
    })
  })
  // 按时间倒序
  all.sort((a, b) => (b.rawTime || '').localeCompare(a.rawTime || ''))
  feedList.value = all.slice(0, 10)
  feedLoading.value = false
}

function goFeedDetail(it: FeedItem) {
  // 跳转到对应模块审批详情（如果还在线）
  showToast(`${it.typeText} ${it.no} 详情待 Phase 3`)
}

// ===== 工具函数 =====
function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 16)
}

function handleLogout() {
  showConfirmDialog({ title: '确认退出登录？' })
    .then(() => {
      userStore.resetState()
      router.replace('/mobile/login')
    })
    .catch(() => {})
}

onMounted(() => {
  // 用户信息可能未拉取
  if (userStore.token && !userStore.username) {
    userStore.fetchUserInfo().catch(() => {})
  }
  fetchTodos()
  fetchFeed()
})
</script>

<style scoped>
.m-dashboard {
  min-height: 100vh;
  background: #f4f5f9;
  padding-bottom: 80px;
}

/* ===== 头部渐变区 ===== */
.hero {
  background: linear-gradient(135deg, #667eea 0%, #5a67d8 50%, #764ba2 100%);
  padding: 0 20px 60px;
  position: relative;
  color: #fff;
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
}

.hero-status {
  height: 44px;
}

.hero-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
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

.user-name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.user-role {
  font-size: 13px;
  opacity: 0.85;
  margin-top: 2px;
}

.hero-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.25);
  flex-shrink: 0;
}

/* 待办统计卡 */
.todo-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 16px 18px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
}

.todo-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.todo-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
}

.todo-total {
  font-size: 13px;
  color: #888;
  font-weight: 500;
}

.todo-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.todo-cell {
  text-align: center;
  padding: 8px 4px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.todo-cell:active {
  background: rgba(102, 126, 234, 0.08);
}

.cell-num {
  font-size: 26px;
  font-weight: 800;
  color: #1a1a2e;
  line-height: 1.1;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}

.cell-num.num-warn {
  color: #ff4d4f;
}

.cell-label {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
}

/* ===== 区块通用 ===== */
.section {
  padding: 6px 14px 0;
}

.section-title {
  padding: 14px 4px 10px;
  font-size: 15px;
  color: #1a1a2e;
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

/* ===== 快捷入口九宫格 ===== */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  background: #fff;
  border-radius: 14px;
  padding: 16px 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.quick-cell {
  text-align: center;
  cursor: pointer;
  padding: 6px 0;
  transition: transform 0.15s;
}

.quick-cell:active {
  transform: scale(0.94);
}

.quick-icon {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.quick-text {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

/* ===== 今日动态列表 ===== */
.feed-list {
  background: #fff;
  border-radius: 14px;
  padding: 4px 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.feed-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid #f7f7f7;
  cursor: pointer;
  transition: background 0.15s;
}

.feed-item:last-child {
  border-bottom: none;
}

.feed-item:active {
  background: #fafafe;
}

.feed-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.feed-body {
  flex: 1;
  min-width: 0;
}

.feed-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.feed-type-tag {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
  flex-shrink: 0;
}

.feed-no {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a2e;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.feed-status {
  font-size: 12px;
  font-weight: 500;
  flex-shrink: 0;
}

.feed-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.feed-party {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 8px;
}

.feed-time {
  font-size: 11px;
  color: #bbb;
  flex-shrink: 0;
}

:deep(.van-empty) {
  padding: 40px 0;
}

/* ===== 底部退出按钮 ===== */
.footer-actions {
  padding: 24px 20px calc(env(safe-area-inset-bottom) + 16px);
  display: flex;
  justify-content: center;
}

.btn-logout {
  border: 1px solid #ddd;
  background: #fff;
  color: #595959;
  height: 40px;
  padding: 0 24px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: opacity 0.15s;
}

.btn-logout:active {
  opacity: 0.7;
}
</style>
