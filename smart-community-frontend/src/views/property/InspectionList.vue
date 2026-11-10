<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="巡检计划" name="plan" />
      <el-tab-pane label="巡检记录" name="record" />
    </el-tabs>

    <div style="margin-bottom: 12px">
      <el-button v-if="activeTab === 'plan'" type="primary" @click="handleAdd">新增计划</el-button>
      <el-button v-if="activeTab === 'plan'" type="success" @click="handleGenerate">生成今日任务</el-button>
      <el-button v-if="activeTab === 'record'" type="primary" @click="handleAddRecord">新增记录</el-button>
    </div>

    <ProTable
      :data="tableData"
      :columns="currentColumns"
      :total="total"
      :loading="loading"
      :page="page"
      :page-size="pageSize"
      :show-add="false"
      :show-edit="activeTab === 'plan'"
      :show-delete="true"
      @edit="handleEdit"
      @delete="handleDelete"
      @update:page="(p: number) => { page = p; loadData() }"
      @update:page-size="(s: number) => { pageSize = s; loadData() }"
    >
      <template #status="{ row }">
        <el-tag v-if="activeTab === 'plan'" :type="row.status === 0 ? 'success' : 'info'" size="small">
          {{ row.status === 0 ? '启用' : '停用' }}
        </el-tag>
        <el-tag v-else :type="row.status === 1 ? 'success' : 'warning'" size="small">
          {{ row.status === 1 ? '已完成' : '待执行' }}
        </el-tag>
      </template>
      <template #actions="{ row }">
        <el-button
          v-if="activeTab === 'record' && row.status !== 1"
          size="small"
          type="success"
          link
          @click="handleComplete(row)"
        >完成巡检</el-button>
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import type { FormField } from '@/components/ProForm.vue'
import {
  getInspectionPlanList,
  createInspectionPlan,
  updateInspectionPlan,
  getInspections,
  createInspection,
  updateInspection,
  deleteInspection,
  generateTodayInspections,
  completeInspectionRecord,
  deleteInspectionPlan
} from '@/api/property'

const activeTab = ref<'plan' | 'record'>('plan')
const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('新增计划')
const editData = ref<any>(null)

const planColumns = [
  { prop: 'area', label: '巡检区域', width: 140 },
  { prop: 'inspectorName', label: '负责人', width: 100 },
  { prop: 'items', label: '巡检项', width: 200 },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 170 }
]
const recordColumns = [
  { prop: 'planId', label: '计划ID', width: 80 },
  { prop: 'area', label: '区域', width: 120 },
  { prop: 'inspectorName', label: '巡检人', width: 100 },
  { prop: 'inspectionDate', label: '巡检日期', width: 120 },
  { prop: 'result', label: '结果', width: 100 },
  { prop: 'status', label: '状态', width: 90, slot: 'status' },
  { prop: 'checkTime', label: '完成时间', width: 170 }
]
const currentColumns = computed(() => activeTab.value === 'plan' ? planColumns : recordColumns)

const formFields = computed<FormField[]>(() => {
  if (activeTab.value === 'plan') {
    return [
      { prop: 'area', label: '巡检区域', required: true },
      { prop: 'inspectorName', label: '负责人' },
      { prop: 'items', label: '巡检项', type: 'textarea' },
      { prop: 'status', label: '状态', type: 'select', options: [
        { label: '启用', value: 0 }, { label: '停用', value: -1 }
      ]}
    ]
  }
  return [
    { prop: 'planId', label: '计划ID', type: 'number' },
    { prop: 'area', label: '区域', required: true },
    { prop: 'inspectorName', label: '巡检人' },
    { prop: 'inspectionDate', label: '巡检日期', type: 'date' },
    { prop: 'items', label: '巡检项', type: 'textarea' }
  ]
})

async function loadData() {
  loading.value = true
  try {
    if (activeTab.value === 'plan') {
      const res = await getInspectionPlanList({ page: page.value, pageSize: pageSize.value })
      tableData.value = res.records
      total.value = res.total
    } else {
      const res = await getInspections({ page: page.value, pageSize: pageSize.value })
      tableData.value = res.records
      total.value = res.total
    }
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  page.value = 1
  loadData()
}

function handleAdd() {
  editData.value = { status: 0 }
  dialogTitle.value = '新增计划'
  dialogVisible.value = true
}
function handleAddRecord() {
  editData.value = {}
  dialogTitle.value = '新增记录'
  dialogVisible.value = true
}
function handleEdit(row: any) {
  editData.value = { ...row }
  dialogTitle.value = activeTab.value === 'plan' ? '编辑计划' : '编辑记录'
  dialogVisible.value = true
}
async function handleSubmit(data: any) {
  submitting.value = true
  try {
    if (activeTab.value === 'plan') {
      if (data.id) await updateInspectionPlan(data)
      else await createInspectionPlan(data)
    } else {
      if (data.id) await updateInspection(data)
      else await createInspection(data)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    editData.value = null
    loadData()
  } finally {
    submitting.value = false
  }
}
async function handleDelete(row: any) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  if (activeTab.value === 'plan') {
    await deleteInspectionPlan(row.id)
  } else {
    await deleteInspection(row.id)
  }
  ElMessage.success('删除成功')
  loadData()
}
async function handleGenerate() {
  const count = await generateTodayInspections()
  ElMessage.success(`已生成 ${count ?? 0} 条今日巡检任务`)
  activeTab.value = 'record'
  loadData()
}
async function handleComplete(row: any) {
  const { value } = await ElMessageBox.prompt('请输入巡检结果', '完成巡检', {
    inputValue: '正常',
    confirmButtonText: '完成'
  })
  await completeInspectionRecord(row.id, { result: value || '正常' })
  ElMessage.success('巡检已完成')
  loadData()
}

onMounted(() => loadData())
</script>

