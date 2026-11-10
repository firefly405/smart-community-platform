import { test, expect } from '@playwright/test'

const menus = [{
  id: 1, menuName: '系统管理', menuType: 1, children: [
    { id: 2, menuName: '用户管理', menuType: 2, path: 'user', children: [
      { id: 3, menuName: '用户新增', menuType: 3 }
    ] }
  ]
}]

test.beforeEach(async ({ context }) => {
  await context.addInitScript(() => localStorage.setItem('token', 'system-management-test'))
  await context.route('**/api/v1/**', async route => {
    const request = route.request()
    const path = new URL(request.url()).pathname
    let data: any = null
    if (path === '/api/v1/setup/check') data = { needSetup: false }
    if (path === '/api/v1/user/info') data = { id: 1, username: 'admin', realName: '测试管理员', userType: 5, status: 1 }
    if (path === '/api/v1/menu/tree') data = menus
    if (path === '/api/v1/user/list') data = { records: [{ id: 10, username: 'zhangsan', realName: '张三', deptId: 100, userType: 2, gender: 1, status: 1 }], total: 1 }
    if (path === '/api/v1/dept/tree') data = [{ id: 100, deptName: '物业服务部', children: [] }]
    if (path === '/api/v1/role/all') data = [{ id: 20, roleName: '系统管理员', roleCode: 'admin', status: 1 }, { id: 21, roleName: '物业主管', roleCode: 'manager', status: 1 }]
    if (path === '/api/v1/user/10/roles' && request.method() === 'GET') data = [20]
    if (path === '/api/v1/role/list') data = { records: [{ id: 20, roleName: '系统管理员', roleCode: 'admin', roleSort: 1, dataScope: 1, status: 1 }], total: 1 }
    if (path === '/api/v1/role/20/menus' && request.method() === 'GET') data = [1, 2]
    if (path === '/api/v1/log/list') data = { records: [{ id: 1, username: 'admin', operation: '更新用户', method: 'PUT', url: '/api/v1/user', ip: '127.0.0.1', executeTime: 15, status: 1, createTime: '2026-10-14 10:00:00' }], total: 1 }
    await route.fulfill({ json: { code: 200, data } })
  })
})

test('user roles can be loaded and assigned', async ({ page }) => {
  await page.goto('/system/user')
  await expect(page.getByRole('heading', { name: '用户管理' })).toBeVisible()
  await expect(page.getByText('物业服务部')).toBeVisible()
  await page.getByRole('button', { name: '分配角色' }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog).toContainText('张三')
  await dialog.getByText('物业主管', { exact: true }).click()
  const submitted = page.waitForRequest(request => request.method() === 'PUT' && new URL(request.url()).pathname === '/api/v1/user/10/roles')
  await dialog.getByRole('button', { name: '保存角色' }).click()
  expect((await submitted).postDataJSON()).toEqual({ roleIds: [20, 21] })
})

test('role menu and button permissions can be assigned', async ({ page }) => {
  await page.goto('/system/role')
  await expect(page.getByText('全部数据')).toBeVisible()
  await page.getByRole('button', { name: '配置权限' }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog).toContainText('用户新增')
  await dialog.locator('.el-tree-node__content').filter({ hasText: '用户新增' }).locator('.el-checkbox').click()
  const submitted = page.waitForRequest(request => request.method() === 'PUT' && new URL(request.url()).pathname === '/api/v1/role/20/menus')
  await dialog.getByRole('button', { name: '保存权限' }).click()
  expect((await submitted).postDataJSON().menuIds).toEqual(expect.arrayContaining([1, 2, 3]))
})

test('operation logs can be exported as csv', async ({ page }) => {
  await page.goto('/system/log')
  await expect(page.getByText('更新用户')).toBeVisible()
  const download = page.waitForEvent('download')
  await page.getByRole('button', { name: '导出' }).click()
  expect((await download).suggestedFilename()).toMatch(/^操作日志-\d{4}-\d{2}-\d{2}\.csv$/)
})
