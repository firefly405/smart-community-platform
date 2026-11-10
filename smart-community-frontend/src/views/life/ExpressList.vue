<template>
  <div class="page-container">
    <ProTable
      :data="tableData"
      :columns="columns"
      :total="total"
      :loading="loading"
      :page="page"
      :page-size="pageSize"
      @search="handleSearch"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    >
      <template #status="{ row }">
        <el-tag :type="expressStatusMap[row.status]?.type || 'info'" size="small">
          {{ expressStatusMap[row.status]?.label || '未知' }}
        </el-tag>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import type { FormField } from '@/components/ProForm.vue'
import { getExpressList, receiveExpress, updateExpress, deleteExpress } from '@/api/life'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('录入快递')
const editData = ref<any>(null)

const expressStatusOptions = [
  { label: '待取件', value: 0 },
  { label: '已取件', value: 1 },
  { label: '滞留', value: 2 },
  { label: '退回', value: 3 }
]
const expressStatusMap: Record<number, { label: string; type: 'warning' | 'success' | 'danger' | 'info' }> = {
  0: { label: '待取件', type: 'warning' },
  1: { label: '已取件', type: 'success' },
  2: { label: '滞留', type: 'danger' },
  3: { label: '退回', type: 'info' }
}

const columns = [
  { prop: 'expressNo', label: '快递编号', width: 140 },
  { prop: 'expressCompany', label: '快递公司', width: 110 },
  { prop: 'trackingNo', label: '物流单号', width: 160 },
  { prop: 'receiverId', label: '收件人ID', width: 100 },
  { prop: 'packageInfo', label: '包裹信息', width: 120 },
  { prop: 'pickupCode', label: '取件码', width: 100 },
  { prop: 'shelfNo', label: '货架号', width: 100 },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'arriveTime', label: '到达时间', width: 170 },
  { prop: 'pickupTime', label: '取件时间', width: 170 }
]

const formFields: FormField[] = [
  { prop: 'expressNo', label: '快递编号', required: true },
  { prop: 'receiverId', label: '收件人ID', type: 'number', required: true },
  { prop: 'expressCompany', label: '快递公司' },
  { prop: 'trackingNo', label: '物流单号' },
  { prop: 'packageInfo', label: '包裹信息' },
  { prop: 'pickupCode', label: '取件码', required: true },
  { prop: 'shelfNo', label: '货架号' },
  { prop: 'status', label: '状态', type: 'select', options: expressStatusOptions }
]

async function loadData(searchParams?: any) {
  loading.value = true
  try {
    const res = await getExpressList({ page: page.value, pageSize: pageSize.value, ...searchParams })
    tableData.value = res.records
    total.value = res.total
  } catch { /* handled */ }
  finally { loading.value = false }
}

function handleSearch(params: any) {
  page.value = params.page
  pageSize.value = params.pageSize
  loadData(params)
}

function handleAdd() {
  editData.value = null
  dialogTitle.value = '录入快递'
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editData.value = { ...row }
  dialogTitle.value = '编辑快递'
  dialogVisible.value = true
}

async function handleSubmit(data: any) {
  submitting.value = true
  try {
    const payload = { ...data, status: data.status ?? 0 }
    if (data.id) {
      await updateExpress(payload)
      ElMessage.success('修改成功')
    } else {
      await receiveExpress(payload)
      ElMessage.success('录入成功')
    }
    dialogVisible.value = false
    editData.value = null
    loadData()
  } catch { /* handled */ }
  finally { submitting.value = false }
}

async function handleDelete(row: any) {
  await deleteExpress(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>
