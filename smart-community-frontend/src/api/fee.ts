import request from '@/utils/request'

// ============== 费用项目 ==============
export interface FeeItem {
  id?: number
  name: string
  code: string
  type?: string // 物业费/停车费/水电费/其他
  unit?: string // 元/㎡/月、元/月、元/吨 等
  price: number
  cycle?: string // 月/季/年
  isAutoGenerate?: number
  status?: number
  createTime?: string
}

export function getFeeItemList(params: any) {
  return request.get<any, { records: FeeItem[]; total: number }>('/api/v1/fee/item/list', { params })
}

export function getAllFeeItems() {
  return request.get<any, FeeItem[]>('/api/v1/fee/item/all')
}

export function createFeeItem(data: FeeItem) {
  return request.post('/api/v1/fee/item', data)
}

export function updateFeeItem(data: FeeItem) {
  return request.put('/api/v1/fee/item', data)
}

export function deleteFeeItem(id: number) {
  return request.delete(`/api/v1/fee/item/${id}`)
}

// ============== 账单管理 ==============
export interface BillItem {
  id?: number
  billNo?: string
  houseId: number
  houseNo?: string
  ownerName?: string
  feeItemId: number
  feeItemName?: string
  amount: number
  paidAmount?: number
  period?: string
  status?: number // 0=待缴费 1=已缴费 2=部分缴费 3=已作废
  payTime?: string
  payMethod?: string
  createTime?: string
}

export function getBillList(params: any) {
  return request.get<any, { records: BillItem[]; total: number }>('/api/v1/fee/bill/list', { params })
}

export function getBillById(id: number) {
  return request.get<any, BillItem>(`/api/v1/fee/bill/${id}`)
}

export function generateBills(data: { period: string; feeItemIds?: number[] }) {
  return request.post('/api/v1/fee/bill/generate', data)
}

/** 账单缴费：经统一支付网关；默认 mock 通道，返回 simulated/tradeNo 等 */
export function payBill(id: number, data: { payMethod: string; amount?: number }) {
  return request.post<any, {
    success?: boolean
    simulated?: boolean
    channel?: string
    tradeNo?: string
    amount?: number
    payMethod?: number
    message?: string
  }>(`/api/v1/fee/bill/${id}/pay`, data)
}

export function remindBill(id: number) {
  return request.post(`/api/v1/fee/bill/${id}/remind`)
}

export function cancelBill(id: number) {
  return request.put(`/api/v1/fee/bill/${id}/cancel`)
}

// ============== 缴费记录 ==============
export interface PaymentItem {
  id?: number
  billId: number
  billNo?: string
  amount: number
  payMethod?: string
  transactionNo?: string
  payTime?: string
  payBy?: string
}

export function getPaymentList(params: any) {
  return request.get<any, { records: PaymentItem[]; total: number }>('/api/v1/fee/payment/list', { params })
}

// ============== 押金管理 ==============
export interface DepositItem {
  id?: number
  depositNo?: string
  ownerId?: number
  houseId: number
  houseNo?: string
  ownerName?: string
  depositType?: number // 1装修 2车位 3其他
  type?: string // 兼容旧字段
  amount: number
  refundedAmount?: number
  status?: number // 0已收 1部分退 2已全退
  remark?: string
  receiveTime?: string
  createTime?: string
}

export function getDepositList(params: any) {
  return request.get<any, { records: DepositItem[]; total: number }>('/api/v1/fee/deposit/list', { params })
}

export function createDeposit(data: DepositItem) {
  return request.post('/api/v1/fee/deposit', data)
}

export function refundDeposit(id: number) {
  return request.put(`/api/v1/fee/deposit/${id}/refund`)
}

// ============== 退款管理 ==============
export interface RefundItem {
  id?: number
  billId?: number
  depositId?: number
  amount: number
  reason: string
  status?: number // 0=待审批 1=已通过 2=已驳回
  applyTime?: string
  approveTime?: string
  approverName?: string
  approveRemark?: string
}

export function getRefundList(params: any) {
  return request.get<any, { records: RefundItem[]; total: number }>('/api/v1/fee/refund/list', { params })
}

export function applyRefund(data: RefundItem) {
  return request.post('/api/v1/fee/refund', data)
}

export function approveRefund(id: number, approved: boolean, remark?: string) {
  return request.put(`/api/v1/fee/refund/${id}/approve`, { approved, remark })
}

// ============== 财务报表 ==============
export function getFeeReport(params: any) {
  return request.get('/api/v1/fee/report/summary', { params })
}

export function getArrearsReport(params: any) {
  return request.get('/api/v1/fee/report/arrears', { params })
}

export function exportFeeReport(params: any) {
  return request.get('/api/v1/fee/report/export', { params, responseType: 'blob' })
}