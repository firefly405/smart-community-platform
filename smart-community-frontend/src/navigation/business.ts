import type { LocationQuery, RouteLocationRaw, RouteLocationNormalizedLoaded, Router } from 'vue-router'

interface Relation { label: string; path: string; field: string; query?: string }
export const relations: Record<string, Relation[]> = {
  '/property/building': [{ label: '查看单元', path: '/property/unit', field: 'id', query: 'buildingId' }, { label: '查看房屋', path: '/property/house', field: 'id', query: 'buildingId' }],
  '/property/unit': [{ label: '查看房屋', path: '/property/house', field: 'id', query: 'unitId' }],
  '/property/owner': [{ label: '关联房屋', path: '/property/house', field: 'id', query: 'ownerId' }, { label: '关联家属', path: '/property/family', field: 'id', query: 'ownerId' }, { label: '业主账单', path: '/charge/bill', field: 'id', query: 'ownerId' }, { label: '报修记录', path: '/service/repair', field: 'id', query: 'ownerId' }, { label: '访客记录', path: '/guard/visitor', field: 'id', query: 'ownerId' }],
  '/property/house': [{ label: '关联业主', path: '/property/owner', field: 'ownerId', query: 'id' }, { label: '房屋账单', path: '/charge/bill', field: 'id', query: 'houseId' }, { label: '报修记录', path: '/service/repair', field: 'id', query: 'houseId' }],
  '/charge/bill': [{ label: '账单详情', path: '/charge/bill/detail/', field: 'id' }, { label: '缴费记录', path: '/charge/payment', field: 'id', query: 'billId' }],
  '/charge/payment': [{ label: '关联账单', path: '/charge/bill/detail/', field: 'billId' }],
  '/service/repair': [{ label: '工单详情', path: '/service/repair/detail/', field: 'id' }, { label: '房屋账单', path: '/charge/bill', field: 'houseId', query: 'houseId' }],
  '/service/equipment': [{ label: '设备详情', path: '/service/equipment/detail/', field: 'id' }, { label: '维保记录', path: '/service/maintain', field: 'id', query: 'equipmentId' }],
  '/service/maintain': [{ label: '关联设备', path: '/service/equipment/detail/', field: 'equipmentId' }],
  '/life/service': [{ label: '服务预约', path: '/life/service-order', field: 'id', query: 'serviceId' }],
  '/guard/visitor': [{ label: '访客详情', path: '/guard/visitor/detail/', field: 'id' }, { label: '关联业主', path: '/property/owner', field: 'ownerId', query: 'id' }]
}

export const listFilters: Record<string, string[]> = {
  '/property/unit': ['buildingId'],
  '/property/house': ['buildingId', 'unitId', 'ownerId'],
  '/property/owner': ['id'],
  '/property/family': ['ownerId'],
  '/charge/bill': ['ownerId', 'houseId'],
  '/charge/payment': ['ownerId', 'houseId', 'billId'],
  '/service/repair': ['ownerId', 'houseId'],
  '/service/maintain': ['equipmentId'],
  '/life/service-order': ['serviceId'],
  '/guard/visitor': ['ownerId']
}

export const filterLabels: Record<string, string> = {
  id: '业主 ID', ownerId: '业主 ID', houseId: '房屋 ID', buildingId: '楼栋 ID',
  unitId: '单元 ID', billId: '账单 ID', equipmentId: '设备 ID', serviceId: '服务 ID'
}

export function relatedLinks(path: string, row: Record<string, unknown>, from: string) {
  return (relations[path] || []).filter(link => /^[1-9]\d*$/.test(String(row[link.field] ?? '')))
    .map(link => ({
      label: link.label,
      to: {
        path: link.query ? link.path : link.path + row[link.field],
        query: { ...(link.query ? { [link.query]: String(row[link.field]) } : {}), from }
      } as RouteLocationRaw
    }))
}

export function associationQuery(path: string, query: LocationQuery) {
  const result: Record<string, string> = {}
  for (const key of listFilters[path] || []) {
    const value = query[key]
    if (typeof value === 'string' && value) result[key] = value
  }
  return result
}

export function isInternalPath(value: unknown): value is string {
  return typeof value === 'string' && value.startsWith('/') &&
    !value.startsWith('//') && !/[\\\r\n]/.test(value)
}

export function returnTarget(router: Router, route: RouteLocationNormalizedLoaded): string {
  const from = route.query.from
  if (isInternalPath(from) && from !== route.fullPath) {
    const target = router.resolve(from)
    if (target.name && !['NotFound', 'Login', 'Setup'].includes(String(target.name))) return from
  }
  return (route.meta.activeMenu as string) || ''
}
