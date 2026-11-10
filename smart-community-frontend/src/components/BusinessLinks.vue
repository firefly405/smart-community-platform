<template>
  <el-dropdown v-if="links.length" trigger="click">
    <el-button link type="primary" size="small">关联业务 ▾</el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item v-for="link in links" :key="link.label" @click="router.push(link.to)">
          {{ link.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { relatedLinks } from '@/navigation/business'
const props = defineProps<{ row: Record<string, unknown> }>()
const route = useRoute()
const router = useRouter()
const links = computed(() => relatedLinks(route.path, props.row, route.fullPath))
</script>
