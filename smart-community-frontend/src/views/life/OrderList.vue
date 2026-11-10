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
      @update:page="(p: number) => { page = p; loadData() }"
      @update:page-size="(s: number) => { pageSize = s; loadData() }"
    >
      <template #status="{ row }">
        <el-tag :type="statusTagType(row.status)" size="small">
          {{ statusText(row.status) }}
        </el-tag>
      </template>
      <template #items="{ row }">
        <span>{{ formatItems(row.items, row) }}</span>
      </template>
      <template #actions="{ row }">
        <el-button v-if="row.status === 0" link type="primary" @click="handlePay(row)">支付</el-button>
        <el-button v-if="row.status === 1" link type="primary" @click="handleShip(row)">发货</el-button>
        <el-button v-if="row.status === 2" link type="success" @click="handleComplete(row)">完成</el-button>
        <el-button v-if="row.status != null && row.status < 3" link type="danger" @click="handleCancel(row)">取消</el-button>
      </template>
    </ProTable>

    <el-dialog v-model="dialogVisible" title="新增订单" width="680px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="用户ID" required>
          <el-input-number v-model="form.userId" :min="1" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.userName" placeholder="可选" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="商品明细" required>
          <div style="width:100%">
            <div v-for="(line, idx) in form.lines" :key="idx" style="display:flex;gap:8px;margin-bottom:8px;align-items:center">
              <el-select v-model="line.productId" placeholder="选择商品" filterable style="flex:1" @change="onProductChange(line)">
                <el-option
                  v-for="p in products"
                  :key="p.id"
                  :label="`${p.productName || p.name}（库存${p.stock}）`"
                  :value="p.id"
                />
              </el-select>
              <el-input-number v-model="line.quantity" :min="1" :max="999" @change="recalcTotal" />
              <span style="width:70px;text-align:right">¥{{ lineAmount(line) }}</span>
              <el-button link type="danger" @click="removeLine(idx)">删除</el-button>
            </div>
            <el-button type="primary" link @click="addLine">+ 添加商品</el-button>
          </div>
        </el-form-item>
        <el-form-item label="总金额">
          <el-input-number v-model="form.totalAmount" :min="0" :precision="2" />
          <span style="margin-left:8px;color:#999">由商品自动汇总，可改</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import { getOrderList, createOrder, cancelOrder, payOrder, shipOrder, completeOrder, getProductList } from '@/api/life'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const products = ref<any[]>([])

const form = ref({
  userId: 1,
  userName: '',
  address: '',
  phone: '',
  totalAmount: 0,
  lines: [{ productId: undefined as number | undefined, productName: '', price: 0, quantity: 1 }]
})

const statusMap: Record<number, string> = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消' }
function statusText(s: number) { return statusMap[s] || '未知' }
function statusTagType(s: number) {
  if (s === 0) return 'warning'
  if (s === 3) return 'success'
  if (s === 4) return 'info'
  return 'primary'
}

const columns = [
  { prop: 'orderNo', label: '订单号', width: 180 },
  { prop: 'userName', label: '用户', width: 100 },
  { prop: 'items', label: '商品', width: 200, slot: 'items' },
  { prop: 'totalAmount', label: '金额', width: 100 },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'address', label: '地址', width: 160 },
  { prop: 'phone', label: '电话', width: 120 },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

function formatItems(items: any, row?: any) {
  if (row?.itemList?.length) {
    return row.itemList.map((x: any) => `${x.productName || ('商品' + x.productId)}×${x.quantity || 1}`).join('，')
  }
  if (!items) return '-'
  if (typeof items === 'string' && !items.trim().startsWith('[')) return items
  try {
    const arr = typeof items === 'string' ? JSON.parse(items) : items
    if (!Array.isArray(arr)) return String(items)
    return arr.map((x: any) => `${x.productName || x.name || ('商品' + x.productId)}×${x.quantity || 1}`).join('，')
  } catch {
    return String(items)
  }
}

function lineAmount(line: any) {
  return ((line.price || 0) * (line.quantity || 0)).toFixed(2)
}
function addLine() {
  form.value.lines.push({ productId: undefined, productName: '', price: 0, quantity: 1 })
}
function removeLine(idx: number) {
  form.value.lines.splice(idx, 1)
  if (!form.value.lines.length) addLine()
  recalcTotal()
}
function onProductChange(line: any) {
  const p = products.value.find(x => x.id === line.productId)
  if (p) {
    line.productName = p.productName || p.name
    line.price = Number(p.price || 0)
  }
  recalcTotal()
}
function recalcTotal() {
  form.value.totalAmount = Number(form.value.lines.reduce((sum, l) => sum + (l.price || 0) * (l.quantity || 0), 0).toFixed(2))
}

async function loadProducts() {
  const res = await getProductList({ page: 1, pageSize: 200 })
  products.value = res.records || []
}

async function loadData(searchParams?: any) {
  loading.value = true
  try {
    const res = await getOrderList({ page: page.value, pageSize: pageSize.value, ...searchParams })
    tableData.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}

function handleSearch(params: any) {
  page.value = params.page
  pageSize.value = params.pageSize
  loadData(params)
}

async function handleAdd() {
  await loadProducts()
  form.value = {
    userId: 1,
    userName: '',
    address: '',
    phone: '',
    totalAmount: 0,
    lines: [{ productId: undefined, productName: '', price: 0, quantity: 1 }]
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const validLines = form.value.lines.filter(l => l.productId && l.quantity > 0)
  if (!validLines.length) {
    ElMessage.warning('请至少选择一个商品')
    return
  }
  submitting.value = true
  try {
    const items = validLines.map(l => ({
      productId: l.productId,
      productName: l.productName,
      price: l.price,
      quantity: l.quantity
    }))
    await createOrder({
      userId: form.value.userId,
      userName: form.value.userName || `用户${form.value.userId}`,
      address: form.value.address,
      phone: form.value.phone,
      totalAmount: form.value.totalAmount,
      items: JSON.stringify(items)
    })
    ElMessage.success('下单成功，已扣减库存')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleCancel(row: any) {
  await cancelOrder(row.id)
  ElMessage.success('取消成功，库存已回补')
  loadData()
}
async function handlePay(row: any) {
  const res: any = await payOrder(row.id)
  const simulated = res?.simulated !== false
  ElMessage.success(simulated ? `模拟支付成功（交易号 ${res?.tradeNo || '-'}，非真实资金）` : '支付成功')
  loadData()
}
async function handleShip(row: any) {
  await shipOrder(row.id)
  ElMessage.success('发货成功')
  loadData()
}
async function handleComplete(row: any) {
  await completeOrder(row.id)
  ElMessage.success('订单已完成')
  loadData()
}

onMounted(() => loadData())
</script>
