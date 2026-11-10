import request from '@/utils/request'

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export interface TenantSummary {
  bound: boolean
  houseId?: number
  leaseEndDate?: string
  daysRemaining?: number
  unpaid: number
  repairs: number
  complaints: number
}

export interface TenantInfo {
  id: number
  ownerId: number
  houseId: number
  realName: string
  phone: string
  idCard?: string
  gender?: number
  leaseStartDate: string
  leaseEndDate: string
  monthlyRent?: number
  deposit?: number
  status: number
  ownerName?: string
  houseNo?: string
}

export interface TenantHouse {
  id: number
  buildingName?: string
  unitName?: string
  houseNo: string
  floor?: number
  houseType?: string
  area?: number
  innerArea?: number
  orientation?: string
  ownerName?: string
}

export interface TenantLeaseDetail {
  tenant: TenantInfo
  house: TenantHouse
}

export interface TenantRecord {
  id: number
  title?: string
  content?: string
  status?: number
  houseNo?: string
  createTime?: string
  [key: string]: unknown
}

export interface TenantServicePayload {
  houseId: number
  title: string
  content: string
  type: number
  urgency?: number
  contactName: string
  contactPhone: string
}

export type TenantListSection = 'bills' | 'repairs' | 'complaints' | 'notices' | 'activities' | 'phones'

const base = '/api/v1/tenant-portal'

export function getTenantSummary() {
  return request.get<any, TenantSummary>(`${base}/summary`)
}

export function getTenantLease() {
  return request.get<any, TenantLeaseDetail>(`${base}/lease`)
}

export function getTenantList<T extends TenantRecord = TenantRecord>(section: TenantListSection, page = 1, pageSize = 10) {
  return request.get<any, PageResult<T>>(`${base}/${section}`, { params: { page, pageSize } })
}

export function createTenantService(section: 'repairs' | 'complaints', data: TenantServicePayload) {
  return request.post<any, number>(`${base}/${section}`, data)
}

export function evaluateTenantRepair(id: number, data: { score: number; content?: string }) {
  return request.post(`${base}/repairs/${id}/evaluate`, data)
}

