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
      @search="handleSearch"
      @export="handleExport"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    >
      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
          {{ row.status === 1 ? '成功' : '失败' }}
        </el-tag>
      </template>
    </ProTable>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ProTable from '@/components/ProTable.vue'
import SystemPageHeader from '@/components/SystemPageHeader.vue'
import { getLoginLogList } from '@/api/system'
import { downloadCsv } from '@/utils/exportCsv'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const latestSearch = ref<Record<string, any>>({})

const columns = [
  { prop: 'username', label: '用户名', width: 120 },
  { prop: 'ip', label: 'IP地址', width: 140 },
  { prop: 'location', label: '登录地点', width: 150 },
  { prop: 'browser', label: '浏览器', width: 120 },
  { prop: 'os', label: '操作系统', width: 120 },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'msg', label: '提示消息', minWidth: 150 },
  { prop: 'loginTime', label: '登录时间', width: 170 }
]

const searchFields = [
  { prop: 'username', label: '用户名', type: 'input' as const },
  { prop: 'status', label: '状态', type: 'select' as const, options: [
    { label: '成功', value: 1 }, { label: '失败', value: 0 }
  ]},
  { prop: 'timeRange', label: '登录时间', type: 'daterange' as const }
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
    const res = await getLoginLogList({ page: page.value, pageSize: pageSize.value, ...apiParams(searchParams) })
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
  const res = await getLoginLogList({ page: 1, pageSize: 10000, ...apiParams(params) })
  downloadCsv(`登录日志-${new Date().toISOString().slice(0, 10)}.csv`, [
    { label: '用户名', value: 'username' }, { label: 'IP地址', value: 'ip' },
    { label: '登录地点', value: 'location' }, { label: '浏览器', value: 'browser' },
    { label: '操作系统', value: 'os' },
    { label: '状态', value: row => row.status === 1 ? '成功' : '失败' },
    { label: '提示消息', value: 'msg' }, { label: '登录时间', value: 'loginTime' }
  ], res.records)
}

onMounted(() => loadData())
</script>
