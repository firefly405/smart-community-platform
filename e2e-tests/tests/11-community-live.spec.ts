import { test, expect, type APIRequestContext } from '@playwright/test'

async function signIn(request: APIRequestContext, username: string) {
  const response = await request.post('/api/v1/auth/login', { data: { username, password: process.env.COMMUNITY_TEST_PASSWORD || 'admin123' } })
  const result = await response.json()
  expect(result.code).toBe(200)
  return { Authorization: `Bearer ${result.data.token}` }
}

test('live owner APIs exclude management access and separate households', async ({ request }) => {
  const first = await signIn(request, 'owner001'), second = await signIn(request, 'owner002')
  for (const path of ['/user/list', '/menu/tree', '/fee/bill/list', '/system/ai-config', '/repair/list']) {
    const response = await request.get('/api/v1' + path, { headers: first })
    expect(response.status(), path).toBe(403)
  }
  for (const section of ['houses', 'bills', 'vehicles', 'repairs', 'complaints']) {
    const a = await (await request.get(`/api/v1/resident/${section}?pageSize=50&ownerId=2`, { headers: first })).json()
    const b = await (await request.get(`/api/v1/resident/${section}?pageSize=50`, { headers: second })).json()
    expect(a.code, section).toBe(200); expect(b.code, section).toBe(200)
    const otherIds = new Set(b.data.records.map((row: any) => row.id))
    expect(a.data.records.some((row: any) => otherIds.has(row.id)), section).toBe(false)
    if (section !== 'houses') {
      expect(a.data.records.every((row: any) => row.ownerId === 1), section).toBe(true)
      expect(b.data.records.every((row: any) => row.ownerId === 2), section).toBe(true)
    }
  }
})

test('live owner pages load and refresh without server errors or management navigation', async ({ page }) => {
  const pageErrors: string[] = []
  page.on('pageerror', error => pageErrors.push(error.message))
  await page.goto('/community/login')
  await page.getByPlaceholder('请输入用户名/手机号').fill('owner001')
  await page.getByPlaceholder('请输入密码', { exact: true }).fill(process.env.COMMUNITY_TEST_PASSWORD || 'admin123')
  await page.locator('.login-button:visible').click()
  await expect(page).toHaveURL(/\/community\/home$/)
  for (const path of ['home', 'houses', 'bills', 'repairs', 'complaints', 'vehicles', 'notices', 'activities', 'phones', 'profile']) {
    await page.goto('/community/' + path)
    await expect(page.locator('.resident-shell')).toBeVisible()
    await expect(page.locator('.el-loading-mask')).toHaveCount(0)
    await expect(page.locator('.el-alert--error')).toHaveCount(0)
    await expect(page.locator('.el-message--error')).toHaveCount(0)
    await expect(page.getByRole('navigation')).not.toContainText(/系统管理|财务分析|角色管理/)
  }
  await page.goto('/system/ai-config')
  await expect(page).toHaveURL(/\/community\/home$/)
  await expect(page.locator('.el-loading-mask')).toHaveCount(0)
  await page.screenshot({ path: 'test-results/community-live-home.png', fullPage: true })
  expect(pageErrors).toEqual([])
})

test('management and resident endpoints read the same work order while admin remains separate', async ({ request }) => {
  const owner = await signIn(request, 'owner001'), admin = await signIn(request, 'admin')
  const result = await (await request.get('/api/v1/resident/repairs', { headers: owner })).json()
  expect(result.code).toBe(200)
  expect(result.data.records.length).toBeGreaterThan(0)
  const repair = result.data.records[0]
  const managed = await (await request.get(`/api/v1/repair/${repair.id}`, { headers: admin })).json()
  expect(managed.code).toBe(200)
  expect(managed.data.title).toBe(repair.title)
  expect(managed.data.status).toBe(repair.status)
  expect(managed.data.handleResult).toBe(repair.handleResult)
  expect((await request.get('/api/v1/resident/summary', { headers: admin })).status()).toBe(403)
  expect((await request.get('/api/v1/user/list', { headers: admin })).status()).toBe(200)
})
