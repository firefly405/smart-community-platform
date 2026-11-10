<template>
  <div class="page-container">
    <ProTable
      :data="tableData"
      :columns="columns"
      :total="total"
      :loading="loading"
      :page="page"
      :page-size="pageSize"
      :search-fields="searchFields"
      @search="handleSearch"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    />
  </div>
</template>

<script setup lang="ts">
import { useListQuery } from '@/composables/useListQuery'
const queryParams = useListQuery()
import { ref, onMounted } from 'vue'
import ProTable from '@/components/ProTable.vue'
import { getPaymentList } from '@/api/fee'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const payMethodMap: Record<number, string> = { 1: '微信', 2: '支付宝', 3: '现金', 4: '银行转账' }
const payStatusMap: Record<number, string> = { 0: '失败', 1: '成功', 2: '退款' }

const columns = [
  { prop: 'paymentNo', label: '缴费单号', width: 180 },
  { prop: 'billId', label: '账单ID', width: 80 },
  { prop: 'ownerName', label: '业主', width: 100 },
  { prop: 'houseNo', label: '房号', width: 100 },
  { prop: 'payAmount', label: '缴费金额', width: 100 },
  { prop: 'payMethod', label: '支付方式', width: 100, formatter: (_r: any, _c: any, v: number) => payMethodMap[v] || '-' },
  { prop: 'payStatus', label: '支付状态', width: 100, formatter: (_r: any, _c: any, v: number) => payStatusMap[v] || '-' },
  { prop: 'payTime', label: '支付时间', width: 170 }
]

const searchFields = [
  { prop: 'ownerId', label: '业主ID', type: 'input' as const },
  { prop: 'houseId', label: '房屋ID', type: 'input' as const }
]

const handleSearch = (params: any) => { loadData(params) }

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getPaymentList({ page: page.value, pageSize: pageSize.value, ...queryParams(params) })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>
