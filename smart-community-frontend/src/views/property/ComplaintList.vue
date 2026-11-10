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
      :search-fields="searchFields"
      @search="handleSearch"
      @add="handleAdd"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    >
      <template #status="{ row }">
        <el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.text }}</el-tag>
      </template>
      <template #actions="{ row }">
        <el-button v-if="row.status === 0" size="small" type="primary" link @click="handleAccept(row)">受理</el-button>
        <el-button v-if="row.status === 0 || row.status === 1" size="small" type="success" link @click="handleHandle(row)">办结</el-button>
        <el-button v-if="row.status === 2" size="small" type="info" link @click="handleClose(row)">关闭</el-button>
      </template>
    </ProTable>

    <ProDialog :visible="dialogVisible" :title="dialogTitle" :form-data="formData" :form-fields="formFields" :loading="submitting" @confirm="handleConfirm" @cancel="dialogVisible = false" />
    <ProDialog :visible="handleVisible" title="办结投诉" :form-data="handleForm" :form-fields="handleFields" :loading="submitting" @confirm="handleHandleConfirm" @cancel="handleVisible = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import { getComplaintList, createComplaint, handleComplaint, acceptComplaint, closeComplaint, type ComplaintItem } from '@/api/property'

const loading = ref(false); const submitting = ref(false)
const tableData = ref<any[]>([]); const total = ref(0); const page = ref(1); const pageSize = ref(10)
const dialogVisible = ref(false); const dialogTitle = ref(''); const handleVisible = ref(false); const currentId = ref(0)

const statusMap: Record<number, { text: string; type: 'success' | 'info' | 'primary' | 'warning' | 'danger' }> = {
  0: { text: '待处理', type: 'warning' },
  1: { text: '处理中', type: 'primary' },
  2: { text: '已完成', type: 'success' },
  3: { text: '已关闭', type: 'info' }
}

const columns = [
  { prop: 'complaintNo', label: '投诉单号', width: 140 },
  { prop: 'title', label: '标题', width: 150 },
  { prop: 'ownerName', label: '业主', width: 100 },
  { prop: 'houseNo', label: '房号', width: 100 },
  { prop: 'complaintType', label: '类型', width: 80 },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'handlerName', label: '处理人', width: 100 }
]
const searchFields = [{ prop: 'ownerId', label: '业主ID', type: 'number' as const }]

const formData = reactive<ComplaintItem>({ title: '', content: '', complaintType: 1, houseId: 0, contactName: '', contactPhone: '' })
const formFields = [
  { prop: 'title', label: '标题', type: 'input' as const, required: true },
  { prop: 'content', label: '内容', type: 'textarea' as const },
  { prop: 'contactName', label: '联系人', type: 'input' as const },
  { prop: 'contactPhone', label: '联系电话', type: 'input' as const }
]

const handleForm = reactive({ handlerId: 0, handlerName: '', result: '', reply: '' })
const handleFields = [
  { prop: 'handlerId', label: '处理人ID', type: 'number' as const },
  { prop: 'handlerName', label: '处理人姓名', type: 'input' as const },
  { prop: 'result', label: '处理结果', type: 'textarea' as const },
  { prop: 'reply', label: '回复内容', type: 'textarea' as const }
]

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getComplaintList({ page: page.value, pageSize: pageSize.value, ...params })
    tableData.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}
const handleSearch = (params: any) => loadData(params)
const handleAdd = () => { dialogTitle.value = '创建投诉'; Object.assign(formData, { title: '', content: '', contactName: '', contactPhone: '' }); dialogVisible.value = true }
const handleConfirm = async () => {
  submitting.value = true
  try {
    await createComplaint(formData)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}
const handleAccept = async (row: ComplaintItem) => {
  await acceptComplaint(row.id!, { handlerName: '物业客服' })
  ElMessage.success('已受理，进入处理中')
  loadData()
}
const handleHandle = (row: ComplaintItem) => {
  currentId.value = row.id!
  Object.assign(handleForm, { handlerId: 0, handlerName: row.handlerName || '', result: '', reply: '' })
  handleVisible.value = true
}
const handleHandleConfirm = async () => {
  submitting.value = true
  try {
    await handleComplaint(currentId.value, handleForm as any)
    ElMessage.success('已办结')
    handleVisible.value = false
    loadData()
  } finally { submitting.value = false }
}
const handleClose = async (row: ComplaintItem) => {
  await closeComplaint(row.id!)
  ElMessage.success('已关闭')
  loadData()
}
onMounted(() => loadData())
</script>
