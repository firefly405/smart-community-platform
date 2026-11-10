<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <template #header>访客详情</template>
      <el-descriptions v-if="visitor" :column="2" border>
        <el-descriptions-item label="访客姓名">{{ visitor.visitorName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ visitor.visitorPhone }}</el-descriptions-item>
        <el-descriptions-item label="被访业主">{{ visitor.ownerName || '#' + visitor.ownerId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statuses[visitor.status] || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="预约开始">{{ visitor.visitStartTime }}</el-descriptions-item>
        <el-descriptions-item label="预约结束">{{ visitor.visitEndTime }}</el-descriptions-item>
        <el-descriptions-item label="来访事由" :span="2">{{ visitor.visitReason }}</el-descriptions-item>
        <el-descriptions-item label="通行凭证" :span="2">
          <el-input v-if="visitor.qrcode" :model-value="visitor.qrcode" readonly aria-label="访客通行凭证" />
          <span v-else>暂无通行凭证</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else-if="!loading" description="访客记录不存在或暂时无法读取" />
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getVisitorById } from '@/api/guard'
const route = useRoute()
const loading = ref(false)
const visitor = ref<any>(null)
const statuses: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已拒绝', 3: '已签到', 4: '已签退' }
onMounted(async () => {
  loading.value = true
  try { visitor.value = await getVisitorById(Number(route.params.id)) }
  catch { visitor.value = null }
  finally { loading.value = false }
})
</script>
