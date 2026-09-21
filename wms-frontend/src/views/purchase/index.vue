<template>
  <div class="page-container">
    <!-- 搜索 -->
    <el-form :model="search" inline class="search-form">
      <el-form-item label="关键字">
        <el-input v-model="search.keyword" placeholder="采购单号/供应商名称" clearable style="width: 220px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="search.status" placeholder="全部状态" clearable style="width: 140px">
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
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增采购单</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column type="index" label="#" width="55" align="center" />
      <el-table-column prop="purchaseNo" label="采购单号" min-width="150" show-overflow-tooltip />
      <el-table-column prop="supplierName" label="供应商" min-width="160" show-overflow-tooltip />
      <el-table-column prop="warehouseName" label="入库仓库" min-width="130" show-overflow-tooltip />
      <el-table-column prop="totalQty" label="总数量" width="100" align="right" />
      <el-table-column label="总金额" width="130" align="right">
        <template #default="{ row }">
          ¥{{ Number(row.totalAmount || 0).toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="expectDate" label="预计到货" width="110" align="center" />
      <el-table-column prop="createName" label="创建人" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="160" align="center" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click="handleDetail(row)">详情</el-button>
          <el-button link type="primary" :icon="Printer" @click="handlePrint(row)">打印</el-button>
          <el-button v-if="row.status === 0" link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 0" link type="success" @click="handleRowSubmit(row)">提交</el-button>
          <el-button v-if="row.status === 1" link type="warning" :icon="Check" @click="handleAudit(row)">审核</el-button>
          <el-button v-if="row.status === 2" link type="info" @click="handleUnaudit(row)">反审核</el-button>
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
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="1100px" top="5vh" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="采购单号" prop="purchaseNo">
              <el-input v-model="form.purchaseNo" placeholder="自动生成" readonly>
                <template #append>
                  <el-button @click="generateNo">生成</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="form.supplierId" placeholder="请选择供应商" filterable style="width: 100%">
                <el-option v-for="s in supplierList" :key="s.supplierId" :label="s.supplierName" :value="s.supplierId" />
              </el-select>
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
            <el-form-item label="预计到货" prop="expectDate">
              <el-date-picker v-model="form.expectDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="税率(%)" prop="taxRate">
              <el-input-number v-model="form.taxRate" :min="0" :max="100" :precision="2" style="width: 100%" @change="calcSummary" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="运费" prop="freight">
              <el-input-number v-model="form.freight" :min="0" :precision="2" style="width: 100%" @change="calcSummary" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="折扣率(%)" prop="discountRate">
              <el-input-number v-model="form.discountRate" :min="0" :max="100" :precision="2" style="width: 100%" @change="calcSummary" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="其他费用" prop="otherAmount">
              <el-input-number v-model="form.otherAmount" :min="0" :precision="2" style="width: 100%" @change="calcSummary" />
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
          <span class="detail-title">采购明细</span>
          <el-button type="primary" size="small" :icon="Plus" @click="addDetailRow">添加行</el-button>
        </div>
        <el-table :data="form.details" border size="small" style="width: 100%">
          <el-table-column type="index" label="#" width="45" align="center" />
          <el-table-column label="商品" min-width="200">
            <template #default="{ row }">
              <el-select
                v-model="row.skuId"
                placeholder="搜索选择商品"
                filterable
                remote
                :remote-method="(q: string) => searchSku(q, row)"
                :loading="skuLoading"
                style="width: 100%"
                @change="onSkuSelect(row)"
              >
                <el-option v-for="s in skuOptions" :key="s.skuId" :label="s.skuName + (s.skuCode ? ' (' + s.skuCode + ')' : '')" :value="s.skuId" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="skuCode" label="编码" width="120" />
          <el-table-column label="内部编码" prop="innerCode" width="110" />
          <el-table-column prop="skuName" label="名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="specText" label="规格" width="120" show-overflow-tooltip />
          <el-table-column prop="unitName" label="单位" width="70" align="center" />
          <el-table-column label="数量" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="0" :precision="2" size="small" style="width: 100%" @change="calcRowSubtotal(row)" />
            </template>
          </el-table-column>
          <el-table-column label="采购价" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.purchasePrice" :min="0" :precision="4" size="small" style="width: 100%" @change="calcRowSubtotal(row)" />
            </template>
          </el-table-column>
          <el-table-column label="税率(%)" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.taxRate" :min="0" :max="100" :precision="2" size="small" style="width: 100%" @change="calcRowSubtotal(row)" />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="110" align="right">
            <template #default="{ row }">
              ¥{{ Number(row.subtotal || 0).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="预计到货" width="140">
            <template #default="{ row }">
              <el-date-picker v-model="row.expectDate" type="date" value-format="YYYY-MM-DD" size="small" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="备注" width="140">
            <template #default="{ row }">
              <el-input v-model="row.remark" size="small" placeholder="备注" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" :icon="Delete" @click="removeDetailRow($index)" />
            </template>
          </el-table-column>
        </el-table>

        <!-- 汇总 -->
        <div class="summary-bar">
          <div class="summary-item"><span>总数量：</span><b>{{ summary.totalQty }}</b></div>
          <div class="summary-item"><span>明细合计：</span><b>¥{{ summary.subtotal.toFixed(2) }}</b></div>
          <div class="summary-item"><span>税额：</span><b>¥{{ summary.taxAmount.toFixed(2) }}</b></div>
          <div class="summary-item total"><span>总金额：</span><b>¥{{ summary.totalAmount.toFixed(2) }}</b></div>
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog title="采购单详情" v-model="detailVisible" width="1000px" top="5vh">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="采购单号">{{ detail.purchaseNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detail.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="入库仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="预计到货">{{ detail.expectDate }}</el-descriptions-item>
        <el-descriptions-item label="税率(%)">{{ detail.taxRate }}</el-descriptions-item>
        <el-descriptions-item label="运费">¥{{ Number(detail.freight || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="折扣率(%)">{{ detail.discountRate }}</el-descriptions-item>
        <el-descriptions-item label="其他费用">¥{{ Number(detail.otherAmount || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTag(detail.status)">{{ statusText(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总数量">{{ detail.totalQty }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ Number(detail.totalAmount || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ detail.createName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ detail.auditName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-section">
        <div class="detail-title">采购明细</div>
        <el-table :data="detail.details" border size="small">
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column prop="skuCode" label="编码" width="120" />
          <el-table-column label="内部编码" prop="innerCode" width="110" />
          <el-table-column prop="skuName" label="名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="specText" label="规格" width="120" show-overflow-tooltip />
          <el-table-column prop="unitName" label="单位" width="70" align="center" />
          <el-table-column prop="quantity" label="数量" width="90" align="right" />
          <el-table-column label="采购价" width="100" align="right">
            <template #default="{ row }">¥{{ Number(row.purchasePrice || 0).toFixed(4) }}</template>
          </el-table-column>
          <el-table-column prop="taxRate" label="税率(%)" width="90" align="center" />
          <el-table-column label="小计" width="110" align="right">
            <template #default="{ row }">¥{{ Number(row.subtotal || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="expectDate" label="预计到货" width="110" align="center" />
        </el-table>
      </div>

      <!-- 状态日志时间线 -->
      <div class="detail-section">
        <div class="detail-title">状态日志</div>
        <el-timeline>
          <el-timeline-item
            v-for="(log, i) in detail.logs"
            :key="i"
            :timestamp="log.operateTime"
            placement="top"
            :type="logType(log.toStatus)"
          >
            <h4>{{ statusText(log.toStatus) }}</h4>
            <p>{{ log.operateBy }} - {{ log.remark || '' }}</p>
          </el-timeline-item>
        </el-timeline>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" :icon="Printer" @click="handlePrint(detail)">打印</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" title="审核采购单" width="480px" :close-on-click-modal="false">
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
        <el-form-item label="单据号">
          <span>{{ auditRow?.purchaseNo || '-' }}</span>
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
import { Search, Refresh, Plus, Edit, Delete, View, Check, Printer } from '@element-plus/icons-vue'
import { purchaseApi, warehouseApi, supplierApi, goodsSpuApi } from '@/api'

const statusOptions = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已提交' },
  { value: 2, label: '已审核' },
  { value: 3, label: '部分到货' },
  { value: 4, label: '已完成' },
  { value: 5, label: '已作废' },
]
function statusText(s: number) {
  return statusOptions.find(o => o.value === s)?.label || '-'
}
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
function statusTag(s: number): TagType | undefined {
  const map: Record<number, TagType | undefined> = { 0: 'info', 1: undefined, 2: 'success', 3: 'warning', 4: 'success', 5: 'danger' }
  return map[s]
}
function logType(s: number) {
  return s === 5 ? 'danger' : s === 4 ? 'success' : 'primary'
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
const skuLoading = ref(false)

const dateRange = ref<[string, string] | []>([])
const search = reactive({
  keyword: '',
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
  } catch (e) { /* handled */ }
}

async function loadData() {
  loading.value = true
  try {
    search.startDate = dateRange.value?.[0] || ''
    search.endDate = dateRange.value?.[1] || ''
    const res: any = await purchaseApi.page(search)
    const d = res.data || {}
    const rows = d.rows || d.records || d.list || []
    // 后端分页只返回 warehouseId/supplierId，前端根据下拉列表反查名称
    rows.forEach((r: any) => {
      r.warehouseName = warehouseList.value.find(w => w.warehouseId == r.warehouseId)?.warehouseName || '-'
      r.supplierName = supplierList.value.find(s => s.supplierId == r.supplierId)?.supplierName || '-'
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

function resetSearch() {
  search.keyword = ''
  search.status = undefined
  dateRange.value = []
  search.startDate = ''
  search.endDate = ''
  search.page = 1
  loadData()
}

// 表单
const dialogVisible = ref(false)
const dialogTitle = ref('新增采购单')
const formRef = ref<FormInstance>()
const defaultForm = () => ({
  purchaseId: undefined as any,
  purchaseNo: '',
  supplierId: undefined as any,
  warehouseId: undefined as any,
  expectDate: '',
  taxRate: 13,
  freight: 0,
  discountRate: 0,
  otherAmount: 0,
  remark: '',
  details: [] as any[],
})
const form = reactive(defaultForm())
const rules: FormRules = {
  purchaseNo: [{ required: true, message: '请生成采购单号', trigger: 'blur' }],
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择入库仓库', trigger: 'change' }],
  expectDate: [{ required: true, message: '请选择预计到货日期', trigger: 'change' }],
}
const summary = reactive({ totalQty: 0, subtotal: 0, taxAmount: 0, totalAmount: 0 })

function resetForm() {
  Object.assign(form, defaultForm())
  Object.assign(summary, { totalQty: 0, subtotal: 0, taxAmount: 0, totalAmount: 0 })
  formRef.value?.resetFields()
}

function generateNo() {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  form.purchaseNo = `PO${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`
}

function addDetailRow() {
  if (!form.supplierId) {
    ElMessage.warning('请先选择供应商，再添加商品明细')
    return
  }
  form.details.push({
    skuId: undefined,
    skuCode: '',
    skuName: '',
    specText: '',
    unitName: '',
    quantity: 1,
    purchasePrice: 0,
    taxRate: form.taxRate,
    subtotal: 0,
    expectDate: form.expectDate,
    remark: '',
  })
}

function removeDetailRow(idx: number) {
  form.details.splice(idx, 1)
  calcSummary()
}

async function searchSku(query: string, _row: any) {
  // 采购单已选供应商时，只能选择该供应商名下的商品
  const supplierId = form.supplierId ? String(form.supplierId) : undefined
  if (!query && !supplierId) {
    skuOptions.value = []
    return
  }
  if (!supplierId) {
    ElMessage.warning('请先选择供应商，再添加商品明细')
    skuOptions.value = []
    return
  }
  skuLoading.value = true
  try {
    const res: any = await goodsSpuApi.searchSku(query, form.warehouseId, supplierId)
    skuOptions.value = res.data || []
  } catch (e) { /* handled */ } finally {
    skuLoading.value = false
  }
}

function onSkuSelect(row: any) {
  const sku = skuOptions.value.find(s => s.skuId === row.skuId)
  if (sku) {
    row.skuCode = sku.skuCode
    row.innerCode = sku.innerCode
    row.skuName = sku.skuName
    row.specText = sku.specText
    row.unitName = sku.unitName
    row.purchasePrice = sku.defaultCost || 0
    calcRowSubtotal(row)
  }
}

function calcRowSubtotal(row: any) {
  const qty = Number(row.quantity || 0)
  const price = Number(row.purchasePrice || 0)
  row.subtotal = Number((qty * price).toFixed(2))
  calcSummary()
}

function calcSummary() {
  let totalQty = 0
  let subtotal = 0
  let taxAmount = 0
  form.details.forEach(row => {
    const qty = Number(row.quantity || 0)
    const price = Number(row.purchasePrice || 0)
    const rate = Number(row.taxRate || 0) / 100
    const lineSubtotal = Number((qty * price).toFixed(2))
    const lineTax = Number((lineSubtotal * rate).toFixed(2))
    row.subtotal = lineSubtotal
    totalQty += qty
    subtotal += lineSubtotal
    taxAmount += lineTax
  })
  const discount = subtotal * (Number(form.discountRate || 0) / 100)
  const freight = Number(form.freight || 0)
  const other = Number(form.otherAmount || 0)
  const totalAmount = subtotal - discount + freight + other + taxAmount
  Object.assign(summary, {
    totalQty: Number(totalQty.toFixed(2)),
    subtotal: Number(subtotal.toFixed(2)),
    taxAmount: Number(taxAmount.toFixed(2)),
    totalAmount: Number(totalAmount.toFixed(2)),
  })
}

function handleAdd() {
  dialogTitle.value = '新增采购单'
  resetForm()
  generateNo()
  dialogVisible.value = true
}

// 编辑采购单：后端返回 {order, items, statusLogs} 嵌套结构，需拆解后赋值给表单
async function handleEdit(row: any) {
  try {
    const res: any = await purchaseApi.getById(row.purchaseId)
    const data = res.data || {}
    const order = data.order || data // 兼容旧接口直接返回 order 的情况
    Object.assign(form, defaultForm(), order)
    form.details = data.items || order.details || []
    form.purchaseId = order.purchaseId
    // 把已有明细的商品塞入 skuOptions，使 el-select 能回显商品名称
    skuOptions.value = form.details.map((d: any) => ({ skuId: d.skuId, skuCode: d.skuCode, skuName: d.skuName }))
    calcSummary()
    dialogTitle.value = '编辑采购单'
    dialogVisible.value = true
  } catch (e) { /* handled */ }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.details.length === 0) {
      ElMessage.warning('请至少添加一行采购明细')
      return
    }
    submitting.value = true
    try {
      const payload = { ...form, items: form.details, totalQty: summary.totalQty, subtotal: summary.subtotal, taxAmount: summary.taxAmount, totalAmount: summary.totalAmount }
      if (form.purchaseId) {
        await purchaseApi.update(payload)
      } else {
        await purchaseApi.save(payload)
      }
      ElMessage.success(form.purchaseId ? '更新成功' : '新增成功')
      dialogVisible.value = false
      loadData()
    } catch (e) { /* handled */ } finally {
      submitting.value = false
    }
  })
}

async function handleStatusChange(row: any, action: string, apiFn: (id: any) => Promise<any>, msg: string) {
  try {
    await ElMessageBox.confirm(msg, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await apiFn(row.purchaseId)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch (e) { /* cancelled or error */ }
}

const handleRowSubmit = (row: any) => handleStatusChange(row, '提交', (id) => purchaseApi.submit(id), `确定提交采购单【${row.purchaseNo}】吗？`)
const handleUnaudit = (row: any) => handleStatusChange(row, '反审核', (id) => purchaseApi.unaudit(id), `确定反审核采购单【${row.purchaseNo}】吗？`)
const handleVoid = (row: any) => handleStatusChange(row, '作废', (id) => purchaseApi.void(id), `确定作废采购单【${row.purchaseNo}】吗？作废后不可恢复。`)


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
    await purchaseApi.audit({
      id: auditRow.value.purchaseId,
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

// 详情
const detailVisible = ref(false)
const detail = reactive<any>({})

// 查看采购单详情：后端返回 {order, items, statusLogs}，扁平化后赋值给 detail
// 同时根据 supplierId/warehouseId 查找对应的名称用于展示
async function handleDetail(row: any) {
  try {
    const res: any = await purchaseApi.getById(row.purchaseId)
    const data = res.data || {}
    Object.assign(detail, data.order || {})
    detail.details = data.items || []
    detail.logs = data.statusLogs || []
    // 订单实体只存 ID，前端根据 ID 从下拉列表反查名称
    detail.supplierName = supplierList.value.find(s => s.supplierId == detail.supplierId)?.supplierName || '-'
    detail.warehouseName = warehouseList.value.find(w => w.warehouseId == detail.warehouseId)?.warehouseName || '-'
    detailVisible.value = true
  } catch (e) { /* handled */ }
}

const router = useRouter()
function handlePrint(row: any) {
  const { href } = router.resolve({ path: `/print/purchase/${row.purchaseId}` })
  window.open(href, '_blank')
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
.summary-bar {
  display: flex;
  justify-content: flex-end;
  gap: 24px;
  padding: 12px 16px;
  margin-top: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  .summary-item {
    font-size: 13px;
    color: #606266;
    b { color: #303030; }
    &.total {
      b { color: #f56c6c; font-size: 16px; }
    }
  }
}
</style>
