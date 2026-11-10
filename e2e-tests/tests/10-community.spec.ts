import { test, expect } from '@playwright/test'

const owner = { id: 13, username: 'owner001', realName: '业主小明', phone: '13800138000', userType: 1, status: 1, roles: [] }
test.beforeEach(async ({ context }) => {
  await context.route('**/api/v1/**', async route => {
    const path = new URL(route.request().url()).pathname
    let data: any = { records: [], total: 0 }
    if (path === '/api/v1/setup/check') data = { needSetup: false }
    if (path === '/api/v1/auth/login') data = { token: 'resident-test', userInfo: owner }
    if (path === '/api/v1/user/info') data = owner
    if (path === '/api/v1/resident/summary') data = { bound: true, houses: 1, unpaid: 2, repairs: 1 }
    if (path === '/api/v1/resident/houses') data = { records: [{ id: 10, buildingName: '一号楼', unitName: '一单元', houseNo: '101', area: 98 }], total: 1 }
    if (path === '/api/v1/resident/repairs' && route.request().method() === 'GET') data = { records: [{ id: 8, title: '厨房漏水', houseNo: '101', status: 3, handleResult: '已更换水管', createTime: '2026-10-14T12:00:00' }], total: 1 }
    if (path === '/api/v1/resident/notices') data = { records: [{ id: 1, title: '供水检修通知', content: '<p>明日上午检修</p>', publishTime: '2026-10-14T09:00:00' }], total: 1 }
    await route.fulfill({ json: { code: 200, data } })
  })
})

test('owner login ignores stale admin redirect and refresh keeps resident identity', async ({ page }) => {
  await page.goto('/community/login?redirect=/system/ai-config')
  await expect(page.getByRole('heading', { name: '智慧社区', exact: true })).toBeVisible()
  await expect(page.getByPlaceholder('请输入用户名/手机号')).toHaveValue('')
  await page.getByPlaceholder('请输入用户名/手机号').fill('owner001')
  await page.getByPlaceholder('请输入密码', { exact: true }).fill('admin123')
  await page.locator('.login-button:visible').click()
  await expect(page).toHaveURL(/\/community\/home$/)
  await expect(page.getByRole('navigation')).not.toContainText(/用户管理|角色管理|AI配置|财务分析/)
  await expect(page.getByRole('heading', { name: '业主小明，欢迎回家' })).toBeVisible()
  await page.reload()
  await expect(page.getByRole('heading', { name: '社区首页' })).toBeVisible()
  await page.goto('/system/user')
  await expect(page).toHaveURL(/\/community\/home$/)
})

test('ordinary owner can submit repair using own house and view a result', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('token', 'resident-test'))
  await page.goto('/community/repairs')
  await page.getByRole('button', { name: '提交报修', exact: true }).click()
  const dialog = page.getByRole('dialog')
  await dialog.locator('.el-form-item').filter({ hasText: /^标题/ }).locator('input').fill('门窗损坏')
  await dialog.locator('textarea').fill('窗户无法关闭，请安排维修')
  const submission = page.waitForRequest(request => request.method() === 'POST' && new URL(request.url()).pathname === '/api/v1/resident/repairs')
  await dialog.getByRole('button', { name: '提交', exact: true }).click()
  expect((await submission).postDataJSON()).toMatchObject({ houseId: 10, title: '门窗损坏', contactName: '业主小明' })
  expect((await submission).postDataJSON()).not.toHaveProperty('ownerId')
  await expect(dialog).not.toBeVisible()
  await page.getByRole('button', { name: '详情', exact: true }).click()
  await expect(page.getByRole('dialog')).toContainText('已更换水管')
  const evaluation = page.waitForRequest(request => request.method() === 'POST' && new URL(request.url()).pathname === '/api/v1/resident/repairs/8/evaluate')
  await page.getByRole('button', { name: '提交评价' }).click()
  expect((await evaluation).postDataJSON()).toMatchObject({ score: 5 })
})

test('unbound owner sees binding warning instead of other households', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('token', 'resident-test'))
  await page.route('**/api/v1/resident/summary', route => route.fulfill({ json: { code: 200, data: { bound: false } } }))
  let privateCalls = 0
  page.on('request', request => { if (new URL(request.url()).pathname === '/api/v1/resident/repairs') privateCalls++ })
  await page.goto('/community/repairs')
  await expect(page.getByRole('alert')).toContainText('账号尚未关联业主档案')
  await expect(page.getByRole('button', { name: '提交报修', exact: true })).toBeDisabled()
  expect(privateCalls).toBe(0)
})

test('admin identity is rejected by owner login', async ({ page }) => {
  await page.route('**/api/v1/auth/login', route => route.fulfill({ json: { code: 200, data: { token: 'admin-test', userInfo: { ...owner, id: 1, userType: 5 } } } }))
  await page.goto('/community/login')
  await page.getByPlaceholder('请输入用户名/手机号').fill('admin')
  await page.getByPlaceholder('请输入密码', { exact: true }).fill('admin123')
  await page.locator('.login-button:visible').click()
  await expect(page.getByText('此入口仅供普通业主使用，请使用物业管理登录')).toBeVisible()
  expect(await page.evaluate(() => localStorage.getItem('token'))).toBeNull()
})

test('mobile resident pages fit viewport and contain no management links', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.addInitScript(() => localStorage.setItem('token', 'resident-test'))
  for (const path of ['home', 'bills', 'repairs', 'profile']) {
    await page.goto('/community/' + path)
    await expect(page.locator('.resident-shell')).toBeVisible()
    await expect(page.locator('.resident-page')).not.toHaveClass(/is-loading/)
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
    await expect(page.getByRole('navigation')).not.toContainText('系统管理')
    await page.screenshot({ path: `test-results/community-mobile-${path}.png`, fullPage: true, animations: 'disabled' })
  }
})

test('desktop overview renders community image and readable navigation', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.addInitScript(() => localStorage.setItem('token', 'resident-test'))
  await page.goto('/community/home')
  await expect(page.getByText('供水检修通知', { exact: true })).toBeVisible()
  await expect(page.getByRole('img', { name: '社区住宅与绿化' })).toBeVisible()
  expect(await page.locator('.welcome img').evaluate((img: HTMLImageElement) => img.complete && img.naturalWidth > 0)).toBe(true)
  await page.screenshot({ path: 'test-results/community-desktop.png', fullPage: true, animations: 'disabled' })
})
