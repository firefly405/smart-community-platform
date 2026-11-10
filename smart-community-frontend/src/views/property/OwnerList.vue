<template>
  <div class="page-container property-page">
    <PropertyPageHeader>
      <template #meta><span>业主总数 <strong>{{ total }}</strong></span></template>
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
      :actions-width="230"
    >
      <template #gender="{ row }">{{ ['未知', '男', '女'][row.gender] || '未知' }}</template>
      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
          {{ row.status === 1 ? '在住' : '迁出' }}
        </el-tag>
      </template>
      <template #actions="{ row }">
        <el-button type="primary" link size="small" @click="openBindDialog(row)">绑定房屋</el-button>
      </template>
    </ProTable>

    <ProDialog
      :visible="dialogVisible"
      :title="dialogTitle"
      :form-data="formData"
      :form-fields="formFields"
      :loading="submitting"
      width="600px"
      @confirm="handleConfirm"
      @cancel="dialogVisible = false"
    />

    <el-dialog v-model="bindVisible" :title="`绑定房屋·${currentOwnerName}`" width="600px">
      <p class="bind-tip">勾选该业主名下房屋，保存后会同步更新房屋产权关联。</p>
      <el-checkbox-group v-if="allHouses.length" v-model="selectedHouseIds" class="house-options">
        <el-checkbox v-for="h in allHouses" :key="h.id" :value="h.id">
          {{ houseLabel(h) }}
        </el-checkbox>
      </el-checkbox-group>
      <el-empty v-else description="暂无可绑定房屋" :image-size="72" />
      <template #footer>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="binding" @click="handleBindSubmit">确认绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { useListQuery } from '@/composables/useListQuery'
const queryParams = useListQuery()
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import PropertyPageHeader from '@/components/PropertyPageHeader.vue'
import { getOwnerList, createOwner, updateOwner, deleteOwner, bindOwnerHouses, getOwnerHouseIds, getHouseList, type OwnerItem } from '@/api/house'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const isEdit = ref(false)
const bindVisible = ref(false)
const binding = ref(false)
const currentOwnerId = ref<number>(0)
const currentOwnerName = ref('')
const selectedHouseIds = ref<number[]>([])
const allHouses = ref<any[]>([])

const columns = [
  { prop: 'realName', label: '姓名', width: 100 },
  { prop: 'phone', label: '手机号', width: 120 },
  { prop: 'idCard', label: '身份证号', width: 170 },
  { prop: 'gender', label: '性别', width: 60, slot: 'gender' },
  { prop: 'status', label: '状态', width: 70, slot: 'status' },
  { prop: 'checkInDate', label: '入住日期', width: 110 }
]

const searchFields = [
  { prop: 'realName', label: '姓名', type: 'input' as const },
  { prop: 'phone', label: '手机号', type: 'input' as const },
  { prop: 'status', label: '状态', type: 'select' as const, options: [
    { label: '在住', value: 1 }, { label: '迁出', value: 0 }
  ]}
]

const formData = reactive<OwnerItem>({
  realName: '',
  phone: '',
  idCard: '',
  gender: 0,
  status: 1
})

const formFields = [
  { prop: 'realName', label: '姓名', type: 'input' as const, required: true },
  { prop: 'phone', label: '手机号', type: 'input' as const, required: true },
  { prop: 'idCard', label: '身份证号', type: 'input' as const },
  { prop: 'gender', label: '性别', type: 'select' as const, options: [
    { label: '未知', value: 0 }, { label: '男', value: 1 }, { label: '女', value: 2 }
  ]},
  { prop: 'email', label: '邮箱', type: 'input' as const },
  { prop: 'birthday', label: '出生日期', type: 'date' as const },
  { prop: 'nation', label: '民族', type: 'input' as const },
  { prop: 'nativePlace', label: '籍贯', type: 'input' as const },
  { prop: 'occupation', label: '职业', type: 'input' as const },
  { prop: 'workUnit', label: '工作单位', type: 'input' as const },
  { prop: 'checkInDate', label: '入住日期', type: 'date' as const },
  { prop: 'checkOutDate', label: '迁出日期', type: 'date' as const },
  { prop: 'status', label: '居住状态', type: 'select' as const, options: [
    { label: '在住', value: 1 }, { label: '迁出', value: 0 }
  ]},
  { prop: 'emergencyContact', label: '紧急联系人', type: 'input' as const },
  { prop: 'emergencyPhone', label: '紧急联系电话', type: 'input' as const },
  { prop: 'remark', label: '备注', type: 'textarea' as const, span: 24 }
]

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getOwnerList({ page: page.value, pageSize: pageSize.value, ...queryParams(params) })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleSearch = (params: any) => loadData(params)

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增业主'
  Object.assign(formData, { id: undefined, realName: '', phone: '', idCard: '', gender: 0, status: 1, email: '', birthday: '', nation: '', nativePlace: '', occupation: '', workUnit: '', checkInDate: '', checkOutDate: '', emergencyContact: '', emergencyPhone: '', remark: '' })
  dialogVisible.value = true
}

const handleEdit = (row: OwnerItem) => {
  isEdit.value = true
  dialogTitle.value = '编辑业主'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: OwnerItem) => {
  await ElMessageBox.confirm('确认删除该业主？', '提示', { type: 'warning' })
  await deleteOwner(row.id!)
  ElMessage.success('删除成功')
  loadData()
}

const handleConfirm = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateOwner(formData)
    } else {
      await createOwner(formData)
    }
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const houseLabel = (house: any) => [house.buildingName, house.unitName, house.houseNo].filter(Boolean).join(' ') || `房屋 #${house.id}`

const openBindDialog = async (row: OwnerItem) => {
  currentOwnerId.value = row.id!
  currentOwnerName.value = row.realName || '业主'
  binding.value = true
  bindVisible.value = true
  try {
    const [houses, houseIds] = await Promise.all([
      getHouseList({ page: 1, pageSize: 1000 }),
      getOwnerHouseIds(row.id!)
    ])
    allHouses.value = houses.records || []
    selectedHouseIds.value = houseIds || []
  } finally {
    binding.value = false
  }
}

const handleBindSubmit = async () => {
  binding.value = true
  try {
    await bindOwnerHouses(currentOwnerId.value, selectedHouseIds.value)
    ElMessage.success('绑定成功')
    bindVisible.value = false
    loadData()
  } finally {
    binding.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.bind-tip { margin: 0 0 14px; color: #667085; font-size: 13px; }
.house-options { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); max-height: 360px; overflow-y: auto; padding: 4px; gap: 8px 16px; }
.house-options :deep(.el-checkbox) { width: 100%; margin-right: 0; }
@media (max-width: 600px) { .house-options { grid-template-columns: 1fr; } }
</style>
