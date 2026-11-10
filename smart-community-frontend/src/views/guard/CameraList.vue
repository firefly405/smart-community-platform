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
import { getCameraList, createCamera, updateCamera, deleteCamera, type CameraItem } from '@/api/guard'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const isEdit = ref(false)

const cameraTypeMap: Record<string, string> = { '1': '枪机', '2': '球机', '3': '全景' }
const statusMap: Record<number, string> = { 0: '离线', 1: '在线', 2: '故障' }

const columns = [
  { prop: 'cameraNo', label: '摄像头编号', width: 120 },
  { prop: 'cameraName', label: '摄像头名称', width: 150 },
  { prop: 'cameraType', label: '类型', width: 80, formatter: (_r: any, _c: any, v: number) => cameraTypeMap[String(v)] || '-' },
  { prop: 'location', label: '安装位置', width: 150 },
  { prop: 'status', label: '状态', width: 80, formatter: (_r: any, _c: any, v: number) => statusMap[v] || '-' },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const searchFields = [
  { prop: 'status', label: '状态', type: 'select' as const, options: Object.entries(statusMap).map(([k, v]) => ({ value: Number(k), label: v })) }
]

const formData = reactive<CameraItem>({
  name: '',
  code: '',
  location: ''
} as any)

const formFields = [
  { prop: 'cameraNo', label: '摄像头编号', type: 'input' as const, required: true },
  { prop: 'cameraName', label: '摄像头名称', type: 'input' as const, required: true },
  { prop: 'cameraType', label: '类型', type: 'select' as const, options: Object.entries(cameraTypeMap).map(([k, v]) => ({ value: Number(k), label: v })) },
  { prop: 'location', label: '安装位置', type: 'input' as const },
  { prop: 'ipAddress', label: 'IP地址', type: 'input' as const },
  { prop: 'status', label: '状态', type: 'select' as const, options: Object.entries(statusMap).map(([k, v]) => ({ value: Number(k), label: v })) }
]

const handleSearch = (params: any) => { loadData(params) }

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getCameraList({ page: page.value, pageSize: pageSize.value, ...params })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增监控设备'
  Object.assign(formData, { cameraNo: '', cameraName: '', cameraType: 1, location: '', ipAddress: '', status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  dialogTitle.value = '编辑监控设备'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该设备？', '提示', { type: 'warning' })
  await deleteCamera(row.id!)
  ElMessage.success('删除成功')
  loadData()
}

const handleConfirm = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCamera(formData)
    } else {
      await createCamera(formData)
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