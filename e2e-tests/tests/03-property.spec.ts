import { test, expect } from '@playwright/test';

const BASE = process.env.E2E_BASE_URL || 'http://127.0.0.1:5174';

async function doLogin(page: any) {
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.locator('input[placeholder*="用户名"]').fill('admin');
  await page.locator('input[placeholder*="密码"]').fill(process.env.E2E_PASSWORD || 'admin123');
  await page.locator('button:has-text("登")').first().click();
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

test.describe('03 - 房产物业', () => {

  test.beforeEach(async ({ page }) => {
    await doLogin(page);
  });

  const propertyPages = [
    { path: '/property/building', name: '楼栋管理' },
    { path: '/property/unit', name: '单元管理' },
    { path: '/property/house', name: '房屋管理' },
    { path: '/property/owner', name: '业主管理' },
    { path: '/property/family', name: '家属管理' },
    { path: '/property/tenant', name: '租户管理' },
    { path: '/property/parking', name: '车位管理' },
    { path: '/property/vehicle', name: '车辆管理' },
    { path: '/property/decoration', name: '装修管理' },
  ];

  for (const { path, name } of propertyPages) {
    test(`页面加载: ${name}`, async ({ page }) => {
      await page.goto(`${BASE}${path}`, { waitUntil: 'networkidle' });
      await page.waitForTimeout(1500);
      await page.screenshot({ path: `screenshots/03-property-${name}.png`, fullPage: true });
      expect(page.url()).toContain(path);
    });
  }
});
