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
        <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'info' : 'warning'" size="small">
          {{ row.status === 1 ? '已发布' : row.status === 2 ? '已下架' : '待审核' }}
        </el-tag>
      </template>
      <template #isTop="{ row }">
        <el-tag v-if="row.isTop === 1" type="danger" size="small">置顶</el-tag>
        <span v-else>-</span>
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
import { getPostList, createPost, updatePost, deletePost } from '@/api/social'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('新增帖子')
const editData = ref<any>(null)

const columns = [
  { prop: 'title', label: '标题', width: 200 },
  { prop: 'category', label: '分类', width: 80 },
  { prop: 'userName', label: '作者', width: 100 },
  { prop: 'viewCount', label: '浏览', width: 80 },
  { prop: 'likeCount', label: '点赞', width: 80 },
  { prop: 'commentCount', label: '评论', width: 80 },
  { prop: 'isTop', label: '置顶', width: 60, slot: 'isTop' },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const formFields: FormField[] = [
  { prop: 'title', label: '标题', required: true },
  { prop: 'content', label: '内容', type: 'textarea', required: true },
  { prop: 'category', label: '分类', type: 'select', options: [
    { label: '求助', value: '求助' },
    { label: '分享', value: '分享' },
    { label: '交易', value: '交易' },
    { label: '其他', value: '其他' }
  ]},
  { prop: 'tags', label: '标签' },
  { prop: 'images', label: '图片URL', type: 'textarea' }
]

async function loadData(searchParams?: any) {
  loading.value = true
  try {
    const res = await getPostList({ page: page.value, pageSize: pageSize.value, ...searchParams })
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
  dialogTitle.value = '新增帖子'
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editData.value = { ...row }
  dialogTitle.value = '编辑帖子'
  dialogVisible.value = true
}

async function handleSubmit(data: any) {
  submitting.value = true
  try {
    if (data.id) {
      await updatePost(data)
      ElMessage.success('修改成功')
    } else {
      await createPost(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    editData.value = null
    loadData()
  } catch { /* handled */ }
  finally { submitting.value = false }
}

async function handleDelete(row: any) {
  await deletePost(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>