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
    >
      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'warning' : 'info'" size="small">
          {{ ['停用', '正常', '维修中', '报废'][row.status] || row.status }}
        </el-tag>
      </template>
    </ProTable>
    <ProDialog :visible="dialogVisible" :title="dialogTitle" :form-data="formData" :form-fields="formFields" :loading="submitting" width="550px" @confirm="handleConfirm" @cancel="dialogVisible = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import { getEquipmentList, createEquipment, updateEquipment, deleteEquipment, type EquipmentItem } from '@/api/property'

const loading = ref(false); const submitting = ref(false); const isEdit = ref(false)
const tableData = ref<any[]>([]); const total = ref(0); const page = ref(1); const pageSize = ref(10)
const dialogVisible = ref(false); const dialogTitle = ref('')

const columns = [
  { prop: 'equipmentNo', label: '设备编号', width: 130 }, { prop: 'equipmentName', label: '名称', width: 120 },
  { prop: 'equipmentType', label: '类型', width: 80 }, { prop: 'brand', label: '品牌', width: 80 },
  { prop: 'location', label: '位置', width: 120 }, { prop: 'status', label: '状态', width: 80, slot: 'status' }
]
const searchFields = [{ prop: 'equipmentName', label: '名称', type: 'input' as const }]

const formData = reactive<EquipmentItem>({ code: '', name: '', type: '', location: '', brand: '', model: '' })
const formFields = [
  { prop: 'equipmentName', label: '名称', type: 'input' as const, required: true }, { prop: 'equipmentType', label: '类型', type: 'select' as const, options: [
    { label: '电梯', value: 1 }, { label: '水泵', value: 2 }, { label: '消防', value: 3 }, { label: '监控', value: 4 }
  ]},
  { prop: 'brand', label: '品牌', type: 'input' as const }, { prop: 'location', label: '位置', type: 'input' as const }
]

const loadData = async (params?: any) => {
  loading.value = true
  try { const res = await getEquipmentList({ page: page.value, pageSize: pageSize.value, ...params }); tableData.value = res.records; total.value = res.total } finally { loading.value = false }
}
const handleSearch = (params: any) => loadData(params)
const handleAdd = () => { isEdit.value = false; dialogTitle.value = '新增设备'; Object.assign(formData, { equipmentNo: '', equipmentName: '', equipmentType: 1, brand: '', location: '' }); dialogVisible.value = true }
const handleEdit = (row: EquipmentItem) => { isEdit.value = true; dialogTitle.value = '编辑设备'; Object.assign(formData, row); dialogVisible.value = true }
const handleDelete = async (row: EquipmentItem) => { await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' }); await deleteEquipment(row.id!); ElMessage.success('删除成功'); loadData() }
const handleConfirm = async () => { submitting.value = true; try { if (isEdit.value) { await updateEquipment(formData) } else { await createEquipment(formData) }; ElMessage.success(isEdit.value ? '修改成功' : '新增成功'); dialogVisible.value = false; loadData() } finally { submitting.value = false } }
onMounted(() => loadData())
</script>