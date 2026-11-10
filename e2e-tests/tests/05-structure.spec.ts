import { test, expect, type Page } from '@playwright/test'

test('direct edit submission returns to the business list without browser history',async({page})=>{
  await page.goto('/service/repair/process/42')
  await page.getByPlaceholder('请填写处理结果').fill('测试处理结果')
  await page.getByRole('button',{name:'提交处理',exact:true}).click()
  await expect(page).toHaveURL(/\/service\/repair$/)
})

test('legacy form dialogs open, validate and submit the edited model',async({page})=>{
  await page.goto('/property/building')
  await page.getByRole('button',{name:'新增',exact:true}).click()
  const dialog=page.getByRole('dialog')
  await expect(dialog).toBeVisible()
  await dialog.getByPlaceholder('请输入楼栋编号').fill('TEST-B')
  await dialog.getByPlaceholder('请输入楼栋名称').fill('测试楼栋')
  const submitted=page.waitForRequest(r=>r.method()==='POST'&&new URL(r.url()).pathname==='/api/v1/building')
  await dialog.getByRole('button',{name:'确定',exact:true}).click()
  expect((await submitted).postDataJSON()).toMatchObject({buildingNo:'TEST-B',buildingName:'测试楼栋'})
  await expect(dialog).not.toBeVisible()
})
test('modern form dialogs keep their event-payload contract',async({page})=>{
  await page.goto('/social/phone')
  await page.getByRole('button',{name:'新增',exact:true}).click()
  const dialog=page.getByRole('dialog')
  await expect(dialog).toBeVisible()
  await dialog.getByPlaceholder('请输入名称').fill('测试便民电话')
  await dialog.getByPlaceholder('请输入电话').fill('10000')
  const submitted=page.waitForRequest(r=>r.method()==='POST'&&new URL(r.url()).pathname==='/api/v1/phone')
  await dialog.getByRole('button',{name:'确定',exact:true}).click()
  expect((await submitted).postDataJSON()).toMatchObject({name:'测试便民电话',phone:'10000'})
  await expect(dialog).not.toBeVisible()
})

const groups = [
  ['system','系统管理','user','用户管理'], ['property','房屋与业主','house','房屋管理'],
  ['service','物业服务','maintain','设备维保'], ['charge','费用管理','bill','账单管理'],
  ['life','社区生活','service-order','服务预约'], ['guard','安防管理','visitor','访客管理'],
  ['social','社区互动','post','社区帖子'], ['report','数据分析','owner','业主分析'],
  ['ai','AI服务','qa','智能问答']
]
const menus = groups.map(([path,menuName,child,title],i) => ({
  id:i+1, path:'/'+path, menuName, menuType:1,
  children:[{ path:child,menuName:title,menuType:2 }]
}))
const bill = { id: 73, houseId: 12, ownerId: 5, billNo: 'BILL-73', payableAmount: 100, status: 0 }
test.beforeEach(async ({ context }) => {
  await context.addInitScript(() => localStorage.setItem('token','structure-test'))
  await context.route('**/api/v1/**', async route => {
    const url = new URL(route.request().url())
    const path = url.pathname
    let data: any = { records: [], total: 0 }
    if (path === '/api/v1/setup/check') data = { needSetup:false }
    if (path === '/api/v1/menu/tree') data = menus
    if (path === '/api/v1/house/list') data = { records:[{ id:12,houseNo:'A-1201',ownerId:5 }],total:1 }
    if (path === '/api/v1/fee/bill/list') data = { records:[bill],total:1 }
    if (path === '/api/v1/fee/bill/73') data = bill
    if (path === '/api/v1/fee/payment/list') data = { records:[{ id:1,billId:73,paymentNo:'PAY-73' }],total:1 }
    if (path === '/api/v1/equipment/list') data = { records:[{ id:8,equipmentName:'一号电梯' }],total:1 }
    if (path === '/api/v1/service/list') data = { records:[{ id:9,serviceName:'家政保洁',price:80 }],total:1 }
    if (path === '/api/v1/visitor/42') data = { id:42,visitorName:'指定访客42',ownerId:5,status:1,qrcode:'QR42' }
    if (path === '/api/v1/user/info') data = { id:1,realName:'测试管理员',userType:5,status:1 }
    await route.fulfill({json:{code:200,data}})
  })
})
async function navigate(page: Page,path:string) {
  await page.evaluate(async path => {
    await (document.querySelector('#app') as any).__vue_app__.config.globalProperties.$router.push(path)
  },path)
}
async function related(page: Page,label:string) {
  await page.getByRole('button',{name:'关联业务'}).first().click()
  await page.getByRole('menuitem',{name:label,exact:true}).click()
}
test('workspace shares all nine module entries with menus and supports search', async ({page}) => {
  await page.goto('/workspace')
  await expect(page.locator('.module-card')).toHaveCount(9)
  await page.getByPlaceholder('搜索账单、报修、访客等功能').fill('维保')
  await expect(page.locator('.module-card')).toHaveCount(1)
  await page.locator('.entries').getByRole('link',{name:'设备维保'}).click()
  await expect(page).toHaveURL(/\/service\/maintain$/)
})
test('house to bills to payments preserves associations across refresh and return', async ({page}) => {
  await page.goto('/property/house')
  const bills = page.waitForRequest(r=>r.url().includes('/fee/bill/list'))
  await related(page,'房屋账单')
  expect(new URL((await bills).url()).searchParams.get('houseId')).toBe('12')
  await expect(page.locator('.page-navigation')).toContainText('房屋 ID：12')
  const payments = page.waitForRequest(r=>r.url().includes('/fee/payment/list'))
  await related(page,'缴费记录')
  expect(new URL((await payments).url()).searchParams.get('billId')).toBe('73')
  const refreshed = page.waitForRequest(r=>r.url().includes('/fee/payment/list'))
  await page.reload()
  expect(new URL((await refreshed).url()).searchParams.get('billId')).toBe('73')
  await page.getByRole('button',{name:'返回上一级'}).click()
  await expect(page).toHaveURL(/\/charge\/bill\?houseId=12/)
  await page.getByRole('button',{name:'查看全部'}).click()
  expect(new URL(page.url()).searchParams.has('houseId')).toBe(false)
})
test('bill detail requests only its own payments and direct links have a safe parent', async ({page}) => {
  const requested=page.waitForRequest(r=>r.url().includes('/fee/payment/list'))
  await page.goto('/charge/bill/detail/73')
  expect(new URL((await requested).url()).searchParams.get('billId')).toBe('73')
  await expect(page.getByText('BILL-73').first()).toBeVisible()
  await page.getByRole('button',{name:'返回上一级'}).click()
  await expect(page).toHaveURL(/\/charge\/bill$/)
})
test('visitor detail loads the selected id rather than the first list record', async ({page}) => {
  const paths:string[]=[]
  page.on('request',r=>paths.push(new URL(r.url()).pathname))
  await page.goto('/guard/visitor/detail/42')
  await expect(page.getByText('指定访客42')).toBeVisible()
  await expect(page.getByLabel('访客通行凭证')).toHaveValue('QR42')
  expect(paths).toContain('/api/v1/visitor/42')
  expect(paths).not.toContain('/api/v1/visitor/list')
})
test('equipment relation opens maintenance with equipment scope and prefilled creation', async ({page}) => {
  await page.goto('/service/equipment')
  const requested=page.waitForRequest(r=>r.url().includes('/equipment/maintain/list'))
  await related(page,'维保记录')
  expect(new URL((await requested).url()).searchParams.get('equipmentId')).toBe('8')
  await page.getByRole('button',{name:'新增',exact:true}).click()
  await expect(page.getByRole('dialog')).toContainText('一号电梯')
})
test('service relation filters reservations and prefills the same service', async ({page}) => {
  await page.goto('/life/service')
  const requested=page.waitForRequest(r=>r.url().includes('/service/order/list'))
  await related(page,'服务预约')
  expect(new URL((await requested).url()).searchParams.get('serviceId')).toBe('9')
  await page.getByRole('button',{name:'新增',exact:true}).click()
  await expect(page.getByRole('dialog')).toContainText('家政保洁')
})
test('module roots and old profile bookmarks resolve without returning home',async({page})=>{
  await page.goto('/property')
  await expect(page.locator('.modules')).toContainText('房屋管理')
  await navigate(page,'/password')
  await expect(page).toHaveURL(/\/system\/user-center#password$/)
  await expect(page.locator('#password')).toBeVisible()
})
test('empty or failed menus do not reintroduce hidden modules',async({page,context})=>{
  await context.route('**/api/v1/menu/tree',r=>r.fulfill({json:{code:200,data:[]}}))
  await page.goto('/workspace')
  await expect(page.getByText('没有匹配的功能入口')).toBeVisible()
  await expect(page.locator('.module-card')).toHaveCount(0)
  await context.route('**/api/v1/menu/tree',r=>r.fulfill({status:500,json:{message:'test'}}))
  await page.reload()
  await expect(page.getByText('菜单加载失败')).toBeVisible()
  await expect(page.getByRole('button',{name:'重新加载'})).toBeVisible()
})
test('tables without mutation handlers have no dead edit/add/delete buttons',async({page})=>{
  await page.goto('/charge/payment?billId=73')
  await expect(page.getByText('PAY-73')).toBeVisible()
  await expect(page.getByRole('button',{name:'新增',exact:true})).toHaveCount(0)
  await expect(page.getByRole('button',{name:'编辑',exact:true})).toHaveCount(0)
  await expect(page.getByRole('button',{name:'删除',exact:true})).toHaveCount(0)
})
test('unknown and external return targets never silently redirect to home or leave app',async({page})=>{
  await page.goto('/guard/visitor/detail/42?from=https://example.com')
  await page.getByRole('button',{name:'返回上一级'}).click()
  await expect(page).toHaveURL(/\/guard\/visitor$/)
  await navigate(page,'/missing-business-page')
  await expect(page.getByRole('heading',{name:'404'})).toBeVisible()
  await expect(page).toHaveURL(/\/missing-business-page$/)
})
test('same-component association changes load the new object',async({page})=>{
  await page.goto('/charge/bill?houseId=12')
  const requested=page.waitForRequest(r=>r.url().includes('/fee/bill/list')&&new URL(r.url()).searchParams.get('houseId')==='13')
  await navigate(page,'/charge/bill?houseId=13')
  await requested
  await expect(page.locator('.page-navigation')).toContainText('房屋 ID：13')
})

