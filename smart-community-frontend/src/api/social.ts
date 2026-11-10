import request from '@/utils/request'

// ============== 帖子管理 ==============
export interface PostItem {
  id?: number
  title: string
  content: string
  category?: string // 求助/分享/交易/其他
  images?: string[]
  userId?: number
  userName?: string
  tags?: string[]
  viewCount?: number
  likeCount?: number
  commentCount?: number
  isTop?: number
  status?: number // 0=待审核 1=已发布 2=已下架
  createTime?: string
}

export function getPostList(params: any) {
  return request.get<any, { records: PostItem[]; total: number }>('/api/v1/post/list', { params })
}

export function getPostById(id: number) {
  return request.get<any, PostItem>(`/api/v1/post/${id}`)
}

export function createPost(data: PostItem) {
  return request.post('/api/v1/post', data)
}

export function updatePost(data: PostItem) {
  return request.put('/api/v1/post', data)
}

export function deletePost(id: number) {
  return request.delete(`/api/v1/post/${id}`)
}

export function likePost(id: number) {
  return request.put(`/api/v1/post/${id}/like`)
}

export function topPost(id: number, isTop: number) {
  return request.put(`/api/v1/post/${id}/top`, { isTop })
}

// ============== 评论管理 ==============
export interface CommentItem {
  id?: number
  postId: number
  parentId?: number
  content: string
  userId?: number
  userName?: string
  createTime?: string
  replies?: CommentItem[]
}

export function getCommentList(postId: number) {
  return request.get<any, CommentItem[]>(`/api/v1/comment/post/${postId}`)
}

export function createComment(data: CommentItem) {
  return request.post('/api/v1/comment', data)
}

export function deleteComment(id: number) {
  return request.delete(`/api/v1/comment/${id}`)
}

// ============== 二手交易 ==============
export interface SecondhandItem {
  id?: number
  title: string
  description: string
  category?: string
  price: number
  originalPrice?: number
  images?: string[]
  userId?: number
  userName?: string
  contactPhone?: string
  condition?: string // 全新/几乎全新/轻微使用/正常使用
  status?: number // 0=在售 1=已售 2=已下架
  viewCount?: number
  createTime?: string
}

export function getSecondhandList(params: any) {
  return request.get<any, { records: SecondhandItem[]; total: number }>('/api/v1/secondhand/list', { params })
}

export function getSecondhandById(id: number) {
  return request.get<any, SecondhandItem>(`/api/v1/secondhand/${id}`)
}

export function createSecondhand(data: SecondhandItem) {
  return request.post('/api/v1/secondhand', data)
}

export function updateSecondhand(data: SecondhandItem) {
  return request.put('/api/v1/secondhand', data)
}

export function deleteSecondhand(id: number) {
  return request.delete(`/api/v1/secondhand/${id}`)
}

// ============== 投票管理 ==============
export interface VoteItem {
  id?: number
  title: string
  description?: string
  isMulti?: number
  isAnonymous?: number
  startTime?: string
  endTime?: string
  status?: number
  options?: VoteOption[]
  totalVotes?: number
  createTime?: string
}

export interface VoteOption {
  id?: number
  voteId: number
  text: string
  count?: number
}

export function getVoteList(params: any) {
  return request.get<any, { records: VoteItem[]; total: number }>('/api/v1/vote/list', { params })
}

export function getVoteById(id: number) {
  return request.get<any, VoteItem>(`/api/v1/vote/${id}`)
}

export function createVote(data: VoteItem) {
  return request.post('/api/v1/vote', data)
}

export function updateVote(data: VoteItem) {
  return request.put('/api/v1/vote', data)
}

export function deleteVote(id: number) {
  return request.delete('/api/v1/vote/' + id)
}

export function submitVote(id: number, data: { optionIds: number[]; userId?: number; userName?: string } | number[]) {
  const body = Array.isArray(data) ? { optionIds: data } : data
  return request.post('/api/v1/vote/' + id + '/submit', body)
}

export function getVoteOptions(id: number) {
  return request.get<any, any[]>('/api/v1/vote/' + id + '/options')
}
export function getVoteResult(id: number) {
  return request.get<any, VoteItem>(`/api/v1/vote/${id}/result`)
}

// ============== 便民电话 ==============
export interface PhoneItem {
  id?: number
  name: string
  phone: string
  category?: string // 物业/维修/医疗/交通/餐饮/其他
  address?: string
  sort?: number
  status?: number
  createTime?: string
}

export function getPhoneList(params: any) {
  return request.get<any, { records: PhoneItem[]; total: number }>('/api/v1/phone/list', { params })
}

export function createPhone(data: PhoneItem) {
  return request.post('/api/v1/phone', data)
}

export function updatePhone(data: PhoneItem) {
  return request.put('/api/v1/phone', data)
}

export function deletePhone(id: number) {
  return request.delete(`/api/v1/phone/${id}`)
}
