<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑公告' : '新增公告' }}</span>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width:700px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="类型" prop="noticeType">
          <el-select v-model="form.noticeType" placeholder="请选择">
            <el-option label="通知" :value="1" />
            <el-option label="活动" :value="2" />
            <el-option label="提醒" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否置顶">
          <el-switch v-model="form.isTop" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { returnTarget } from '@/navigation/business'
import { ElMessage } from 'element-plus'
import { getNoticeById, createNotice, updateNotice, type NoticeItem } from '@/api/property'

const route = useRoute()
const router = useRouter()
const goBack = () => router.push(returnTarget(router, route) || '/workspace')
const submitting = ref(false)
const isEdit = ref(false)

const form = reactive<NoticeItem>({
  title: '', content: '', noticeType: 1, isTop: 0, status: 0
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

onMounted(async () => {
  const id = route.params.id
  if (id) {
    isEdit.value = true
    try {
      const data = await getNoticeById(Number(id))
      Object.assign(form, data)
    } catch { /* handled */ }
  }
})

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateNotice(form)
      ElMessage.success('修改成功')
    } else {
      await createNotice(form)
      ElMessage.success('创建成功')
    }
    goBack()
  } finally { submitting.value = false }
}
</script>
