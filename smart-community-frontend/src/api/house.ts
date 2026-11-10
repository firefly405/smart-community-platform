import request from '@/utils/request'

// ============== 楼栋管理 ==============
export interface BuildingItem {
  id?: number
  name?: string
  code?: string
  buildingNo?: string
  buildingName?: string
  unitCount?: number
  floorCount?: number
  floors?: number
  units?: number
  area?: number
  buildYear?: string
  address?: string
  description?: string
  createTime?: string
}

export function getBuildingList(params: any) {
  return request.get<any, { records: BuildingItem[]; total: number }>('/api/v1/building/list', { params })
}

export function getAllBuildings() {
  return request.get<any, BuildingItem[]>('/api/v1/building/all')
}

export function createBuilding(data: BuildingItem) {
  return request.post('/api/v1/building', data)
}

export function updateBuilding(data: BuildingItem) {
  return request.put('/api/v1/building', data)
}

export function deleteBuilding(id: number) {
  return request.delete(`/api/v1/building/${id}`)
}

// ============== 单元管理 ==============
export interface UnitItem {
  id?: number
  buildingId?: number
  buildingName?: string
  name?: string
  code?: string
  unitNo?: string
  floorCount?: number
  floors?: number
  housesPerFloor?: number
  description?: string
}

export function getUnitList(params: any) {
  return request.get<any, { records: UnitItem[]; total: number }>('/api/v1/unit/list', { params })
}

export function createUnit(data: UnitItem) {
  return request.post('/api/v1/unit', data)
}

export function updateUnit(data: UnitItem) {
  return request.put('/api/v1/unit', data)
}

export function deleteUnit(id: number) {
  return request.delete(`/api/v1/unit/${id}`)
}

// ============== 房屋管理 ==============
export interface HouseItem {
  id?: number
  buildingId: number
  buildingName?: string
  unitId: number
  unitName?: string
  houseNo: string
  area?: number
  innerArea?: number
  floor?: number
  roomCount?: number
  hallCount?: number
  houseType?: string
  propertyType?: number
  deliveryDate?: string
  orientation?: string
  decoration?: string
  status?: number // 0=空置 1=已售 2=出租
  ownerId?: number
  ownerName?: string
  remark?: string
  createTime?: string
}

export function getHouseList(params: any) {
  return request.get<any, { records: HouseItem[]; total: number }>('/api/v1/house/list', { params })
}

export function getHouseById(id: number) {
  return request.get<any, HouseItem>(`/api/v1/house/${id}`)
}

export function createHouse(data: HouseItem) {
  return request.post('/api/v1/house', data)
}

export function updateHouse(data: HouseItem) {
  return request.put('/api/v1/house', data)
}

export function deleteHouse(id: number) {
  return request.delete(`/api/v1/house/${id}`)
}

export function importHouse(houses: HouseItem[]) {
  return request.post('/api/v1/house/import', houses)
}

export function exportHouse(params: any) {
  return request.get<any, HouseItem[]>('/api/v1/house/export', { params })
}

// ============== 业主管理 ==============
export interface OwnerItem {
  id?: number
  name?: string
  realName?: string
  phone?: string
  idCard?: string
  gender?: number
  email?: string
  birthday?: string
  nation?: string
  nativePlace?: string
  occupation?: string
  workUnit?: string
  checkInDate?: string
  checkOutDate?: string
  remark?: string
  emergencyContact?: string
  emergencyPhone?: string
  tags?: string[]
  houseIds?: number[]
  houses?: { id: number; houseNo: string }[]
  status?: number
  createTime?: string
}

export function getOwnerList(params: any) {
  return request.get<any, { records: OwnerItem[]; total: number }>('/api/v1/owner/list', { params })
}

export function getOwnerById(id: number) {
  return request.get<any, OwnerItem>(`/api/v1/owner/${id}`)
}

export function createOwner(data: OwnerItem) {
  return request.post('/api/v1/owner', data)
}

export function updateOwner(data: OwnerItem) {
  return request.put('/api/v1/owner', data)
}

export function deleteOwner(id: number) {
  return request.delete(`/api/v1/owner/${id}`)
}

export function bindOwnerHouses(ownerId: number, houseIds: number[]) {
  return request.put(`/api/v1/owner/${ownerId}/houses`, { houseIds })
}

export function getOwnerHouseIds(ownerId: number) {
  return request.get<any, number[]>(`/api/v1/owner/${ownerId}/houses`)
}

// ============== 家属/租户管理 ==============
export interface FamilyItem {
  id?: number
  ownerId?: number
  ownerName?: string
  name?: string
  realName?: string
  phone?: string
  relation?: string
  idCard?: string
  gender?: number
  status?: number
  createTime?: string
}

export function getFamilyList(ownerId: number) {
  return request.get<any, FamilyItem[]>(`/api/v1/family/owner/${ownerId}`)
}

export function createFamily(data: FamilyItem) {
  return request.post('/api/v1/family', data)
}

export function updateFamily(data: FamilyItem) {
  return request.put('/api/v1/family', data)
}

export function deleteFamily(id: number) {
  return request.delete(`/api/v1/family/${id}`)
}

// ============== 车位/车辆管理 ==============
export interface ParkingItem {
  id?: number
  code?: string
  type?: string // 地上/地下
  parkingNo?: string
  parkingType?: number
  area?: number | string
  status?: number // 0=空闲 1=已售 2=已租
  ownerId?: number
  ownerName?: string
  fee?: number
  price?: number
  monthlyRent?: number
}

export function getParkingList(params: any) {
  return request.get<any, { records: ParkingItem[]; total: number }>('/api/v1/parking/list', { params })
}

export function createParking(data: ParkingItem) {
  return request.post('/api/v1/parking', data)
}

export function updateParking(data: ParkingItem) {
  return request.put('/api/v1/parking', data)
}

export function deleteParking(id: number) {
  return request.delete(`/api/v1/parking/${id}`)
}

export interface VehicleItem {
  id?: number
  plateNumber?: string
  plateNo?: string
  brand?: string
  color?: string
  type?: string
  vehicleType?: string
  ownerId?: number
  ownerName?: string
  parkingId?: number
  parkingCode?: string
  status?: number
}

export function getVehicleList(params: any) {
  return request.get<any, { records: VehicleItem[]; total: number }>('/api/v1/vehicle/list', { params })
}

export function createVehicle(data: VehicleItem) {
  return request.post('/api/v1/vehicle', data)
}

export function updateVehicle(data: VehicleItem) {
  return request.put('/api/v1/vehicle', data)
}

export function deleteVehicle(id: number) {
  return request.delete(`/api/v1/vehicle/${id}`)
}

// ============== 租户管理 ==============
export interface TenantItem {
  id?: number
  ownerId?: number
  houseId: number
  realName: string
  phone: string
  idCard?: string
  gender?: number
  leaseStartDate?: string
  leaseEndDate?: string
  monthlyRent?: number
  deposit?: number
  status?: number
  createTime?: string
}

export function getTenantList(params: any) {
  return request.get<any, { records: TenantItem[]; total: number }>('/api/v1/tenant/list', { params })
}

export function createTenant(data: TenantItem) {
  return request.post('/api/v1/tenant', data)
}

export function updateTenant(data: TenantItem) {
  return request.put('/api/v1/tenant', data)
}

export function deleteTenant(id: number) {
  return request.delete(`/api/v1/tenant/${id}`)
}

// ============== 装修管理 ==============
export interface DecorationItem {
  id?: number
  ownerId: number
  houseId: number
  ownerName?: string
  houseNo?: string
  decorationNo?: string
  companyName?: string
  companyPhone?: string
  startDate?: string
  expectEndDate?: string
  actualEndDate?: string
  depositAmount?: number
  status?: number
  violationRecord?: string
  checkResult?: string
  createTime?: string
}

export function getDecorationList(params: any) {
  return request.get<any, { records: DecorationItem[]; total: number }>('/api/v1/decoration/list', { params })
}

export function createDecoration(data: DecorationItem) {
  return request.post('/api/v1/decoration', data)
}

export function updateDecoration(data: DecorationItem) {
  return request.put('/api/v1/decoration', data)
}

export function deleteDecoration(id: number) {
  return request.delete(`/api/v1/decoration/${id}`)
}

export function approveDecoration(id: number, data: { status: number; checkResult: string }) {
  return request.put(`/api/v1/decoration/${id}/approve`, data)
}
