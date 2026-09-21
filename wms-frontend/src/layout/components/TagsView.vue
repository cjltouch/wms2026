<template>
  <div class="tags-view-wrapper">
    <el-scrollbar class="tags-scrollbar">
      <div class="tags-container">
        <div
          v-for="tag in tabsStore.visitedViews"
          :key="tag.fullPath"
          class="tag-item"
          :class="{ active: tag.fullPath === currentFullPath, affix: tag.affix }"
          @click="goTo(tag)"
          @contextmenu.prevent="openContextMenu($event, tag)"
        >
          <span class="tag-title" :title="tag.title">{{ tag.title }}</span>
          <el-icon
            v-if="!tag.affix"
            class="tag-close"
            @click.stop="handleClose(tag)"
          >
            <Close />
          </el-icon>
          <el-icon v-else class="tag-affix-icon" size="12">
            <Link />
          </el-icon>
        </div>
      </div>
    </el-scrollbar>

    <!-- 右键菜单 -->
    <ul
      v-show="contextMenu.visible"
      class="context-menu"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
    >
      <li @click="handleClose(contextMenu.tag!)">
        <el-icon><Close /></el-icon>
        <span>关闭当前</span>
      </li>
      <li @click="handleCloseOthers(contextMenu.tag!)">
        <el-icon><CircleClose /></el-icon>
        <span>关闭其他</span>
      </li>
      <li @click="handleCloseLeft(contextMenu.tag!)">
        <el-icon><Back /></el-icon>
        <span>关闭左侧</span>
      </li>
      <li @click="handleCloseRight(contextMenu.tag!)">
        <el-icon><Right /></el-icon>
        <span>关闭右侧</span>
      </li>
      <li class="divider" />
      <li @click="handleCloseAll">
        <el-icon><FolderDelete /></el-icon>
        <span>关闭所有</span>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTabsStore, type VisitedView } from '@/store/tabs'
import { Close, Link, CircleClose, Back, Right, FolderDelete } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const tabsStore = useTabsStore()

const currentFullPath = computed(() => route.fullPath)

/** 右键菜单状态 */
const contextMenu = reactive<{
  visible: boolean
  x: number
  y: number
  tag: VisitedView | null
}>({
  visible: false,
  x: 0,
  y: 0,
  tag: null
})

/** 全局点击关闭右键菜单 */
function closeContextMenu() {
  contextMenu.visible = false
  contextMenu.tag = null
}

onMounted(() => {
  document.addEventListener('click', closeContextMenu)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeContextMenu)
})

/** 打开右键菜单 */
function openContextMenu(e: MouseEvent, tag: VisitedView) {
  contextMenu.visible = true
  contextMenu.x = e.clientX
  contextMenu.y = e.clientY
  contextMenu.tag = tag
}

/** 跳转到指定标签页 */
function goTo(tag: VisitedView) {
  if (tag.fullPath === currentFullPath.value) return
  router.push(tag.fullPath)
}

/** 关闭单个标签页 */
function handleClose(tag: VisitedView) {
  const nextPath = tabsStore.closeTab(tag, currentFullPath.value)
  if (nextPath) {
    nextTick(() => router.push(nextPath))
  }
  closeContextMenu()
}

/** 关闭其他标签页 */
function handleCloseOthers(tag: VisitedView) {
  tabsStore.closeOthers(tag)
  // 如果当前路由被关闭了，跳转到保留的第一个
  if (!tabsStore.visitedViews.find((v) => v.fullPath === currentFullPath.value)) {
    const first = tabsStore.visitedViews[0]
    if (first) nextTick(() => router.push(first.fullPath))
  }
  closeContextMenu()
}

/** 关闭左侧标签页 */
function handleCloseLeft(tag: VisitedView) {
  tabsStore.closeLeft(tag)
  if (!tabsStore.visitedViews.find((v) => v.fullPath === currentFullPath.value)) {
    const first = tabsStore.visitedViews[0]
    if (first) nextTick(() => router.push(first.fullPath))
  }
  closeContextMenu()
}

/** 关闭右侧标签页 */
function handleCloseRight(tag: VisitedView) {
  tabsStore.closeRight(tag)
  if (!tabsStore.visitedViews.find((v) => v.fullPath === currentFullPath.value)) {
    const first = tabsStore.visitedViews[0]
    if (first) nextTick(() => router.push(first.fullPath))
  }
  closeContextMenu()
}

/** 关闭所有标签页 */
function handleCloseAll() {
  const affixPath = tabsStore.closeAll()
  if (affixPath) {
    nextTick(() => router.push(affixPath))
  }
  closeContextMenu()
}

/** 路由变化时自动添加标签页 */
watch(
  () => route.fullPath,
  () => {
    tabsStore.addTab(route)
  },
  { immediate: true }
)
</script>

<style scoped lang="scss">
.tags-view-wrapper {
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 4px 12px 0;
  position: relative;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.tags-scrollbar {
  :deep(.el-scrollbar__wrap) {
    height: 36px;
    overflow-y: hidden;
  }
  :deep(.el-scrollbar__bar.is-horizontal) {
    height: 4px;
  }
}

.tags-container {
  display: flex;
  gap: 6px;
  padding-bottom: 4px;
  white-space: nowrap;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0 10px;
  height: 28px;
  border: 1px solid #d8dce5;
  border-radius: 3px;
  background: #fff;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  max-width: 160px;

  &:hover {
    color: #409eff;
    border-color: #409eff;
  }

  &.active {
    background: #ecf5ff;
    border-color: #409eff;
    color: #409eff;

    &::before {
      content: '';
      display: inline-block;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #409eff;
      margin-right: 2px;
      flex-shrink: 0;
    }
  }

  &.affix {
    background: #f4f4f5;
    color: #909399;
    cursor: default;

    &:hover {
      border-color: #d8dce5;
      color: #909399;
    }
  }

  .tag-title {
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .tag-close {
    font-size: 12px;
    border-radius: 50%;
    padding: 2px;
    transition: all 0.2s;

    &:hover {
      background: #f56c6c;
      color: #fff;
    }
  }

  .tag-affix-icon {
    color: #909399;
  }
}

/* 右键菜单 */
.context-menu {
  position: fixed;
  z-index: 3000;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
  padding: 4px 0;
  margin: 0;
  list-style: none;
  min-width: 140px;

  li {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 14px;
    font-size: 13px;
    color: #606266;
    cursor: pointer;
    transition: background 0.15s;

    &:hover {
      background: #ecf5ff;
      color: #409eff;
    }

    .el-icon {
      font-size: 14px;
    }

    &.divider {
      height: 1px;
      padding: 0;
      margin: 4px 0;
      background: #ebeef5;
      cursor: default;

      &:hover {
        background: #ebeef5;
      }
    }
  }
}
</style>
