<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="success" @click="handleGenerate">生成账单</el-button>
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
        <el-button type="primary" size="small" link @click="handlePay(row)" :disabled="row.status === 2 || row.status === 3">缴费</el-button>
        <el-button type="warning" size="small" link @click="handleRemind(row)">催缴</el-button>
        <el-button type="danger" size="small" link @click="handleCancel(row)" :disabled="row.status === 2 || row.status === 3">作废</el-button>
      </template>
    </ProTable>

    <!-- 生成账单弹窗 -->
    <el-dialog v-model="generateVisible" title="生成账单" width="500px">
      <el-form :model="generateForm" label-width="100px">
        <el-form-item label="账期">
          <el-input v-model="generateForm.period" placeholder="如 2026-06" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" @click="doGenerate" :loading="generating">确认生成</el-button>
      </template>
    </el-dialog>

    <!-- 缴费弹窗（经统一支付网关；默认 mock 通道，非真实资金） -->
    <el-dialog v-model="payVisible" title="缴费（模拟支付）" width="500px">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="当前为模拟支付通道，不会发生真实资金流转。生产环境请关闭 payment.mock 或接入真实通道。"
        style="margin-bottom: 12px"
      />
      <el-descriptions :column="2" border>
        <el-descriptions-item label="账单号">{{ payRow?.billNo }}</el-descriptions-item>
        <el-descriptions-item label="应付金额">{{ payRow?.payableAmount }}</el-descriptions-item>
        <el-descriptions-item label="已付金额">{{ payRow?.paidAmount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="待付金额">{{ (payRow?.payableAmount || 0) - (payRow?.paidAmount || 0) }}</el-descriptions-item>
      </el-descriptions>
      <el-form :model="payForm" label-width="100px" style="margin-top: 16px">
        <el-form-item label="缴费金额" required>
          <el-input-number v-model="payForm.amount" :min="0.01" :max="(payRow?.payableAmount || 0) - (payRow?.paidAmount || 0)" />
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="payForm.payMethod" placeholder="请选择">
            <el-option label="微信（演示）" :value="1" />
            <el-option label="支付宝（演示）" :value="2" />
            <el-option label="现金（演示）" :value="3" />
            <el-option label="银行转账（演示）" :value="4" />
            <el-option label="模拟支付" :value="9" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" @click="doPay" :loading="paying">确认模拟缴费</el-button>
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
import { getBillList, generateBills, payBill, remindBill, cancelBill } from '@/api/fee'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const statusMap: Record<number, string> = { 0: '未缴费', 1: '部分缴费', 2: '已缴清', 3: '已作废' }

const columns = [
  { prop: 'billNo', label: '账单号', width: 180 },
  { prop: 'ownerName', label: '业主', width: 100 },
  { prop: 'houseNo', label: '房号', width: 100 },
  { prop: 'itemName', label: '费用项目', width: 120 },
  { prop: 'billPeriod', label: '账期', width: 100 },
  { prop: 'payableAmount', label: '应付金额', width: 100 },
  { prop: 'paidAmount', label: '已付金额', width: 100 },
  { prop: 'status', label: '状态', width: 100, formatter: (_r: any, _c: any, v: number) => statusMap[v] || '-' },
  { prop: 'payTime', label: '支付时间', width: 170 },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const searchFields = [
  { prop: 'houseId', label: '房屋ID', type: 'input' as const },
  { prop: 'billPeriod', label: '账期', type: 'input' as const },
  { prop: 'status', label: '状态', type: 'select' as const, options: Object.entries(statusMap).map(([k, v]) => ({ value: Number(k), label: v })) }
]

// 生成账单
const generateVisible = ref(false)
const generating = ref(false)
const generateForm = reactive({ period: '' })
const handleGenerate = () => { generateVisible.value = true }
const doGenerate = async () => {
  generating.value = true
  try {
    await generateBills({ period: generateForm.period })
    ElMessage.success('账单生成成功')
    generateVisible.value = false
    loadData()
  } finally {
    generating.value = false
  }
}

// 缴费
const payVisible = ref(false)
const paying = ref(false)
const payRow = ref<any>(null)
const payForm = reactive({ amount: 0, payMethod: 1 })
const handlePay = (row: any) => {
  payRow.value = row
  payForm.amount = (row.payableAmount || 0) - (row.paidAmount || 0)
  payForm.payMethod = 1
  payVisible.value = true
}
const doPay = async () => {
  paying.value = true
  try {
    const res: any = await payBill(payRow.value.id, { amount: payForm.amount, payMethod: String(payForm.payMethod) })
    const simulated = res?.simulated !== false
    ElMessage.success(simulated ? `模拟缴费成功（交易号 ${res?.tradeNo || '-'}）` : '缴费成功')
    payVisible.value = false
    loadData()
  } finally {
    paying.value = false
  }
}

// 催缴
const handleRemind = async (row: any) => {
  await remindBill(row.id)
  ElMessage.success('已发送催缴通知')
}

// 作废
const handleCancel = async (row: any) => {
  await ElMessageBox.confirm('确认作废该账单？', '提示', { type: 'warning' })
  await cancelBill(row.id)
  ElMessage.success('已作废')
  loadData()
}

const handleSearch = (params: any) => { loadData(params) }

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getBillList({ page: page.value, pageSize: pageSize.value, ...queryParams(params) })
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
