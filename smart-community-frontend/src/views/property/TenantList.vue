<template>
  <div class="page-container property-page">
    <PropertyPageHeader>
      <template #meta><span>租户总数 <strong>{{ total }}</strong></span></template>
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
      @search="handleSearch"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    >
      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
          {{ row.status === 1 ? '租赁中' : '已退租' }}
        </el-tag>
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
import { ref, reactive, onMounted } from 'vue'
import { computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import PropertyPageHeader from '@/components/PropertyPageHeader.vue'
import { getTenantList as getList, createTenant, updateTenant, deleteTenant, getHouseList, type TenantItem } from '@/api/house'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const isEdit = ref(false)
const houseOptions = ref<{ label: string; value: number }[]>([])

const columns = [
  { prop: 'realName', label: '姓名', width: 100 },
  { prop: 'phone', label: '手机号', width: 120 },
  { prop: 'ownerName', label: '业主', width: 100, formatter: (_r: any, _c: any, value: string) => value || '-' },
  { prop: 'houseNo', label: '房号', width: 100 },
  { prop: 'leaseStartDate', label: '租期开始', width: 110 },
  { prop: 'leaseEndDate', label: '租期结束', width: 110 },
  { prop: 'monthlyRent', label: '月租金', width: 90 },
  { prop: 'status', label: '状态', width: 80, slot: 'status' }
]

const searchFields = computed(() => [
  { prop: 'realName', label: '姓名', type: 'input' as const },
  { prop: 'phone', label: '手机号', type: 'input' as const },
  { prop: 'houseId', label: '房屋', type: 'select' as const, options: houseOptions.value },
  { prop: 'status', label: '状态', type: 'select' as const, options: [
    { label: '租赁中', value: 1 }, { label: '已退租', value: 0 }
  ]}
])

const formData = reactive<TenantItem>({
  houseId: 0,
  realName: '',
  phone: '',
  leaseStartDate: '',
  leaseEndDate: '',
  monthlyRent: 0,
  deposit: 0,
  status: 1
})

const formFields = computed(() => [
  { prop: 'houseId', label: '租住房屋', type: 'select' as const, required: true, filterable: true, options: houseOptions.value },
  { prop: 'realName', label: '姓名', type: 'input' as const, required: true },
  { prop: 'phone', label: '手机号', type: 'input' as const, required: true },
  { prop: 'idCard', label: '身份证号', type: 'input' as const },
  { prop: 'gender', label: '性别', type: 'select' as const, options: [
    { label: '未知', value: 0 }, { label: '男', value: 1 }, { label: '女', value: 2 }
  ]},
  { prop: 'leaseStartDate', label: '租期开始', type: 'date' as const },
  { prop: 'leaseEndDate', label: '租期结束', type: 'date' as const },
  { prop: 'monthlyRent', label: '月租金', type: 'number' as const },
  { prop: 'deposit', label: '押金', type: 'number' as const },
  { prop: 'status', label: '租赁状态', type: 'select' as const, options: [
    { label: '租赁中', value: 1 }, { label: '已退租', value: 0 }
  ]}
])

const loadHouses = async () => {
  const res = await getHouseList({ page: 1, pageSize: 1000 })
  houseOptions.value = (res.records || []).map((house: any) => ({
    value: house.id,
    label: [house.buildingName, house.unitName, house.houseNo].filter(Boolean).join(' ') || `房屋 #${house.id}`
  }))
}

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getList({ page: page.value, pageSize: pageSize.value, ...params })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleSearch = (params: any) => loadData(params)

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增租户'
  Object.assign(formData, { id: undefined, houseId: undefined as any, realName: '', phone: '', idCard: '', gender: 0, leaseStartDate: '', leaseEndDate: '', monthlyRent: 0, deposit: 0, status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row: TenantItem) => {
  isEdit.value = true
  dialogTitle.value = '编辑租户'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: TenantItem) => {
  await ElMessageBox.confirm('确认删除该租户？', '提示', { type: 'warning' })
  await deleteTenant(row.id!)
  ElMessage.success('删除成功')
  loadData()
}

const handleConfirm = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateTenant(formData)
    } else {
      await createTenant(formData)
    }
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadHouses().catch(() => {})
  loadData()
})
</script>
