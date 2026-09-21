<template>
  <div class="page-container">
    <!-- 搜索表单 -->
    <el-form :inline="true" :model="queryParams" class="search-form">
      <el-form-item label="用户名">
        <el-input v-model="queryParams.userName" placeholder="请输入用户名" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="queryParams.phonenumber" placeholder="请输入手机号" clearable @keyup.enter="handleQuery" />
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
      <el-button v-perm="'system:user:add'" type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
      <el-button v-perm="'system:user:remove'" type="danger" :icon="Delete" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除</el-button>
    </div>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="tableData" border stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column type="index" label="#" width="50" align="center" />
      <el-table-column prop="userName" label="用户名" min-width="110" show-overflow-tooltip />
      <el-table-column prop="realName" label="真实姓名" min-width="100" show-overflow-tooltip />
      <el-table-column prop="nickName" label="昵称" min-width="100" show-overflow-tooltip />
      <el-table-column prop="phonenumber" label="手机号" min-width="120" show-overflow-tooltip />
      <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-switch
            v-model="row.status"
            active-value="0"
            inactive-value="1"
            :disabled="row.userId === 1 || !hasPerm('system:user:edit')"
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="loginTime" label="最后登录" width="160" align="center" />
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-perm="'system:user:edit'" link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button v-perm="'system:user:resetPwd'" link type="warning" :icon="Key" @click="handleResetPwd(row)">重置密码</el-button>
          <el-button v-perm="'system:user:remove'" link type="danger" :icon="Delete" :disabled="row.userId === 1" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="0">
          <el-col :span="12">
            <el-form-item label="用户名" prop="user.userName">
              <el-input v-model="form.user.userName" placeholder="请输入登录用户名" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="user.realName">
              <el-input v-model="form.user.realName" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" prop="user.nickName">
              <el-input v-model="form.user.nickName" placeholder="请输入昵称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="user.phonenumber">
              <el-input v-model="form.user.phonenumber" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="user.email">
              <el-input v-model="form.user.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col v-if="!isEdit" :span="12">
            <el-form-item label="初始密码" prop="user.password">
              <el-input v-model="form.user.password" type="password" show-password placeholder="请输入初始密码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="user.status">
              <el-radio-group v-model="form.user.status">
                <el-radio value="0">正常</el-radio>
                <el-radio value="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所属角色" prop="roleIds">
              <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
                <el-option v-for="r in roleOptions" :key="r.roleId" :label="r.roleName" :value="r.roleId" :disabled="Number(r.status) === 1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="user.remark">
              <el-input v-model="form.user.remark" type="textarea" :rows="2" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="420px" @close="resetPwdForm.password = ''">
      <el-form ref="resetPwdFormRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="90px">
        <el-form-item label="用户名">
          <el-input :model-value="resetPwdTarget?.userName" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetPwdForm.password" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetPwdLoading" @click="submitResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'SysUser' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Key } from '@element-plus/icons-vue'
import { userApi, roleApi } from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const hasPerm = (perm: string) => {
  const perms = userStore.permissions || []
  return perms.includes('*:*:*') || perms.includes(perm) || perms.some((p: string) => p.endsWith(':*') && perm.startsWith(p.slice(0, -1)))
}

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const selectedIds = ref<any[]>([])
const roleOptions = ref<any[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  userName: '',
  phonenumber: '',
  status: undefined as undefined | string
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = () => ({
  userId: undefined as undefined | string,
  user: {
    userName: '',
    realName: '',
    nickName: '',
    phonenumber: '',
    email: '',
    password: '',
    status: '0',
    remark: ''
  },
  roleIds: [] as string[]
})

const form = reactive(defaultForm())

const rules: FormRules = {
  'user.userName': [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  'user.realName': [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  'user.password': [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

// 重置密码
const resetPwdVisible = ref(false)
const resetPwdLoading = ref(false)
const resetPwdTarget = ref<any>(null)
const resetPwdFormRef = ref<FormInstance>()
const resetPwdForm = reactive({ password: '' })
const resetPwdRules: FormRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

onMounted(() => {
  loadData()
  loadRoleOptions()
})

async function loadRoleOptions() {
  try {
    const res: any = await roleApi.list()
    roleOptions.value = res.data || []
  } catch (e) {
    // handled by interceptor
  }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await userApi.page(queryParams)
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
  queryParams.userName = ''
  queryParams.phonenumber = ''
  queryParams.status = undefined
  queryParams.pageNum = 1
  loadData()
}

function handleSelectionChange(rows: any[]) {
  selectedIds.value = rows
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  Object.assign(form, defaultForm())
  try {
    const [detailRes, roleIdsRes]: any = await Promise.all([
      userApi.getById(row.userId),
      userApi.getRoleIds(row.userId)
    ])
    const u = detailRes.data || {}
    Object.assign(form.user, {
      userId: u.userId,
      userName: u.userName || '',
      realName: u.realName || '',
      nickName: u.nickName || '',
      phonenumber: u.phonenumber || '',
      email: u.email || '',
      status: u.status ?? '0',
      remark: u.remark || ''
    })
    form.roleIds = (roleIdsRes.data || []).map(String)
  } catch (e) {
    // handled by interceptor
  }
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
      // 后端 UserSaveReq 结构：{ user: {...}, roleIds: [...] }
      const payload: any = {
        user: { ...form.user },
        roleIds: form.roleIds
      }
      if (isEdit.value) {
        delete payload.user.password
        await userApi.update(payload)
        ElMessage.success('修改成功')
      } else {
        await userApi.save(payload)
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

async function handleStatusChange(row: any) {
  const target = row.status === '1' ? '1' : '0'
  const action = target === '1' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}用户「${row.userName}」吗？`, '提示', { type: 'warning' })
    await userApi.changeStatus(row.userId, target)
    ElMessage.success(`${action}成功`)
  } catch (e) {
    row.status = target === '1' ? '0' : '1' // 取消则回滚开关
    return
  }
  loadData()
}

function handleResetPwd(row: any) {
  resetPwdTarget.value = row
  resetPwdForm.password = ''
  resetPwdVisible.value = true
}

async function submitResetPwd() {
  if (!resetPwdFormRef.value) return
  await resetPwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    resetPwdLoading.value = true
    try {
      await userApi.resetPwd(resetPwdTarget.value.userId, resetPwdForm.password)
      ElMessage.success('密码重置成功')
      resetPwdVisible.value = false
    } catch (e) {
      // handled by interceptor
    } finally {
      resetPwdLoading.value = false
    }
  })
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除用户「${row.userName}」吗？`, '提示', { type: 'warning' })
    await userApi.remove(row.userId)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // cancelled or handled by interceptor
  }
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) return
  if (selectedIds.value.some((r: any) => r.userId === 1)) {
    ElMessage.warning('内置管理员不可删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条数据吗？`, '提示', { type: 'warning' })
    await userApi.batchDelete(selectedIds.value.map((r: any) => r.userId))
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
