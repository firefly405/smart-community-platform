<template>
  <div class="page-container">
    <ProTable
      :data="tableData"
      :columns="columns"
      :total="total"
      :loading="loading"
      :page="page"
      :page-size="pageSize"
      :search-fields="searchFields"
      @search="handleSearch"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    >
      <template #actions="{ row }">
        <el-button type="primary" size="small" link @click="handleHandle(row)" :disabled="row.status === 2">处理</el-button>
      </template>
    </ProTable>

    <el-dialog v-model="handleVisible" title="处理报警" width="500px">
      <el-form :model="handleForm" label-width="100px">
        <el-form-item label="处理结果">
          <el-input v-model="handleForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" @click="doHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import { getAlarmList, handleAlarm } from '@/api/guard'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const alarmTypeMap: Record<number, string> = { 1: '消防', 2: '燃气', 3: '入侵', 4: '故障', 5: '紧急', 6: '其他' }
const levelMap: Record<number, string> = { 1: '一般', 2: '重要', 3: '紧急' }
const statusMap: Record<number, string> = { 0: '待处理', 1: '处理中', 2: '已处理', 3: '误报' }

const columns = [
  { prop: 'alarmNo', label: '报警单号', width: 180 },
  { prop: 'alarmType', label: '报警类型', width: 100, formatter: (_r: any, _c: any, v: number) => alarmTypeMap[v] || '-' },
  { prop: 'alarmLevel', label: '级别', width: 80, formatter: (_r: any, _c: any, v: number) => levelMap[v] || '-' },
  { prop: 'location', label: '报警位置', width: 150 },
  { prop: 'alarmTime', label: '报警时间', width: 170 },
  { prop: 'status', label: '状态', width: 100, formatter: (_r: any, _c: any, v: number) => statusMap[v] || '-' },
  { prop: 'handleResult', label: '处理结果', width: 200 },
  { prop: 'handleTime', label: '处理时间', width: 170 }
]

const searchFields = [
  { prop: 'status', label: '状态', type: 'select' as const, options: Object.entries(statusMap).map(([k, v]) => ({ value: Number(k), label: v })) }
]

const handleVisible = ref(false)
const handleForm = reactive({ remark: '' })
const pendingAlarmId = ref(0)

const handleHandle = (row: any) => {
  pendingAlarmId.value = row.id
  handleForm.remark = ''
  handleVisible.value = true
}

const doHandle = async () => {
  try {
    await handleAlarm(pendingAlarmId.value, { remark: handleForm.remark })
    ElMessage.success('处理完成')
    handleVisible.value = false
    loadData()
  } catch {
    // handled by interceptor
  }
}

const handleSearch = (params: any) => { loadData(params) }

const loadData = async (params?: any) => {
  loading.value = true
  try {
    const res = await getAlarmList({ page: page.value, pageSize: pageSize.value, ...params })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>