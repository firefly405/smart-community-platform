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
import { getAccessDeviceList, createAccessDevice, updateAccessDevice, deleteAccessDevice, type AccessDeviceItem } from '@/api/guard'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const isEdit = ref(false)

const accessTypeMap: Record<string, string> = { '1': '大门', '2': '单元门', '3': '电梯', '4': '车库' }
const statusMap: Record<number, string> = { 0: '离线', 1: '在线', 2: '故障' }

const columns = [
  { prop: 'accessNo', label: '门禁编号', width: 120 },
  { prop: 'accessName', label: '门禁名称', width: 150 },
  { prop: 'accessType', label: '门禁类型', width: 100, formatter: (_r: any, _c: any, v: number) => accessTypeMap[String(v)] || '-' },
  { prop: 'location', label: '安装位置', width: 150 },
  { prop: 'status', label: '状态', width: 80, formatter: (_r: any, _c: any, v: number) => statusMap[v] || '-' },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const searchFields = [
  { prop: 'status', label: '状态', type: 'select' as const, options: Object.entries(statusMap).map(([k, v]) => ({ value: Number(k), label: v })) }
]

const formData = reactive<AccessDeviceItem>({
  name: '',
  code: '',
  location: ''
} as any)

const formFields = [
  { prop: 'accessNo', label: '门禁编号', type: 'input' as const, required: true },
  { prop: 'accessName', label: '门禁名称', type: 'input' as const, required: true },
  { prop: 'accessType', label: '门禁类型', type: 'select' as const, options: Object.entries(accessTypeMap).map(([k, v]) => ({ value: Number(k), label: v })) },
  { prop: 'location', label: '安装位置', type: 'input' as const },
  { prop: 'ipAddress', label: 'IP地址', type: 'input' as const },
  { prop: 'status', label: '状态', type: 'select' as const, options: Object.entries(statusMap).map(([k, v]) => ({ value: Number(k), label: v })) }
]

const handleSearch = (params: any) => { loadData(params) }

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getAccessDeviceList({ page: page.value, pageSize: pageSize.value, ...params })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增门禁设备'
  Object.assign(formData, { accessNo: '', accessName: '', accessType: 1, location: '', ipAddress: '', status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  dialogTitle.value = '编辑门禁设备'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该设备？', '提示', { type: 'warning' })
  await deleteAccessDevice(row.id!)
  ElMessage.success('删除成功')
  loadData()
}

const handleConfirm = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateAccessDevice(formData)
    } else {
      await createAccessDevice(formData)
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