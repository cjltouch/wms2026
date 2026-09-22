<template>
  <div class="page-container">
    <!-- 搜索表单 -->
    <el-form :inline="true" :model="queryParams" class="search-form">
      <el-form-item label="名称">
        <el-input v-model="queryParams.itemName" placeholder="请输入名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="queryParams.type" placeholder="全部类型" clearable style="width: 120px">
          <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期范围">
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
        <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button v-perm="'wms:office:add'" type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
      <el-button v-perm="'wms:office:export'" type="success" :icon="Download" @click="handleExport">导出</el-button>
      <el-button v-perm="'wms:office:remove'" type="danger" :icon="Delete" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除</el-button>
    </div>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="tableData" border stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column type="index" label="#" width="50" align="center" />
      <el-table-column prop="recordDate" label="日期" width="110" align="center" />
      <el-table-column prop="itemName" label="名称" min-width="140" show-overflow-tooltip />
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.type)">{{ typeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="80" align="center" />
      <el-table-column prop="quantity" label="数量" width="80" align="center" />
      <el-table-column prop="personName" label="姓名" width="100" align="center" />
      <el-table-column prop="spec" label="规格" min-width="120" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-perm="'wms:office:edit'" link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button v-perm="'wms:office:remove'" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 弹窗表单 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="日期" prop="recordDate">
              <el-date-picker v-model="form.recordDate" type="date" placeholder="默认今天" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
                <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称" prop="itemName">
              <el-select
                v-model="form.itemName"
                filterable
                remote
                allow-create
                default-first-option
                clearable
                placeholder="请输入或选择名称"
                :remote-method="searchItemName"
                :loading="itemNameLoading"
                style="width: 100%"
                @change="onItemNameChange"
              >
                <el-option
                  v-for="n in itemNameOptions"
                  :key="n.itemName"
                  :label="n.itemName + (n.spec ? '  ' + n.spec : '') + (n.unit ? ' (' + n.unit + ')' : '')"
                  :value="n.itemName"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格" prop="spec">
              <el-input v-model="form.spec" placeholder="如 A4/70g" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="如 包、个、箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="personName">
              <el-input v-model="form.personName" placeholder="经手/领用人" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'OfficeRecord' })
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Download } from '@element-plus/icons-vue'
import { officeRecordApi } from '@/api'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const selectedIds = ref<any[]>([])

const typeOptions = [
  { value: 1, label: '入库' },
  { value: 2, label: '领取' },
  { value: 3, label: '报损' }
]
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
function typeText(t: number) {
  return typeOptions.find(o => o.value === t)?.label || '-'
}
function typeTag(t: number): TagType {
  return t === 1 ? 'success' : t === 2 ? 'primary' : 'danger'
}

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  itemName: '',
  type: undefined as undefined | number,
  dateStart: '',
  dateEnd: ''
})

// 日期范围选择器与查询参数双向同步
const dateRange = ref<[string, string] | null>(null)
watch(dateRange, (v) => {
  queryParams.dateStart = v?.[0] || ''
  queryParams.dateEnd = v?.[1] || ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增登记')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

// 名称远程搜索选项
interface NameSuggest { itemName: string; spec: string; unit: string }
const itemNameOptions = ref<NameSuggest[]>([])
const itemNameLoading = ref(false)
let itemNameSearchTimer: ReturnType<typeof setTimeout> | null = null
async function searchItemName(q: string) {
  if (itemNameSearchTimer) clearTimeout(itemNameSearchTimer)
  itemNameSearchTimer = setTimeout(async () => {
    itemNameLoading.value = true
    try {
      const res: any = await officeRecordApi.suggestName(q || '')
      itemNameOptions.value = res.data || []
    } catch (e) {
      // handled by interceptor
    } finally {
      itemNameLoading.value = false
    }
  }, 200)
}

// 选中/输入名称后的联动：从历史记录里找到匹配项，自动回填规格和单位（若当前为空）
function onItemNameChange(val: string) {
  if (!val) return
  const matched = itemNameOptions.value.find(o => o.itemName === val)
  if (!matched) return
  if (!form.spec && matched.spec) form.spec = matched.spec
  if (!form.unit && matched.unit) form.unit = matched.unit
}

const defaultForm = () => ({
  recordId: undefined as undefined | string,
  recordDate: '',
  itemName: '',
  type: 1,
  unit: '',
  quantity: 1,
  personName: '',
  spec: '',
  remark: ''
})

const form = reactive(defaultForm())

const rules: FormRules = {
  itemName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }]
}

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res: any = await officeRecordApi.page(queryParams)
    const d = res.data || {}
    tableData.value = d.rows || []
    total.value = d.total || 0
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.itemName = ''
  queryParams.type = undefined
  queryParams.dateStart = ''
  queryParams.dateEnd = ''
  dateRange.value = null
  queryParams.pageNum = 1
  loadData()
}

function handleSelectionChange(rows: any[]) {
  selectedIds.value = rows
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增登记'
  Object.assign(form, defaultForm())
  searchItemName('') // 预加载历史名称，下拉展开即可选
  dialogVisible.value = true
}

function handleEdit(row: any) {
  isEdit.value = true
  dialogTitle.value = '编辑登记'
  Object.assign(form, defaultForm(), row)
  searchItemName(row.itemName || '') // 编辑时预加载相关名称
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, defaultForm())
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (isEdit.value) {
        await officeRecordApi.update(form)
        ElMessage.success('修改成功')
      } else {
        await officeRecordApi.save(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (e) {
      // handled by interceptor
    } finally {
      submitLoading.value = false
    }
  })
}

async function handleExport() {
  try {
    const params: Record<string, any> = {}
    if (queryParams.itemName) params.itemName = queryParams.itemName
    if (queryParams.type != null) params.type = queryParams.type
    if (queryParams.dateStart) params.dateStart = queryParams.dateStart
    if (queryParams.dateEnd) params.dateEnd = queryParams.dateEnd
    const res: any = await officeRecordApi.export(params)
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `用品登记_${new Date().toISOString().slice(0, 10)}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除「${row.itemName}」的这条登记记录吗？`, '提示', { type: 'warning' })
    await officeRecordApi.remove(row.recordId)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // cancelled or handled by interceptor
  }
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条记录吗？`, '提示', { type: 'warning' })
    await officeRecordApi.batchDelete(selectedIds.value.map((r: any) => r.recordId))
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // cancelled or handled by interceptor
  }
}
</script>

<style scoped lang="scss">
.search-form { margin-bottom: 12px; }
.action-bar { margin-bottom: 12px; }
.pagination-bar { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>
