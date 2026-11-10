<template>
  <div class="community-images">
    <div v-for="(url, index) in modelValue" :key="url" class="image-thumb"><img :src="url" alt="已上传图片" /><el-button circle size="small" :icon="Close" :disabled="uploading" :aria-label="'移除图片' + (index + 1)" @click="emit('update:modelValue', modelValue.filter((_, i) => i !== index))" /></div>
    <el-upload v-if="modelValue.length < 5" :show-file-list="false" :before-upload="upload" accept="image/jpeg,image/png" :disabled="uploading"><el-button :icon="Picture" :loading="uploading">添加图片</el-button></el-upload>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Picture } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { socialBase } from '@/api/community'
const props = defineProps<{ modelValue: string[] }>()
const emit = defineEmits<{ 'update:modelValue': [value: string[]]; uploading: [value: boolean] }>()
const uploading = ref(false)
async function upload(file: File) {
  if (uploading.value || props.modelValue.length >= 5) return false
  if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 2 * 1024 * 1024) { ElMessage.warning('请选择2MB以内的 JPG 或 PNG 图片'); return false }
  uploading.value = true; emit('uploading', true)
  try {
    const form = new FormData(); form.append('file', file)
    const result = await request.post<any, { url: string }>(socialBase + '/images', form, { headers: { 'Content-Type': 'multipart/form-data' } })
    emit('update:modelValue', [...props.modelValue, result.url])
  } catch {} finally { uploading.value = false; emit('uploading', false) }
  return false
}
</script>
<style scoped>
.community-images { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }.image-thumb { position: relative; width: 76px; height: 76px; }.image-thumb img { width: 100%; height: 100%; object-fit: cover; border: 1px solid #dce5df; border-radius: 4px; }.image-thumb .el-button { position: absolute; top: 0; right: 0; }
</style>
