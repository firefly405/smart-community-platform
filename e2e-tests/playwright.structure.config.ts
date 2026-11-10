import { defineConfig } from '@playwright/test'
import base from './playwright.config'

// Focused navigation/business checks; legacy screenshot suites use different fixtures.
export default defineConfig({
  ...base,
  testMatch: ['01-navigation.spec.ts', '05-structure.spec.ts', '06-live-structure.spec.ts'],
  outputDir: './test-results/structure',
  use: {
    ...base.use,
    baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:5174',
    channel: process.env.E2E_CHANNEL || 'msedge'
  }
})

