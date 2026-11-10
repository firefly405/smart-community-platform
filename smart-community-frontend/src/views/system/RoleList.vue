<template>
  <div class="page-container">
    <SystemPageHeader>
      <template #meta><span>角色总数 <strong>{{ total }}</strong></span></template>
    </SystemPageHeader>
    <ProTable
      :data="tableData"
      :columns="columns"
      :total="total"
      :loading="loading"
      :page="page"
      :page-size="pageSize"
      :search-fields="searchFields"
      :actions-width="240"
      @search="handleSearch"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    >
      <template #dataScope="{ row }">
        <el-tag type="info" effect="plain" size="small">{{ dataScopeLabel(row.dataScope) }}</el-tag>
      </template>
      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
          {{ row.status === 1 ? '启用' : '禁用' }}
        </el-tag>
      </template>
      <template #actions="{ row }">
        <el-button type="primary" link size="small" @click="openPermissionDialog(row)">配置权限</el-button>
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

    <el-dialog v-model="permissionDialogVisible" title="配置菜单与按钮权限" width="620px" destroy-on-close>
      <div class="permission-intro">
        <div><strong>{{ activeRole?.roleName }}</strong><span>{{ activeRole?.roleCode }}</span></div>
        <el-button link type="primary" @click="toggleAll">{{ allChecked ? '取消全选' : '全选' }}</el-button>
      </div>
      <div class="permission-tree" v-loading="permissionLoading">
        <el-tree
          ref="menuTreeRef"
          :data="menuTree"
          node-key="id"
          show-checkbox
          default-expand-all
          :props="{ label: 'menuName', children: 'children' }"
          @check="syncAllChecked"
        >
          <template #default="{ data }">
            <span class="permission-node">
              <span>{{ data.menuName }}</span>
              <el-tag size="small" effect="plain" :type="data.menuType === 3 ? 'warning' : 'info'">
                {{ data.menuType === 1 ? '目录' : data.menuType === 2 ? '页面' : '按钮' }}
              </el-tag>
            </span>
          </template>
        </el-tree>
      </div>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permissionSaving" @click="savePermissions">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import SystemPageHeader from '@/components/SystemPageHeader.vue'
import type { FormField } from '@/components/ProForm.vue'
import {
  getRoleList, createRole, updateRole, deleteRole, getMenuTree,
  getRoleMenuIds, assignRoleMenus, type MenuItem
} from '@/api/system'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const editData = ref<any>(null)
const permissionDialogVisible = ref(false)
const permissionLoading = ref(false)
const permissionSaving = ref(false)
const activeRole = ref<any>(null)
const menuTree = ref<MenuItem[]>([])
const menuTreeRef = ref()
const allChecked = ref(false)

const columns = [
  { prop: 'roleName', label: '角色名称', width: 140 },
  { prop: 'roleCode', label: '角色编码', width: 140 },
  { prop: 'roleSort', label: '排序', width: 80 },
  { prop: 'dataScope', label: '数据范围', width: 120, slot: 'dataScope' },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const searchFields = [
  { prop: 'roleName', label: '角色名称', type: 'input' as const }
]

const formFields: FormField[] = [
  { prop: 'roleName', label: '角色名称', required: true },
  { prop: 'roleCode', label: '角色编码', required: true },
  { prop: 'roleSort', label: '排序', type: 'number' },
  { prop: 'dataScope', label: '数据范围', type: 'select', options: [
    { label: '全部', value: 1 },
    { label: '本部门', value: 2 },
    { label: '本部门及子部门', value: 3 },
    { label: '仅本人', value: 4 }
  ]},
  { prop: 'status', label: '状态', type: 'switch', activeValue: 1, inactiveValue: 0 },
  { prop: 'remark', label: '备注', type: 'textarea' }
]

function dataScopeLabel(value?: number) {
  return ({ 1: '全部数据', 2: '本部门', 3: '本部门及子部门', 4: '仅本人' } as Record<number, string>)[value ?? 4] || '未设置'
}

function collectIds(nodes: MenuItem[], leavesOnly = false): number[] {
  return nodes.flatMap(node => {
    const children = node.children || []
    const own = node.id && (!leavesOnly || children.length === 0) ? [Number(node.id)] : []
    return [...own, ...collectIds(children, leavesOnly)]
  })
}

async function loadData(searchParams?: any) {
  loading.value = true
  try {
    const res = await getRoleList({ page: page.value, pageSize: pageSize.value, ...searchParams })
    tableData.value = res.records
    total.value = res.total
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}

function handleSearch(params: any) {
  page.value = params.page
  pageSize.value = params.pageSize
  loadData(params)
}

function handleAdd() {
  editData.value = null
  dialogTitle.value = '新增角色'
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editData.value = { ...row }
  dialogTitle.value = '编辑角色'
  dialogVisible.value = true
}

async function openPermissionDialog(row: any) {
  activeRole.value = row
  permissionDialogVisible.value = true
  permissionLoading.value = true
  try {
    const [tree, assignedIds] = await Promise.all([getMenuTree(), getRoleMenuIds(row.id)])
    menuTree.value = tree
    const assigned = new Set(assignedIds.map(Number))
    const leafIds = collectIds(tree, true).filter(id => assigned.has(id))
    await nextTick()
    menuTreeRef.value?.setCheckedKeys(leafIds)
    syncAllChecked()
  } finally {
    permissionLoading.value = false
  }
}

function syncAllChecked() {
  const checked = menuTreeRef.value?.getCheckedKeys() || []
  allChecked.value = collectIds(menuTree.value, true).every(id => checked.includes(id))
}

function toggleAll() {
  menuTreeRef.value?.setCheckedKeys(allChecked.value ? [] : collectIds(menuTree.value, true))
  syncAllChecked()
}

async function savePermissions() {
  if (!activeRole.value?.id) return
  permissionSaving.value = true
  try {
    const checked = menuTreeRef.value?.getCheckedKeys() || []
    const halfChecked = menuTreeRef.value?.getHalfCheckedKeys() || []
    await assignRoleMenus(activeRole.value.id, [...new Set([...checked, ...halfChecked])].map(Number))
    ElMessage.success('权限配置成功')
    permissionDialogVisible.value = false
  } finally {
    permissionSaving.value = false
  }
}

async function handleSubmit(data: any) {
  submitting.value = true
  try {
    if (data.id) {
      await updateRole(data)
      ElMessage.success('修改成功')
    } else {
      await createRole(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    editData.value = null
    loadData()
  } catch { /* handled by interceptor */ }
  finally { submitting.value = false }
}

async function handleDelete(row: any) {
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped>
.permission-intro { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.permission-intro div { display: flex; align-items: baseline; gap: 10px; }
.permission-intro span { color: #667085; font-size: 13px; }
.permission-tree { min-height: 260px; max-height: 55vh; overflow: auto; padding: 12px; border: 1px solid #e4e7ed; border-radius: 6px; }
.permission-node { display: inline-flex; align-items: center; gap: 8px; }
</style>
