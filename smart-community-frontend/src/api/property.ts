import request from '@/utils/request'

// ============== 报修管理 ==============
export interface RepairItem {
  id?: number
  repairNo?: string
  title: string
  content: string
  repairType?: number
  category?: string
  urgency?: number // 0=普通 1=紧急
  images?: string[]
  houseId?: number
  houseNo?: string
  ownerId?: number
  contactName?: string
  contactPhone?: string
  applicantName?: string
  applicantPhone?: string
  appointmentDate?: string
  appointmentTime?: string
  status?: number // 0=待派单 1=已派单 2=处理中 3=已完成 4=已取消
  handlerId?: number
  handlerName?: string
  handleResult?: string
  result?: string
  handleRemark?: string
  handleImages?: string
  handleTime?: string
  finishTime?: string
  completeTime?: string
  evaluateScore?: number
  score?: number
  evaluateContent?: string
  evaluation?: string
  evaluateTime?: string
  createTime?: string
}

export function getRepairList(params: any) {
  return request.get<any, { records: RepairItem[]; total: number }>('/api/v1/repair/list', { params })
}

export function getRepairById(id: number) {
  return request.get<any, RepairItem>(`/api/v1/repair/${id}`)
}

export function createRepair(data: RepairItem) {
  return request.post('/api/v1/repair', data)
}

export function dispatchRepair(id: number, handlerId: number) {
  return request.put(`/api/v1/repair/${id}/dispatch`, { handlerId })
}

export function processRepair(id: number, data: { result: string; images?: string }) {
  return request.put(`/api/v1/repair/${id}/process`, data)
}

export function completeRepair(id: number) {
  return request.put(`/api/v1/repair/${id}/complete`)
}

export function evaluateRepair(id: number, data: { score: number; evaluation?: string }) {
  return request.put(`/api/v1/repair/${id}/evaluate`, data)
}

export function cancelRepair(id: number) {
  return request.put(`/api/v1/repair/${id}/cancel`)
}

// ============== 投诉管理 ==============
export interface ComplaintItem {
  id?: number
  title: string
  content: string
  category?: string
  complaintType?: number
  images?: string[]
  houseId?: number
  houseNo?: string
  contactName?: string
  contactPhone?: string
  complainantName?: string
  complainantPhone?: string
  status?: number // 0=待处理 1=处理中 2=已完成 3=已关闭
  handlerId?: number
  handlerName?: string
  handleRemark?: string
  handleTime?: string
  score?: number
  createTime?: string
}

export function getComplaintList(params: any) {
  return request.get<any, { records: ComplaintItem[]; total: number }>('/api/v1/complaint/list', { params })
}

export function getComplaintById(id: number) {
  return request.get<any, ComplaintItem>(`/api/v1/complaint/${id}`)
}

export function createComplaint(data: ComplaintItem) {
  return request.post('/api/v1/complaint', data)
}

export function acceptComplaint(id: number, data: any = {}) {
  return request.put(`/api/v1/complaint/${id}/accept`, data)
}

export function handleComplaint(id: number, data: any) {
  return request.put(`/api/v1/complaint/${id}/handle`, data)
}

export function closeComplaint(id: number) {
  return request.put(`/api/v1/complaint/${id}/close`)
}
// ============== 公告管理 ==============
export interface NoticeItem {
  id?: number
  title: string
  content: string
  type?: string
  noticeType?: number
  isTop?: number
  isEmergency?: number
  isPublish?: number
  viewCount?: number
  status?: number // 0=草稿 1=已发布 2=已撤回
  publishTime?: string
  createBy?: string
  createTime?: string
  readCount?: number
}

export function getNoticeList(params: any) {
  return request.get<any, { records: NoticeItem[]; total: number }>('/api/v1/notice/list', { params })
}

export function getNoticeById(id: number) {
  return request.get<any, NoticeItem>(`/api/v1/notice/${id}`)
}

export function createNotice(data: NoticeItem) {
  return request.post('/api/v1/notice', data)
}

export function updateNotice(data: NoticeItem) {
  return request.put('/api/v1/notice', data)
}

export function publishNotice(id: number) {
  return request.put(`/api/v1/notice/${id}/publish`)
}

export function revokeNotice(id: number) {
  return request.put(`/api/v1/notice/${id}/revoke`)
}

export function deleteNotice(id: number) {
  return request.delete(`/api/v1/notice/${id}`)
}

// ============== 设备管理 ==============
export interface EquipmentItem {
  id?: number
  name: string
  equipmentName?: string
  code: string
  equipmentCode?: string
  type?: string
  equipmentType?: string
  location?: string
  brand?: string
  model?: string
  installDate?: string
  warrantyDate?: string
  lifeYears?: number
  status?: number
  maintainCycle?: number // 天
  lastMaintainDate?: string
  nextMaintainDate?: string
  remark?: string
  createTime?: string
}

export function getEquipmentList(params: any) {
  return request.get<any, { records: EquipmentItem[]; total: number }>('/api/v1/equipment/list', { params })
}

export function getEquipmentById(id: number) {
  return request.get<any, EquipmentItem>(`/api/v1/equipment/${id}`)
}

export function createEquipment(data: EquipmentItem) {
  return request.post('/api/v1/equipment', data)
}

export function updateEquipment(data: EquipmentItem) {
  return request.put('/api/v1/equipment', data)
}

export function deleteEquipment(id: number) {
  return request.delete(`/api/v1/equipment/${id}`)
}

export function getEquipmentMaintainList(equipmentId: number) {
  return request.get<any, any[]>(`/api/v1/equipment/${equipmentId}/maintain`)
}

export interface InspectionItem {
  id?: number
  name?: string
  inspectorId?: number
  inspectorName: string
  area?: string
  inspectionDate?: string
  status?: number
}

export function getInspections(params: any) {
  return request.get<any, { records: InspectionItem[]; total: number }>('/api/v1/inspection/record/list', { params })
}

export function createInspection(data: InspectionItem) {
  return request.post('/api/v1/inspection/record', data)
}

export function updateInspection(data: InspectionItem) {
  return request.put('/api/v1/inspection/record', data)
}

export function deleteInspection(id: number) {
  return request.delete(`/api/v1/inspection/record/${id}`)
}

// ============== 巡检计划管理 ==============
export interface InspectionPlanItem {
  id?: number
  name: string
  area?: string
  frequency?: string
  items?: string[]
  assigneeId?: number
  assigneeName?: string
  startDate?: string
  status?: number
}

export function getInspectionPlanList(params: any) {
  return request.get<any, { records: InspectionPlanItem[]; total: number }>('/api/v1/inspection/plan/list', { params })
}

export function createInspectionPlan(data: InspectionPlanItem) {
  return request.post('/api/v1/inspection/plan', data)
}

export function updateInspectionPlan(data: InspectionPlanItem) {
  return request.put('/api/v1/inspection/plan', data)
}

export function submitInspectionRecord(data: any) {
  return request.post('/api/v1/inspection/record', data)
}

export function generateTodayInspections() {
  return request.post<any, number>('/api/v1/inspection/plan/generate-today')
}

export function completeInspectionRecord(id: number, data: any = {}) {
  return request.put(`/api/v1/inspection/record/${id}/complete`, data)
}

export function deleteInspectionPlan(id: number) {
  return request.delete(`/api/v1/inspection/plan/${id}`)
}

// ============== 保洁管理 ==============
export interface CleaningItem {
  id?: number
  area: string
  task?: string
  cleanerId?: number
  cleanerName?: string
  taskDate?: string
  taskType?: number
  taskContent?: string
  assigneeId?: number
  assigneeName?: string
  scheduleDate?: string
  status?: number
  checkScore?: number
}

export function getCleaningList(params: any) {
  return request.get<any, { records: CleaningItem[]; total: number }>('/api/v1/cleaning/list', { params })
}

export function createCleaning(data: CleaningItem) {
  return request.post('/api/v1/cleaning', data)
}

export function updateCleaning(data: CleaningItem) {
  return request.put('/api/v1/cleaning', data)
}

export function checkCleaning(id: number, score: number, remark?: string) {
  return request.put(`/api/v1/cleaning/${id}/check`, { score, remark })
}

export function deleteCleaning(id: number) {
  return request.delete(`/api/v1/cleaning/${id}`)
}


export interface MaintainItem {
  id?: number
  equipmentId: number
  maintainType?: number
  maintainDate?: string
  maintainerName?: string
  maintainContent: string
  maintainResult?: string
  cost?: number
  nextMaintainDate?: string
  status?: number
}
export function getMaintainList(params: any) {
  return request.get<any, { records: MaintainItem[]; total: number }>('/api/v1/equipment/maintain/list', { params })
}
export function createMaintain(data: MaintainItem) { return request.post('/api/v1/equipment/maintain', data) }
export function updateMaintain(data: MaintainItem) { return request.put('/api/v1/equipment/maintain', data) }
export function completeMaintain(id: number) { return request.put('/api/v1/equipment/maintain/' + id + '/complete') }

