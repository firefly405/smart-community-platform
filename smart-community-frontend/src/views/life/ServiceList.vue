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
      <template #serviceType="{ row }">
        {{ serviceTypeLabels[row.serviceType] || '未知' }}
      </template>
      <template #status="{ row }">
        <el-tag :type="row.isOnSale === 1 ? 'success' : 'info'" size="small">
          {{ row.isOnSale === 1 ? '上架' : '下架' }}
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
import { getServiceList, createService, updateService, deleteService } from '@/api/life'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('新增服务')
const editData = ref<any>(null)

const serviceTypeOptions = [
  { label: '保洁', value: 1 },
  { label: '保姆', value: 2 },
  { label: '月嫂', value: 3 },
  { label: '维修', value: 4 },
  { label: '搬家', value: 5 },
  { label: '其他', value: 6 }
]
const serviceTypeLabels = Object.fromEntries(serviceTypeOptions.map(item => [item.value, item.label]))

const columns = [
  { prop: 'serviceName', label: '服务名称', width: 180 },
  { prop: 'serviceType', label: '分类', width: 100, slot: 'serviceType' },
  { prop: 'price', label: '价格', width: 100 },
  { prop: 'unit', label: '单位', width: 80 },
  { prop: 'isOnSale', label: '状态', width: 80, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const formFields: FormField[] = [
  { prop: 'serviceName', label: '服务名称', required: true },
  { prop: 'serviceType', label: '分类', type: 'select', options: serviceTypeOptions, required: true },
  { prop: 'price', label: '价格', type: 'number', required: true },
  { prop: 'unit', label: '单位' },
  { prop: 'serviceImage', label: '图片URL' },
  { prop: 'description', label: '描述', type: 'textarea' },
  { prop: 'isOnSale', label: '是否上架', type: 'switch', activeValue: 1, inactiveValue: 0 }
]

async function loadData(searchParams?: any) {
  loading.value = true
  try {
    const res = await getServiceList({ page: page.value, pageSize: pageSize.value, ...searchParams })
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
  dialogTitle.value = '新增服务'
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editData.value = { ...row }
  dialogTitle.value = '编辑服务'
  dialogVisible.value = true
}

async function handleSubmit(data: any) {
  submitting.value = true
  try {
    const payload = { ...data, isOnSale: data.isOnSale ?? 1 }
    if (data.id) {
      await updateService(payload)
      ElMessage.success('修改成功')
    } else {
      await createService(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    editData.value = null
    loadData()
  } catch { /* handled */ }
  finally { submitting.value = false }
}

async function handleDelete(row: any) {
  await deleteService(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>
