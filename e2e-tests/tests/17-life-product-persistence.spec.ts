import { test, expect, type APIRequestContext, type Page } from '@playwright/test'

const PASSWORD = process.env.E2E_PASSWORD || 'admin123'
const categories = new Map([
  [1, '生鲜蔬果'],
  [2, '粮油调味'],
  [3, '日用品'],
  [4, '零食饮料'],
  [5, '家居厨具'],
  [6, '儿童用品'],
])

async function getAdminHeaders(request: APIRequestContext) {
  const response = await request.post('/api/v1/auth/login', {
    data: { username: 'admin', password: PASSWORD },
  })
  const result = await response.json()
  expect(result.code).toBe(200)
  return { Authorization: `Bearer ${result.data.token}` }
}

async function login(page: Page) {
  await page.goto('/login')
  await page.getByPlaceholder(/用户名/).fill('admin')
  await page.getByPlaceholder(/密码/).fill(PASSWORD)
  await page.getByRole('button', { name: /登/ }).first().click()
  await expect(page).toHaveURL(/\/dashboard$/)
}

test('社区商品名称和分类可显示、编辑并持久化', async ({ page, request }) => {
  test.setTimeout(60_000)
  page.setDefaultTimeout(10_000)
  const headers = await getAdminHeaders(request)
  let original: Record<string, any> | undefined

  try {
    await login(page)

    const initialList = page.waitForResponse(response =>
      response.url().includes('/api/v1/product/list') && response.request().method() === 'GET',
    )
    await page.goto('/life/product')
    const initialResult = await (await initialList).json()
    original = initialResult.data.records[0]

    expect(original?.productName).toBeTruthy()
    expect(categories.has(Number(original?.categoryId))).toBe(true)

    let firstRow = page.locator('.el-table__body-wrapper tbody tr').first()
    await expect(firstRow.locator('td').nth(1)).toContainText(original!.productName)
    await expect(firstRow.locator('td').nth(2)).toContainText(categories.get(Number(original!.categoryId))!)

    const updatedName = `${original!.productName}-E2E${Date.now().toString().slice(-6)}`
    const updatedCategoryId = Number(original!.categoryId) === 6 ? 1 : Number(original!.categoryId) + 1
    const updatedCategoryName = categories.get(updatedCategoryId)!

    await firstRow.getByRole('button', { name: '编辑', exact: true }).click()
    const dialog = page.locator('.el-dialog:visible')
    await dialog.getByPlaceholder('请输入商品名称').fill(updatedName)
    await dialog.locator('.el-form-item').filter({ hasText: '分类' }).locator('.el-select__wrapper').click()
    await page.getByRole('option', { name: updatedCategoryName, exact: true }).click()

    const updateResponse = page.waitForResponse(response =>
      response.url().endsWith('/api/v1/product') && response.request().method() === 'PUT',
    )
    const refreshedList = page.waitForResponse(response =>
      response.url().includes('/api/v1/product/list') && response.request().method() === 'GET',
    )
    await dialog.getByRole('button', { name: '确定', exact: true }).click()
    expect((await updateResponse).ok()).toBe(true)
    await refreshedList

    firstRow = page.locator('.el-table__body-wrapper tbody tr').first()
    await expect(firstRow.locator('td').nth(1)).toContainText(updatedName)
    await expect(firstRow.locator('td').nth(2)).toContainText(updatedCategoryName)

    const reloadedList = page.waitForResponse(response =>
      response.url().includes('/api/v1/product/list') && response.request().method() === 'GET',
    )
    await page.reload()
    await reloadedList

    firstRow = page.locator('.el-table__body-wrapper tbody tr').first()
    await expect(firstRow.locator('td').nth(1)).toContainText(updatedName)
    await expect(firstRow.locator('td').nth(2)).toContainText(updatedCategoryName)
  } finally {
    if (original) {
      const restore = await request.put('/api/v1/product', { headers, data: original })
      expect(restore.ok()).toBe(true)

      const restored = await request.get(`/api/v1/product/${original.id}`, { headers })
      const restoredResult = await restored.json()
      expect(restoredResult.data.productName).toBe(original.productName)
      expect(restoredResult.data.categoryId).toBe(original.categoryId)
    }
  }
})
