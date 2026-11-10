<template>
  <div class="page-container property-page">
    <PropertyPageHeader>
      <template #meta><span>单元总数 <strong>{{ total }}</strong></span></template>
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
      <template #building="{ row }">{{ buildingLabel(row.buildingId) }}</template>
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
import { useListQuery } from '@/composables/useListQuery'
const queryParams = useListQuery()
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import PropertyPageHeader from '@/components/PropertyPageHeader.vue'
import { getUnitList, createUnit, updateUnit, deleteUnit, getAllBuildings, type UnitItem } from '@/api/house'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const isEdit = ref(false)
const buildingOptions = ref<{ label: string; value: number }[]>([])

const columns = [
  { prop: 'unitNo', label: '单元编号', width: 120 },
  { prop: 'buildingId', label: '所属楼栋', width: 140, slot: 'building' },
  { prop: 'floorCount', label: '楼层数', width: 80 },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const searchFields = computed(() => [
  { prop: 'buildingId', label: '所属楼栋', type: 'select' as const, options: buildingOptions.value }
])

const formData = reactive<UnitItem>({
  buildingId: 0,
  unitNo: '',
  floorCount: 1
})

const formFields = computed(() => [
  { prop: 'buildingId', label: '所属楼栋', type: 'select' as const, required: true, filterable: true, options: buildingOptions.value },
  { prop: 'unitNo', label: '单元编号', type: 'input' as const, required: true },
  { prop: 'floorCount', label: '楼层数', type: 'number' as const }
])

const buildingLabel = (id?: number) => buildingOptions.value.find(item => item.value === id)?.label || `楼栋 #${id || '-'}`

const loadBuildings = async () => {
  const list = await getAllBuildings()
  buildingOptions.value = (list || []).map((item: any) => ({
    value: item.id,
    label: item.buildingName || item.buildingNo || `楼栋 #${item.id}`
  }))
}

const handleSearch = (params: any) => {
  loadData(params)
}

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getUnitList({ page: page.value, pageSize: pageSize.value, ...queryParams(params) })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增单元'
  Object.assign(formData, { buildingId: 0, unitNo: '', floorCount: 1 })
  dialogVisible.value = true
}

const handleEdit = (row: UnitItem) => {
  isEdit.value = true
  dialogTitle.value = '编辑单元'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: UnitItem) => {
  await ElMessageBox.confirm('确认删除该单元？', '提示', { type: 'warning' })
  await deleteUnit(row.id!)
  ElMessage.success('删除成功')
  loadData()
}

const handleConfirm = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateUnit(formData)
    } else {
      await createUnit(formData)
    }
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadBuildings().catch(() => {})
  loadData()
})
</script>
