<template>
  <div class="page-container" v-loading="loading">
    <div class="report-grid">
      <div v-for="card in cards" :key="card.label" class="stat-panel">
        <el-statistic :title="card.label" :value="card.value" :suffix="card.suffix" />
      </div>
    </div>

    <div class="chart-grid">
      <el-card shadow="never" class="chart-panel">
        <template #header><div class="chart-panel__header"><span>近 7 天报修趋势</span><el-tag effect="plain">按创建时间</el-tag></div></template>
        <ReportChart :option="repairOption" aria-label="近七天报修数量趋势图" />
      </el-card>
      <el-card shadow="never" class="chart-panel">
        <template #header><div class="chart-panel__header"><span>投诉处理状态</span><span class="chart-note">共 {{ complaint.total }} 单</span></div></template>
        <ReportChart :option="complaintOption" aria-label="投诉处理状态环形图" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import ReportChart from '@/components/ReportChart.vue'
import { getComplaintStats, getRepairTrend, getServiceStats } from '@/api/report'

const loading = ref(false)
const stats = ref({ totalRepairs: 0, completedRepairs: 0, totalComplaints: 0, todayRepairs: 0, completionRate: 0 })
const complaint = ref({ total: 0, pending: 0, handled: 0 })
const repairTrend = ref<any[]>([])
const cards = computed(() => [
  { label: '报修总数', value: stats.value.totalRepairs, suffix: '单' },
  { label: '已完成', value: stats.value.completedRepairs, suffix: '单' },
  { label: '报修完成率', value: stats.value.completionRate, suffix: '%' },
  { label: '今日报修', value: stats.value.todayRepairs, suffix: '单' }
])

const repairOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 24, right: 24, top: 24, bottom: 24, containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: repairTrend.value.map(item => item.day), axisTick: { show: false } },
  yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#edf0f2' } } },
  color: ['#287c6a'],
  series: [{ name: '报修数', type: 'line', smooth: true, symbolSize: 7, data: repairTrend.value.map(item => Number(item.count || 0)), areaStyle: { color: 'rgba(40,124,106,.12)' } }]
}))

const complaintOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 单（{d}%）' },
  legend: { bottom: 4 },
  color: ['#d99a2b', '#287c6a', '#cbd2d9'],
  series: [{
    type: 'pie', radius: ['48%', '72%'], center: ['50%', '44%'],
    label: { formatter: '{b}\n{c} 单' },
    data: [
      { name: '待处理', value: complaint.value.pending },
      { name: '已处理', value: complaint.value.handled },
      { name: '其他状态', value: Math.max(0, complaint.value.total - complaint.value.pending - complaint.value.handled) }
    ]
  }]
}))

onMounted(async () => {
  loading.value = true
  try {
    const [serviceData, complaintData, trendData]: any[] = await Promise.all([getServiceStats(), getComplaintStats(), getRepairTrend()])
    stats.value = { ...stats.value, ...serviceData }
    complaint.value = { ...complaint.value, ...complaintData }
    repairTrend.value = Array.isArray(trendData) ? trendData : []
  } finally { loading.value = false }
})
</script>

<style scoped>
.chart-note { color: #7b8794; font-size: 12px; font-weight: 400; }
</style>
