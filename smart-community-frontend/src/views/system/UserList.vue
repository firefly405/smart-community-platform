<template>
  <div class="page-container">
    <SystemPageHeader>
      <template #meta><span>账号总数 <strong>{{ total }}</strong></span></template>
    </SystemPageHeader>
    <ProTable
      :data="tableData"
      :columns="columns"
      :total="total"
      :loading="loading"
      :page="page"
      :page-size="pageSize"
      :search-fields="searchFields"
      @search="handleSearch"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
      :actions-width="240"
    >
      <template #gender="{ row }">{{ genderLabel(row.gender) }}</template>
      <template #userType="{ row }">
        <el-tag size="small" effect="plain">{{ userTypeLabel(row.userType) }}</el-tag>
      </template>
      <template #dept="{ row }">{{ deptNames.get(Number(row.deptId)) || '未分配' }}</template>
      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
          {{ row.status === 1 ? '启用' : '禁用' }}
        </el-tag>
      </template>
      <template #actions="{ row }">
        <el-button type="primary" link size="small" @click="openRoleDialog(row)">分配角色</el-button>
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

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="520px" destroy-on-close>
      <div class="role-dialog-copy">
        <strong>{{ activeUser?.realName || activeUser?.username }}</strong>
        <span>可为一个账号分配多个业务角色。</span>
      </div>
      <el-checkbox-group v-model="selectedRoleIds" class="role-options" v-loading="roleLoading">
        <el-checkbox v-for="role in allRoles" :key="role.id" :value="role.id" border>
          {{ role.roleName }}
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSaving" @click="saveRoles">保存角色</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import SystemPageHeader from '@/components/SystemPageHeader.vue'
import type { FormField } from '@/components/ProForm.vue'
import {
  getUserList, createUser, updateUser, deleteUser, getDeptTree,
  getAllRoles, getUserRoles, assignUserRoles, type RoleItem
} from '@/api/system'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const editData = ref<any>(null)
const deptTree = ref<any[]>([])
const deptNames = ref(new Map<number, string>())
const allRoles = ref<RoleItem[]>([])
const roleDialogVisible = ref(false)
const roleLoading = ref(false)
const roleSaving = ref(false)
const activeUser = ref<any>(null)
const selectedRoleIds = ref<number[]>([])
const deptSearchOptions: { label: string; value: number }[] = []

const columns = [
  { prop: 'username', label: '用户名', width: 120 },
  { prop: 'realName', label: '姓名', width: 100 },
  { prop: 'deptId', label: '所属部门', width: 150, slot: 'dept' },
  { prop: 'userType', label: '账号类型', width: 100, slot: 'userType' },
  { prop: 'phone', label: '手机号', width: 130 },
  { prop: 'email', label: '邮箱', minWidth: 180 },
  { prop: 'gender', label: '性别', width: 70, slot: 'gender' },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const searchFields = [
  { prop: 'username', label: '用户名', type: 'input' as const },
  { prop: 'phone', label: '手机号', type: 'input' as const },
  { prop: 'deptId', label: '所属部门', type: 'select' as const, options: deptSearchOptions },
  { prop: 'status', label: '状态', type: 'select' as const, options: [
    { label: '启用', value: 1 }, { label: '禁用', value: 0 }
  ]}
]

const formFields: FormField[] = [
  { prop: 'username', label: '用户名', required: true },
  { prop: 'password', label: '密码', type: 'input', inputType: 'password', required: true },
  { prop: 'realName', label: '姓名' },
  { prop: 'phone', label: '手机号' },
  { prop: 'email', label: '邮箱' },
  { prop: 'deptId', label: '所属部门', type: 'tree', options: deptTree.value, filterable: true,
    treeProps: { label: 'deptName', children: 'children', value: 'id' } },
  { prop: 'userType', label: '账号类型', type: 'select', options: [
    { label: '系统用户', value: 1 }, { label: '物业人员', value: 2 }, { label: '社区居民', value: 3 }
  ]},
  { prop: 'gender', label: '性别', type: 'select', options: [
    { label: '未知', value: 0 }, { label: '男', value: 1 }, { label: '女', value: 2 }
  ]},
  { prop: 'status', label: '状态', type: 'switch', activeValue: 1, inactiveValue: 0 }
]

function genderLabel(value?: number) {
  return ({ 0: '未知', 1: '男', 2: '女' } as Record<number, string>)[value ?? 0] || '未知'
}

function userTypeLabel(value?: number) {
  return ({ 1: '系统用户', 2: '物业人员', 3: '社区居民' } as Record<number, string>)[value ?? 1] || '其他'
}

function indexDepartments(nodes: any[], depth = 0) {
  for (const node of nodes || []) {
    deptNames.value.set(Number(node.id), node.deptName)
    deptSearchOptions.push({ label: `${'　'.repeat(depth)}${node.deptName}`, value: Number(node.id) })
    indexDepartments(node.children || [], depth + 1)
  }
}

async function loadOptions() {
  const [depts, roles] = await Promise.all([getDeptTree(), getAllRoles()])
  deptTree.value = depts
  deptNames.value.clear()
  deptSearchOptions.splice(0)
  indexDepartments(depts)
  const deptField = formFields.find(field => field.prop === 'deptId')
  if (deptField) deptField.options = depts
  allRoles.value = roles.filter(role => role.status !== 0)
}

async function loadData(searchParams?: any) {
  loading.value = true
  try {
    const res = await getUserList({ page: page.value, pageSize: pageSize.value, ...searchParams })
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
  dialogTitle.value = '新增用户'
  formFields.forEach(f => {
    if (f.prop === 'password') f.hidden = false
  })
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editData.value = { ...row, password: '' }
  dialogTitle.value = '编辑用户'
  formFields.forEach(f => {
    if (f.prop === 'password') f.hidden = true
  })
  dialogVisible.value = true
}

async function openRoleDialog(row: any) {
  activeUser.value = row
  selectedRoleIds.value = []
  roleDialogVisible.value = true
  roleLoading.value = true
  try {
    selectedRoleIds.value = (await getUserRoles(row.id)).map(Number)
  } finally {
    roleLoading.value = false
  }
}

async function saveRoles() {
  if (!activeUser.value?.id) return
  roleSaving.value = true
  try {
    await assignUserRoles(activeUser.value.id, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
  } finally {
    roleSaving.value = false
  }
}

async function handleSubmit(data: any) {
  submitting.value = true
  try {
    if (data.id) {
      await updateUser(data)
      ElMessage.success('修改成功')
    } else {
      await createUser(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    editData.value = null
    loadData()
  } catch { /* handled by interceptor */ }
  finally { submitting.value = false }
}

async function handleDelete(row: any) {
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(async () => {
  try { await loadOptions() } catch { /* handled by interceptor */ }
  loadData()
})
</script>

<style scoped>
.role-dialog-copy { display: flex; align-items: baseline; gap: 10px; margin-bottom: 18px; }
.role-dialog-copy span { color: #667085; font-size: 13px; }
.role-options { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; min-height: 48px; }
.role-options :deep(.el-checkbox) { width: 100%; margin: 0; }
@media (max-width: 640px) { .role-options { grid-template-columns: 1fr; } }
</style>
