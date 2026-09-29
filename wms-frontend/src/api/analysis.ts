import request from '@/utils/request'

export interface ReplenishmentReq {
  dimension: 'sku' | 'innerCode'
  historyDays: number
  forecastDays: number
  warehouseId?: string
  categoryId?: string
}

export interface ReplenishmentItem {
  skuId: string
  skuCode: string
  skuName: string
  specText: string
  color: string
  innerCode: string
  historyQty: number
  dailyAvgQty: number
  availableQty: number
  forecastQty: number
  suggestQty: number
  supportDays: number | null
  status: 1 | 2 | 3
  defaultCost: number
  suggestAmount: number
}

// 智能补货分析
export const analysisApi = {
  replenishment: (data: ReplenishmentReq) =>
    request({ url: '/api/analysis/replenishment', method: 'post', data }),
}
