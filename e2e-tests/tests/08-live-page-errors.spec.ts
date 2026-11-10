import { test, expect, type Page } from '@playwright/test'

async function signIn(page: Page) {
  await page.goto('/login', { waitUntil: 'networkidle' })
  await page.getByPlaceholder('请输入用户名/手机号').fill(process.env.E2E_USERNAME || 'admin')
  await page.getByPlaceholder('请输入密码', { exact: true }).fill(process.env.E2E_PASSWORD || 'admin123')
  await page.locator('.login-button').first().click()
  await page.waitForURL(url => !url.pathname.includes('/login'))
}

test('community posts opens without a server error', async ({ page }) => {
  const failedApi: Array<{ url: string; status: number; body: string }> = []
  const pageErrors: string[] = []

  page.on('pageerror', error => pageErrors.push(error.message))
  page.on('response', async response => {
    if (!response.url().includes('/api/') || response.status() < 400) return
    failedApi.push({
      url: new URL(response.url()).pathname,
      status: response.status(),
      body: (await response.text().catch(() => '')).slice(0, 500)
    })
  })

  await signIn(page)
  await page.evaluate(() => {
    ;(window as any).__serverErrorMessages = []
    new MutationObserver(records => {
      for (const record of records) {
        for (const node of Array.from(record.addedNodes)) {
          const text = node.textContent || ''
          if (text.includes('服务器错误')) (window as any).__serverErrorMessages.push(text.trim())
        }
      }
    }).observe(document.body, { childList: true, subtree: true })
  })

  await page.goto('/social/post', { waitUntil: 'networkidle' })
  const serverMessages = await page.evaluate(() => (window as any).__serverErrorMessages || [])

  expect({ failedApi, pageErrors, serverMessages }, JSON.stringify({ failedApi, pageErrors, serverMessages }, null, 2))
    .toEqual({ failedApi: [], pageErrors: [], serverMessages: [] })
})

test('all configured menu pages open without a server error', async ({ page }) => {
  test.setTimeout(120_000)
  const failures: Array<{ page: string; api?: string; status?: number; error?: string }> = []
  let currentPage = '/login'

  page.on('pageerror', error => failures.push({ page: currentPage, error: error.message }))
  page.on('response', response => {
    if (response.url().includes('/api/') && response.status() >= 500) {
      failures.push({ page: currentPage, api: new URL(response.url()).pathname, status: response.status() })
    }
  })

  await signIn(page)
  const paths = await page.evaluate(async () => {
    const token = localStorage.getItem('token')
    const response = await fetch('/api/v1/menu/tree', { headers: { Authorization: `Bearer ${token}` } })
    const payload = await response.json()
    const result: string[] = []
    const visit = (nodes: any[], parentPath = '') => {
      for (const node of nodes || []) {
        const raw = String(node.path || '')
        const path = raw.startsWith('/') ? raw : `${parentPath.replace(/\/$/, '')}/${raw}`
        if (node.menuType === 2 && path !== '/') result.push(path)
        visit(node.children || [], path)
      }
    }
    visit(payload.data || [])
    return [...new Set(result)]
  })

  for (const path of paths) {
    currentPage = path
    try {
      await page.goto(path, { waitUntil: 'networkidle', timeout: 15_000 })
    } catch (error) {
      failures.push({ page: path, error: error instanceof Error ? error.message : String(error) })
    }
  }

  expect(failures, JSON.stringify(failures, null, 2)).toEqual([])
})

test('user center presents complete profile and security sections responsively', async ({ page }) => {
  const failedApi: string[] = []
  page.on('response', response => {
    if (response.url().includes('/api/') && response.status() >= 500) {
      failedApi.push(`${response.status()} ${new URL(response.url()).pathname}`)
    }
  })

  await signIn(page)
  await page.goto('/system/user-center', { waitUntil: 'networkidle' })

  await expect(page.getByRole('heading', { name: '基本资料' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '账号安全' })).toBeVisible()
  await expect(page.getByRole('button', { name: '更换头像', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '保存资料' })).toBeDisabled()
  await expect(page.getByText('账号正常')).toBeVisible()

  await page.setViewportSize({ width: 390, height: 844 })
  await page.reload({ waitUntil: 'networkidle' })
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)
  expect(overflow).toBe(false)
  expect(failedApi).toEqual([])
})

test('duplicate vehicle plate returns a useful validation message instead of server error', async ({ page }) => {
  await signIn(page)

  const result = await page.evaluate(async () => {
    const token = localStorage.getItem('token')
    const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
    const listResponse = await fetch('/api/v1/vehicle/list?page=1&pageSize=1', { headers })
    const list = await listResponse.json()
    const existing = list.data?.records?.[0]
    if (!existing) throw new Error('No existing vehicle is available for duplicate validation')
    const response = await fetch('/api/v1/vehicle', {
      method: 'POST',
      headers,
      body: JSON.stringify({ ownerId: existing.ownerId, plateNo: existing.plateNo })
    })
    return { status: response.status, body: await response.json() }
  })

  expect(result.status).not.toBeGreaterThanOrEqual(500)
  expect(result.body.code).toBe(400)
  expect(result.body.message).toContain('车牌号已存在')
})

test('all property pages keep their navigation and remain responsive', async ({ page }) => {
  test.setTimeout(120_000)
  const pages = [
    ['/property/building', '楼栋管理'],
    ['/property/unit', '单元管理'],
    ['/property/house', '房屋管理'],
    ['/property/owner', '业主管理'],
    ['/property/family', '家属管理'],
    ['/property/tenant', '租户管理'],
    ['/property/parking', '车位管理'],
    ['/property/vehicle', '车辆管理'],
    ['/property/decoration', '装修管理']
  ] as const
  const failures: string[] = []

  page.on('pageerror', error => failures.push(error.message))
  page.on('response', response => {
    if (response.url().includes('/api/') && response.status() >= 500) {
      failures.push(`${response.status()} ${new URL(response.url()).pathname}`)
    }
  })

  await signIn(page)

  for (const [path, title] of pages) {
    await page.goto(path, { waitUntil: 'networkidle' })
    await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible()
    await expect(page.locator('.property-heading .section-nav a')).toHaveCount(9)
    await expect(page.locator('.pro-table')).toBeVisible()
  }

  await page.setViewportSize({ width: 390, height: 844 })
  for (const [path] of pages) {
    await page.goto(path, { waitUntil: 'networkidle' })
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)
    expect(overflow, `${path} has page-level horizontal overflow`).toBe(false)
    const fixedPositions = await page.locator('.el-table-fixed-column--right').evaluateAll(elements =>
      [...new Set(elements.map(element => getComputedStyle(element).position))]
    )
    expect(fixedPositions, `${path} keeps an overlaying action column on mobile`).not.toContain('sticky')
  }

  expect(failures).toEqual([])
})

test('module navigation and report charts render responsively', async ({ page }) => {
  test.setTimeout(120_000)
  const failures: string[] = []
  page.on('pageerror', error => failures.push(error.message))
  page.on('response', response => {
    if (response.url().includes('/api/') && response.status() >= 500) {
      failures.push(`${response.status()} ${new URL(response.url()).pathname}`)
    }
  })

  await signIn(page)

  const modules = [
    ['/service/repair', '物业服务', 7],
    ['/charge/bill', '费用与财务', 7],
    ['/guard/alarm', '安防门禁', 7],
    ['/life/order', '社区生活', 7],
    ['/social/post', '社区互动', 4],
    ['/report/finance', '数据分析', 5],
    ['/ai/knowledge', 'AI 服务', 2]
  ] as const

  for (const [path, moduleName, navCount] of modules) {
    await page.goto(path, { waitUntil: 'networkidle' })
    await expect(page.locator('.module-heading .eyebrow')).toHaveText(moduleName)
    await expect(page.locator('.module-heading .section-nav a')).toHaveCount(navCount)
  }

  for (const path of ['/report/owner', '/report/service', '/report/finance', '/report/dashboard', '/report/custom']) {
    await page.goto(path, { waitUntil: 'networkidle' })
    await expect(page.locator('canvas').first()).toBeVisible()
    const canvases = await page.locator('canvas').evaluateAll(items => items.map(item => ({
      width: (item as HTMLCanvasElement).width,
      height: (item as HTMLCanvasElement).height,
      pixels: (item as HTMLCanvasElement).toDataURL().length
    })))
    expect(canvases.every(item => item.width > 0 && item.height > 0 && item.pixels > 1000), `${path} contains a blank chart`).toBe(true)
  }

  await page.setViewportSize({ width: 390, height: 844 })
  for (const path of ['/service/repair', '/charge/report', '/guard/alarm', '/life/order', '/social/post', '/report/finance', '/ai/knowledge']) {
    await page.goto(path, { waitUntil: 'networkidle' })
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)
    expect(overflow, `${path} has page-level horizontal overflow`).toBe(false)
  }

  expect(failures).toEqual([])
})

test('AI configuration is secure and responsive', async ({ page }) => {
  const failedApi: string[] = []
  page.on('response', response => {
    if (response.url().includes('/api/') && response.status() >= 500) {
      failedApi.push(`${response.status()} ${new URL(response.url()).pathname}`)
    }
  })

  await signIn(page)
  await page.goto('/system/ai-config', { waitUntil: 'networkidle' })

  await expect(page.getByRole('heading', { name: 'AI配置', exact: true })).toBeVisible()
  await expect(page.getByText('OpenAI兼容 API', { exact: true })).toBeVisible()
  await expect(page.getByLabel('API Base URL')).toBeVisible()
  await expect(page.locator('input[type="password"]')).toHaveCount(1)
  await expect(page.getByRole('button', { name: '测试连接' })).toBeDisabled()

  const responseKeys = await page.evaluate(async () => {
    const response = await fetch('/api/v1/system/ai-config', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    })
    const payload = await response.json()
    return Object.keys(payload.data || {})
  })
  expect(responseKeys).not.toContain('apiKey')

  await page.setViewportSize({ width: 390, height: 844 })
  await page.reload({ waitUntil: 'networkidle' })
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)
  expect(overflow).toBe(false)
  expect(failedApi).toEqual([])
})
