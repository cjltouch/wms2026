<template>
  <div class="page-container">
    <!-- 搜索 -->
    <el-form :model="search" inline class="search-form">
      <el-form-item label="仓库名称">
        <el-input v-model="search.warehouseName" placeholder="请输入仓库名称" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作 -->
    <div class="table-operations">
      <el-button v-perm="'wms:warehouse:add'" type="primary" :icon="Plus" @click="handleAdd">新增仓库</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column type="index" label="#" width="55" align="center" />
      <el-table-column prop="warehouseCode" label="仓库编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="warehouseName" label="仓库名称" min-width="140" show-overflow-tooltip />
      <el-table-column label="仓库类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="warehouseTypeTag(row.warehouseType)">{{ warehouseTypeText(row.warehouseType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="所在地区" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          {{ [row.province, row.city, row.district].filter(Boolean).join('/') }}
        </template>
      </el-table-column>
      <el-table-column prop="manager" label="负责人" width="100" />
      <el-table-column prop="phone" label="联系电话" width="130" />
      <el-table-column prop="area" label="面积(㎡)" width="100" align="right" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'">{{ row.status === '0' ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-perm="'wms:warehouse:edit'" link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button v-perm="'wms:warehouse:remove'" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="search.pageNum"
        v-model:page-size="search.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="680px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="仓库编码" prop="warehouseCode">
              <el-input v-model="form.warehouseCode" placeholder="请输入仓库编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="仓库名称" prop="warehouseName">
              <el-input v-model="form.warehouseName" placeholder="请输入仓库名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="仓库类型" prop="warehouseType">
              <el-select v-model="form.warehouseType" placeholder="请选择仓库类型" style="width: 100%">
                <el-option v-for="o in warehouseTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="面积(㎡)" prop="area">
              <el-input-number v-model="form.area" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="省" prop="province">
              <el-input v-model="form.province" placeholder="省" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="市" prop="city">
              <el-input v-model="form.city" placeholder="市" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区" prop="district">
              <el-input v-model="form.district" placeholder="区" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址" prop="detailAddress">
              <el-input v-model="form.detailAddress" placeholder="请输入详细地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经度" prop="longitude">
              <el-input-number v-model="form.longitude" :precision="6" :step="0.0001" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度" prop="latitude">
              <el-input-number v-model="form.latitude" :precision="6" :step="0.0001" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人" prop="manager">
              <el-input v-model="form.manager" placeholder="请输入负责人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="'0'">启用</el-radio>
                <el-radio :value="'1'">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Warehouse' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { warehouseApi } from '@/api'

const warehouseTypeOptions = [
  { value: 1, label: '平面仓' },
  { value: 2, label: '立体仓' },
  { value: 3, label: '冷链仓' },
]

function warehouseTypeText(t: number) {
  return warehouseTypeOptions.find(o => o.value === t)?.label || '-'
}
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
function warehouseTypeTag(t: number): TagType | undefined {
  const map: Record<number, TagType | undefined> = { 1: undefined, 2: 'warning', 3: 'success' }
  return map[t]
}

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const search = reactive({
  warehouseName: '',
  pageNum: 1,
  pageSize: 10,
})

async function loadData() {
  loading.value = true
  try {
    const res: any = await warehouseApi.page(search)
    const d = res.data || {}
    tableData.value = d.rows || []
    total.value = d.total || 0
  } catch (e) {
    /* handled by interceptor */
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  search.pageNum = 1
  loadData()
}

function resetSearch() {
  search.warehouseName = ''
  search.pageNum = 1
  loadData()
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增仓库')
const formRef = ref<FormInstance>()
const defaultForm = () => ({
  warehouseId: undefined as any,
  warehouseCode: '',
  warehouseName: '',
  warehouseType: 1,
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  longitude: undefined as any,
  latitude: undefined as any,
  manager: '',
  phone: '',
  area: undefined as any,
  sort: 0,
  status: '0',
})
const form = reactive(defaultForm())
const rules: FormRules = {
  warehouseCode: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }],
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
  warehouseType: [{ required: true, message: '请选择仓库类型', trigger: 'change' }],
  manager: [{ required: true, message: '请输入负责人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
}

function resetForm() {
  Object.assign(form, defaultForm())
  formRef.value?.resetFields()
}

function handleAdd() {
  dialogTitle.value = '新增仓库'
  resetForm()
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  try {
    const res: any = await warehouseApi.getById(row.warehouseId)
    Object.assign(form, defaultForm(), res.data || row)
    dialogTitle.value = '编辑仓库'
    dialogVisible.value = true
  } catch (e) {
    /* handled */
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (form.warehouseId) {
        await warehouseApi.update({ ...form })
        ElMessage.success('更新成功')
      } else {
        await warehouseApi.save({ ...form })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (e) {
      /* handled */
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除仓库【${row.warehouseName}】吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await warehouseApi.remove(row.warehouseId)
    ElMessage.success('删除成功')
    if (tableData.value.length === 1 && search.pageNum > 1) search.pageNum--
    loadData()
  } catch (e) {
    /* cancelled or error */
  }
}

onMounted(() => {
  loadData()
})
</script>
