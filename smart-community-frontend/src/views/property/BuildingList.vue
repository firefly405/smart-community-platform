<template>
  <div class="page-container property-page">
    <PropertyPageHeader>
      <template #meta><span>楼栋总数 <strong>{{ total }}</strong></span></template>
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
    />

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
import { getBuildingList, createBuilding, updateBuilding, deleteBuilding, type BuildingItem } from '@/api/house'

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
  { prop: 'buildingNo', label: '楼栋编号', width: 120 },
  { prop: 'buildingName', label: '楼栋名称', width: 150 },
  { prop: 'unitCount', label: '单元数', width: 80 },
  { prop: 'floorCount', label: '楼层数', width: 80 },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const searchFields = [
  { prop: 'buildingName', label: '楼栋名称', type: 'input' as const }
]

const formData = reactive<BuildingItem>({
  buildingNo: '',
  buildingName: '',
  unitCount: 1,
  floorCount: 1
})

const formFields = [
  { prop: 'buildingNo', label: '楼栋编号', type: 'input' as const, required: true },
  { prop: 'buildingName', label: '楼栋名称', type: 'input' as const, required: true },
  { prop: 'unitCount', label: '单元数', type: 'number' as const },
  { prop: 'floorCount', label: '楼层数', type: 'number' as const }
]

const handleSearch = (params: any) => {
  loadData(params)
}

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getBuildingList({ page: page.value, pageSize: pageSize.value, ...params })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增楼栋'
  Object.assign(formData, { buildingNo: '', buildingName: '', unitCount: 1, floorCount: 1 })
  dialogVisible.value = true
}

const handleEdit = (row: BuildingItem) => {
  isEdit.value = true
  dialogTitle.value = '编辑楼栋'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: BuildingItem) => {
  await ElMessageBox.confirm('确认删除该楼栋？', '提示', { type: 'warning' })
  await deleteBuilding(row.id!)
  ElMessage.success('删除成功')
  loadData()
}

const handleConfirm = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateBuilding(formData)
    } else {
      await createBuilding(formData)
    }
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(() => loadData())
</script>
