import { test, expect } from '@playwright/test'

const owner = { id: 13, username: 'owner001', realName: '业主小明', phone: '13800138000', userType: 1, status: 1 }
const post = { id: 71, userId: 13, userName: '业主小明', title: '周末交换图书', content: '欢迎邻居一起交换图书', category: '2', status: 1, commentCount: 1, createTime: '2026-10-14T12:00:00' }
const item = { id: 81, userId: 14, userName: '邻居', title: '闲置书桌', description: '九成新，自提', category: 1, price: 88, conditionLevel: 2, status: 0, tradeMethod: 1, contactName: '张先生', contactPhone: '13800138001' }
test.beforeEach(async ({ context }) => {
  await context.addInitScript(() => localStorage.setItem('token', 'resident-test'))
  await context.route('**/api/v1/**', async route => {
    const path = new URL(route.request().url()).pathname
    let data: any = { records: [], total: 0 }
    if (path === '/api/v1/setup/check') data = { needSetup: false }
    if (path === '/api/v1/user/info') data = owner
    if (path.endsWith('/social/posts')) data = route.request().method() === 'POST' ? 71 : { records: [post], total: 1 }
    if (path.endsWith('/social/posts/71')) data = post
    if (path.endsWith('/posts/71/comments')) data = { records: [{ id: 91, userId: 14, userName: '邻居', content: '我有几本小说' }], total: 1 }
    if (path.endsWith('/social/items')) data = { records: [item], total: 1 }
    if (path.endsWith('/social/items/81')) data = item
    if (path.endsWith('/qa/questions')) data = { id: 'test-job' }
    if (path.endsWith('/qa/questions/test-job')) data = { state: 'completed', response: { answer: '<img src=x onerror=alert(1)>物业电话请查看便民电话' } }
    if (path.endsWith('/qa/history')) data = { records: [{ id: 1, question: '物业电话？', answer: '请查看便民电话' }], total: 1 }
    await route.fulfill({ json: { code: 200, data } })
  })
})

test('resident navigation exposes three modules but no management links', async ({ page }) => {
  await page.goto('/community/posts')
  const nav = page.getByRole('navigation')
  for (const name of ['智能问答', '邻里二手', '社区广场']) await expect(nav.getByText(name, { exact: true })).toBeVisible()
  await expect(nav).not.toContainText(/系统管理|AI配置|角色管理/)
  await page.getByRole('link', { name: '周末交换图书' }).click()
  await expect(page).toHaveURL(/\/community\/posts\/71$/)
  await expect(page.getByText('我有几本小说', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '回复', exact: true }).click()
  await page.getByRole('textbox', { name: '评论内容' }).fill('可以交换')
  const sent = page.waitForRequest(r => r.method() === 'POST' && r.url().endsWith('/posts/71/comments'))
  await page.getByRole('button', { name: '提交回复' }).click()
  expect((await sent).postDataJSON()).toEqual({ content: '可以交换', parentId: 91 })
  await expect(page.getByText('评论已发布', { exact: true })).toBeVisible()
})

test('publish a post without supplying author or moderation flags', async ({ page }) => {
  await page.goto('/community/posts')
  await page.getByRole('button', { name: '发布帖子', exact: true }).click()
  const dialog = page.getByRole('dialog')
  await dialog.locator('.el-form-item').filter({ hasText: /^标题/ }).locator('input').fill('周末交换图书')
  await dialog.locator('textarea').fill('欢迎邻居一起交换图书')
  const sent = page.waitForRequest(r => r.method() === 'POST' && r.url().endsWith('/social/posts'))
  await dialog.getByRole('button', { name: '发布', exact: true }).click()
  const payload = (await sent).postDataJSON()
  expect(payload).toMatchObject({ title: post.title, content: post.content, images: [] })
  expect(payload).not.toHaveProperty('userId'); expect(payload).not.toHaveProperty('status')
  await expect(page.getByRole('heading', { name: post.title })).toBeVisible()
})

test('buyer can contact seller and cannot see seller management controls', async ({ page }) => {
  await page.goto('/community/marketplace')
  await page.getByRole('button', { name: '查看物品：闲置书桌' }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('link', { name: '联系卖家' })).toHaveAttribute('href', 'tel:13800138001')
  await expect(dialog.getByRole('button', { name: '标记已售' })).toHaveCount(0)
  await expect(dialog.getByRole('button', { name: '删除', exact: true })).toHaveCount(0)
})

test('publish item validates form and sends only owner-safe fields', async ({ page }) => {
  await page.goto('/community/marketplace')
  await page.getByRole('button', { name: '发布闲置', exact: true }).click()
  const dialog = page.getByRole('dialog')
  await dialog.locator('.el-form-item').filter({ hasText: /^物品名称/ }).locator('input').fill('闲置书桌')
  await dialog.locator('textarea').fill('九成新，自提')
  const sent = page.waitForRequest(r => r.method() === 'POST' && r.url().endsWith('/social/items'))
  await dialog.getByRole('button', { name: '发布', exact: true }).click()
  const payload = (await sent).postDataJSON()
  expect(payload).toMatchObject({ title: item.title, category: 1, contactName: owner.realName, contactPhone: owner.phone })
  expect(payload).not.toHaveProperty('userId'); expect(payload).not.toHaveProperty('status')
  await expect(dialog).not.toBeVisible()
})

test('seller can mark own item sold after confirmation', async ({ page }) => {
  await page.route('**/social/items/81', route => route.fulfill({ json: { code: 200, data: { ...item, userId: 13 } } }))
  await page.goto('/community/marketplace')
  await page.getByRole('button', { name: '查看物品：闲置书桌' }).click()
  await page.getByRole('button', { name: '标记已售' }).click()
  const sent = page.waitForRequest(r => r.method() === 'PUT' && r.url().endsWith('/items/81/status'))
  await page.getByRole('button', { name: '确认', exact: true }).click()
  expect((await sent).postDataJSON()).toEqual({ status: 1 })
  await expect(page.getByRole('dialog', { name: '物品详情', exact: true })).not.toBeVisible()
  await expect(page.getByRole('dialog', { name: '更新物品状态', exact: true })).not.toBeVisible()
})

test('AI answer renders as text and own history opens', async ({ page }) => {
  await page.goto('/community/qa')
  await page.getByRole('textbox', { name: '咨询问题' }).fill('物业电话？')
  await page.getByRole('button', { name: '发送', exact: true }).click()
  await expect(page.locator('.assistant .message-text')).toContainText('<img src=x onerror=alert(1)>')
  await expect(page.locator('.assistant .message-text img')).toHaveCount(0)
  await page.getByRole('button', { name: '最近问答' }).click()
  await expect(page.getByRole('heading', { name: '物业电话？' })).toBeVisible()
})

test('AI polling failure releases submit control', async ({ page }) => {
  await page.route('**/qa/questions/test-job', route => route.fulfill({ json: { code: 200, data: { state: 'failed', message: '智能问答暂时不可用' } } }))
  await page.goto('/community/qa')
  await page.getByRole('textbox', { name: '咨询问题' }).fill('物业电话？')
  await page.getByRole('button', { name: '发送', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('智能问答暂时不可用')
  await expect(page.getByRole('textbox', { name: '咨询问题' })).toBeEnabled()
})

for (const width of [1440, 390]) test(`new pages fit ${width}px viewport`, async ({ page }) => {
  await page.setViewportSize({ width, height: 900 })
  for (const path of ['posts', 'posts/71', 'marketplace', 'qa']) {
    await page.goto('/community/' + path)
    await expect(page.locator('.resident-shell')).toBeVisible()
    await expect(page.locator('.el-loading-mask')).toHaveCount(0)
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await page.screenshot({ path: `test-results/community-${width}-${path.replace('/', '-')}.png`, fullPage: true })
  }
})
