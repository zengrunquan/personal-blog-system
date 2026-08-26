import { chromium } from '@playwright/test'

const baseUrl =
  process.env.QA_BASE_URL ??
  'http://localhost:8080/personal_blog_system_war_exploded/'
const outputDir = process.env.QA_OUTPUT_DIR

if (!outputDir) {
  throw new Error('缺少 QA_OUTPUT_DIR，无法确定截图输出目录')
}

const browser = await chromium.launch({
  executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  headless: true,
})

const captures = [
  { name: 'home-reference-size.png', width: 1024, height: 1536 },
  { name: 'home-desktop.png', width: 1440, height: 1024 },
  { name: 'home-mobile.png', width: 390, height: 844 },
]

const results = []

try {
  for (const capture of captures) {
    const page = await browser.newPage({
      viewport: { width: capture.width, height: capture.height },
      deviceScaleFactor: 1,
    })
    const consoleErrors = []
    page.on('console', (message) => {
      if (message.type() === 'error') {
        consoleErrors.push(message.text())
      }
    })
    page.on('pageerror', (error) => consoleErrors.push(error.stack ?? error.message))

    const response = await page.goto(baseUrl, { waitUntil: 'networkidle' })
    await page.locator('main').waitFor({ state: 'visible' })
    const title = await page.locator('h1').first().textContent()
    const screenshotPath = `${outputDir}\\${capture.name}`
    await page.screenshot({ path: screenshotPath, fullPage: true })

    results.push({
      ...capture,
      status: response?.status(),
      title,
      screenshotPath,
      consoleErrors,
      layout: await page.evaluate(() => {
        const box = (selector) => {
          const element = document.querySelector(selector)
          if (!element) return null
          const rect = element.getBoundingClientRect()
          return { top: rect.top + scrollY, width: rect.width, height: rect.height }
        }
        return {
          header: box('.site-header'),
          hero: box('.hero'),
          content: box('.home-content'),
          footer: box('.site-footer'),
          article: box('.article-row'),
          rail: box('.home-rail'),
        }
      }),
      pageSize: await page.evaluate(() => ({
        width: document.documentElement.scrollWidth,
        height: document.documentElement.scrollHeight,
      })),
    })
    await page.close()
  }
} finally {
  await browser.close()
}

process.stdout.write(JSON.stringify(results, null, 2))
