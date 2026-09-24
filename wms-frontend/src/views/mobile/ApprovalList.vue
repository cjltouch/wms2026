<template>
  <div class="m-list">
    <!-- ===== 头部渐变区（内嵌摘要 + Tab）===== -->
    <div class="hero">
      <!-- 状态栏占位 -->
      <div class="hero-status"></div>

      <!-- 标题行 -->
      <div class="hero-head">
        <div class="hero-title">
          <span class="greeting">你好 👋</span>
          <span class="app-name">采购审批工作台</span>
        </div>
        <div class="hero-avatar" @click="handleLogout">
          <van-icon name="user-o" size="18" />
        </div>
      </div>

      <!-- 待审批大数字卡 -->
      <div class="stat-card" @click="activeTab = 0; switchTab(0)">
        <div class="stat-icon">
          <van-icon name="clock-o" size="22" color="#667eea" />
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ pendingCount }}</div>
          <div class="stat-label">待我审批</div>
        </div>
        <van-icon name="arrow" size="16" color="#bbb" />
      </div>

      <!-- Tab（嵌在渐变区内） -->
      <div class="hero-tabs">
        <div
          class="hero-tab"
          :class="{ active: activeTab === 0 }"
          @click="switchTab(0)"
        >
          <span>待审批</span>
          <span v-if="pendingCount > 0" class="dot" />
        </div>
        <div
          class="hero-tab"
          :class="{ active: activeTab === 1 }"
          @click="switchTab(1)"
        >
          已审批
        </div>
        <!-- 滑动指示器 -->
        <div class="slider" :class="activeTab === 1 ? 'right' : 'left'" />
      </div>
    </div>

    <!-- 列表区（在波浪下方） -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh" class="pull-wrap">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="—— 没有更多了 ——"
        @load="onLoad"
        :error="false"
      >
        <div
          v-for="item in list"
          :key="item.purchaseId"
          class="order-card"
          @click="goDetail(item.purchaseId)"
        >
          <!-- 左侧状态条 + 右侧内容 -->
          <div class="card-indicator" :class="activeTab === 0 ? 'wait' : 'done'" />
          <div class="card-body">
            <div class="card-head">
              <span class="order-no">{{ item.purchaseNo }}</span>
              <span class="money">￥{{ formatAmount(item.totalAmount) }}</span>
            </div>
            <div class="card-info">
              <div class="info-line">
                <van-icon name="shop-o" />
                <span>{{ item.supplierName || '未指定供应商' }}</span>
              </div>
              <div class="info-line">
                <van-icon name="contact" />
                <span>{{ item.purchaserName || item.createName || '-' }}</span>
              </div>
            </div>
            <div class="card-foot">
              <span class="time">{{ formatDate(item.createTime) }}</span>
              <span class="items-count">{{ item.totalQty || 0 }} 件商品 ›</span>
            </div>
          </div>
        </div>

        <van-empty v-if="finished && list.length === 0" :description="activeTab === 0 ? '暂无待审批单据 🎉' : '暂无已审批记录'" image="search" />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { purchaseApi } from '@/api'
import { useUserStore } from '@/store/user'

defineOptions({ name: 'MobileApprovalList' })

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref(0)
const list = ref<any[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const pageSize = 10
const pendingCount = ref(0)

async function fetchPendingCount() {
  try {
    const res: any = await purchaseApi.page({ status: 1, pageNum: 1, pageSize: 1 })
    pendingCount.value = res.data?.total || res.total || 0
  } catch (e) { /* ignore */ }
}

async function onLoad() {
  const status = activeTab.value === 0 ? 1 : 2
  try {
    const res: any = await purchaseApi.page({ status, pageNum: pageNum.value, pageSize })
    const rows = res.data?.rows || res.rows || []
    const total = res.data?.total || res.total || 0
    if (pageNum.value === 1) {
      list.value = rows
    } else {
      list.value.push(...rows)
    }
    if (list.value.length >= total) {
      finished.value = true
    } else {
      pageNum.value++
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

function switchTab(idx: number) {
  activeTab.value = idx
  list.value = []
  pageNum.value = 1
  finished.value = false
  loading.value = true
  onLoad()
}

function goDetail(id: string) {
  router.push(`/mobile/approval/${id}`)
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

.app-name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
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
}

/* 待审批大数字卡 */
.stat-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
  margin-bottom: 18px;
  cursor: pointer;
}

.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: linear-gradient(135deg, #eef2ff, #e0e7ff);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-num {
  font-size: 28px;
  font-weight: 800;
  color: #1a1a2e;
  line-height: 1.1;
}

.stat-label {
  font-size: 13px;
  color: #888;
  margin-top: 2px;
}

/* Tab 切换 */
.hero-tabs {
  display: flex;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(8px);
  border-radius: 12px;
  padding: 4px;
  position: relative;
  width: 220px;
}

.hero-tab {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  font-weight: 500;
  position: relative;
  z-index: 2;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: color 0.2s;
}

.hero-tab.active {
  color: #1a1a2e;
}

.slider {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: #fff;
  border-radius: 9px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 1;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.slider.right {
  transform: translateX(100%);
}

.dot {
  width: 8px;
  height: 8px;
  background: #ff4d4f;
  border-radius: 50%;
  box-shadow: 0 0 6px #ff4d4f;
}

/* ===== 列表区 ===== */
.pull-wrap {
  flex: 1;
  overflow-y: auto;
  padding-top: 8px;
}

.order-card {
  background: #fff;
  margin: 0 14px 12px;
  border-radius: 14px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: transform 0.15s;
}

.order-card:active {
  transform: scale(0.985);
}

.card-indicator {
  width: 4px;
  flex-shrink: 0;
}

.card-indicator.wait {
  background: linear-gradient(180deg, #faad14, #ff7a45);
}

.card-indicator.done {
  background: linear-gradient(180deg, #52c41a, #389e0d);
}

.card-body {
  flex: 1;
  padding: 14px 16px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.order-no {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a2e;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}

.money {
  font-size: 17px;
  font-weight: 800;
  color: #ff4d4f;
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
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
  font-weight: 500;
}

:deep(.van-empty) {
  padding: 60px 0;
}
</style>
