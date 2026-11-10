<template>
  <div class="tenant-shell">
    <header class="tenant-header">
      <router-link to="/tenant/home" class="brand">
        <span class="brand-mark"><el-icon><Key /></el-icon></span>
        <span>智慧社区<small>租户服务中心</small></span>
      </router-link>
      <div class="account">
        <el-tag type="warning" effect="plain" disable-transitions>社区租户</el-tag>
        <router-link to="/tenant/profile">{{ auth.userInfo?.realName || auth.userInfo?.username }}</router-link>
        <el-button text :icon="SwitchButton" @click="signOut">退出</el-button>
      </div>
    </header>

    <div class="tenant-body">
      <nav aria-label="租户服务导航">
        <router-link v-for="item in tenantNav" :key="item.path" :to="`/tenant/${item.path}`">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
          <el-icon class="nav-arrow"><ArrowRight /></el-icon>
        </router-link>
      </nav>
      <main><router-view /></main>
    </div>

    <footer>智慧社区 · 租户服务中心</footer>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Key, SwitchButton } from '@element-plus/icons-vue'
import { logout } from '@/api/auth'
import { useAuthStore } from '@/store/auth'
import { tenantNav } from './navigation'

const auth = useAuthStore()
const router = useRouter()

async function signOut() {
  try { await logout() }
  finally {
    auth.clearAuth()
    await router.replace('/tenant/login')
  }
}
</script>

<style scoped lang="scss">
.tenant-shell {
  min-height: 100vh;
  color: #263b3f;
  background: #f3f6f5;
  --el-color-primary: #236979;
}
.tenant-header {
  display: flex;
  min-height: 80px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px max(24px, calc((100vw - 1320px) / 2));
  background: #fff;
  border-bottom: 1px solid #dce6e4;
  box-shadow: 0 2px 8px rgb(29 65 70 / 5%);
}
.brand { display: flex; align-items: center; gap: 12px; color: #1f5965; font-size: 23px; font-weight: 700; text-decoration: none; }
.brand-mark { display: grid; width: 40px; height: 40px; place-items: center; color: #fff; background: #236979; border-radius: 7px; font-size: 21px; }
.brand small { display: block; margin-top: 3px; color: #6e7f7d; font-size: 11px; font-weight: 400; }
.account { display: flex; align-items: center; justify-content: flex-end; gap: 12px; font-size: 14px; }
.account a { color: #31565c; overflow-wrap: anywhere; text-decoration: none; }
.tenant-body { display: grid; grid-template-columns: 205px minmax(0, 1fr); gap: 30px; width: min(1320px, 100%); min-height: calc(100vh - 178px); margin: 26px auto; padding: 0 24px; }
nav { display: flex; flex-direction: column; gap: 5px; }
nav a { display: flex; min-height: 46px; align-items: center; gap: 11px; padding: 10px 13px; color: #617275; border-radius: 6px; font-size: 14px; text-decoration: none; }
nav a:hover { color: #1f5965; background: #e6efee; }
nav a.router-link-active { color: #fff; background: #236979; }
.nav-arrow { margin-left: auto; font-size: 12px; }
main { min-width: 0; }
footer { padding: 12px 24px 24px; color: #879391; font-size: 12px; text-align: center; }

@media (max-width: 780px) {
  .tenant-header { align-items: flex-start; padding: 14px 16px; }
  .brand { flex-shrink: 0; font-size: 20px; }
  .brand-mark { width: 36px; height: 36px; }
  .account { max-width: 52%; flex-wrap: wrap; gap: 5px; }
  .tenant-body { display: block; margin: 0; padding: 0 16px; }
  nav { flex-direction: row; gap: 4px; margin-bottom: 8px; padding: 15px 0; overflow-x: auto; }
  nav a { flex: 0 0 auto; min-height: 40px; padding: 9px; gap: 6px; }
  .nav-arrow { display: none; }
}

@media (max-width: 430px) {
  .tenant-header { min-height: 72px; }
  .brand { font-size: 18px; }
  .brand-mark { display: none; }
  .account .el-tag { display: none; }
}
</style>

