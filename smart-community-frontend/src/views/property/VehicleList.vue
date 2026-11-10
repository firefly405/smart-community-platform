<template>
  <div class="page-container property-page">
    <PropertyPageHeader>
      <template #meta><span>车辆总数 <strong>{{ total }}</strong></span></template>
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
      <template #status="{ row }"><el-tag :type="row.status === 0 ? 'info' : 'success'" size="small">{{ row.status === 0 ? '停用' : '正常' }}</el-tag></template>
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
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import PropertyPageHeader from '@/components/PropertyPageHeader.vue'
import { getVehicleList, createVehicle, updateVehicle, deleteVehicle, getParkingList, getOwnerList, type VehicleItem } from '@/api/house'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const isEdit = ref(false)
const parkingOptions = ref<{ label: string; value: number }[]>([])
const ownerOptions = ref<{ label: string; value: number }[]>([])

const columns = [
  { prop: 'plateNo', label: '车牌号', width: 120 },
  { prop: 'brand', label: '品牌', width: 100 },
  { prop: 'color', label: '颜色', width: 80 },
  { prop: 'vehicleType', label: '车辆类型', width: 90 },
  { prop: 'ownerName', label: '业主', width: 100 },
  { prop: 'parkingNo', label: '绑定车位', width: 120 },
  { prop: 'status', label: '状态', width: 80, slot: 'status' }
]

const searchFields = computed(() => [
  { prop: 'plateNo', label: '车牌号', type: 'input' as const },
  { prop: 'ownerId', label: '所属业主', type: 'select' as const, filterable: true, options: ownerOptions.value }
])

const formData = reactive<VehicleItem>({
  plateNo: '',
  brand: '',
  color: '',
  vehicleType: '',
  ownerId: undefined,
  parkingId: undefined as any,
  status: 1
})

const formFields = computed(() => [
  { prop: 'plateNo', label: '车牌号', type: 'input' as const, required: true },
  { prop: 'brand', label: '品牌', type: 'input' as const },
  { prop: 'color', label: '颜色', type: 'input' as const },
  { prop: 'vehicleType', label: '车辆类型', type: 'input' as const },
  { prop: 'ownerId', label: '所属业主', type: 'select' as const, required: true, options: ownerOptions.value, filterable: true },
  { prop: 'parkingId', label: '绑定车位', type: 'select' as const, options: parkingOptions.value, clearable: true },
  { prop: 'status', label: '车辆状态', type: 'select' as const, options: [
    { label: '正常', value: 1 }, { label: '停用', value: 0 }
  ]}
])

const loadFormOptions = async () => {
  try {
    const [parkingRes, ownerRes] = await Promise.all([
      getParkingList({ page: 1, pageSize: 500 }),
      getOwnerList({ page: 1, pageSize: 500, status: 1 })
    ])
    parkingOptions.value = (parkingRes.records || [])
      .filter((p: any) => p.status === 0 || p.id === formData.parkingId)
      .map((p: any) => ({
      label: `${p.parkingNo || p.id}${p.status === 0 ? '（空闲）' : p.status === 1 ? '（已占用）' : ''}`,
      value: p.id
    }))
    ownerOptions.value = (ownerRes.records || []).map((owner: any) => ({
      label: `${owner.realName || owner.name || '未命名'}（${owner.phone || owner.id}）`,
      value: owner.id
    }))
  } catch {
    parkingOptions.value = []
    ownerOptions.value = []
  }
}

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getVehicleList({ page: page.value, pageSize: pageSize.value, ...params })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleSearch = (params: any) => loadData(params)

const handleAdd = async () => {
  isEdit.value = false
  dialogTitle.value = '新增车辆'
  Object.assign(formData, { id: undefined, plateNo: '', brand: '', color: '', vehicleType: '', ownerId: undefined, parkingId: undefined, status: 1 })
  await loadFormOptions()
  dialogVisible.value = true
}

const handleEdit = async (row: VehicleItem) => {
  isEdit.value = true
  dialogTitle.value = '编辑车辆'
  Object.assign(formData, row)
  await loadFormOptions()
  dialogVisible.value = true
}

const handleDelete = async (row: VehicleItem) => {
  await ElMessageBox.confirm('确认删除该车辆？删除后将释放绑定车位。', '提示', { type: 'warning' })
  await deleteVehicle(row.id!)
  ElMessage.success('删除成功')
  loadData()
}

const handleConfirm = async () => {
  submitting.value = true
  try {
    const payload = { ...formData, plateNo: formData.plateNo?.trim().toUpperCase(), parkingId: formData.parkingId || undefined }
    if (isEdit.value) {
      await updateVehicle(payload)
    } else {
      await createVehicle(payload)
    }
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadFormOptions()
  loadData()
})
</script>
