<template>
  <section v-if="module" class="module-heading" :style="headingStyle">
    <div class="heading-row">
      <div class="heading-copy">
        <span class="heading-icon" aria-hidden="true">
          <el-icon><component :is="module.icon" /></el-icon>
        </span>
        <div>
          <span class="eyebrow">{{ module.label }}</span>
          <h1>{{ route.meta.title }}</h1>
          <p>{{ currentPage?.summary || module.summary }}</p>
        </div>
      </div>
      <el-tag effect="plain" round>{{ module.badge }}</el-tag>
    </div>

    <nav v-if="module.pages.length" class="section-nav" :aria-label="`${module.label}功能`">
      <router-link
        v-for="item in module.pages"
        :key="item.path"
        :to="item.path"
        :class="{ active: isActive(item.path) }"
      >
        {{ item.label }}
      </router-link>
    </nav>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  ChatDotRound, ChatLineSquare, DataAnalysis, HomeFilled, Lock, Money,
  ShoppingCart, Tools
} from '@element-plus/icons-vue'

interface PageItem { path: string; label: string; summary: string }
interface ModuleItem {
  label: string
  summary: string
  badge: string
  color: string
  tint: string
  border: string
  icon: any
  pages: PageItem[]
}

const route = useRoute()

const modules: Record<string, ModuleItem> = {
  dashboard: {
    label: '工作台', summary: '集中查看社区运营指标、服务动态和待办事项', badge: '实时概览',
    color: '#176b5b', tint: '#e9f5f2', border: '#cfe8e1', icon: HomeFilled, pages: []
  },
  service: {
    label: '物业服务', summary: '覆盖报修、投诉、公告、设备、巡检和保洁全流程', badge: '服务闭环',
    color: '#176b5b', tint: '#e9f5f2', border: '#cfe8e1', icon: Tools,
    pages: [
      ['/service/repair', '报修管理', '受理、派单并跟踪报修处理进度'],
      ['/service/complaint', '投诉管理', '受理住户诉求并记录处理结果'],
      ['/service/notice', '公告管理', '发布和维护社区通知公告'],
      ['/service/equipment', '设备管理', '维护公共设施档案与运行状态'],
      ['/service/inspection', '巡检管理', '制定巡检任务并跟踪异常处理'],
      ['/service/cleaning', '保洁管理', '安排保洁任务并记录完成情况'],
      ['/service/maintain', '设备维保', '跟踪设备保养和维修记录']
    ].map(([path, label, summary]) => ({ path, label, summary }))
  },
  charge: {
    label: '费用与财务', summary: '管理收费项目、账单、缴费、押金和退款', badge: '账务清晰',
    color: '#935f14', tint: '#fff7e8', border: '#f3dfb8', icon: Money,
    pages: [
      ['/charge/fee-item', '费用项目', '维护计费项目、周期和收费标准'],
      ['/charge/bill', '账单管理', '生成并跟踪住户应收账单'],
      ['/charge/payment', '缴费记录', '查询支付流水与入账信息'],
      ['/charge/deposit', '押金管理', '管理装修等业务押金收退'],
      ['/charge/refund', '退款管理', '审核和跟踪退款申请'],
      ['/charge/report', '财务报表', '汇总收缴情况和欠费明细'],
      ['/charge/income-report', '收支报表', '分析收入、欠费和收缴表现']
    ].map(([path, label, summary]) => ({ path, label, summary }))
  },
  guard: {
    label: '安防门禁', summary: '统一管理门禁、访客、监控和报警事件', badge: '安全运行',
    color: '#a23b3b', tint: '#fff0f0', border: '#f3d2d2', icon: Lock,
    pages: [
      ['/guard/access-device', '门禁设备', '维护门禁设备档案和在线状态'],
      ['/guard/access-permission', '门禁权限', '配置人员通行范围和有效期'],
      ['/guard/access-log', '通行记录', '查询人员和车辆通行流水'],
      ['/guard/visitor', '访客管理', '登记访客并跟踪进出状态'],
      ['/guard/camera', '监控设备', '维护摄像头点位与在线状态'],
      ['/guard/monitor', '监控大屏', '集中查看监控画面和设备状态'],
      ['/guard/alarm', '报警管理', '受理并闭环处置报警事件']
    ].map(([path, label, summary]) => ({ path, label, summary }))
  },
  life: {
    label: '社区生活', summary: '连接社区商品、便民服务、活动与租售信息', badge: '便民运营',
    color: '#2f6b3e', tint: '#edf7ef', border: '#d2e9d7', icon: ShoppingCart,
    pages: [
      ['/life/product', '社区商品', '维护商品资料、价格和库存'],
      ['/life/order', '商品订单', '处理下单、支付、发货和完成流程'],
      ['/life/service', '便民服务', '维护可预约的社区服务项目'],
      ['/life/service-order', '服务预约', '跟踪服务预约和履约状态'],
      ['/life/express', '快递管理', '登记快递到件与领取情况'],
      ['/life/activity', '社区活动', '发布活动并管理报名信息'],
      ['/life/rental', '房屋租售', '发布和维护社区租售信息']
    ].map(([path, label, summary]) => ({ path, label, summary }))
  },
  social: {
    label: '社区互动', summary: '管理社区内容、邻里交易、投票和便民通讯录', badge: '邻里共建',
    color: '#8a4b17', tint: '#fff4e8', border: '#f0d7bf', icon: ChatDotRound,
    pages: [
      ['/social/post', '社区帖子', '审核和维护居民交流内容'],
      ['/social/secondhand', '二手交易', '管理邻里闲置物品信息'],
      ['/social/vote', '社区投票', '创建投票并查看参与结果'],
      ['/social/phone', '便民电话', '维护常用公共服务联系方式']
    ].map(([path, label, summary]) => ({ path, label, summary }))
  },
  report: {
    label: '数据分析', summary: '用真实业务数据观察社区运行和服务质量', badge: '数据洞察',
    color: '#265f8f', tint: '#edf5fb', border: '#d4e5f3', icon: DataAnalysis,
    pages: [
      ['/report/owner', '业主分析', '查看住户规模和房屋入住情况'],
      ['/report/service', '服务分析', '分析报修效率与投诉处理情况'],
      ['/report/finance', '财务分析', '分析收缴率、收入结构和欠费趋势'],
      ['/report/dashboard', '运营大屏', '集中展示关键运营和安全指标'],
      ['/report/custom', '自定义报表', '按业务主题组合和导出统计指标']
    ].map(([path, label, summary]) => ({ path, label, summary }))
  },
  ai: {
    label: 'AI 服务', summary: '使用社区知识库辅助工作人员快速检索和答疑', badge: '智能辅助',
    color: '#5f4a8b', tint: '#f3f0fa', border: '#dfd7ee', icon: ChatLineSquare,
    pages: [
      ['/ai/qa', '智能问答', '基于社区知识快速获取业务答案'],
      ['/ai/knowledge', '知识库管理', '维护问答所需的社区知识内容']
    ].map(([path, label, summary]) => ({ path, label, summary }))
  }
}

const moduleKey = computed(() => route.path === '/dashboard' ? 'dashboard' : route.path.split('/')[1])
const module = computed(() => modules[moduleKey.value])
const currentPage = computed(() => module.value?.pages.find(item => isActive(item.path)))
const headingStyle = computed(() => ({
  '--module-color': module.value?.color,
  '--module-tint': module.value?.tint,
  '--module-border': module.value?.border
}))

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}
</script>

<style scoped>
.module-heading { margin-bottom: 18px; }
.heading-row { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 16px; }
.heading-copy { display: flex; align-items: center; min-width: 0; gap: 13px; }
.heading-icon { display: grid; flex: 0 0 42px; width: 42px; height: 42px; place-items: center; color: var(--module-color); background: var(--module-tint); border: 1px solid var(--module-border); border-radius: 7px; font-size: 21px; }
.eyebrow { display: block; margin-bottom: 2px; color: #7b8794; font-size: 12px; }
h1 { margin: 0; color: #1f2937; font-size: 22px; line-height: 1.3; letter-spacing: 0; }
p { margin: 3px 0 0; color: #667085; font-size: 13px; }
.el-tag { color: var(--module-color); border-color: var(--module-border); background: var(--module-tint); }
.section-nav { display: flex; overflow-x: auto; border-bottom: 1px solid #dfe4ea; scrollbar-width: none; }
.section-nav::-webkit-scrollbar { display: none; }
.section-nav a { display: inline-flex; flex: 0 0 auto; padding: 10px 13px; color: #667085; border-bottom: 2px solid transparent; font-size: 13px; text-decoration: none; }
.section-nav a:hover { color: var(--module-color); background: var(--module-tint); }
.section-nav a.active { color: var(--module-color); border-bottom-color: var(--module-color); font-weight: 600; }
@media (max-width: 720px) {
  .heading-row { align-items: flex-start; }
  .heading-row > .el-tag { display: none; }
  .section-nav a { padding: 9px 11px; }
}
</style>
