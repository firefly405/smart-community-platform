<template>
  <div class="page-container">
    <ProTable :data="rows" :columns="columns" :total="total" :loading="loading"
      :page="page" :page-size="pageSize" :search-fields="searchFields"
      @search="load" @add="add" @edit="edit">
      <template #actions="{ row }">
        <el-button v-if="row.status !== 2" link type="success" @click="complete(row.id)">完成维保</el-button>
      </template>
    </ProTable>
    <ProDialog :visible="visible" :title="form.id ? '编辑维保记录' : '新增维保记录'"
      :form-data="form" :form-fields="fields" :loading="saving"
      @confirm="save" @cancel="visible = false" />
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import { useListQuery } from '@/composables/useListQuery'
import { getEquipmentList, getMaintainList, createMaintain, updateMaintain, completeMaintain, type MaintainItem } from '@/api/property'
const queryParams = useListQuery()
const rows = ref<MaintainItem[]>([])
const equipment = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const form = ref<MaintainItem>({ equipmentId: 0, maintainType: 1, maintainContent: '', status: 0 })
const statuses = ['待维保', '进行中', '已完成']
const columns = [
  { prop: 'equipmentId', label: '设备', minWidth: 130, formatter: (row: MaintainItem) => equipment.value.find(e => e.id === row.equipmentId)?.equipmentName || '#' + row.equipmentId },
  { prop: 'maintainContent', label: '维保内容', minWidth: 180 },
  { prop: 'maintainerName', label: '维保人员', width: 110 },
  { prop: 'maintainDate', label: '维保日期', width: 120 },
  { prop: 'nextMaintainDate', label: '下次维保', width: 120 },
  { prop: 'cost', label: '费用', width: 90 },
  { prop: 'status', label: '状态', width: 90, formatter: (r: MaintainItem) => statuses[r.status ?? 0] || '-' }
]
const searchFields = [{ prop: 'status', label: '状态', type: 'select' as const, options: statuses.map((label, value) => ({ label, value })) }]
const fields = computed(() => [
  { prop: 'equipmentId', label: '设备', type: 'select' as const, required: true, options: equipment.value.map(e => ({ label: e.equipmentName, value: e.id })) },
  { prop: 'maintainType', label: '类型', type: 'select' as const, options: [{ label: '日常保养', value: 1 }, { label: '维修', value: 2 }, { label: '检测', value: 3 }] },
  { prop: 'maintainContent', label: '维保内容', type: 'textarea' as const, required: true },
  { prop: 'maintainerName', label: '维保人员', type: 'input' as const },
  { prop: 'maintainDate', label: '维保日期', type: 'date' as const, required: true },
  { prop: 'maintainResult', label: '维保结果', type: 'textarea' as const },
  { prop: 'cost', label: '费用', type: 'number' as const, min: 0 },
  { prop: 'nextMaintainDate', label: '下次维保', type: 'date' as const }
])
async function load(params?: any) {
  const query = queryParams(params)
  page.value = Number(query.page || page.value)
  pageSize.value = Number(query.pageSize || pageSize.value)
  loading.value = true
  try {
    const result = await getMaintainList({ page: page.value, pageSize: pageSize.value, ...query })
    rows.value = result.records
    total.value = result.total
  } finally { loading.value = false }
}
function add() {
  form.value = { equipmentId: Number(queryParams().equipmentId) || 0, maintainType: 1, maintainContent: '', status: 0 }
  visible.value = true
}
function edit(row: MaintainItem) { form.value = { ...row }; visible.value = true }
async function save() {
  if (!form.value.equipmentId || !form.value.maintainContent?.trim() || !form.value.maintainDate) {
    ElMessage.warning('请选择设备并填写维保内容和日期')
    return
  }
  saving.value = true
  try {
    await (form.value.id ? updateMaintain(form.value) : createMaintain(form.value))
    visible.value = false
    ElMessage.success('维保记录已保存')
    await load()
  } finally { saving.value = false }
}
async function complete(id: number) {
  try { await ElMessageBox.confirm('确认已完成本次维保？', '完成维保') } catch { return }
  await completeMaintain(id)
  ElMessage.success('维保已完成')
  await load()
}
onMounted(async () => {
  const result = await getEquipmentList({ page: 1, pageSize: 500 })
  equipment.value = result.records
  await load()
})
</script>
