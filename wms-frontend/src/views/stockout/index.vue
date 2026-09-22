<template>
  <div class="page-container">
    <!-- 搜索表单 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline size="default">
        <el-form-item label="出库单号">
          <el-input v-model="query.stockOutNo" placeholder="请输入出库单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="出库类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button v-perm="'wms:stock-out:add'" type="primary" :icon="Plus" @click="handleAdd">新增出库单</el-button>
      <el-button v-perm="'wms:stock-out:export'" type="success" :icon="Download" @click="handleExport">导出出库明细</el-button>
      <el-button :icon="RefreshRight" @click="loadData">刷新</el-button>
    </div>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border stripe size="default">
        <el-table-column prop="stockOutNo" label="出库单号" min-width="150" show-overflow-tooltip />
        <el-table-column label="出库类型" width="100">
          <template #default="{ row }">{{ typeMap[row.type] || '-' }}</template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="出库仓库" min-width="120" show-overflow-tooltip />
        <el-table-column prop="totalQty" label="总数量" width="90" align="right" />
        <el-table-column label="总金额" width="120" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.totalSale) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusMap[row.status] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="outByName" label="出库人" width="100" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'wms:stock-out:list'" link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="handlePrint(row)">打印</el-button>
            <el-button v-if="row.status === 0" v-perm="'wms:stock-out:edit'" link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perm="'wms:stock-out:submit'" link type="primary" size="small" @click="handleSubmit(row)">提交</el-button>
            <el-button v-if="row.status === 1" v-perm="'wms:stock-out:lock'" link type="warning" size="small" @click="handleLock(row)">锁定库存</el-button>
            <el-button v-if="row.status === 2" v-perm="'wms:stock-out:pick'" link type="warning" size="small" @click="handlePick(row)">拣货确认</el-button>
            <el-button v-if="row.status === 3" v-perm="'wms:stock-out:audit'" link type="success" size="small" @click="handleAudit(row)">审核出库</el-button>
            <el-button v-if="[0, 1, 2, 3].includes(row.status)" v-perm="'wms:stock-out:void'" link type="danger" size="small" @click="handleVoid(row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="1100px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" size="default">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="出库单号" prop="stockOutNo">
              <el-input v-model="form.stockOutNo" placeholder="自动生成" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出库类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出库仓库" prop="warehouseId">
              <el-select v-model="form.warehouseId" placeholder="请选择" filterable style="width: 100%">
                <el-option v-for="w in warehouseList" :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="客户名称" prop="customerName">
              <el-input v-model="form.customerName" placeholder="请输入客户名称" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="detail-bar">
          <span class="detail-title">出库明细</span>
          <el-button type="primary" size="small" :icon="Plus" @click="addDetailRow">添加行</el-button>
        </div>
        <el-table :data="form.items" border size="small" style="width: 100%">
          <el-table-column label="序号" type="index" width="55" align="center" />
          <el-table-column label="商品" min-width="180">
            <template #default="{ row }">
              <el-select
                v-model="row.skuId"
                placeholder="搜索选择商品"
                filterable
                remote
                :remote-method="(q: string) => searchSku(q)"
                :loading="skuLoading"
                size="small"
                style="width: 100%"
                @change="() => onSkuSelect(row)"
              >
                <el-option v-for="s in skuOptions" :key="s.skuId" :label="s.skuName + (s.skuCode ? ' (' + s.skuCode + ')' : '')" :value="s.skuId" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="skuCode" label="编码" width="120" />
          <el-table-column label="内部编码" prop="innerCode" width="110" />
          <el-table-column label="商品名称" min-width="150" prop="skuName" show-overflow-tooltip />
          <el-table-column label="单位" prop="unitName" width="60" />
          <el-table-column label="应出数量" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.expectedQty" :min="0" :precision="0" size="small" controls-position="right" style="width: 100%" @change="calcSubtotal(row)" />
            </template>
          </el-table-column>
          <el-table-column label="实出数量" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.actualQty" :min="0" :precision="0" size="small" controls-position="right" style="width: 100%" @change="calcSubtotal(row)" />
            </template>
          </el-table-column>
          <el-table-column label="成本价" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.costPrice" :min="0" :precision="2" size="small" controls-position="right" style="width: 100%" @change="calcSubtotal(row)" />
            </template>
          </el-table-column>
          <el-table-column label="销售价" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.salePrice" :min="0" :precision="2" size="small" controls-position="right" style="width: 100%" @change="calcSubtotal(row)" />
            </template>
          </el-table-column>
          <el-table-column label="销售小计" width="110" align="right">
            <template #default="{ row }">¥{{ toFixed2(row.subtotalSale) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="removeDetailRow($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="出库单详情" width="1000px" destroy-on-close>
      <el-descriptions :column="3" border size="default">
        <el-descriptions-item label="出库单号">{{ detail.stockOutNo }}</el-descriptions-item>
        <el-descriptions-item label="出库类型">{{ typeMap[detail.type] || '-' }}</el-descriptions-item>
        <el-descriptions-item label="出库仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ detail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="出库人">{{ detail.outByName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)">{{ statusMap[detail.status] || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总数量">{{ detail.totalQty }}</el-descriptions-item>
        <el-descriptions-item label="成本总额">¥{{ toFixed2(detail.totalCost) }}</el-descriptions-item>
<!--        <el-descriptions-item label="销售总额">¥{{ toFixed2(detail.totalSale) }}</el-descriptions-item>-->
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-section-title">出库明细</div>
      <el-table :data="detail.items || []" border size="small">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="skuCode" label="商品编码" min-width="120" show-overflow-tooltip />
        <el-table-column label="内部编码" prop="innerCode" width="110" />
        <el-table-column prop="skuName" label="商品名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="单位" prop="unitName" width="60" />
        <el-table-column label="应出数量" width="90" align="right" prop="expectedQty" />
        <el-table-column label="实出数量" width="90" align="right" prop="actualQty" />
        <el-table-column label="成本价" width="110" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.costPrice) }}</template>
        </el-table-column>
        <el-table-column label="销售价" width="110" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.salePrice) }}</template>
        </el-table-column>
        <el-table-column label="成本小计" width="110" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.subtotalCost) }}</template>
        </el-table-column>
        <el-table-column label="销售小计" width="110" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.subtotalSale) }}</template>
        </el-table-column>
      </el-table>

      <div class="detail-section-title">状态日志</div>
      <el-timeline>
        <el-timeline-item v-for="(log, idx) in detail.logs || []" :key="idx" :timestamp="log.operateTime" placement="top">
          <h4>{{ statusMap[log.toStatus] || '-' }}</h4>
          <p>{{ log.operateName || log.operateBy }} - {{ log.remark || '' }}</p>
        </el-timeline-item>
      </el-timeline>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="printCurrentDetail">打印出库单</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" title="审核出库单" width="480px" :close-on-click-modal="false">
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
        <el-form-item label="单据号">
          <span>{{ auditRow?.stockOutNo || '-' }}</span>
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
defineOptions({ name: 'StockOut' })
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, RefreshRight, Download } from '@element-plus/icons-vue'
import { stockOutApi, warehouseApi, customerApi, goodsSpuApi } from '@/api'

const router = useRouter()

/** 在新标签页打开单据打印页 */
function openPrint(stockOutId: string) {
  const url = router.resolve({ name: 'BillPrint', params: { billType: 'stock-out', id: stockOutId } }).href
  window.open(url, '_blank')
}

const typeOptions = [
  { value: 1, label: '销售出库' },
  { value: 2, label: '调拨出库' },
  { value: 3, label: '退货出库' },
  { value: 4, label: '其他出库' },
]
const typeMap: Record<number, string> = { 1: '销售出库', 2: '调拨出库', 3: '退货出库', 4: '其他出库' }

const statusOptions = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已提交' },
  { value: 2, label: '已锁定' },
  { value: 3, label: '已拣货' },
  { value: 4, label: '已出库' },
  { value: 5, label: '作废' },
]
const statusMap: Record<number, string> = { 0: '草稿', 1: '已提交', 2: '已锁定', 3: '已拣货', 4: '已出库', 5: '作废' }
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
const statusTagMap: Record<number, TagType | undefined> = { 0: 'info', 1: 'warning', 2: 'primary', 3: 'warning', 4: 'success', 5: 'danger' }

function statusTagType(status: number): TagType | undefined {
  return statusTagMap[status]
}

function toFixed2(n: any): string {
  return Number(n || 0).toFixed(2)
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
const customerList = ref<any[]>([])
const dateRange = ref<string[]>([])

const query = reactive({
  stockOutNo: '',
  type: undefined as number | undefined,
  status: undefined as number | undefined,
  startDate: '',
  endDate: '',
  page: 1,
  size: 10,
})

const formVisible = ref(false)
const formTitle = ref('新增出库单')
const formRef = ref<FormInstance>()
const isEdit = ref(false)

const initForm = () => ({
  stockOutId: undefined as string | undefined,
  stockOutNo: '',
  type: undefined as number | undefined,
  warehouseId: undefined as string | undefined,
  customerName: '',
  remark: '',
  items: [] as any[],
})

const form = reactive(initForm())

const formRules: FormRules = {
  stockOutNo: [{ required: true, message: '请输入出库单号', trigger: 'blur' }],
  type: [{ required: true, message: '请选择出库类型', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择出库仓库', trigger: 'change' }],
}

const detailVisible = ref(false)
const detail = ref<any>({})

onMounted(() => {
  loadWarehouse()
  loadData()
})

async function loadWarehouse() {
  try {
    const [w, c] = await Promise.all([warehouseApi.listAll(), customerApi.listAll()])
    warehouseList.value = (w as any).data || []
    customerList.value = (c as any).data || []
  } catch (e) { /* ignore */ }
}

async function loadData() {
  loading.value = true
  try {
    query.startDate = dateRange.value?.[0] || ''
    query.endDate = dateRange.value?.[1] || ''
    const params: Record<string, any> = { ...query }
    if (query.startDate) params.dateRangeStart = query.startDate + ' 00:00:00'
    if (query.endDate) params.dateRangeEnd = query.endDate + ' 23:59:59'
    const res: any = await stockOutApi.page(params)
    const rows = res.data?.rows || res.data?.records || []
    // 名称优先用后端冗余字段，缺失时前端按下拉列表反查兜底
    rows.forEach((r: any) => {
      r.warehouseName = r.warehouseName || warehouseList.value.find(w => w.warehouseId == r.warehouseId)?.warehouseName || '-'
      r.customerName = r.customerName || customerList.value.find(c => c.customerId == r.customerId)?.customerName || '-'
    })
    tableData.value = rows
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('加载列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadData()
}

function handleReset() {
  query.stockOutNo = ''
  query.type = undefined
  query.status = undefined
  dateRange.value = []
  query.startDate = ''
  query.endDate = ''
  query.page = 1
  loadData()
}

const exporting = ref(false)
async function handleExport() {
  exporting.value = true
  try {
    const params: Record<string, any> = {}
    if (query.stockOutNo) params.stockOutNo = query.stockOutNo
    if (query.type != null) params.type = query.type
    if (query.startDate) params.dateRangeStart = query.startDate + ' 00:00:00'
    if (query.endDate) params.dateRangeEnd = query.endDate + ' 23:59:59'
    const res: any = await stockOutApi.export(params)
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `出库明细_${new Date().toISOString().slice(0, 10)}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  formTitle.value = '新增出库单'
  Object.assign(form, initForm())
  // 预生成出库单号
  stockOutApi.generateNo().then((res: any) => {
    if (res?.data) form.stockOutNo = res.data
  }).catch(() => {
    // 接口失败也不阻塞，用户可以手动填
  })
  formVisible.value = true
}

// 编辑出库单：后端返回 {order, items, statusLogs} 嵌套结构，需拆解后赋值给表单
async function handleEdit(row: any) {
  isEdit.value = true
  formTitle.value = '编辑出库单'
  try {
    const res: any = await stockOutApi.getById(row.stockOutId)
    const data = res.data || {}
    const order = data.order || data // 兼容旧接口直接返回 order 的情况
    Object.assign(form, order)
    // 后端明细字段 expectedQty/actualQty/subtotalSale/subtotalCost 与前端一致，直接使用
    form.items = (data.items || order.items || []).map((d: any) => ({ ...d }))
    form.items.forEach((it: any) => calcSubtotal(it))
    form.stockOutId = order.stockOutId
    // 把已有明细的商品塞入 skuOptions，使 el-select 能回显商品名称
    skuOptions.value = form.items.map((d: any) => ({ skuId: d.skuId, skuCode: d.skuCode, skuName: d.skuName }))
    formVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

function addDetailRow() {
  form.items.push({ skuId: undefined, skuCode: '', skuName: '', expectedQty: 0, actualQty: 0, costPrice: 0, salePrice: 0, subtotalSale: 0, subtotalCost: 0 })
}

const skuOptions = ref<any[]>([])
const skuLoading = ref(false)

// 远程搜索商品 SKU，按关键字 + 仓库过滤可出库的商品
async function searchSku(query: string) {
  if (!query) { skuOptions.value = []; return }
  skuLoading.value = true
  try {
    const res: any = await goodsSpuApi.searchSku(query, form.warehouseId)
    skuOptions.value = res.data || []
  } catch (e) { /* handled */ } finally {
    skuLoading.value = false
  }
}

// 选中商品后自动回填编码、名称、单位、默认成本价和销售价
function onSkuSelect(row: any) {
  const sku = skuOptions.value.find(s => s.skuId === row.skuId)
  if (sku) {
    row.skuCode = sku.skuCode
    row.innerCode = sku.innerCode
    row.skuName = sku.skuName
    row.unitId = sku.unitId
    row.unitName = sku.unitName
    row.costPrice = sku.defaultCost || 0
    row.salePrice = sku.defaultPrice || 0
    calcSubtotal(row)
  }
}

function removeDetailRow(idx: number) {
  form.items.splice(idx, 1)
}

// 出库数量以实际为准，未填实际时取预期；小计 = 数量 × 单价
function calcSubtotal(row: any) {
  const qty = Number(row.actualQty || row.expectedQty || 0)
  row.subtotalSale = qty * Number(row.salePrice || 0)
  row.subtotalCost = qty * Number(row.costPrice || 0)
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!form.items.length) {
      ElMessage.warning('请至少添加一条明细')
      return
    }
    submitting.value = true
    try {
      const items = form.items.map((it: any) => {
        const qty = Number(it.actualQty || it.expectedQty || 0)
        return { ...it, subtotalSale: qty * Number(it.salePrice || 0), subtotalCost: qty * Number(it.costPrice || 0) }
      })
      const payload = {
        ...form,
        warehouseName: warehouseList.value.find((w: any) => w.warehouseId == form.warehouseId)?.warehouseName || '',
        totalQty: items.reduce((s: number, it: any) => s + Number(it.actualQty || it.expectedQty || 0), 0),
        totalAmount: items.reduce((s: number, it: any) => s + Number(it.subtotalSale || 0), 0),
        items,
      }
      if (isEdit.value) {
        await stockOutApi.update(payload)
      } else {
        await stockOutApi.save(payload)
      }
      ElMessage.success('保存成功')
      formVisible.value = false
      loadData()
    } catch (e) {
      ElMessage.error('保存失败')
    } finally {
      submitting.value = false
    }
  })
}

// 拉取出库单详情：后端返回 {order, items, statusLogs}，扁平化并反查仓库名/客户名
async function fetchDetailData(row: any) {
  const res: any = await stockOutApi.getById(row.stockOutId)
  const data = res.data || {}
  const d: any = { ...data.order, items: data.items || [], logs: data.statusLogs || [] }
  // 名称优先用后端冗余字段，缺失时前端按 ID 从下拉列表反查兜底
  d.warehouseName = d.warehouseName || warehouseList.value.find(w => w.warehouseId == d.warehouseId)?.warehouseName || '-'
  d.customerName = d.customerName || customerList.value.find(c => c.customerId == d.customerId)?.customerName || '-'
  return d
}

async function handleDetail(row: any) {
  try {
    detail.value = await fetchDetailData(row)
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

/** 列表行直接打印 */
function handlePrint(row: any) {
  openPrint(row.stockOutId)
}

/** 详情弹窗内打印 */
function printCurrentDetail() {
  if (detail.value.stockOutId) openPrint(detail.value.stockOutId)
}

async function handleSubmit(row: any) {
  try {
    await ElMessageBox.confirm(`确认提交出库单【${row.stockOutNo}】吗？`, '提示', { type: 'warning' })
    await stockOutApi.submit(row.stockOutId)
    ElMessage.success('提交成功')
    loadData()
  } catch (e) { /* cancelled */ }
}


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
    await stockOutApi.audit({
      id: auditRow.value.stockOutId,
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

async function handleVoid(row: any) {
  try {
    await ElMessageBox.confirm(`确认作废出库单【${row.stockOutNo}】吗？作废后不可恢复。`, '提示', { type: 'warning' })
    await stockOutApi.void(row.stockOutId)
    ElMessage.success('作废成功')
    loadData()
  } catch (e) { /* cancelled */ }
}

// 锁定库存：已提交(1) → 已锁定(2)
async function handleLock(row: any) {
  try {
    await ElMessageBox.confirm(`确认锁定出库单【${row.stockOutNo}】的库存吗？`, '提示', { type: 'warning' })
    await stockOutApi.lockInventory(row.stockOutId)
    ElMessage.success('库存锁定成功')
    loadData()
  } catch (e) { /* cancelled */ }
}

// 拣货确认：已锁定(2) → 已拣货(3)
async function handlePick(row: any) {
  try {
    await ElMessageBox.confirm(`确认出库单【${row.stockOutNo}】拣货完成吗？`, '提示', { type: 'warning' })
    await stockOutApi.pickConfirm(row.stockOutId)
    ElMessage.success('拣货确认成功')
    loadData()
  } catch (e) { /* cancelled */ }
}
</script>

<style scoped lang="scss">
.search-card { margin-bottom: 12px; }
.action-bar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
.detail-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16px 0 8px;
  .detail-title { font-size: 14px; font-weight: 600; }
}
.detail-section-title {
  font-size: 14px;
  font-weight: 600;
  margin: 20px 0 10px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}
</style>
