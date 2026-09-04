<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="card-row">
      <el-col :span="4" v-for="card in cards" :key="card.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: card.color }">
            <el-icon size="28"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-title">{{ card.title }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图 + 仓库饼图 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>近30天出入库趋势</template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>各仓库库存分布</template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- TOP10 SKU -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>入库TOP10商品</template>
          <el-table :data="topInSku" size="small" stripe>
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="skuName" label="商品名称" show-overflow-tooltip />
            <el-table-column prop="totalQty" label="入库数量" width="100" align="right" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>出库TOP10商品</template>
          <el-table :data="topOutSku" size="small" stripe>
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="skuName" label="商品名称" show-overflow-tooltip />
            <el-table-column prop="totalQty" label="出库数量" width="100" align="right" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { dashboardApi } from '@/api'

const cards = reactive([
  { title: '今日入库', value: 0, icon: 'Download', color: '#409eff' },
  { title: '今日出库', value: 0, icon: 'Upload', color: '#67c23a' },
  { title: '在途采购', value: '¥0', icon: 'ShoppingCart', color: '#e6a23c' },
  { title: '库存SKU', value: 0, icon: 'Box', color: '#909399' },
  { title: '库存总额', value: '¥0', icon: 'Money', color: '#f56c6c' },
  { title: '临期预警', value: 0, icon: 'Warning', color: '#9c27b0' },
])

const topInSku = ref<any[]>([])
const topOutSku = ref<any[]>([])
const trendChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()

onMounted(() => {
  loadSummary()
  loadTrend()
  loadPie()
  loadTopSku()
})

async function loadSummary() {
  try {
    const res: any = await dashboardApi.summary()
    const d = res.data || {}
    cards[0].value = d.todayStockInQty || 0
    cards[1].value = d.todayStockOutQty || 0
    cards[2].value = `¥${formatNum(d.inTransitAmount)}`
    cards[3].value = d.inventorySkuCount || 0
    cards[4].value = `¥${formatNum(d.inventoryTotalAmount)}`
    cards[5].value = d.expireWarningCount || 0
  } catch (e) { /* empty data ok */ }
}

async function loadTrend() {
  try {
    const res: any = await dashboardApi.trend30({ startDate: '', endDate: '' })
    const data = res.data || []
    await nextTick()
    if (!trendChartRef.value) return
    const chart = echarts.init(trendChartRef.value)
    const dates = data.map((d: any) => d.date)
    const inData = data.map((d: any) => d.stockInQty || 0)
    const outData = data.map((d: any) => d.stockOutQty || 0)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['入库', '出库'] },
      grid: { left: 40, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value' },
      series: [
        { name: '入库', type: 'line', smooth: true, data: inData, itemStyle: { color: '#409eff' } },
        { name: '出库', type: 'line', smooth: true, data: outData, itemStyle: { color: '#67c23a' } },
      ]
    })
  } catch (e) { /* empty ok */ }
}

async function loadPie() {
  try {
    const res: any = await dashboardApi.warehousePie()
    const data = res.data || []
    await nextTick()
    if (!pieChartRef.value) return
    const chart = echarts.init(pieChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: data.map((d: any) => ({ name: d.warehouseName, value: d.totalQty })),
        label: { fontSize: 12 }
      }]
    })
  } catch (e) { /* empty ok */ }
}

async function loadTopSku() {
  try {
    const res: any = await dashboardApi.topSku({ limit: 10 })
    const data = res.data || {}
    topInSku.value = data.inList || []
    topOutSku.value = data.outList || []
  } catch (e) { /* empty ok */ }
}

function formatNum(n: any): string {
  const num = Number(n || 0)
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 0 })
}
</script>

<style scoped lang="scss">
.card-row { margin-bottom: 16px; }

.stat-card {
  :deep(.el-card__body) {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
  }
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-info {
  .stat-value {
    font-size: 22px;
    font-weight: 700;
    color: #303030;
  }
  .stat-title {
    font-size: 13px;
    color: #909399;
    margin-top: 4px;
  }
}

.chart-row { margin-bottom: 16px; }
.chart-container { height: 300px; }
</style>
