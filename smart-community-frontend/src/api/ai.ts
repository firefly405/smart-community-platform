import request from '@/utils/request'

export interface QaLogItem {
  id: number
  question: string
  answer: string
  sources?: string
  userId?: number
  userName?: string
  createTime?: string
}

export interface KnowledgeDocItem {
  id?: number
  title: string
  content: string
  category: string
  status?: number
  embeddingId?: string
  createTime?: string
}

export interface AiConfig {
  provider: string
  enabled: boolean
  runtimeEnabled: boolean
  apiKeyConfigured: boolean
  maskedApiKey: string
  apiUrl: string
  chatModel: string
  embeddingModel: string
  restartRequired: boolean
  updatedAt?: string
}

export interface AiConfigInput {
  enabled: boolean
  apiUrl: string
  apiKey?: string
  chatModel?: string
  embeddingModel?: string
}

export interface AiConnectionResult {
  success: boolean
  message: string
  latencyMs: number
  models: string[]
  chatModel: string
  embeddingModel: string
}

export function askQuestionSync(question: string): Promise<QaLogItem> {
  return request.post<any, QaLogItem>('/api/v1/qa/ask-sync', { question }, {
    timeout: 120000,
    timeoutErrorMessage: 'AI回答耗时较长，本次等待已超时，请稍后重试。'
  })
}

export function getQaLogList(params: any) {
  return request.get<any, { records: QaLogItem[]; total: number }>('/api/v1/qa/log/list', { params })
}

export function deleteQaLog(id: number) {
  return request.delete(`/api/v1/qa/log/${id}`)
}

export function getKnowledgeList(params: any) {
  return request.get<any, { records: KnowledgeDocItem[]; total: number }>('/api/v1/qa/knowledge/list', { params })
}

export function getKnowledgeById(id: number) {
  return request.get<any, KnowledgeDocItem>(`/api/v1/qa/knowledge/${id}`)
}

export function createKnowledge(data: KnowledgeDocItem) {
  return request.post('/api/v1/qa/knowledge', data)
}

export function updateKnowledge(data: KnowledgeDocItem) {
  return request.put('/api/v1/qa/knowledge', data)
}

export function deleteKnowledge(id: number) {
  return request.delete(`/api/v1/qa/knowledge/${id}`)
}

export function reloadVectorIndex() {
  return request.post('/api/v1/qa/knowledge/reload')
}

export function getAiConfig() {
  return request.get<any, AiConfig>('/api/v1/system/ai-config')
}

export function saveAiConfig(data: AiConfigInput) {
  return request.put<any, AiConfig>('/api/v1/system/ai-config', data)
}

export function testAiConnection(data: Partial<AiConfigInput>) {
  return request.post<any, AiConnectionResult>('/api/v1/system/ai-config/test', data)
}

export function restartBackend() {
  return request.post('/api/v1/system/ai-config/restart')
}
