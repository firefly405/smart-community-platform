import { test, expect } from '@playwright/test'

test('live owner publishing, cross-owner replies and seller lifecycle remain isolated', async ({ request }) => {
  async function login(username: string) {
    const result = await (await request.post('/api/v1/auth/login', { data: { username, password: process.env.COMMUNITY_TEST_PASSWORD || 'admin123' } })).json()
    expect(result.code).toBe(200)
    return { Authorization: `Bearer ${result.data.token}` }
  }
  const first = await login('owner001'), second = await login('owner002'), admin = await login('admin')
  const root = '/api/v1/resident/social'
  let postId: number | undefined, commentId: number | undefined, replyId: number | undefined, itemId: number | undefined
  const title = '自动验证临时记录-' + Date.now()
  const post = { title, content: '自动验证后删除', category: 2, images: [], userId: 1, status: 2, isTop: 1 }
  const item = { title, description: '自动验证后删除', category: 1, price: 9.99, originPrice: 20, conditionLevel: 2, contactName: '验证账号', contactPhone: '13800138000', tradeMethod: 1, images: [], userId: 1, status: 1 }
  try {
    const created = await (await request.post(root + '/posts', { headers: first, data: post })).json()
    expect(created.code).toBe(200); postId = created.data
    const detail = await (await request.get(`${root}/posts/${postId}`, { headers: second })).json()
    expect(detail.data).toMatchObject({ title, userId: 13, status: 1, isTop: 0 })
    for (const method of ['put', 'delete'] as const) {
      const blocked = await request[method](`${root}/posts/${postId}`, { headers: second, ...(method === 'put' ? { data: post } : {}) })
      expect((await blocked.json()).code).toBe(403)
    }
    const comment = await (await request.post(`${root}/posts/${postId}/comments`, { headers: second, data: { content: '邻居评论', userId: 1 } })).json()
    expect(comment.code).toBe(200); commentId = comment.data
    const reply = await (await request.post(`${root}/posts/${postId}/comments`, { headers: first, data: { content: '业主回复', parentId: commentId } })).json()
    expect(reply.code).toBe(200); replyId = reply.data
    const comments = await (await request.get(`${root}/posts/${postId}/comments`, { headers: first })).json()
    expect(comments.data.records).toEqual(expect.arrayContaining([expect.objectContaining({ id: commentId, userId: 14 }), expect.objectContaining({ id: replyId, userId: 13, parentId: commentId })]))
    expect((await (await request.delete(`${root}/comments/${commentId}`, { headers: first })).json()).code).toBe(403)
    expect((await (await request.get(`${root}/posts/${postId}`, { headers: first })).json()).data.commentCount).toBe(2)

    const createdItem = await (await request.post(root + '/items', { headers: first, data: item })).json()
    expect(createdItem.code).toBe(200); itemId = createdItem.data
    expect((await (await request.get(`${root}/items/${itemId}`, { headers: second })).json()).data).toMatchObject({ userId: 13, status: 0, contactPhone: item.contactPhone })
    expect((await (await request.put(`${root}/items/${itemId}`, { headers: second, data: item })).json()).code).toBe(403)
    expect((await (await request.put(`${root}/items/${itemId}`, { headers: first, data: { ...item, originPrice: null, price: 8.88 } })).json()).code).toBe(200)
    const edited = await (await request.get(`${root}/items/${itemId}`, { headers: first })).json()
    expect(edited.data.originPrice).toBeNull(); expect(edited.data.price).toBe(8.88)
    expect((await (await request.put(`${root}/items/${itemId}/status`, { headers: first, data: { status: 1 } })).json()).code).toBe(200)
    expect((await (await request.get(`${root}/items/${itemId}`, { headers: second })).json()).code).toBe(404)
    expect((await (await request.get(`${root}/items/${itemId}`, { headers: first })).json()).data.status).toBe(1)
    expect((await (await request.put(`${root}/items/${itemId}/status`, { headers: first, data: { status: 0 } })).json()).code).toBe(400)
    for (const path of ['/post/list', '/comment/list', '/secondhand/list', '/qa/history', '/user/list']) expect((await request.get('/api/v1' + path, { headers: first })).status()).toBe(403)
    for (const path of ['/resident/social/posts', '/resident/qa/history']) expect((await request.get('/api/v1' + path, { headers: admin })).status()).toBe(403)
  } finally {
    if (replyId) expect((await (await request.delete(`${root}/comments/${replyId}`, { headers: first })).json()).code).toBe(200)
    if (commentId) expect((await (await request.delete(`${root}/comments/${commentId}`, { headers: second })).json()).code).toBe(200)
    if (postId) expect((await (await request.delete(`${root}/posts/${postId}`, { headers: first })).json()).code).toBe(200)
    if (itemId) expect((await (await request.delete(`${root}/items/${itemId}`, { headers: first })).json()).code).toBe(200)
  }
})

test('live new resident pages render without server errors', async ({ page, request }) => {
  const result = await (await request.post('/api/v1/auth/login', { data: { username: 'owner001', password: process.env.COMMUNITY_TEST_PASSWORD || 'admin123' } })).json()
  await page.addInitScript(token => localStorage.setItem('token', token), result.data.token)
  const errors: string[] = []; page.on('pageerror', e => errors.push(e.message))
  for (const width of [1440, 390]) {
    await page.setViewportSize({ width, height: 900 })
    for (const path of ['posts', 'marketplace', 'qa']) {
      await page.goto('/community/' + path)
      await expect(page.locator('.resident-shell')).toBeVisible()
      await expect(page.locator('.el-loading-mask')).toHaveCount(0)
      await expect(page.locator('.el-message--error')).toHaveCount(0)
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
      await page.screenshot({ path: `test-results/community-live-${width}-${path}.png`, fullPage: true })
    }
  }
  expect(errors).toEqual([])
})
