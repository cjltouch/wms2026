<template>
  <div class="page-container">
    <!-- 搜索表单 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline size="default">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="销售单号/客户名称" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="销售日期">
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
      <el-button v-perm="'wms:sale:add'" type="primary" :icon="Plus" @click="handleAdd">新增销售单</el-button>
      <el-button :icon="RefreshRight" @click="loadData">刷新</el-button>
    </div>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border stripe size="default">
        <el-table-column prop="saleNo" label="销售单号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户名称" min-width="130" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="出库仓库" min-width="120" show-overflow-tooltip />
        <el-table-column label="销售类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ saleTypeMap[row.saleType] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalQty" label="总数量" width="90" align="right" />
        <el-table-column label="销售金额" width="120" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.saleAmount) }}</template>
        </el-table-column>
        <el-table-column label="已收金额" width="120" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.receivedAmount) }}</template>
        </el-table-column>
        <el-table-column label="付款状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="payStatusTagType(row.payStatus)" size="small">{{ payStatusMap[row.payStatus] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusMap[row.status] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="saleDate" label="销售日期" width="110" />
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button v-perm="'wms:sale:list'" link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" v-perm="'wms:sale:edit'" link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" v-perm="'wms:sale:submit'" link type="primary" size="small" @click="handleSubmit(row)">提交</el-button>
            <el-button v-if="row.status === 1" v-perm="'wms:sale:audit'" link type="warning" size="small" @click="handleAudit(row)">审核</el-button>
            <el-button v-if="row.status === 2" v-perm="'wms:sale:out'" link type="success" size="small" @click="handleConfirmOut(row)">确认出库</el-button>
            <el-button v-if="row.status === 3" v-perm="'wms:sale:pay'" link type="primary" size="small" @click="handleConfirmPay(row)">确认收款</el-button>
            <el-button v-if="row.status === 3" v-perm="'wms:sale:complete'" link type="success" size="small" @click="handleComplete(row)">完成订单</el-button>
            <el-button v-if="[0, 1, 2].includes(row.status)" v-perm="'wms:sale:void'" link type="danger" size="small" @click="handleVoid(row)">作废</el-button>
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
            <el-form-item label="销售单号" prop="saleNo">
              <el-input v-model="form.saleNo" placeholder="请输入销售单号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户" prop="customerId">
              <el-select v-model="form.customerId" placeholder="请选择客户" filterable style="width: 100%">
                <el-option v-for="c in customerList" :key="c.customerId" :label="c.customerName" :value="c.customerId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出库仓库" prop="warehouseId">
              <el-select v-model="form.warehouseId" placeholder="请选择仓库" filterable style="width: 100%">
                <el-option v-for="w in warehouseList" :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="销售类型" prop="saleType">
              <el-select v-model="form.saleType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in saleTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="销售日期" prop="saleDate">
              <el-date-picker v-model="form.saleDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="快递费用">
              <el-input-number v-model="form.expressFee" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="收货人">
              <el-input v-model="form.receiverName" placeholder="请输入收货人" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收货电话">
              <el-input v-model="form.receiverPhone" placeholder="请输入收货电话" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="快递公司">
              <el-input v-model="form.expressCompany" placeholder="请输入快递公司" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="快递单号">
              <el-input v-model="form.expressNo" placeholder="请输入快递单号" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="收货地址">
              <el-input v-model="form.receiverAddress" placeholder="请输入收货地址" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>

        <div class="detail-bar">
          <span class="detail-title">销售明细</span>
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
          <el-table-column label="数量" width="90">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="0" :precision="0" size="small" controls-position="right" style="width: 100%" @change="calcSubtotal(row)" />
            </template>
          </el-table-column>
          <el-table-column label="销售价" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.salePrice" :min="0" :precision="2" size="small" controls-position="right" style="width: 100%" @change="calcSubtotal(row)" />
            </template>
          </el-table-column>
          <el-table-column label="成本价" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.costPrice" :min="0" :precision="2" size="small" controls-position="right" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="折扣率%" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.discountRate" :min="0" :max="100" :precision="0" size="small" controls-position="right" style="width: 100%" @change="calcSubtotal(row)" />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="110" align="right">
            <template #default="{ row }">¥{{ toFixed2(row.subtotal) }}</template>
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

    <!-- 确认收款弹窗 -->
    <el-dialog v-model="payVisible" title="确认收款" width="420px" destroy-on-close>
      <el-form :model="payForm" label-width="90px" size="default">
        <el-form-item label="销售单号">{{ payForm.saleNo }}</el-form-item>
        <el-form-item label="应收金额">¥{{ toFixed2(payForm.saleAmount) }}</el-form-item>
        <el-form-item label="已收金额">¥{{ toFixed2(payForm.receivedAmount) }}</el-form-item>
        <el-form-item label="本次收款">
          <el-input-number v-model="payForm.amount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handlePaySubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="销售单详情" width="1050px" destroy-on-close>
      <el-descriptions :column="3" border size="default">
        <el-descriptions-item label="销售单号">{{ detail.saleNo }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ detail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="出库仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="销售类型">{{ saleTypeMap[detail.saleType] || '-' }}</el-descriptions-item>
        <el-descriptions-item label="销售日期">{{ detail.saleDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)">{{ statusMap[detail.status] || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="销售金额">¥{{ toFixed2(detail.saleAmount) }}</el-descriptions-item>
        <el-descriptions-item label="已收金额">¥{{ toFixed2(detail.receivedAmount) }}</el-descriptions-item>
        <el-descriptions-item label="付款状态">
          <el-tag :type="payStatusTagType(detail.payStatus)">{{ payStatusMap[detail.payStatus] || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="收货人">{{ detail.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="收货电话">{{ detail.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="快递公司">{{ detail.expressCompany || '-' }}</el-descriptions-item>
        <el-descriptions-item label="快递单号">{{ detail.expressNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="快递费用">¥{{ toFixed2(detail.expressFee) }}</el-descriptions-item>
        <el-descriptions-item label="利润">
          <span class="profit-text">¥{{ toFixed2(detail.profit) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="3">{{ detail.receiverAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-section-title">销售明细</div>
      <el-table :data="detail.items || []" border size="small">
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="skuCode" label="商品编码" min-width="120" show-overflow-tooltip />
        <el-table-column label="内部编码" prop="innerCode" width="110" />
        <el-table-column prop="skuName" label="商品名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="quantity" label="数量" width="90" align="right" />
        <el-table-column label="销售价" width="110" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.salePrice) }}</template>
        </el-table-column>
        <el-table-column label="成本价" width="110" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.costPrice) }}</template>
        </el-table-column>
        <el-table-column label="折扣率" width="90" align="right">
          <template #default="{ row }">{{ row.discountRate || 100 }}%</template>
        </el-table-column>
        <el-table-column label="小计" width="110" align="right">
          <template #default="{ row }">¥{{ toFixed2(row.subtotal) }}</template>
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
    <el-dialog v-model="auditVisible" title="审核销售单" width="480px" :close-on-click-modal="false">
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
        <el-form-item label="单据号">
          <span>{{ auditRow?.saleNo || '-' }}</span>
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
defineOptions({ name: 'Sale' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, RefreshRight } from '@element-plus/icons-vue'
import { saleApi, customerApi, warehouseApi, goodsSpuApi } from '@/api'

const saleTypeOptions = [
  { value: 1, label: '零售' },
  { value: 2, label: '批发' },
  { value: 3, label: '线上' },
]
const saleTypeMap: Record<number, string> = { 1: '零售', 2: '批发', 3: '线上' }

const statusOptions = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已提交' },
  { value: 2, label: '已审核' },
  { value: 3, label: '已出库' },
  { value: 4, label: '已完成' },
  { value: 5, label: '作废' },
]
const statusMap: Record<number, string> = { 0: '草稿', 1: '已提交', 2: '已审核', 3: '已出库', 4: '已完成', 5: '作废' }
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
const statusTagMap: Record<number, TagType | undefined> = { 0: 'info', 1: 'warning', 2: 'primary', 3: 'warning', 4: 'success', 5: 'danger' }

const payStatusMap: Record<number, string> = { 0: '未付', 1: '部分', 2: '已付' }
const payStatusTagMap: Record<number, TagType | undefined> = { 0: 'danger', 1: 'warning', 2: 'success' }

function statusTagType(status: number): TagType | undefined {
  return statusTagMap[status]
}

function payStatusTagType(status: number): TagType | undefined {
  return payStatusTagMap[status]
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
const customerList = ref<any[]>([])
const warehouseList = ref<any[]>([])
const dateRange = ref<string[]>([])

const query = reactive({
  keyword: '',
  status: undefined as number | undefined,
  startDate: '',
  endDate: '',
  page: 1,
  size: 10,
})

const formVisible = ref(false)
const formTitle = ref('新增销售单')
const formRef = ref<FormInstance>()
const isEdit = ref(false)

const initForm = () => ({
  saleId: undefined as string | undefined,
  saleNo: '',
  customerId: undefined as string | undefined,
  warehouseId: undefined as string | undefined,
  saleType: 1,
  saleDate: '',
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  expressCompany: '',
  expressNo: '',
  expressFee: 0,
  remark: '',
  items: [] as any[],
})

const form = reactive(initForm())

const formRules: FormRules = {
  saleNo: [{ required: true, message: '请输入销售单号', trigger: 'blur' }],
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  saleType: [{ required: true, message: '请选择销售类型', trigger: 'change' }],
  saleDate: [{ required: true, message: '请选择销售日期', trigger: 'change' }],
}

const detailVisible = ref(false)
const detail = ref<any>({})

const payVisible = ref(false)
const payForm = reactive({ id: '', saleNo: '', saleAmount: 0, receivedAmount: 0, amount: 0 })

onMounted(() => {
  loadCustomer()
  loadWarehouse()
  loadData()
})

async function loadCustomer() {
  try {
    const res: any = await customerApi.listAll()
    customerList.value = res.data || []
  } catch (e) { /* ignore */ }
}

async function loadWarehouse() {
  try {
    const res: any = await warehouseApi.listAll()
    warehouseList.value = res.data || []
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
    const res: any = await saleApi.page(params)
    const rows = res.data?.rows || res.data?.records || []
    // 后端分页只返回 warehouseId/customerId，前端根据下拉列表反查名称
    rows.forEach((r: any) => {
      r.warehouseName = warehouseList.value.find(w => w.warehouseId == r.warehouseId)?.warehouseName || '-'
      r.customerName = customerList.value.find(c => c.customerId == r.customerId)?.customerName || '-'
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
  query.status = undefined
  dateRange.value = []
  query.startDate = ''
  query.endDate = ''
  query.page = 1
  loadData()
}

function handleAdd() {
  isEdit.value = false
  formTitle.value = '新增销售单'
  Object.assign(form, initForm())
  formVisible.value = true
}

// 编辑销售单：后端返回 {order, items, statusLogs} 嵌套结构，需拆解后赋值给表单
async function handleEdit(row: any) {
  isEdit.value = true
  formTitle.value = '编辑销售单'
  try {
    const res: any = await saleApi.getById(row.saleId)
    const data = res.data || {}
    const order = data.order || data // 兼容旧接口直接返回 order 的情况
    Object.assign(form, order)
    form.items = data.items || order.items || []
    form.saleId = order.saleId
    // 把已有明细的商品塞入 skuOptions，使 el-select 能回显商品名称
    skuOptions.value = form.items.map((d: any) => ({ skuId: d.skuId, skuCode: d.skuCode, skuName: d.skuName }))
    formVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

function addDetailRow() {
  form.items.push({ skuId: undefined, skuCode: '', skuName: '', quantity: 0, salePrice: 0, costPrice: 0, discountRate: 100, subtotal: 0 })
}

const skuOptions = ref<any[]>([])
const skuLoading = ref(false)

// 远程搜索商品 SKU，按关键字 + 仓库过滤可销售的商品
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

// 选中商品后自动回填编码、名称、默认销售价和成本价
function onSkuSelect(row: any) {
  const sku = skuOptions.value.find(s => s.skuId === row.skuId)
  if (sku) {
    row.skuCode = sku.skuCode
    row.innerCode = sku.innerCode
    row.skuName = sku.skuName
    row.salePrice = sku.defaultPrice || 0
    row.costPrice = sku.defaultCost || 0
  }
}

function removeDetailRow(idx: number) {
  form.items.splice(idx, 1)
}

function calcSubtotal(row: any) {
  const qty = Number(row.quantity || 0)
  const price = Number(row.salePrice || 0)
  const discount = Number(row.discountRate || 100) / 100
  row.subtotal = qty * price * discount
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
      const items = form.items.map((it: any) => ({
        ...it,
        subtotal: Number(it.quantity) * Number(it.salePrice) * (Number(it.discountRate || 100) / 100),
      }))
      const payload = {
        ...form,
        totalQty: items.reduce((s: number, it: any) => s + Number(it.quantity || 0), 0),
        totalAmount: items.reduce((s: number, it: any) => s + Number(it.subtotal || 0), 0),
        items,
      }
      if (isEdit.value) {
        await saleApi.update(payload)
      } else {
        await saleApi.save(payload)
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

// 查看销售单详情：后端返回 {order, items, statusLogs}，扁平化后赋值给 detail
// 同时根据 customerId/warehouseId 查找对应的客户名和仓库名用于展示
async function handleDetail(row: any) {
  try {
    const res: any = await saleApi.getById(row.saleId)
    const data = res.data || {}
    detail.value = { ...data.order, items: data.items || [], logs: data.statusLogs || [] }
    // 订单实体只存 ID，前端根据 ID 从下拉列表反查名称
    detail.value.customerName = customerList.value.find(c => c.customerId == detail.value.customerId)?.customerName || '-'
    detail.value.warehouseName = warehouseList.value.find(w => w.warehouseId == detail.value.warehouseId)?.warehouseName || '-'
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

async function handleSubmit(row: any) {
  try {
    await ElMessageBox.confirm(`确认提交销售单【${row.saleNo}】吗？`, '提示', { type: 'warning' })
    await saleApi.submit(row.saleId)
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
    await saleApi.audit({
      id: auditRow.value.saleId,
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
    await ElMessageBox.confirm(`确认销售单【${row.saleNo}】已出库吗？`, '提示', { type: 'warning' })
    await saleApi.confirmOut(row.saleId)
    ElMessage.success('确认出库成功')
    loadData()
  } catch (e) { /* cancelled */ }
}

function handleConfirmPay(row: any) {
  payForm.id = row.saleId
  payForm.saleNo = row.saleNo
  payForm.saleAmount = row.saleAmount
  payForm.receivedAmount = row.receivedAmount
  payForm.amount = Number(row.saleAmount || 0) - Number(row.receivedAmount || 0)
  payVisible.value = true
}

async function handlePaySubmit() {
  if (payForm.amount <= 0) {
    ElMessage.warning('收款金额必须大于0')
    return
  }
  submitting.value = true
  try {
    await saleApi.confirmPay({ id: payForm.id, amount: payForm.amount })
    ElMessage.success('收款成功')
    payVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('收款失败')
  } finally {
    submitting.value = false
  }
}

async function handleComplete(row: any) {
  try {
    await ElMessageBox.confirm(`确认完成销售单【${row.saleNo}】吗？`, '提示', { type: 'warning' })
    await saleApi.complete(row.saleId)
    ElMessage.success('完成订单成功')
    loadData()
  } catch (e) { /* cancelled */ }
}

async function handleVoid(row: any) {
  try {
    await ElMessageBox.confirm(`确认作废销售单【${row.saleNo}】吗？作废后不可恢复。`, '提示', { type: 'warning' })
    await saleApi.void(row.saleId)
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
.profit-text {
  color: #f56c6c;
  font-weight: 700;
}
</style>
