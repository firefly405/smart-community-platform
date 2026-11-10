import { test, expect } from '@playwright/test';

const BASE = process.env.E2E_BASE_URL || 'http://127.0.0.1:5174';
const USERNAME = 'admin';
const PASSWORD = process.env.E2E_PASSWORD || 'admin123';

/**
 * Helper: 填写登录表单并提交
 */
async function doLogin(page: any, username: string, password: string) {
  // 确保在登录页
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);

  // Element Plus 的 el-input 内部是标准 input，用 placeholder 定位
  const userInput = page.locator('input[placeholder*="用户名"]');
  const passInput = page.locator('input[placeholder*="密码"]');
  const loginBtn = page.locator('button:has-text("登")').first();

  await userInput.fill(username);
  await passInput.fill(password);
  await loginBtn.click();
}

test.describe('01 - 登录认证', () => {

  test('登录页面正常加载', async ({ page }) => {
    await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
    await expect(page).toHaveTitle(/智慧社区/);
    // 验证关键元素
    await expect(page.locator('h1')).toContainText('智慧社区');
    await expect(page.locator('input[placeholder*="用户名"]')).toBeVisible();
    await expect(page.locator('input[placeholder*="密码"]')).toBeVisible();
    await page.screenshot({ path: 'screenshots/01-login-page.png', fullPage: true });
  });

  test('正确凭据登录成功，跳转到首页', async ({ page }) => {
    await doLogin(page, USERNAME, PASSWORD);
    // 等待跳转到 /dashboard
    await page.waitForURL('**/dashboard', { timeout: 10000 });
    await page.waitForTimeout(500);
    await page.screenshot({ path: 'screenshots/01-dashboard.png', fullPage: true });
    expect(page.url()).toContain('dashboard');
  });

  test('错误密码被拒绝', async ({ page }) => {
    await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
    await page.locator('input[placeholder*="用户名"]').fill(USERNAME);
    await page.locator('input[placeholder*="密码"]').fill('wrongpassword');
    await page.locator('button:has-text("登")').first().click();
    // 等待错误消息出现
    await page.waitForTimeout(2000);
    await page.screenshot({ path: 'screenshots/01-login-fail.png', fullPage: true });
    // 仍应在登录页
    expect(page.url()).toContain('login');
  });

  test('空表单提交被拦截', async ({ page }) => {
    await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
    await page.locator('input[placeholder*="用户名"]').fill('');
    await page.locator('input[placeholder*="密码"]').fill('');
    await page.locator('button:has-text("登")').first().click();
    await page.waitForTimeout(1000);
    await page.screenshot({ path: 'screenshots/01-login-empty.png', fullPage: true });
    expect(page.url()).toContain('login');
  });

  test('登录后可访问用户管理页面', async ({ page }) => {
    await doLogin(page, USERNAME, PASSWORD);
    await page.waitForURL('**/dashboard', { timeout: 10000 });
    // 直接导航到用户管理
    await page.goto(`${BASE}/system/user`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1500);
    await page.screenshot({ path: 'screenshots/01-system-user.png', fullPage: true });
    expect(page.url()).toContain('system/user');
  });

  test('侧边栏导航菜单可见', async ({ page }) => {
    await doLogin(page, USERNAME, PASSWORD);
    await page.waitForURL('**/dashboard', { timeout: 10000 });
    await page.waitForTimeout(1000);
    await page.screenshot({ path: 'screenshots/01-sidebar-menu.png', fullPage: true });
  });
});
