<template>
  <div class="page-container">
    <SystemPageHeader>
      <template #meta><span>查询结果 <strong>{{ total }}</strong></span></template>
    </SystemPageHeader>
    <ProTable
      :data="tableData"
      :columns="columns"
      :total="total"
      :loading="loading"
      :page="page"
      :page-size="pageSize"
      :show-add="false"
      :show-edit="false"
      :show-delete="false"
      show-export
      :search-fields="searchFields"
      :actions-width="90"
      @search="handleSearch"
      @export="handleExport"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    >
      <template #operation="{ row }">
        <el-tag size="small">{{ row.operation }}</el-tag>
      </template>
      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
          {{ row.status === 1 ? '成功' : '失败' }}
        </el-tag>
      </template>
      <template #actions="{ row }">
        <el-button type="primary" link size="small" @click="detailRow = row">详情</el-button>
      </template>
    </ProTable>

    <el-drawer v-model="detailVisible" title="操作日志详情" size="min(620px, 92vw)">
      <el-descriptions v-if="detailRow" :column="1" border>
        <el-descriptions-item label="操作用户">{{ detailRow.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ detailRow.operation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ detailRow.method || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求地址">{{ detailRow.url || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP / 地点">{{ detailRow.ip || '-' }} / {{ detailRow.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="执行耗时">{{ detailRow.executeTime ?? '-' }} ms</el-descriptions-item>
        <el-descriptions-item label="请求参数"><pre>{{ detailRow.params || '-' }}</pre></el-descriptions-item>
        <el-descriptions-item label="返回结果"><pre>{{ detailRow.result || '-' }}</pre></el-descriptions-item>
        <el-descriptions-item v-if="detailRow.errorMsg" label="错误信息"><pre class="error-text">{{ detailRow.errorMsg }}</pre></el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import ProTable from '@/components/ProTable.vue'
import SystemPageHeader from '@/components/SystemPageHeader.vue'
import { getLogList } from '@/api/system'
import { downloadCsv } from '@/utils/exportCsv'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const latestSearch = ref<Record<string, any>>({})
const detailRow = ref<any>(null)
const detailVisible = computed({ get: () => Boolean(detailRow.value), set: value => { if (!value) detailRow.value = null } })

const columns = [
  { prop: 'username', label: '操作用户', width: 120 },
  { prop: 'operation', label: '操作类型', width: 100, slot: 'operation' },
  { prop: 'url', label: '请求地址', minWidth: 200 },
  { prop: 'ip', label: 'IP地址', width: 140 },
  { prop: 'executeTime', label: '耗时(ms)', width: 100 },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'createTime', label: '操作时间', width: 170 }
]

const searchFields = [
  { prop: 'username', label: '用户名', type: 'input' as const },
  { prop: 'operation', label: '操作类型', type: 'input' as const },
  { prop: 'timeRange', label: '操作时间', type: 'daterange' as const }
]

function apiParams(params: Record<string, any> = {}) {
  const { timeRange, ...rest } = params
  return {
    ...rest,
    beginTime: timeRange?.[0] ? `${timeRange[0]}T00:00:00` : undefined,
    endTime: timeRange?.[1] ? `${timeRange[1]}T23:59:59` : undefined
  }
}

async function loadData(searchParams?: any) {
  loading.value = true
  try {
    const res = await getLogList({ page: page.value, pageSize: pageSize.value, ...apiParams(searchParams) })
    tableData.value = res.records
    total.value = res.total
  } catch { /* handled */ }
  finally { loading.value = false }
}

function handleSearch(params: any) {
  page.value = params.page
  pageSize.value = params.pageSize
  latestSearch.value = params
  loadData(params)
}

async function handleExport(searchParams: any) {
  const params = Object.keys(searchParams || {}).length ? searchParams : latestSearch.value
  const res = await getLogList({ page: 1, pageSize: 10000, ...apiParams(params) })
  downloadCsv(`操作日志-${new Date().toISOString().slice(0, 10)}.csv`, [
    { label: '操作用户', value: 'username' }, { label: '操作类型', value: 'operation' },
    { label: '请求方法', value: 'method' }, { label: '请求地址', value: 'url' },
    { label: 'IP地址', value: 'ip' }, { label: '执行耗时(ms)', value: 'executeTime' },
    { label: '状态', value: row => row.status === 1 ? '成功' : '失败' },
    { label: '操作时间', value: 'createTime' }
  ], res.records)
}

onMounted(() => loadData())
</script>

<style scoped>
pre { margin: 0; white-space: pre-wrap; overflow-wrap: anywhere; font: inherit; }
.error-text { color: #c03639; }
</style>
