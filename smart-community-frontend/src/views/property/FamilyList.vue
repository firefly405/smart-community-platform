<template>
  <div class="page-container property-page">
    <PropertyPageHeader>
      <template #meta><span>家属数量 <strong>{{ total }}</strong></span></template>
    </PropertyPageHeader>

    <ProTable
      :data="tableData"
      :columns="columns"
      :total="total"
      :loading="loading"
      :page="page"
      :page-size="pageSize"
      :show-add="false"
      :show-pagination="false"
      :show-edit="true"
      :show-delete="true"
      @edit="handleEdit"
      @delete="handleDelete"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    >
      <template #toolbar>
        <el-select v-model="selectedOwnerId" placeholder="先选择业主" filterable clearable @change="loadData" style="width: 240px">
          <el-option v-for="o in owners" :key="o.id" :label="ownerLabel(o)" :value="o.id" />
        </el-select>
        <el-button type="primary" :disabled="!selectedOwnerId" @click="handleAdd"><el-icon><Plus /></el-icon>新增家属</el-button>
      </template>
      <template #gender="{ row }">{{ ['未知', '男', '女'][row.gender] || '未知' }}</template>
      <template #status="{ row }"><el-tag :type="row.status === 0 ? 'info' : 'success'" size="small">{{ row.status === 0 ? '已移除' : '有效' }}</el-tag></template>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import PropertyPageHeader from '@/components/PropertyPageHeader.vue'
import { getFamilyList, createFamily, updateFamily, deleteFamily, type FamilyItem } from '@/api/house'
import { getOwnerList } from '@/api/house'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const isEdit = ref(false)
const selectedOwnerId = ref<number>(Number(useRoute().query.ownerId) || 0)
const owners = ref<any[]>([])

const columns = [
  { prop: 'realName', label: '姓名', width: 100 },
  { prop: 'phone', label: '手机号', width: 120 },
  { prop: 'relation', label: '关系', width: 80 },
  { prop: 'gender', label: '性别', width: 70, slot: 'gender' },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'idCard', label: '身份证号', width: 170 },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const formData = reactive<FamilyItem>({
  ownerId: 0,
  realName: '',
  phone: '',
  relation: '',
  idCard: ''
})

const formFields = computed(() => [
  { prop: 'ownerId', label: '所属业主', type: 'select' as const, required: true, filterable: true, options: owners.value.map(o => ({ value: o.id, label: ownerLabel(o) })) },
  { prop: 'realName', label: '姓名', type: 'input' as const, required: true },
  { prop: 'phone', label: '手机号', type: 'input' as const },
  { prop: 'relation', label: '关系', type: 'input' as const },
  { prop: 'idCard', label: '身份证号', type: 'input' as const },
  { prop: 'gender', label: '性别', type: 'select' as const, options: [
    { label: '未知', value: 0 }, { label: '男', value: 1 }, { label: '女', value: 2 }
  ]},
  { prop: 'status', label: '档案状态', type: 'select' as const, options: [
    { label: '有效', value: 1 }, { label: '已移除', value: 0 }
  ]}
])

const ownerLabel = (owner: any) => `${owner.realName || '未命名'}${owner.phone ? `（${owner.phone}）` : ''}`

const loadOwners = async () => {
  const res = await getOwnerList({ page: 1, pageSize: 9999 })
  owners.value = res.records
}

const loadData = async () => {
  if (!selectedOwnerId.value) {
    tableData.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res = await getFamilyList(selectedOwnerId.value)
    tableData.value = Array.isArray(res) ? res : []
    total.value = tableData.value.length
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  if (!selectedOwnerId.value) {
    ElMessage.warning('请先选择业主')
    return
  }
  isEdit.value = false
  dialogTitle.value = '新增家属'
  Object.assign(formData, { id: undefined, ownerId: selectedOwnerId.value, realName: '', phone: '', relation: '', idCard: '', gender: 0, status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row: FamilyItem) => {
  isEdit.value = true
  dialogTitle.value = '编辑家属'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: FamilyItem) => {
  await ElMessageBox.confirm('确认删除该家属？', '提示', { type: 'warning' })
  await deleteFamily(row.id!)
  ElMessage.success('删除成功')
  loadData()
}

const handleConfirm = async () => {
  submitting.value = true
  try {
    if (isEdit.value) await updateFamily(formData)
    else await createFamily(formData)
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(() => { loadOwners(); loadData() })
</script>
