<template>
  <div class="page-container">
    <!-- 搜索表单 -->
    <el-form :inline="true" :model="queryParams" class="search-form">
      <el-form-item label="关键字">
        <el-input v-model="queryParams.keyword" placeholder="SPU名称/编码" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="分类">
        <el-tree-select
          v-model="queryParams.categoryId"
          :data="categoryOptions"
          :props="treeSelectProps"
          node-key="categoryId"
          check-strictly
          :render-after-expand="false"
          placeholder="请选择分类"
          clearable
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="品牌">
        <el-select v-model="queryParams.brandId" placeholder="请选择品牌" clearable filterable style="width: 160px">
          <el-option v-for="b in brandOptions" :key="b.brandId" :label="b.brandName" :value="b.brandId" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
          <el-option label="正常" :value="'0'" />
          <el-option label="停用" :value="'1'" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
      <el-button type="success" :icon="Download" @click="handleExport">导出商品</el-button>
      <el-button type="success" :icon="Top" :disabled="!selectedIds.length" @click="handleBatchStatus('0')">批量上架</el-button>
      <el-button type="warning" :icon="Bottom" :disabled="!selectedIds.length" @click="handleBatchStatus('1')">批量下架</el-button>
      <el-button type="danger" :icon="Delete" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除</el-button>
    </div>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="tableData" border stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column type="index" label="#" width="50" align="center" />
      <el-table-column prop="spuCode" label="SPU编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="spuName" label="SPU名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" min-width="120" show-overflow-tooltip />
      <el-table-column prop="brandName" label="品牌" min-width="120" show-overflow-tooltip />
      <el-table-column prop="unitName" label="单位" width="90" align="center" />
      <el-table-column prop="abcLevel" label="ABC等级" width="100" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'">{{ row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click="handleView(row)">查看</el-button>
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

    <!-- 查看详情弹窗 -->
    <el-dialog v-model="viewVisible" :title="`商品详情 - ${viewData.spuName || ''}`" width="900px" top="6vh">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="SPU编码">{{ viewData.spuCode || '—' }}</el-descriptions-item>
        <el-descriptions-item label="SPU名称">{{ viewData.spuName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ viewData.categoryName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ viewData.brandName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="单位">{{ viewData.unitName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="ABC等级">{{ viewData.abcLevel || '—' }}</el-descriptions-item>
        <el-descriptions-item label="产地">{{ viewData.origin || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewData.status === '0' ? 'success' : 'danger'">{{ viewData.status === '0' ? '正常' : '停用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ viewData.createTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="3">{{ viewData.description || '—' }}</el-descriptions-item>
      </el-descriptions>
      <div class="view-sku-title">SKU 明细</div>
      <el-table :data="viewData.skuList || []" border size="small">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="skuCode" label="SKU编码" min-width="110" />
        <el-table-column label="内部编码" prop="innerCode" width="110" />
        <el-table-column prop="skuName" label="商品名称" min-width="120" />
        <el-table-column label="供应商" min-width="120">
          <template #default="{ row }">
            {{ supplierOptions.find((s: any) => s.supplierId === row.supplierId)?.supplierName || '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="specText" label="规格" min-width="90" />
        <el-table-column prop="barcode" label="条码" min-width="110" />
        <el-table-column label="成本价" width="90" align="right">
          <template #default="{ row }">¥{{ row.defaultCost ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="售价" width="90" align="right">
          <template #default="{ row }">¥{{ row.defaultSale ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">{{ row.status === '0' ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="viewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑大弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="1100px" top="5vh" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="8">
          <el-col :span="8">
            <el-form-item label="SPU编码" prop="spuCode">
              <el-input v-model="form.spuCode" placeholder="请输入SPU编码" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="SPU名称" prop="spuName">
              <el-input v-model="form.spuName" placeholder="请输入SPU名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分类" prop="categoryId">
              <el-tree-select
                v-model="form.categoryId"
                :data="categoryOptions"
                :props="treeSelectProps"
                node-key="categoryId"
                check-strictly
                :render-after-expand="false"
                placeholder="请选择分类"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="品牌" prop="brandId">
              <el-select v-model="form.brandId" placeholder="请选择品牌" clearable filterable style="width: 100%">
                <el-option v-for="b in brandOptions" :key="b.brandId" :label="b.brandName" :value="b.brandId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unitId">
              <el-select v-model="form.unitId" placeholder="请选择单位" clearable filterable style="width: 100%">
                <el-option v-for="u in unitOptions" :key="u.unitId" :label="u.unitName" :value="u.unitId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="产地" prop="origin">
              <el-input v-model="form.origin" placeholder="请输入产地" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="ABC等级" prop="abcLevel">
              <el-select v-model="form.abcLevel" placeholder="请选择" style="width: 100%">
                <el-option label="A" value="A" />
                <el-option label="B" value="B" />
                <el-option label="C" value="C" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="'0'">正常</el-radio>
                <el-radio :value="'1'">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="图片URL" prop="picUrls">
              <el-input v-model="form.picUrls" placeholder="多URL逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- SKU明细 -->
        <div class="sku-section">
          <div class="sku-header">
            <span class="sku-title">SKU明细</span>
            <el-button type="primary" size="small" :icon="Plus" @click="addSkuRow">添加SKU</el-button>
          </div>
          <el-table :data="form.skuList" border size="small" style="width: 100%">
            <el-table-column type="index" label="#" width="45" align="center" fixed />
            <el-table-column label="SKU编码" width="130" fixed>
              <template #default="{ row }">
                <el-input v-model="row.skuCode" size="small" placeholder="SKU编码" />
              </template>
            </el-table-column>
            <el-table-column label="内部编码" width="130">
              <template #default="{ row }">
                <el-input v-model="row.innerCode" size="small" placeholder="内部编码" />
              </template>
            </el-table-column>
            <el-table-column label="供应商" width="150">
              <template #default="{ row }">
                <el-select v-model="row.supplierId" size="small" placeholder="主供应商" filterable clearable style="width: 100%">
                  <el-option v-for="s in supplierOptions" :key="s.supplierId" :label="s.supplierName" :value="s.supplierId" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="规格" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.specText" size="small" placeholder="如 500ml/瓶" />
              </template>
            </el-table-column>
            <el-table-column label="颜色" width="100">
              <template #default="{ row }">
                <el-input v-model="row.color" size="small" placeholder="颜色" />
              </template>
            </el-table-column>
            <el-table-column label="成本价" width="110">
              <template #default="{ row }">
                <el-input-number v-model="row.defaultCost" size="small" :min="0" :precision="2" :controls="false" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="销售价" width="110">
              <template #default="{ row }">
                <el-input-number v-model="row.defaultSale" size="small" :min="0" :precision="2" :controls="false" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="条形码" width="140">
              <template #default="{ row }">
                <el-input v-model="row.barcode" size="small" placeholder="条形码" />
              </template>
            </el-table-column>
            <el-table-column label="重量(g)" width="100">
              <template #default="{ row }">
                <el-input-number v-model="row.weightG" size="small" :min="0" :precision="0" :controls="false" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="体积(ml)" width="100">
              <template #default="{ row }">
                <el-input-number v-model="row.volumeMl" size="small" :min="0" :precision="0" :controls="false" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="批次" width="70" align="center">
              <template #default="{ row }">
                <el-checkbox v-model="row.batchFlag" :true-value="1" :false-value="0" />
              </template>
            </el-table-column>
            <el-table-column label="保质期" width="70" align="center">
              <template #default="{ row }">
                <el-checkbox v-model="row.expireFlag" :true-value="1" :false-value="0" />
              </template>
            </el-table-column>
            <el-table-column label="SN" width="70" align="center">
              <template #default="{ row }">
                <el-checkbox v-model="row.snFlag" :true-value="1" :false-value="0" />
              </template>
            </el-table-column>
            <el-table-column label="保质天数" width="100">
              <template #default="{ row }">
                <el-input-number v-model="row.shelfLifeDays" size="small" :min="0" :controls="false" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center" fixed="right">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" :icon="Delete" @click="removeSkuRow($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
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
import { Search, Refresh, Plus, Edit, Delete, Top, Bottom, View, Download } from '@element-plus/icons-vue'
import { goodsSpuApi, categoryApi, brandApi, unitApi, supplierApi } from '@/api'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const selectedIds = ref<any[]>([])

const categoryOptions = ref<any[]>([])
const brandOptions = ref<any[]>([])
const unitOptions = ref<any[]>([])
const supplierOptions = ref<any[]>([])
const treeSelectProps = { label: 'categoryName', children: 'children' }

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  categoryId: undefined as undefined | string,
  brandId: undefined as undefined | string,
  status: undefined as undefined | string
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增SPU')

// 查看详情
const viewVisible = ref(false)
const viewData = ref<any>({})

const isEdit = ref(false)
const formRef = ref<FormInstance>()

async function handleView(row: any) {
  const res: any = await goodsSpuApi.getById(row.spuId)
  viewData.value = res.data || {}
  viewVisible.value = true
}

const defaultSkuRow = () => ({
  skuId: undefined as undefined | string,
  supplierId: undefined as undefined | string,
  skuCode: '',
  innerCode: '',
  barcode: '',
  specText: '',
  color: '',
  weightG: 0,
  volumeMl: 0,
  batchFlag: 0,
  expireFlag: 0,
  snFlag: 0,
  shelfLifeDays: 0,
  defaultCost: 0,
  defaultSale: 0
})

const defaultForm = () => ({
  spuId: undefined as undefined | string,
  spuCode: '',
  spuName: '',
  categoryId: undefined as undefined | string,
  brandId: undefined as undefined | string,
  unitId: undefined as undefined | string,
  origin: '',
  abcLevel: 'A',
  picUrls: '',
  description: '',
  status: '0',
  skuList: [] as any[]
})

const form = reactive(defaultForm())

const rules: FormRules = {
  spuCode: [{ required: true, message: '请输入SPU编码', trigger: 'blur' }],
  spuName: [{ required: true, message: '请输入SPU名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  brandId: [{ required: true, message: '请选择品牌', trigger: 'change' }],
  unitId: [{ required: true, message: '请选择单位', trigger: 'change' }]
}

onMounted(() => {
  loadData()
  loadOptions()
})

async function loadData() {
  loading.value = true
  try {
    const res: any = await goodsSpuApi.page(queryParams)
    const d = res.data || {}
    tableData.value = d.rows || []
    total.value = d.total || 0
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleExport() {
  try {
    const params: Record<string, any> = {}
    if (queryParams.keyword) params.keyword = queryParams.keyword
    if (queryParams.categoryId) params.categoryId = queryParams.categoryId
    if (queryParams.brandId) params.brandId = queryParams.brandId
    if (queryParams.status) params.status = queryParams.status
    const res: any = await goodsSpuApi.export(params)
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `商品列表_${new Date().toISOString().slice(0, 10)}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

async function loadOptions() {
  try {
    const [catRes, brandRes, unitRes, supRes]: any = await Promise.all([
      categoryApi.tree(),
      brandApi.listAll(),
      unitApi.listAll(),
      supplierApi.page({ pageNum: 1, pageSize: 500 })
    ])
    categoryOptions.value = catRes.data || []
    brandOptions.value = brandRes.data || []
    unitOptions.value = unitRes.data || []
    supplierOptions.value = supRes.data?.rows || supRes.data?.records || []
  } catch (e) {
    // handled by interceptor
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  loadData()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.categoryId = undefined
  queryParams.brandId = undefined
  queryParams.status = undefined
  queryParams.pageNum = 1
  loadData()
}

function handleSelectionChange(rows: any[]) {
  selectedIds.value = rows
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增SPU'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  isEdit.value = true
  dialogTitle.value = '编辑SPU'
  Object.assign(form, defaultForm())
  try {
    const res: any = await goodsSpuApi.getById(row.spuId)
    const d = res.data || {}
    Object.assign(form, {
      spuId: d.spuId,
      spuCode: d.spuCode || '',
      spuName: d.spuName || '',
      categoryId: d.categoryId,
      brandId: d.brandId,
      unitId: d.unitId,
      origin: d.origin || '',
      abcLevel: d.abcLevel || 'A',
      picUrls: d.picUrls || '',
      description: d.description || '',
      status: d.status ?? '0',
      skuList: (d.skuList || []).map((s: any) => ({ ...defaultSkuRow(), ...s }))
    })
  } catch (e) {
    // handled by interceptor
  }
  dialogVisible.value = true
}

function addSkuRow() {
  form.skuList.push(defaultSkuRow())
}

function removeSkuRow(index: number) {
  form.skuList.splice(index, 1)
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
        await goodsSpuApi.update(form)
        ElMessage.success('修改成功')
      } else {
        await goodsSpuApi.save(form)
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
    await ElMessageBox.confirm(`确认删除SPU「${row.spuName}」吗？`, '提示', { type: 'warning' })
    await goodsSpuApi.remove(row.spuId)
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
    await goodsSpuApi.batchDelete(selectedIds.value.map((r: any) => r.spuId))
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // cancelled or handled by interceptor
  }
}

async function handleBatchStatus(status: string) {
  if (!selectedIds.value.length) return
  const action = status === '0' ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确认批量${action}选中的 ${selectedIds.value.length} 条数据吗？`, '提示', { type: 'warning' })
    await goodsSpuApi.batchChangeStatus({
      ids: selectedIds.value.map((r: any) => r.spuId),
      status
    })
    ElMessage.success(`${action}成功`)
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
.view-sku-title { margin: 16px 0 8px; font-weight: 600; font-size: 14px; }

.sku-section {
  margin-top: 16px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;

  .sku-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;

    .sku-title {
      font-size: 14px;
      font-weight: 600;
      color: #303030;

      &::before {
        content: '';
        display: inline-block;
        width: 3px;
        height: 14px;
        background: #409eff;
        margin-right: 6px;
        vertical-align: middle;
      }
    }
  }
}
</style>
