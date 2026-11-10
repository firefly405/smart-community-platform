<template>
  <div class="page-container" v-loading="loading">
    <div class="report-actions">
      <span>近 6 个月收缴与欠费趋势</span>
      <el-button @click="handleExport"><el-icon><Download /></el-icon>导出 CSV</el-button>
    </div>
    <div class="report-grid">
      <div v-for="card in cards" :key="card.label" class="stat-panel">
        <el-statistic :title="card.label" :value="card.value" :prefix="card.prefix" :suffix="card.suffix" />
      </div>
    </div>
    <el-card shadow="never" class="chart-panel trend-card">
      <template #header><div class="chart-panel__header"><span>月度收缴表现</span><el-tag effect="plain">真实账单数据</el-tag></div></template>
      <ReportChart :option="trendOption" height="340px" aria-label="近六个月收费率和欠费金额图" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ReportChart from '@/components/ReportChart.vue'
import { getArrearsTrend, getFeeRateTrend, getFinanceStats } from '@/api/report'
import { downloadCsv } from '@/utils/exportCsv'

const loading = ref(false)
const finance = ref<any>({})
const rates = ref<any[]>([])
const arrears = ref<any[]>([])
const amount = (value: any) => Number(value || 0)
const cards = computed(() => [
  { label: '本月应收', value: amount(finance.value.totalAmount), prefix: '¥', suffix: '' },
  { label: '本月实收', value: amount(finance.value.totalPaid), prefix: '¥', suffix: '' },
  { label: '本月收缴率', value: amount(finance.value.collectionRate), prefix: '', suffix: '%' },
  { label: '本月账单', value: amount(finance.value.billCount), prefix: '', suffix: '笔' }
])
const rows = computed(() => {
  const arrearsMap = new Map(arrears.value.map(item => [item.period, amount(item.amount)]))
  return rates.value.map(item => ({ period: item.period, rate: amount(item.rate), arrears: arrearsMap.get(item.period) || 0 }))
})
const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' }, legend: { bottom: 0 },
  grid: { left: 24, right: 30, top: 24, bottom: 44, containLabel: true },
  xAxis: { type: 'category', data: rows.value.map(item => item.period), axisTick: { show: false } },
  yAxis: [
    { type: 'value', min: 0, max: 100, axisLabel: { formatter: '{value}%' }, splitLine: { lineStyle: { color: '#edf0f2' } } },
    { type: 'value', axisLabel: { formatter: '¥{value}' }, splitLine: { show: false } }
  ],
  series: [
    { name: '收缴率', type: 'line', smooth: true, data: rows.value.map(item => item.rate), itemStyle: { color: '#287c6a' } },
    { name: '欠费金额', type: 'bar', yAxisIndex: 1, barMaxWidth: 34, data: rows.value.map(item => item.arrears), itemStyle: { color: '#d99a2b', borderRadius: [3, 3, 0, 0] } }
  ]
}))

async function loadData() {
  loading.value = true
  try {
    const [financeData, rateData, arrearsData]: any[] = await Promise.all([getFinanceStats(), getFeeRateTrend(), getArrearsTrend()])
    finance.value = financeData || {}
    rates.value = Array.isArray(rateData) ? rateData : []
    arrears.value = Array.isArray(arrearsData) ? arrearsData : []
  } finally { loading.value = false }
}
function handleExport() {
  downloadCsv('近6个月收缴趋势.csv', [
    { label: '月份', value: 'period' }, { label: '收缴率(%)', value: 'rate' }, { label: '欠费金额(元)', value: 'arrears' }
  ], rows.value)
  ElMessage.success('报表已导出')
}
onMounted(loadData)
</script>

<style scoped>
.report-actions { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-bottom: 14px; color: #344054; font-weight: 600; }
.trend-card { margin-top: 16px; }
</style>
