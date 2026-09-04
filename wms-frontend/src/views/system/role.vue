<template>
  <div class="page-container">
    <!-- 搜索表单 -->
    <el-form :inline="true" :model="queryParams" class="search-form">
      <el-form-item label="角色名称">
        <el-input v-model="queryParams.roleName" placeholder="请输入角色名称" clearable @keyup.enter="handleQuery" />
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
      <el-table-column prop="roleName" label="角色名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="roleKey" label="角色标识" min-width="140" show-overflow-tooltip />
      <el-table-column prop="roleSort" label="排序" width="90" align="center" />
      <el-table-column prop="dataScope" label="数据范围" width="120" align="center">
        <template #default="{ row }">
          {{ dataScopeText(row.dataScope) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="success" :icon="Key" @click="handleAssignMenu(row)">分配权限</el-button>
          <el-button link type="danger" :icon="Delete" :disabled="row.roleId === 1" @click="handleDelete(row)">删除</el-button>
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="0">
          <el-col :span="12">
            <el-form-item label="角色名称" prop="roleName">
              <el-input v-model="form.roleName" placeholder="请输入角色名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色标识" prop="roleKey">
              <el-input v-model="form.roleKey" placeholder="如 admin" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="roleSort">
              <el-input-number v-model="form.roleSort" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据范围" prop="dataScope">
              <el-select v-model="form.dataScope" placeholder="请选择" style="width: 100%">
                <el-option v-for="opt in dataScopeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
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

    <!-- 分配菜单权限弹窗 -->
    <el-dialog v-model="assignVisible" :title="`分配菜单权限 - ${assignTarget?.roleName || ''}`" width="480px">
      <div class="assign-tip">
        勾选菜单=可查看该页面；勾选按钮=可执行对应操作。两者独立选择：只勾菜单即为"仅查看"，需要操作时再单独勾选对应按钮。
      </div>
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        :props="{ label: 'menuName', children: 'children' }"
        node-key="menuId"
        show-checkbox
        check-strictly
        default-expand-all
        :default-checked-keys="checkedMenuIds"
      />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="submitAssignMenu">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Key } from '@element-plus/icons-vue'
import { roleApi, menuApi } from '@/api'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const selectedIds = ref<any[]>([])

const dataScopeOptions = [
  { value: 1, label: '全部数据' },
  { value: 2, label: '自定义数据' },
  { value: 3, label: '本部门数据' },
  { value: 4, label: '本部门及以下数据' },
  { value: 5, label: '仅本人数据' }
]

function dataScopeText(v: any): string {
  return dataScopeOptions.find((o) => o.value === Number(v))?.label || '-'
}

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  roleName: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = () => ({
  roleId: undefined as undefined | string,
  roleName: '',
  roleKey: '',
  roleSort: 0,
  dataScope: 1,
  status: 0,
  remark: ''
})

const form = reactive(defaultForm())

const rules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }],
  roleSort: [{ required: true, message: '请输入排序', trigger: 'blur' }]
}

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res: any = await roleApi.page(queryParams)
    const d = res.data || {}
    tableData.value = d.rows || d.records || []
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
  queryParams.roleName = ''
  queryParams.pageNum = 1
  loadData()
}

function handleSelectionChange(rows: any[]) {
  selectedIds.value = rows
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增角色'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  isEdit.value = true
  dialogTitle.value = '编辑角色'
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
        await roleApi.update(form)
        ElMessage.success('修改成功')
      } else {
        await roleApi.save(form)
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
    await ElMessageBox.confirm(`确认删除角色「${row.roleName}」吗？`, '提示', { type: 'warning' })
    await roleApi.remove(row.roleId)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // cancelled or handled by interceptor
  }
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) return
  if (selectedIds.value.some((r: any) => r.roleId === 1)) {
    ElMessage.warning('内置超级管理员角色不可删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条数据吗？`, '提示', { type: 'warning' })
    await roleApi.batchDelete(selectedIds.value.map((r: any) => r.roleId))
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // cancelled or handled by interceptor
  }
}

// ===== 分配菜单权限 =====
const assignVisible = ref(false)
const assignLoading = ref(false)
const assignTarget = ref<any>(null)
const menuTree = ref<any[]>([])
const checkedMenuIds = ref<(string | number)[]>([])
const menuTreeRef = ref()

async function handleAssignMenu(row: any) {
  assignTarget.value = row
  assignVisible.value = true
  try {
    const [treeRes, idsRes]: any = await Promise.all([
      menuApi.tree(),
      roleApi.getMenuIds(row.roleId)
    ])
    menuTree.value = treeRes.data || []
    // check-strictly 模式下父子互不联动，回显时精确勾选所有已授权节点（目录/菜单/按钮各自独立显示勾选态）
    const assignedIds: (string | number)[] = (idsRes.data || []).map(String)
    checkedMenuIds.value = assignedIds
    await nextTick()
    menuTreeRef.value?.setCheckedKeys(assignedIds)
  } catch (e) {
    // handled by interceptor
  }
}

/** 构建 menuId -> parentId 映射，用于提交时补齐祖先层级 */
function buildParentMap(nodes: any[], map: Record<string, string> = {}, parentId: string | null = null): Record<string, string> {
  for (const n of nodes) {
    const id = String(n.menuId)
    map[id] = parentId as string
    if (n.children && n.children.length) {
      buildParentMap(n.children, map, id)
    }
  }
  return map
}

async function submitAssignMenu() {
  if (!assignTarget.value) return
  assignLoading.value = true
  try {
    // check-strictly 模式下勾选子节点不会自动带父节点，
    // 需手动补齐每个勾选节点的所有祖先（目录/菜单），保证菜单树层级完整、侧边栏能正常显示；
    // 但只勾菜单不勾按钮时，不会带入任何按钮权限 → 实现“仅查看”
    const checked = (menuTreeRef.value?.getCheckedKeys() || []) as (string | number)[]
    const parentMap = buildParentMap(menuTree.value)
    const idSet = new Set<string>(checked.map(String))
    for (const id of checked) {
      let pid = parentMap[String(id)]
      while (pid != null && pid !== '0' && pid !== '') {
        idSet.add(pid)
        pid = parentMap[pid]
      }
    }
    await roleApi.assignMenu({
      roleId: assignTarget.value.roleId,
      menuIds: [...idSet]
    })
    ElMessage.success('授权成功')
    assignVisible.value = false
    loadData()
  } catch (e) {
    // handled by interceptor
  } finally {
    assignLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.search-form { margin-bottom: 12px; }
.action-bar { margin-bottom: 12px; }
.pagination-bar { margin-top: 12px; display: flex; justify-content: flex-end; }
.assign-tip {
  margin-bottom: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
