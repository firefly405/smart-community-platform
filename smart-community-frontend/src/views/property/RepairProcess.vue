<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>处理报修</span>
        </div>
      </template>
      <el-form :model="form" label-width="100px" style="max-width:600px">
        <el-form-item label="处理结果" required>
          <el-input v-model="form.result" type="textarea" :rows="4" placeholder="请填写处理结果" />
        </el-form-item>
        <el-form-item label="处理图片">
          <el-input v-model="form.images" placeholder="图片URL，多个用逗号分隔" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交处理</el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { returnTarget } from '@/navigation/business'
import { ElMessage } from 'element-plus'
import { processRepair } from '@/api/property'

const route = useRoute()
const router = useRouter()
const goBack = () => router.push(returnTarget(router, route) || '/workspace')
const submitting = ref(false)
const form = reactive({ result: '', images: '' })

async function handleSubmit() {
  if (!form.result) { ElMessage.warning('请填写处理结果'); return }
  submitting.value = true
  try {
    await processRepair(Number(route.params.id), form)
    ElMessage.success('处理成功')
    goBack()
  } finally { submitting.value = false }
}
</script>
