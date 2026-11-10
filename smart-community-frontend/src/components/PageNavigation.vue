<template>
  <div class="page-navigation">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/workspace' }">功能导航</el-breadcrumb-item>
      <el-breadcrumb-item v-if="section" :to="{ path: section.path }">{{ section.menuName }}</el-breadcrumb-item>
      <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
    </el-breadcrumb>
    <div class="context">
      <el-tag v-for="(value, key) in association" :key="key">{{ filterLabels[key] || key }}：{{ value }}</el-tag>
      <el-button v-if="Object.keys(association).length" link type="primary" @click="clearAssociation">查看全部</el-button>
      <el-button v-if="backTarget" @click="router.push(backTarget)">返回上一级</el-button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useNavigationStore } from '@/store/navigation'
import { associationQuery, filterLabels, returnTarget } from '@/navigation/business'
const route = useRoute()
const router = useRouter()
const navigation = useNavigationStore()
const section = computed(() => navigation.menus.find(m => m.path && route.path.startsWith(m.path + '/')))
const association = computed(() => associationQuery(route.path, route.query))
const backTarget = computed(() => returnTarget(router, route))
function clearAssociation() {
  const query = { ...route.query }
  for (const key of Object.keys(association.value)) delete query[key]
  void router.push({ path: route.path, query })
}
</script>
<style scoped>
.page-navigation { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 18px; }
.context { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
</style>
