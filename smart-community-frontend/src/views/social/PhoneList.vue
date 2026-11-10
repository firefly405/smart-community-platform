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
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
          {{ row.status === 1 ? '启用' : '禁用' }}
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
import { getPhoneList, createPhone, updatePhone, deletePhone } from '@/api/social'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('新增电话')
const editData = ref<any>(null)

const columns = [
  { prop: 'name', label: '名称', width: 160 },
  { prop: 'phone', label: '电话', width: 140 },
  { prop: 'category', label: '分类', width: 100 },
  { prop: 'address', label: '地址', width: 200 },
  { prop: 'sort', label: '排序', width: 80 },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const formFields: FormField[] = [
  { prop: 'name', label: '名称', required: true },
  { prop: 'phone', label: '电话', required: true },
  { prop: 'category', label: '分类', type: 'select', options: [
    { label: '物业', value: '物业' },
    { label: '维修', value: '维修' },
    { label: '医疗', value: '医疗' },
    { label: '交通', value: '交通' },
    { label: '餐饮', value: '餐饮' },
    { label: '其他', value: '其他' }
  ]},
  { prop: 'address', label: '地址' },
  { prop: 'sort', label: '排序', type: 'number' },
  { prop: 'status', label: '状态', type: 'switch', activeValue: 1, inactiveValue: 0 }
]

async function loadData(searchParams?: any) {
  loading.value = true
  try {
    const res = await getPhoneList({ page: page.value, pageSize: pageSize.value, ...searchParams })
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
  dialogTitle.value = '新增电话'
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editData.value = { ...row }
  dialogTitle.value = '编辑电话'
  dialogVisible.value = true
}

async function handleSubmit(data: any) {
  submitting.value = true
  try {
    if (data.id) {
      await updatePhone(data)
      ElMessage.success('修改成功')
    } else {
      await createPhone(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    editData.value = null
    loadData()
  } catch { /* handled */ }
  finally { submitting.value = false }
}

async function handleDelete(row: any) {
  await deletePhone(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>