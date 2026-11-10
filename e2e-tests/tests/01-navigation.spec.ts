import { test, expect, type Page } from '@playwright/test';

const target = '/property/house?from=bookmark#list';
const userInfo = { id: 1, username: 'admin', realName: '管理员', roles: [], userType: 5, status: 1 };

test.beforeEach(async ({ context }) => {
  // Keep navigation regressions independent of database contents and real credentials.
  await context.route('**/api/v1/**', async route => {
    const path = new URL(route.request().url()).pathname;
    const data = path === '/api/v1/setup/check' ? { needSetup: false }
      : path.startsWith('/api/v1/auth/login') ? { token: 'navigation-test-token', userInfo }
      : path === '/api/v1/user/info' ? userInfo
      : { records: [], total: 0 };
    await route.fulfill({ json: { code: 200, data } });
  });
});

async function signIn(page: Page) {
  await page.getByPlaceholder('请输入用户名/手机号').fill('admin');
  await page.getByPlaceholder('请输入密码', { exact: true }).fill('test-password');
  await page.locator('.login-button:visible').click();
  await expect(page.locator('.layout-container')).toBeVisible();
}

async function navigate(page: Page, path: string) {
  await page.evaluate(async destination => {
    const router = (document.querySelector('#app') as any).__vue_app__.config.globalProperties.$router;
    await router.push(destination);
  }, path);
}

test('opening a business URL preserves query and hash through login, refresh and a new tab', async ({ page, context }) => {
  await page.goto(target);
  await expect(page).toHaveURL(/\/login\?/);
  expect(new URL(page.url()).searchParams.get('redirect')).toBe(target);
  await signIn(page);
  await expect(page).toHaveURL(new RegExp('/property/house\\?from=bookmark#list$'));
  await page.reload();
  await expect(page.locator('.layout-container')).toBeVisible();
  await expect(page).toHaveURL(new RegExp('/property/house\\?from=bookmark#list$'));
  const another = await context.newPage();
  await another.goto('/charge/bill');
  await expect(another.locator('.layout-container')).toBeVisible();
  await expect(another).toHaveURL(/\/charge\/bill$/);
});

for (const httpStatus of [200, 401]) {
  test(`concurrent expired requests preserve the business URL (HTTP ${httpStatus})`, async ({ page }) => {
    await page.goto(target);
    await signIn(page);
    await expect(page).toHaveURL(/\/property\/house\?/);
    await page.route('**/api/navigation-expired/*', route => route.fulfill({
      status: httpStatus, json: { code: 401, message: 'expired' }
    }));
    await page.evaluate(async () => {
      const modulePath = performance.getEntriesByType('resource').find(entry => new URL(entry.name).pathname === '/src/utils/request.ts')!.name;
      const { default: request } = await import(modulePath);
      await Promise.allSettled([request.get('/api/navigation-expired/a'), request.get('/api/navigation-expired/b')]);
    });
    await expect(page).toHaveURL(/\/login\?/);
    expect(new URL(page.url()).searchParams.get('redirect')).toBe(target);
    await signIn(page);
    await expect(page).toHaveURL(/\/property\/house\?from=bookmark#list$/);
  });
}

test('a late 401 from an old session cannot clear a newer login', async ({ page }) => {
  await page.goto(target);
  await signIn(page);
  await expect(page).toHaveURL(/\/property\/house\?/);
  await page.route('**/api/navigation-stale', async route => {
    await page.evaluate(() => localStorage.setItem('token', 'newer-session-token'));
    await route.fulfill({ status: 401, json: { code: 401 } });
  });
  await page.evaluate(async () => {
    const modulePath = performance.getEntriesByType('resource').find(entry => new URL(entry.name).pathname === '/src/utils/request.ts')!.name;
    const { default: request } = await import(modulePath);
    await request.get('/api/navigation-stale').catch(() => {});
  });
  expect(await page.evaluate(() => localStorage.getItem('token'))).toBe('newer-session-token');
  await expect(page).toHaveURL(/\/property\/house\?from=bookmark#list$/);
});

test('repeated Enter submits once and a late login response cannot override navigation', async ({ page }) => {
  let attempts = 0;
  let release!: () => void;
  const delayed = new Promise<void>(resolve => { release = resolve; });
  await page.route('**/api/v1/auth/login', async route => {
    attempts++;
    await delayed;
    await route.fulfill({ json: { code: 200, data: { token: 'late-token', userInfo } } });
  });
  await page.goto('/login?redirect=' + encodeURIComponent(target));
  const password = page.getByPlaceholder('请输入密码', { exact: true });
  await page.getByPlaceholder('请输入用户名/手机号').fill('admin');
  await password.fill('test-password');
  await password.press('Enter');
  await expect.poll(() => attempts).toBe(1);
  await password.press('Enter');
  await password.press('Enter');
  await page.evaluate(() => localStorage.setItem('token', 'active-session'));
  await navigate(page, '/system/user');
  await expect(page).toHaveURL(/\/system\/user$/);
  const response = page.waitForResponse('**/api/v1/auth/login');
  release();
  await response;
  await page.waitForLoadState('networkidle');
  expect(attempts).toBe(1);
  await expect(page).toHaveURL(/\/system\/user$/);
  expect(await page.evaluate(() => localStorage.getItem('token'))).toBe('active-session');
});

test('SMS login returns to the requested business page', async ({ page }) => {
  await page.goto(target);
  await page.getByRole('tab', { name: '短信登录' }).click();
  await page.getByPlaceholder('请输入手机号', { exact: true }).fill('13800138000');
  await page.getByPlaceholder('请输入验证码').fill('123456');
  await page.locator('.login-button:visible').click();
  await expect(page).toHaveURL(/\/property\/house\?from=bookmark#list$/);
});

for (const redirect of ['https://example.com', '//example.com', '/\\example.com', '/login', '/setup']) {
  test(`reject unsafe or looping login return address: ${redirect}`, async ({ page }) => {
    await page.goto('/login?redirect=' + encodeURIComponent(redirect));
    await signIn(page);
    await expect(page).toHaveURL(/\/dashboard$/);
  });
}

test('refresh and back/forward preserve an authenticated business page', async ({ page }) => {
  await page.goto('/login');
  await signIn(page);
  await expect(page).toHaveURL(/\/dashboard$/);
  await navigate(page, '/system/user');
  await navigate(page, '/charge/bill');
  await page.reload();
  await expect(page.locator('.layout-container')).toBeVisible();
  await expect(page).toHaveURL(/\/charge\/bill$/);
  await page.goBack();
  await expect(page).toHaveURL(/\/system\/user$/);
  await page.goForward();
  await expect(page).toHaveURL(/\/charge\/bill$/);
  await page.goto('/login');
  await page.reload();
  await expect(page.locator('.login-box')).toBeVisible();
  await expect(page).toHaveURL(/\/login$/);
});
