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
        <el-tag :type="row.status === 0 ? 'primary' : row.status === 1 ? 'success' : 'info'" size="small">
          {{ row.status === 0 ? '在售' : row.status === 1 ? '已售' : '已下架' }}
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
import { getSecondhandList, createSecondhand, updateSecondhand, deleteSecondhand } from '@/api/social'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('新增二手')
const editData = ref<any>(null)

const columns = [
  { prop: 'title', label: '标题', width: 180 },
  { prop: 'category', label: '分类', width: 80 },
  { prop: 'price', label: '价格', width: 100 },
  { prop: 'originalPrice', label: '原价', width: 100 },
  { prop: 'conditionValue', label: '成色', width: 80 },
  { prop: 'userName', label: '发布人', width: 100 },
  { prop: 'viewCount', label: '浏览', width: 80 },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const formFields: FormField[] = [
  { prop: 'title', label: '标题', required: true },
  { prop: 'description', label: '描述', type: 'textarea' },
  { prop: 'category', label: '分类', type: 'select', options: [
    { label: '手机数码', value: '手机数码' },
    { label: '家具家电', value: '家具家电' },
    { label: '图书文具', value: '图书文具' },
    { label: '服装鞋包', value: '服装鞋包' },
    { label: '其他', value: '其他' }
  ]},
  { prop: 'price', label: '价格', type: 'number', required: true },
  { prop: 'originalPrice', label: '原价', type: 'number' },
  { prop: 'conditionValue', label: '成色', type: 'select', options: [
    { label: '全新', value: '全新' },
    { label: '几乎全新', value: '几乎全新' },
    { label: '轻微使用', value: '轻微使用' },
    { label: '正常使用', value: '正常使用' }
  ]},
  { prop: 'images', label: '图片URL', type: 'textarea' },
  { prop: 'contactPhone', label: '联系电话' }
]

async function loadData(searchParams?: any) {
  loading.value = true
  try {
    const res = await getSecondhandList({ page: page.value, pageSize: pageSize.value, ...searchParams })
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
  dialogTitle.value = '新增二手'
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editData.value = { ...row }
  dialogTitle.value = '编辑二手'
  dialogVisible.value = true
}

async function handleSubmit(data: any) {
  submitting.value = true
  try {
    if (data.id) {
      await updateSecondhand(data)
      ElMessage.success('修改成功')
    } else {
      await createSecondhand(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    editData.value = null
    loadData()
  } catch { /* handled */ }
  finally { submitting.value = false }
}

async function handleDelete(row: any) {
  await deleteSecondhand(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>