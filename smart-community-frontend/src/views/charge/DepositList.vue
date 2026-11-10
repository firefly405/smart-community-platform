<template>
  <div class="page-container">
    <ProTable
      :data="tableData"
      :columns="columns"
      :total="total"
      :loading="loading"
      :page="page"
      :page-size="pageSize"
      :show-add="true"
      :search-fields="searchFields"
      @search="handleSearch"
      @add="handleAdd"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    >
      <template #actions="{ row }">
        <el-button type="warning" size="small" link @click="handleRefund(row)" :disabled="row.status === 2">退款</el-button>
      </template>
    </ProTable>

    <ProDialog
      :visible="dialogVisible"
      :title="dialogTitle"
      :form-data="formData"
      :form-fields="formFields"
      :loading="submitting"
      @confirm="handleConfirm"
      @cancel="dialogVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import { getDepositList, createDeposit, refundDeposit, type DepositItem } from '@/api/fee'
import { getOwnerList, getHouseList } from '@/api/house'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const ownerOptions = ref<{ label: string; value: number }[]>([])
const houseOptions = ref<{ label: string; value: number }[]>([])

const depositTypeMap: Record<string, string> = { '1': '装修押金', '2': '车位押金', '3': '其他' }
const statusMap: Record<number, string> = { 0: '已收', 1: '部分退', 2: '已全退' }

const columns = [
  { prop: 'depositNo', label: '押金单号', width: 180 },
  { prop: 'ownerName', label: '业主', width: 100, formatter: (_r: any, _c: any, v: string) => v || '-' },
  { prop: 'houseNo', label: '房号', width: 100, formatter: (_r: any, _c: any, v: string) => v || '-' },
  { prop: 'depositType', label: '押金类型', width: 100, formatter: (_r: any, _c: any, v: number) => depositTypeMap[String(v)] || '-' },
  { prop: 'amount', label: '押金金额', width: 100 },
  { prop: 'refundedAmount', label: '已退金额', width: 100 },
  { prop: 'status', label: '状态', width: 100, formatter: (_r: any, _c: any, v: number) => statusMap[v] || '-' },
  { prop: 'remark', label: '备注', minWidth: 160, formatter: (_r: any, _c: any, v: string) => v || '-' },
  { prop: 'receiveTime', label: '收款时间', width: 170 },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const searchFields = [
  { prop: 'status', label: '状态', type: 'select' as const, options: Object.entries(statusMap).map(([k, v]) => ({ value: Number(k), label: v })) }
]

const formData = reactive<DepositItem>({
  houseId: 0,
  ownerId: 0,
  depositType: 1,
  amount: 0
} as any)

const formFields = computed(() => [
  { prop: 'ownerId', label: '业主', type: 'select' as const, required: true, filterable: true, options: ownerOptions.value },
  { prop: 'houseId', label: '房屋', type: 'select' as const, required: true, filterable: true, options: houseOptions.value },
  { prop: 'depositType', label: '押金类型', type: 'select' as const, options: Object.entries(depositTypeMap).map(([k, v]) => ({ value: Number(k), label: v })) },
  { prop: 'amount', label: '押金金额', type: 'number' as const, required: true }
])

const loadOptions = async () => {
  const [owners, houses] = await Promise.all([
    getOwnerList({ page: 1, pageSize: 500 }),
    getHouseList({ page: 1, pageSize: 500 })
  ])
  ownerOptions.value = (owners.records || []).map((o: any) => ({
    value: o.id,
    label: `${o.realName || o.name || '业主'}${o.phone ? ' / ' + o.phone : ''} (#${o.id})`
  }))
  houseOptions.value = (houses.records || []).map((h: any) => ({
    value: h.id,
    label: `${h.buildingName || ''} ${h.unitName || ''} ${h.houseNo || ''}`.trim() + ` (#${h.id})`
  }))
}

const handleAdd = () => {
  dialogTitle.value = '新增押金'
  Object.assign(formData, { houseId: undefined, ownerId: undefined, depositType: 1, amount: 0 })
  dialogVisible.value = true
}

const handleConfirm = async () => {
  submitting.value = true
  try {
    await createDeposit({
      ...formData,
      // 兼容后端 depositType 字段
      depositType: (formData as any).depositType ?? (formData as any).type ?? 1
    } as any)
    ElMessage.success('新增成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleRefund = async (row: any) => {
  await ElMessageBox.confirm('确认退款该押金？', '提示', { type: 'warning' })
  await refundDeposit(row.id!)
  ElMessage.success('退款成功')
  loadData()
}

const handleSearch = (params: any) => { loadData(params) }

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getDepositList({ page: page.value, pageSize: pageSize.value, ...params })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadOptions().catch(() => {})
  loadData()
})
</script>
