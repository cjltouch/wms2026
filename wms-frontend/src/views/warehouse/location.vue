<template>
  <div class="page-container">
    <!-- 搜索 -->
    <el-form :model="search" inline class="search-form">
      <el-form-item label="所属仓库">
        <el-select v-model="search.warehouseId" placeholder="请选择仓库" clearable filterable style="width: 180px" @change="onWarehouseChange">
          <el-option v-for="w in warehouseList" :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
        </el-select>
      </el-form-item>
      <el-form-item label="所属库区">
        <el-select v-model="search.areaId" placeholder="请选择库区" clearable filterable style="width: 180px">
          <el-option v-for="a in areaList" :key="a.areaId" :label="a.areaName" :value="a.areaId" />
        </el-select>
      </el-form-item>
      <el-form-item label="库位编码">
        <el-input v-model="search.locationCode" placeholder="请输入库位编码" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作 -->
    <div class="table-operations">
      <el-button v-perm="'wms:location:add'" type="primary" :icon="Plus" @click="handleAdd">新增库位</el-button>
      <el-button v-perm="'wms:location:add'" type="success" :icon="MagicStick" @click="batchDialogVisible = true">批量生成</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column type="index" label="#" width="55" align="center" />
      <el-table-column prop="locationCode" label="库位编码" min-width="130" show-overflow-tooltip />
      <el-table-column prop="locationName" label="库位名称" min-width="130" show-overflow-tooltip />
      <el-table-column label="所属仓库" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
      </el-table-column>
      <el-table-column label="所属库区" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ areaName(row.areaId) }}</template>
      </el-table-column>
      <el-table-column prop="locationType" label="库位类型" width="100" align="center" />
      <el-table-column prop="rowNo" label="行" width="60" align="center" />
      <el-table-column prop="columnNo" label="列" width="60" align="center" />
      <el-table-column prop="levelNo" label="层" width="60" align="center" />
      <el-table-column prop="maxQty" label="最大容量" width="100" align="right" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'">{{ row.status === '0' ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-perm="'wms:location:edit'" link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button v-perm="'wms:location:remove'" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="640px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属仓库" prop="warehouseId">
              <el-select v-model="form.warehouseId" placeholder="请选择仓库" filterable style="width: 100%" @change="onFormWarehouseChange">
                <el-option v-for="w in warehouseList" :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属库区" prop="areaId">
              <el-select v-model="form.areaId" placeholder="请选择库区" filterable style="width: 100%">
                <el-option v-for="a in formAreaList" :key="a.areaId" :label="a.areaName" :value="a.areaId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库位编码" prop="locationCode">
              <el-input v-model="form.locationCode" placeholder="请输入库位编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库位名称" prop="locationName">
              <el-input v-model="form.locationName" placeholder="请输入库位名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库位类型" prop="locationType">
              <el-input v-model="form.locationType" placeholder="如：货架/地堆" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大容量" prop="maxQty">
              <el-input-number v-model="form.maxQty" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="行" prop="rowNo">
              <el-input-number v-model="form.rowNo" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="列" prop="columnNo">
              <el-input-number v-model="form.columnNo" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="层" prop="levelNo">
              <el-input-number v-model="form.levelNo" :min="0" style="width: 100%" />
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
                <el-radio value="0">启用</el-radio>
                <el-radio value="1">禁用</el-radio>
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

    <!-- 批量生成弹窗 -->
    <el-dialog title="批量生成库位" v-model="batchDialogVisible" width="560px" @closed="resetBatchForm">
      <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-width="100px">
        <el-form-item label="所属仓库" prop="warehouseId">
          <el-select v-model="batchForm.warehouseId" placeholder="请选择仓库" filterable style="width: 100%" @change="onBatchWarehouseChange">
            <el-option v-for="w in warehouseList" :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属库区" prop="areaId">
          <el-select v-model="batchForm.areaId" placeholder="请选择库区" filterable style="width: 100%">
            <el-option v-for="a in batchAreaList" :key="a.areaId" :label="a.areaName" :value="a.areaId" />
          </el-select>
        </el-form-item>
        <el-form-item label="编码模板" prop="locationTemplate">
          <el-input v-model="batchForm.locationTemplate" placeholder="如 A01-01-01~A10-10-05" />
          <div class="tip-text">格式：前缀+行-列-层~前缀+行-列-层，例：A01-01-01~A10-10-05 将生成 A01-01-01 到 A10-10-05 的所有库位</div>
        </el-form-item>
        <el-form-item label="库位类型" prop="locationType">
          <el-input v-model="batchForm.locationType" placeholder="如：货架/地堆" />
        </el-form-item>
        <el-form-item label="最大容量" prop="maxQty">
          <el-input-number v-model="batchForm.maxQty" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="batchForm.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="handleBatchSubmit">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'WarehouseLocation' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, MagicStick } from '@element-plus/icons-vue'
import { locationApi, areaApi, warehouseApi } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const batchSubmitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const warehouseList = ref<any[]>([])
const areaList = ref<any[]>([])
const allAreaList = ref<any[]>([])
const formAreaList = ref<any[]>([])
const batchAreaList = ref<any[]>([])

const search = reactive({
  warehouseId: undefined as any,
  areaId: undefined as any,
  locationCode: '',
  pageNum: 1,
  pageSize: 10,
})

function warehouseName(id: any) {
  if (id == null) return ''
  const w = warehouseList.value.find((x) => String(x.warehouseId) === String(id))
  return w ? w.warehouseName : id
}

function areaName(id: any) {
  if (id == null) return ''
  const a = allAreaList.value.find((x) => String(x.areaId) === String(id))
  return a ? a.areaName : id
}

async function loadWarehouses() {
  try {
    const res: any = await warehouseApi.listAll()
    warehouseList.value = res.data || []
  } catch (e) { /* handled */ }
}

async function loadAllAreas() {
  try {
    const res: any = await areaApi.listAll()
    allAreaList.value = res.data || []
  } catch (e) { /* handled */ }
}

async function loadAreas(warehouseId: any, target: typeof areaList) {
  if (!warehouseId) {
    target.value = []
    return
  }
  try {
    const res: any = await areaApi.page({ warehouseId, pageNum: 1, pageSize: 9999 })
    const d = res.data || {}
    target.value = d.rows || d.records || d.list || []
  } catch (e) { /* handled */ }
}

async function onWarehouseChange(val: any) {
  search.areaId = undefined
  await loadAreas(val, areaList)
  handleSearch()
}

function onFormWarehouseChange(val: any) {
  form.areaId = undefined
  loadAreas(val, formAreaList)
}

function onBatchWarehouseChange(val: any) {
  batchForm.areaId = undefined
  loadAreas(val, batchAreaList)
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await locationApi.page(search)
    const d = res.data || {}
    tableData.value = d.rows || d.records || d.list || []
    total.value = d.total || 0
  } catch (e) {
    /* handled */
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  search.pageNum = 1
  loadData()
}

function resetSearch() {
  search.warehouseId = undefined
  search.areaId = undefined
  search.locationCode = ''
  areaList.value = []
  search.pageNum = 1
  loadData()
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增库位')
const formRef = ref<FormInstance>()
const defaultForm = () => ({
  locationId: undefined as any,
  warehouseId: undefined as any,
  areaId: undefined as any,
  locationCode: '',
  locationName: '',
  locationType: '',
  rowNo: 1,
  columnNo: 1,
  levelNo: 1,
  maxQty: 0,
  sort: 0,
  status: '0',
})
const form = reactive(defaultForm())
const rules: FormRules = {
  warehouseId: [{ required: true, message: '请选择所属仓库', trigger: 'change' }],
  areaId: [{ required: true, message: '请选择所属库区', trigger: 'change' }],
  locationCode: [{ required: true, message: '请输入库位编码', trigger: 'blur' }],
  locationName: [{ required: true, message: '请输入库位名称', trigger: 'blur' }],
}

function resetForm() {
  Object.assign(form, defaultForm())
  formAreaList.value = []
  formRef.value?.resetFields()
}

function handleAdd() {
  dialogTitle.value = '新增库位'
  resetForm()
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  try {
    const res: any = await locationApi.getById(row.locationId)
    const data = res.data || row
    Object.assign(form, defaultForm(), data)
    await loadAreas(form.warehouseId, formAreaList)
    dialogTitle.value = '编辑库位'
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
      if (form.locationId) {
        await locationApi.update({ ...form })
        ElMessage.success('更新成功')
      } else {
        await locationApi.save({ ...form })
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
    await ElMessageBox.confirm(`确定删除库位【${row.locationCode}】吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await locationApi.remove(row.locationId)
    ElMessage.success('删除成功')
    if (tableData.value.length === 1 && search.pageNum > 1) search.pageNum--
    loadData()
  } catch (e) {
    /* cancelled or error */
  }
}

// 批量生成
const batchDialogVisible = ref(false)
const batchFormRef = ref<FormInstance>()
const defaultBatchForm = () => ({
  warehouseId: undefined as any,
  areaId: undefined as any,
  locationTemplate: '',
  locationType: '',
  maxQty: 0,
  status: '0',
})
const batchForm = reactive(defaultBatchForm())
const batchRules: FormRules = {
  warehouseId: [{ required: true, message: '请选择所属仓库', trigger: 'change' }],
  areaId: [{ required: true, message: '请选择所属库区', trigger: 'change' }],
  locationTemplate: [{ required: true, message: '请输入编码模板', trigger: 'blur' }],
}

function resetBatchForm() {
  Object.assign(batchForm, defaultBatchForm())
  batchAreaList.value = []
  batchFormRef.value?.resetFields()
}

async function handleBatchSubmit() {
  if (!batchFormRef.value) return
  await batchFormRef.value.validate(async (valid) => {
    if (!valid) return
    batchSubmitting.value = true
    try {
      const res: any = await locationApi.batchGenerate({
        warehouseId: batchForm.warehouseId,
        areaId: batchForm.areaId,
        locationTemplate: batchForm.locationTemplate,
        locationType: batchForm.locationType,
        maxQty: batchForm.maxQty,
        status: batchForm.status,
      })
      const count = res.data ?? 0
      ElMessage.success(`成功生成 ${count} 个库位`)
      batchDialogVisible.value = false
      loadData()
    } catch (e) {
      /* handled */
    } finally {
      batchSubmitting.value = false
    }
  })
}

onMounted(() => {
  loadWarehouses()
  loadAllAreas()
  loadData()
})
</script>

<style scoped lang="scss">
.tip-text {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
