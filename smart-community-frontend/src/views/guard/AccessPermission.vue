<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <el-button @click="$router.back()" link><el-icon><ArrowLeft /></el-icon> 返回</el-button>
          <span>门禁权限配置</span>
        </div>
      </template>
      <el-form :inline="true" style="margin-bottom:12px">
        <el-form-item label="选择设备">
          <el-select v-model="selectedDeviceId" placeholder="请选择门禁设备" @change="loadPermissions" style="width:280px">
            <el-option
              v-for="d in deviceList"
              :key="d.id"
              :label="`${d.accessName || d.name || d.accessNo || d.id} (${d.location || '-'})`"
              :value="d.id!"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template v-if="selectedDeviceId">
        <el-form :model="permForm" label-width="120px" style="max-width:560px">
          <el-form-item label="授权用户ID">
            <el-select v-model="permForm.userIds" multiple filterable allow-create default-first-option placeholder="输入用户/业主ID后回车">
            </el-select>
          </el-form-item>
          <el-form-item label="有效期起">
            <el-date-picker v-model="permForm.validFrom" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" />
          </el-form-item>
          <el-form-item label="有效期止">
            <el-date-picker v-model="permForm.validTo" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSave">保存权限</el-button>
            <el-button @click="loadPermissions">刷新</el-button>
          </el-form-item>
        </el-form>
        <el-alert
          v-if="savedHint"
          type="success"
          :closable="false"
          :title="savedHint"
          style="margin-top:12px;max-width:560px"
        />
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAccessDeviceList,
  getAccessPermissions,
  setAccessPermissions,
  type AccessDeviceItem
} from '@/api/guard'

const loading = ref(false)
const saving = ref(false)
const deviceList = ref<AccessDeviceItem[]>([])
const selectedDeviceId = ref<number | null>(null)
const savedHint = ref('')
const permForm = reactive({ userIds: [] as (number | string)[], validFrom: '', validTo: '' })

onMounted(async () => {
  loading.value = true
  try {
    const res = await getAccessDeviceList({ page: 1, pageSize: 100 })
    deviceList.value = res.records || []
  } finally {
    loading.value = false
  }
})

async function loadPermissions() {
  savedHint.value = ''
  permForm.userIds = []
  permForm.validFrom = ''
  permForm.validTo = ''
  if (!selectedDeviceId.value) return
  loading.value = true
  try {
    const res: any = await getAccessPermissions(selectedDeviceId.value)
    const userIds = (res?.userIds || []).map((x: any) => Number(x))
    permForm.userIds = userIds
    const perms = res?.permissions || []
    if (perms.length > 0) {
      const first = perms[0]
      if (first.startTime) permForm.validFrom = String(first.startTime).slice(0, 10)
      if (first.endTime) permForm.validTo = String(first.endTime).slice(0, 10)
    }
    savedHint.value = `已加载 ${userIds.length} 条授权`
  } catch {
    /* handled */
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!selectedDeviceId.value) return
  saving.value = true
  try {
    await setAccessPermissions(selectedDeviceId.value, {
      userIds: permForm.userIds.map(Number).filter((n) => !Number.isNaN(n)),
      validFrom: permForm.validFrom || undefined,
      validTo: permForm.validTo || undefined
    })
    ElMessage.success('权限保存成功')
    await loadPermissions()
  } finally {
    saving.value = false
  }
}
</script>
