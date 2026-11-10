<template>
  <div class="page-container property-page">
    <PropertyPageHeader>
      <template #meta><span>车位总数 <strong>{{ total }}</strong></span></template>
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
        <el-tag :type="row.status === 0 ? 'info' : row.status === 1 ? 'success' : 'warning'" size="small">
          {{ ['空闲', '已售', '已租'][row.status] || row.status }}
        </el-tag>
      </template>
      <template #parkingType="{ row }">{{ row.parkingType === 2 ? '地下' : '地上' }}</template>
      <template #price="{ row }">{{ formatMoney(row.price) }}</template>
      <template #rent="{ row }">{{ formatMoney(row.monthlyRent) }}</template>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import PropertyPageHeader from '@/components/PropertyPageHeader.vue'
import { getParkingList, createParking, updateParking, deleteParking, type ParkingItem } from '@/api/house'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const isEdit = ref(false)

const columns = [
  { prop: 'parkingNo', label: '车位编号', width: 120 },
  { prop: 'parkingType', label: '类型', width: 80, slot: 'parkingType' },
  { prop: 'area', label: '区域', width: 100 },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'price', label: '售价', width: 110, slot: 'price' },
  { prop: 'monthlyRent', label: '月租金', width: 100, slot: 'rent' }
]

const searchFields = [
  { prop: 'parkingNo', label: '车位编号', type: 'input' as const },
  { prop: 'status', label: '状态', type: 'select' as const, options: [
    { label: '空闲', value: 0 }, { label: '已售', value: 1 }, { label: '已租', value: 2 }
  ]}
]

const formData = reactive<ParkingItem>({
  parkingNo: '',
  parkingType: 1,
  area: '',
  status: 0,
  price: 0,
  monthlyRent: 0
})

const formFields = [
  { prop: 'parkingNo', label: '车位编号', type: 'input' as const, required: true },
  { prop: 'parkingType', label: '类型', type: 'select' as const, options: [
    { label: '地上', value: 1 }, { label: '地下', value: 2 }
  ]},
  { prop: 'area', label: '区域', type: 'input' as const },
  { prop: 'status', label: '状态', type: 'select' as const, options: [
    { label: '空闲', value: 0 }, { label: '已售', value: 1 }, { label: '已租', value: 2 }
  ]},
  { prop: 'price', label: '售价', type: 'number' as const },
  { prop: 'monthlyRent', label: '月租金', type: 'number' as const }
]

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getParkingList({ page: page.value, pageSize: pageSize.value, ...params })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleSearch = (params: any) => loadData(params)

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增车位'
  Object.assign(formData, { id: undefined, parkingNo: '', parkingType: 1, area: '', status: 0, price: 0, monthlyRent: 0 })
  dialogVisible.value = true
}

const handleEdit = (row: ParkingItem) => {
  isEdit.value = true
  dialogTitle.value = '编辑车位'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: ParkingItem) => {
  await ElMessageBox.confirm('确认删除该车位？', '提示', { type: 'warning' })
  await deleteParking(row.id!)
  ElMessage.success('删除成功')
  loadData()
}

const handleConfirm = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateParking(formData)
    } else {
      await createParking(formData)
    }
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const formatMoney = (value?: number) => value == null ? '-' : `¥${Number(value).toLocaleString('zh-CN')}`

onMounted(() => loadData())
</script>
