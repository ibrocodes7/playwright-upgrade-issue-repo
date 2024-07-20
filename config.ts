export default {
  BROWSER: (process.env.BROWSER! || 'chromium') as 'chromium' | 'firefox' | 'webkit',
  BROWSER_WIDTH: parseInt(process.env.BROWSER_WIDTH, 10) || 1920,
  BROWSER_HEIGHT: parseInt(process.env.BROWSER_HEIGHT, 10) || 1080,
  RETRIES: parseInt(process.env.RETRIES, 10) || 0,
  REPEAT_EACH: parseInt(process.env.REPEAT_EACH, 10) || 1,
  HEADLESS: !!(process.env.HEADLESS && process.env.HEADLESS.toLowerCase() === 'true'),
  TEST_TIMEOUT: parseInt(process.env.TEST_TIMEOUT, 10) || 8, // minutes
  NAVIGATION_TIMEOUT: parseInt(process.env.NAVIGATION_TIMEOUT, 10) || 20, // seconds
  ACTION_TIMEOUT: parseInt(process.env.ACTION_TIMEOUT, 10) || 20, // seconds
  WORKERS: parseInt(process.env.WORKERS, 10) || 1,
  CUSTOM_TIMEOUT_MULTIPLY: parseInt(process.env.CUSTOM_TIMEOUT_MULTIPLY, 10) || 1,
};
