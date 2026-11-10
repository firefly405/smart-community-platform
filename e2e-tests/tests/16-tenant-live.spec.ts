import { test, expect, type APIRequestContext } from '@playwright/test'

const password = process.env.TENANT_TEST_PASSWORD || 'admin123'

async function signIn(request: APIRequestContext, username: string) {
  const response = await request.post('/api/v1/auth/login', { data: { username, password } })
  const result = await response.json()
  expect(result.code).toBe(200)
  return result.data
}

test('live tenant APIs enforce lease and role boundaries without writing data', async ({ request }) => {
  const current = await signIn(request, 'tenant001')
  const expired = await signIn(request, 'tenant002')
  const owner = await signIn(request, 'owner001')
  const admin = await signIn(request, 'admin')
  const auth = (token: string) => ({ Authorization: `Bearer ${token}` })

  const currentSummary = await (await request.get('/api/v1/tenant-portal/summary', { headers: auth(current.token) })).json()
  expect(currentSummary.data).toMatchObject({ bound: true, houseId: 6, leaseEndDate: '2026-12-31' })
  const expiredSummary = await (await request.get('/api/v1/tenant-portal/summary', { headers: auth(expired.token) })).json()
  expect(expiredSummary.data.bound).toBe(false)

  for (const path of ['lease', 'bills', 'repairs', 'complaints', 'notices', 'activities', 'phones']) {
    expect((await request.get(`/api/v1/tenant-portal/${path}`, { headers: auth(current.token) })).status()).toBe(200)
  }
  expect((await request.get('/api/v1/user/list?page=1&pageSize=1', { headers: auth(current.token) })).status()).toBe(403)
  expect((await request.get('/api/v1/resident/summary', { headers: auth(current.token) })).status()).toBe(403)
  expect((await request.get('/api/v1/tenant-portal/summary', { headers: auth(owner.token) })).status()).toBe(403)
  expect((await request.get('/api/v1/tenant-portal/summary', { headers: auth(admin.token) })).status()).toBe(403)
})

test('live tenant login rejects an expired lease before success and accepts a current lease', async ({ page }) => {
  await page.goto('/tenant/login')
  await page.getByPlaceholder('请输入用户名/手机号').fill('tenant002')
  await page.getByPlaceholder('请输入密码', { exact: true }).fill(password)
  await page.locator('.login-button:visible').click()
  await expect(page.getByText('当前账号没有有效租约，请联系物业核实')).toBeVisible()
  await expect(page).toHaveURL(/\/tenant\/login$/)
  expect(await page.evaluate(() => localStorage.getItem('token'))).toBeNull()

  await page.getByPlaceholder('请输入用户名/手机号').fill('tenant001')
  await page.getByPlaceholder('请输入密码', { exact: true }).fill(password)
  await page.locator('.login-button:visible').click()
  await expect(page).toHaveURL(/\/tenant\/home$/)
  await expect(page.getByRole('heading', { name: /欢迎回来$/ })).toBeVisible()
})

test('live tenant pages load without server errors or management navigation', async ({ page, request }) => {
  const login = await signIn(request, 'tenant001')
  await page.addInitScript(token => localStorage.setItem('token', token), login.token)
  const pageErrors: string[] = []
  page.on('pageerror', error => pageErrors.push(error.message))

  for (const path of ['home', 'lease', 'bills', 'repairs', 'complaints', 'notices', 'activities', 'phones', 'profile']) {
    await page.goto('/tenant/' + path)
    await expect(page.locator('.tenant-shell')).toBeVisible()
    await expect(page.locator('.el-loading-mask')).toHaveCount(0)
    await expect(page.locator('.el-message--error')).toHaveCount(0)
    await expect(page.getByRole('navigation', { name: '租户服务导航' })).not.toContainText(/用户管理|房屋管理|租户管理|收费配置|系统设置/)
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  }
  expect(pageErrors).toEqual([])
})
