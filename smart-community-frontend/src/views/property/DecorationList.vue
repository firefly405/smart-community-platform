<template>
  <div class="page-container property-page">
    <PropertyPageHeader>
      <template #meta><span>装修申请 <strong>{{ total }}</strong></span></template>
    </PropertyPageHeader>
    <ProTable
      :data="tableData"
      :columns="columns"
      :total="total"
      :loading="loading"
      :page="page"
      :page-size="pageSize"
      :show-add="true"
      :show-edit="true"
      :show-delete="true"
      :search-fields="searchFields"
      :actions-width="300"
      @search="handleSearch"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    >
      <template #status="{ row }">
        <el-tag :type="getStatusType(row.status)" size="small">
          {{ getStatusText(row.status) }}
        </el-tag>
      </template>
      <template #actions="{ row }">
        <el-button v-if="row.status === 0" type="success" size="small" link @click="handleApprove(row, 1)">通过</el-button>
        <el-button v-if="row.status === 0" type="danger" size="small" link @click="handleApprove(row, 4)">驳回</el-button>
        <el-button v-if="row.status === 1" type="primary" size="small" link @click="handleApprove(row, 2)">开始施工</el-button>
        <el-button v-if="row.status === 2" type="success" size="small" link @click="handleApprove(row, 3)">验收</el-button>
        <el-button v-if="row.depositAmount > 0 && [1, 2, 3].includes(row.status)" type="primary" size="small" link @click="openDeposit(row)">查看押金</el-button>
      </template>
    </ProTable>

    <ProDialog
      :visible="dialogVisible"
      :title="dialogTitle"
      :form-data="formData"
      :form-fields="formFields"
      :loading="submitting"
      width="550px"
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
import PropertyPageHeader from '@/components/PropertyPageHeader.vue'
import { useRouter } from 'vue-router'
import {
  getDecorationList,
  createDecoration,
  updateDecoration,
  deleteDecoration,
  approveDecoration,
  getOwnerList,
  getHouseList,
  type DecorationItem
} from '@/api/house'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const isEdit = ref(false)
const ownerOptions = ref<{ label: string; value: number }[]>([])
const houseOptions = ref<{ label: string; value: number }[]>([])
const router = useRouter()

const statusTextMap: Record<number, string> = {
  0: '待审批', 1: '已通过', 2: '施工中', 3: '已验收', 4: '已驳回'
}
const statusTypeMap: Record<number, 'success' | 'info' | 'primary' | 'warning' | 'danger'> = {
  0: 'warning', 1: 'success', 2: 'primary', 3: 'success', 4: 'danger'
}

const getStatusText = (s: number) => statusTextMap[s] || '-'
const getStatusType = (s: number) => statusTypeMap[s] || 'info'

const columns = [
  { prop: 'decorationNo', label: '装修单号', width: 180 },
  { prop: 'ownerName', label: '业主', width: 100, formatter: (_r: any, _c: any, v: string) => v || '-' },
  { prop: 'houseNo', label: '房号', width: 100, formatter: (_r: any, _c: any, v: string) => v || '-' },
  { prop: 'companyName', label: '装修公司', width: 140, formatter: (_r: any, _c: any, v: string) => v || '-' },
  { prop: 'companyPhone', label: '公司电话', width: 120, formatter: (_r: any, _c: any, v: string) => v || '-' },
  { prop: 'depositAmount', label: '押金金额', width: 100 },
  { prop: 'startDate', label: '开工日期', width: 110 },
  { prop: 'expectEndDate', label: '预计完工', width: 110 },
  { prop: 'status', label: '状态', width: 80, slot: 'status' }
]

const searchFields = computed(() => [
  { prop: 'ownerId', label: '业主', type: 'select' as const, filterable: true, options: ownerOptions.value },
  { prop: 'houseId', label: '房屋', type: 'select' as const, filterable: true, options: houseOptions.value },
  {
    prop: 'status',
    label: '状态',
    type: 'select' as const,
    options: Object.entries(statusTextMap).map(([k, v]) => ({ value: Number(k), label: v }))
  }
])

const formData = reactive<DecorationItem>({
  ownerId: 0,
  houseId: 0,
  companyName: '',
  companyPhone: '',
  startDate: '',
  expectEndDate: '',
  depositAmount: 0
})

const formFields = computed(() => [
  { prop: 'ownerId', label: '业主', type: 'select' as const, required: true, filterable: true, options: ownerOptions.value },
  { prop: 'houseId', label: '房屋', type: 'select' as const, required: true, filterable: true, options: houseOptions.value },
  { prop: 'companyName', label: '装修公司', type: 'input' as const },
  { prop: 'companyPhone', label: '公司电话', type: 'input' as const },
  { prop: 'startDate', label: '开工日期', type: 'date' as const },
  { prop: 'expectEndDate', label: '预计完工', type: 'date' as const },
  { prop: 'actualEndDate', label: '实际完工', type: 'date' as const },
  { prop: 'depositAmount', label: '押金', type: 'number' as const },
  { prop: 'violationRecord', label: '违规记录', type: 'textarea' as const, span: 24, rows: 2 },
  { prop: 'checkResult', label: '验收结果', type: 'textarea' as const, span: 24, rows: 2 }
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

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getDecorationList({ page: page.value, pageSize: pageSize.value, ...params })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleSearch = (params: any) => loadData(params)

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增装修申请'
  Object.assign(formData, {
    id: undefined,
    ownerId: undefined as any,
    houseId: undefined as any,
    companyName: '',
    companyPhone: '',
    startDate: '',
    expectEndDate: '',
    depositAmount: 0,
    actualEndDate: '',
    violationRecord: '',
    checkResult: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: DecorationItem) => {
  isEdit.value = true
  dialogTitle.value = '编辑装修申请'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: DecorationItem) => {
  await ElMessageBox.confirm('确认删除该申请？', '提示', { type: 'warning' })
  await deleteDecoration(row.id!)
  ElMessage.success('删除成功')
  loadData()
}

const handleConfirm = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateDecoration(formData)
    } else {
      await createDecoration(formData)
    }
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleApprove = async (row: DecorationItem, status: number) => {
  try {
    let checkResult = ''
    if (status === 3 || status === 4) {
      const action = status === 3 ? '验收' : '驳回'
      const result = await ElMessageBox.prompt(`请填写${action}意见`, `${action}装修申请`, {
        confirmButtonText: '确认', cancelButtonText: '取消',
        inputType: 'textarea', inputValidator: value => Boolean(value?.trim()) || `请填写${action}意见`
      })
      checkResult = result.value.trim()
    } else {
      await ElMessageBox.confirm(status === 1 ? '确认审批通过该装修申请？' : '确认标记为已开工？', '操作确认', { type: 'warning' })
    }
    await approveDecoration(row.id!, { status, checkResult })
    if (status === 1) {
      const amount = Number(row.depositAmount || 0)
      ElMessage.success(amount > 0 ? '审批通过，已自动生成装修押金单' : '审批通过')
    } else if (status === 3) {
      ElMessage.success('验收完成，可到押金管理办理退还')
    } else {
      ElMessage.success('操作成功')
    }
    loadData()
  } catch {
    // handled by interceptor
  }
}

const openDeposit = (row: DecorationItem) => router.push({ path: '/charge/deposit', query: { ownerId: row.ownerId, houseId: row.houseId } })

onMounted(async () => {
  await loadOptions().catch(() => {})
  loadData()
})
</script>
