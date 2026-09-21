<template>
  <div class="page-container">
    <!-- 搜索表单 -->
    <el-form :inline="true" class="search-form">
      <el-form-item label="分类名称">
        <el-input v-model="filterText" placeholder="请输入分类名称过滤" clearable />
      </el-form-item>
      <el-form-item>
        <el-button v-perm="'wms:category:add'" type="primary" :icon="Plus" @click="handleAddRoot">新增顶级分类</el-button>
        <el-button :icon="Refresh" @click="loadTree">刷新</el-button>
      </el-form-item>
    </el-form>

    <!-- 树形表格 -->
    <el-card shadow="never" v-loading="loading" class="tree-card">
      <el-tree
        ref="treeRef"
        :data="treeData"
        node-key="categoryId"
        :props="treeProps"
        :filter-node-method="filterNode"
        :expand-on-click-node="false"
        default-expand-all
        draggable
        :allow-drop="allowDrop"
        @node-drop="handleDrop"
      >
        <template #default="{ node, data }">
          <div class="tree-node">
            <div class="node-label">
              <el-icon v-if="data.children && data.children.length" size="14"><Folder /></el-icon>
              <el-icon v-else size="14"><Document /></el-icon>
              <span class="node-name">{{ data.categoryName }}</span>
              <el-tag size="small" :type="data.status === '0' ? 'success' : 'danger'" class="node-status">
                {{ data.status === '0' ? '正常' : '停用' }}
              </el-tag>
              <span class="node-order">排序: {{ data.orderNum ?? 0 }}</span>
            </div>
            <div class="node-actions">
              <el-button v-perm="'wms:category:add'" link type="primary" :icon="Plus" @click.stop="handleAddChild(data)">子分类</el-button>
              <el-button v-perm="'wms:category:edit'" link type="primary" :icon="Edit" @click.stop="handleEdit(data)">编辑</el-button>
              <el-button v-perm="'wms:category:remove'" link type="danger" :icon="Delete" @click.stop="handleDelete(data)">删除</el-button>
            </div>
          </div>
        </template>
      </el-tree>
    </el-card>

    <!-- 弹窗表单 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级分类">
          <el-input v-model="form.parentName" placeholder="顶级分类" readonly />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" controls-position="right" style="width: 100%" />
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
defineOptions({ name: 'GoodsCategory' })
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Edit, Delete, Refresh, Folder, Document } from '@element-plus/icons-vue'
import { categoryApi } from '@/api'

const loading = ref(false)
const submitLoading = ref(false)
const treeData = ref<any[]>([])
const filterText = ref('')
const treeRef = ref()

const treeProps = { label: 'categoryName', children: 'children' }

const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = () => ({
  categoryId: undefined as undefined | string,
  parentId: '0' as string,
  parentName: '顶级分类',
  categoryName: '',
  orderNum: 0,
  status: '0'
})

const form = reactive(defaultForm())

const rules: FormRules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

onMounted(() => {
  loadTree()
})

async function loadTree() {
  loading.value = true
  try {
    const res: any = await categoryApi.tree()
    treeData.value = res.data || []
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function filterNode(value: string, data: any) {
  if (!value) return true
  return data.categoryName?.indexOf(value) !== -1
}

function allowDrop(_draggingNode: any, _dropNode: any, type: 'prev' | 'inner' | 'next') {
  // 允许拖拽到同级前后，不允许 inner 改变父子关系（保持简单）
  return type === 'prev' || type === 'next'
}

async function handleDrop() {
  // 拖拽完成后，按当前树顺序把同级节点顺序提交后端更新
  try {
    const flat: any[] = []
    const walk = (nodes: any[], parentId = '0') => {
      nodes.forEach((n, idx) => {
        flat.push({ categoryId: n.categoryId, parentId, orderNum: idx })
        if (n.children && n.children.length) walk(n.children, n.categoryId)
      })
    }
    walk(treeData.value)
    // 后端如果支持批量保存排序，可调用 save；这里逐条 update 以兼容简单接口
    for (const item of flat) {
      try {
        await categoryApi.update(item)
      } catch (e) {
        // 单条失败不中断
      }
    }
    ElMessage.success('排序已更新')
  } catch (e) {
    // handled by interceptor
  }
}

function handleAddRoot() {
  isEdit.value = false
  dialogTitle.value = '新增顶级分类'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function handleAddChild(row: any) {
  isEdit.value = false
  dialogTitle.value = '新增子分类'
  Object.assign(form, defaultForm(), {
    parentId: row.categoryId,
    parentName: row.categoryName
  })
  dialogVisible.value = true
}

function handleEdit(row: any) {
  isEdit.value = true
  dialogTitle.value = '编辑分类'
  Object.assign(form, defaultForm(), row, {
    parentName: row.parentName || '顶级分类'
  })
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
        await categoryApi.update(form)
        ElMessage.success('修改成功')
      } else {
        await categoryApi.save(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadTree()
    } catch (e) {
      // handled by interceptor
    } finally {
      submitLoading.value = false
    }
  })
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除分类「${row.categoryName}」吗？子分类将一并删除。`, '提示', { type: 'warning' })
    await categoryApi.remove(row.categoryId)
    ElMessage.success('删除成功')
    loadTree()
  } catch (e) {
    // cancelled or handled by interceptor
  }
}
</script>

<style scoped lang="scss">
.search-form { margin-bottom: 12px; }
.tree-card { min-height: 320px; }
.tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 8px;

  .node-label {
    display: flex;
    align-items: center;
    gap: 6px;

    .node-name { font-size: 14px; }
    .node-status { margin-left: 8px; }
    .node-order { margin-left: 12px; font-size: 12px; color: #909399; }
  }
}
</style>
