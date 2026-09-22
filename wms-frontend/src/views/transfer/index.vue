<template>
  <div class="page-container">
    <!-- 搜索表单 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline size="default">
        <el-form-item label="调拨单号">
          <el-input v-model="query.transferNo" placeholder="请输入调拨单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="调出仓库">
          <el-select v-model="query.outWarehouseId" placeholder="全部" clearable filterable style="width: 150px">
            <el-option v-for="w in warehouseList" :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="调入仓库">
          <el-select v-model="query.inWarehouseId" placeholder="全部" clearable filterable style="width: 150px">
            <el-option v-for="w in warehouseList" :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
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
      <el-button v-perm="'wms:transfer:add'" type="primary" :icon="Plus" @click="handleAdd">新增调拨单</el-button>
      <el-button :icon="RefreshRight" @click="loadData">刷新</el-button>
    </div>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border stripe size="default">
        <el-table-column prop="transferNo" label="调拨单号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="outWarehouseName" label="调出仓库" min-width="120" show-overflow-tooltip />
        <el-table-column prop="inWarehouseName" label="调入仓库" min-width="120" show-overflow-tooltip />
        <el-table-column label="调拨类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.transferType === 2 ? 'danger' : 'info'" size="small">{{ transferTypeMap[row.transferType] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalQty" label="总数量" width="90" align="right" />
        <el-table-column label="总金额" width="120" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusMap[row.status] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expectDate" label="期望到货日期" width="120" />
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'wms:transfer:list'" link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" v-perm="'wms:transfer:edit'" link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perm="'wms:transfer:submit'" link type="primary" size="small" @click="handleSubmit(row)">提交</el-button>
            <el-button v-if="row.status === 1" v-perm="'wms:transfer:audit'" link type="warning" size="small" @click="handleAudit(row)">审核</el-button>
            <el-button v-if="row.status === 2" v-perm="'wms:transfer:out'" link type="success" size="small" @click="handleConfirmOut(row)">确认出库</el-button>
            <el-button v-if="row.status === 3" v-perm="'wms:transfer:in'" link type="success" size="small" @click="handleConfirmIn(row)">确认入库</el-button>
            <el-button v-if="[0, 1, 2].includes(row.status)" v-perm="'wms:transfer:void'" link type="danger" size="small" @click="handleVoid(row)">作废</el-button>
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
    <el-dialog v-model="formVisible" :title="formTitle" width="1150px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" size="default">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="调拨单号" prop="transferNo">
              <el-input v-model="form.transferNo" placeholder="自动生成" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="调出仓库" prop="outWarehouseId">
              <el-select v-model="form.outWarehouseId" placeholder="请选择" filterable style="width: 100%">
                <el-option v-for="w in warehouseList" :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="调入仓库" prop="inWarehouseId">
              <el-select v-model="form.inWarehouseId" placeholder="请选择" filterable style="width: 100%">
                <el-option v-for="w in warehouseList" :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="调拨类型" prop="transferType">
              <el-select v-model="form.transferType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in transferTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="期望到货" prop="expectDate">
              <el-date-picker v-model="form.expectDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="detail-bar">
          <span class="detail-title">调拨明细</span>
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
          <el-table-column prop="skuName" label="商品名称" min-width="140" show-overflow-tooltip />
          <el-table-column label="调拨数量" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.transferQty" :min="0" :precision="0" size="small" controls-position="right" style="width: 100%" @change="calcSubtotal(row)" />
            </template>
          </el-table-column>
          <el-table-column label="成本价" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.costPrice" :min="0" :precision="2" size="small" controls-position="right" style="width: 100%" @change="calcSubtotal(row)" />
            </template>
          </el-table-column>
          <el-table-column label="批次号" width="130">
            <template #default="{ row }">
              <el-input v-model="row.batchNo" size="small" placeholder="批次号" />
            </template>
          </el-table-column>
          <el-table-column label="调出库位" width="130">
            <template #default="{ row }">
              <el-input v-model="row.outLocationId" size="small" placeholder="调出库位" />
            </template>
          </el-table-column>
          <el-table-column label="调入库位" width="130">
            <template #default="{ row }">
              <el-input v-model="row.inLocationId" size="small" placeholder="调入库位" />
            </template>
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
    <el-dialog v-model="detailVisible" title="调拨单详情" width="1050px" destroy-on-close>
      <el-descriptions :column="3" border size="default">
        <el-descriptions-item label="调拨单号">{{ detail.transferNo }}</el-descriptions-item>
        <el-descriptions-item label="调出仓库">{{ detail.outWarehouseName }}</el-descriptions-item>
        <el-descriptions-item label="调入仓库">{{ detail.inWarehouseName }}</el-descriptions-item>
        <el-descriptions-item label="调拨类型">{{ transferTypeMap[detail.transferType] || '-' }}</el-descriptions-item>
        <el-descriptions-item label="期望到货">{{ detail.expectDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)">{{ statusMap[detail.status] || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总数量">{{ detail.totalQty }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ toFixed2(detail.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-section-title">调拨明细</div>
      <el-table :data="detail.items || []" border size="small">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="skuCode" label="商品编码" min-width="120" show-overflow-tooltip />
        <el-table-column label="内部编码" prop="innerCode" width="110" />
        <el-table-column prop="skuName" label="商品名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="transferQty" label="调拨数量" width="100" align="right" />
        <el-table-column label="成本价" width="110" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.costPrice) }}</template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次号" width="120" show-overflow-tooltip />
        <el-table-column prop="outLocationId" label="调出库位" width="120" show-overflow-tooltip />
        <el-table-column prop="inLocationId" label="调入库位" width="120" show-overflow-tooltip />
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
    <el-dialog v-model="auditVisible" title="审核调拨单" width="480px" :close-on-click-modal="false">
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
        <el-form-item label="单据号">
          <span>{{ auditRow?.transferNo || '-' }}</span>
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
defineOptions({ name: 'Transfer' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, RefreshRight } from '@element-plus/icons-vue'
import { transferApi, warehouseApi, goodsSpuApi } from '@/api'

const transferTypeOptions = [
  { value: 1, label: '常规' },
  { value: 2, label: '紧急' },
]
const transferTypeMap: Record<number, string> = { 1: '常规', 2: '紧急' }

const statusOptions = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已提交' },
  { value: 2, label: '已审核' },
  { value: 3, label: '已出库' },
  { value: 4, label: '已入库' },
  { value: 5, label: '作废' },
]
const statusMap: Record<number, string> = { 0: '草稿', 1: '已提交', 2: '已审核', 3: '已出库', 4: '已入库', 5: '作废' }
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

const query = reactive({
  transferNo: '',
  outWarehouseId: undefined as string | undefined,
  inWarehouseId: undefined as string | undefined,
  status: undefined as number | undefined,
  page: 1,
  size: 10,
})

const formVisible = ref(false)
const formTitle = ref('新增调拨单')
const formRef = ref<FormInstance>()
const isEdit = ref(false)

const initForm = () => ({
  transferId: undefined as string | undefined,
  transferNo: '',
  outWarehouseId: undefined as string | undefined,
  inWarehouseId: undefined as string | undefined,
  transferType: 1,
  expectDate: '',
  remark: '',
  items: [] as any[],
})

const form = reactive(initForm())

const formRules: FormRules = {
  transferNo: [{ required: true, message: '请输入调拨单号', trigger: 'blur' }],
  outWarehouseId: [{ required: true, message: '请选择调出仓库', trigger: 'change' }],
  inWarehouseId: [{ required: true, message: '请选择调入仓库', trigger: 'change' }],
  transferType: [{ required: true, message: '请选择调拨类型', trigger: 'change' }],
  expectDate: [{ required: true, message: '请选择期望到货日期', trigger: 'change' }],
}

const detailVisible = ref(false)
const detail = ref<any>({})

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

async function loadData() {
  loading.value = true
  try {
    const res: any = await transferApi.page(query)
    const rows = res.data?.rows || res.data?.records || []
    // 后端分页只返回 outWarehouseId/inWarehouseId，前端根据下拉列表反查仓库名称
    rows.forEach((r: any) => {
      r.outWarehouseName = warehouseList.value.find(w => w.warehouseId == r.outWarehouseId)?.warehouseName || '-'
      r.inWarehouseName = warehouseList.value.find(w => w.warehouseId == r.inWarehouseId)?.warehouseName || '-'
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
  query.transferNo = ''
  query.outWarehouseId = undefined
  query.inWarehouseId = undefined
  query.status = undefined
  query.page = 1
  loadData()
}

function handleAdd() {
  isEdit.value = false
  formTitle.value = '新增调拨单'
  Object.assign(form, initForm())
  // 预生成调拨单号
  transferApi.generateNo().then((res: any) => {
    if (res?.data) form.transferNo = res.data
  }).catch(() => {})
  formVisible.value = true
}

// 编辑调拨单：后端返回 {order, items, statusLogs} 嵌套结构，需拆解后赋值给表单
async function handleEdit(row: any) {
  isEdit.value = true
  formTitle.value = '编辑调拨单'
  try {
    const res: any = await transferApi.getById(row.transferId)
    const data = res.data || {}
    const order = data.order || data // 兼容旧接口直接返回 order 的情况
    Object.assign(form, order)
    form.items = data.items || order.items || []
    form.transferId = order.transferId
    // 把已有明细的商品塞入 skuOptions，使 el-select 能回显商品名称
    skuOptions.value = form.items.map((d: any) => ({ skuId: d.skuId, skuCode: d.skuCode, skuName: d.skuName }))
    formVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

function addDetailRow() {
  form.items.push({ skuId: undefined, skuCode: '', skuName: '', transferQty: 0, costPrice: 0, batchNo: '', outLocationId: '', inLocationId: '' })
}

const skuOptions = ref<any[]>([])
const skuLoading = ref(false)

// 远程搜索商品 SKU，按关键字 + 调出仓库过滤（只能调拨调出仓库里有的商品）
async function searchSku(query: string) {
  if (!query) { skuOptions.value = []; return }
  skuLoading.value = true
  try {
    const res: any = await goodsSpuApi.searchSku(query, form.outWarehouseId)
    skuOptions.value = res.data || []
  } catch (e) { /* handled */ } finally {
    skuLoading.value = false
  }
}

// 选中商品后自动回填编码、名称、默认成本价
function onSkuSelect(row: any) {
  const sku = skuOptions.value.find(s => s.skuId === row.skuId)
  if (sku) {
    row.skuCode = sku.skuCode
    row.innerCode = sku.innerCode
    row.skuName = sku.skuName
    row.costPrice = sku.defaultCost || 0
  }
}

function removeDetailRow(idx: number) {
  form.items.splice(idx, 1)
}

function calcSubtotal(row: any) {
  row.subtotal = Number(row.transferQty || 0) * Number(row.costPrice || 0)
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!form.items.length) {
      ElMessage.warning('请至少添加一条明细')
      return
    }
    if (form.outWarehouseId === form.inWarehouseId) {
      ElMessage.warning('调出仓库与调入仓库不能相同')
      return
    }
    submitting.value = true
    try {
      const items = form.items.map((it: any) => ({ ...it, subtotal: Number(it.transferQty || 0) * Number(it.costPrice || 0) }))
      const payload = {
        ...form,
        totalQty: items.reduce((s: number, it: any) => s + Number(it.transferQty || 0), 0),
        totalAmount: items.reduce((s: number, it: any) => s + Number(it.subtotal || 0), 0),
        items,
      }
      if (isEdit.value) {
        await transferApi.update(payload)
      } else {
        await transferApi.save(payload)
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

// 查看调拨单详情：后端返回 {order, items, statusLogs}，扁平化后赋值给 detail
// 同时根据 outWarehouseId/inWarehouseId 查找调出/调入仓库名称用于展示
async function handleDetail(row: any) {
  try {
    const res: any = await transferApi.getById(row.transferId)
    const data = res.data || {}
    detail.value = { ...data.order, items: data.items || [], logs: data.statusLogs || [] }
    // 订单实体只存 ID，前端根据 ID 从下拉列表反查仓库名称
    detail.value.outWarehouseName = warehouseList.value.find(w => w.warehouseId == detail.value.outWarehouseId)?.warehouseName || '-'
    detail.value.inWarehouseName = warehouseList.value.find(w => w.warehouseId == detail.value.inWarehouseId)?.warehouseName || '-'
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

async function handleSubmit(row: any) {
  try {
    await ElMessageBox.confirm(`确认提交调拨单【${row.transferNo}】吗？`, '提示', { type: 'warning' })
    await transferApi.submit(row.transferId)
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
    await transferApi.audit({
      id: auditRow.value.transferId,
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

async function handleConfirmOut(row: any) {
  try {
    await ElMessageBox.confirm(`确认调拨单【${row.transferNo}】已出库吗？`, '提示', { type: 'warning' })
    await transferApi.confirmOut(row.transferId)
    ElMessage.success('确认出库成功')
    loadData()
  } catch (e) { /* cancelled */ }
}

async function handleConfirmIn(row: any) {
  try {
    await ElMessageBox.confirm(`确认调拨单【${row.transferNo}】已入库吗？`, '提示', { type: 'warning' })
    await transferApi.confirmIn(row.transferId)
    ElMessage.success('确认入库成功')
    loadData()
  } catch (e) { /* cancelled */ }
}

async function handleVoid(row: any) {
  try {
    await ElMessageBox.confirm(`确认作废调拨单【${row.transferNo}】吗？作废后不可恢复。`, '提示', { type: 'warning' })
    await transferApi.void(row.transferId)
    ElMessage.success('作废成功')
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
