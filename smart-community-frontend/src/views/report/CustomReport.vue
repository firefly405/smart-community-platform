<template>
  <div class="page-container">
    <div class="report-actions">
      <el-form :inline="true" :model="query">
        <el-form-item label="报表主题">
          <el-select v-model="query.type" style="width: 180px" @change="loadData">
            <el-option label="业主与房屋" value="owner" />
            <el-option label="物业服务" value="service" />
            <el-option label="费用与财务" value="finance" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadData"><el-icon><Refresh /></el-icon>刷新</el-button>
          <el-button @click="handleSaveTemplate"><el-icon><CollectionTag /></el-icon>保存模板</el-button>
          <el-button @click="handleExport"><el-icon><Download /></el-icon>导出 CSV</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="custom-layout" v-loading="loading">
      <el-card shadow="never" class="chart-panel">
        <template #header><div class="chart-panel__header"><span>{{ currentTypeLabel }}指标</span><el-tag effect="plain">实时查询</el-tag></div></template>
        <ReportChart :option="chartOption" height="320px" :aria-label="`${currentTypeLabel}指标柱状图`" />
      </el-card>
      <el-card shadow="never">
        <template #header><div class="chart-panel__header"><span>指标明细</span><span class="metric-count">{{ tableData.length }} 项</span></div></template>
        <el-table :data="tableData" height="320" stripe>
          <el-table-column prop="label" label="指标" min-width="120" />
          <el-table-column label="数值" min-width="100" align="right">
            <template #default="{ row }"><strong>{{ formatValue(row.value) }}</strong> {{ row.unit }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-card shadow="never" class="template-card">
      <template #header><div class="chart-panel__header"><span>我的报表模板</span><span class="metric-count">保存在当前浏览器</span></div></template>
      <el-empty v-if="!templates.length" description="暂无保存的模板" :image-size="72" />
      <div v-else class="template-list">
        <div v-for="item in templates" :key="item.id" class="template-item">
          <div><strong>{{ item.name }}</strong><span>{{ typeLabels[item.type] }} · {{ item.createTime }}</span></div>
          <div><el-button link type="primary" @click="applyTemplate(item)">应用</el-button><el-button link type="danger" @click="deleteTemplate(item.id)">删除</el-button></div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ReportChart from '@/components/ReportChart.vue'
import { getFinanceStats, getOwnerStats, getServiceStats } from '@/api/report'
import { downloadCsv } from '@/utils/exportCsv'

type ReportType = 'owner' | 'service' | 'finance'
interface Metric { label: string; value: number; unit: string }
interface Template { id: number; name: string; type: ReportType; createTime: string }

const STORAGE_KEY = 'smart-community-report-templates'
const loading = ref(false)
const tableData = ref<Metric[]>([])
const templates = ref<Template[]>([])
const query = reactive<{ type: ReportType }>({ type: 'owner' })
const typeLabels: Record<ReportType, string> = { owner: '业主与房屋', service: '物业服务', finance: '费用与财务' }
const currentTypeLabel = computed(() => typeLabels[query.type])
const formatValue = (value: number) => Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })

const chartOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: (items: any[]) => {
    const item = tableData.value[items?.[0]?.dataIndex]
    return item ? `${item.label}<br/>${formatValue(item.value)} ${item.unit}` : ''
  } },
  grid: { left: 24, right: 24, top: 20, bottom: 24, containLabel: true },
  xAxis: { type: 'category', data: tableData.value.map(item => item.label), axisTick: { show: false }, axisLabel: { interval: 0 } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#edf0f2' } } },
  color: ['#376f9f'],
  series: [{ type: 'bar', barMaxWidth: 46, data: tableData.value.map(item => item.value), itemStyle: { borderRadius: [4, 4, 0, 0] } }]
}))

async function loadData() {
  loading.value = true
  try {
    if (query.type === 'owner') {
      const data: any = await getOwnerStats()
      tableData.value = [
        { label: '业主总数', value: Number(data.totalOwners || 0), unit: '人' },
        { label: '房屋总数', value: Number(data.totalHouses || 0), unit: '套' },
        { label: '入住房屋', value: Number(data.occupiedHouses || 0), unit: '套' },
        { label: '入住率', value: Number(data.occupancyRate || 0), unit: '%' }
      ]
    } else if (query.type === 'service') {
      const data: any = await getServiceStats()
      tableData.value = [
        { label: '报修总数', value: Number(data.totalRepairs || 0), unit: '单' },
        { label: '已完成报修', value: Number(data.completedRepairs || 0), unit: '单' },
        { label: '报修完成率', value: Number(data.completionRate || 0), unit: '%' },
        { label: '投诉总数', value: Number(data.totalComplaints || 0), unit: '单' }
      ]
    } else {
      const data: any = await getFinanceStats()
      tableData.value = [
        { label: '本月应收', value: Number(data.totalAmount || 0), unit: '元' },
        { label: '本月实收', value: Number(data.totalPaid || 0), unit: '元' },
        { label: '收缴率', value: Number(data.collectionRate || 0), unit: '%' },
        { label: '账单数', value: Number(data.billCount || 0), unit: '笔' }
      ]
    }
  } finally { loading.value = false }
}

function persistTemplates() { localStorage.setItem(STORAGE_KEY, JSON.stringify(templates.value)) }
async function handleSaveTemplate() {
  const result = await ElMessageBox.prompt('请输入模板名称', '保存报表模板', {
    confirmButtonText: '保存', cancelButtonText: '取消', inputPlaceholder: `${currentTypeLabel.value}报表`,
    inputValidator: value => Boolean(value.trim()) || '请输入模板名称'
  }).catch(() => null)
  if (!result) return
  templates.value.unshift({ id: Date.now(), name: result.value.trim(), type: query.type, createTime: new Date().toLocaleString('zh-CN') })
  persistTemplates()
  ElMessage.success('模板已保存')
}
function applyTemplate(item: Template) { query.type = item.type; void loadData() }
function deleteTemplate(id: number) { templates.value = templates.value.filter(item => item.id !== id); persistTemplates(); ElMessage.success('模板已删除') }
function handleExport() {
  downloadCsv(`${currentTypeLabel.value}报表.csv`, [
    { label: '指标', value: 'label' }, { label: '数值', value: 'value' }, { label: '单位', value: 'unit' }
  ], tableData.value as unknown as Record<string, any>[])
  ElMessage.success('报表已导出')
}

onMounted(() => {
  try { templates.value = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]') } catch { templates.value = [] }
  void loadData()
})
</script>

<style scoped>
.report-actions { margin-bottom: 14px; padding: 14px 16px 0; border: 1px solid #e2e7ec; border-radius: 6px; background: #fff; }
.custom-layout { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(300px, .75fr); gap: 16px; }
.metric-count { color: #7b8794; font-size: 12px; font-weight: 400; }
.template-card { margin-top: 16px; }
.template-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.template-item { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 14px; border: 1px solid #e5e9ed; border-radius: 6px; background: #f9fafb; }
.template-item strong,.template-item span { display: block; }.template-item strong { color: #344054; }.template-item span { margin-top: 3px; color: #7b8794; font-size: 12px; }
@media (max-width: 900px) { .custom-layout,.template-list { grid-template-columns: 1fr; } }
</style>
