<template>
  <div class="page-container" v-loading="loading">
    <div class="report-grid">
      <div v-for="card in cards" :key="card.label" class="stat-panel">
        <el-statistic :title="card.label" :value="card.value" :suffix="card.suffix" />
      </div>
    </div>

    <div class="chart-grid">
      <el-card shadow="never" class="chart-panel">
        <template #header><div class="chart-panel__header"><span>房屋入住结构</span><el-tag effect="plain">实时数据</el-tag></div></template>
        <ReportChart :option="occupancyOption" aria-label="房屋入住与空置数量环形图" />
      </el-card>
      <el-card shadow="never" class="chart-panel">
        <template #header><div class="chart-panel__header"><span>社区资源概览</span><span class="chart-note">当前社区</span></div></template>
        <ReportChart :option="resourceOption" aria-label="业主和房屋数量柱状图" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import ReportChart from '@/components/ReportChart.vue'
import { getOwnerStats } from '@/api/report'

const loading = ref(false)
const stats = ref({ totalOwners: 0, totalHouses: 0, occupiedHouses: 0, occupancyRate: 0 })
const cards = computed(() => [
  { label: '业主总数', value: stats.value.totalOwners, suffix: '人' },
  { label: '房屋总数', value: stats.value.totalHouses, suffix: '套' },
  { label: '入住房屋', value: stats.value.occupiedHouses, suffix: '套' },
  { label: '入住率', value: stats.value.occupancyRate, suffix: '%' }
])

const occupancyOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 套（{d}%）' },
  legend: { bottom: 4 },
  color: ['#287c6a', '#d6dde3'],
  series: [{
    type: 'pie', radius: ['48%', '72%'], center: ['50%', '44%'],
    label: { formatter: '{b}\n{c} 套' },
    data: [
      { name: '已入住', value: stats.value.occupiedHouses },
      { name: '待入住', value: Math.max(0, stats.value.totalHouses - stats.value.occupiedHouses) }
    ]
  }]
}))

const resourceOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 24, right: 24, top: 24, bottom: 24, containLabel: true },
  xAxis: { type: 'category', data: ['业主', '房屋', '入住房屋'], axisTick: { show: false } },
  yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#edf0f2' } } },
  color: ['#376f9f'],
  series: [{ type: 'bar', barMaxWidth: 48, data: [stats.value.totalOwners, stats.value.totalHouses, stats.value.occupiedHouses], itemStyle: { borderRadius: [4, 4, 0, 0] } }]
}))

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await getOwnerStats()
    stats.value = {
      totalOwners: Number(res.totalOwners || 0),
      totalHouses: Number(res.totalHouses || 0),
      occupiedHouses: Number(res.occupiedHouses || 0),
      occupancyRate: Number(res.occupancyRate || 0)
    }
  } finally { loading.value = false }
})
</script>

<style scoped>
.chart-note { color: #7b8794; font-size: 12px; font-weight: 400; }
</style>
