<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="handleApply">发起退款</el-button>
    </div>

    <ProTable
      :data="tableData"
      :columns="columns"
      :total="total"
      :loading="loading"
      :page="page"
      :page-size="pageSize"
      :search-fields="searchFields"
      @search="handleSearch"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    >
      <template #actions="{ row }">
        <el-button type="success" size="small" link @click="handleApprove(row, true)" :disabled="row.status !== 0">通过</el-button>
        <el-button type="danger" size="small" link @click="handleApprove(row, false)" :disabled="row.status !== 0">驳回</el-button>
      </template>
    </ProTable>

    <!-- 申请退款弹窗 -->
    <ProDialog
      :visible="applyVisible"
      title="申请退款"
      :form-data="applyForm"
      :form-fields="applyFields"
      :loading="applying"
      @confirm="doApply"
      @cancel="applyVisible = false"
    />

    <!-- 审批驳回弹窗 -->
    <el-dialog v-model="rejectVisible" title="驳回原因" width="400px">
      <el-input v-model="rejectReason" placeholder="请输入驳回原因" type="textarea" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="doReject">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import { getRefundList, applyRefund, approveRefund, type RefundItem } from '@/api/fee'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const refundTypeMap: Record<number, string> = { 1: '押金退款', 2: '费用退款', 3: '其他' }
const statusMap: Record<number, string> = { 0: '待审批', 1: '已通过', 2: '已退款', 3: '已驳回' }

const columns = [
  { prop: 'refundNo', label: '退款单号', width: 180 },
  { prop: 'ownerName', label: '业主', width: 100 },
  { prop: 'refundType', label: '退款类型', width: 100, formatter: (_r: any, _c: any, v: number) => refundTypeMap[v] || '-' },
  { prop: 'amount', label: '退款金额', width: 100 },
  { prop: 'status', label: '状态', width: 100, formatter: (_r: any, _c: any, v: number) => statusMap[v] || '-' },
  { prop: 'applyReason', label: '申请原因', width: 200 },
  { prop: 'rejectReason', label: '驳回原因', width: 200 },
  { prop: 'approveTime', label: '审批时间', width: 170 },
  { prop: 'createTime', label: '申请时间', width: 170 }
]

const searchFields = [
  { prop: 'status', label: '状态', type: 'select' as const, options: Object.entries(statusMap).map(([k, v]) => ({ value: Number(k), label: v })) }
]

// 申请退款
const applyVisible = ref(false)
const applying = ref(false)
const applyForm = reactive({ amount: 0, reason: '', depositId: 0, ownerId: 0, refundType: 1 })
const applyFields = [
  { prop: 'ownerId', label: '业主ID', type: 'number' as const, required: true },
  { prop: 'depositId', label: '关联押金ID', type: 'number' as const },
  { prop: 'refundType', label: '退款类型', type: 'select' as const, options: Object.entries(refundTypeMap).map(([k, v]) => ({ value: Number(k), label: v })) },
  { prop: 'amount', label: '退款金额', type: 'number' as const, required: true },
  { prop: 'reason', label: '申请原因', type: 'textarea' as const }
]

const handleApply = () => {
  applyVisible.value = true
  Object.assign(applyForm, { amount: 0, reason: '', depositId: 0, ownerId: 0, refundType: 1 })
}

const doApply = async () => {
  applying.value = true
  try {
    await applyRefund(applyForm as RefundItem)
    ElMessage.success('退款申请已提交')
    applyVisible.value = false
    loadData()
  } finally {
    applying.value = false
  }
}

// 审批
const rejectVisible = ref(false)
const rejectReason = ref('')
const pendingApproveId = ref(0)
const pendingApproved = ref(false)

const handleApprove = (row: any, approved: boolean) => {
  pendingApproveId.value = row.id
  pendingApproved.value = approved
  if (approved) {
    approveRefund(row.id, true).then(() => {
      ElMessage.success('已通过')
      loadData()
    })
  } else {
    rejectReason.value = ''
    rejectVisible.value = true
  }
}

const doReject = async () => {
  try {
    await approveRefund(pendingApproveId.value, false, rejectReason.value)
    ElMessage.success('已驳回')
    rejectVisible.value = false
    loadData()
  } catch {
    // handled by interceptor
  }
}

const handleSearch = (params: any) => { loadData(params) }

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getRefundList({ page: page.value, pageSize: pageSize.value, ...params })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>