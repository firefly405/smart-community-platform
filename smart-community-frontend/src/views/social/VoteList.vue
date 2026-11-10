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
      @search="handleSearch"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
      @update:page="(p: number) => { page = p; loadData() }"
      @update:page-size="(s: number) => { pageSize = s; loadData() }"
    >
      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'primary' : 'info'" size="small">
          {{ row.status === 1 ? '进行中' : '已结束' }}
        </el-tag>
      </template>
      <template #actions="{ row }">
        <el-button v-if="row.status === 1" link type="primary" @click="openVote(row)">投票</el-button>
        <el-button link type="success" @click="openResult(row)">结果</el-button>
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

    <el-dialog v-model="voteVisible" title="参与投票" width="520px" destroy-on-close>
      <div v-loading="voteLoading">
        <p style="margin-bottom: 12px; color: #666">{{ currentVote?.title }}</p>
        <el-radio-group v-if="!isMulti" v-model="selectedSingle" style="display:flex;flex-direction:column;gap:10px">
          <el-radio v-for="opt in options" :key="opt.id" :value="opt.id" :label="opt.id">
            {{ opt.optionText || opt.text }}
          </el-radio>
        </el-radio-group>
        <el-checkbox-group v-else v-model="selectedMulti" style="display:flex;flex-direction:column;gap:10px">
          <el-checkbox v-for="opt in options" :key="opt.id" :label="opt.id" :value="opt.id">
            {{ opt.optionText || opt.text }}
          </el-checkbox>
        </el-checkbox-group>
        <el-form style="margin-top:16px" label-width="80px">
          <el-form-item label="用户ID">
            <el-input-number v-model="voteUserId" :min="1" />
          </el-form-item>
          <el-form-item label="用户名">
            <el-input v-model="voteUserName" placeholder="可选" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="voteVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmVote">提交投票</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resultVisible" title="投票结果" width="560px" destroy-on-close>
      <div v-loading="resultLoading">
        <p style="margin-bottom: 8px">
          <strong>{{ resultVote?.title }}</strong>
          <span style="margin-left:12px;color:#999">参与人数：{{ resultVote?.totalVotes ?? 0 }}</span>
        </p>
        <div v-if="!resultOptions.length" style="color:#999;padding:20px 0;text-align:center">暂无选项/票数</div>
        <div v-for="opt in resultOptions" :key="opt.id" style="margin-bottom:14px">
          <div style="display:flex;justify-content:space-between;margin-bottom:4px">
            <span>{{ opt.optionText || opt.text }}</span>
            <span>{{ opt.voteCount ?? opt.count ?? 0 }} 票</span>
          </div>
          <el-progress
            :percentage="resultPercent(opt)"
            :stroke-width="14"
            :text-inside="true"
          />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import type { FormField } from '@/components/ProForm.vue'
import {
  getVoteList,
  createVote,
  updateVote,
  deleteVote,
  getVoteById,
  getVoteOptions,
  submitVote,
  getVoteResult
} from '@/api/social'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('新增投票')
const editData = ref<any>(null)

const voteVisible = ref(false)
const voteLoading = ref(false)
const currentVote = ref<any>(null)
const options = ref<any[]>([])
const selectedSingle = ref<number | undefined>()
const selectedMulti = ref<number[]>([])
const voteUserId = ref(1)
const voteUserName = ref('演示用户')

const resultVisible = ref(false)
const resultLoading = ref(false)
const resultVote = ref<any>(null)
const resultOptions = ref<any[]>([])

const isMulti = computed(() => Number(currentVote.value?.isMulti ?? (currentVote.value?.voteType === 2 ? 1 : 0)) === 1)

const columns = [
  { prop: 'title', label: '投票标题', width: 200 },
  { prop: 'totalVotes', label: '参与人数', width: 90 },
  { prop: 'startTime', label: '开始时间', width: 170 },
  { prop: 'endTime', label: '结束时间', width: 170 },
  { prop: 'status', label: '状态', width: 90, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const formFields: FormField[] = [
  { prop: 'title', label: '投票标题', required: true },
  { prop: 'description', label: '描述', type: 'textarea' },
  { prop: 'options', label: '选项(每行一个)', type: 'textarea', required: true },
  { prop: 'isMulti', label: '多选', type: 'switch', activeValue: 1, inactiveValue: 0 },
  { prop: 'isAnonymous', label: '匿名', type: 'switch', activeValue: 1, inactiveValue: 0 },
  { prop: 'startTime', label: '开始时间', type: 'datetime' },
  { prop: 'endTime', label: '结束时间', type: 'datetime' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '进行中', value: 1 },
      { label: '已结束', value: 0 }
    ]
  }
]

async function loadData(searchParams?: any) {
  loading.value = true
  try {
    const res = await getVoteList({ page: page.value, pageSize: pageSize.value, ...searchParams })
    tableData.value = res.records
    total.value = res.total
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
  editData.value = { status: 1, isMulti: 0, options: '赞成\n反对\n弃权' }
  dialogTitle.value = '新增投票'
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editData.value = { ...row }
  dialogTitle.value = '编辑投票'
  dialogVisible.value = true
}

async function handleSubmit(data: any) {
  submitting.value = true
  try {
    if (data.id) {
      await updateVote(data)
      ElMessage.success('修改成功')
    } else {
      await createVote(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    editData.value = null
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: any) {
  await deleteVote(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function openVote(row: any) {
  currentVote.value = row
  selectedSingle.value = undefined
  selectedMulti.value = []
  voteUserId.value = 1
  voteUserName.value = '演示用户'
  voteVisible.value = true
  voteLoading.value = true
  try {
    const detail = await getVoteById(row.id)
    currentVote.value = detail || row
    const list = (detail as any)?.optionList || (detail as any)?.options
    if (Array.isArray(list) && list.length && typeof list[0] === 'object') {
      options.value = list
    } else {
      options.value = (await getVoteOptions(row.id)) || []
    }
  } finally {
    voteLoading.value = false
  }
}

async function confirmVote() {
  const optionIds = isMulti.value
    ? selectedMulti.value
    : (selectedSingle.value != null ? [selectedSingle.value] : [])
  if (!optionIds.length) {
    ElMessage.warning('请选择投票选项')
    return
  }
  submitting.value = true
  try {
    await submitVote(currentVote.value.id, {
      optionIds,
      userId: voteUserId.value,
      userName: voteUserName.value || `用户${voteUserId.value}`
    })
    ElMessage.success('投票成功')
    voteVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function openResult(row: any) {
  resultVisible.value = true
  resultLoading.value = true
  try {
    const res = await getVoteResult(row.id)
    resultVote.value = res
    resultOptions.value = (res as any)?.optionList || (res as any)?.options || []
    if (!resultOptions.value.length) {
      resultOptions.value = (await getVoteOptions(row.id)) || []
    }
  } finally {
    resultLoading.value = false
  }
}

function resultPercent(opt: any) {
  const count = Number(opt.voteCount ?? opt.count ?? 0)
  const totalCount = resultOptions.value.reduce((s, o) => s + Number(o.voteCount ?? o.count ?? 0), 0)
  if (!totalCount) return 0
  return Math.round((count / totalCount) * 100)
}

onMounted(() => loadData())
</script>