<template>
  <div class="page-container">
    <!-- 搜索表单 -->
    <el-form :inline="true" :model="queryParams" class="search-form">
      <el-form-item label="菜单名称">
        <el-input v-model="queryParams.menuName" placeholder="请输入菜单名称" clearable @keyup.enter="loadTree" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadTree">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button v-perm="'system:menu:add'" type="primary" :icon="Plus" @click="handleAddRoot">新增顶级菜单</el-button>
    </div>

    <!-- 树形表格 -->
    <el-table
      v-loading="loading"
      :data="treeData"
      row-key="menuId"
      border
      stripe
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      default-expand-all
    >
      <el-table-column prop="menuName" label="菜单名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="menuType" label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="menuTypeTag(row.menuType)" size="small">{{ menuTypeText(row.menuType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路由路径" min-width="140" show-overflow-tooltip />
      <el-table-column prop="component" label="组件" min-width="160" show-overflow-tooltip />
      <el-table-column prop="perms" label="权限标识" min-width="160" show-overflow-tooltip />
      <el-table-column prop="orderNum" label="排序" width="90" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-perm="'system:menu:add'" link type="primary" :icon="Plus" @click="handleAddChild(row)">新增</el-button>
          <el-button v-perm="'system:menu:edit'" link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button v-perm="'system:menu:remove'" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 弹窗表单 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="0">
          <el-col :span="24">
            <el-form-item label="上级菜单" prop="parentId">
              <el-tree-select
                v-model="form.parentId"
                :data="parentTreeData"
                :props="treeSelectProps"
                node-key="menuId"
                check-strictly
                :render-after-expand="false"
                placeholder="请选择上级菜单（顶级留空）"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单类型" prop="menuType">
              <el-radio-group v-model="form.menuType">
                <el-radio value="M">目录</el-radio>
                <el-radio value="C">菜单</el-radio>
                <el-radio value="F">按钮</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.menuType !== 'F'" :span="12">
            <el-form-item label="路由路径" prop="path">
              <el-input v-model="form.path" :placeholder="form.menuType === 'M' ? '如 /system' : '如 user'" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.menuType === 'C'" :span="12">
            <el-form-item label="组件路径" prop="component">
              <el-input v-model="form.component" placeholder="如 system/user/index" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.menuType !== 'M'" :span="12">
            <el-form-item label="权限标识" prop="perms">
              <el-input v-model="form.perms" placeholder="如 system:user:list" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图标" prop="icon">
              <el-input v-model="form.icon" placeholder="如 Setting" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否显示" prop="visible">
              <el-radio-group v-model="form.visible">
                <el-radio :value="1">显示</el-radio>
                <el-radio :value="0">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="0">正常</el-radio>
                <el-radio :value="1">停用</el-radio>
              </el-radio-group>
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
defineOptions({ name: 'SysMenu' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { menuApi } from '@/api'

const loading = ref(false)
const submitLoading = ref(false)
const treeData = ref<any[]>([])
const parentTreeData = ref<any[]>([])

const treeSelectProps = { label: 'menuName', children: 'children' }

const queryParams = reactive({
  menuName: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = () => ({
  menuId: undefined as undefined | string,
  parentId: '0' as string,
  menuName: '',
  menuType: 'M',
  path: '',
  component: '',
  perms: '',
  icon: '',
  orderNum: 0,
  visible: 1,
  status: 0
})

const form = reactive(defaultForm())

const rules: FormRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

function menuTypeText(t: string): string {
  if (t === 'M') return '目录'
  if (t === 'C') return '菜单'
  if (t === 'F') return '按钮'
  return '-'
}

function menuTypeTag(t: string): 'info' | 'success' | 'warning' {
  if (t === 'M') return 'info'
  if (t === 'C') return 'success'
  if (t === 'F') return 'warning'
  return 'info'
}

onMounted(() => {
  loadTree()
})

async function loadTree() {
  loading.value = true
  try {
    const res: any = await menuApi.tree()
    const list = res.data || []
    // 顶级菜单选择树：根节点 + 已有树（node-key 为 menuId，根节点也用 menuId 字段）
    parentTreeData.value = [{ menuId: '0', menuName: '顶级菜单', children: list }]
    treeData.value = list
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleReset() {
  queryParams.menuName = ''
  loadTree()
}

function handleAddRoot() {
  isEdit.value = false
  dialogTitle.value = '新增顶级菜单'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function handleAddChild(row: any) {
  isEdit.value = false
  dialogTitle.value = '新增子菜单'
  Object.assign(form, defaultForm(), { parentId: row.menuId })
  dialogVisible.value = true
}

function handleEdit(row: any) {
  isEdit.value = true
  dialogTitle.value = '编辑菜单'
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
        await menuApi.update(form)
        ElMessage.success('修改成功')
      } else {
        await menuApi.save(form)
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
    await ElMessageBox.confirm(`确认删除菜单「${row.menuName}」吗？子菜单将一并删除。`, '提示', { type: 'warning' })
    await menuApi.remove(row.menuId)
    ElMessage.success('删除成功')
    loadTree()
  } catch (e) {
    // cancelled or handled by interceptor
  }
}
</script>

<style scoped lang="scss">
.search-form { margin-bottom: 12px; }
.action-bar { margin-bottom: 12px; }
</style>
