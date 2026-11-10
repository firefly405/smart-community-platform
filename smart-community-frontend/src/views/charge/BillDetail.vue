<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>账单详情 - {{ bill.billNo }}</span>
        </div>
      </template>

      <el-descriptions v-if="bill.id" :column="2" border>
        <el-descriptions-item label="账单编号">{{ bill.billNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[bill.status]?.type" size="small">{{ statusMap[bill.status]?.text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="房号">{{ bill.houseNo || (bill.houseId ? ('#' + bill.houseId) : '-') }}</el-descriptions-item>
        <el-descriptions-item label="业主">{{ bill.ownerName || (bill.ownerId ? ('#' + bill.ownerId) : '-') }}</el-descriptions-item>
        <el-descriptions-item label="费用项目">{{ bill.itemName }}</el-descriptions-item>
        <el-descriptions-item label="账单周期">{{ bill.billPeriod }}</el-descriptions-item>
        <el-descriptions-item label="费用类型">{{ bill.billType }}</el-descriptions-item>
        <el-descriptions-item label="应收金额">¥{{ bill.amount }}</el-descriptions-item>
        <el-descriptions-item label="优惠金额">¥{{ bill.discountAmount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="滞纳金">¥{{ bill.lateFee || 0 }}</el-descriptions-item>
        <el-descriptions-item label="应付金额" :span="2">
          <span style="font-size:18px;font-weight:bold;color:#e6a23c">¥{{ bill.payableAmount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="已付金额">¥{{ bill.paidAmount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="缴费时间">{{ bill.payTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="缴费方式">{{ bill.payMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="交易流水号">{{ bill.payTradeNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ bill.createTime }}</el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <el-card v-if="paymentList.length > 0" header="缴费记录" style="margin-top:12px">
        <el-table :data="paymentList" border size="small">
          <el-table-column prop="paymentNo" label="缴费编号" width="200" />
          <el-table-column prop="payAmount" label="金额" width="100" />
          <el-table-column prop="payMethod" label="方式" width="80" />
          <el-table-column prop="payTradeNo" label="交易流水号" width="200" />
          <el-table-column prop="payTime" label="缴费时间" width="170" />
        </el-table>
      </el-card>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getBillById } from '@/api/fee'
import { getPaymentList } from '@/api/fee'

const route = useRoute()
const loading = ref(false)
const bill = ref<any>({})
const paymentList = ref<any[]>([])

const statusMap: Record<number, { text: string; type: 'success' | 'info' | 'primary' | 'warning' | 'danger' }> = {
  0: { text: '待缴费', type: 'warning' },
  1: { text: '部分缴费', type: 'primary' },
  2: { text: '已缴清', type: 'success' },
  3: { text: '已作废', type: 'info' }
}

onMounted(async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const [b, p] = await Promise.all([
      getBillById(id),
      getPaymentList({ billId: id, pageSize: 50 })
    ])
    bill.value = b
    paymentList.value = (p as any)?.records || []
  } finally { loading.value = false }
})
</script>
