import { defineConfig } from '@playwright/test'
import base from './playwright.config'

export default defineConfig({
  ...base,
  testMatch: ['08-live-page-errors.spec.ts'],
  outputDir: './test-results/live-page-errors',
  use: {
    ...base.use,
    baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:5174',
    channel: process.env.E2E_CHANNEL || 'msedge',
    video: 'off'
  }
})
