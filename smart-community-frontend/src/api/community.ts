import request from '@/utils/request'
import exampleCredits from '@/assets/marketplace-image-credits.json'

export interface CommunityPost {
  id: number; title: string; content: string; category: string; images: string
  userId: number; userName: string; isTop: number; commentCount: number; status: number; createTime: string
}
export interface CommunityComment {
  id: number; parentId?: number; content: string; userId: number; userName: string; createTime: string
}
export interface CommunityItem {
  id: number; title: string; description: string; category: number; price: number; originPrice?: number
  conditionLevel: number; images: string; contactName: string; contactPhone: string; tradeMethod: number
  userId: number; userName: string; status: number; createTime: string
}
export interface QaEntry { id?: number; question: string; answer: string; createTime?: string }
export interface PageResult<T> { records: T[]; total: number }
export const socialBase = '/api/v1/resident/social'
export function postList(params: object) { return request.get<any, PageResult<CommunityPost>>(socialBase + '/posts', { params }) }
export function postDetail(id: number) { return request.get<any, CommunityPost>(`${socialBase}/posts/${id}`) }
export function commentList(id: number, page: number) { return request.get<any, PageResult<CommunityComment>>(`${socialBase}/posts/${id}/comments`, { params: { page, pageSize: 20 } }) }
export function itemList(params: object) { return request.get<any, PageResult<CommunityItem>>(socialBase + '/items', { params }) }
export function itemDetail(id: number) { return request.get<any, CommunityItem>(`${socialBase}/items/${id}`) }
export function qaHistory(page: number) { return request.get<any, PageResult<QaEntry>>('/api/v1/resident/qa/history', { params: { page } }) }
export function beginQuestion(question: string) { return request.post<any, { id: string }>('/api/v1/resident/qa/questions', { question }) }
export function questionResult(id: string) { return request.get<any, { state: 'pending' | 'completed' | 'failed'; response?: QaEntry; message?: string }>(`/api/v1/resident/qa/questions/${id}`) }
export function imageUrls(images?: string): string[] {
  try {
    const values: unknown = JSON.parse(images || '[]')
    return Array.isArray(values) ? values.filter((value): value is string => typeof value === 'string' && (/^https?:\/\//i.test(value) || value.startsWith('/uploads/'))).slice(0, 5) : []
  } catch { return [] }
}
export function displayDate(value?: string) { return value ? value.replace('T', ' ').slice(0, 16) : '—' }
export function exampleImageCredit(images?: string) { return exampleCredits.find(credit => imageUrls(images).includes(credit.image)) }
export const postCategories = ['求助', '分享', '交易', '活动', '邻里话题', '其他']
export const itemCategories = ['家具', '家电', '数码', '母婴', '图书', '其他']
export const conditions = ['全新', '九成新', '八成新', '五成新']
