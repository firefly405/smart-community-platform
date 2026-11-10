<template>
  <div class="page-container">
    <SystemPageHeader>
      <template #meta><span>部门节点 <strong>{{ nodeCount }}</strong></span></template>
    </SystemPageHeader>
    <div class="table-toolbar">
      <el-button type="primary" @click="handleAdd(null)">新增根部门</el-button>
    </div>
    <el-table :data="tableData" border stripe row-key="id" v-loading="loading">
      <el-table-column prop="deptName" label="部门名称" min-width="180" />
      <el-table-column prop="deptCode" label="部门编码" width="140" />
      <el-table-column prop="leader" label="负责人" width="120" />
      <el-table-column prop="phone" label="联系电话" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="sort" label="排序" width="70" align="center" />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleAdd(row)">添加</el-button>
          <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button type="danger" link size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

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
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import ProDialog from '@/components/ProDialog.vue'
import SystemPageHeader from '@/components/SystemPageHeader.vue'
import type { FormField } from '@/components/ProForm.vue'
import { getDeptTree, createDept, updateDept, deleteDept } from '@/api/system'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增部门')
const editData = ref<any>(null)

const formFields: FormField[] = [
  { prop: 'parentId', label: '上级部门', type: 'tree', options: [], filterable: true,
    treeProps: { label: 'deptName', children: 'children', value: 'id' } },
  { prop: 'deptName', label: '部门名称', required: true },
  { prop: 'deptCode', label: '部门编码' },
  { prop: 'leader', label: '负责人' },
  { prop: 'phone', label: '联系电话' },
  { prop: 'email', label: '邮箱' },
  { prop: 'sort', label: '排序', type: 'number' },
  { prop: 'status', label: '状态', type: 'switch', activeValue: 1, inactiveValue: 0 }
]

const nodeCount = computed(() => countNodes(tableData.value))

function countNodes(nodes: any[]): number {
  return (nodes || []).reduce((total, node) => total + 1 + countNodes(node.children || []), 0)
}

function withoutBranch(nodes: any[], excludedId?: number): any[] {
  return (nodes || [])
    .filter(node => Number(node.id) !== Number(excludedId))
    .map(node => ({ ...node, children: withoutBranch(node.children || [], excludedId) }))
}

function setParentOptions(excludedId?: number) {
  const field = formFields.find(item => item.prop === 'parentId')
  if (field) {
    field.options = [{ id: 0, deptName: '顶级部门', children: withoutBranch(tableData.value, excludedId) }]
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getDeptTree()
    flattenTree(res)
    tableData.value = res
  } catch { /* handled */ }
  finally { loading.value = false }
}

function flattenTree(tree: any[], parentId?: number) {
  if (!tree) return
  for (const node of tree) {
    node.parentId = parentId || 0
    if (node.children) {
      flattenTree(node.children, node.id)
    }
  }
}

function handleAdd(parent: any) {
  setParentOptions()
  editData.value = parent ? { parentId: parent.id, status: 1, sort: 0 } : { parentId: 0, status: 1, sort: 0 }
  dialogTitle.value = '新增部门'
  dialogVisible.value = true
}

function handleEdit(row: any) {
  setParentOptions(row.id)
  editData.value = { ...row }
  dialogTitle.value = '编辑部门'
  dialogVisible.value = true
}

async function handleSubmit(data: any) {
  submitting.value = true
  try {
    if (data.id) {
      await updateDept(data)
      ElMessage.success('修改成功')
    } else {
      await createDept(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    editData.value = null
    loadData()
  } catch { /* handled */ }
  finally { submitting.value = false }
}

async function handleDelete(row: any) {
  await deleteDept(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>
