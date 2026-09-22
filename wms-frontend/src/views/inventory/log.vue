<template>
  <div class="page-container">
    <!-- 搜索 -->
    <el-form :model="search" inline class="search-form">
      <el-form-item label="单据号">
        <el-input v-model="search.billNo" placeholder="请输入单据号" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="仓库">
        <el-select v-model="search.warehouseId" placeholder="全部仓库" clearable style="width: 160px">
          <el-option v-for="w in warehouseOptions" :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
        </el-select>
      </el-form-item>
      <el-form-item label="单据类型">
        <el-select v-model="search.billType" placeholder="全部类型" clearable style="width: 160px">
          <el-option v-for="o in billTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="SKU编码">
        <el-input v-model="search.skuCode" placeholder="请输入SKU编码" clearable style="width: 160px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column type="index" label="#" width="55" align="center" />
      <el-table-column prop="billNo" label="单据号" min-width="150" show-overflow-tooltip />
      <el-table-column label="单据类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ billTypeText(row.billType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="仓库" width="120" align="center">
        <template #default="{ row }">
          <el-tag type="info" effect="plain" size="small">{{ row.warehouseName || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="skuCode" label="SKU编码" min-width="130" show-overflow-tooltip />
      <el-table-column prop="innerCode" label="内部编码" min-width="130" show-overflow-tooltip />
      <el-table-column prop="skuName" label="商品名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="方向" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.direction === 1" type="success" size="small">入库</el-tag>
          <el-tag v-else-if="row.direction === -1" type="danger" size="small">出库</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="变动数量" width="110" align="right">
        <template #default="{ row }">
          <span :class="row.direction === 1 ? 'text-success' : (row.direction === -1 ? 'text-danger' : '')">
            <template v-if="row.direction === 1">+</template>
            <template v-else-if="row.direction === -1">-</template>
            {{ Number(row.qtyChange || 0).toFixed(2) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="锁定变动" width="110" align="right">
        <template #default="{ row }">
          <span :class="row.changeLocked > 0 ? 'text-warning' : (row.changeLocked < 0 ? 'text-danger' : '')">
            <template v-if="row.changeLocked > 0">+</template>
            {{ Number(row.changeLocked || 0).toFixed(2) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="beforeQty" label="变动前" width="100" align="right" />
      <el-table-column prop="afterQty" label="变动后" width="100" align="right" />
      <el-table-column label="单价" width="110" align="right">
        <template #default="{ row }">
          ¥{{ Number(row.unitPrice || 0).toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column label="操作人" width="100">
        <template #default="{ row }">
          {{ row.operateName || row.operateBy || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="operateTime" label="操作时间" width="160" align="center" />
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="search.pageNum"
        v-model:page-size="search.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'InventoryLog' })
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { inventoryApi, warehouseApi } from '@/api'

const billTypeOptions = [
  { value: 'STOCK_IN', label: '入库单' },
  { value: 'STOCK_OUT', label: '出库单' },
  { value: 'STOCK_OUT_LOCK', label: '出库锁定' },
  { value: 'PURCHASE_RETURN', label: '采购退货' },
  { value: 'TRANSFER_OUT', label: '调拨出库' },
  { value: 'TRANSFER_IN', label: '调拨入库' },
  { value: 'LOSS', label: '报损单' },
  { value: 'CHECK', label: '盘点单' },
  { value: 'PURCHASE', label: '采购单' },
  { value: 'SALE', label: '销售单' },
  { value: 'TRANSFER', label: '调拨单' },
]
function billTypeText(t: number | string) {
  if (t === null || t === undefined || t === '') return '-'
  // 先按字符串 value 匹配
  const v = String(t)
  const byStr = billTypeOptions.find(o => String(o.value) === v)
  if (byStr) return byStr.label
  // 兼容数字值（老接口 ChangeType.code）
  const numMap: Record<string, string> = {
    '1': '采购退货', '2': '入库单', '3': '出库锁定', '4': '出库单',
    '5': '调拨出库', '6': '调拨入库', '7': '报损单', '8': '盘点单',
  }
  return numMap[v] || v
}

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

// 仓库下拉选项
const warehouseOptions = ref<any[]>([])
async function loadWarehouses() {
  try {
    const res: any = await warehouseApi.page({ pageNum: 1, pageSize: 1000 })
    warehouseOptions.value = res.data?.rows || res.data?.records || []
  } catch (e) { /* handled */ }
}

const dateRange = ref<[string, string] | []>([])
const search = reactive({
  billNo: '',
  warehouseId: undefined as undefined | string,
  billType: '',
  skuCode: '',
  startDate: '',
  endDate: '',
  pageNum: 1,
  pageSize: 10,
})

async function loadData() {
  loading.value = true
  try {
    search.startDate = dateRange.value?.[0] || ''
    search.endDate = dateRange.value?.[1] || ''
    const params: Record<string, any> = { ...search }
    if (search.startDate) params.dateRangeStart = search.startDate + ' 00:00:00'
    if (search.endDate) params.dateRangeEnd = search.endDate + ' 23:59:59'
    const res: any = await inventoryApi.logPage(params)
    const d = res.data || {}
    tableData.value = d.rows || d.records || d.list || []
    total.value = d.total || 0
  } catch (e) {
    /* handled */
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  search.pageNum = 1
  loadData()
}

function resetSearch() {
  search.billNo = ''
  search.warehouseId = undefined
  search.billType = ''
  search.skuCode = ''
  dateRange.value = []
  search.startDate = ''
  search.endDate = ''
  search.pageNum = 1
  loadData()
}

onMounted(() => {
  loadWarehouses()
  loadData()
})
</script>

<style scoped lang="scss">
.text-success {
  color: #67c23a;
  font-weight: 600;
}
.text-danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>
