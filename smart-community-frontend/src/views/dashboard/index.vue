<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon owner">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalOwners ?? '--' }}</div>
            <div class="stat-label">业主总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon repair">
            <el-icon><Tools /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.todayRepairs ?? '--' }}</div>
            <div class="stat-label">今日报修</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon fee">
            <el-icon><Money /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ formatMoney(stats.unpaidAmount) }}</div>
            <div class="stat-label">待缴费用</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon visitor">
            <el-icon><UserFilled /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.todayVisitors ?? '--' }}</div>
            <div class="stat-label">今日访客</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-20">
      <el-col :xs="24" :lg="12">
        <el-card header="报修趋势（近7天）">
          <div ref="repairChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card header="本月收入构成">
          <div ref="feeChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-20">
      <el-col :xs="24" :lg="12">
        <el-card header="最新报修工单">
          <el-table :data="latestRepairs" style="width: 100%">
            <el-table-column prop="repairNo" label="工单号" width="140" />
            <el-table-column prop="title" label="报修标题" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card header="物业概况">
          <div class="overview-list">
            <div class="overview-item">
              <span class="overview-label">房屋总数</span>
              <span class="overview-value">{{ stats.totalHouses ?? '--' }} 套</span>
            </div>
            <div class="overview-item">
              <span class="overview-label">物业费收缴率</span>
              <span class="overview-value">
                <el-progress :percentage="stats.collectionRate ?? 0" :stroke-width="16" :show-text="true" />
              </span>
            </div>
            <div class="overview-item">
              <span class="overview-label">今日报修</span>
              <span class="overview-value">{{ stats.todayRepairs ?? '--' }}</span>
            </div>
            <div class="overview-item">
              <span class="overview-label">今日访客</span>
              <span class="overview-value">{{ stats.todayVisitors ?? '--' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboardData, getRepairTrend, getIncomeStructure, getLatestRepairs } from '@/api/report'

interface DashboardStats {
  totalOwners?: number
  totalHouses?: number
  todayRepairs?: number
  todayVisitors?: number
  unpaidAmount?: number
  collectionRate?: number
}

const repairChartRef = ref<HTMLElement>()
const feeChartRef = ref<HTMLElement>()
const stats = ref<DashboardStats>({})
const latestRepairs = ref<any[]>([])
let repairChart: echarts.ECharts | undefined
let feeChart: echarts.ECharts | undefined
let resizeObserver: ResizeObserver | undefined

function formatMoney(val: any): string {
  if (val === null || val === undefined) return '--'
  const n = Number(val)
  if (isNaN(n)) return '--'
  if (n >= 10000) return '¥' + (n / 10000).toFixed(1) + '万'
  return '¥' + n.toFixed(0)
}

const statusTexts = ['', '已派单', '处理中', '已完成', '已取消']
const statusTypes = ['', 'warning', 'primary', 'success', 'info'] as const

function getStatusText(s: number) { return statusTexts[s] || '' }
function getStatusType(s: number): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  return (statusTypes[s] || 'info') as 'primary' | 'success' | 'warning' | 'info' | 'danger'
}

async function loadData() {
  try {
    const ds = await getDashboardData()
    stats.value = ds as DashboardStats
  } catch { /* ignore */ }

  try {
    const res = await getLatestRepairs(5)
    latestRepairs.value = res.records || []
  } catch { /* ignore */ }
}

function initRepairChart(data: { day: string; count: number }[]) {
  if (!repairChartRef.value) return
  repairChart ||= echarts.init(repairChartRef.value)
  repairChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => d.day) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      name: '报修数', type: 'line', smooth: true,
      data: data.map(d => d.count),
      areaStyle: { color: 'rgba(64, 158, 255, 0.15)' },
      lineStyle: { color: '#409eff', width: 2 },
      itemStyle: { color: '#409eff' }
    }]
  })
}

function initFeeChart(data: { category: string; amount: number; percent: number }[]) {
  if (!feeChartRef.value) return
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#909399']
  feeChart ||= echarts.init(feeChartRef.value)
  feeChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: '5%' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      label: { show: true, formatter: '{b}\n{d}%' },
      data: data.map((d, i) => ({
        value: d.amount, name: d.category,
        itemStyle: { color: colors[i % colors.length], borderRadius: 6, borderColor: '#fff', borderWidth: 2 }
      }))
    }]
  })
}

onMounted(async () => {
  await loadData()

  try {
    const trend = await getRepairTrend()
    if (trend) nextTick(() => initRepairChart(trend))
  } catch { /* ignore */ }

  try {
    const income = await getIncomeStructure()
    if (income) nextTick(() => initFeeChart(income as any))
  } catch { /* ignore */ }

  resizeObserver = new ResizeObserver(() => {
    repairChart?.resize()
    feeChart?.resize()
  })
  if (repairChartRef.value) resizeObserver.observe(repairChartRef.value)
  if (feeChartRef.value) resizeObserver.observe(feeChartRef.value)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  repairChart?.dispose()
  feeChart?.dispose()
})
</script>

<style lang="scss" scoped>
.dashboard { padding: 0; }

.stat-card {
  display: flex; align-items: center; min-height: 104px; margin-bottom: 14px; padding: 16px;
  .stat-icon {
    width: 60px; height: 60px; border-radius: 50%;
    display: flex; align-items: center; justify-content: center;
    font-size: 28px; color: #fff; margin-right: 20px;
    &.owner { background: #376f9f; }
    &.repair { background: #b84a4a; }
    &.fee { background: #c18424; }
    &.visitor { background: #287c6a; }
  }
  .stat-content {
    .stat-value { font-size: 28px; font-weight: bold; color: #333; }
    .stat-label { font-size: 14px; color: #999; margin-top: 5px; }
  }
}

.overview-list {
  .overview-item {
    display: flex; justify-content: space-between; align-items: center;
    padding: 12px 0; border-bottom: 1px solid #f5f5f5;
    &:last-child { border-bottom: none; }
    .overview-label { color: #666; font-size: 14px; }
    .overview-value { color: #333; font-weight: 500; }
  }
}

@media (max-width: 991px) {
  .mt-20 { margin-top: 0; }
  .dashboard :deep(.el-col) { margin-bottom: 14px; }
}
</style>
