<template>
  <div class="page-container">
    <!-- 搜索表单 -->
    <el-form :inline="true" :model="queryParams" class="search-form">
      <el-form-item label="单位名称">
        <el-input v-model="queryParams.unitName" placeholder="请输入单位名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
      <el-button type="danger" :icon="Delete" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除</el-button>
    </div>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="tableData" border stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column type="index" label="#" width="50" align="center" />
      <el-table-column prop="unitCode" label="单位编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="unitName" label="单位名称" min-width="120" show-overflow-tooltip />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'">{{ row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="单位编码" prop="unitCode">
          <el-input v-model="form.unitCode" placeholder="请输入单位编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="单位名称" prop="unitName">
          <el-input v-model="form.unitName" placeholder="请输入单位名称" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="'0'">正常</el-radio>
            <el-radio :value="'1'">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'GoodsUnit' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { unitApi } from '@/api'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const selectedIds = ref<any[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  unitName: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增计量单位')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = () => ({
  unitId: undefined as undefined | string,
  unitCode: '',
  unitName: '',
  status: '0'
})

const form = reactive(defaultForm())

const rules: FormRules = {
  unitCode: [{ required: true, message: '请输入单位编码', trigger: 'blur' }],
  unitName: [{ required: true, message: '请输入单位名称', trigger: 'blur' }]
}

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res: any = await unitApi.page(queryParams)
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
  queryParams.unitName = ''
  queryParams.pageNum = 1
  loadData()
}

function handleSelectionChange(rows: any[]) {
  selectedIds.value = rows
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增计量单位'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  isEdit.value = true
  dialogTitle.value = '编辑计量单位'
  Object.assign(form, defaultForm(), row)
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
        await unitApi.update(form)
        ElMessage.success('修改成功')
      } else {
        await unitApi.save(form)
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

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除单位「${row.unitName}」吗？`, '提示', { type: 'warning' })
    await unitApi.remove(row.unitId)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // cancelled or handled by interceptor
  }
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条数据吗？`, '提示', { type: 'warning' })
    await unitApi.batchDelete(selectedIds.value.map((r: any) => r.unitId))
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
