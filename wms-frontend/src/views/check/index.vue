<template>
  <div class="page-container">
    <!-- 搜索表单 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline size="default">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="盘点单号/仓库名称" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="盘点类型">
          <el-select v-model="query.checkType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in checkTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button v-perm="'wms:check:add'" type="primary" :icon="Plus" @click="handleAdd">新增盘点单</el-button>
      <el-button :icon="RefreshRight" @click="loadData">刷新</el-button>
    </div>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border stripe size="default">
        <el-table-column prop="checkNo" label="盘点单号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" min-width="120" show-overflow-tooltip />
        <el-table-column prop="areaName" label="库区" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">{{ row.areaName || '-' }}</template>
        </el-table-column>
        <el-table-column label="盘点类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ checkTypeMap[row.checkType] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalSkuCount" label="SKU数" width="80" align="right" />
        <el-table-column label="盘盈数量" width="90" align="right">
          <template #default="{ row }">
            <span :class="{ 'profit-text': row.profitQty > 0 }">{{ row.profitQty || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="盘亏数量" width="90" align="right">
          <template #default="{ row }">
            <span :class="{ 'loss-text': row.lossQty > 0 }">{{ row.lossQty || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="盘盈金额" width="110" align="right">
          <template #default="{ row }">
            <span :class="{ 'profit-text': row.profitAmount > 0 }">¥{{ toFixed2(row.profitAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="盘亏金额" width="110" align="right">
          <template #default="{ row }">
            <span :class="{ 'loss-text': row.lossAmount > 0 }">¥{{ toFixed2(row.lossAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusMap[row.status] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkerName" label="盘点人" width="100" show-overflow-tooltip />
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'wms:check:list'" link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" v-perm="'wms:check:add'" link type="primary" size="small" @click="handleLoadInventory(row)">加载库存</el-button>
            <el-button v-if="row.status === 0" v-perm="'wms:check:submit'" link type="warning" size="small" @click="handleStartCheck(row)">开始盘点</el-button>
            <el-button v-if="row.status === 1" v-perm="'wms:check:edit'" link type="primary" size="small" @click="handleInputActual(row)">录入实盘</el-button>
            <el-button v-if="row.status === 1" v-perm="'wms:check:submit'" link type="success" size="small" @click="handleFinishCheck(row)">完成盘点</el-button>
            <el-button v-if="row.status === 2" v-perm="'wms:check:audit'" link type="warning" size="small" @click="handleAudit(row)">审核</el-button>
            <el-button v-if="row.status === 3" v-perm="'wms:check:handle'" link type="success" size="small" @click="handleProcess(row)">处理</el-button>
            <el-button v-if="[0, 1, 2, 3].includes(row.status)" v-perm="'wms:check:void'" link type="danger" size="small" @click="handleVoid(row)">作废</el-button>
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

    <!-- 新增弹窗 -->
    <el-dialog v-model="formVisible" title="新增盘点单" width="700px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" size="default">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="盘点单号" prop="checkNo">
              <el-input v-model="form.checkNo" placeholder="请输入盘点单号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="盘点类型" prop="checkType">
              <el-select v-model="form.checkType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in checkTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="仓库" prop="warehouseId">
              <el-select v-model="form.warehouseId" placeholder="请选择" filterable style="width: 100%" @change="onWarehouseChange">
                <el-option v-for="w in warehouseList" :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库区">
              <el-select v-model="form.areaId" placeholder="请选择(可空)" clearable filterable style="width: 100%">
                <el-option v-for="a in areaList" :key="a.areaId" :label="a.areaName" :value="a.areaId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="盘点日期" prop="checkDate">
              <el-date-picker v-model="form.checkDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 录入实盘大弹窗 -->
    <el-dialog v-model="inputVisible" title="录入实盘数量" width="1300px" :close-on-click-modal="false" destroy-on-close>
      <div class="input-header">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="盘点单号">{{ inputRow.checkNo }}</el-descriptions-item>
          <el-descriptions-item label="仓库">{{ inputRow.warehouseName }}</el-descriptions-item>
          <el-descriptions-item label="盘点类型">{{ checkTypeMap[inputRow.checkType] || '-' }}</el-descriptions-item>
          <el-descriptions-item label="盘点人">{{ inputRow.checkerName || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div class="input-summary">
        <span>共 <b>{{ inputItems.length }}</b> 条明细</span>
        <span>盘盈合计：<b class="profit-text">{{ summaryProfitQty }}</b> 条 / <b class="profit-text">¥{{ toFixed2(summaryProfitAmount) }}</b></span>
        <span>盘亏合计：<b class="loss-text">{{ summaryLossQty }}</b> 条 / <b class="loss-text">¥{{ toFixed2(summaryLossAmount) }}</b></span>
      </div>
      <el-table :data="inputItems" border size="small" v-loading="inputLoading" max-height="500">
        <el-table-column type="index" label="#" width="55" align="center" fixed />
        <el-table-column prop="skuCode" label="商品编码" min-width="120" show-overflow-tooltip fixed />
        <el-table-column label="内部编码" prop="innerCode" width="110" />
        <el-table-column prop="skuName" label="商品名称" min-width="160" show-overflow-tooltip fixed />
        <el-table-column prop="batchNo" label="批次号" width="120" show-overflow-tooltip />
        <el-table-column prop="locationId" label="库位" width="110" show-overflow-tooltip />
        <el-table-column label="账面数量" width="100" align="right">
          <template #default="{ row }">{{ row.bookQty }}</template>
        </el-table-column>
        <el-table-column label="实盘数量" width="140">
          <template #default="{ row }">
            <el-input-number
              v-model="row.actualQty"
              :min="0"
              :precision="0"
              size="small"
              controls-position="right"
              style="width: 100%"
              @change="calcDiff(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="差异数量" width="100" align="right">
          <template #default="{ row }">
            <span :class="row.diffQty > 0 ? 'profit-text' : (row.diffQty < 0 ? 'loss-text' : '')">{{ row.diffQty }}</span>
          </template>
        </el-table-column>
        <el-table-column label="成本价" width="100" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.costPrice) }}</template>
        </el-table-column>
        <el-table-column label="差异金额" width="120" align="right">
          <template #default="{ row }">
            <span :class="row.diffAmount > 0 ? 'profit-text' : (row.diffAmount < 0 ? 'loss-text' : '')">¥{{ toFixed2(row.diffAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" width="160">
          <template #default="{ row }">
            <el-input v-model="row.remark" size="small" placeholder="备注" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="inputVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSaveActual">保存实盘</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="盘点单详情" width="1100px" destroy-on-close>
      <el-descriptions :column="3" border size="default">
        <el-descriptions-item label="盘点单号">{{ detail.checkNo }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="库区">{{ detail.areaName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="盘点类型">{{ checkTypeMap[detail.checkType] || '-' }}</el-descriptions-item>
        <el-descriptions-item label="盘点人">{{ detail.checkerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)">{{ statusMap[detail.status] || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="SKU总数">{{ detail.totalSkuCount }}</el-descriptions-item>
        <el-descriptions-item label="盘盈数量">
          <span class="profit-text">{{ detail.profitQty || 0 }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="盘亏数量">
          <span class="loss-text">{{ detail.lossQty || 0 }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="盘盈金额">
          <span class="profit-text">¥{{ toFixed2(detail.profitAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="盘亏金额">
          <span class="loss-text">¥{{ toFixed2(detail.lossAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="盘点日期">{{ detail.checkDate }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-section-title">盘点明细</div>
      <el-table :data="detail.items || []" border size="small">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="skuCode" label="商品编码" min-width="120" show-overflow-tooltip />
        <el-table-column label="内部编码" prop="innerCode" width="110" />
        <el-table-column prop="skuName" label="商品名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="batchNo" label="批次号" width="120" show-overflow-tooltip />
        <el-table-column prop="locationId" label="库位" width="110" show-overflow-tooltip />
        <el-table-column prop="bookQty" label="账面数量" width="100" align="right" />
        <el-table-column prop="actualQty" label="实盘数量" width="100" align="right" />
        <el-table-column label="差异数量" width="100" align="right">
          <template #default="{ row }">
            <span :class="row.diffQty > 0 ? 'profit-text' : (row.diffQty < 0 ? 'loss-text' : '')">{{ row.diffQty }}</span>
          </template>
        </el-table-column>
        <el-table-column label="成本价" width="100" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.costPrice) }}</template>
        </el-table-column>
        <el-table-column label="差异金额" width="120" align="right">
          <template #default="{ row }">
            <span :class="row.diffAmount > 0 ? 'profit-text' : (row.diffAmount < 0 ? 'loss-text' : '')">¥{{ toFixed2(row.diffAmount) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="detail-section-title">状态日志</div>
      <el-timeline>
        <el-timeline-item v-for="(log, idx) in detail.logs || []" :key="idx" :timestamp="log.operateTime" placement="top">
          <h4>{{ statusMap[log.toStatus] || '-' }}</h4>
          <p>{{ log.operateBy }} - {{ log.remark || '' }}</p>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" title="审核盘点单" width="480px" :close-on-click-modal="false">
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
        <el-form-item label="单据号">
          <span>{{ auditRow?.checkNo || '-' }}</span>
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
defineOptions({ name: 'Check' })
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, RefreshRight } from '@element-plus/icons-vue'
import { checkApi, warehouseApi, areaApi } from '@/api'

const checkTypeOptions = [
  { value: 1, label: '全盘' },
  { value: 2, label: '区域盘' },
  { value: 3, label: '动态盘' },
]
const checkTypeMap: Record<number, string> = { 1: '全盘', 2: '区域盘', 3: '动态盘' }

const statusOptions = [
  { value: 0, label: '草稿' },
  { value: 1, label: '盘点中' },
  { value: 2, label: '已盘点' },
  { value: 3, label: '已审核' },
  { value: 4, label: '已处理' },
  { value: 5, label: '作废' },
]
const statusMap: Record<number, string> = { 0: '草稿', 1: '盘点中', 2: '已盘点', 3: '已审核', 4: '已处理', 5: '作废' }
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
const tableData = ref<any[]>([])
const total = ref(0)
const warehouseList = ref<any[]>([])
const areaList = ref<any[]>([])

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


const query = reactive({
  keyword: '',
  checkType: undefined as number | undefined,
  status: undefined as number | undefined,
  page: 1,
  size: 10,
})

const formVisible = ref(false)
const formRef = ref<FormInstance>()

const initForm = () => ({
  checkNo: '',
  warehouseId: undefined as string | undefined,
  areaId: undefined as string | undefined,
  checkType: 1,
  checkDate: '',
  remark: '',
})

const form = reactive(initForm())

const formRules: FormRules = {
  checkNo: [{ required: true, message: '请输入盘点单号', trigger: 'blur' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  checkType: [{ required: true, message: '请选择盘点类型', trigger: 'change' }],
  checkDate: [{ required: true, message: '请选择盘点日期', trigger: 'change' }],
}

const detailVisible = ref(false)
const detail = ref<any>({})

// 录入实盘
const inputVisible = ref(false)
const inputLoading = ref(false)
const inputRow = ref<any>({})
const inputItems = ref<any[]>([])

const summaryProfitQty = computed(() => inputItems.value.filter((it: any) => it.diffQty > 0).length)
const summaryLossQty = computed(() => inputItems.value.filter((it: any) => it.diffQty < 0).length)
const summaryProfitAmount = computed(() => inputItems.value.filter((it: any) => it.diffAmount > 0).reduce((s: number, it: any) => s + Number(it.diffAmount || 0), 0))
const summaryLossAmount = computed(() => inputItems.value.filter((it: any) => it.diffAmount < 0).reduce((s: number, it: any) => s + Number(it.diffAmount || 0), 0))

onMounted(() => {
  loadWarehouse()
  loadData()
})

async function loadWarehouse() {
  try {
    const res: any = await warehouseApi.listAll()
    warehouseList.value = res.data || []
  } catch (e) { /* ignore */ }
}

async function onWarehouseChange(warehouseId: string) {
  form.areaId = undefined
  if (!warehouseId) {
    areaList.value = []
    return
  }
  try {
    const res: any = await areaApi.page({ warehouseId, page: 1, size: 1000 })
    areaList.value = res.data?.rows || res.data?.records || []
  } catch (e) { /* ignore */ }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await checkApi.page(query)
    const rows = res.data?.rows || res.data?.records || []
    // 后端分页只返回 warehouseId，前端根据下拉列表反查仓库名称
    rows.forEach((r: any) => {
      r.warehouseName = warehouseList.value.find(w => w.warehouseId == r.warehouseId)?.warehouseName || '-'
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
  query.keyword = ''
  query.checkType = undefined
  query.status = undefined
  query.page = 1
  loadData()
}

function handleAdd() {
  Object.assign(form, initForm())
  formVisible.value = true
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await checkApi.save({ ...form })
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

async function handleLoadInventory(row: any) {
  try {
    await ElMessageBox.confirm(`确认加载仓库【${row.warehouseName}】的库存生成盘点明细吗？`, '提示', { type: 'warning' })
    loading.value = true
    await checkApi.loadInventory({ checkId: row.checkId, warehouseId: row.warehouseId, areaId: row.areaId })
    ElMessage.success('库存加载成功')
    loadData()
  } catch (e) { /* cancelled */ } finally {
    loading.value = false
  }
}

async function handleStartCheck(row: any) {
  try {
    await ElMessageBox.confirm(`确认开始盘点【${row.checkNo}】吗？`, '提示', { type: 'warning' })
    await checkApi.startCheck(row.checkId)
    ElMessage.success('已开始盘点')
    loadData()
  } catch (e) { /* cancelled */ }
}

async function handleInputActual(row: any) {
  inputRow.value = row
  inputItems.value = []
  inputVisible.value = true
  inputLoading.value = true
  try {
    const res: any = await checkApi.getById(row.checkId)
    const items = res.data?.items || []
    inputItems.value = items.map((it: any) => ({
      ...it,
      actualQty: it.actualQty ?? it.bookQty ?? 0,
      diffQty: (it.actualQty ?? it.bookQty ?? 0) - (it.bookQty ?? 0),
      diffAmount: ((it.actualQty ?? it.bookQty ?? 0) - (it.bookQty ?? 0)) * Number(it.costPrice || 0),
    }))
  } catch (e) {
    ElMessage.error('加载明细失败')
  } finally {
    inputLoading.value = false
  }
}

function calcDiff(row: any) {
  const actual = Number(row.actualQty || 0)
  const book = Number(row.bookQty || 0)
  row.diffQty = actual - book
  row.diffAmount = row.diffQty * Number(row.costPrice || 0)
}

async function handleSaveActual() {
  submitting.value = true
  try {
    await checkApi.inputActual({
      checkId: inputRow.value.checkId,
      items: inputItems.value.map((it: any) => ({
        itemId: it.itemId,
        actualQty: it.actualQty,
      })),
    })
    ElMessage.success('保存成功')
    inputVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleFinishCheck(row: any) {
  try {
    await ElMessageBox.confirm(`确认完成盘点【${row.checkNo}】吗？完成后将进入待审核状态。`, '提示', { type: 'warning' })
    await checkApi.finishCheck(row.checkId)
    ElMessage.success('完成盘点成功')
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
    await checkApi.audit({
      id: auditRow.value.checkId,
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

async function handleProcess(row: any) {
  try {
    await ElMessageBox.confirm(`确认处理盘点单【${row.checkNo}】吗？处理后将根据差异调整库存。`, '提示', { type: 'warning' })
    await checkApi.handle(row.checkId)
    ElMessage.success('处理成功')
    loadData()
  } catch (e) { /* cancelled */ }
}

async function handleVoid(row: any) {
  try {
    await ElMessageBox.confirm(`确认作废盘点单【${row.checkNo}】吗？作废后不可恢复。`, '提示', { type: 'warning' })
    await checkApi.void(row.checkId)
    ElMessage.success('作废成功')
    loadData()
  } catch (e) { /* cancelled */ }
}

// 查看盘点单详情：后端返回 {order, items, statusLogs}，扁平化后赋值给 detail
// 同时根据 warehouseId 查找仓库名称用于展示
async function handleDetail(row: any) {
  try {
    const res: any = await checkApi.getById(row.checkId)
    const data = res.data || {}
    detail.value = { ...data.order, items: data.items || [], logs: data.statusLogs || [] }
    // 订单实体只存 ID，前端根据 ID 从下拉列表反查仓库名称
    detail.value.warehouseName = warehouseList.value.find(w => w.warehouseId == detail.value.warehouseId)?.warehouseName || '-'
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}
</script>

<style scoped lang="scss">
.search-card { margin-bottom: 12px; }
.action-bar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
.detail-section-title {
  font-size: 14px;
  font-weight: 600;
  margin: 20px 0 10px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}
.input-header { margin-bottom: 12px; }
.input-summary {
  display: flex;
  align-items: center;
  gap: 32px;
  margin-bottom: 12px;
  padding: 10px 16px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  b { margin: 0 4px; }
}
.profit-text { color: #67c23a; font-weight: 600; }
.loss-text { color: #f56c6c; font-weight: 600; }
</style>
