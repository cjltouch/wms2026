<template>
  <div class="m-inv">
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
          <span class="greeting">实时库存 👋</span>
          <span class="app-name">库存查询</span>
        </div>
        <div class="hero-avatar" @click="handleLogout">
          <van-icon name="user-o" size="18" />
        </div>
      </div>

      <!-- 搜索框 -->
      <div class="search-card">
        <van-search
          v-model="keyword"
          :placeholder="subTab === 0 ? 'SKU编码/名称/批次号' : '单据号/SKU编码'"
          shape="round"
          :clearable="true"
          @search="onSearch"
          @clear="onClearKeyword"
          class="glass-search"
        />
      </div>

      <!-- 仓库筛选下拉 -->
      <div class="warehouse-filter">
        <van-dropdown-menu class="glass-dropdown">
          <van-dropdown-item v-model="warehouseId" :options="warehouseOptions" @change="onSearch" />
        </van-dropdown-menu>
      </div>
    </div>

    <!-- 子 Tab：库存 / 流水 -->
    <div class="sub-tabs">
      <div class="sub-tab" :class="{ active: subTab === 0 }" @click="switchSubTab(0)">
        <span class="sub-text">库存明细</span>
      </div>
      <div class="sub-tab" :class="{ active: subTab === 1 }" @click="switchSubTab(1)">
        <span class="sub-text">库存流水</span>
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
        <!-- 库存明细卡片 -->
        <template v-if="subTab === 0">
          <div
            v-for="(item, idx) in list"
            :key="`inv-${idx}`"
            class="inv-card"
          >
            <div class="card-indicator" :class="stockStatus(item)"></div>
            <div class="card-body">
              <div class="card-head">
                <span class="sku-name">{{ item.skuName || '-' }}</span>
                <span v-if="item.expireDate" class="expire-badge" :class="expireClass(item.expireDate)">
                  {{ expireText(item.expireDate) }}
                </span>
              </div>
              <div class="card-meta">
                <span class="sku-code">{{ item.skuCode || '-' }}</span>
                <span v-if="item.batchNo" class="batch-tag">批次 {{ item.batchNo }}</span>
              </div>
              <div class="card-warehouse">
                <van-icon name="shop-o" size="12" />
                <span>{{ item.warehouseName || '-' }}</span>
                <span v-if="item.supplierName" class="supplier">· {{ item.supplierName }}</span>
              </div>
              <div class="qty-grid">
                <div class="qty-cell">
                  <div class="qty-num">{{ item.quantity || 0 }}</div>
                  <div class="qty-label">库存</div>
                </div>
                <div class="qty-cell">
                  <div class="qty-num warn" v-if="(item.lockedQty || 0) > 0">{{ item.lockedQty || 0 }}</div>
                  <div class="qty-num" v-else>{{ item.lockedQty || 0 }}</div>
                  <div class="qty-label">锁定</div>
                </div>
                <div class="qty-cell">
                  <div class="qty-num" :class="{ danger: (item.availableQty || 0) <= 0 }">{{ item.availableQty || 0 }}</div>
                  <div class="qty-label">可用</div>
                </div>
                <div class="qty-cell">
                  <div class="qty-num money">¥{{ formatAmount(item.totalAmount) }}</div>
                  <div class="qty-label">金额</div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- 库存流水卡片 -->
        <template v-else>
          <div
            v-for="(item, idx) in list"
            :key="`log-${idx}`"
            class="log-card"
          >
            <div class="log-dir" :class="item.direction === 1 ? 'in' : 'out'">
              <van-icon :name="item.direction === 1 ? 'down' : 'up'" size="14" color="#fff" />
            </div>
            <div class="log-body">
              <div class="log-head">
                <span class="log-bill">{{ item.billNo || '-' }}</span>
                <span class="log-type-tag">{{ billTypeText(item.billType) }}</span>
              </div>
              <div class="log-meta">
                <span>{{ item.skuCode || '-' }}</span>
                <span class="log-sku-name">{{ item.skuName || '' }}</span>
              </div>
              <div class="log-qty-row">
                <span class="log-qty" :class="item.direction === 1 ? 'in' : 'out'">
                  {{ item.direction === 1 ? '+' : '-' }}{{ Number(item.qtyChange || 0).toFixed(2) }}
                </span>
                <span class="log-before-after">{{ item.beforeQty || 0 }} → {{ item.afterQty || 0 }}</span>
              </div>
              <div class="log-foot">
                <span class="log-time">{{ formatDate(item.operateTime) }}</span>
                <span class="log-operator">{{ item.operateName || item.operateBy || '-' }}</span>
              </div>
            </div>
          </div>
        </template>

        <van-empty
          v-if="finished && list.length === 0"
          :description="subTab === 0 ? '暂无库存数据' : '暂无流水记录'"
          image="search"
        />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { inventoryApi, warehouseApi } from '@/api'
import { useUserStore } from '@/store/user'

defineOptions({ name: 'MobileInventoryQuery' })

const router = useRouter()
const userStore = useUserStore()

// ===== 搜索状态 =====
const keyword = ref('')
const warehouseId = ref<string | number | undefined>(undefined)
const subTab = ref(0) // 0=库存明细, 1=库存流水
const list = ref<any[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const pageSize = 20

// 仓库下拉
const warehouseOptions = ref<{ text: string; value: string | number }[]>([{ text: '全部仓库', value: undefined as any }])

async function loadWarehouses() {
  try {
    const res: any = await warehouseApi.listAll()
    const rows = res.data || []
    warehouseOptions.value = [
      { text: '全部仓库', value: undefined as any },
      ...rows.map((w: any) => ({ text: w.warehouseName, value: w.warehouseId }))
    ]
  } catch { /* handled */ }
}

// ===== 单据类型（流水） =====
const billTypeMap: Record<string, string> = {
  STOCK_IN: '入库单',
  STOCK_OUT: '出库单',
  STOCK_OUT_LOCK: '出库锁定',
  PURCHASE_RETURN: '采购退货',
  TRANSFER_OUT: '调拨出库',
  TRANSFER_IN: '调拨入库',
  LOSS: '报损单',
  CHECK: '盘点单',
  PURCHASE: '采购单',
  SALE: '销售单',
  TRANSFER: '调拨单'
}
const numTypeMap: Record<string, string> = {
  '1': '采购退货', '2': '入库单', '3': '出库锁定', '4': '出库单',
  '5': '调拨出库', '6': '调拨入库', '7': '报损单', '8': '盘点单'
}
function billTypeText(t: any): string {
  if (t === null || t === undefined || t === '') return '-'
  const v = String(t)
  return billTypeMap[v] || numTypeMap[v] || v
}

// ===== 库存状态判断 =====
function stockStatus(item: any): string {
  if ((item.availableQty || 0) <= 0) return 'danger'
  if ((item.lockedQty || 0) > 0) return 'warn'
  return 'ok'
}

// ===== 过期判断 =====
function isExpired(dateStr: string): boolean {
  if (!dateStr) return false
  return new Date(dateStr) < new Date()
}
function isExpiringSoon(dateStr: string): boolean {
  if (!dateStr) return false
  const d = new Date(dateStr)
  const now = new Date()
  const diff = (d.getTime() - now.getTime()) / (1000 * 60 * 60 * 24)
  return diff >= 0 && diff <= 30
}
function expireText(dateStr: string): string {
  if (isExpired(dateStr)) return '已过期'
  if (isExpiringSoon(dateStr)) return '临期'
  return dateStr
}
function expireClass(dateStr: string): string {
  if (isExpired(dateStr)) return 'expired'
  if (isExpiringSoon(dateStr)) return 'soon'
  return 'normal'
}

// ===== 拉取列表 =====
async function onLoad() {
  try {
    if (subTab.value === 0) {
      // 库存明细
      const params: any = {
        keyword: keyword.value,
        warehouseId: warehouseId.value,
        page: pageNum.value,
        size: pageSize
      }
      const res: any = await inventoryApi.page(params)
      const d = res.data || {}
      const rows = d.rows || d.records || d.list || []
      const total = d.total || 0
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
    } else {
      // 库存流水
      const params: any = {
        billNo: keyword.value,
        skuCode: keyword.value,
        warehouseId: warehouseId.value,
        pageNum: pageNum.value,
        pageSize
      }
      const res: any = await inventoryApi.logPage(params)
      const d = res.data || {}
      const rows = d.rows || d.records || d.list || []
      const total = d.total || 0
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

function switchSubTab(idx: number) {
  if (subTab.value === idx) return
  subTab.value = idx
  list.value = []
  pageNum.value = 1
  finished.value = false
  loading.value = true
  onLoad()
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
  loadWarehouses()
})
</script>

<style scoped>
.m-inv {
  min-height: 100vh;
  background: #f4f5f9;
  display: flex;
  flex-direction: column;
}

/* ===== 头部渐变区 ===== */
.hero {
  background: linear-gradient(135deg, #36d1dc 0%, #5b86e5 50%, #2196f3 100%);
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
  background: rgba(255, 255, 255, 0.3);
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
  background: rgba(255, 255, 255, 0.15);
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

/* 搜索框 */
.search-card {
  position: relative;
  z-index: 2;
  margin-bottom: 10px;
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

/* 仓库筛选 */
.warehouse-filter {
  position: relative;
  z-index: 2;
  margin-bottom: 6px;
}

.glass-dropdown {
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(14px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  overflow: hidden;
}

:deep(.glass-dropdown .van-dropdown-menu__bar) {
  background: transparent;
  box-shadow: none;
  border-radius: 12px;
  height: 38px;
}

:deep(.glass-dropdown .van-dropdown-menu__item) {
  color: #fff;
  font-size: 13px;
}

:deep(.glass-dropdown .van-dropdown-menu__title) {
  color: #fff;
}

/* ===== 子 Tab ===== */
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
}

.sub-tab.active {
  background: linear-gradient(135deg, #36d1dc, #5b86e5);
  color: #fff;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(54, 209, 220, 0.3);
}

/* ===== 列表区 ===== */
.pull-wrap {
  flex: 1;
  overflow-y: auto;
  padding-top: 10px;
}

/* 库存明细卡片 */
.inv-card {
  background: #fff;
  margin: 0 14px 12px;
  border-radius: 16px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.card-indicator {
  width: 5px;
  flex-shrink: 0;
}

.card-indicator.ok {
  background: linear-gradient(180deg, #52c41a 0%, #389e0d 100%);
}

.card-indicator.warn {
  background: linear-gradient(180deg, #faad14 0%, #ff7a45 100%);
}

.card-indicator.danger {
  background: linear-gradient(180deg, #ff4d4f 0%, #cf1322 100%);
}

.card-body {
  flex: 1;
  padding: 14px 16px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 6px;
}

.sku-name {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a2e;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.expire-badge {
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 6px;
  font-weight: 600;
  flex-shrink: 0;
}

.expire-badge.expired {
  color: #ff4d4f;
  background: #fff2f0;
}

.expire-badge.soon {
  color: #faad14;
  background: #fff7e6;
}

.expire-badge.normal {
  color: #8c8c8c;
  background: #f5f5f5;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.sku-code {
  font-size: 12px;
  color: #888;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}

.batch-tag {
  font-size: 11px;
  color: #5b86e5;
  background: #5b86e51a;
  padding: 1px 7px;
  border-radius: 4px;
}

.card-warehouse {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #999;
  margin-bottom: 12px;
}

.card-warehouse .van-icon {
  font-size: 12px;
}

.supplier {
  margin-left: 4px;
}

/* 数量网格 */
.qty-grid {
  display: flex;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px dashed #f0f0f0;
}

.qty-cell {
  flex: 1;
  text-align: center;
}

.qty-num {
  font-size: 18px;
  font-weight: 800;
  color: #1a1a2e;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  line-height: 1.2;
}

.qty-num.warn {
  color: #faad14;
}

.qty-num.danger {
  color: #ff4d4f;
}

.qty-num.money {
  font-size: 14px;
  background: linear-gradient(135deg, #ff4d4f, #ff7a45);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.qty-label {
  font-size: 11px;
  color: #bbb;
  margin-top: 2px;
}

/* 库存流水卡片 */
.log-card {
  background: #fff;
  margin: 0 14px 12px;
  border-radius: 16px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.log-dir {
  width: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.log-dir.in {
  background: linear-gradient(180deg, #52c41a 0%, #389e0d 100%);
}

.log-dir.out {
  background: linear-gradient(180deg, #ff4d4f 0%, #cf1322 100%);
}

.log-body {
  flex: 1;
  padding: 12px 14px;
}

.log-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.log-bill {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a2e;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-type-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  background: #5b86e51a;
  color: #5b86e5;
  font-weight: 600;
  flex-shrink: 0;
}

.log-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
  color: #888;
}

.log-sku-name {
  color: #666;
}

.log-qty-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.log-qty {
  font-size: 17px;
  font-weight: 800;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}

.log-qty.in {
  color: #52c41a;
}

.log-qty.out {
  color: #ff4d4f;
}

.log-before-after {
  font-size: 12px;
  color: #bbb;
}

.log-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 6px;
  border-top: 1px dashed #f0f0f0;
}

.log-time {
  font-size: 12px;
  color: #bbb;
}

.log-operator {
  font-size: 12px;
  color: #999;
}

:deep(.van-empty) {
  padding: 60px 0;
}
</style>
