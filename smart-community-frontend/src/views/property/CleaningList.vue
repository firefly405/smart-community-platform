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
      @search="handleSearch"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    />
    <ProDialog :visible="dialogVisible" :title="dialogTitle" :form-data="formData" :form-fields="formFields" :loading="submitting" @confirm="handleConfirm" @cancel="dialogVisible = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import { getCleaningList, createCleaning, updateCleaning, deleteCleaning } from '@/api/property'

const loading = ref(false); const submitting = ref(false); const isEdit = ref(false)
const tableData = ref<any[]>([]); const total = ref(0); const page = ref(1); const pageSize = ref(10)
const dialogVisible = ref(false); const dialogTitle = ref('')

const columns = [
  { prop: 'cleanerName', label: '保洁人员', width: 100 }, { prop: 'area', label: '区域', width: 120 },
  { prop: 'taskDate', label: '日期', width: 110 }, { prop: 'taskType', label: '类型', width: 80 },
  { prop: 'status', label: '状态', width: 80 }
]

const formData = reactive({ cleanerId: 0, cleanerName: '', area: '', taskDate: '', taskType: 1, taskContent: '' })
const formFields = [
  { prop: 'cleanerName', label: '保洁人员', type: 'input' as const, required: true },
  { prop: 'area', label: '区域', type: 'input' as const, required: true },
  { prop: 'taskDate', label: '日期', type: 'date' as const }, { prop: 'taskContent', label: '任务内容', type: 'textarea' as const }
]

const loadData = async (params?: any) => {
  loading.value = true
  try { const res = await getCleaningList({ page: page.value, pageSize: pageSize.value, ...params }); tableData.value = res.records; total.value = res.total } finally { loading.value = false }
}
const handleSearch = (params: any) => loadData(params)
const handleAdd = () => { isEdit.value = false; dialogTitle.value = '新增保洁任务'; Object.assign(formData, { cleanerName: '', area: '', taskDate: '', taskType: 1, taskContent: '' }); dialogVisible.value = true }
const handleEdit = (row: any) => { isEdit.value = true; dialogTitle.value = '编辑保洁任务'; Object.assign(formData, row); dialogVisible.value = true }
const handleDelete = async (row: any) => { await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' }); await deleteCleaning(row.id!); ElMessage.success('删除成功'); loadData() }
const handleConfirm = async () => { submitting.value = true; try { if (isEdit.value) { await updateCleaning(formData) } else { await createCleaning(formData) }; ElMessage.success(isEdit.value ? '修改成功' : '新增成功'); dialogVisible.value = false; loadData() } finally { submitting.value = false } }
onMounted(() => loadData())
</script>