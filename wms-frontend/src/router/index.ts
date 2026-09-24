import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'
import NProgress from 'nprogress'

const Layout = () => import('@/layout/index.vue')

export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/print/:billType/:id',
    name: 'BillPrint',
    component: () => import('@/views/print/index.vue'),
    meta: { title: '单据打印' }
  },
  {
    path: '/mobile/login',
    name: 'MobileLogin',
    component: () => import('@/views/mobile/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/mobile/approval',
    name: 'MobileApprovalList',
    component: () => import('@/views/mobile/ApprovalList.vue'),
    meta: { title: '采购审批' }
  },
  {
    path: '/mobile/approval/:id',
    name: 'MobileApprovalDetail',
    component: () => import('@/views/mobile/ApprovalDetail.vue'),
    meta: { title: '审批详情' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'Odometer' }
      }
    ]
  },
  {
    path: '/goods',
    component: Layout,
    meta: { title: '商品管理', icon: 'Goods' },
    children: [
      {
        path: 'spu',
        name: 'GoodsSpu',
        component: () => import('@/views/goods/spu.vue'),
        meta: { title: '商品SPU' }
      },
      {
        path: 'category',
        name: 'GoodsCategory',
        component: () => import('@/views/goods/category.vue'),
        meta: { title: '商品分类' }
      },
      {
        path: 'brand',
        name: 'GoodsBrand',
        component: () => import('@/views/goods/brand.vue'),
        meta: { title: '品牌管理' }
      },
      {
        path: 'unit',
        name: 'GoodsUnit',
        component: () => import('@/views/goods/unit.vue'),
        meta: { title: '计量单位' }
      }
    ]
  },
  {
    path: '/partner',
    component: Layout,
    meta: { title: '往来管理', icon: 'User' },
    children: [
      {
        path: 'supplier',
        name: 'Supplier',
        component: () => import('@/views/partner/supplier.vue'),
        meta: { title: '供应商管理' }
      },
      {
        path: 'customer',
        name: 'Customer',
        component: () => import('@/views/partner/customer.vue'),
        meta: { title: '客户管理' }
      }
    ]
  },
  {
    path: '/warehouse',
    component: Layout,
    meta: { title: '仓库管理', icon: 'House' },
    children: [
      {
        path: 'list',
        name: 'Warehouse',
        component: () => import('@/views/warehouse/index.vue'),
        meta: { title: '仓库管理' }
      },
      {
        path: 'area',
        name: 'WarehouseArea',
        component: () => import('@/views/warehouse/area.vue'),
        meta: { title: '库区管理' }
      },
      {
        path: 'location',
        name: 'WarehouseLocation',
        component: () => import('@/views/warehouse/location.vue'),
        meta: { title: '库位管理' }
      }
    ]
  },
  {
    path: '/purchase',
    component: Layout,
    meta: { title: '采购管理', icon: 'ShoppingCart' },
    children: [
      {
        path: 'order',
        name: 'PurchaseOrder',
        component: () => import('@/views/purchase/index.vue'),
        meta: { title: '采购订单' }
      }
    ]
  },
  {
    path: '/stockin',
    component: Layout,
    meta: { title: '入库管理', icon: 'Download' },
    children: [
      {
        path: 'order',
        name: 'StockIn',
        component: () => import('@/views/stockin/index.vue'),
        meta: { title: '入库单' }
      }
    ]
  },
  {
    path: '/stockout',
    component: Layout,
    meta: { title: '出库管理', icon: 'Upload' },
    children: [
      {
        path: 'order',
        name: 'StockOut',
        component: () => import('@/views/stockout/index.vue'),
        meta: { title: '出库单' }
      }
    ]
  },
  {
    path: '/transfer',
    component: Layout,
    meta: { title: '调拨管理', icon: 'Switch' },
    children: [
      {
        path: 'order',
        name: 'Transfer',
        component: () => import('@/views/transfer/index.vue'),
        meta: { title: '调拨单' }
      }
    ]
  },
  {
    path: '/sale',
    component: Layout,
    meta: { title: '销售管理', icon: 'Money' },
    children: [
      {
        path: 'order',
        name: 'Sale',
        component: () => import('@/views/sale/index.vue'),
        meta: { title: '销售单' }
      }
    ]
  },
  {
    path: '/loss',
    component: Layout,
    meta: { title: '报损管理', icon: 'Warning' },
    children: [
      {
        path: 'order',
        name: 'Loss',
        component: () => import('@/views/loss/index.vue'),
        meta: { title: '报损单' }
      }
    ]
  },
  {
    path: '/check',
    component: Layout,
    meta: { title: '盘点管理', icon: 'DocumentChecked' },
    children: [
      {
        path: 'order',
        name: 'Check',
        component: () => import('@/views/check/index.vue'),
        meta: { title: '盘点单' }
      }
    ]
  },
  {
    path: '/inventory',
    component: Layout,
    meta: { title: '库存管理', icon: 'Box' },
    children: [
      {
        path: 'list',
        name: 'Inventory',
        component: () => import('@/views/inventory/index.vue'),
        meta: { title: '库存查询' }
      },
      {
        path: 'log',
        name: 'InventoryLog',
        component: () => import('@/views/inventory/log.vue'),
        meta: { title: '库存流水' }
      }
    ]
  },
  {
    path: '/office',
    component: Layout,
    meta: { title: '用品管理', icon: 'Collection' },
    children: [
      {
        path: 'record',
        name: 'OfficeRecord',
        component: () => import('@/views/office/index.vue'),
        meta: { title: '用品登记' }
      }
    ]
  },
  {
    path: '/system',
    component: Layout,
    meta: { title: '系统管理', icon: 'Setting' },
    children: [
      {
        path: 'user',
        name: 'SysUser',
        component: () => import('@/views/system/user.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'role',
        name: 'SysRole',
        component: () => import('@/views/system/role.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'menu',
        name: 'SysMenu',
        component: () => import('@/views/system/menu.vue'),
        meta: { title: '菜单管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

const whiteList = ['/login', '/mobile/login']

/**
 * 从后端菜单权限树收集允许访问的绝对路径集合
 * 目录节点下钻，页面节点收集：'/goods' + 'spu' => '/goods/spu'
 */
function collectAllowedPaths(routers: any[], prefix = '', set = new Set<string>()): Set<string> {
  for (const r of routers || []) {
    const abs = prefix
      ? (prefix.endsWith('/') ? prefix.slice(0, -1) : prefix) + '/' + (r.path || '').replace(/^\/+/, '')
      : (r.path?.startsWith('/') ? r.path : '/' + (r.path || ''))
    if (r.children && r.children.length) {
      collectAllowedPaths(r.children, abs, set)
    } else {
      set.add(abs)
    }
  }
  return set
}

// 任何登录用户都可访问的兜底页面
const alwaysAllowed = ['/dashboard']

/**
 * 检测当前设备是否为手机或平板（通过 UserAgent + 屏幕宽度双重判断）
 */
function isMobileDevice(): boolean {
  const ua = navigator.userAgent.toLowerCase()
  const isMobileUA = /iphone|ipad|ipod|android|mobile|harmonyos|openharmony|micromessenger/.test(ua)
  const narrowScreen = window.innerWidth <= 1024
  return isMobileUA || narrowScreen
}

router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  document.title = `${to.meta.title || ''} - WMS仓储管理系统`

  const userStore = useUserStore()
  const isMobile = to.path.startsWith('/mobile')
  const deviceIsMobile = isMobileDevice()

  // 手机/平板访问 PC 页面时自动跳转到移动端
  if (deviceIsMobile && !isMobile && !to.path.startsWith('/print/') && to.path !== '/login') {
    if (userStore.token) {
      next('/mobile/approval')
    } else {
      next('/mobile/login')
    }
    NProgress.done()
    return
  }

  if (userStore.token) {
    if (to.path === '/login') {
      next('/')
      NProgress.done()
    } else if (to.path === '/mobile/login') {
      next('/mobile/approval')
      NProgress.done()
    } else {
      if (!userStore.username) {
        try {
          await userStore.fetchUserInfo()
        } catch (e) {
          userStore.resetState()
          next(isMobile ? '/mobile/login' : '/login')
          NProgress.done()
          return
        }
      }
      // 移动端路由跳过 PC 端菜单权限校验
      if (!isMobile && !to.path.startsWith('/print/')) {
        const allowed = collectAllowedPaths(userStore.routers)
        const pass = allowed.has(to.path) || alwaysAllowed.includes(to.path)
        if (!pass) {
          next('/dashboard')
          NProgress.done()
          return
        }
      }
      next()
    }
  } else {
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next(isMobile ? '/mobile/login' : '/login')
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
