<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>报修详情 - {{ detail.repairNo }}</span>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="报修单号">{{ detail.repairNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[detail.status as number]?.type" size="small">{{ statusMap[detail.status as number]?.text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.repairType }}</el-descriptions-item>
        <el-descriptions-item label="内容" :span="2">{{ detail.content }}</el-descriptions-item>
        <el-descriptions-item label="房屋ID">{{ detail.houseId }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ detail.handlerName }}</el-descriptions-item>
        <el-descriptions-item label="处理结果" :span="2">{{ detail.result || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理图片" :span="2">
          <span v-if="detail.images">{{ detail.images }}</span><span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="评分">{{ detail.score ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="评价">{{ detail.evaluation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getRepairById, type RepairItem } from '@/api/property'

const route = useRoute()
const loading = ref(false)
const detail = ref<RepairItem>({} as RepairItem)

const statusMap: Record<number, { text: string; type: 'success' | 'info' | 'primary' | 'warning' | 'danger' }> = {
  0: { text: '待处理', type: 'warning' }, 1: { text: '已派单', type: 'primary' },
  2: { text: '处理中', type: 'info' }, 3: { text: '已完成', type: 'success' }, 4: { text: '已取消', type: 'danger' }
}

onMounted(async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    detail.value = await getRepairById(id)
  } finally { loading.value = false }
})
</script>
