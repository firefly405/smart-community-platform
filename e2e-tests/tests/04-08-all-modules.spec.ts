import { test, expect } from '@playwright/test';

const BASE = process.env.E2E_BASE_URL || 'http://127.0.0.1:5174';

async function doLogin(page: any) {
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.locator('input[placeholder*="用户名"]').fill('admin');
  await page.locator('input[placeholder*="密码"]').fill(process.env.E2E_PASSWORD || 'admin123');
  await page.locator('button:has-text("登")').first().click();
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

// ========== 04 - 物业服务 ==========
test.describe('04 - 物业服务', () => {
  test.beforeEach(async ({ page }) => { await doLogin(page); });

  const pages = [
    { path: '/service/repair', name: '报修管理' },
    { path: '/service/complaint', name: '投诉管理' },
    { path: '/service/notice', name: '公告管理' },
    { path: '/service/equipment', name: '设备管理' },
    { path: '/service/inspection', name: '巡检管理' },
    { path: '/service/cleaning', name: '保洁管理' },
  ];
  for (const { path, name } of pages) {
    test(`页面加载: ${name}`, async ({ page }) => {
      await page.goto(`${BASE}${path}`, { waitUntil: 'networkidle' });
      await page.waitForTimeout(1500);
      await page.screenshot({ path: `screenshots/04-service-${name}.png`, fullPage: true });
      expect(page.url()).toContain(path);
    });
  }
});

// ========== 05 - 费用管理 ==========
test.describe('05 - 费用管理', () => {
  test.beforeEach(async ({ page }) => { await doLogin(page); });

  const pages = [
    { path: '/charge/fee-item', name: '费用项目' },
    { path: '/charge/bill', name: '账单管理' },
    { path: '/charge/payment', name: '缴费记录' },
    { path: '/charge/deposit', name: '押金管理' },
    { path: '/charge/refund', name: '退款管理' },
    { path: '/charge/report', name: '财务报表' },
    { path: '/charge/income-report', name: '收支报表' },
  ];
  for (const { path, name } of pages) {
    test(`页面加载: ${name}`, async ({ page }) => {
      await page.goto(`${BASE}${path}`, { waitUntil: 'networkidle' });
      await page.waitForTimeout(1500);
      await page.screenshot({ path: `screenshots/05-charge-${name}.png`, fullPage: true });
      expect(page.url()).toContain(path);
    });
  }
});

// ========== 06 - 安防门禁 ==========
test.describe('06 - 安防门禁', () => {
  test.beforeEach(async ({ page }) => { await doLogin(page); });

  const pages = [
    { path: '/guard/access-device', name: '门禁设备' },
    { path: '/guard/access-permission', name: '门禁权限' },
    { path: '/guard/access-log', name: '通行记录' },
    { path: '/guard/visitor', name: '访客管理' },
    { path: '/guard/camera', name: '监控设备' },
    { path: '/guard/monitor', name: '监控大屏' },
    { path: '/guard/alarm', name: '报警管理' },
  ];
  for (const { path, name } of pages) {
    test(`页面加载: ${name}`, async ({ page }) => {
      await page.goto(`${BASE}${path}`, { waitUntil: 'networkidle' });
      await page.waitForTimeout(1500);
      await page.screenshot({ path: `screenshots/06-guard-${name}.png`, fullPage: true });
      expect(page.url()).toContain(path);
    });
  }
});

// ========== 07 - 社区生活 ==========
test.describe('07 - 社区生活', () => {
  test.beforeEach(async ({ page }) => { await doLogin(page); });

  const pages = [
    { path: '/life/product', name: '社区商品' },
    { path: '/life/order', name: '商品订单' },
    { path: '/life/service', name: '便民服务' },
    { path: '/life/express', name: '快递管理' },
    { path: '/life/activity', name: '社区活动' },
    { path: '/life/rental', name: '房屋租售' },
  ];
  for (const { path, name } of pages) {
    test(`页面加载: ${name}`, async ({ page }) => {
      await page.goto(`${BASE}${path}`, { waitUntil: 'networkidle' });
      await page.waitForTimeout(1500);
      await page.screenshot({ path: `screenshots/07-life-${name}.png`, fullPage: true });
      expect(page.url()).toContain(path);
    });
  }
});

// ========== 08 - 社区互动 ==========
test.describe('08 - 社区互动', () => {
  test.beforeEach(async ({ page }) => { await doLogin(page); });

  const pages = [
    { path: '/social/post', name: '社区帖子' },
    { path: '/social/secondhand', name: '二手交易' },
    { path: '/social/vote', name: '社区投票' },
    { path: '/social/phone', name: '便民电话' },
  ];
  for (const { path, name } of pages) {
    test(`页面加载: ${name}`, async ({ page }) => {
      await page.goto(`${BASE}${path}`, { waitUntil: 'networkidle' });
      await page.waitForTimeout(1500);
      await page.screenshot({ path: `screenshots/08-social-${name}.png`, fullPage: true });
      expect(page.url()).toContain(path);
    });
  }
});

// ========== 09 - 数据报表 ==========
test.describe('09 - 数据报表', () => {
  test.beforeEach(async ({ page }) => { await doLogin(page); });

  const pages = [
    { path: '/report/owner', name: '业主分析' },
    { path: '/report/service', name: '服务分析' },
    { path: '/report/finance', name: '财务分析' },
    { path: '/report/dashboard', name: '运营大屏' },
    { path: '/report/custom', name: '自定义报表' },
  ];
  for (const { path, name } of pages) {
    test(`页面加载: ${name}`, async ({ page }) => {
      await page.goto(`${BASE}${path}`, { waitUntil: 'networkidle' });
      await page.waitForTimeout(1500);
      await page.screenshot({ path: `screenshots/09-report-${name}.png`, fullPage: true });
      expect(page.url()).toContain(path);
    });
  }
});
