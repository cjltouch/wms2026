<template>
  <div class="m-dashboard">
    <!-- ===== 沉浸式渐变头部 ===== -->
    <div class="hero">
      <!-- 多层渐变光斑 -->
      <div class="hero-orb orb-a"></div>
      <div class="hero-orb orb-b"></div>
      <div class="hero-orb orb-c"></div>

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

      <!-- ===== 待办主大数字卡 + 副小卡层级 ===== -->
      <div class="todo-stack">
        <!-- 主卡：今日待办总数 -->
        <div class="todo-main" @click="goApproval('purchase')">
          <div class="main-left">
            <div class="main-label">今日待办</div>
            <div class="main-num">{{ todoTotal }}</div>
            <div class="main-sub">项任务待处理</div>
          </div>
          <div class="main-ring">
            <van-icon name="clock-o" size="28" />
          </div>
        </div>

        <!-- 副卡：4 分类小数字 -->
        <div class="todo-subs">
          <div class="sub-cell" @click="goApproval('purchase')">
            <div class="sub-num" :class="{ warn: todo.approval > 0 }">{{ todo.approval }}</div>
            <div class="sub-label">待审批</div>
          </div>
          <div class="sub-cell" @click="goWork('stockin')">
            <div class="sub-num" :class="{ warn: todo.stockin > 0 }">{{ todo.stockin }}</div>
            <div class="sub-label">待上架</div>
          </div>
          <div class="sub-cell" @click="goWork('stockout')">
            <div class="sub-num" :class="{ warn: todo.stockout > 0 }">{{ todo.stockout }}</div>
            <div class="sub-label">待拣货</div>
          </div>
          <div class="sub-cell" @click="goWork('check')">
            <div class="sub-num" :class="{ warn: todo.check > 0 }">{{ todo.check }}</div>
            <div class="sub-label">待盘点</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 快捷入口：渐变大卡 + 圆形图标网格混合布局 ===== -->
    <div class="section">
      <div class="section-title">
        <span class="title-dot"></span>快捷入口
      </div>

      <!-- 上排：两个渐变大卡 -->
      <div class="quick-big-row">
        <div class="quick-big big-a" @click="goQuick({ key: 'approval', text: '审批中心' })">
          <div class="big-icon"><van-icon name="passed" size="24" color="#fff" /></div>
          <div class="big-body">
            <div class="big-text">审批中心</div>
            <div class="big-sub">{{ todo.approval }} 单待我审批</div>
          </div>
          <van-icon name="arrow" size="14" color="rgba(255,255,255,0.8)" />
        </div>
        <div class="quick-big big-b" @click="goQuick({ key: 'stockin', text: '入库上架' })">
          <div class="big-icon"><van-icon name="after-sale" size="24" color="#fff" /></div>
          <div class="big-body">
            <div class="big-text">入库上架</div>
            <div class="big-sub">{{ todo.stockin }} 单待上架</div>
          </div>
          <van-icon name="arrow" size="14" color="rgba(255,255,255,0.8)" />
        </div>
      </div>

      <!-- 下排：圆形图标网格 -->
      <div class="quick-grid">
        <div
          v-for="it in quickSmall"
          :key="it.key"
          class="quick-cell"
          @click="goQuick(it)"
        >
          <div class="quick-icon" :style="{ background: it.bg }">
            <van-icon :name="it.icon" size="20" color="#fff" />
          </div>
          <div class="quick-text">{{ it.text }}</div>
        </div>
      </div>
    </div>

    <!-- ===== 今日动态：时间轴样式 ===== -->
    <div class="section">
      <div class="section-title">
        <span class="title-dot"></span>今日动态
        <span class="section-sub">最近 10 条</span>
      </div>

      <div class="timeline" v-if="feedList.length > 0">
        <div
          v-for="(it, idx) in feedList"
          :key="idx"
          class="tl-item"
          :class="{ last: idx === feedList.length - 1 }"
          @click="goFeedDetail(it)"
        >
          <!-- 左侧轴线 + 色点 -->
          <div class="tl-rail">
            <div class="tl-dot" :style="{ background: it.color, boxShadow: `0 0 0 4px ${it.color}22` }"></div>
            <div class="tl-line" v-if="idx !== feedList.length - 1"></div>
          </div>

          <!-- 右侧卡片 -->
          <div class="tl-card">
            <div class="tl-head">
              <span class="tl-tag" :style="{ color: it.color, background: it.color + '1a', borderColor: it.color + '40' }">{{ it.typeText }}</span>
              <span class="tl-status" :style="{ color: it.color }">{{ it.statusText }}</span>
            </div>
            <div class="tl-no">{{ it.no }}</div>
            <div class="tl-meta">
              <span class="tl-party">{{ it.party || '-' }}</span>
              <span class="tl-time">{{ it.time }}</span>
            </div>
          </div>
        </div>
      </div>

      <van-empty
        v-else-if="!feedLoading"
        description="今日暂无动态"
        image="search"
      />
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

// ===== 快捷入口（下排小图标网格） =====
const quickSmall = [
  { key: 'stockout', icon: 'logistics', text: '出库拣货', bg: 'linear-gradient(135deg,#667eea,#764ba2)' },
  { key: 'transfer', icon: 'exchange', text: '调拨搬运', bg: 'linear-gradient(135deg,#f093fb,#f5576c)' },
  { key: 'check', icon: 'records', text: '盘点作业', bg: 'linear-gradient(135deg,#faad14,#ff7a45)' },
  { key: 'loss', icon: 'warning-o', text: '报损录入', bg: 'linear-gradient(135deg,#ff4d4f,#cf1322)' },
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
  bottom: 60px;
  left: -50px;
}

.orb-c {
  width: 120px;
  height: 120px;
  background: rgba(255, 255, 255, 0.25);
  top: 35%;
  right: 20%;
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
  margin-bottom: 20px;
  position: relative;
  z-index: 2;
}

.hero-title {
  display: flex;
  flex-direction: column;
  gap: 3px;
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
  font-size: 12px;
  opacity: 0.8;
  margin-top: 1px;
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

/* ===== 待办主大数字卡 + 副小卡 ===== */
.todo-stack {
  position: relative;
  z-index: 2;
}

.todo-main {
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-radius: 18px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 28px rgba(45, 35, 110, 0.18);
  margin-bottom: 10px;
  cursor: pointer;
  transition: transform 0.15s;
}

.todo-main:active {
  transform: scale(0.98);
}

.main-label {
  font-size: 13px;
  opacity: 0.85;
  margin-bottom: 2px;
}

.main-num {
  font-size: 40px;
  font-weight: 800;
  line-height: 1;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.main-sub {
  font-size: 12px;
  opacity: 0.75;
  margin-top: 4px;
}

.main-ring {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 副卡：4 分类小数字 */
.todo-subs {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.sub-cell {
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 14px;
  padding: 12px 4px 10px;
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.22);
  cursor: pointer;
  transition: transform 0.15s, background 0.2s;
}

.sub-cell:active {
  transform: scale(0.94);
  background: rgba(255, 255, 255, 0.26);
}

.sub-num {
  font-size: 22px;
  font-weight: 800;
  color: #fff;
  line-height: 1.1;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}

.sub-num.warn {
  color: #ffd666;
  text-shadow: 0 0 8px rgba(255, 214, 102, 0.5);
}

.sub-label {
  font-size: 11px;
  opacity: 0.85;
  margin-top: 4px;
}

/* ===== 区块通用 ===== */
.section {
  padding: 6px 14px 0;
  position: relative;
  z-index: 4;
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

/* ===== 快捷入口：上排渐变大卡 ===== */
.quick-big-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 10px;
}

.quick-big {
  border-radius: 16px;
  padding: 14px 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: transform 0.15s;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.12);
  position: relative;
  overflow: hidden;
}

.quick-big:active {
  transform: scale(0.97);
}

.big-a {
  background: linear-gradient(135deg, #5a67d8 0%, #764ba2 100%);
}

.big-b {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.big-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.big-body {
  flex: 1;
  min-width: 0;
}

.big-text {
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 2px;
}

.big-sub {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.85);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 下排：圆形图标网格 */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  background: #fff;
  border-radius: 16px;
  padding: 14px 8px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.05);
}

.quick-cell {
  text-align: center;
  cursor: pointer;
  padding: 6px 0;
  transition: transform 0.15s;
}

.quick-cell:active {
  transform: scale(0.92);
}

.quick-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.quick-text {
  font-size: 12px;
  color: #595959;
  font-weight: 500;
}

/* ===== 今日动态：时间轴 ===== */
.timeline {
  background: #fff;
  border-radius: 16px;
  padding: 12px 14px 4px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.05);
}

.tl-item {
  display: flex;
  gap: 12px;
  padding-bottom: 14px;
  cursor: pointer;
}

.tl-item:last-child {
  padding-bottom: 8px;
}

.tl-rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  padding-top: 4px;
}

.tl-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  z-index: 1;
}

.tl-line {
  flex: 1;
  width: 2px;
  background: linear-gradient(180deg, #e8e8e8, transparent);
  margin-top: 4px;
  min-height: 30px;
}

.tl-card {
  flex: 1;
  min-width: 0;
  background: #fafafe;
  border-radius: 12px;
  padding: 10px 12px;
  transition: background 0.15s;
}

.tl-item:active .tl-card {
  background: #f0f0f8;
}

.tl-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.tl-tag {
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 6px;
  font-weight: 600;
  border: 1px solid transparent;
}

.tl-status {
  font-size: 11px;
  font-weight: 600;
}

.tl-no {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a2e;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tl-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  color: #999;
}

.tl-party {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 8px;
}

.tl-time {
  flex-shrink: 0;
  color: #bbb;
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
