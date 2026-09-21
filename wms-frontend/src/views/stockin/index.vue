<template>
  <div class="page-container">
    <!-- 搜索 -->
    <el-form :model="search" inline class="search-form">
      <el-form-item label="入库单号">
        <el-input v-model="search.stockInNo" placeholder="请输入入库单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="入库类型">
        <el-select v-model="search.type" placeholder="全部类型" clearable style="width: 140px">
          <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="search.status" placeholder="全部状态" clearable style="width: 120px">
          <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
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

    <!-- 操作 -->
    <div class="table-operations">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增入库单</el-button>
      <el-button type="success" :icon="Download" @click="handleExport">导出入库明细</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column type="index" label="#" width="55" align="center" />
      <el-table-column prop="stockInNo" label="入库单号" min-width="150" show-overflow-tooltip />
      <el-table-column label="入库类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.type)">{{ typeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
<!--      <el-table-column prop="sourceBillNo" label="源单号" min-width="140" show-overflow-tooltip />-->
      <el-table-column prop="warehouseName" label="入库仓库" min-width="130" show-overflow-tooltip />
      <el-table-column prop="totalQty" label="总数量" width="100" align="right" />
      <el-table-column label="总金额" width="130" align="right">
        <template #default="{ row }">
          ¥{{ Number(row.totalAmount || 0).toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="inByName" label="入库人" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="160" align="center" />
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click="handleDetail(row)">详情</el-button>
          <el-button link type="primary" @click="handlePrint(row)">打印</el-button>
          <el-button v-if="row.status === 0" link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 0" link type="success" @click="handleRowSubmit(row)">提交</el-button>
          <el-button v-if="[1, 2].includes(row.status)" link type="warning" :icon="Check" @click="handleAudit(row)">验收上架</el-button>
          <el-button v-if="row.status === 3" link type="info" @click="handleUnaudit(row)">反审核</el-button>
          <el-button v-if="[0, 1, 2].includes(row.status)" link type="danger" @click="handleVoid(row)">作废</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="1050px" top="5vh" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="入库单号" prop="stockInNo">
              <el-input v-model="form.stockInNo" placeholder="自动生成" readonly>
                <template #append>
                  <el-button @click="generateNo">生成</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="入库类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择入库类型" style="width: 100%">
                <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="源单号" prop="sourceBillNo">
              <el-input v-model="form.sourceBillNo" placeholder="如采购单号" @blur="onSourceBlur" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="入库仓库" prop="warehouseId">
              <el-select v-model="form.warehouseId" placeholder="请选择仓库" filterable style="width: 100%">
                <el-option v-for="w in warehouseList" :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="form.supplierId" placeholder="请选择供应商(选填)" filterable clearable style="width: 100%">
                <el-option v-for="s in supplierList" :key="s.supplierId" :label="s.supplierName" :value="s.supplierId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- 明细 -->
      <div class="detail-section">
        <div class="detail-header">
          <span class="detail-title">入库明细</span>
          <el-button type="primary" size="small" :icon="Plus" @click="addDetailRow">添加行</el-button>
        </div>
        <el-table :data="form.details" border size="small" style="width: 100%">
          <el-table-column type="index" label="#" width="45" align="center" />
          <el-table-column label="商品" min-width="180">
            <template #default="{ row }">
              <el-select
                v-model="row.skuId"
                placeholder="搜索选择商品"
                filterable
                remote
                :remote-method="(q: string) => searchSku(q)"
                :loading="skuLoading"
                style="width: 100%"
                @change="() => onSkuSelect(row)"
              >
                <el-option v-for="s in skuOptions" :key="s.skuId" :label="s.skuName + (s.skuCode ? ' (' + s.skuCode + ')' : '')" :value="s.skuId" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="skuCode" label="编码" width="110" />
          <el-table-column label="内部编码" prop="innerCode" width="110" />
          <el-table-column prop="skuName" label="名称" min-width="130" show-overflow-tooltip />
          <el-table-column label="单位" prop="unitName" width="60" />
          <el-table-column label="预期数量" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.expectedQty" :min="0" :precision="2" size="small" style="width: 100%" @change="calcRow(row)" />
            </template>
          </el-table-column>
          <el-table-column label="实际数量" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.actualQty" :min="0" :precision="2" size="small" style="width: 100%" @change="calcRow(row)" />
            </template>
          </el-table-column>
          <el-table-column label="差异数量" width="100" align="right">
            <template #default="{ row }">
              <span :class="{ 'diff-neg': row.diffQty < 0 }">{{ row.diffQty }}</span>
            </template>
          </el-table-column>
          <el-table-column label="成本单价" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.costPrice" :min="0" :precision="4" size="small" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="批次号" width="130">
            <template #default="{ row }">
              <el-input v-model="row.batchNo" size="small" placeholder="批次号" />
            </template>
          </el-table-column>
          <el-table-column label="生产日期" width="140">
            <template #default="{ row }">
              <el-date-picker v-model="row.produceDate" type="date" value-format="YYYY-MM-DD" size="small" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="过期日期" width="140">
            <template #default="{ row }">
              <el-date-picker v-model="row.expireDate" type="date" value-format="YYYY-MM-DD" size="small" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="库位" min-width="150">
            <template #default="{ row }">
              <el-select v-model="row.locationId" placeholder="选择库位" filterable size="small" style="width: 100%">
                <el-option v-for="l in locationOptions" :key="l.locationId" :label="l.locationCode" :value="l.locationId" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" :icon="Delete" @click="removeDetailRow($index)" />
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog title="入库单详情" v-model="detailVisible" width="950px" top="5vh">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="入库单号">{{ detail.stockInNo }}</el-descriptions-item>
        <el-descriptions-item label="入库类型">{{ typeText(detail.type) }}</el-descriptions-item>
        <el-descriptions-item label="源单号">{{ detail.sourceBillNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="入库仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detail.supplierName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTag(detail.status)">{{ statusText(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总数量">{{ detail.totalQty }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ Number(detail.totalAmount || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="入库人">{{ detail.inByName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-section">
        <div class="detail-title">入库明细</div>
        <el-table :data="detail.details" border size="small">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="skuCode" label="编码" width="110" />
          <el-table-column label="内部编码" prop="innerCode" width="110" />
          <el-table-column prop="skuName" label="名称" min-width="130" show-overflow-tooltip />
          <el-table-column label="单位" prop="unitName" width="60" />
          <el-table-column prop="expectedQty" label="预期数量" width="100" align="right" />
          <el-table-column prop="actualQty" label="实际数量" width="100" align="right" />
          <el-table-column prop="diffQty" label="差异" width="90" align="right" />
          <el-table-column label="成本单价" width="100" align="right">
            <template #default="{ row }">¥{{ Number(row.costPrice || 0).toFixed(4) }}</template>
          </el-table-column>
          <el-table-column prop="batchNo" label="批次号" width="120" />
          <el-table-column prop="produceDate" label="生产日期" width="110" align="center" />
          <el-table-column prop="expireDate" label="过期日期" width="110" align="center" />
          <el-table-column prop="locationCode" label="库位" width="120" />
        </el-table>
      </div>

      <div class="detail-section">
        <div class="detail-title">状态日志</div>
        <el-timeline>
          <el-timeline-item
            v-for="(log, i) in detail.logs"
            :key="i"
            :timestamp="log.operateTime"
            placement="top"
            :type="log.toStatus === 4 ? 'danger' : 'primary'"
          >
            <h4>{{ statusText(log.toStatus) }}</h4>
            <p>{{ log.operateName || log.operateBy }} - {{ log.remark || '' }}</p>
          </el-timeline-item>
        </el-timeline>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="printCurrentDetail">打印入库单</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" title="验收上架" width="480px" :close-on-click-modal="false">
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
        <el-form-item label="单据号">
          <span>{{ auditRow?.stockInNo || '-' }}</span>
        </el-form-item>
        <el-form-item label="审核结果" prop="pass">
          <el-radio-group v-model="auditForm.pass">
            <el-radio :label="true">通过</el-radio>
            <el-radio :label="false">不通过</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="auditForm.pass === false" label="不通过原因" prop="remark">
          <el-input v-model="auditForm.remark" type="textarea" :rows="4" placeholder="请填写审核不通过原因" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item v-else label="备注">
          <el-input v-model="auditForm.remark" type="textarea" :rows="2" placeholder="选填" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAudit">确定</el-button>
      </template>
    </el-dialog>


  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, View, Check, Download } from '@element-plus/icons-vue'
import { stockInApi, warehouseApi, supplierApi, locationApi, goodsSpuApi } from '@/api'

const router = useRouter()

/** 在新标签页打开单据打印页 */
function openPrint(stockInId: string) {
  const url = router.resolve({ name: 'BillPrint', params: { billType: 'stock-in', id: stockInId } }).href
  window.open(url, '_blank')
}

const typeOptions = [
  { value: 1, label: '采购入库' },
  { value: 2, label: '调拨入库' },
  { value: 3, label: '生产入库' },
  { value: 4, label: '盘盈入库' },
  { value: 5, label: '其他入库' },
  { value: 6, label: '销售退货' },
  { value: 7, label: '拒收退货' },
]
function typeText(t: number) {
  return typeOptions.find(o => o.value === t)?.label || '-'
}
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
function typeTag(t: number): TagType | undefined {
  const map: Record<number, TagType | undefined> = { 1: undefined, 2: 'success', 3: 'warning', 4: 'info', 5: undefined, 6: 'danger', 7: 'danger' }
  return map[t]
}

const statusOptions = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已提交' },
  { value: 2, label: '已验收' },
  { value: 3, label: '已上架' },
  { value: 4, label: '作废' },
]
function statusText(s: number) {
  return statusOptions.find(o => o.value === s)?.label || '-'
}
function statusTag(s: number): TagType | undefined {
  const map: Record<number, TagType | undefined> = { 0: 'info', 1: undefined, 2: 'warning', 3: 'success', 4: 'danger' }
  return map[s]
}

const loading = ref(false)
const submitting = ref(false)

// ===== 审核弹窗 =====
const auditVisible = ref(false)
const auditRow = ref<any>(null)
const auditFormRef = ref<FormInstance>()
const auditForm = reactive<{ pass: boolean | undefined; remark: string }>({ pass: true, remark: '' })
const auditRules: FormRules = {
  pass: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
  remark: [
    {
      validator: (_rule, value, callback) => {
        if (auditForm.pass === false && (!value || !value.trim())) {
          callback(new Error('审核不通过必须填写原因'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

const tableData = ref<any[]>([])
const total = ref(0)
const warehouseList = ref<any[]>([])
const supplierList = ref<any[]>([])
const skuOptions = ref<any[]>([])
const locationOptions = ref<any[]>([])
const skuLoading = ref(false)

const dateRange = ref<[string, string] | []>([])
const search = reactive({
  stockInNo: '',
  type: undefined as any,
  status: undefined as any,
  startDate: '',
  endDate: '',
  page: 1,
  size: 10,
})

async function loadOptions() {
  try {
    const [w, s] = await Promise.all([warehouseApi.listAll(), supplierApi.listAll()])
    warehouseList.value = (w as any).data || []
    supplierList.value = (s as any).data || []
    const locRes: any = await locationApi.page({ size: 9999, page: 1 })
    const ld = locRes.data || {}
    locationOptions.value = ld.rows || ld.records || ld.list || []
  } catch (e) { /* handled */ }
}

async function loadData() {
  loading.value = true
  try {
    search.startDate = dateRange.value?.[0] || ''
    search.endDate = dateRange.value?.[1] || ''
    const params: Record<string, any> = { ...search }
    if (search.startDate) params.dateRangeStart = search.startDate + ' 00:00:00'
    if (search.endDate) params.dateRangeEnd = search.endDate + ' 23:59:59'
    const res: any = await stockInApi.page(params)
    const d = res.data || {}
    const rows = d.rows || d.records || d.list || []
    // 名称优先用后端冗余字段，缺失时前端按下拉列表反查兜底
    rows.forEach((r: any) => {
      r.warehouseName = r.warehouseName || warehouseList.value.find(w => w.warehouseId == r.warehouseId)?.warehouseName || '-'
      r.supplierName = r.supplierName || supplierList.value.find(s => s.supplierId == r.supplierId)?.supplierName || '-'
    })
    tableData.value = rows
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

async function handleExport() {
  try {
    const params: Record<string, any> = {}
    if (search.stockInNo) params.stockInNo = search.stockInNo
    if (search.type != null) params.type = search.type
    if (search.startDate) params.dateRangeStart = search.startDate + ' 00:00:00'
    if (search.endDate) params.dateRangeEnd = search.endDate + ' 23:59:59'
    const res: any = await stockInApi.export(params)
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `入库明细_${new Date().toISOString().slice(0, 10)}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

function resetSearch() {
  search.stockInNo = ''
  search.type = undefined
  search.status = undefined
  dateRange.value = []
  search.startDate = ''
  search.endDate = ''
  search.page = 1
  loadData()
}

// 表单
const dialogVisible = ref(false)
const dialogTitle = ref('新增入库单')
const formRef = ref<FormInstance>()
const defaultForm = () => ({
  stockInId: undefined as any,
  stockInNo: '',
  type: 1,
  sourceBillNo: '',
  warehouseId: undefined as any,
  supplierId: undefined as any,
  remark: '',
  details: [] as any[],
})
const form = reactive(defaultForm())
const rules: FormRules = {
  stockInNo: [{ required: true, message: '请生成入库单号', trigger: 'blur' }],
  type: [{ required: true, message: '请选择入库类型', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择入库仓库', trigger: 'change' }],
}

function resetForm() {
  Object.assign(form, defaultForm())
  formRef.value?.resetFields()
}

function generateNo() {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  form.stockInNo = `IN${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`
}

function addDetailRow() {
  form.details.push({
    skuId: undefined,
    skuCode: '',
    skuName: '',
    expectedQty: 0,
    actualQty: 0,
    diffQty: 0,
    costPrice: 0,
    batchNo: '',
    produceDate: '',
    expireDate: '',
    locationId: undefined,
  })
}

function removeDetailRow(idx: number) {
  form.details.splice(idx, 1)
}

// 远程搜索商品 SKU，按关键字 + 仓库过滤可入库的商品
async function searchSku(query: string) {
  if (!query) {
    skuOptions.value = []
    return
  }
  skuLoading.value = true
  try {
    const res: any = await goodsSpuApi.searchSku(query, form.warehouseId)
    skuOptions.value = res.data || []
  } catch (e) { /* handled */ } finally {
    skuLoading.value = false
  }
}

// 选中商品后自动回填编码、名称、规格、单位、默认成本价
function onSkuSelect(row: any) {
  const sku = skuOptions.value.find(s => s.skuId === row.skuId)
  if (sku) {
    row.skuCode = sku.skuCode
    row.innerCode = sku.innerCode
    row.skuName = sku.skuName
    row.specText = sku.specText
    row.unitId = sku.unitId
    row.unitName = sku.unitName
    row.costPrice = sku.defaultCost || 0
  }
}

function calcRow(row: any) {
  row.diffQty = Number((Number(row.actualQty || 0) - Number(row.expectedQty || 0)).toFixed(2))
}

async function onSourceBlur() {
  if (!form.sourceBillNo || form.details.length > 0) return
  try {
    const res: any = await stockInApi.fromSource(form.sourceBillNo)
    if (res.data) {
      const data = res.data
      form.warehouseId = data.warehouseId || form.warehouseId
      form.supplierId = data.supplierId || form.supplierId
      if (data.details?.length) {
        form.details = data.details.map((d: any) => ({
          skuId: d.skuId,
          skuCode: d.skuCode,
          skuName: d.skuName,
          unitId: d.unitId,
          unitName: d.unitName,
          expectedQty: d.quantity || d.expectedQty || 0,
          actualQty: 0,
          diffQty: 0,
          costPrice: d.costPrice || 0,
          batchNo: '',
          produceDate: '',
          expireDate: '',
          locationId: undefined,
        }))
      }
    }
  } catch (e) { /* handled */ }
}

function handleAdd() {
  dialogTitle.value = '新增入库单'
  resetForm()
  generateNo()
  dialogVisible.value = true
}

// 编辑入库单：后端返回 {order, items, statusLogs} 嵌套结构，需拆解后赋值给表单
async function handleEdit(row: any) {
  try {
    const res: any = await stockInApi.getById(row.stockInId)
    const data = res.data || {}
    const order = data.order || data // 兼容旧接口直接返回 order 的情况
    Object.assign(form, defaultForm(), order)
    form.details = data.items || order.details || []
    form.stockInId = order.stockInId
    // 把已有明细的商品塞入 skuOptions，使 el-select 能回显商品名称
    skuOptions.value = form.details.map((d: any) => ({ skuId: d.skuId, skuCode: d.skuCode, skuName: d.skuName }))
    dialogTitle.value = '编辑入库单'
    dialogVisible.value = true
  } catch (e) { /* handled */ }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.details.length === 0) {
      ElMessage.warning('请至少添加一行入库明细')
      return
    }
    submitting.value = true
    try {
      const payload = {
        ...form,
        warehouseName: warehouseList.value.find((w: any) => w.warehouseId == form.warehouseId)?.warehouseName || '',
        supplierName: supplierList.value.find((s: any) => s.supplierId == form.supplierId)?.supplierName || '',
        items: form.details,
      }
      if (form.stockInId) {
        await stockInApi.update(payload)
      } else {
        await stockInApi.save(payload)
      }
      ElMessage.success(form.stockInId ? '更新成功' : '新增成功')
      dialogVisible.value = false
      loadData()
    } catch (e) { /* handled */ } finally {
      submitting.value = false
    }
  })
}

async function handleRowAction(row: any, action: string, apiFn: (id: any) => Promise<any>, msg: string) {
  try {
    await ElMessageBox.confirm(msg, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await apiFn(row.stockInId)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch (e) { /* cancelled or error */ }
}

const handleRowSubmit = (row: any) => handleRowAction(row, '提交', (id) => stockInApi.submit(id), `确定提交入库单【${row.stockInNo}】吗？`)
const handleVoid = (row: any) => handleRowAction(row, '作废', (id) => stockInApi.void(id), `确定作废入库单【${row.stockInNo}】吗？作废后不可恢复。`)


function handleAudit(row: any) {
  auditRow.value = row
  auditForm.pass = true
  auditForm.remark = ''
  auditVisible.value = true
}

async function submitAudit() {
  try {
    await auditFormRef.value?.validate()
    if (!auditRow.value) return
    submitting.value = true
    await stockInApi.audit({
      id: auditRow.value.stockInId,
      pass: auditForm.pass,
      remark: auditForm.remark,
    })
    ElMessage.success(auditForm.pass ? '审核通过成功' : '已驳回')
    auditVisible.value = false
    loadData()
  } catch (e) { /* validation cancelled */ } finally {
    submitting.value = false
  }
}

// 反审核：已上架(3) → 已验收(2)，仅上架后 5 分钟内允许（后端校验）
const handleUnaudit = (row: any) => handleRowAction(row, '反审核', (id) => stockInApi.unaudit(id), `确定反审核入库单【${row.stockInNo}】吗？仅上架后5分钟内允许。`)

// 详情
const detailVisible = ref(false)
const detail = reactive<any>({})

// 拉取入库单详情：后端返回 {order, items, statusLogs}，扁平化并反查仓库名/供应商名
async function fetchDetailData(row: any) {
  const res: any = await stockInApi.getById(row.stockInId)
  const data = res.data || {}
  const d: any = { ...data.order, details: data.items || [], logs: data.statusLogs || [] }
  // 订单实体只存 ID，前端根据 ID 从下拉列表反查名称
  d.warehouseName = warehouseList.value.find(w => w.warehouseId == d.warehouseId)?.warehouseName || '-'
  d.supplierName = supplierList.value.find(s => s.supplierId == d.supplierId)?.supplierName || '-'
  return d
}

// 查看入库单详情
async function handleDetail(row: any) {
  try {
    Object.assign(detail, await fetchDetailData(row))
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

/** 列表行直接打印 */
function handlePrint(row: any) {
  openPrint(row.stockInId)
}

/** 详情弹窗内打印 */
function printCurrentDetail() {
  if (detail.stockInId) openPrint(detail.stockInId)
}

onMounted(() => {
  loadOptions()
  loadData()
})
</script>

<style scoped lang="scss">
.detail-section {
  margin-top: 16px;
}
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.detail-title {
  font-size: 14px;
  font-weight: 600;
  color: #303030;
}
.diff-neg {
  color: #f56c6c;
  font-weight: 600;
}
</style>
