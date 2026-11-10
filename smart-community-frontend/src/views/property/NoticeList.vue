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
      :show-edit="true"
      :show-delete="true"
      :search-fields="searchFields"
      @search="handleSearch"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    >
      <template #actions="{ row }">
        <el-button v-if="row.isPublish !== 1" size="small" type="success" link @click="handlePublish(row)">发布</el-button>
        <el-button v-if="row.isPublish === 1" size="small" type="warning" link @click="handleRevoke(row)">撤回</el-button>
      </template>
    </ProTable>

    <ProDialog :visible="dialogVisible" :title="dialogTitle" :form-data="formData" :form-fields="formFields" :loading="submitting" width="600px" @confirm="handleConfirm" @cancel="dialogVisible = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import { getNoticeList, createNotice, updateNotice, deleteNotice, publishNotice, revokeNotice, type NoticeItem } from '@/api/property'

const loading = ref(false); const submitting = ref(false); const isEdit = ref(false)
const tableData = ref<any[]>([]); const total = ref(0); const page = ref(1); const pageSize = ref(10)
const dialogVisible = ref(false); const dialogTitle = ref('')

const columns = [
  { prop: 'title', label: '标题', width: 200 }, { prop: 'noticeType', label: '类型', width: 80 },
  { prop: 'isTop', label: '置顶', width: 60 }, { prop: 'isPublish', label: '状态', width: 80 },
  { prop: 'viewCount', label: '阅读量', width: 80 }, { prop: 'publishTime', label: '发布时间', width: 170 }
]
const searchFields = [{ prop: 'title', label: '标题', type: 'input' as const }]

const formData = reactive<NoticeItem>({ title: '', content: '', noticeType: 1, isTop: 0 })
const formFields = [
  { prop: 'title', label: '标题', type: 'input' as const, required: true }, { prop: 'content', label: '内容', type: 'textarea' as const },
  { prop: 'noticeType', label: '类型', type: 'select' as const, options: [{ label: '通知', value: 1 }, { label: '活动', value: 2 }, { label: '紧急', value: 3 }] }
]

const loadData = async (params?: any) => {
  loading.value = true
  try { const res = await getNoticeList({ page: page.value, pageSize: pageSize.value, ...params }); tableData.value = res.records; total.value = res.total } finally { loading.value = false }
}
const handleSearch = (params: any) => loadData(params)
const handleAdd = () => { isEdit.value = false; dialogTitle.value = '新增公告'; Object.assign(formData, { title: '', content: '', noticeType: 1, isTop: 0 }); dialogVisible.value = true }
const handleEdit = (row: NoticeItem) => { isEdit.value = true; dialogTitle.value = '编辑公告'; Object.assign(formData, row); dialogVisible.value = true }
const handleDelete = async (row: NoticeItem) => { await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' }); await deleteNotice(row.id!); ElMessage.success('删除成功'); loadData() }
const handleConfirm = async () => { submitting.value = true; try { if (isEdit.value) { await updateNotice(formData) } else { await createNotice(formData) }; ElMessage.success(isEdit.value ? '修改成功' : '新增成功'); dialogVisible.value = false; loadData() } finally { submitting.value = false } }
const handlePublish = async (row: NoticeItem) => { await publishNotice(row.id!); ElMessage.success('已发布'); loadData() }
const handleRevoke = async (row: NoticeItem) => { await revokeNotice(row.id!); ElMessage.success('已撤回'); loadData() }
onMounted(() => loadData())
</script>