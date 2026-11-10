import request from '@/utils/request'

// ============== 商品管理 ==============
export interface ProductItem {
  id?: number
  productName: string
  categoryId?: number
  price: number
  originPrice?: number
  stock: number
  productImage?: string
  description?: string
  isOnSale?: number // 0=下架 1=上架
  status?: number // 0=停用 1=启用
  sales?: number
  createTime?: string
}

export function getProductList(params: any) {
  return request.get<any, { records: ProductItem[]; total: number }>('/api/v1/product/list', { params })
}

export function getProductById(id: number) {
  return request.get<any, ProductItem>(`/api/v1/product/${id}`)
}

export function createProduct(data: ProductItem) {
  return request.post('/api/v1/product', data)
}

export function updateProduct(data: ProductItem) {
  return request.put('/api/v1/product', data)
}

export function deleteProduct(id: number) {
  return request.delete(`/api/v1/product/${id}`)
}

export function toggleProductStatus(id: number, status: number) {
  return request.put(`/api/v1/product/${id}/status`, { status })
}

// ============== 订单管理 ==============
export interface OrderItem {
  id?: number
  orderNo?: string
  userId: number
  userName?: string
  totalAmount: number
  status?: number // 0=待付款 1=待发货 2=待收货 3=已完成 4=已取消
  items?: string
  itemList?: OrderDetailItem[]
  address?: string
  phone?: string
  createTime?: string
}

export interface OrderDetailItem {
  productId: number
  productName?: string
  price: number
  quantity: number
}

export function getOrderList(params: any) {
  return request.get<any, { records: OrderItem[]; total: number }>('/api/v1/order/list', { params })
}

export function getOrderById(id: number) {
  return request.get<any, OrderItem>(`/api/v1/order/${id}`)
}

export function createOrder(data: OrderItem) {
  return request.post('/api/v1/order', data)
}

export function cancelOrder(id: number) {
  return request.put('/api/v1/order/' + id + '/cancel')
}

/** 订单支付：经统一支付网关；默认 mock 通道，返回 simulated/tradeNo 等 */
export function payOrder(id: number, data?: { payMethod?: number }) {
  return request.put<any, {
    success?: boolean
    simulated?: boolean
    channel?: string
    tradeNo?: string
    amount?: number
    payMethod?: number
    message?: string
  }>('/api/v1/order/' + id + '/pay', data || {})
}

export function shipOrder(id: number) {
  return request.put('/api/v1/order/' + id + '/ship')
}

export function completeOrder(id: number) {
  return request.put('/api/v1/order/' + id + '/complete')
}

// ============== 家政服务 ==============
export interface ServiceItem {
  id?: number
  serviceName: string
  serviceType: number
  price: number
  unit?: string
  description?: string
  serviceImage?: string
  isOnSale?: number
  staffCount?: number
  sort?: number
  createTime?: string
}

export function getServiceList(params: any) {
  return request.get<any, { records: ServiceItem[]; total: number }>('/api/v1/service/list', { params })
}

export function createService(data: ServiceItem) {
  return request.post('/api/v1/service', data)
}

export function updateService(data: ServiceItem) {
  return request.put('/api/v1/service', data)
}

export function deleteService(id: number) {
  return request.delete('/api/v1/service/' + id)
}

export function createServiceOrder(data: any) {
  return request.post('/api/v1/service/order', data)
}

export function getServiceOrderList(params: any) {
  return request.get<any, { records: any[]; total: number }>('/api/v1/service/order/list', { params })
}

export function acceptServiceOrder(id: number) {
  return request.put('/api/v1/service/order/' + id + '/accept')
}

export function completeServiceOrder(id: number) {
  return request.put('/api/v1/service/order/' + id + '/complete')
}

export function cancelServiceOrder(id: number) {
  return request.put('/api/v1/service/order/' + id + '/cancel')
}

// ============== 快递管理 ==============
export interface ExpressItem {
  id?: number
  expressNo: string
  receiverId: number
  expressCompany?: string
  trackingNo?: string
  packageInfo?: string
  pickupCode: string
  shelfNo?: string
  status?: number // 0=待取件 1=已取件 2=滞留 3=退回
  arriveTime?: string
  pickupTime?: string
  createTime?: string
}

export function getExpressList(params: any) {
  return request.get<any, { records: ExpressItem[]; total: number }>('/api/v1/express/list', { params })
}

export function receiveExpress(data: ExpressItem) {
  return request.post('/api/v1/express', data)
}

export function updateExpress(data: ExpressItem) {
  return request.put('/api/v1/express', data)
}

export function pickupExpress(id: number, pickupCode: string) {
  return request.put(`/api/v1/express/${id}/pickup`, { pickupCode })
}

export function deleteExpress(id: number) {
  return request.delete(`/api/v1/express/${id}`)
}

// ============== 活动管理 ==============
export interface ActivityItem {
  id?: number
  title: string
  content?: string
  images?: string
  activityType?: number
  location?: string
  startTime: string
  endTime: string
  signStartTime?: string
  signEndTime?: string
  maxParticipants?: number
  currentParticipants?: number
  contactName?: string
  contactPhone?: string
  isTop?: number
  viewCount?: number
  status?: number // 0=草稿 1=已发布 2=进行中 3=已结束 4=已取消
  createTime?: string
}

export function getActivityList(params: any) {
  return request.get<any, { records: ActivityItem[]; total: number }>('/api/v1/activity/list', { params })
}

export function getActivityById(id: number) {
  return request.get<any, ActivityItem>(`/api/v1/activity/${id}`)
}

export function createActivity(data: ActivityItem) {
  return request.post('/api/v1/activity', data)
}

export function updateActivity(data: ActivityItem) {
  return request.put('/api/v1/activity', data)
}

export function deleteActivity(id: number) {
  return request.delete('/api/v1/activity/' + id)
}

export function joinActivity(id: number, data: any = {}) {
  return request.post('/api/v1/activity/' + id + '/join', data)
}

export function getActivityJoinList(activityId: number, params: any = {}) {
  return request.get<any, any>('/api/v1/activity/' + activityId + '/joins', { params })
}

export function cancelActivityJoin(joinId: number) {
  return request.delete('/api/v1/activity/join/' + joinId)
}

// ============== 房屋租赁 ==============
export interface RentalItem {
  id?: number
  rentalNo: string
  ownerId: number
  houseId?: number
  title: string
  rentalType: number
  description?: string
  images?: string
  monthlyRent: number
  deposit?: number
  paymentMethod?: string
  contactName?: string
  contactPhone?: string
  status?: number // 0=待审核 1=已发布 2=已出租 3=已下架
  isRecommend?: number
  publishTime?: string
  createTime?: string
}

export function getRentalList(params: any) {
  return request.get<any, { records: RentalItem[]; total: number }>('/api/v1/rental/list', { params })
}

export function createRental(data: RentalItem) {
  return request.post('/api/v1/rental', data)
}

export function updateRental(data: RentalItem) {
  return request.put('/api/v1/rental', data)
}

export function deleteRental(id: number) {
  return request.delete(`/api/v1/rental/${id}`)
}
