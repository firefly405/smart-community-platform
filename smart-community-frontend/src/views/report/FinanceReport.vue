<template>
  <div class="page-container" v-loading="loading">
    <div class="report-grid">
      <div v-for="card in cards" :key="card.label" class="stat-panel">
        <el-statistic :title="card.label" :value="card.value" :prefix="card.prefix" :suffix="card.suffix" />
      </div>
    </div>

    <div class="chart-grid">
      <el-card shadow="never" class="chart-panel">
        <template #header><div class="chart-panel__header"><span>本月收入结构</span><el-tag effect="plain">按已收金额</el-tag></div></template>
        <ReportChart :option="incomeOption" aria-label="本月各费用类型收入结构图" />
      </el-card>
      <el-card shadow="never" class="chart-panel">
        <template #header><div class="chart-panel__header"><span>近 6 个月收缴表现</span><span class="chart-note">收缴率 / 欠费金额</span></div></template>
        <ReportChart :option="trendOption" aria-label="近六个月收费率与欠费金额趋势图" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import ReportChart from '@/components/ReportChart.vue'
import { getArrearsTrend, getFeeRateTrend, getFinanceStats, getIncomeStructure } from '@/api/report'

const loading = ref(false)
const finance = ref({ totalAmount: 0, totalPaid: 0, collectionRate: 0, billCount: 0 })
const incomeData = ref<any[]>([])
const rateTrend = ref<any[]>([])
const arrearsTrend = ref<any[]>([])
const money = (value: any) => Number(value || 0)

const cards = computed(() => [
  { label: '本月应收', value: money(finance.value.totalAmount), prefix: '¥', suffix: '' },
  { label: '本月实收', value: money(finance.value.totalPaid), prefix: '¥', suffix: '' },
  { label: '收缴率', value: money(finance.value.collectionRate), prefix: '', suffix: '%' },
  { label: '账单数', value: money(finance.value.billCount), prefix: '', suffix: '笔' }
])

const incomeOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}<br/>¥{c}（{d}%）' },
  legend: { bottom: 4 },
  color: ['#287c6a', '#376f9f', '#d99a2b', '#8c6b9d'],
  series: [{
    type: 'pie', radius: ['45%', '70%'], center: ['50%', '44%'],
    label: { formatter: '{b}\n{d}%' },
    data: incomeData.value.map(item => ({ name: item.category, value: money(item.amount) }))
  }]
}))

const trendOption = computed(() => {
  const months = rateTrend.value.map(item => item.period)
  const arrears = new Map(arrearsTrend.value.map(item => [item.period, money(item.amount)]))
  return {
    tooltip: { trigger: 'axis' },
    legend: { bottom: 2, data: ['收缴率', '欠费金额'] },
    grid: { left: 20, right: 26, top: 24, bottom: 46, containLabel: true },
    xAxis: { type: 'category', data: months, axisTick: { show: false } },
    yAxis: [
      { type: 'value', name: '收缴率', min: 0, max: 100, axisLabel: { formatter: '{value}%' }, splitLine: { lineStyle: { color: '#edf0f2' } } },
      { type: 'value', name: '欠费', axisLabel: { formatter: '¥{value}' }, splitLine: { show: false } }
    ],
    series: [
      { name: '收缴率', type: 'line', smooth: true, data: rateTrend.value.map(item => money(item.rate)), itemStyle: { color: '#287c6a' } },
      { name: '欠费金额', type: 'bar', yAxisIndex: 1, barMaxWidth: 28, data: months.map(month => arrears.get(month) || 0), itemStyle: { color: '#d99a2b', borderRadius: [3, 3, 0, 0] } }
    ]
  }
})

onMounted(async () => {
  loading.value = true
  try {
    const [financeData, income, rates, arrears]: any[] = await Promise.all([
      getFinanceStats(), getIncomeStructure(), getFeeRateTrend(), getArrearsTrend()
    ])
    finance.value = { ...finance.value, ...financeData }
    incomeData.value = Array.isArray(income) ? income : []
    rateTrend.value = Array.isArray(rates) ? rates : []
    arrearsTrend.value = Array.isArray(arrears) ? arrears : []
  } finally { loading.value = false }
})
</script>

<style scoped>
.chart-note { color: #7b8794; font-size: 12px; font-weight: 400; }
</style>
