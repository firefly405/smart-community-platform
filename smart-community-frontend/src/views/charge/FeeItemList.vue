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
import { getFeeItemList, createFeeItem, updateFeeItem, deleteFeeItem, type FeeItem } from '@/api/fee'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const isEdit = ref(false)

const itemTypeMap: Record<number, string> = { 1: '物业费', 2: '停车费', 3: '水电费', 4: '其他' }
const calcTypeMap: Record<number, string> = { 1: '按面积', 2: '按套', 3: '固定金额' }

const columns = [
  { prop: 'itemCode', label: '项目编码', width: 120 },
  { prop: 'itemName', label: '项目名称', width: 150 },
  { prop: 'itemType', label: '费用类型', width: 100, formatter: (_r: any, _c: any, v: number) => itemTypeMap[v] || '-' },
  { prop: 'unitPrice', label: '单价', width: 100 },
  { prop: 'unit', label: '计量单位', width: 100 },
  { prop: 'calcType', label: '计算方式', width: 100, formatter: (_r: any, _c: any, v: number) => calcTypeMap[v] || '-' },
  { prop: 'status', label: '状态', width: 80, formatter: (_r: any, _c: any, v: number) => v === 1 ? '启用' : '停用' },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const searchFields = [
  { prop: 'itemName', label: '项目名称', type: 'input' as const }
]

const formData = reactive<FeeItem>({
  name: '',
  code: '',
  price: 0
} as any)

const formFields = [
  { prop: 'itemCode', label: '项目编码', type: 'input' as const, required: true },
  { prop: 'itemName', label: '项目名称', type: 'input' as const, required: true },
  { prop: 'itemType', label: '费用类型', type: 'select' as const, options: Object.entries(itemTypeMap).map(([k, v]) => ({ value: Number(k), label: v })) },
  { prop: 'unitPrice', label: '单价', type: 'number' as const },
  { prop: 'unit', label: '计量单位', type: 'input' as const },
  { prop: 'calcType', label: '计算方式', type: 'select' as const, options: Object.entries(calcTypeMap).map(([k, v]) => ({ value: Number(k), label: v })) },
  { prop: 'status', label: '状态', type: 'select' as const, options: [{ value: 1, label: '启用' }, { value: 0, label: '停用' }] }
]

const handleSearch = (params: any) => { loadData(params) }

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getFeeItemList({ page: page.value, pageSize: pageSize.value, ...params })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增费用项目'
  Object.assign(formData, { itemCode: '', itemName: '', itemType: 1, unitPrice: 0, unit: '', calcType: 1, status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  dialogTitle.value = '编辑费用项目'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该费用项目？', '提示', { type: 'warning' })
  await deleteFeeItem(row.id!)
  ElMessage.success('删除成功')
  loadData()
}

const handleConfirm = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateFeeItem(formData as FeeItem)
    } else {
      await createFeeItem(formData as FeeItem)
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