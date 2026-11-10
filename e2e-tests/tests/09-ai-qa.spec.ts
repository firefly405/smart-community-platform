import { test, expect } from '@playwright/test'

test.beforeEach(async ({ context }) => {
  await context.addInitScript(() => localStorage.setItem('token', 'ai-qa-test'))
  await context.route('**/api/v1/**', async route => {
    const path = new URL(route.request().url()).pathname
    let data: any = null
    if (path === '/api/v1/setup/check') data = { needSetup: false }
    if (path === '/api/v1/user/info') data = { id: 1, realName: 'QA', userType: 5 }
    if (path === '/api/v1/menu/tree') data = []
    await route.fulfill({ json: { code: 200, data } })
  })
})

test('AI answers taking more than 30 seconds still reach the chat', async ({ page }) => {
  test.setTimeout(55000)
  await page.route('**/api/v1/qa/ask-sync', async route => {
    expect(route.request().postDataJSON().question).toBe('Slow question')
    await new Promise(resolve => setTimeout(resolve, 31000))
    await route.fulfill({ json: { code: 200, data: { answer: 'Delayed answer received' } } })
  })
  await page.goto('/ai/qa')
  await page.locator('.chat-input input').fill('Slow question')
  await page.locator('.chat-input input').press('Enter')
  await expect(page.locator('.typing-indicator')).toBeVisible()
  await expect(page.locator('.message.assistant .message-text')).toHaveText('Delayed answer received', { timeout: 40000 })
  await expect(page.locator('.typing-indicator')).toHaveCount(0)
  await expect(page.locator('.chat-input input')).toBeEnabled()
})

test('a failed AI request releases the input for another question', async ({ page }) => {
  await page.route('**/api/v1/qa/ask-sync', route => route.abort('failed'))
  await page.goto('/ai/qa')
  await page.locator('.chat-input input').fill('Failed question')
  await page.locator('.chat-input input').press('Enter')
  await expect(page.locator('.message.assistant .message-text')).toContainText('AI服务暂时不可用')
  await expect(page.locator('.typing-indicator')).toHaveCount(0)
  await expect(page.locator('.chat-input input')).toBeEnabled()
})
