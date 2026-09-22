import request from '@/utils/request'

// 通用CRUD API 生成器
export function createCrudApi(baseUrl: string) {
  return {
    page(params: any) {
      return request({ url: `${baseUrl}/page`, method: 'get', params })
    },
    pagePost(data: any) {
      return request({ url: `${baseUrl}/page`, method: 'post', data })
    },
    getById(id: string | number) {
      return request({ url: `${baseUrl}/${id}`, method: 'get' })
    },
    save(data: any) {
      return request({ url: `${baseUrl}`, method: 'post', data })
    },
    update(data: any) {
      return request({ url: `${baseUrl}`, method: 'put', data })
    },
    remove(id: string | number) {
      return request({ url: `${baseUrl}/${id}`, method: 'delete' })
    },
    batchDelete(ids: (string | number)[]) {
      return request({ url: `${baseUrl}/batch-delete`, method: 'delete', data: { ids } })
    },
    listAll() {
      return request({ url: `${baseUrl}/list-all`, method: 'get' })
    },
    export(params?: any) {
      return request({ url: `${baseUrl}/export`, method: 'get', params, responseType: 'blob' })
    }
  }
}

// 商品SPU
export const goodsSpuApi = {
  page: (params: any) => request({ url: '/api/wms/goods/spu/page', method: 'get', params }),
  getById: (id: string) => request({ url: `/api/wms/goods/spu/${id}`, method: 'get' }),
  save: (data: any) => request({ url: '/api/wms/goods/spu', method: 'post', data }),
  update: (data: any) => request({ url: '/api/wms/goods/spu', method: 'put', data }),
  remove: (id: string) => request({ url: `/api/wms/goods/spu/${id}`, method: 'delete' }),
  batchDelete: (ids: string[]) => request({ url: '/api/wms/goods/spu/batch-delete', method: 'delete', data: { ids } }),
  batchChangeStatus: (data: any) => request({ url: '/api/wms/goods/spu/batch-change-status', method: 'put', data }),
  searchSku: (keyword: string, warehouseId?: string, supplierId?: string) => request({ url: '/api/wms/goods/sku/search', method: 'get', params: { keyword, warehouseId, supplierId } }),
  export: (params: any) => request({ url: '/api/wms/goods/spu/export', method: 'get', params, responseType: 'blob' }),
}

// 分类
export const categoryApi = {
  tree: () => request({ url: '/api/wms/category/tree', method: 'get' }),
  save: (data: any) => request({ url: '/api/wms/category', method: 'post', data }),
  update: (data: any) => request({ url: '/api/wms/category', method: 'put', data }),
  remove: (id: string) => request({ url: `/api/wms/category/${id}`, method: 'delete' }),
}

// 品牌
export const brandApi = createCrudApi('/api/wms/brand')
// 单位
export const unitApi = createCrudApi('/api/wms/unit')
// 供应商
export const supplierApi = createCrudApi('/api/wms/supplier')
// 客户
export const customerApi = createCrudApi('/api/wms/customer')
// 仓库
export const warehouseApi = createCrudApi('/api/wms/warehouse')
// 库区
export const areaApi = createCrudApi('/api/wms/area')
// 库位
export const locationApi = {
  ...createCrudApi('/api/wms/location'),
  batchGenerate: (data: any) => request({ url: '/api/wms/location/batch-generate', method: 'post', data }),
}
// 用品登记（办公用品/消耗品出入库记录）
export const officeRecordApi = {
  ...createCrudApi('/api/wms/office-record'),
  suggestName: (keyword: string) => request({ url: '/api/wms/office-record/suggest-name', method: 'get', params: { keyword } }),
}

// 采购
export const purchaseApi = {
  page: (data: any) => request({ url: '/api/wms/purchase/page', method: 'post', data }),
  getById: (id: string) => request({ url: `/api/wms/purchase/${id}`, method: 'get' }),
  save: (data: any) => request({ url: '/api/wms/purchase/save', method: 'post', data }),
  update: (data: any) => request({ url: '/api/wms/purchase/update', method: 'put', data }),
  remove: (id: string) => request({ url: `/api/wms/purchase/${id}`, method: 'delete' }),
  submit: (id: string) => request({ url: '/api/wms/purchase/submit', method: 'post', params: { id } }),
  audit: (data: any) => request({ url: '/api/wms/purchase/audit', method: 'post', data }),
  unaudit: (id: string) => request({ url: '/api/wms/purchase/unaudit', method: 'post', params: { id } }),
  void: (id: string) => request({ url: '/api/wms/purchase/void', method: 'post', params: { id } }),
  batchAudit: (data: any) => request({ url: '/api/wms/purchase/batch-audit', method: 'post', data }),
}

// 入库
export const stockInApi = {
  page: (data: any) => request({ url: '/api/wms/stock-in/page', method: 'post', data }),
  getById: (id: string) => request({ url: `/api/wms/stock-in/${id}`, method: 'get' }),
  save: (data: any) => request({ url: '/api/wms/stock-in/save', method: 'post', data }),
  update: (data: any) => request({ url: '/api/wms/stock-in/update', method: 'put', data }),
  remove: (id: string) => request({ url: `/api/wms/stock-in/${id}`, method: 'delete' }),
  submit: (id: string) => request({ url: '/api/wms/stock-in/submit', method: 'post', params: { id } }),
  audit: (data: any) => request({ url: '/api/wms/stock-in/audit', method: 'post', data }),
  unaudit: (id: string, remark?: string) => request({ url: '/api/wms/stock-in/unaudit', method: 'post', params: { id, remark } }),
  void: (id: string) => request({ url: '/api/wms/stock-in/void', method: 'post', params: { id } }),
  fromSource: (sourceBillNo: string) => request({ url: '/api/wms/stock-in/from-source', method: 'post', params: { sourceBillNo } }),
  export: (params: any) => request({ url: '/api/wms/stock-in/export', method: 'get', params, responseType: 'blob' }),
}

// 出库
export const stockOutApi = {
  page: (data: any) => request({ url: '/api/wms/stock-out/page', method: 'post', data }),
  getById: (id: string) => request({ url: `/api/wms/stock-out/${id}`, method: 'get' }),
  save: (data: any) => request({ url: '/api/wms/stock-out/save', method: 'post', data }),
  update: (data: any) => request({ url: '/api/wms/stock-out/update', method: 'put', data }),
  remove: (id: string) => request({ url: `/api/wms/stock-out/${id}`, method: 'delete' }),
  submit: (id: string) => request({ url: '/api/wms/stock-out/submit', method: 'post', params: { id } }),
  lockInventory: (id: string) => request({ url: '/api/wms/stock-out/lock-inventory', method: 'post', params: { id } }),
  pickConfirm: (id: string) => request({ url: '/api/wms/stock-out/pick-confirm', method: 'post', params: { id } }),
  audit: (data: any) => request({ url: '/api/wms/stock-out/audit', method: 'post', data }),
  void: (id: string) => request({ url: `/api/wms/stock-out/void`, method: 'post', params: { id } }),
  export: (params: any) => request({ url: '/api/wms/stock-out/export', method: 'get', params, responseType: 'blob' }),
}

// 调拨
export const transferApi = {
  page: (data: any) => request({ url: '/api/wms/transfer/page', method: 'post', data }),
  getById: (id: string) => request({ url: `/api/wms/transfer/${id}`, method: 'get' }),
  save: (data: any) => request({ url: '/api/wms/transfer/save', method: 'post', data }),
  update: (data: any) => request({ url: '/api/wms/transfer/', method: 'put', data }),
  remove: (id: string) => request({ url: `/api/wms/transfer/${id}`, method: 'delete' }),
  submit: (id: string) => request({ url: '/api/wms/transfer/submit', method: 'post', params: { id } }),
  audit: (data: any) => request({ url: '/api/wms/transfer/audit', method: 'post', data }),
  confirmOut: (id: string) => request({ url: '/api/wms/transfer/confirm-out', method: 'post', params: { id } }),
  confirmIn: (id: string) => request({ url: '/api/wms/transfer/confirm-in', method: 'post', params: { id } }),
  void: (id: string) => request({ url: '/api/wms/transfer/void', method: 'post', params: { id } }),
}

// 销售
export const saleApi = {
  page: (data: any) => request({ url: '/api/wms/sale/page', method: 'post', data }),
  getById: (id: string) => request({ url: `/api/wms/sale/${id}`, method: 'get' }),
  save: (data: any) => request({ url: '/api/wms/sale/save', method: 'post', data }),
  update: (data: any) => request({ url: '/api/wms/sale/update', method: 'put', data }),
  remove: (id: string) => request({ url: `/api/wms/sale/${id}`, method: 'delete' }),
  submit: (id: string) => request({ url: '/api/wms/sale/submit', method: 'post', params: { id } }),
  audit: (data: any) => request({ url: '/api/wms/sale/audit', method: 'post', data }),
  confirmOut: (id: string) => request({ url: '/api/wms/sale/confirm-out', method: 'post', params: { id } }),
  confirmPay: (data: any) => request({ url: '/api/wms/sale/confirm-pay', method: 'post', params: data }),
  complete: (id: string) => request({ url: '/api/wms/sale/complete', method: 'post', params: { id } }),
  void: (id: string) => request({ url: '/api/wms/sale/void', method: 'post', params: { id } }),
}

// 报损
export const lossApi = {
  page: (data: any) => request({ url: '/api/wms/loss/page', method: 'post', data }),
  getById: (id: string) => request({ url: `/api/wms/loss/${id}`, method: 'get' }),
  save: (data: any) => request({ url: '/api/wms/loss/save', method: 'post', data }),
  update: (data: any) => request({ url: '/api/wms/loss/update', method: 'put', data }),
  remove: (id: string) => request({ url: `/api/wms/loss/${id}`, method: 'delete' }),
  submit: (id: string) => request({ url: '/api/wms/loss/submit', method: 'post', params: { id } }),
  audit: (data: any) => request({ url: '/api/wms/loss/audit', method: 'post', data }),
  handle: (data: any) => request({ url: '/api/wms/loss/handle', method: 'post', data }),
  void: (id: string) => request({ url: '/api/wms/loss/void', method: 'post', params: { id } }),
}

// 盘点
export const checkApi = {
  page: (data: any) => request({ url: '/api/wms/check/page', method: 'post', data }),
  getById: (id: string) => request({ url: `/api/wms/check/${id}`, method: 'get' }),
  save: (data: any) => request({ url: '/api/wms/check/save', method: 'post', data }),
  remove: (id: string) => request({ url: `/api/wms/check/${id}`, method: 'delete' }),
  loadInventory: (data: any) => request({ url: '/api/wms/check/load-inventory', method: 'post', params: data }),
  startCheck: (id: string) => request({ url: '/api/wms/check/start-check', method: 'post', params: { id } }),
  inputActual: (data: any) => request({ url: '/api/wms/check/input-actual', method: 'post', data }),
  finishCheck: (id: string) => request({ url: '/api/wms/check/finish-check', method: 'post', params: { id } }),
  audit: (data: any) => request({ url: '/api/wms/check/audit', method: 'post', data }),
  handle: (id: string) => request({ url: '/api/wms/check/handle', method: 'post', params: { id } }),
  void: (id: string) => request({ url: '/api/wms/check/void', method: 'post', params: { id } }),
}

// 库存
export const inventoryApi = {
  page: (data: any) => request({ url: '/api/wms/inventory/page', method: 'post', data }),
  export: (params: any) => request({ url: '/api/wms/inventory/export', method: 'get', params, responseType: 'blob' }),
  bySku: (skuId: string) => request({ url: `/api/wms/inventory/by-sku/${skuId}`, method: 'get' }),
  logPage: (data: any) => request({ url: '/api/wms/inventory/log/page', method: 'post', data }),
}

// 仪表盘
export const dashboardApi = {
  summary: () => request({ url: '/api/report/dashboard/summary', method: 'get' }),
  trend30: (data: any) => request({ url: '/api/report/dashboard/trend-30', method: 'post', data }),
  topSku: (data: any) => request({ url: '/api/report/dashboard/top-in-out-sku', method: 'post', data }),
  warehousePie: () => request({ url: '/api/report/dashboard/warehouse-qty-pie', method: 'post', data: {} }),
}

// 系统管理
export const userApi = {
  ...createCrudApi('/api/system/user'),
  // 后端 batch-delete 接收 List<Long> 原始数组
  batchDelete: (ids: (string | number)[]) => request({ url: '/api/system/user/batch-delete', method: 'delete', data: ids }),
  resetPwd: (userId: string, password: string) => request({ url: '/api/system/user/reset-pwd', method: 'put', data: { userId, password } }),
  changeStatus: (userId: string, status: string) => request({ url: '/api/system/user/change-status', method: 'put', data: { userId, status } }),
  getRoleIds: (id: string) => request({ url: `/api/system/user/${id}/roleIds`, method: 'get' }),
}
export const roleApi = {
  ...createCrudApi('/api/system/role'),
  batchDelete: (ids: (string | number)[]) => request({ url: '/api/system/role/batch-delete', method: 'delete', data: ids }),
  list: () => request({ url: '/api/system/role/list', method: 'get' }),
  getMenuIds: (id: string) => request({ url: `/api/system/role/${id}/menuIds`, method: 'get' }),
  assignMenu: (data: { roleId: string; menuIds: (string | number)[] }) => request({ url: '/api/system/role/assign-menu', method: 'put', data }),
}
export const menuApi = {
  tree: () => request({ url: '/api/system/menu/tree', method: 'get' }),
  save: (data: any) => request({ url: '/api/system/menu', method: 'post', data }),
  update: (data: any) => request({ url: '/api/system/menu', method: 'put', data }),
  remove: (id: string) => request({ url: `/api/system/menu/${id}`, method: 'delete' }),
}
