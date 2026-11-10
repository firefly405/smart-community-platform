<template>
  <div class="page-container">
    <SystemPageHeader>
      <template #meta><span>菜单节点 <strong>{{ nodeCount }}</strong></span></template>
    </SystemPageHeader>
    <div class="table-toolbar">
      <el-button type="primary" @click="handleAdd(null)">新增根菜单</el-button>
    </div>
    <el-table :data="tableData" border stripe row-key="id" v-loading="loading">
      <el-table-column prop="menuName" label="菜单名称" min-width="180" />
      <el-table-column prop="icon" label="图标" width="80" />
      <el-table-column prop="path" label="路由地址" width="160" />
      <el-table-column prop="permission" label="权限标识" width="160" />
      <el-table-column prop="menuType" label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.menuType === 1 ? 'info' : row.menuType === 2 ? 'success' : 'warning'">
            {{ row.menuType === 1 ? '目录' : row.menuType === 2 ? '菜单' : '按钮' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="70" align="center" />
      <el-table-column prop="visible" label="显示" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.visible === 0 ? 'info' : 'success'" size="small" effect="plain">
            {{ row.visible === 0 ? '隐藏' : '显示' }}
          </el-tag>
        </template>
      </el-table-column>
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
import { getMenuTree, createMenu, updateMenu, deleteMenu } from '@/api/system'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const editData = ref<any>(null)

const formFields: FormField[] = [
  { prop: 'parentId', label: '上级菜单', type: 'tree', options: [], filterable: true,
    treeProps: { label: 'menuName', children: 'children', value: 'id' } },
  { prop: 'menuName', label: '名称', required: true },
  { prop: 'menuType', label: '类型', type: 'radio', required: true, options: [
    { label: '目录', value: 1 }, { label: '菜单', value: 2 }, { label: '按钮', value: 3 }
  ]},
  { prop: 'icon', label: '图标' },
  { prop: 'path', label: '路由地址' },
  { prop: 'component', label: '组件路径' },
  { prop: 'permission', label: '权限标识' },
  { prop: 'sort', label: '排序', type: 'number' },
  { prop: 'visible', label: '是否显示', type: 'switch', activeValue: 1, inactiveValue: 0 },
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
    field.options = [{ id: 0, menuName: '顶级菜单', children: withoutBranch(tableData.value, excludedId) }]
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMenuTree()
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
  editData.value = parent ? { parentId: parent.id, menuType: 2, visible: 1, status: 1, sort: 0 } : { parentId: 0, menuType: 1, visible: 1, status: 1, sort: 0 }
  dialogTitle.value = '新增菜单'
  dialogVisible.value = true
}

function handleEdit(row: any) {
  setParentOptions(row.id)
  editData.value = { ...row }
  dialogTitle.value = '编辑菜单'
  dialogVisible.value = true
}

async function handleSubmit(data: any) {
  submitting.value = true
  try {
    if (data.id) {
      await updateMenu(data)
      ElMessage.success('修改成功')
    } else {
      await createMenu(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    editData.value = null
    loadData()
  } catch { /* handled */ }
  finally { submitting.value = false }
}

async function handleDelete(row: any) {
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>
