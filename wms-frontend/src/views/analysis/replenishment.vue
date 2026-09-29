<template>
  <div class="page-container">
    <!-- 筛选栏 -->
    <el-form :model="search" inline class="search-form">
      <el-form-item label="分析维度">
        <el-radio-group v-model="search.dimension">
          <el-radio-button label="sku">按SKU</el-radio-button>
          <el-radio-button label="innerCode">按内部编码</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="历史周期">
        <el-radio-group v-model="search.historyDays">
          <el-radio-button :label="7">近7天</el-radio-button>
          <el-radio-button :label="15">近15天</el-radio-button>
          <el-radio-button :label="30">近30天</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="预测周期">
        <el-radio-group v-model="search.forecastDays">
          <el-radio-button :label="7">未来7天</el-radio-button>
          <el-radio-button :label="15">未来15天</el-radio-button>
          <el-radio-button :label="30">未来30天</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="所属仓库">
        <el-select v-model="search.warehouseId" placeholder="全部仓库" clearable filterable style="width: 180px">
          <el-option v-for="w in warehouseList" :key="w.warehouseId" :label="w.warehouseName" :value="String(w.warehouseId)" />
        </el-select>
      </el-form-item>
      <el-form-item label="商品分类">
        <el-tree-select
          v-model="search.categoryId"
          :data="categoryTree"
          :props="{ label: 'categoryName', children: 'children' }"
          node-key="categoryId"
          check-strictly
          :render-after-expand="false"
          placeholder="全部分类"
          clearable
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="TrendCharts" :loading="loading" @click="handleAnalyze">开始分析</el-button>
        <el-button :icon="Download" :disabled="!tableData.length" @click="handleExport">导出CSV</el-button>
      </el-form-item>
    </el-form>

    <!-- 汇总看板 -->
    <el-row :gutter="16" class="summary-row">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover">
          <div class="summary-item">
            <div class="summary-title">分析SKU数</div>
            <div class="summary-value">{{ summary.skuCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover">
          <div class="summary-item">
            <div class="summary-title">总预测需求</div>
            <div class="summary-value">{{ summary.totalForecast }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover">
          <div class="summary-item">
            <div class="summary-title">总建议采购量</div>
            <div class="summary-value text-danger">{{ summary.totalSuggest }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="cursor-pointer" @click="filterShortage">
          <div class="summary-item">
            <div class="summary-title">紧急预警数</div>
            <div class="summary-value text-danger font-bold">{{ summary.shortageCount }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table
      :data="displayData"
      v-loading="loading"
      border
      stripe
      show-summary
      :summary-method="getSummaries"
      @sort-change="handleSortChange"
      class="replenishment-table"
    >
      <el-table-column type="index" label="#" width="55" align="center" />
      <el-table-column prop="skuCode" label="SKU编码" min-width="130" show-overflow-tooltip sortable="custom" />
      <el-table-column prop="skuName" label="商品名称" min-width="160" show-overflow-tooltip sortable="custom" />
      <el-table-column prop="specText" label="规格" min-width="120" show-overflow-tooltip />
      <el-table-column prop="color" label="颜色" width="90" show-overflow-tooltip />
      <el-table-column prop="innerCode" label="内部编码" min-width="120" show-overflow-tooltip sortable="custom" />
      <el-table-column prop="historyQty" label="历史出库量" width="110" align="right" sortable="custom" />
      <el-table-column prop="dailyAvgQty" label="日均出库" width="110" align="right" sortable="custom">
        <template #default="{ row }">{{ Number(row.dailyAvgQty || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="availableQty" label="当前可用" width="110" align="right" sortable="custom" />
      <el-table-column prop="forecastQty" label="预测需求" width="110" align="right" sortable="custom" />
      <el-table-column prop="suggestQty" label="建议采购" width="110" align="right" sortable="custom">
        <template #default="{ row }">
          <span :class="{ 'text-danger font-bold': row.suggestQty > 0 }">{{ row.suggestQty }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="supportDays" label="支撑天数" width="110" align="center" sortable="custom">
        <template #default="{ row }">
          <span v-if="row.supportDays == null">充足</span>
          <span v-else :class="{ 'text-danger font-bold': row.status === 3 }">{{ Number(row.supportDays).toFixed(1) }}天</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.status === 1" type="success" size="small">充足</el-tag>
          <el-tag v-else-if="row.status === 2" type="warning" size="small">紧张</el-tag>
          <el-tag v-else type="danger" size="small">缺货</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="suggestAmount" label="建议采购金额" width="140" align="right" sortable="custom">
        <template #default="{ row }">
          <span :class="{ 'text-danger': row.suggestAmount > 0 }">
            ¥{{ Number(row.suggestAmount || 0).toFixed(2) }}
          </span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Replenishment' })
import { ref, reactive, onMounted, computed } from 'vue'
import { TrendCharts, Download } from '@element-plus/icons-vue'
import { analysisApi, type ReplenishmentItem } from '@/api/analysis'
import { warehouseApi, categoryApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tableData = ref<ReplenishmentItem[]>([])
const warehouseList = ref<any[]>([])
const categoryTree = ref<any[]>([])

const search = reactive({
  dimension: 'sku' as 'sku' | 'innerCode',
  historyDays: 30,
  forecastDays: 15,
  warehouseId: undefined as undefined | string,
  categoryId: undefined as undefined | string,
})

// 紧急筛选状态（点击紧急预警卡片时只显示缺货）
const shortageOnly = ref(false)

const displayData = computed(() => {
  let rows = tableData.value
  if (shortageOnly.value) {
    rows = rows.filter(r => r.status === 3)
  }
  return rows
})

const summary = computed(() => {
  const rows = tableData.value
  const skuCount = rows.length
  const totalForecast = rows.reduce((sum, r) => sum + (r.forecastQty || 0), 0)
  const totalSuggest = rows.reduce((sum, r) => sum + (r.suggestQty || 0), 0)
  const shortageCount = rows.filter(r => r.status === 3).length
  return { skuCount, totalForecast, totalSuggest, shortageCount }
})

async function loadOptions() {
  try {
    const [wRes, cRes]: any = await Promise.all([
      warehouseApi.listAll(),
      categoryApi.tree(),
    ])
    warehouseList.value = wRes.data || []
    categoryTree.value = cRes.data || []
  } catch (e) {
    // handled by interceptor
  }
}

async function handleAnalyze() {
  loading.value = true
  shortageOnly.value = false
  try {
    const res: any = await analysisApi.replenishment({
      dimension: search.dimension,
      historyDays: search.historyDays,
      forecastDays: search.forecastDays,
      warehouseId: search.warehouseId,
      categoryId: search.categoryId,
    })
    tableData.value = res.data || []
  } catch (e) {
    /* handled */
  } finally {
    loading.value = false
  }
}

function filterShortage() {
  shortageOnly.value = !shortageOnly.value
  if (shortageOnly.value && tableData.value.length > 0) {
    ElMessage.info(shortageOnly.value ? '已筛选：仅显示紧急缺货项' : '已显示全部')
  }
}

function handleExport() {
  const rows = tableData.value
  if (!rows.length) return
  const headers = [
    'SKU编码', '商品名称', '规格', '颜色', '内部编码',
    '历史出库量', '日均出库', '当前可用', '预测需求', '建议采购',
    '支撑天数', '状态', '建议采购金额'
  ]
  const statusMap: Record<number, string> = { 1: '充足', 2: '紧张', 3: '缺货' }
  const lines = rows.map(r => [
    r.skuCode || '',
    r.skuName || '',
    r.specText || '',
    r.color || '',
    r.innerCode || '',
    String(r.historyQty || 0),
    Number(r.dailyAvgQty || 0).toFixed(2),
    String(r.availableQty || 0),
    String(r.forecastQty || 0),
    String(r.suggestQty || 0),
    r.supportDays == null ? '充足' : Number(r.supportDays).toFixed(1),
    statusMap[r.status] || '',
    Number(r.suggestAmount || 0).toFixed(2),
  ])
  const csvContent = lines.map(line => line.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(',')).join('\n')
  const content = '\uFEFF' + headers.join(',') + '\n' + csvContent
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  const date = new Date()
  const dateStr = `${date.getFullYear()}${String(date.getMonth() + 1).padStart(2, '0')}${String(date.getDate()).padStart(2, '0')}`
  link.download = `补货建议_${dateStr}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function getSummaries({ columns, data }: { columns: any[]; data: ReplenishmentItem[] }) {
  const sums: string[] = []
  columns.forEach((col, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    const prop = col.property
    if (['historyQty', 'availableQty', 'forecastQty', 'suggestQty'].includes(prop)) {
      sums[index] = String(data.reduce((prev, cur) => prev + Number((cur as any)[prop] || 0), 0))
    } else if (prop === 'suggestAmount') {
      const val = data.reduce((prev, cur) => prev + Number(cur.suggestAmount || 0), 0)
      sums[index] = `¥${val.toFixed(2)}`
    } else {
      sums[index] = ''
    }
  })
  return sums
}

// 表格前端排序（点击表头时触发，结合后端默认排序）
function handleSortChange({ prop, order }: { prop: string | null; order: string | null }) {
  if (!prop || !order) return
  const multiplier = order === 'ascending' ? 1 : -1
  tableData.value.sort((a: any, b: any) => {
    const va = a[prop] ?? 0
    const vb = b[prop] ?? 0
    if (typeof va === 'number' && typeof vb === 'number') {
      return (va - vb) * multiplier
    }
    return String(va).localeCompare(String(vb)) * multiplier
  })
}

onMounted(() => {
  loadOptions()
  handleAnalyze()
})
</script>

<style scoped>
.summary-row {
  margin-bottom: 16px;
}
.summary-item {
  text-align: center;
}
.summary-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}
.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}
.cursor-pointer {
  cursor: pointer;
}
.replenishment-table {
  margin-top: 16px;
}
.font-bold {
  font-weight: 700;
}
</style>
