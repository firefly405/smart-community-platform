import request from '@/utils/request'

// ============== 用户管理 ==============
export interface UserItem {
  id?: number
  username: string
  password?: string
  realName?: string
  phone?: string
  email?: string
  avatar?: string
  gender?: number
  status?: number
  userType?: number
  deptId?: number
  deptName?: string
  roleIds?: number[]
  createTime?: string
}

export function getUserList(params: any) {
  return request.get<any, { records: UserItem[]; total: number }>('/api/v1/user/list', { params })
}

export function getUserById(id: number) {
  return request.get<any, UserItem>(`/api/v1/user/${id}`)
}

export function createUser(data: UserItem) {
  return request.post('/api/v1/user', data)
}

export function updateUser(data: UserItem) {
  return request.put('/api/v1/user', data)
}

export function deleteUser(id: number) {
  return request.delete(`/api/v1/user/${id}`)
}

export function resetPassword(id: number, password: string) {
  return request.put(`/api/v1/user/${id}/password`, { password })
}

export function getUserRoles(userId: number) {
  return request.get<any, number[]>(`/api/v1/user/${userId}/roles`)
}

export function assignUserRoles(userId: number, roleIds: number[]) {
  return request.put(`/api/v1/user/${userId}/roles`, { roleIds })
}

// ============== 角色管理 ==============
export interface RoleItem {
  id?: number
  roleName: string
  roleCode: string
  roleSort?: number
  dataScope?: number
  remark?: string
  status?: number
  menuIds?: number[]
  createTime?: string
}

export function getRoleList(params: any) {
  return request.get<any, { records: RoleItem[]; total: number }>('/api/v1/role/list', { params })
}

export function getAllRoles() {
  return request.get<any, RoleItem[]>('/api/v1/role/all')
}

export function createRole(data: RoleItem) {
  return request.post('/api/v1/role', data)
}

export function updateRole(data: RoleItem) {
  return request.put('/api/v1/role', data)
}

export function deleteRole(id: number) {
  return request.delete(`/api/v1/role/${id}`)
}

export function assignRoleMenus(roleId: number, menuIds: number[]) {
  return request.put(`/api/v1/role/${roleId}/menus`, { menuIds })
}

export function getRoleMenuIds(roleId: number) {
  return request.get<any, number[]>(`/api/v1/role/${roleId}/menus`)
}

// ============== 菜单管理 ==============
export interface MenuItem {
  id?: number
  parentId?: number
  /** 后端字段 menuName；兼容旧 name */
  menuName?: string
  name?: string
  /** 1=目录 2=菜单 3=按钮；兼容旧 type */
  menuType?: number
  type?: number
  path?: string
  component?: string
  permission?: string
  perms?: string
  icon?: string
  sort?: number
  visible?: number
  status?: number
  children?: MenuItem[]
}

export function getMenuTree() {
  return request.get<any, MenuItem[]>('/api/v1/menu/tree')
}

export function createMenu(data: MenuItem) {
  return request.post('/api/v1/menu', data)
}

export function updateMenu(data: MenuItem) {
  return request.put('/api/v1/menu', data)
}

export function deleteMenu(id: number) {
  return request.delete(`/api/v1/menu/${id}`)
}

// ============== 部门管理 ==============
export interface DeptItem {
  id?: number
  parentId?: number
  deptName: string
  deptCode?: string
  leader?: string
  phone?: string
  email?: string
  sort?: number
  status?: number
  children?: DeptItem[]
}

export function getDeptTree() {
  return request.get<any, DeptItem[]>('/api/v1/dept/tree')
}

export function getAllDepts() {
  return request.get<any, DeptItem[]>('/api/v1/dept/all')
}

export function createDept(data: DeptItem) {
  return request.post('/api/v1/dept', data)
}

export function updateDept(data: DeptItem) {
  return request.put('/api/v1/dept', data)
}

export function deleteDept(id: number) {
  return request.delete(`/api/v1/dept/${id}`)
}

// ============== 字典管理 ==============
export interface DictItem {
  id?: number
  type: string
  label: string
  value: string
  sort?: number
  status?: number
}

export function getDictList(params: any) {
  return request.get<any, { records: DictItem[]; total: number }>('/api/v1/dict/list', { params })
}

export function getDictByType(type: string) {
  return request.get<any, DictItem[]>(`/api/v1/dict/type/${type}`)
}

// ============== 操作日志 ==============
export function getLogList(params: any) {
  return request.get<any, { records: any[]; total: number }>('/api/v1/log/list', { params })
}

export function getLoginLogList(params: any) {
  return request.get<any, { records: any[]; total: number }>('/api/v1/login-log/list', { params })
}
