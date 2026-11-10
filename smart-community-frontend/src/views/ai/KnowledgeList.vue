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
      @update:page="(p: number) => (page = p)"
      @update:page-size="(s: number) => (pageSize = s)"
    >
      <template #tools>
        <el-button type="success" @click="handleReload" :loading="reloading">
          <el-icon><Refresh /></el-icon>
          重建向量索引
        </el-button>
      </template>

      <template #category="{ row }">
        <el-tag size="small" :type="categoryTagType(row.category)">
          {{ categoryLabel(row.category) }}
        </el-tag>
      </template>

      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
          {{ row.status === 1 ? '启用' : '禁用' }}
        </el-tag>
      </template>

      <template #content="{ row }">
        <span>{{ row.content ? row.content.substring(0, 60) + '...' : '' }}</span>
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
import { Refresh } from '@element-plus/icons-vue'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import type { FormField } from '@/components/ProForm.vue'
import { getKnowledgeList, createKnowledge, updateKnowledge, deleteKnowledge, reloadVectorIndex } from '@/api/ai'

const loading = ref(false)
const submitting = ref(false)
const reloading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('新增知识文档')
const editData = ref<any>(null)

const columns = [
  { prop: 'title', label: '文档标题', width: 200 },
  { prop: 'category', label: '分类', width: 100, slot: 'category' },
  { prop: 'content', label: '内容摘要', slot: 'content' },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 160 }
]

const formFields: FormField[] = [
  { prop: 'title', label: '文档标题', required: true },
  {
    prop: 'category',
    label: '分类',
    type: 'select',
    required: true,
    options: [
      { label: '物业管理', value: 'property' },
      { label: '安防管理', value: 'guard' },
      { label: '费用管理', value: 'fee' },
      { label: '生活服务', value: 'life' },
      { label: '社区互动', value: 'social' },
      { label: '综合信息', value: 'general' }
    ]
  },
  { prop: 'content', label: '文档内容', type: 'textarea', required: true },
  { prop: 'status', label: '状态', type: 'switch', activeValue: 1, inactiveValue: 0 }
]

const categoryMap: Record<string, string> = {
  property: '物业管理',
  guard: '安防管理',
  fee: '费用管理',
  life: '生活服务',
  social: '社区互动',
  general: '综合信息'
}
const categoryTagTypeMap: Record<string, string> = {
  property: '',
  guard: 'danger',
  fee: 'warning',
  life: 'success',
  social: 'info',
  general: ''
}

function categoryLabel(cat: string) {
  return categoryMap[cat] || cat
}

function categoryTagType(cat: string): 'success' | 'info' | 'primary' | 'warning' | 'danger' | undefined {
  return (categoryTagTypeMap[cat] || undefined) as 'success' | 'info' | 'primary' | 'warning' | 'danger' | undefined
}

async function loadData(searchParams?: any) {
  loading.value = true
  try {
    const res = await getKnowledgeList({ page: page.value, pageSize: pageSize.value, ...searchParams })
    tableData.value = res.records
    total.value = res.total
  } catch {
    /* handled by request interceptor */
  } finally {
    loading.value = false
  }
}

function handleSearch(params: any) {
  page.value = params.page
  pageSize.value = params.pageSize
  loadData(params)
}

function handleAdd() {
  editData.value = null
  dialogTitle.value = '新增知识文档'
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editData.value = { ...row }
  dialogTitle.value = '编辑知识文档'
  dialogVisible.value = true
}

async function handleSubmit(data: any) {
  submitting.value = true
  try {
    if (data.id) {
      await updateKnowledge(data)
      ElMessage.success('修改成功')
    } else {
      await createKnowledge(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    editData.value = null
    loadData()
  } catch {
    /* handled */
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: any) {
  await deleteKnowledge(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleReload() {
  reloading.value = true
  try {
    await reloadVectorIndex()
    ElMessage.success('向量索引已重建')
  } catch {
    /* handled */
  } finally {
    reloading.value = false
  }
}

onMounted(() => loadData())
</script>

<style lang="scss" scoped>
.page-container {
  padding: 0;
}
</style>
