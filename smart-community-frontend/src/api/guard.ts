import request from '@/utils/request'

// ============== 门禁设备 ==============
export interface AccessDeviceItem {
  id?: number
  name?: string
  accessName?: string
  accessNo?: string
  code?: string
  type?: string
  accessType?: number
  location: string
  ip?: string
  ipAddress?: string
  status?: number
  createTime?: string
}

export function getAccessDeviceList(params: any) {
  return request.get<any, { records: AccessDeviceItem[]; total: number }>('/api/v1/access/device/list', { params })
}

export function createAccessDevice(data: AccessDeviceItem) {
  return request.post('/api/v1/access/device', data)
}

export function updateAccessDevice(data: AccessDeviceItem) {
  return request.put('/api/v1/access/device', data)
}

export function deleteAccessDevice(id: number) {
  return request.delete(`/api/v1/access/device/${id}`)
}

export function setAccessPermissions(deviceId: number, data: { userIds: number[]; validFrom?: string; validTo?: string }) {
  return request.put(`/api/v1/access/${deviceId}/permissions`, data)
}

export function getAccessPermissions(deviceId: number) {
  return request.get<any, { deviceId: number; userIds: number[]; permissions: any[] }>('/api/v1/access/' + deviceId + '/permissions')
}

export function createAccessLog(data: any) {
  return request.post('/api/v1/access/log', data)
}

// ============== 通行记录 ==============
export interface AccessLogItem {
  id?: number
  deviceId: number
  deviceName?: string
  userId?: number
  userName?: string
  houseNo?: string
  type?: string // 人脸识别/刷卡/二维码
  status?: number // 0=失败 1=成功
  passTime?: string
}

export function getAccessLogList(params: any) {
  return request.get<any, { records: AccessLogItem[]; total: number }>('/api/v1/access/log/list', { params })
}

// ============== 访客管理 ==============
export interface VisitorItem {
  id?: number
  name: string
  phone: string
  idCard?: string
  plateNumber?: string
  reason: string
  hostId: number
  hostName?: string
  houseNo?: string
  visitStart: string
  visitEnd: string
  status?: number // 0=待审核 1=已通过 2=已拒绝 3=已签到 4=已签退
  qrcode?: string
  createTime?: string
}

export function getVisitorById(id: number) {
  return request.get<any, any>('/api/v1/visitor/' + id)
}

export function getVisitorList(params: any) {
  return request.get<any, { records: VisitorItem[]; total: number }>('/api/v1/visitor/list', { params })
}

export function createVisitor(data: VisitorItem) {
  return request.post('/api/v1/visitor', data)
}

export function approveVisitor(id: number, approved: boolean, remark?: string) {
  return request.put(`/api/v1/visitor/${id}/approve`, { approved, remark })
}

export function getVisitorQrcode(id: number) {
  return request.get<any, string>(`/api/v1/visitor/${id}/qrcode`)
}

export function verifyVisitor(qrcode: string, accessId: number) {
  return request.post('/api/v1/visitor/verify', { qrcode, accessId })
}

// ============== 黑名单 ==============
export interface BlacklistItem {
  id?: number
  name: string
  phone?: string
  idCard?: string
  reason: string
  createTime?: string
}

export function getBlacklist(params: any) {
  return request.get<any, { records: BlacklistItem[]; total: number }>('/api/v1/visitor/blacklist/list', { params })
}

export function addBlacklist(data: BlacklistItem) {
  return request.post('/api/v1/visitor/blacklist', data)
}

export function removeBlacklist(id: number) {
  return request.delete(`/api/v1/visitor/blacklist/${id}`)
}

// ============== 监控设备 ==============
export interface CameraItem {
  cameraName?: string
  cameraNo?: string
  id?: number
  name: string
  code: string
  location: string
  ip?: string
  port?: number
  username?: string
  password?: string
  streamUrl?: string
  type?: string
  status?: number // 0=离线 1=在线
  createTime?: string
}

export function getCameraList(params: any) {
  return request.get<any, { records: CameraItem[]; total: number }>('/api/v1/camera/list', { params })
}

export function createCamera(data: CameraItem) {
  return request.post('/api/v1/camera', data)
}

export function updateCamera(data: CameraItem) {
  return request.put('/api/v1/camera', data)
}

export function deleteCamera(id: number) {
  return request.delete(`/api/v1/camera/${id}`)
}

export function getCameraStreamUrl(id: number) {
  return request.get<any, string>(`/api/v1/camera/${id}/stream`)
}

// ============== 报警管理 ==============
export interface AlarmItem {
  id?: number
  type: string // 消防/陌生人/烟雾/燃气
  level?: string // 高/中/低
  location: string
  description?: string
  status?: number // 0=待处理 1=处理中 2=已处理
  handlerId?: number
  handlerName?: string
  handleRemark?: string
  handleTime?: string
  createTime?: string
}

export function getAlarmList(params: any) {
  return request.get<any, { records: AlarmItem[]; total: number }>('/api/v1/alarm/list', { params })
}

export function handleAlarm(id: number, data: { remark: string }) {
  return request.put(`/api/v1/alarm/${id}/handle`, data)
}
