import { createServer } from 'node:http'
import { readFile } from 'node:fs/promises'
import { chromium } from '@playwright/test'

const sourcePath = process.env.QA_SOURCE_PATH
const implementationPath = process.env.QA_IMPLEMENTATION_PATH
const outputPath = process.env.QA_COMPARISON_PATH

if (!sourcePath || !implementationPath || !outputPath) {
  throw new Error('缺少 QA_SOURCE_PATH、QA_IMPLEMENTATION_PATH 或 QA_COMPARISON_PATH')
}

const [source, implementation] = await Promise.all([
  readFile(sourcePath),
  readFile(implementationPath),
])

const server = createServer((request, response) => {
  if (request.url === '/source.png') {
    response.writeHead(200, { 'Content-Type': 'image/png' })
    response.end(source)
    return
  }
  if (request.url === '/implementation.png') {
    response.writeHead(200, { 'Content-Type': 'image/png' })
    response.end(implementation)
    return
  }

  response.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' })
  response.end(`<!doctype html>
    <html lang="zh-CN">
      <head>
        <meta charset="utf-8" />
        <style>
          * { box-sizing: border-box; }
          body { margin: 0; background: #d8d3ca; font: 24px sans-serif; }
          main { display: grid; grid-template-columns: 1024px 1024px; gap: 24px; padding: 24px; }
          figure { margin: 0; background: #fff; }
          figcaption { height: 52px; padding: 12px 16px; background: #202020; color: #fff; }
          img { display: block; width: 1024px; height: auto; }
        </style>
      </head>
      <body>
        <main>
          <figure><figcaption>视觉基准 · 1024 × 1536</figcaption><img src="/source.png" /></figure>
          <figure><figcaption>Vue 实现 · 1024 CSS px</figcaption><img src="/implementation.png" /></figure>
        </main>
      </body>
    </html>`)
})

await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
const address = server.address()
if (!address || typeof address === 'string') {
  server.close()
  throw new Error('无法启动本地对照页')
}

const browser = await chromium.launch({
  executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  headless: true,
})

try {
  const page = await browser.newPage({ viewport: { width: 2100, height: 2000 }, deviceScaleFactor: 1 })
  await page.goto(`http://127.0.0.1:${address.port}`, { waitUntil: 'networkidle' })
  await page.screenshot({ path: outputPath, fullPage: true })
} finally {
  await browser.close()
  server.close()
}

process.stdout.write(outputPath)
