import { test, expect } from '@playwright/test'
test('real menus, route targets and read-only business navigation',async({page,request},testInfo)=>{
  test.skip(!process.env.E2E_USERNAME || !process.env.E2E_PASSWORD, 'Set local test account via environment')
  const response=await request.post('http://127.0.0.1:8001/api/v1/auth/login',{data:{username:process.env.E2E_USERNAME,password:process.env.E2E_PASSWORD}})
  const login=await response.json()
  expect(login.code).toBe(200)
  await page.addInitScript(token=>localStorage.setItem('token',token),login.data.token)
  const errors:string[]=[]
  page.on('pageerror',error=>errors.push(error.message))
  await page.goto('/workspace')
  await expect(page.locator('.modules .module-card')).toHaveCount(9)
  const invalid=await page.locator('.entries a').evaluateAll(links=>{
    const router=(document.querySelector('#app') as any).__vue_app__.config.globalProperties.$router
    return links.map(a=>a.getAttribute('href')!).filter(path=>router.resolve(path).name==='NotFound')
  })
  expect(invalid).toEqual([])
  await page.screenshot({path:testInfo.outputPath('workspace-verified.png'),fullPage:true})
  await page.goto('/property/house')
  await page.getByRole('button',{name:'关联业务'}).first().click()
  await page.getByRole('menuitem',{name:'房屋账单',exact:true}).click()
  await expect(page.locator('.page-navigation')).toContainText('房屋 ID：')
  const linkedUrl=page.url()
  await page.reload()
  await expect(page).toHaveURL(linkedUrl)
  await page.getByRole('button',{name:'返回上一级'}).click()
  await expect(page).toHaveURL(/\/property\/house$/)
  expect(errors).toEqual([])
})
