import request from '@/utils/request'

export interface LoginData {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  userInfo: {
    id: number
    username: string
    realName: string
    phone: string
    avatar: string
    roles: string[]
    userType: number
    status: number
  }
}

export interface UserProfile {
  id: number
  username: string
  realName: string
  phone: string
  email: string
  avatar: string
  gender: number
  birthday: string
  status: number
  userType: number
  loginTime: string
  createTime: string
}

export const login = (data: LoginData) => {
  return request.post('/api/v1/auth/login', data) as Promise<LoginResponse>
}

export const loginBySms = (data: { phone: string; code: string }) => {
  return request.post('/api/v1/auth/login-by-sms', data) as Promise<LoginResponse>
}

export const sendCode = (phone: string) => {
  return request.post('/api/v1/auth/send-code', { phone })
}

export const registerBySms = (data: { phone: string; code: string; username?: string; password?: string }) => {
  return request.post('/api/v1/auth/register-by-sms', data)
}

export const getUserInfo = () => {
  return request.get<any, UserProfile>('/api/v1/user/info')
}

export const updateProfile = (data: {
  realName?: string
  phone?: string
  email?: string
  gender?: number
  birthday?: string
}) => {
  return request.put<any, UserProfile>('/api/v1/user/profile', data)
}

export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, { url: string }>('/api/v1/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const logout = () => {
  return request.post('/api/v1/auth/logout')
}

export const changePassword = (data: { oldPassword: string; newPassword: string }) => {
  return request.put('/api/v1/user/password', data)
}

export default {
  login,
  loginBySms,
  sendCode,
  registerBySms,
  getUserInfo,
  updateProfile,
  uploadAvatar,
  logout,
  changePassword
}
