import { test, expect } from '@playwright/test';

const BASE = process.env.E2E_BASE_URL || 'http://127.0.0.1:5174';

async function doLogin(page: any) {
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.locator('input[placeholder*="用户名"]').fill('admin');
  await page.locator('input[placeholder*="密码"]').fill(process.env.E2E_PASSWORD || 'admin123');
  await page.locator('button:has-text("登")').first().click();
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

test.describe('02 - 系统管理', () => {

  test.beforeEach(async ({ page }) => {
    await doLogin(page);
  });

  const systemPages = [
    { path: '/system/user', name: '用户管理' },
    { path: '/system/role', name: '角色管理' },
    { path: '/system/menu', name: '菜单管理' },
    { path: '/system/dept', name: '部门管理' },
    { path: '/system/log', name: '操作日志' },
    { path: '/system/login-log', name: '登录日志' },
    { path: '/system/user-center', name: '个人中心' },
  ];

  for (const { path, name } of systemPages) {
    test(`页面加载: ${name}`, async ({ page }) => {
      await page.goto(`${BASE}${path}`, { waitUntil: 'networkidle' });
      await page.waitForTimeout(1500);
      await page.screenshot({ path: `screenshots/02-system-${name}.png`, fullPage: true });
      expect(page.url()).toContain(path);
    });
  }
});
