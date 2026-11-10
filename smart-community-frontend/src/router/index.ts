import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import request from '@/utils/request'
import { useAuthStore } from '@/store/auth'
import { getUserInfo } from '@/api/auth'

let setupChecked = false
let needSetup = false

const routes: RouteRecordRaw[] = [
  { path: '/community/login', name: 'CommunityLogin', component: () => import('@/views/login/index.vue'), meta: { title: '业主登录', communityLogin: true } },
  {
    path: '/community', component: () => import('@/views/community/Layout.vue'), redirect: '/community/home',
    children: [
      { path: 'qa', component: () => import('@/views/community/CommunityQa.vue'), meta: { title: '智能问答' } },
      { path: 'marketplace', component: () => import('@/views/community/Marketplace.vue'), meta: { title: '邻里二手' } },
      { path: 'posts', component: () => import('@/views/community/Forum.vue'), meta: { title: '社区广场' } },
      { path: 'posts/:id', component: () => import('@/views/community/Forum.vue'), meta: { title: '帖子详情' } },
      { path: 'home', component: () => import('@/views/community/ResidentPage.vue'), meta: { title: '社区首页', residentSection: 'home' } },
      ...Object.entries({ houses: '我的房屋', bills: '我的账单', vehicles: '我的车辆', repairs: '在线报修', complaints: '投诉建议', notices: '社区公告', activities: '社区活动', phones: '便民电话', profile: '个人中心' })
        .map(([path, title]) => ({ path, component: () => import('@/views/community/ResidentPage.vue'), meta: { title, residentSection: path } }))
    ]
  },
  { path: '/tenant/login', name: 'TenantLogin', component: () => import('@/views/login/index.vue'), meta: { title: '租户登录', tenantLogin: true } },
  {
    path: '/tenant', component: () => import('@/views/tenant/Layout.vue'), redirect: '/tenant/home',
    children: Object.entries({
      home: '首页概览', lease: '我的租约 / 房屋', bills: '我的账单', repairs: '在线报修',
      complaints: '投诉建议', notices: '社区公告', activities: '社区活动', phones: '便民电话', profile: '个人中心'
    }).map(([path, title]) => ({ path, component: () => import('@/views/tenant/PortalPage.vue'), meta: { title, tenantSection: path } }))
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'workspace', name: 'workspaceOverview', component: () => import('@/views/workspace/index.vue'), meta: { title: '功能导航' } },
      { path: 'system', name: 'systemOverview', component: () => import('@/views/workspace/index.vue'), meta: { title: '系统管理' } },
      { path: 'property', name: 'propertyOverview', component: () => import('@/views/workspace/index.vue'), meta: { title: '房屋与业主' } },
      { path: 'service', name: 'serviceOverview', component: () => import('@/views/workspace/index.vue'), meta: { title: '物业服务' } },
      { path: 'charge', name: 'chargeOverview', component: () => import('@/views/workspace/index.vue'), meta: { title: '费用与财务' } },
      { path: 'life', name: 'lifeOverview', component: () => import('@/views/workspace/index.vue'), meta: { title: '社区生活' } },
      { path: 'guard', name: 'guardOverview', component: () => import('@/views/workspace/index.vue'), meta: { title: '安防门禁' } },
      { path: 'social', name: 'socialOverview', component: () => import('@/views/workspace/index.vue'), meta: { title: '社区互动' } },
      { path: 'report', name: 'reportOverview', component: () => import('@/views/workspace/index.vue'), meta: { title: '数据分析' } },
      { path: 'ai', name: 'aiOverview', component: () => import('@/views/workspace/index.vue'), meta: { title: 'AI服务' } },
      { path: 'service/maintain', name: 'ServiceMaintain', component: () => import('@/views/property/MaintainList.vue'), meta: { title: '设备维保' } },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/RoleList.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'system/menu',
        name: 'SystemMenu',
        component: () => import('@/views/system/MenuList.vue'),
        meta: { title: '菜单管理' }
      },
      {
        path: 'system/dept',
        name: 'SystemDept',
        component: () => import('@/views/system/DeptList.vue'),
        meta: { title: '部门管理' }
      },
      {
        path: 'system/log',
        name: 'SystemLog',
        component: () => import('@/views/system/LogList.vue'),
        meta: { title: '操作日志' }
      },
      {
        path: 'system/login-log',
        name: 'SystemLoginLog',
        component: () => import('@/views/system/LoginLogList.vue'),
        meta: { title: '登录日志' }
      },
      {
        path: 'system/ai-config',
        name: 'SystemAiConfig',
        component: () => import('@/views/system/AiConfig.vue'),
        meta: { title: 'AI配置' }
      },
      {
        path: 'system/user-center',
        name: 'UserCenter',
        component: () => import('@/views/system/UserCenter.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'property/building',
        name: 'PropertyBuilding',
        component: () => import('@/views/property/BuildingList.vue'),
        meta: { title: '楼栋管理' }
      },
      {
        path: 'property/unit',
        name: 'PropertyUnit',
        component: () => import('@/views/property/UnitList.vue'),
        meta: { title: '单元管理' }
      },
      {
        path: 'property/house',
        name: 'PropertyHouse',
        component: () => import('@/views/property/HouseList.vue'),
        meta: { title: '房屋管理' }
      },
      {
        path: 'property/owner',
        name: 'PropertyOwner',
        component: () => import('@/views/property/OwnerList.vue'),
        meta: { title: '业主管理' }
      },
      {
        path: 'property/family',
        name: 'PropertyFamily',
        component: () => import('@/views/property/FamilyList.vue'),
        meta: { title: '家属管理' }
      },
      {
        path: 'property/tenant',
        name: 'PropertyTenant',
        component: () => import('@/views/property/TenantList.vue'),
        meta: { title: '租户管理' }
      },
      {
        path: 'property/parking',
        name: 'PropertyParking',
        component: () => import('@/views/property/ParkingList.vue'),
        meta: { title: '车位管理' }
      },
      {
        path: 'property/vehicle',
        name: 'PropertyVehicle',
        component: () => import('@/views/property/VehicleList.vue'),
        meta: { title: '车辆管理' }
      },
      {
        path: 'property/decoration',
        name: 'PropertyDecoration',
        component: () => import('@/views/property/DecorationList.vue'),
        meta: { title: '装修管理' }
      },
      {
        path: 'service/repair',
        name: 'ServiceRepair',
        component: () => import('@/views/property/RepairList.vue'),
        meta: { title: '报修管理' }
      },
      {
        path: 'service/repair/detail/:id',
        name: 'ServiceRepairDetail',
        component: () => import('@/views/property/RepairDetail.vue'),
        meta: { title: '报修详情' }
      },
      {
        path: 'service/repair/process/:id',
        name: 'ServiceRepairProcess',
        component: () => import('@/views/property/RepairProcess.vue'),
        meta: { title: '处理报修' }
      },
      {
        path: 'service/complaint',
        name: 'ServiceComplaint',
        component: () => import('@/views/property/ComplaintList.vue'),
        meta: { title: '投诉管理' }
      },
      {
        path: 'service/notice',
        name: 'ServiceNotice',
        component: () => import('@/views/property/NoticeList.vue'),
        meta: { title: '公告管理' }
      },
      {
        path: 'service/notice/form/:id?',
        name: 'ServiceNoticeForm',
        component: () => import('@/views/property/NoticeForm.vue'),
        meta: { title: '编辑公告' }
      },
      {
        path: 'service/equipment',
        name: 'ServiceEquipment',
        component: () => import('@/views/property/EquipmentList.vue'),
        meta: { title: '设备管理' }
      },
      {
        path: 'service/equipment/detail/:id',
        name: 'ServiceEquipmentDetail',
        component: () => import('@/views/property/EquipmentDetail.vue'),
        meta: { title: '设备详情' }
      },
      {
        path: 'service/inspection',
        name: 'ServiceInspection',
        component: () => import('@/views/property/InspectionList.vue'),
        meta: { title: '巡检管理' }
      },
      {
        path: 'service/cleaning',
        name: 'ServiceCleaning',
        component: () => import('@/views/property/CleaningList.vue'),
        meta: { title: '保洁管理' }
      },
      {
        path: 'charge/fee-item',
        name: 'ChargeFeeItem',
        component: () => import('@/views/charge/FeeItemList.vue'),
        meta: { title: '费用项目管理' }
      },
      {
        path: 'charge/bill',
        name: 'ChargeBill',
        component: () => import('@/views/charge/BillList.vue'),
        meta: { title: '账单管理' }
      },
      {
        path: 'charge/bill/detail/:id',
        name: 'ChargeBillDetail',
        component: () => import('@/views/charge/BillDetail.vue'),
        meta: { title: '账单详情' }
      },
      {
        path: 'charge/payment',
        name: 'ChargePayment',
        component: () => import('@/views/charge/PaymentList.vue'),
        meta: { title: '缴费记录' }
      },
      {
        path: 'charge/deposit',
        name: 'ChargeDeposit',
        component: () => import('@/views/charge/DepositList.vue'),
        meta: { title: '押金管理' }
      },
      {
        path: 'charge/refund',
        name: 'ChargeRefund',
        component: () => import('@/views/charge/RefundList.vue'),
        meta: { title: '退款管理' }
      },
      {
        path: 'charge/report',
        name: 'ChargeReport',
        component: () => import('@/views/charge/FeeReport.vue'),
        meta: { title: '财务报表' }
      },
      {
        path: 'charge/income-report',
        name: 'ChargeIncomeReport',
        component: () => import('@/views/charge/IncomeReport.vue'),
        meta: { title: '收支报表' }
      },
      {
        path: 'guard/access-device',
        name: 'GuardAccessDevice',
        component: () => import('@/views/guard/AccessDeviceList.vue'),
        meta: { title: '门禁设备' }
      },
      {
        path: 'guard/access-permission',
        name: 'GuardAccessPermission',
        component: () => import('@/views/guard/AccessPermission.vue'),
        meta: { title: '门禁权限' }
      },
      {
        path: 'guard/access-log',
        name: 'GuardAccessLog',
        component: () => import('@/views/guard/AccessLogList.vue'),
        meta: { title: '通行记录' }
      },
      {
        path: 'guard/visitor',
        name: 'GuardVisitor',
        component: () => import('@/views/guard/VisitorList.vue'),
        meta: { title: '访客管理' }
      },
      {
        path: 'guard/visitor/detail/:id',
        name: 'GuardVisitorDetail',
        component: () => import('@/views/guard/VisitorDetail.vue'),
        meta: { title: '访客详情' }
      },
      {
        path: 'guard/camera',
        name: 'GuardCamera',
        component: () => import('@/views/guard/CameraList.vue'),
        meta: { title: '监控设备' }
      },
      {
        path: 'guard/monitor',
        name: 'GuardMonitor',
        component: () => import('@/views/guard/MonitorScreen.vue'),
        meta: { title: '监控大屏' }
      },
      {
        path: 'guard/alarm',
        name: 'GuardAlarm',
        component: () => import('@/views/guard/AlarmList.vue'),
        meta: { title: '报警管理' }
      },
      {
        path: 'report/owner',
        name: 'ReportOwner',
        component: () => import('@/views/report/OwnerReport.vue'),
        meta: { title: '业主分析' }
      },
      {
        path: 'report/service',
        name: 'ReportService',
        component: () => import('@/views/report/ServiceReport.vue'),
        meta: { title: '服务分析' }
      },
      {
        path: 'report/finance',
        name: 'ReportFinance',
        component: () => import('@/views/report/FinanceReport.vue'),
        meta: { title: '财务分析' }
      },
      {
        path: 'report/dashboard',
        name: 'ReportDashboard',
        component: () => import('@/views/report/Dashboard.vue'),
        meta: { title: '运营大屏' }
      },
      {
        path: 'report/custom',
        name: 'ReportCustom',
        component: () => import('@/views/report/CustomReport.vue'),
        meta: { title: '自定义报表' }
      },
      {
        path: 'life/product',
        name: 'LifeProduct',
        component: () => import('@/views/life/ProductList.vue'),
        meta: { title: '社区商品' }
      },
      {
        path: 'life/order',
        name: 'LifeOrder',
        component: () => import('@/views/life/OrderList.vue'),
        meta: { title: '商品订单' }
      },
      {
        path: 'life/service',
        name: 'LifeService',
        component: () => import('@/views/life/ServiceList.vue'),
        meta: { title: '便民服务' }
      },
      {
        path: 'life/service-order',
        name: 'LifeServiceOrder',
        component: () => import('@/views/life/ServiceOrderList.vue'),
        meta: { title: '服务预约' }
      },
      {
        path: 'life/express',
        name: 'LifeExpress',
        component: () => import('@/views/life/ExpressList.vue'),
        meta: { title: '快递管理' }
      },
      {
        path: 'life/activity',
        name: 'LifeActivity',
        component: () => import('@/views/life/ActivityList.vue'),
        meta: { title: '社区活动' }
      },
      {
        path: 'life/rental',
        name: 'LifeRental',
        component: () => import('@/views/life/RentalList.vue'),
        meta: { title: '房屋租售' }
      },
      {
        path: 'social/post',
        name: 'SocialPost',
        component: () => import('@/views/social/PostList.vue'),
        meta: { title: '社区帖子' }
      },
      {
        path: 'social/secondhand',
        name: 'SocialSecondhand',
        component: () => import('@/views/social/SecondhandList.vue'),
        meta: { title: '二手交易' }
      },
      {
        path: 'social/vote',
        name: 'SocialVote',
        component: () => import('@/views/social/VoteList.vue'),
        meta: { title: '社区投票' }
      },
      {
        path: 'social/phone',
        name: 'SocialPhone',
        component: () => import('@/views/social/PhoneList.vue'),
        meta: { title: '便民电话' }
      },
      {
        path: 'ai/qa',
        name: 'AiQa',
        component: () => import('@/views/ai/QaPage.vue'),
        meta: { title: '智能问答' }
      },
      {
        path: 'ai/knowledge',
        name: 'AiKnowledge',
        component: () => import('@/views/ai/KnowledgeList.vue'),
        meta: { title: '知识库管理' }
      }
    ]
  },
  {
    path: '/setup',
    name: 'Setup',
    component: () => import('@/views/setup/index.vue'),
    meta: { title: '系统初始化' }
  },
  { path: '/profile', redirect: to => ({ path: '/system/user-center', query: to.query, hash: to.hash }) },
  { path: '/password', redirect: to => ({ path: '/system/user-center', query: to.query, hash: '#password' }) },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/not-found/index.vue'),
    meta: { title: '404' }
  }
]

const businessRoutes = routes.find(r => r.name === 'Layout')!.children!
for (const record of businessRoutes) {
  if (!record.path.includes(':')) continue
  const parent = businessRoutes.filter(r => !r.path.includes(':') && record.path.startsWith(r.path + '/'))
    .sort((a, b) => b.path.length - a.path.length)[0]
  if (parent) record.meta = { ...record.meta, activeMenu: '/' + parent.path }
}

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to) { return to.hash ? { el: to.hash } : { top: 0 } },
  routes
})

router.beforeEach(async (to, from, next) => {
  // Initialization may have completed while the setup page was open.
  // Re-check before allowing navigation away from it.
  if (from.path === '/setup' && to.path !== '/setup') {
    setupChecked = false
  }

  // Check setup status before handling public routes such as the login page.
  if (!setupChecked) {
    setupChecked = true
    try {
      const res: any = await request.get('/api/v1/setup/check')
      needSetup = res?.needSetup === true
    } catch {
      // If check fails, don't block
    }
  }

  if (needSetup) {
    if (to.path === '/setup') {
      document.title = '系统初始化 - 智慧社区综合管理平台'
      next()
    } else {
      next('/setup')
    }
    return
  }

  // Do not allow returning to setup after initialization has completed.
  if (to.path === '/setup') {
    next('/login')
    return
  }

  // Always allow login pages after setup status has been checked.
  if (to.path === '/login' || to.path === '/community/login' || to.path === '/tenant/login') {
    const title = to.meta.title as string
    if (title) {
      const product = to.path === '/tenant/login' ? '租户服务中心' : to.path === '/community/login' ? '智慧社区' : '智慧社区综合管理平台'
      document.title = `${title} - ${product}`
    }
    next()
    return
  }

  // 登录守卫：除登录/初始化页外，其他页面需要 token
  const token = localStorage.getItem('token')
  if (!token) {
    const loginPath = to.path.startsWith('/tenant') ? '/tenant/login' : to.path.startsWith('/community') ? '/community/login' : '/login'
    next({ path: loginPath, query: { redirect: to.fullPath }, replace: true })
    return
  }

  const auth = useAuthStore()
  try {
    if (!auth.userInfo) {
      const profile = await getUserInfo()
      if (localStorage.getItem('token') !== token) { next(false); return }
      auth.setUserInfo({ ...profile, roles: [] })
    }
  } catch {
    const loginPath = to.path.startsWith('/tenant') ? '/tenant/login' : to.path.startsWith('/community') ? '/community/login' : '/login'
    next({ path: loginPath, query: { redirect: to.fullPath }, replace: true })
    return
  }
  const type = auth.userInfo?.userType
  if (![1, 3, 4, 5].includes(type || 0)) {
    auth.clearAuth()
    next('/login')
    return
  }
  const homePath = type === 1 ? '/community/home' : type === 3 ? '/tenant/home' : '/dashboard'
  const inOwnPortal = type === 1
    ? to.path.startsWith('/community/')
    : type === 3
      ? to.path.startsWith('/tenant/')
      : !to.path.startsWith('/community/') && !to.path.startsWith('/tenant/')
  if (!inOwnPortal) { next(homePath); return }

  const title = to.meta.title as string
  if (title) {
    const product = type === 1 ? '智慧社区' : type === 3 ? '租户服务中心' : '智慧社区综合管理平台'
    document.title = `${title} - ${product}`
  }
  next()
})

export default router
