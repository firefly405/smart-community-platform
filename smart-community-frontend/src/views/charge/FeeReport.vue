<template>
  <div class="page-container" v-loading="loading">
    <div class="report-actions">
      <el-form :inline="true">
        <el-form-item label="账期">
          <el-date-picker v-model="billPeriod" type="month" value-format="YYYY-MM" placeholder="全部账期" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData"><el-icon><Search /></el-icon>查询</el-button>
          <el-button @click="handleExport"><el-icon><Download /></el-icon>导出 CSV</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="report-grid">
      <div v-for="card in summaryCards" :key="card.label" class="stat-panel">
        <el-statistic :title="card.label" :value="card.value" :prefix="card.prefix" :suffix="card.suffix" />
      </div>
    </div>

    <div class="chart-grid fee-layout">
      <el-card shadow="never" class="chart-panel">
        <template #header><div class="chart-panel__header"><span>收缴金额结构</span><el-tag effect="plain">{{ billPeriod || '全部账期' }}</el-tag></div></template>
        <ReportChart :option="collectionOption" height="270px" aria-label="已收和待收金额环形图" />
      </el-card>
      <el-card shadow="never" class="arrears-card">
        <template #header>
          <div class="chart-panel__header"><span>欠费明细</span><span class="arrears-total">{{ arrearsBills.length }} 笔 / ¥{{ totalArrears.toLocaleString() }}</span></div>
        </template>
        <el-table :data="arrearsBills" height="270" stripe empty-text="暂无欠费记录">
          <el-table-column prop="houseNo" label="房号" min-width="90" />
          <el-table-column prop="ownerName" label="业主" min-width="90" />
          <el-table-column prop="billPeriod" label="账期" width="95" />
          <el-table-column label="欠费金额" width="110" align="right">
            <template #default="{ row }">¥{{ arrearsAmount(row).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ReportChart from '@/components/ReportChart.vue'
import { getArrearsReport, getFeeReport } from '@/api/fee'
import { downloadCsv } from '@/utils/exportCsv'

const loading = ref(false)
const billPeriod = ref('')
const summary = ref<any>({})
const arrearsBills = ref<any[]>([])
const totalArrears = ref(0)
const amount = (value: any) => Number(value || 0)
const arrearsAmount = (row: any) => Math.max(0, amount(row.payableAmount) - amount(row.paidAmount))

const summaryCards = computed(() => [
  { label: '账单总数', value: amount(summary.value.billCount), prefix: '', suffix: '张' },
  { label: '应收总额', value: amount(summary.value.totalAmount), prefix: '¥', suffix: '' },
  { label: '已收总额', value: amount(summary.value.totalPaid), prefix: '¥', suffix: '' },
  { label: '收缴率', value: amount(summary.value.collectionRate), prefix: '', suffix: '%' }
])
const collectionOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}<br/>¥{c}（{d}%）' },
  legend: { bottom: 0 },
  color: ['#287c6a', '#d99a2b'],
  series: [{ type: 'pie', radius: ['48%', '72%'], center: ['50%', '43%'], label: { formatter: '{b}\n{d}%' }, data: [
    { name: '已收金额', value: amount(summary.value.totalPaid) },
    { name: '待收金额', value: Math.max(0, amount(summary.value.totalUnpaid)) }
  ] }]
}))

async function loadData() {
  loading.value = true
  try {
    const [summaryData, arrearsData]: any[] = await Promise.all([
      getFeeReport({ billPeriod: billPeriod.value || undefined }), getArrearsReport({})
    ])
    summary.value = summaryData || {}
    arrearsBills.value = Array.isArray(arrearsData?.arrearsBills) ? arrearsData.arrearsBills : []
    totalArrears.value = amount(arrearsData?.totalArrears)
  } finally { loading.value = false }
}

function handleExport() {
  const rows = arrearsBills.value.map(row => ({ ...row, arrearsAmount: arrearsAmount(row).toFixed(2) }))
  if (!rows.length) {
    ElMessage.info('当前没有可导出的欠费明细')
    return
  }
  downloadCsv(`欠费明细-${billPeriod.value || '全部账期'}.csv`, [
    { label: '账单号', value: 'billNo' }, { label: '业主', value: 'ownerName' },
    { label: '房号', value: 'houseNo' }, { label: '账期', value: 'billPeriod' },
    { label: '费用项目', value: 'itemName' }, { label: '应付金额', value: 'payableAmount' },
    { label: '已付金额', value: 'paidAmount' }, { label: '欠费金额', value: 'arrearsAmount' }
  ], rows)
  ElMessage.success('报表已导出')
}

onMounted(loadData)
</script>

<style scoped>
.report-actions { margin-bottom: 14px; padding: 14px 16px 0; border: 1px solid #e2e7ec; border-radius: 6px; background: #fff; }
.fee-layout { grid-template-columns: minmax(300px, .8fr) minmax(0, 1.2fr); }
.arrears-total { color: #b43c3c; font-size: 13px; font-weight: 500; }
@media (max-width: 900px) { .fee-layout { grid-template-columns: 1fr; } }
</style>
