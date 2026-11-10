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
        <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
      </template>
      <template #actions="{ row }">
        <el-button v-if="row.status === 0" link type="primary" @click="doAccept(row)">接单</el-button>
        <el-button v-if="row.status === 0 || row.status === 1" link type="success" @click="doComplete(row)">完成</el-button>
        <el-button v-if="row.status === 0 || row.status === 1" link type="danger" @click="doCancel(row)">取消</el-button>
      </template>
    </ProTable>

    <el-dialog v-model="dialogVisible" title="新建服务预约" width="560px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="服务项目" required>
          <el-select v-model="form.serviceId" filterable style="width:100%" @change="onServiceChange">
            <el-option
              v-for="s in services"
              :key="s.id"
              :label="`${s.serviceName || s.name}（¥${s.price || 0}）`"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="用户ID" required>
          <el-input-number v-model="form.userId" :min="1" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.userName" />
        </el-form-item>
        <el-form-item label="预约日期">
          <el-input v-model="form.appointmentDate" placeholder="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="预约时段">
          <el-input v-model="form.appointmentTime" placeholder="如 09:00-10:00" />
        </el-form-item>
        <el-form-item label="服务地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="form.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
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
import { useListQuery } from '@/composables/useListQuery'
const queryParams = useListQuery()
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import {
  getServiceList,
  getServiceOrderList,
  createServiceOrder,
  acceptServiceOrder,
  completeServiceOrder,
  cancelServiceOrder
} from '@/api/life'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const services = ref<any[]>([])

const form = ref<any>({
  serviceId: undefined,
  serviceName: '',
  userId: 1,
  userName: '',
  appointmentDate: '',
  appointmentTime: '',
  address: '',
  phone: '',
  price: 0,
  remark: ''
})

const columns = [
  { prop: 'serviceName', label: '服务项目', width: 140 },
  { prop: 'userName', label: '用户', width: 100 },
  { prop: 'price', label: '价格', width: 90 },
  { prop: 'appointmentDate', label: '预约日期', width: 120 },
  { prop: 'appointmentTime', label: '时段', width: 120 },
  { prop: 'address', label: '地址', width: 160 },
  { prop: 'phone', label: '电话', width: 120 },
  { prop: 'status', label: '状态', width: 90, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

function statusText(s: number) {
  return ({ 0: '待接单', 1: '服务中', 2: '服务中', 3: '已完成', 4: '已取消' } as any)[s] || '未知'
}
function statusTag(s: number) {
  if (s === 0) return 'warning'
  if (s === 1) return 'primary'
  if (s === 3) return 'success'
  return 'info'
}

async function loadData(params?: any) {
  loading.value = true
  try {
    const res = await getServiceOrderList({ page: page.value, pageSize: pageSize.value, ...queryParams(params) })
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
  const res = await getServiceList({ page: 1, pageSize: 200 })
  services.value = res.records || []
  form.value = {
    serviceId: Number(queryParams().serviceId) || undefined, serviceName: '', userId: 1, userName: '',
    appointmentDate: '', appointmentTime: '', address: '', phone: '', price: 0, remark: ''
  }
  if (form.value.serviceId) onServiceChange(form.value.serviceId)
  dialogVisible.value = true
}
function onServiceChange(id: number) {
  const s = services.value.find(x => x.id === id)
  if (s) {
    form.value.serviceName = s.serviceName || s.name
    form.value.price = Number(s.price || 0)
  }
}
async function handleSubmit() {
  if (!form.value.serviceId) {
    ElMessage.warning('请选择服务项目')
    return
  }
  submitting.value = true
  try {
    await createServiceOrder({
      ...form.value,
      userName: form.value.userName || `用户${form.value.userId}`,
      status: 0
    })
    ElMessage.success('预约成功')
    dialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}
async function doAccept(row: any) {
  await acceptServiceOrder(row.id)
  ElMessage.success('已接单')
  loadData()
}
async function doComplete(row: any) {
  await completeServiceOrder(row.id)
  ElMessage.success('服务已完成')
  loadData()
}
async function doCancel(row: any) {
  await cancelServiceOrder(row.id)
  ElMessage.success('已取消')
  loadData()
}
onMounted(() => loadData())
</script>
