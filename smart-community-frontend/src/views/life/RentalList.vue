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
      <template #rentalType="{ row }">
        {{ rentalTypeLabels[row.rentalType] || '未知' }}
      </template>
      <template #status="{ row }">
        <el-tag :type="rentalStatusMap[row.status]?.type || 'info'" size="small">
          {{ rentalStatusMap[row.status]?.label || '未知' }}
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
import { getRentalList, createRental, updateRental, deleteRental } from '@/api/life'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('新增房源')
const editData = ref<any>(null)

const rentalTypeOptions = [
  { label: '整租', value: 1 },
  { label: '合租', value: 2 },
  { label: '短租', value: 3 }
]
const rentalTypeLabels = Object.fromEntries(rentalTypeOptions.map(item => [item.value, item.label]))
const rentalStatusOptions = [
  { label: '待审核', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已出租', value: 2 },
  { label: '已下架', value: 3 }
]
const rentalStatusMap: Record<number, { label: string; type: 'warning' | 'success' | 'info' | 'danger' }> = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已发布', type: 'success' },
  2: { label: '已出租', type: 'info' },
  3: { label: '已下架', type: 'danger' }
}

const columns = [
  { prop: 'rentalNo', label: '房源编号', width: 130 },
  { prop: 'title', label: '标题', width: 160 },
  { prop: 'rentalType', label: '租赁类型', width: 100, slot: 'rentalType' },
  { prop: 'houseId', label: '房屋ID', width: 90 },
  { prop: 'monthlyRent', label: '月租金', width: 100 },
  { prop: 'deposit', label: '押金', width: 100 },
  { prop: 'paymentMethod', label: '付款方式', width: 110 },
  { prop: 'contactName', label: '联系人', width: 100 },
  { prop: 'contactPhone', label: '电话', width: 130 },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const formFields: FormField[] = [
  { prop: 'rentalNo', label: '房源编号', required: true },
  { prop: 'ownerId', label: '发布者ID', type: 'number', required: true },
  { prop: 'title', label: '标题', required: true },
  { prop: 'houseId', label: '房屋ID', type: 'number' },
  { prop: 'rentalType', label: '租赁类型', type: 'select', options: rentalTypeOptions, required: true },
  { prop: 'monthlyRent', label: '月租金', type: 'number', required: true },
  { prop: 'deposit', label: '押金', type: 'number' },
  { prop: 'paymentMethod', label: '付款方式' },
  { prop: 'images', label: '图片URL', type: 'textarea' },
  { prop: 'description', label: '描述', type: 'textarea' },
  { prop: 'contactName', label: '联系人' },
  { prop: 'contactPhone', label: '联系电话' },
  { prop: 'status', label: '状态', type: 'select', options: rentalStatusOptions },
  { prop: 'isRecommend', label: '是否推荐', type: 'switch', activeValue: 1, inactiveValue: 0 }
]

async function loadData(searchParams?: any) {
  loading.value = true
  try {
    const res = await getRentalList({ page: page.value, pageSize: pageSize.value, ...searchParams })
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
  dialogTitle.value = '新增房源'
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editData.value = { ...row }
  dialogTitle.value = '编辑房源'
  dialogVisible.value = true
}

async function handleSubmit(data: any) {
  submitting.value = true
  try {
    const payload = { ...data, status: data.status ?? 0, isRecommend: data.isRecommend ?? 0 }
    if (data.id) {
      await updateRental(payload)
      ElMessage.success('修改成功')
    } else {
      await createRental(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    editData.value = null
    loadData()
  } catch { /* handled */ }
  finally { submitting.value = false }
}

async function handleDelete(row: any) {
  await deleteRental(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>
