<template>
  <div class="page-container">
    <!-- 搜索表单 -->
    <el-form :inline="true" :model="queryParams" class="search-form">
      <el-form-item label="供应商名称">
        <el-input v-model="queryParams.supplierName" placeholder="请输入供应商名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商编码">
        <el-input v-model="queryParams.supplierCode" placeholder="请输入供应商编码" clearable @keyup.enter="handleQuery" />
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
      <el-table-column prop="supplierCode" label="供应商编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="supplierName" label="供应商名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="contact" label="联系人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="phone" label="电话" min-width="130" show-overflow-tooltip />
      <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="820px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="0">
          <el-col :span="12">
            <el-form-item label="供应商编码" prop="supplierCode">
              <el-input v-model="form.supplierCode" placeholder="请输入供应商编码" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商名称" prop="supplierName">
              <el-input v-model="form.supplierName" placeholder="请输入供应商名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="简称" prop="shortName">
              <el-input v-model="form.shortName" placeholder="请输入简称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contact">
              <el-input v-model="form.contact" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="座机" prop="tel">
              <el-input v-model="form.tel" placeholder="请输入座机" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="税号" prop="taxNo">
              <el-input v-model="form.taxNo" placeholder="请输入税号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开户行" prop="bankName">
              <el-input v-model="form.bankName" placeholder="请输入开户行" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="银行账号" prop="bankAccount">
              <el-input v-model="form.bankAccount" placeholder="请输入银行账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="税率(%)" prop="taxRate">
              <el-input-number v-model="form.taxRate" :min="0" :max="100" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结算方式" prop="paymentTerms">
              <el-input v-model="form.paymentTerms" placeholder="如 月结30天" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="信用等级" prop="creditLevel">
              <el-select v-model="form.creditLevel" placeholder="请选择" style="width: 100%">
                <el-option label="A" :value="1" />
                <el-option label="B" :value="2" />
                <el-option label="C" :value="3" />
                <el-option label="D" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="'0'">正常</el-radio>
                <el-radio :value="'1'">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址" prop="address">
              <el-input v-model="form.address" type="textarea" :rows="2" placeholder="请输入地址" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { supplierApi } from '@/api'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const selectedIds = ref<any[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增供应商')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = () => ({
  supplierId: undefined as undefined | string,
  supplierCode: '',
  supplierName: '',
  shortName: '',
  contact: '',
  phone: '',
  tel: '',
  email: '',
  address: '',
  taxNo: '',
  bankName: '',
  bankAccount: '',
  taxRate: 0,
  paymentTerms: '',
  creditLevel: 2,
  status: '0'
})

const form = reactive(defaultForm())

const rules: FormRules = {
  supplierCode: [{ required: true, message: '请输入供应商编码', trigger: 'blur' }],
  supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res: any = await supplierApi.page(queryParams)
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
  queryParams.supplierName = ''
  queryParams.supplierCode = ''
  queryParams.pageNum = 1
  loadData()
}

function handleSelectionChange(rows: any[]) {
  selectedIds.value = rows
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增供应商'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  isEdit.value = true
  dialogTitle.value = '编辑供应商'
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
        await supplierApi.update(form)
        ElMessage.success('修改成功')
      } else {
        await supplierApi.save(form)
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
    await ElMessageBox.confirm(`确认删除供应商「${row.supplierName}」吗？`, '提示', { type: 'warning' })
    await supplierApi.remove(row.supplierId)
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
    await supplierApi.batchDelete(selectedIds.value.map((r: any) => r.supplierId))
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
