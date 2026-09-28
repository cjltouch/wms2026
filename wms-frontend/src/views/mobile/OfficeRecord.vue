<template>
  <div class="m-office">
    <!-- ===== 沉浸式渐变头部 ===== -->
    <div class="hero">
      <div class="hero-orb orb-a"></div>
      <div class="hero-orb orb-b"></div>
      <div class="hero-orb orb-c"></div>

      <div class="hero-status"></div>

      <div class="hero-head">
        <div class="hero-back" @click="goBack">
          <van-icon name="arrow-left" size="18" />
        </div>
        <div class="hero-title">
          <span class="greeting">办公用品 👋</span>
          <span class="app-name">用品登记</span>
        </div>
        <div class="hero-avatar" @click="handleLogout">
          <van-icon name="user-o" size="18" />
        </div>
      </div>

      <!-- 搜索框 -->
      <div class="search-card">
        <van-search
          v-model="keyword"
          placeholder="用品名称"
          shape="round"
          :clearable="true"
          @search="onSearch"
          @clear="onClearKeyword"
          class="glass-search"
        />
      </div>

      <!-- 类型 Tab -->
      <div class="hero-tabs">
        <div class="tab-slider" :style="sliderStyle"></div>
        <div
          v-for="t in typeTabs"
          :key="t.value"
          class="hero-tab"
          :class="{ active: activeType === t.value }"
          :ref="(el) => setTabRef(String(t.value), el as HTMLElement)"
          @click="switchType(t.value)"
        >
          <span>{{ t.label }}</span>
        </div>
      </div>
    </div>

    <!-- 列表区 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh" class="pull-wrap">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="—— 没有更多了 ——"
        @load="onLoad"
      >
        <div
          v-for="(item, idx) in list"
          :key="`office-${item.recordId || idx}`"
          class="record-card"
          @click="goEdit(item)"
        >
          <div class="card-indicator" :class="typeClass(item.type)"></div>
          <div class="card-body">
            <div class="card-head">
              <span class="type-tag" :class="typeClass(item.type)">{{ typeText(item.type) }}</span>
              <span class="item-name">{{ item.itemName || '-' }}</span>
              <span class="qty-num" :class="typeClass(item.type)">{{ item.quantity || 0 }}<span class="unit">{{ item.unit || '' }}</span></span>
            </div>
            <div class="card-meta">
              <span v-if="item.spec" class="meta-tag">规格 {{ item.spec }}</span>
              <span class="person">
                <van-icon name="contact" size="12" />
                {{ item.personName || '-' }}
              </span>
            </div>
            <div class="card-foot">
              <span class="date">
                <van-icon name="calendar-o" size="12" />
                {{ item.recordDate || '-' }}
              </span>
              <span class="edit-hint">编辑 ›</span>
            </div>
            <div class="card-remark" v-if="item.remark">{{ item.remark }}</div>
          </div>
        </div>

        <van-empty
          v-if="finished && list.length === 0"
          description="暂无登记记录"
          image="search"
        />
      </van-list>
    </van-pull-refresh>

    <!-- 新增悬浮按钮 -->
    <div class="fab" @click="goAdd">
      <van-icon name="plus" size="24" color="#fff" />
    </div>

    <!-- 新增/编辑弹窗 -->
    <van-popup v-model:show="formVisible" position="bottom" round :style="{ height: '80vh' }" class="form-popup">
      <div class="popup-head">
        <span class="popup-title">{{ isEdit ? '编辑登记' : '新增登记' }}</span>
        <van-icon name="cross" size="18" color="#999" @click="formVisible = false" />
      </div>
      <div class="popup-body">
        <van-form @submit="handleSubmit">
          <van-cell-group inset>
            <van-field name="type" label="类型">
              <template #input>
                <div class="type-pick">
                  <div
                    v-for="t in typeOptions"
                    :key="t.value"
                    class="type-pick-item"
                    :class="[typeClass(t.value), { active: form.type === t.value }]"
                    @click="form.type = t.value"
                  >
                    {{ t.label }}
                  </div>
                </div>
              </template>
            </van-field>

            <van-field
              v-model="form.recordDate"
              label="日期"
              placeholder="默认今天"
              readonly
              @click="showDatePicker = true"
              right-icon="calendar-o"
            />

            <van-field
              v-model="form.itemName"
              label="名称"
              placeholder="请输入或选择名称"
              :rules="[{ required: true, message: '请输入名称' }]"
            >
              <template #extra>
                <van-icon name="search" size="16" @click="showNameSuggest = !showNameSuggest" />
              </template>
            </van-field>

            <div v-if="showNameSuggest && nameSuggestions.length" class="suggest-list">
              <div
                v-for="(n, i) in nameSuggestions"
                :key="i"
                class="suggest-item"
                @click="pickName(n)"
              >
                <span class="suggest-name">{{ n.itemName }}</span>
                <span class="suggest-meta" v-if="n.spec || n.unit">{{ [n.spec, n.unit].filter(Boolean).join(' / ') }}</span>
              </div>
            </div>

            <van-field v-model="form.spec" label="规格" placeholder="如 A4/70g" />
            <van-field v-model="form.unit" label="单位" placeholder="如 包、个、箱" />
            <van-field
              v-model.number="form.quantity"
              type="digit"
              label="数量"
              placeholder="请输入数量"
              :rules="[{ required: true, message: '请输入数量' }]"
            />
            <van-field v-model="form.personName" label="姓名" placeholder="经手/领用人" />
            <van-field
              v-model="form.remark"
              label="备注"
              type="textarea"
              placeholder="请输入备注"
              rows="2"
              autosize
            />
          </van-cell-group>

          <div class="form-actions">
            <van-button v-if="isEdit" type="danger" plain block @click="handleDelete">删除</van-button>
            <van-button type="primary" block native-type="submit" :loading="submitting">
              {{ isEdit ? '保存修改' : '确认新增' }}
            </van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <!-- 日期选择器 -->
    <van-popup v-model:show="showDatePicker" position="bottom" round>
      <van-date-picker
        v-model="datePickerVal"
        title="选择日期"
        @confirm="onDateConfirm"
        @cancel="showDatePicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog, showSuccessToast } from 'vant'
import { officeRecordApi } from '@/api'
import { useUserStore } from '@/store/user'

defineOptions({ name: 'MobileOfficeRecord' })

const router = useRouter()
const userStore = useUserStore()

// ===== 类型配置 =====
const typeOptions = [
  { value: 1, label: '入库' },
  { value: 2, label: '领取' },
  { value: 3, label: '报损' }
]
const typeTabs = [
  { value: -1, label: '全部' },
  ...typeOptions
]

function typeText(t: number): string {
  return typeOptions.find(o => o.value === t)?.label || '-'
}

function typeClass(t: number): string {
  if (t === 1) return 'in'
  if (t === 2) return 'out'
  if (t === 3) return 'loss'
  return 'in'
}

// ===== 列表状态 =====
const keyword = ref('')
const activeType = ref<number>(-1)
const list = ref<any[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const pageSize = 20

// ===== Tab 滑块 =====
const tabRefs = ref<Record<string, HTMLElement | null>>({})
const sliderStyle = ref({ width: '0px', transform: 'translateX(0px)' })

function setTabRef(key: string, el: HTMLElement | null) {
  tabRefs.value[key] = el
}

async function updateSlider() {
  await nextTick()
  const el = tabRefs.value[String(activeType.value)]
  if (el) {
    sliderStyle.value = {
      width: el.offsetWidth + 'px',
      transform: `translateX(${el.offsetLeft}px)`
    }
  }
}

watch(activeType, () => updateSlider())
onMounted(() => {
  setTimeout(updateSlider, 100)
})

// ===== 拉取列表 =====
async function onLoad() {
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize
    }
    if (keyword.value) params.itemName = keyword.value
    if (activeType.value !== -1) params.type = activeType.value

    const res: any = await officeRecordApi.page(params)
    const d = res.data || {}
    const rows = d.rows || d.records || d.list || []
    const total = d.total || 0
    if (pageNum.value === 1) {
      list.value = rows
    } else {
      list.value.push(...rows)
    }
    if (list.value.length >= total) {
      finished.value = true
    } else {
      pageNum.value++
    }
  } catch (e: any) {
    showToast(e.message || '加载失败')
    finished.value = true
  } finally {
    loading.value = false
  }
}

function onRefresh() {
  pageNum.value = 1
  finished.value = false
  loading.value = true
  onLoad()
  refreshing.value = false
}

function onSearch() {
  list.value = []
  pageNum.value = 1
  finished.value = false
  loading.value = true
  onLoad()
}

function onClearKeyword() {
  keyword.value = ''
  onSearch()
}

function switchType(val: number) {
  activeType.value = val
  list.value = []
  pageNum.value = 1
  finished.value = false
  loading.value = true
  onLoad()
}

// ===== 表单弹窗 =====
const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const showDatePicker = ref(false)
const showNameSuggest = ref(false)
const datePickerVal = ref<string[]>([])
const nameSuggestions = ref<any[]>([])

const defaultForm = () => ({
  recordId: undefined as any,
  recordDate: '',
  itemName: '',
  type: 1,
  spec: '',
  unit: '',
  quantity: 1,
  personName: '',
  remark: ''
})

const form = reactive(defaultForm())

async function fetchNameSuggest(q: string) {
  try {
    const res: any = await officeRecordApi.suggestName(q || '')
    nameSuggestions.value = res.data || []
  } catch { /* handled */ }
}

function goAdd() {
  isEdit.value = false
  Object.assign(form, defaultForm())
  const today = new Date()
  form.recordDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  datePickerVal.value = form.recordDate.split('-')
  fetchNameSuggest('')
  formVisible.value = true
}

function goEdit(row: any) {
  isEdit.value = true
  Object.assign(form, defaultForm(), row)
  datePickerVal.value = (form.recordDate || '').split('-')
  fetchNameSuggest(row.itemName || '')
  formVisible.value = true
}

function pickName(n: any) {
  form.itemName = n.itemName
  if (n.spec && !form.spec) form.spec = n.spec
  if (n.unit && !form.unit) form.unit = n.unit
  showNameSuggest.value = false
}

function onDateConfirm({ selectedValues }: { selectedValues: string[] }) {
  form.recordDate = selectedValues.join('-')
  showDatePicker.value = false
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value) {
      await officeRecordApi.update(form)
      showSuccessToast('修改成功')
    } else {
      await officeRecordApi.save(form)
      showSuccessToast('新增成功')
    }
    formVisible.value = false
    onRefresh()
  } catch (e: any) {
    showToast(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete() {
  try {
    await showConfirmDialog({ title: '确认删除这条记录？' })
    await officeRecordApi.remove(form.recordId)
    showSuccessToast('删除成功')
    formVisible.value = false
    onRefresh()
  } catch { /* cancelled */ }
}

function goBack() {
  router.back()
}

function handleLogout() {
  showConfirmDialog({ title: '确认退出登录？' })
    .then(() => {
      userStore.resetState()
      router.replace('/mobile/login')
    })
    .catch(() => {})
}
</script>

<style scoped>
.m-office {
  min-height: 100vh;
  background: #f4f5f9;
  display: flex;
  flex-direction: column;
  padding-bottom: 80px;
}

/* ===== 头部渐变区 ===== */
.hero {
  background: linear-gradient(135deg, #834d9b 0%, #a855c4 50%, #d04ed6 100%);
  padding: 0 20px 70px;
  position: relative;
  color: #fff;
  overflow: hidden;
}

.hero-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(45px);
  pointer-events: none;
}

.orb-a {
  width: 200px;
  height: 200px;
  background: rgba(255, 200, 230, 0.35);
  top: -40px;
  right: -30px;
}

.orb-b {
  width: 160px;
  height: 160px;
  background: rgba(160, 120, 220, 0.4);
  bottom: 80px;
  left: -50px;
}

.orb-c {
  width: 120px;
  height: 120px;
  background: rgba(255, 255, 255, 0.2);
  top: 30%;
  right: 18%;
}

.hero::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 60px;
  background: #f4f5f9;
  border-radius: 30px 30px 0 0;
  z-index: 3;
}

.hero-status {
  height: 44px;
  position: relative;
  z-index: 2;
}

.hero-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
  position: relative;
  z-index: 2;
}

.hero-back {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
}

.hero-title {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-left: 12px;
}

.greeting {
  font-size: 13px;
  opacity: 0.85;
}

.app-name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.hero-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.3);
  flex-shrink: 0;
}

/* 搜索框 */
.search-card {
  position: relative;
  z-index: 2;
  margin-bottom: 14px;
}

.glass-search {
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(14px);
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  overflow: hidden;
}

:deep(.glass-search .van-search__content) {
  background: transparent;
}

:deep(.glass-search .van-field__control) {
  color: #fff;
}

:deep(.glass-search .van-field__control::placeholder) {
  color: rgba(255, 255, 255, 0.6);
}

:deep(.glass-search .van-search) {
  background: transparent;
}

/* 类型 Tab */
.hero-tabs {
  position: relative;
  z-index: 2;
  display: flex;
  gap: 8px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
  border-radius: 20px;
  padding: 6px;
}

.tab-slider {
  position: absolute;
  top: 6px;
  left: 0;
  height: calc(100% - 12px);
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 0;
}

.hero-tab {
  flex: 1;
  padding: 7px 14px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  cursor: pointer;
  transition: color 0.25s;
  position: relative;
  z-index: 1;
  text-align: center;
}

.hero-tab.active {
  color: #834d9b;
  font-weight: 700;
}

/* ===== 列表区 ===== */
.pull-wrap {
  flex: 1;
  overflow-y: auto;
  padding-top: 10px;
}

.record-card {
  background: #fff;
  margin: 0 14px 12px;
  border-radius: 16px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: transform 0.15s;
}

.record-card:active {
  transform: scale(0.985);
}

.card-indicator {
  width: 5px;
  flex-shrink: 0;
}

.card-indicator.in {
  background: linear-gradient(180deg, #52c41a 0%, #389e0d 100%);
}

.card-indicator.out {
  background: linear-gradient(180deg, #1890ff 0%, #0050b3 100%);
}

.card-indicator.loss {
  background: linear-gradient(180deg, #ff4d4f 0%, #cf1322 100%);
}

.card-body {
  flex: 1;
  padding: 14px 16px;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.type-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 600;
  flex-shrink: 0;
}

.type-tag.in {
  color: #52c41a;
  background: #f6ffed;
}

.type-tag.out {
  color: #1890ff;
  background: #e6f7ff;
}

.type-tag.loss {
  color: #ff4d4f;
  background: #fff2f0;
}

.item-name {
  flex: 1;
  font-size: 15px;
  font-weight: 700;
  color: #1a1a2e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qty-num {
  font-size: 20px;
  font-weight: 800;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  flex-shrink: 0;
}

.qty-num.in {
  color: #52c41a;
}

.qty-num.out {
  color: #1890ff;
}

.qty-num.loss {
  color: #ff4d4f;
}

.qty-num .unit {
  font-size: 12px;
  font-weight: 500;
  margin-left: 2px;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #888;
}

.meta-tag {
  color: #834d9b;
  background: #834d9b1a;
  padding: 1px 7px;
  border-radius: 4px;
}

.person {
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px dashed #f0f0f0;
}

.date {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #bbb;
}

.edit-hint {
  font-size: 12px;
  color: #834d9b;
  font-weight: 600;
}

.card-remark {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
  line-height: 1.5;
  background: #fafafa;
  border-radius: 6px;
  padding: 6px 8px;
}

/* ===== 悬浮新增按钮 ===== */
.fab {
  position: fixed;
  right: 20px;
  bottom: 30px;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #834d9b, #d04ed6);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(131, 77, 155, 0.4);
  cursor: pointer;
  z-index: 10;
  transition: transform 0.2s;
}

.fab:active {
  transform: scale(0.9);
}

/* ===== 表单弹窗 ===== */
.form-popup {
  background: #f4f5f9;
}

.popup-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px 10px;
  border-bottom: 1px solid #f0f0f0;
}

.popup-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a2e;
}

.popup-body {
  padding: 10px 0;
  height: calc(80vh - 52px);
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.type-pick {
  display: flex;
  gap: 10px;
  width: 100%;
}

.type-pick-item {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  font-size: 14px;
  border-radius: 10px;
  background: #f5f5f5;
  color: #888;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.type-pick-item.in.active {
  background: #52c41a15;
  color: #52c41a;
  font-weight: 700;
  border-color: #52c41a;
}

.type-pick-item.out.active {
  background: #1890ff15;
  color: #1890ff;
  font-weight: 700;
  border-color: #1890ff;
}

.type-pick-item.loss.active {
  background: #ff4d4f15;
  color: #ff4d4f;
  font-weight: 700;
  border-color: #ff4d4f;
}

.suggest-list {
  margin: 0 16px 8px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.suggest-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}

.suggest-item:last-child {
  border-bottom: none;
}

.suggest-name {
  font-size: 13px;
  color: #1a1a2e;
  font-weight: 500;
}

.suggest-meta {
  font-size: 11px;
  color: #bbb;
}

.form-actions {
  padding: 16px;
  display: flex;
  gap: 10px;
}

:deep(.van-empty) {
  padding: 60px 0;
}
</style>
