<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">新增访客</el-button>
    </div>

    <ProTable
      :data="tableData"
      :columns="columns"
      :total="total"
      :loading="loading"
      :page="page"
      :page-size="pageSize"
      :show-add="false"
      :show-edit="false"
      :show-delete="false"
      :search-fields="searchFields"
      :actions-width="280"
      @search="handleSearch"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    >
      <template #actions="{ row }">
        <el-button type="success" size="small" link @click="handleApprove(row, true)" :disabled="row.status !== 0">通过</el-button>
        <el-button type="danger" size="small" link @click="handleApprove(row, false)" :disabled="row.status !== 0">拒绝</el-button>
        <el-button type="primary" size="small" link @click="openVerify(row)" :disabled="row.status !== 1 || !row.qrcode">核销</el-button>
        <el-button type="warning" size="small" link @click="handleBlacklist(row)" :disabled="row.isBlacklist === 1">加黑名单</el-button>
      </template>
    </ProTable>

    <ProDialog
      :visible="dialogVisible"
      title="新增访客"
      :form-data="formData"
      :form-fields="formFields"
      :loading="submitting"
      @confirm="doCreate"
      @cancel="dialogVisible = false"
    />

    <el-dialog v-model="verifyVisible" title="访客核销" width="480px">
      <el-form label-width="100px">
        <el-form-item label="访客">
          <span>{{ verifyForm.visitorName || '-' }}</span>
        </el-form-item>
        <el-form-item label="二维码">
          <el-input v-model="verifyForm.qrcode" readonly />
        </el-form-item>
        <el-form-item label="门禁设备" required>
          <el-select v-model="verifyForm.accessId" placeholder="请选择核销门禁设备" filterable style="width: 100%">
            <el-option
              v-for="d in deviceOptions"
              :key="d.id"
              :label="`${d.accessName || d.accessNo || d.name || ('设备' + d.id)} (${d.location || '未设置位置'})`"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="verifyVisible = false">取消</el-button>
        <el-button type="primary" :loading="verifying" @click="doVerify">确认核销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { useListQuery } from '@/composables/useListQuery'
const queryParams = useListQuery()
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import {
  getVisitorList,
  createVisitor,
  approveVisitor,
  addBlacklist,
  verifyVisitor,
  getAccessDeviceList,
  type VisitorItem
} from '@/api/guard'
import { getOwnerList } from '@/api/house'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const ownerOptions = ref<{ label: string; value: number }[]>([])

const statusMap: Record<number, string> = {
  0: '待审核', 1: '已通过', 2: '已拒绝', 3: '已签到', 4: '已签退', 5: '已过期'
}

const columns = [
  { prop: 'visitorNo', label: '访客单号', width: 180 },
  { prop: 'visitorName', label: '访客姓名', width: 100 },
  { prop: 'visitorPhone', label: '访客手机', width: 130 },
  { prop: 'ownerName', label: '业主', width: 100, formatter: (_r: any, _c: any, v: string) => v || '-' },
  { prop: 'visitReason', label: '来访事由', width: 150 },
  { prop: 'visitStartTime', label: '开始时间', width: 170 },
  { prop: 'visitEndTime', label: '结束时间', width: 170 },
  { prop: 'status', label: '状态', width: 90, formatter: (_r: any, _c: any, v: number) => statusMap[v] || '-' },
  { prop: 'qrcode', label: '通行码', minWidth: 160, formatter: (_r: any, _c: any, v: string) => v || '-' }
]

const searchFields = [
  { prop: 'status', label: '状态', type: 'select' as const, options: Object.entries(statusMap).map(([k, v]) => ({ value: Number(k), label: v })) }
]

const dialogVisible = ref(false)
const submitting = ref(false)
const formData = reactive({ visitorName: '', visitorPhone: '', ownerId: undefined as number | undefined, visitStartTime: '', visitEndTime: '', visitReason: '' })
const formFields = computed(() => [
  { prop: 'visitorName', label: '访客姓名', type: 'input' as const, required: true },
  { prop: 'visitorPhone', label: '访客手机', type: 'input' as const, required: true },
  { prop: 'ownerId', label: '业主', type: 'select' as const, required: true, filterable: true, options: ownerOptions.value },
  { prop: 'visitStartTime', label: '开始时间', type: 'datetime' as const },
  { prop: 'visitEndTime', label: '结束时间', type: 'datetime' as const },
  { prop: 'visitReason', label: '来访事由', type: 'input' as const }
])

const verifyVisible = ref(false)
const verifying = ref(false)
const deviceOptions = ref<any[]>([])
const verifyForm = reactive({ visitorName: '', qrcode: '', accessId: undefined as number | undefined })

const loadOwners = async () => {
  const res = await getOwnerList({ page: 1, pageSize: 500 })
  ownerOptions.value = (res.records || []).map((o: any) => ({
    value: o.id,
    label: `${o.realName || o.name || '业主'}${o.phone ? ' / ' + o.phone : ''} (#${o.id})`
  }))
}

const handleAdd = () => {
  Object.assign(formData, { visitorName: '', visitorPhone: '', ownerId: undefined, visitStartTime: '', visitEndTime: '', visitReason: '' })
  dialogVisible.value = true
}

const doCreate = async () => {
  if (!formData.ownerId) {
    ElMessage.warning('请选择业主')
    return
  }
  submitting.value = true
  try {
    await createVisitor(formData as unknown as VisitorItem)
    ElMessage.success('新增成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleApprove = async (row: any, approved: boolean) => {
  await approveVisitor(row.id, approved)
  ElMessage.success(approved ? '已通过' : '已拒绝')
  loadData()
}

const loadDevices = async () => {
  const res: any = await getAccessDeviceList({ page: 1, pageSize: 200 })
  deviceOptions.value = res.records || res || []
}

const openVerify = async (row: any) => {
  if (!row.qrcode) {
    ElMessage.warning('该访客尚未生成通行码')
    return
  }
  if (!deviceOptions.value.length) {
    await loadDevices()
  }
  if (!deviceOptions.value.length) {
    ElMessage.error('系统中暂无门禁设备，请先录入设备后再核销')
    return
  }
  verifyForm.visitorName = row.visitorName
  verifyForm.qrcode = row.qrcode
  verifyForm.accessId = deviceOptions.value[0]?.id
  verifyVisible.value = true
}

const doVerify = async () => {
  if (!verifyForm.accessId) {
    ElMessage.warning('请选择门禁设备')
    return
  }
  if (!verifyForm.qrcode) {
    ElMessage.warning('缺少通行码')
    return
  }
  verifying.value = true
  try {
    await verifyVisitor(verifyForm.qrcode, verifyForm.accessId)
    ElMessage.success('核销成功，已写入通行记录')
    verifyVisible.value = false
    loadData()
  } finally {
    verifying.value = false
  }
}

const handleBlacklist = async (row: any) => {
  await addBlacklist({ name: row.visitorName, reason: '违规', phone: row.visitorPhone, idCard: row.visitorIdCard, visitorId: row.id } as any)
  ElMessage.success('已加入黑名单')
  loadData()
}

const handleSearch = (params: any) => { loadData(params) }

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getVisitorList({ page: page.value, pageSize: pageSize.value, ...queryParams(params) })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
  loadOwners().catch(() => {})
  loadDevices().catch(() => {})
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
