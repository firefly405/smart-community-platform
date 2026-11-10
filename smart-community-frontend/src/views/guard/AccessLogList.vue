<template>
  <div class="page-container">
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
      :search-fields="searchFields"
      @search="handleSearch"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ProTable from '@/components/ProTable.vue'
import { getAccessLogList } from '@/api/guard'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const accessTypeMap: Record<number, string> = { 1: '人脸识别', 2: '刷卡', 3: '密码', 4: '二维码', 5: '远程开门' }
const directionMap: Record<number, string> = { 1: '进', 2: '出' }
const resultMap: Record<number, string> = { 1: '成功', 0: '失败', 2: '失败' }
const userTypeMap: Record<number, string> = { 1: '业主', 2: '家属', 3: '租户', 4: '访客', 5: '物业' }

const columns = [
  { prop: 'accessName', label: '门禁设备', width: 140, formatter: (_r: any, _c: any, v: string) => v || '未指定设备' },
  { prop: 'userName', label: '通行人员', width: 120, formatter: (_r: any, _c: any, v: string) => v || '-' },
  { prop: 'userType', label: '用户类型', width: 90, formatter: (_r: any, _c: any, v: number) => userTypeMap[v] || '-' },
  { prop: 'accessType', label: '通行方式', width: 100, formatter: (_r: any, _c: any, v: number) => accessTypeMap[v] || '-' },
  { prop: 'accessDirection', label: '进出', width: 60, formatter: (_r: any, _c: any, v: number) => directionMap[v] || '-' },
  { prop: 'accessTime', label: '通行时间', width: 170 },
  { prop: 'accessResult', label: '结果', width: 80, formatter: (_r: any, _c: any, v: number) => resultMap[v] || '-' },
  { prop: 'remark', label: '备注', minWidth: 180, formatter: (_r: any, _c: any, v: string) => v || '-' },
  { prop: 'failReason', label: '失败原因', width: 120, formatter: (_r: any, _c: any, v: string) => v || '-' }
]

const searchFields = [
  { prop: 'accessId', label: '设备ID', type: 'input' as const }
]

const handleSearch = (params: any) => loadData(params)

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getAccessLogList({ page: page.value, pageSize: pageSize.value, ...params })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>
