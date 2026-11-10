<template>
  <div class="operations-dashboard" v-loading="loading">
    <div class="screen-toolbar">
      <div><strong>运营实时态势</strong><span>{{ currentTime }}</span></div>
      <el-button :loading="refreshing" @click="refresh"><el-icon><Refresh /></el-icon>刷新数据</el-button>
    </div>

    <div class="report-grid">
      <div v-for="card in topCards" :key="card.label" class="stat-panel operation-stat">
        <span>{{ card.label }}</span><strong>{{ card.value }}<small>{{ card.unit }}</small></strong>
      </div>
    </div>

    <div class="live-strip">
      <div v-for="item in liveCards" :key="item.label">
        <span class="status-dot" :class="item.tone" />
        <span>{{ item.label }}</span><strong>{{ item.value }}</strong><small>{{ item.unit }}</small>
      </div>
    </div>

    <div class="chart-grid">
      <el-card shadow="never" class="chart-panel">
        <template #header><div class="chart-panel__header"><span>近 7 天报修趋势</span><el-tag effect="plain">服务动态</el-tag></div></template>
        <ReportChart :option="repairOption" height="280px" aria-label="近七天报修趋势图" />
      </el-card>
      <el-card shadow="never" class="chart-panel">
        <template #header><div class="chart-panel__header"><span>本月收入结构</span><el-tag effect="plain" type="warning">财务动态</el-tag></div></template>
        <ReportChart :option="incomeOption" height="280px" aria-label="本月收入结构环形图" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import ReportChart from '@/components/ReportChart.vue'
import { getDashboardData, getDashboardRealtime, getIncomeStructure, getRepairTrend } from '@/api/report'

const loading = ref(false)
const refreshing = ref(false)
const currentTime = ref('')
const base = ref<any>({})
const realtime = ref<any>({})
const repairTrend = ref<any[]>([])
const incomeData = ref<any[]>([])
let clockTimer: ReturnType<typeof setInterval> | undefined
let refreshTimer: ReturnType<typeof setInterval> | undefined

const occupancyRate = computed(() => base.value.totalHouses
  ? Math.round(Number(base.value.occupiedHouses || 0) * 100 / Number(base.value.totalHouses))
  : 0)
const topCards = computed(() => [
  { label: '业主总数', value: Number(base.value.totalOwners || 0), unit: '人' },
  { label: '房屋总数', value: Number(base.value.totalHouses || 0), unit: '套' },
  { label: '入住率', value: occupancyRate.value, unit: '%' },
  { label: '收缴率', value: Number(base.value.collectionRate || 0), unit: '%' }
])
const liveCards = computed(() => [
  { label: '今日报修', value: Number(base.value.todayRepairs || 0), unit: '单', tone: 'blue' },
  { label: '今日访客', value: Number(realtime.value.todayVisits ?? base.value.todayVisitors ?? 0), unit: '人', tone: 'green' },
  { label: '待处理报警', value: Number(realtime.value.activeAlarms || 0), unit: '起', tone: 'red' },
  { label: '在线摄像头', value: Number(realtime.value.onlineCameras || 0), unit: '个', tone: 'green' },
  { label: '在线门禁', value: Number(realtime.value.onlineDevices || 0), unit: '台', tone: 'blue' },
  { label: '待缴费用', value: `¥${Number(base.value.unpaidAmount || 0).toLocaleString()}`, unit: '', tone: 'amber' }
])

const repairOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 22, right: 22, top: 20, bottom: 22, containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: repairTrend.value.map(item => item.day), axisTick: { show: false } },
  yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#edf0f2' } } },
  color: ['#376f9f'],
  series: [{ name: '报修数', type: 'line', smooth: true, symbolSize: 7, data: repairTrend.value.map(item => Number(item.count || 0)), areaStyle: { color: 'rgba(55,111,159,.12)' } }]
}))
const incomeOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}<br/>¥{c}（{d}%）' },
  legend: { bottom: 0 },
  color: ['#287c6a', '#376f9f', '#d99a2b', '#8c6b9d'],
  series: [{ type: 'pie', radius: ['44%', '68%'], center: ['50%', '43%'], label: { formatter: '{b}\n{d}%' }, data: incomeData.value.map(item => ({ name: item.category, value: Number(item.amount || 0) })) }]
}))

function updateTime() { currentTime.value = new Date().toLocaleString('zh-CN', { hour12: false }) }
async function refresh() {
  refreshing.value = true
  try {
    const [dashboardData, realtimeData, repairs, income]: any[] = await Promise.all([
      getDashboardData(), getDashboardRealtime(), getRepairTrend(), getIncomeStructure()
    ])
    base.value = dashboardData || {}
    realtime.value = realtimeData || {}
    repairTrend.value = Array.isArray(repairs) ? repairs : []
    incomeData.value = Array.isArray(income) ? income : []
  } finally { refreshing.value = false }
}

onMounted(async () => {
  loading.value = true
  updateTime()
  await refresh()
  loading.value = false
  clockTimer = setInterval(updateTime, 1000)
  refreshTimer = setInterval(refresh, 30000)
})
onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.screen-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 14px; }
.screen-toolbar > div { display: flex; align-items: baseline; gap: 12px; }
.screen-toolbar strong { color: #273444; font-size: 16px; }
.screen-toolbar span { color: #7b8794; font-size: 13px; }
.operation-stat span { display: block; margin-bottom: 8px; color: #667085; font-size: 13px; }
.operation-stat strong { color: #1f2937; font-size: 28px; font-weight: 650; }
.operation-stat small { margin-left: 4px; color: #7b8794; font-size: 13px; font-weight: 400; }
.live-strip { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 1px; overflow: hidden; margin-top: 14px; border: 1px solid #e2e7ec; border-radius: 6px; background: #e2e7ec; }
.live-strip > div { display: flex; align-items: center; gap: 7px; min-width: 0; padding: 13px 14px; background: #fff; }
.live-strip span:not(.status-dot) { overflow: hidden; color: #667085; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.live-strip strong { margin-left: auto; color: #273444; font-size: 17px; }
.live-strip small { color: #7b8794; }
.status-dot { flex: 0 0 7px; width: 7px; height: 7px; border-radius: 50%; background: #376f9f; }
.status-dot.green { background: #287c6a; }.status-dot.red { background: #bd3f3f; }.status-dot.amber { background: #d99a2b; }
@media (max-width: 1100px) { .live-strip { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 620px) {
  .screen-toolbar > div { display: block; }.screen-toolbar span { display: block; margin-top: 3px; }
  .live-strip { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
