<template>
  <section class="system-heading">
    <div class="heading-row">
      <div class="heading-copy">
        <span class="heading-icon" aria-hidden="true"><el-icon><component :is="current.icon" /></el-icon></span>
        <div><span class="eyebrow">系统管理</span><h1>{{ current.label }}</h1><p>{{ current.summary }}</p></div>
      </div>
      <div v-if="$slots.meta" class="heading-meta"><slot name="meta" /></div>
    </div>
    <nav class="section-nav" aria-label="系统管理功能">
      <router-link v-for="item in pages" :key="item.path" :to="item.path" :class="{ active: route.path === item.path }">
        <el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span>
      </router-link>
    </nav>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Avatar, Collection, Cpu, Menu as MenuIcon, OfficeBuilding, Tickets, User, UserFilled } from '@element-plus/icons-vue'
const route = useRoute()
const pages = [
  { path: '/system/user', label: '用户管理', summary: '账号、部门与角色归属', icon: User },
  { path: '/system/role', label: '角色管理', summary: '数据范围与菜单权限', icon: Avatar },
  { path: '/system/menu', label: '菜单管理', summary: '导航、页面与按钮权限', icon: MenuIcon },
  { path: '/system/dept', label: '部门管理', summary: '物业组织与人员归属', icon: OfficeBuilding },
  { path: '/system/log', label: '操作日志', summary: '关键操作与异常追溯', icon: Tickets },
  { path: '/system/login-log', label: '登录日志', summary: '登录结果与访问终端', icon: Collection },
  { path: '/system/user-center', label: '个人中心', summary: '个人资料与登录密码', icon: UserFilled },
  { path: '/system/ai-config', label: 'AI配置', summary: '模型服务、API密钥与连接状态', icon: Cpu }
]
const current = computed(() => pages.find(item => item.path === route.path) || pages[0])
</script>

<style scoped>
.system-heading { margin-bottom: 18px; }
.heading-row { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 16px; }
.heading-copy { display: flex; align-items: center; min-width: 0; gap: 13px; }
.heading-icon { display: grid; flex: 0 0 42px; width: 42px; height: 42px; place-items: center; color: #1f5f99; background: #eaf2fb; border: 1px solid #d8e6f4; border-radius: 7px; font-size: 21px; }
.eyebrow { display: block; margin-bottom: 2px; color: #7b8794; font-size: 12px; }
h1 { margin: 0; color: #1f2937; font-size: 22px; line-height: 1.3; letter-spacing: 0; }
p { margin: 3px 0 0; color: #667085; font-size: 13px; }
.heading-meta { display: flex; align-items: center; gap: 14px; color: #667085; font-size: 13px; }
.heading-meta :deep(strong) { color: #1f2937; font-size: 18px; }
.section-nav { display: flex; overflow-x: auto; border-bottom: 1px solid #dfe4ea; scrollbar-width: none; }
.section-nav::-webkit-scrollbar { display: none; }
.section-nav a { display: inline-flex; flex: 0 0 auto; align-items: center; gap: 6px; padding: 10px 14px; color: #667085; border-bottom: 2px solid transparent; font-size: 13px; text-decoration: none; }
.section-nav a:hover { color: #245f92; background: #f4f7fa; }
.section-nav a.active { color: #1f5f99; border-bottom-color: #1f5f99; font-weight: 600; }
@media (max-width: 720px) { .heading-row { align-items: flex-start; flex-direction: column; } .heading-meta { padding-left: 55px; } }
</style>
