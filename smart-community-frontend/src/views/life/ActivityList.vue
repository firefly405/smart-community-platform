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
      @search="handleSearch"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
      @update:page="(p: number) => { page = p; loadData() }"
      @update:page-size="(s: number) => { pageSize = s; loadData() }"
    >
      <template #status="{ row }">
        <el-tag :type="activityStatusMap[row.status]?.type || 'info'" size="small">
          {{ activityStatusMap[row.status]?.label || '未知' }}
        </el-tag>
      </template>
      <template #participants="{ row }">
        {{ row.currentParticipants || 0 }} / {{ row.maxParticipants || '不限' }}
      </template>
      <template #actions="{ row }">
        <el-button link type="primary" @click="handleJoin(row)">报名</el-button>
        <el-button link type="info" @click="viewJoins(row)">报名列表</el-button>
      </template>
    </ProTable>

    <ProDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :fields="formFields"
      :edit-data="editData"
      :loading="submitting"
      @confirm="handleSubmit"
      @close="editData = null"
    />

    <el-dialog v-model="joinDialogVisible" title="活动报名" width="420px">
      <el-form label-width="90px">
        <el-form-item label="用户ID">
          <el-input-number v-model="joinForm.userId" :min="1" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="joinForm.userName" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="joinDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmJoin">确认报名</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="joinsVisible" title="报名列表" width="640px">
      <el-table :data="joinList" border stripe v-loading="joinsLoading">
        <el-table-column prop="userName" label="姓名" width="120" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="joinTime" label="报名时间" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="danger" @click="cancelJoin(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import type { FormField } from '@/components/ProForm.vue'
import {
  getActivityList, createActivity, updateActivity, deleteActivity,
  joinActivity, getActivityJoinList, cancelActivityJoin
} from '@/api/life'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('新增活动')
const editData = ref<any>(null)
const joinDialogVisible = ref(false)
const joinsVisible = ref(false)
const joinsLoading = ref(false)
const currentActivityId = ref(0)
const joinList = ref<any[]>([])
const joinForm = ref({ userId: 1, userName: '' })

const activityStatusMap: Record<number, { label: string; type: 'info' | 'primary' | 'success' | 'warning' | 'danger' }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '已发布', type: 'primary' },
  2: { label: '进行中', type: 'success' },
  3: { label: '已结束', type: 'info' },
  4: { label: '已取消', type: 'danger' }
}

const columns = [
  { prop: 'title', label: '活动标题', width: 180 },
  { prop: 'location', label: '地点', width: 140 },
  { prop: 'startTime', label: '开始时间', width: 170 },
  { prop: 'endTime', label: '结束时间', width: 170 },
  { prop: 'participants', label: '报名人数', width: 100, slot: 'participants' },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const formFields: FormField[] = [
  { prop: 'title', label: '活动标题', required: true },
  { prop: 'content', label: '活动内容', type: 'textarea' },
  { prop: 'location', label: '地点' },
  { prop: 'startTime', label: '开始时间', type: 'datetime' },
  { prop: 'endTime', label: '结束时间', type: 'datetime' },
  { prop: 'maxParticipants', label: '人数上限', type: 'number' }
]

async function loadData(searchParams?: any) {
  loading.value = true
  try {
    const res = await getActivityList({ page: page.value, pageSize: pageSize.value, ...searchParams })
    tableData.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}
function handleSearch(params: any) {
  page.value = params.page
  pageSize.value = params.pageSize
  loadData(params)
}
function handleAdd() {
  editData.value = null
  dialogTitle.value = '新增活动'
  dialogVisible.value = true
}
function handleEdit(row: any) {
  editData.value = { ...row }
  dialogTitle.value = '编辑活动'
  dialogVisible.value = true
}
async function handleSubmit(data: any) {
  submitting.value = true
  try {
    if (data.id) {
      await updateActivity(data)
      ElMessage.success('修改成功')
    } else {
      await createActivity({ ...data, currentParticipants: 0, status: 1 })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    editData.value = null
    loadData()
  } finally { submitting.value = false }
}
async function handleDelete(row: any) {
  await deleteActivity(row.id)
  ElMessage.success('删除成功')
  loadData()
}
function handleJoin(row: any) {
  currentActivityId.value = row.id
  joinForm.value = { userId: 1, userName: '' }
  joinDialogVisible.value = true
}
async function confirmJoin() {
  submitting.value = true
  try {
    await joinActivity(currentActivityId.value, {
      userId: joinForm.value.userId,
      userName: joinForm.value.userName || `用户${joinForm.value.userId}`
    })
    ElMessage.success('报名成功')
    joinDialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}
async function viewJoins(row: any) {
  currentActivityId.value = row.id
  joinsVisible.value = true
  joinsLoading.value = true
  try {
    const res = await getActivityJoinList(row.id)
    joinList.value = (res as any).records || res || []
  } finally { joinsLoading.value = false }
}
async function cancelJoin(row: any) {
  await cancelActivityJoin(row.id)
  ElMessage.success('已取消报名')
  const res = await getActivityJoinList(currentActivityId.value)
  joinList.value = (res as any).records || res || []
  loadData()
}
onMounted(() => loadData())
</script>
