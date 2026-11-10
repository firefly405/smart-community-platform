<template>
  <div class="page-container property-page">
    <PropertyPageHeader>
      <template #meta><span>房屋总数 <strong>{{ total }}</strong></span></template>
    </PropertyPageHeader>

    <ProTable
      :data="tableData"
      :columns="columns"
      :total="total"
      :loading="loading"
      :page="page"
      :page-size="pageSize"
      :show-add="false"
      :show-edit="true"
      :show-delete="true"
      :search-fields="searchFields"
      @search="handleSearch"
      @edit="handleEdit"
      @delete="handleDelete"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    >
      <template #toolbar>
        <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>新增房屋</el-button>
        <el-button @click="handleImport"><el-icon><Upload /></el-icon>批量导入</el-button>
        <el-button @click="handleExport"><el-icon><Download /></el-icon>导出</el-button>
      </template>
      <template #status="{ row }">
        <el-tag :type="houseStatusType(row.status)" size="small">
          {{ ['空置', '已售', '已租', '自住'][row.status] || '未知' }}
        </el-tag>
      </template>
      <template #propertyType="{ row }">{{ propertyTypeLabel(row.propertyType) }}</template>
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

    <el-dialog v-model="importVisible" title="批量导入房屋" width="500px">
      <el-upload drag :auto-upload="false" :on-change="handleFileChange" accept=".json">
        <el-icon :size="40"><Upload /></el-icon>
        <div>拖拽文件或点击上传 (JSON格式)</div>
      </el-upload>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImportSubmit">开始导入</el-button>
      </template>
    </el-dialog>
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
import {
  getHouseList,
  createHouse,
  updateHouse,
  deleteHouse,
  importHouse,
  exportHouse,
  getAllBuildings,
  getUnitList,
  type HouseItem
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
const importVisible = ref(false)
const importing = ref(false)
const importFile = ref<any>(null)
const buildingOptions = ref<{ label: string; value: number }[]>([])
const unitOptions = ref<{ label: string; value: number }[]>([])

const columns = [
  { prop: 'houseNo', label: '房号', width: 120 },
  { prop: 'buildingName', label: '楼栋', width: 120, formatter: (_r: any, _c: any, v: string) => v || '-' },
  { prop: 'unitName', label: '单元', width: 100, formatter: (_r: any, _c: any, v: string) => v || '-' },
  { prop: 'ownerName', label: '业主', width: 100, formatter: (_r: any, _c: any, v: string) => v || '-' },
  { prop: 'area', label: '面积(㎡)', width: 100 },
  { prop: 'houseType', label: '户型', width: 80 },
  { prop: 'orientation', label: '朝向', width: 70 },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'propertyType', label: '产权类型', width: 90, slot: 'propertyType' }
]

const searchFields = computed(() => [
  { prop: 'buildingId', label: '楼栋', type: 'select' as const, filterable: true, options: buildingOptions.value },
  { prop: 'unitId', label: '单元', type: 'select' as const, filterable: true, options: unitOptions.value },
  { prop: 'status', label: '状态', type: 'select' as const, options: [
    { label: '空置', value: 0 }, { label: '已售', value: 1 }, { label: '已租', value: 2 }, { label: '自住', value: 3 }
  ]},
  { prop: 'houseNo', label: '房号', type: 'input' as const }
])

const formData = reactive<HouseItem>({
  buildingId: 0,
  unitId: 0,
  houseNo: '',
  area: 0,
  houseType: '',
  orientation: '',
  status: 0,
  propertyType: 1
})

const formFields = computed(() => [
  { prop: 'buildingId', label: '楼栋', type: 'select' as const, required: true, filterable: true, options: buildingOptions.value },
  { prop: 'unitId', label: '单元', type: 'select' as const, filterable: true, options: unitOptions.value },
  { prop: 'houseNo', label: '房号', type: 'input' as const, required: true },
  { prop: 'area', label: '面积(㎡)', type: 'number' as const },
  { prop: 'innerArea', label: '套内面积(㎡)', type: 'number' as const },
  { prop: 'floor', label: '所在楼层', type: 'number' as const },
  { prop: 'houseType', label: '户型', type: 'input' as const },
  { prop: 'orientation', label: '朝向', type: 'input' as const },
  { prop: 'propertyType', label: '产权类型', type: 'select' as const, options: [
    { label: '住宅', value: 1 }, { label: '商铺', value: 2 }, { label: '车位', value: 3 }, { label: '仓库', value: 4 }
  ]},
  { prop: 'deliveryDate', label: '交付日期', type: 'date' as const },
  { prop: 'status', label: '状态', type: 'select' as const, options: [
    { label: '空置', value: 0 }, { label: '已售', value: 1 }, { label: '已租', value: 2 }, { label: '自住', value: 3 }
  ]}
])

const houseStatusType = (status?: number) => ({ 0: 'info', 1: 'success', 2: 'warning', 3: 'primary' } as const)[status ?? 0] || 'info'
const propertyTypeLabel = (type?: number) => ({ 1: '住宅', 2: '商铺', 3: '车位', 4: '仓库' } as Record<number, string>)[type ?? 0] || '未知'

const loadOptions = async () => {
  const [buildings, units] = await Promise.all([
    getAllBuildings().catch(() => [] as any),
    getUnitList({ page: 1, pageSize: 500 }).catch(() => ({ records: [] }))
  ])
  const buildingList = Array.isArray(buildings) ? buildings : ((buildings as any).records || [])
  buildingOptions.value = buildingList.map((b: any) => ({
    value: b.id,
    label: b.buildingName || b.buildingNo || b.name || `楼栋#${b.id}`
  }))
  unitOptions.value = ((units as any).records || units || []).map((u: any) => ({
    value: u.id,
    label: `${u.unitNo || u.name || '单元'}${u.buildingName ? ' / ' + u.buildingName : ''} (#${u.id})`
  }))
}

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getHouseList({ page: page.value, pageSize: pageSize.value, ...queryParams(params) })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleSearch = (params: any) => loadData(params)

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增房屋'
  Object.assign(formData, { id: undefined, buildingId: undefined as any, unitId: undefined as any, houseNo: '', area: 0, innerArea: 0, floor: 1, houseType: '', orientation: '', propertyType: 1, deliveryDate: '', status: 0 })
  dialogVisible.value = true
}

const handleEdit = (row: HouseItem) => {
  isEdit.value = true
  dialogTitle.value = '编辑房屋'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: HouseItem) => {
  await ElMessageBox.confirm('确认删除该房屋？', '提示', { type: 'warning' })
  await deleteHouse(row.id!)
  ElMessage.success('删除成功')
  loadData()
}

const handleConfirm = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateHouse(formData)
    } else {
      await createHouse(formData)
    }
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleImport = () => {
  importVisible.value = true
}

const handleFileChange = (file: any) => {
  importFile.value = file.raw
}

const handleImportSubmit = async () => {
  if (!importFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  importing.value = true
  try {
    const text = await importFile.value.text()
    const houses = JSON.parse(text)
    if (!Array.isArray(houses) || houses.length === 0) throw new Error('invalid file')
    await importHouse(houses)
    ElMessage.success('导入成功')
    importVisible.value = false
    loadData()
  } catch {
    ElMessage.error('导入失败，请检查文件格式')
  } finally {
    importing.value = false
  }
}

const handleExport = async () => {
  try {
    const data = await exportHouse({})
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '房屋数据.json'
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('导出失败')
  }
}

onMounted(async () => {
  await loadOptions().catch(() => {})
  loadData()
})
</script>
