import { defineConfig, devices } from '@playwright/test';

const browserChannel = process.env.PLAYWRIGHT_CHANNEL as 'msedge' | 'chrome' | undefined;
const videoMode = process.env.PLAYWRIGHT_VIDEO === 'off' ? 'off' : 'retain-on-failure';

export default defineConfig({
  testDir: './tests',
  timeout: 30000,
  expect: { timeout: 10000 },
  fullyParallel: false,
  retries: 0,
  workers: 1,
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['list']
  ],
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5174',
    channel: browserChannel,
    screenshot: 'on',
    trace: 'retain-on-failure',
    video: videoMode,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
