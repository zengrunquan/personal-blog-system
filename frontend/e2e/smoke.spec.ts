import { expect, test } from '@playwright/test'

test('访客可以阅读首页并通过搜索进入可刷新列表', async ({ page, isMobile }) => {
  await page.goto('./')

  await expect(page.getByRole('heading', { level: 1, name: '写代码，也写生活' })).toBeVisible()
  await expect(page.getByRole('link', { name: /开始阅读/ })).toBeVisible()

  if (isMobile) {
    await page.getByRole('button', { name: '打开导航' }).click()
  }
  const search = page.getByRole('searchbox', { name: '搜索文章' })
  await search.fill('JavaWeb')
  await search.press('Enter')

  await expect(page).toHaveURL(/\/articles\?q=JavaWeb$/)
  await expect(page.getByRole('heading', { level: 1, name: '文章' })).toBeVisible()
  await expect(page.locator('html')).not.toHaveClass(/overflow/)
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
    await page.evaluate(() => window.innerWidth),
  )
})

test('旧登录地址永久跳转到 Vue 登录页', async ({ page, baseURL }) => {
  const oldLoginUrl = new URL('login.jsp', baseURL).toString()
  const response = await page.goto(oldLoginUrl)

  expect(response?.status()).toBe(200)
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('heading', { level: 1, name: /回来继续写/ })).toBeVisible()
})

test('未知无扩展名地址由 Vue 404 页面接管', async ({ page }) => {
  const response = await page.goto('unknown-route')

  expect(response?.status()).toBe(200)
  await expect(page.getByRole('heading', { level: 1, name: '这一页暂时没有文字' })).toBeVisible()
})

test('手机端导航可展开且页面没有横向裁切', async ({ page, isMobile }) => {
  test.skip(!isMobile, '仅在手机项目验证折叠导航')
  await page.goto('./')

  await page.getByRole('button', { name: '打开导航' }).click()
  await expect(page.getByRole('navigation', { name: '主导航' })).toBeVisible()
  await expect(page.getByRole('link', { name: '文章', exact: true })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
    await page.evaluate(() => window.innerWidth),
  )
})
