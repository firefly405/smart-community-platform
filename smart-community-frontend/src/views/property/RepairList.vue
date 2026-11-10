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
      :search-fields="searchFields"
      @search="handleSearch"
      @add="handleAdd"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    >
      <template #status="{ row }">
        <el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.text }}</el-tag>
      </template>
      <template #actions="{ row }">
        <el-button size="small" link @click="handleDetail(row)">详情</el-button>
        <el-button v-if="row.status === 0" size="small" type="primary" link @click="handleDispatch(row)">派单</el-button>
        <el-button v-if="row.status === 1" size="small" type="warning" link @click="handleProcess(row)">处理</el-button>
        <el-button v-if="row.status === 2" size="small" type="success" link @click="handleComplete(row)">完成</el-button>
        <el-button v-if="row.status === 3" size="small" link @click="handleEvaluate(row)">评价</el-button>
        <el-button v-if="row.status === 0" size="small" type="danger" link @click="handleCancel(row)">取消</el-button>
      </template>
    </ProTable>

    <ProDialog :visible="dialogVisible" :title="dialogTitle" :form-data="formData" :form-fields="formFields" :loading="submitting" @confirm="handleConfirm" @cancel="dialogVisible = false" />
    <ProDialog :visible="detailVisible" title="报修详情" :form-data="detailData" :form-fields="detailFields" :show-submit="false" @cancel="detailVisible = false" />
    <ProDialog :visible="processVisible" title="处理报修" :form-data="processForm" :form-fields="processFields" :loading="submitting" @confirm="handleProcessConfirm" @cancel="processVisible = false" />
    <ProDialog :visible="evalVisible" title="评价报修" :form-data="evalForm" :form-fields="evalFields" :loading="submitting" @confirm="handleEvalConfirm" @cancel="evalVisible = false" />
  </div>
</template>

<script setup lang="ts">
import { useListQuery } from '@/composables/useListQuery'
const queryParams = useListQuery()
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import { getRepairList, createRepair, dispatchRepair, processRepair, completeRepair, evaluateRepair, cancelRepair, type RepairItem } from '@/api/property'

const loading = ref(false); const submitting = ref(false)
const tableData = ref<any[]>([]); const total = ref(0); const page = ref(1); const pageSize = ref(10)
const dialogVisible = ref(false); const dialogTitle = ref(''); const currentId = ref(0)
const detailVisible = ref(false); const processVisible = ref(false); const evalVisible = ref(false)

const statusMap: Record<number, { text: string; type: 'success' | 'info' | 'primary' | 'warning' | 'danger' }> = {
  0: { text: '待处理', type: 'warning' }, 1: { text: '已派单', type: 'primary' },
  2: { text: '处理中', type: 'info' }, 3: { text: '已完成', type: 'success' }, 4: { text: '已取消', type: 'danger' }
}

const columns = [
  { prop: 'repairNo', label: '报修单号', width: 140 }, { prop: 'title', label: '标题', width: 150 },
  { prop: 'ownerName', label: '业主', width: 100 }, { prop: 'houseNo', label: '房号', width: 100 }, { prop: 'repairType', label: '类型', width: 80 },
  { prop: 'status', label: '状态', width: 80, slot: 'status' }, { prop: 'handlerName', label: '处理人', width: 100 }
]
const searchFields = [
  { prop: 'ownerId', label: '业主ID', type: 'number' as const },
  { prop: 'status', label: '状态', type: 'select' as const, options: Object.entries(statusMap).map(([k, v]) => ({ label: v.text, value: Number(k) })) }
]

const formData = reactive<RepairItem>({ title: '', content: '', repairType: 1, urgency: 1, houseId: 0, contactName: '', contactPhone: '' })
const formFields = [
  { prop: 'title', label: '标题', type: 'input' as const, required: true },
  { prop: 'content', label: '内容', type: 'textarea' as const }, { prop: 'houseId', label: '房屋ID', type: 'number' as const },
  { prop: 'repairType', label: '类型', type: 'select' as const, options: [{ label: '水电', value: 1 }, { label: '门窗', value: 2 }, { label: '墙面', value: 3 }] }
]

const detailData = reactive<RepairItem>({ title: '', content: '' })
const detailFields = [{ prop: 'title', label: '标题' }, { prop: 'content', label: '内容' }, { prop: 'repairNo', label: '单号' }, { prop: 'handlerName', label: '处理人' }]

const processForm = reactive({ result: '', images: '' })
const processFields = [{ prop: 'result', label: '处理结果', type: 'textarea' as const, required: true }]

const evalForm = reactive({ score: 5, content: '' })
const evalFields = [
  { prop: 'score', label: '评分(1-5)', type: 'number' as const, required: true },
  { prop: 'content', label: '评价内容', type: 'textarea' as const }
]

const loadData = async (params?: any) => {
  loading.value = true
  try { const res = await getRepairList({ page: page.value, pageSize: pageSize.value, ...queryParams(params) }); tableData.value = res.records; total.value = res.total } finally { loading.value = false }
}

const handleSearch = (params: any) => loadData(params)
const handleAdd = () => { dialogTitle.value = '创建报修'; Object.assign(formData, { title: '', content: '', repairType: 1, urgency: 1, houseId: 0, contactName: '', contactPhone: '' }); dialogVisible.value = true }
const handleDetail = (row: RepairItem) => { Object.assign(detailData, row); detailVisible.value = true }

const handleConfirm = async () => { submitting.value = true; try { await createRepair(formData); ElMessage.success('创建成功'); dialogVisible.value = false; loadData() } finally { submitting.value = false } }

const handleDispatch = async (row: RepairItem) => {
  try { const handlerId = prompt('请输入处理人ID'); if (handlerId) { await dispatchRepair(row.id!, Number(handlerId)); ElMessage.success('派单成功'); loadData() } } catch {}
}

const handleProcess = (row: RepairItem) => { currentId.value = row.id!; Object.assign(processForm, { result: '', images: '' }); processVisible.value = true }
const handleProcessConfirm = async () => { submitting.value = true; try { await processRepair(currentId.value, processForm); ElMessage.success('处理成功'); processVisible.value = false; loadData() } finally { submitting.value = false } }

const handleComplete = async (row: RepairItem) => {
  await ElMessageBox.confirm('确认完成该报修？', '提示', { type: 'warning' })
  await completeRepair(row.id!); ElMessage.success('已完成'); loadData()
}

const handleEvaluate = (row: RepairItem) => { currentId.value = row.id!; Object.assign(evalForm, { score: 5, content: '' }); evalVisible.value = true }
const handleEvalConfirm = async () => { submitting.value = true; try { await evaluateRepair(currentId.value, evalForm); ElMessage.success('评价成功'); evalVisible.value = false; loadData() } finally { submitting.value = false } }

const handleCancel = async (row: RepairItem) => {
  await ElMessageBox.confirm('确认取消该报修？', '提示', { type: 'warning' })
  await cancelRepair(row.id!); ElMessage.success('已取消'); loadData()
}

onMounted(() => loadData())
</script>
