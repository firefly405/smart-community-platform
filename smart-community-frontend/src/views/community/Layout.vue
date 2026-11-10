<template>
  <div class="resident-shell">
    <header class="resident-header">
      <router-link to="/community/home" class="brand"><el-icon><OfficeBuilding /></el-icon><span>智慧社区<small>业主服务</small></span></router-link>
      <div class="account"><el-tag type="success" effect="plain" disable-transitions>普通业主</el-tag><router-link to="/community/profile">{{ auth.userInfo?.realName || auth.userInfo?.username }}</router-link><el-button text :icon="SwitchButton" @click="signOut">退出</el-button></div>
    </header>
    <div class="resident-body">
      <nav aria-label="业主服务导航">
        <router-link v-for="item in residentNav" :key="item.path" :to="'/community/' + item.path" :class="{ 'nav-current': item.path === 'posts' && route.path.startsWith('/community/posts/') }"><el-icon><component :is="item.icon" /></el-icon><span>{{ item.title }}</span><el-icon class="nav-arrow"><ArrowRight /></el-icon></router-link>
      </nav>
      <main><router-view /></main>
    </div>
    <footer>智慧社区 · 业主服务中心</footer>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import { logout } from '@/api/auth'
import { residentNav } from './navigation'
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
async function signOut() {
  try { await logout() } finally { auth.clearAuth(); await router.replace('/community/login') }
}
</script>

<style scoped lang="scss">
.resident-shell { min-height: 100vh; background: #f4f6f7; color: #253b36; --el-color-primary: #176b5b; }
.resident-header { min-height: 80px; background: white; border-bottom: 1px solid #e1e8e5; display: flex; align-items: center; justify-content: space-between; padding: 16px max(24px, calc((100vw - 1320px) / 2)); gap: 16px; }
.brand { display: flex; align-items: center; gap: 12px; color: #176b5b; text-decoration: none; font-size: 24px; font-weight: 700; }
.brand > .el-icon { font-size: 32px; }
.brand small { display: block; font-size: 11px; font-weight: 400; color: #74837c; margin-top: 3px; }
.account { display: flex; align-items: center; gap: 12px; font-size: 14px; flex-wrap: wrap; justify-content: flex-end; }
.account a { text-decoration: none; color: #304d43; overflow-wrap: anywhere; }
.account .el-tag { flex-shrink: 0; }
.resident-body { max-width: 1320px; margin: 28px auto; padding: 0 24px; display: grid; grid-template-columns: 190px minmax(0, 1fr); gap: 32px; min-height: calc(100vh - 180px); }
nav { display: flex; flex-direction: column; gap: 6px; }
nav a { display: flex; align-items: center; gap: 12px; min-height: 45px; padding: 10px 14px; color: #62716c; text-decoration: none; border-radius: 6px; font-size: 14px; }
nav a:hover { background: #e9efed; }
nav a.router-link-active, nav a.nav-current { background: #176b5b; color: white; }
.nav-arrow { margin-left: auto; font-size: 12px; }
main { min-width: 0; }
footer { text-align: center; color: #89948e; padding: 12px 24px 24px; font-size: 12px; }
@media (max-width: 780px) {
  .resident-header { padding: 16px; align-items: flex-start; }
  .brand { font-size: 21px; flex-shrink: 0; }
  .account { gap: 5px; max-width: 55%; }
  .resident-body { display: block; margin: 0; padding: 0 16px; }
  nav { flex-direction: row; overflow-x: auto; gap: 4px; padding: 16px 0; margin-bottom: 10px; }
  nav a { flex: 0 0 auto; padding: 10px; min-height: 40px; gap: 6px; }
  .nav-arrow { display: none; }
}
</style>
