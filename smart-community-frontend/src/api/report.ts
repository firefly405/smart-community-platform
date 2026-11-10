import request from '@/utils/request'

// ============== 业主统计 ==============
export function getOwnerStats(params?: any) {
  return request.get('/api/v1/report/owner', { params })
}

export function getOwnerAgeDistribution() {
  return request.get('/api/v1/report/owner/age-distribution')
}

export function getOwnerActiveRanking(params?: any) {
  return request.get('/api/v1/report/owner/active-ranking', { params })
}

// ============== 服务统计 ==============
export function getServiceStats(params?: any) {
  return request.get('/api/v1/report/service', { params })
}

export function getRepairTrend() {
  return request.get<any, { day: string; date: string; count: number }[]>('/api/v1/report/service/repair-trend')
}

export function getRepairResponseTime(params?: any) {
  return request.get('/api/v1/report/service/response-time', { params })
}

export function getRepairCompletionRate(params?: any) {
  return request.get('/api/v1/report/service/completion-rate', { params })
}

export function getComplaintStats(params?: any) {
  return request.get('/api/v1/report/service/complaint', { params })
}

// ============== 财务统计 ==============
export function getFinanceStats(params?: any) {
  return request.get('/api/v1/report/finance', { params })
}

export function getFeeRateTrend(params?: any) {
  return request.get('/api/v1/report/finance/rate-trend', { params })
}

export function getArrearsTrend(params?: any) {
  return request.get('/api/v1/report/finance/arrears-trend', { params })
}

export function getIncomeStructure(params?: any) {
  return request.get('/api/v1/report/finance/income-structure', { params })
}

// ============== 数据大屏 ==============
export function getDashboardData() {
  return request.get('/api/v1/report/dashboard')
}

export function getLatestRepairs(limit?: number) {
  return request.get<any, { records: any[]; total: number }>('/api/v1/repair/list', { params: { page: 1, pageSize: limit || 5 } })
}

export function getDashboardRealtime() {
  return request.get('/api/v1/report/dashboard/realtime')
}

// ============== 报表导出 ==============
export function exportReport(type: string, params?: any) {
  return request.get(`/api/v1/report/${type}/export`, { params, responseType: 'blob' })
}

export function saveReportTemplate(data: { name: string; query: any }) {
  return request.post('/api/v1/report/template', data)
}

export function getReportTemplates() {
  return request.get('/api/v1/report/template/list')
}