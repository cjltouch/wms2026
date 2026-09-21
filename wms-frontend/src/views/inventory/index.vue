<template>
  <div class="page-container">
    <!-- 搜索 -->
    <el-form :model="search" inline class="search-form">
      <el-form-item label="所属仓库">
        <el-select v-model="search.warehouseId" placeholder="全部仓库" clearable filterable style="width: 180px">
          <el-option v-for="w in warehouseList" :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
        </el-select>
      </el-form-item>
      <el-form-item label="商品关键字">
        <el-input v-model="search.keyword" placeholder="SKU编码/名称" clearable style="width: 200px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="批次号">
        <el-input v-model="search.batchNo" placeholder="请输入批次号" clearable style="width: 160px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作 -->
    <div class="table-operations">
      <el-button v-perm="'wms:inventory:list'" :icon="Download" :loading="exporting" @click="handleExport">导出</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" border stripe show-summary :summary-method="getSummaries">
      <el-table-column type="index" label="#" width="55" align="center" />
      <el-table-column prop="warehouseName" label="所属仓库" min-width="130" show-overflow-tooltip />
      <el-table-column prop="supplierName" label="供应商" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.supplierName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="skuCode" label="SKU编码" min-width="130" show-overflow-tooltip />
      <el-table-column prop="innerCode" label="内部编码" min-width="130" show-overflow-tooltip />
      <el-table-column prop="skuName" label="商品名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="specText" label="规格" min-width="120" show-overflow-tooltip />
      <el-table-column prop="batchNo" label="批次号" width="120" show-overflow-tooltip />
      <el-table-column prop="quantity" label="库存数量" width="100" align="right" />
      <el-table-column prop="lockedQty" label="锁定数量" width="100" align="right">
        <template #default="{ row }">
          <span :class="{ 'text-danger': row.lockedQty > 0 }">{{ row.lockedQty || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="availableQty" label="可用数量" width="100" align="right">
        <template #default="{ row }">
          <span :class="{ 'text-warning': row.availableQty <= 0 }">{{ row.availableQty || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="成本单价" width="120" align="right">
        <template #default="{ row }">
          ¥{{ Number(row.costPrice || 0).toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column label="库存金额" width="130" align="right">
        <template #default="{ row }">
          ¥{{ Number(row.totalAmount || 0).toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column prop="produceDate" label="生产日期" width="110" align="center" />
      <el-table-column prop="expireDate" label="过期日期" width="110" align="center">
        <template #default="{ row }">
          <el-tag v-if="isExpired(row.expireDate)" type="danger" size="small">已过期</el-tag>
          <el-tag v-else-if="isExpiringSoon(row.expireDate)" type="warning" size="small">临期</el-tag>
          <span v-else>{{ row.expireDate || '-' }}</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="search.page"
        v-model:page-size="search.size"
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
defineOptions({ name: 'Inventory' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import { inventoryApi, warehouseApi } from '@/api'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const warehouseList = ref<any[]>([])

const search = reactive({
  warehouseId: undefined as any,
  keyword: '',
  batchNo: '',
  page: 1,
  size: 10,
})

async function loadWarehouses() {
  try {
    const res: any = await warehouseApi.listAll()
    warehouseList.value = res.data || []
  } catch (e) { /* handled */ }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await inventoryApi.page(search)
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
  search.page = 1
  loadData()
}

function resetSearch() {
  search.warehouseId = undefined
  search.keyword = ''
  search.batchNo = ''
  search.page = 1
  loadData()
}

function isExpired(date: string) {
  if (!date) return false
  return new Date(date).getTime() < Date.now()
}

function isExpiringSoon(date: string) {
  if (!date) return false
  const diff = new Date(date).getTime() - Date.now()
  return diff > 0 && diff <= 30 * 24 * 60 * 60 * 1000
}

function getSummaries({ columns, data }: { columns: any[]; data: any[] }) {
  const sums: string[] = []
  columns.forEach((col, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    const prop = col.property
    if (['quantity', 'lockedQty', 'availableQty'].includes(prop)) {
      sums[index] = String(data.reduce((prev, cur) => prev + Number(cur[prop] || 0), 0))
    } else if (prop === 'totalAmount') {
      const val = data.reduce((prev, cur) => prev + Number(cur[prop] || 0), 0)
      sums[index] = `¥${val.toFixed(2)}`
    } else {
      sums[index] = ''
    }
  })
  return sums
}

const exporting = ref(false)

async function handleExport() {
  if (exporting.value) return
  exporting.value = true
  try {
    const blob: any = await inventoryApi.export({
      warehouseId: search.warehouseId || undefined,
      keyword: search.keyword || undefined,
      batchNo: search.batchNo || undefined,
    })
    // 后端返回的错误JSON也可能被包装成blob，需识别
    if (blob && (blob.type || '').includes('application/json')) {
      const text = await blob.text()
      try {
        const err = JSON.parse(text)
        ElMessage.error(err.msg || '导出失败')
      } catch {
        ElMessage.error('导出失败')
      }
      return
    }
    const url = window.URL.createObjectURL(blob as Blob)
    const a = document.createElement('a')
    const today = new Date()
    const stamp = `${today.getFullYear()}${String(today.getMonth() + 1).padStart(2, '0')}${String(today.getDate()).padStart(2, '0')}`
    a.href = url
    a.download = `库存查询_${stamp}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  loadWarehouses()
  loadData()
})
</script>

<style scoped lang="scss">
.text-danger {
  color: #f56c6c;
  font-weight: 600;
}
.text-warning {
  color: #e6a23c;
  font-weight: 600;
}
</style>
