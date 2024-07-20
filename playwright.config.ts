import { PlaywrightTestConfig } from '@playwright/test';
import blinkConfig from './config';

const config: PlaywrightTestConfig & { args: string[]; headless: boolean } = {
  // globalSetup: './global-setup',
  // globalTeardown: './global-teardown',
  args: ['--disable-features=IsolateOrigins,site-per-process'],
  headless: false,

  use: {
    // Artifacts
    headless: blinkConfig.HEADLESS,
    viewport: { width: blinkConfig.BROWSER_WIDTH, height: blinkConfig.BROWSER_HEIGHT },
    ignoreHTTPSErrors: true,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: {
      mode: 'retain-on-failure',
      screenshots: true,
      snapshots: true,
      sources: true,
    },
    actionTimeout: blinkConfig.ACTION_TIMEOUT * 1000 * blinkConfig.CUSTOM_TIMEOUT_MULTIPLY,
    navigationTimeout: blinkConfig.NAVIGATION_TIMEOUT * 1000 * blinkConfig.CUSTOM_TIMEOUT_MULTIPLY,
    browserName: blinkConfig.BROWSER,
    permissions: ['clipboard-read', 'clipboard-write'],
  },
  timeout: blinkConfig.TEST_TIMEOUT * 60 * 1000 * blinkConfig.CUSTOM_TIMEOUT_MULTIPLY,

  expect: { timeout: 15 * 1000 }, // 15 seconds

  retries: blinkConfig.RETRIES,

  workers: blinkConfig.WORKERS,

  repeatEach: blinkConfig.REPEAT_EACH,

  // testMatch: [''],
  testDir: 'tests',

  reporter: [
    ['list'],
    [
      'allure-playwright',
      {
        suiteTitle: true,
        environmentInfo: {
          NODE_VERSION: process.version,
          OS: process.platform,
          IS_HEADLESS: process.env.HEADLESS,
          BROWSER: process.env.BROWSER,
        },
      },
    ],
    ['json', { outputFile: 'test-results.json' }],
    ['junit', { outputFile: 'test-results.xml' }],
    ['html', { open: 'never' }],
  ],

  // outputDir: './reports',
  // reporter: 'line',
  // outputDir: path.dirname('./ts-results'),
  // Two reporters for CI:
  // - concise "dot"
  // - comprehensive json report

  // reporter: !process.env.CI
  //   ? // Default 'list' reporter for the terminal
  //     'list'
  //   : // Two reporters for CI:
  //     // - concise "dot"
  //     // - comprehensive json report
  //     [['dot'], ['json', { outputFile: 'test-results.json' }], ['junit']],

  // projects: [
  //   {
  //     name: 'Chromium',
  //     use: {
  //       browserName: 'chromium',

  //       // Context options
  //       viewport: { width: 1920, height: 1020 },
  //     },
  //   },
  //   {
  //     name: 'Firefox',
  //     use: {
  //       browserName: 'firefox',

  //       // Context options
  //       viewport: { width: 1920, height: 1020 },
  //     },
  //   },
  //   {
  //     name: 'WebKit',
  //     use: { browserName: 'webkit', viewport: { width: 600, height: 800 } },
  //   },
  // ],
};

export default config;
