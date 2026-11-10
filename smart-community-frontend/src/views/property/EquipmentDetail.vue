<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>设备详情 - {{ detail.equipmentName }}</span>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="设备名称">{{ detail.equipmentName }}</el-descriptions-item>
        <el-descriptions-item label="设备编码">{{ detail.code || detail.id }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">{{ detail.equipmentType }}</el-descriptions-item>
        <el-descriptions-item label="位置">{{ detail.location }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ detail.brand || '-' }}</el-descriptions-item>
        <el-descriptions-item label="型号">{{ detail.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'danger'" size="small">
            {{ detail.status === 1 ? '正常' : '故障' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="安装日期">{{ detail.installDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">维保记录</el-divider>
      <el-table :data="maintainList" border style="width:100%">
        <el-table-column prop="maintainType" label="维保类型" width="100" />
        <el-table-column prop="maintainContent" label="维保内容" />
        <el-table-column prop="maintainerName" label="处理人" width="100" />
        <el-table-column prop="maintainDate" label="维保日期" width="120" />
        <el-table-column prop="cost" label="费用" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 2 ? 'success' : 'info'" size="small">
              {{ row.status === 2 ? '完成' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getEquipmentById, getEquipmentMaintainList, type EquipmentItem } from '@/api/property'

const route = useRoute()
const loading = ref(false)
const detail = ref<EquipmentItem>({} as EquipmentItem)
const maintainList = ref<any[]>([])

onMounted(async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const [eq, maintains] = await Promise.all([getEquipmentById(id), getEquipmentMaintainList(id)])
    detail.value = eq
    maintainList.value = Array.isArray(maintains) ? maintains : (maintains as any)?.records || []
  } finally { loading.value = false }
})
</script>
