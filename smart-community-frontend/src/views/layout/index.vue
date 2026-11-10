<template>
  <div class="layout-container">
    <el-container>
      <button v-if="mobileMenuOpen" class="mobile-backdrop" aria-label="关闭导航" @click="mobileMenuOpen = false" />
      <el-aside width="200px" class="app-sidebar" :class="{ 'is-mobile-open': mobileMenuOpen }">
        <div class="logo">
          <span>智慧社区</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409eff"
          @select="mobileMenuOpen = false"
        >
          <el-menu-item index="/dashboard">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>

          <el-menu-item index="/workspace"><el-icon><MenuIcon /></el-icon><span>功能导航</span></el-menu-item>
          <template v-for="menu in menuTree" :key="menu.id || menu.path || menu.menuName">
            <!-- 目录：有子菜单 -->
            <el-sub-menu v-if="menu.children && menu.children.length" :index="menuKey(menu)">
              <template #title>
                <el-icon><component :is="resolveIcon(menu.icon)" /></el-icon>
                <span>{{ menu.menuName }}</span>
              </template>
              <el-menu-item
                v-for="child in visibleChildren(menu)"
                :key="child.id || child.path || child.menuName"
                :index="resolvePath(menu, child)"
              >
                {{ child.menuName }}
              </el-menu-item>
            </el-sub-menu>
            <!-- 单菜单 -->
            <el-menu-item v-else-if="menu.menuType !== 3" :index="resolvePath(null, menu)">
              <el-icon><component :is="resolveIcon(menu.icon)" /></el-icon>
              <span>{{ menu.menuName }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header>
          <div class="header-left">
            <el-button class="mobile-menu-button" text aria-label="打开导航" @click="mobileMenuOpen = true">
              <el-icon><MenuIcon /></el-icon>
            </el-button>
            <span class="welcome">欢迎回来，{{ userName }}</span>
          </div>
          <div class="header-right">
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="30" :src="authStore.userInfo?.avatar || ''">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <span>{{ userName }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="password">修改密码</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>
        <el-main>
          <PageNavigation />
          <ModulePageHeader v-if="showModuleHeader" />
          <router-view :key="route.fullPath" />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { ElMessageBox } from 'element-plus'
import {
  HomeFilled, Setting, House, Tools, Money, Lock, DataAnalysis,
  ShoppingCart, ChatDotRound, ChatLineSquare, User, Menu as MenuIcon
} from '@element-plus/icons-vue'
import type { MenuItem } from '@/api/system'
import { useNavigationStore } from '@/store/navigation'
import PageNavigation from '@/components/PageNavigation.vue'
import ModulePageHeader from '@/components/ModulePageHeader.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => (route.meta.activeMenu as string) || route.path)
const userName = computed(() => authStore.userInfo?.realName || '管理员')
const navigation = useNavigationStore()
const menuTree = computed(() => navigation.menus)
const mobileMenuOpen = ref(false)
const showModuleHeader = computed(() => {
  const path = route.path
  return path === '/dashboard' || ['/service/', '/charge/', '/guard/', '/life/', '/social/', '/report/', '/ai/']
    .some(prefix => path.startsWith(prefix))
})

const iconMap: Record<string, any> = {
  setting: Setting,
  house: House,
  service: Tools,
  tools: Tools,
  money: Money,
  security: Lock,
  lock: Lock,
  chart: DataAnalysis,
  life: ShoppingCart,
  shopping: ShoppingCart,
  comment: ChatDotRound,
  chat: ChatDotRound,
  ai: ChatLineSquare,
  menu: MenuIcon,
  user: User
}

function resolveIcon(icon?: string) {
  if (!icon) return MenuIcon
  const key = icon.toLowerCase().replace(/[_-]/g, '')
  // try exact then partial
  if (iconMap[icon]) return iconMap[icon]
  for (const k of Object.keys(iconMap)) {
    if (key.includes(k) || k.includes(key)) return iconMap[k]
  }
  return MenuIcon
}

function menuKey(menu: MenuItem) {
  return String(menu.id || menu.path || menu.menuName)
}

function visibleChildren(menu: MenuItem) {
  return (menu.children || []).filter((c) => c.menuType !== 3 && c.visible !== 0 && c.status !== 0)
}

function resolvePath(parent: MenuItem | null, child: MenuItem) {
  const raw = (child.path || '').trim()
  if (!raw) return '/'
  if (raw.startsWith('/')) return raw
  const parentPath = (parent?.path || '').trim()
  if (!parentPath) return `/${raw}`
  const base = parentPath.endsWith('/') ? parentPath.slice(0, -1) : parentPath
  return `${base.startsWith('/') ? base : '/' + base}/${raw}`
}

const handleCommand = (command: string) => {
  switch (command) {
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        authStore.clearAuth()
        router.push('/login')
      }).catch(() => {})
      break
    case 'profile':
      router.push('/system/user-center')
      break
    case 'password':
      router.push('/system/user-center#password')
      break
  }
}

onMounted(() => {
  navigation.load(router)
})
</script>

<style lang="scss" scoped>
.layout-container {
  width: 100%;
  height: 100vh;
}

.el-container {
  height: 100%;
}

.el-aside {
  background-color: #304156;
  overflow-y: auto;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #2b3a4a;
    color: #fff;
    font-size: 18px;
    font-weight: bold;
  }

  .el-menu {
    border-right: none;
  }
}

.el-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);

  .welcome {
    color: #666;
  }

  .user-info {
    display: flex;
    align-items: center;
    cursor: pointer;

    span {
      margin-left: 5px;
    }
  }
}

.el-main {
  padding: 20px;
  background-color: #f5f7fa;
}

.mobile-menu-button,
.mobile-backdrop {
  display: none;
}

@media (max-width: 768px) {
  .app-sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    z-index: 1002;
    transform: translateX(-100%);
    transition: transform .2s ease;

    &.is-mobile-open { transform: translateX(0); }
  }

  .mobile-backdrop {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 1001;
    width: 100%;
    height: 100%;
    padding: 0;
    border: 0;
    background: rgba(15, 23, 42, .42);
  }

  .mobile-menu-button {
    display: inline-flex;
    width: 36px;
    height: 36px;
    margin-right: 6px;
    font-size: 20px;
  }

  .el-header { height: 54px; padding: 0 12px; }
  .header-left { display: flex; align-items: center; min-width: 0; }
  .welcome { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
  .el-main { padding: 12px; }
}
</style>
