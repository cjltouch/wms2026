<template>
  <div class="page-container">
    <!-- 搜索表单 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline size="default">
        <el-form-item label="报损单号">
          <el-input v-model="query.lossNo" placeholder="请输入报损单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="query.warehouseId" placeholder="全部" clearable filterable style="width: 150px">
            <el-option v-for="w in warehouseList" :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="报损类型">
          <el-select v-model="query.lossType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in lossTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
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
      <el-button v-perm="'wms:loss:add'" type="primary" :icon="Plus" @click="handleAdd">新增报损单</el-button>
      <el-button :icon="RefreshRight" @click="loadData">刷新</el-button>
    </div>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border stripe size="default">
        <el-table-column prop="lossNo" label="报损单号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" min-width="120" show-overflow-tooltip />
        <el-table-column label="报损类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="lossTypeTagType(row.lossType)" size="small">{{ lossTypeMap[row.lossType] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalQty" label="总数量" width="90" align="right" />
        <el-table-column label="总金额" width="120" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="lossDate" label="报损日期" width="110" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusMap[row.status] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'wms:loss:list'" link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" v-perm="'wms:loss:edit'" link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perm="'wms:loss:submit'" link type="primary" size="small" @click="handleSubmit(row)">提交</el-button>
            <el-button v-if="row.status === 1" v-perm="'wms:loss:audit'" link type="warning" size="small" @click="handleAudit(row)">审核</el-button>
            <el-button v-if="row.status === 2" v-perm="'wms:loss:handle'" link type="success" size="small" @click="handleProcess(row)">处理</el-button>
            <el-button v-if="[0, 1, 2].includes(row.status)" v-perm="'wms:loss:void'" link type="danger" size="small" @click="handleVoid(row)">作废</el-button>
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
            <el-form-item label="报损单号" prop="lossNo">
              <el-input v-model="form.lossNo" placeholder="请输入报损单号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="仓库" prop="warehouseId">
              <el-select v-model="form.warehouseId" placeholder="请选择" filterable style="width: 100%">
                <el-option v-for="w in warehouseList" :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="报损类型" prop="lossType">
              <el-select v-model="form.lossType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in lossTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="报损日期" prop="lossDate">
              <el-date-picker v-model="form.lossDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="detail-bar">
          <span class="detail-title">报损明细</span>
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
          <el-table-column label="报损数量" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.lossQty" :min="0" :precision="0" size="small" controls-position="right" style="width: 100%" @change="calcSubtotal(row)" />
            </template>
          </el-table-column>
          <el-table-column label="成本价" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.costPrice" :min="0" :precision="2" size="small" controls-position="right" style="width: 100%" @change="calcSubtotal(row)" />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="110" align="right">
            <template #default="{ row }">¥{{ toFixed2(row.subtotal) }}</template>
          </el-table-column>
          <el-table-column label="批次号" width="130">
            <template #default="{ row }">
              <el-input v-model="row.batchNo" size="small" placeholder="批次号" />
            </template>
          </el-table-column>
          <el-table-column label="库位" width="130">
            <template #default="{ row }">
              <el-input v-model="row.locationId" size="small" placeholder="库位" />
            </template>
          </el-table-column>
          <el-table-column label="报损原因" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.lossReason" size="small" placeholder="报损原因" />
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
    <el-dialog v-model="detailVisible" title="报损单详情" width="1050px" destroy-on-close>
      <el-descriptions :column="3" border size="default">
        <el-descriptions-item label="报损单号">{{ detail.lossNo }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="报损类型">
          <el-tag :type="lossTypeTagType(detail.lossType)" size="small">{{ lossTypeMap[detail.lossType] || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="报损日期">{{ detail.lossDate }}</el-descriptions-item>
        <el-descriptions-item label="总数量">{{ detail.totalQty }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ toFixed2(detail.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)">{{ statusMap[detail.status] || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-section-title">报损明细</div>
      <el-table :data="detail.items || []" border size="small">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="skuCode" label="商品编码" min-width="120" show-overflow-tooltip />
        <el-table-column label="内部编码" prop="innerCode" width="110" />
        <el-table-column prop="skuName" label="商品名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="lossQty" label="报损数量" width="100" align="right" />
        <el-table-column label="成本价" width="110" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.costPrice) }}</template>
        </el-table-column>
        <el-table-column label="小计" width="110" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.subtotal) }}</template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次号" width="120" show-overflow-tooltip />
        <el-table-column prop="locationId" label="库位" width="120" show-overflow-tooltip />
        <el-table-column prop="lossReason" label="报损原因" min-width="140" show-overflow-tooltip />
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
    <el-dialog v-model="auditVisible" title="审核报损单" width="480px" :close-on-click-modal="false">
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
        <el-form-item label="单据号">
          <span>{{ auditRow?.lossNo || '-' }}</span>
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
defineOptions({ name: 'Loss' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, RefreshRight } from '@element-plus/icons-vue'
import { lossApi, warehouseApi, goodsSpuApi } from '@/api'

const lossTypeOptions = [
  { value: 1, label: '报损' },
  { value: 2, label: '报溢' },
  { value: 3, label: '破损' },
  { value: 4, label: '过期' },
  { value: 5, label: '丢失' },
]
const lossTypeMap: Record<number, string> = { 1: '报损', 2: '报溢', 3: '破损', 4: '过期', 5: '丢失' }
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
const lossTypeTagMap: Record<number, TagType | undefined> = { 1: 'danger', 2: 'success', 3: 'warning', 4: 'warning', 5: 'danger' }

function lossTypeTagType(type: number): TagType | undefined {
  return lossTypeTagMap[type]
}

const statusOptions = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已提交' },
  { value: 2, label: '已审核' },
  { value: 3, label: '已处理' },
  { value: 5, label: '作废' },
]
const statusMap: Record<number, string> = { 0: '草稿', 1: '已提交', 2: '已审核', 3: '已处理', 5: '作废' }
const statusTagMap: Record<number, TagType | undefined> = { 0: 'info', 1: 'warning', 2: 'primary', 3: 'success', 5: 'danger' }

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
  lossNo: '',
  warehouseId: undefined as string | undefined,
  lossType: undefined as number | undefined,
  status: undefined as number | undefined,
  page: 1,
  size: 10,
})

const formVisible = ref(false)
const formTitle = ref('新增报损单')
const formRef = ref<FormInstance>()
const isEdit = ref(false)

const initForm = () => ({
  lossId: undefined as string | undefined,
  lossNo: '',
  warehouseId: undefined as string | undefined,
  lossType: undefined as number | undefined,
  lossDate: '',
  remark: '',
  items: [] as any[],
})

const form = reactive(initForm())

const formRules: FormRules = {
  lossNo: [{ required: true, message: '请输入报损单号', trigger: 'blur' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  lossType: [{ required: true, message: '请选择报损类型', trigger: 'change' }],
  lossDate: [{ required: true, message: '请选择报损日期', trigger: 'change' }],
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
    const res: any = await lossApi.page(query)
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
  query.lossNo = ''
  query.warehouseId = undefined
  query.lossType = undefined
  query.status = undefined
  query.page = 1
  loadData()
}

function handleAdd() {
  isEdit.value = false
  formTitle.value = '新增报损单'
  Object.assign(form, initForm())
  formVisible.value = true
}

// 编辑报损单：后端返回 {order, items, statusLogs} 嵌套结构，需拆解后赋值给表单
async function handleEdit(row: any) {
  isEdit.value = true
  formTitle.value = '编辑报损单'
  try {
    const res: any = await lossApi.getById(row.lossId)
    const data = res.data || {}
    const order = data.order || data // 兼容旧接口直接返回 order 的情况
    Object.assign(form, order)
    form.items = data.items || order.items || []
    form.lossId = order.lossId
    // 把已有明细的商品塞入 skuOptions，使 el-select 能回显商品名称
    skuOptions.value = form.items.map((d: any) => ({ skuId: d.skuId, skuCode: d.skuCode, skuName: d.skuName }))
    formVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

function addDetailRow() {
  form.items.push({ skuId: undefined, skuCode: '', skuName: '', lossQty: 0, costPrice: 0, subtotal: 0, batchNo: '', locationId: '', lossReason: '' })
}

const skuOptions = ref<any[]>([])
const skuLoading = ref(false)

// 远程搜索商品 SKU，按关键字 + 仓库过滤可报损的商品
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
  row.subtotal = Number(row.lossQty || 0) * Number(row.costPrice || 0)
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
      const items = form.items.map((it: any) => ({ ...it, subtotal: Number(it.lossQty) * Number(it.costPrice) }))
      const payload = {
        ...form,
        totalQty: items.reduce((s: number, it: any) => s + Number(it.lossQty || 0), 0),
        totalAmount: items.reduce((s: number, it: any) => s + Number(it.subtotal || 0), 0),
        items,
      }
      if (isEdit.value) {
        await lossApi.update(payload)
      } else {
        await lossApi.save(payload)
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

// 查看报损单详情：后端返回 {order, items, statusLogs}，扁平化后赋值给 detail
// 同时根据 warehouseId 查找仓库名称用于展示
async function handleDetail(row: any) {
  try {
    const res: any = await lossApi.getById(row.lossId)
    const data = res.data || {}
    detail.value = { ...data.order, items: data.items || [], logs: data.statusLogs || [] }
    // 订单实体只存 ID，前端根据 ID 从下拉列表反查仓库名称
    detail.value.warehouseName = warehouseList.value.find(w => w.warehouseId == detail.value.warehouseId)?.warehouseName || '-'
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

async function handleSubmit(row: any) {
  try {
    await ElMessageBox.confirm(`确认提交报损单【${row.lossNo}】吗？`, '提示', { type: 'warning' })
    await lossApi.submit(row.lossId)
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
    await lossApi.audit({
      id: auditRow.value.lossId,
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
    await ElMessageBox.confirm(`确认处理报损单【${row.lossNo}】吗？处理后将调整库存。`, '提示', { type: 'warning' })
    await lossApi.handle({ id: row.lossId })
    ElMessage.success('处理成功')
    loadData()
  } catch (e) { /* cancelled */ }
}

async function handleVoid(row: any) {
  try {
    await ElMessageBox.confirm(`确认作废报损单【${row.lossNo}】吗？作废后不可恢复。`, '提示', { type: 'warning' })
    await lossApi.void(row.lossId)
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
