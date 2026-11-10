<template>
  <section class="workspace-page">
    <header class="page-heading">
      <div class="heading-main">
        <span class="heading-icon" aria-hidden="true"><el-icon><MenuIcon /></el-icon></span>
        <div><h2>{{ pageTitle }}</h2><p>{{ pageDescription }}</p></div>
      </div>
      <div v-if="!navigation.loading && !navigation.error" class="summary" aria-live="polite">
        <strong>{{ modules.length }}</strong> 个模块 <span></span> <strong>{{ entryCount }}</strong> 项功能
      </div>
    </header>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索账单、报修、访客等功能" clearable class="search" aria-label="搜索功能">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <span v-if="keyword.trim()" class="search-status">已显示匹配结果</span>
    </div>
    <el-alert v-if="navigation.error" type="error" :closable="false" title="菜单加载失败">
      <el-button @click="navigation.load(router)">重新加载</el-button>
    </el-alert>
    <nav v-loading="navigation.loading" class="modules" aria-label="业务功能导航">
      <article v-for="module in modules" :key="module.path" class="module-card" :class="`module-card--${module.visual.tone}`">
        <div class="module-header">
          <span class="module-icon" aria-hidden="true"><el-icon><component :is="module.visual.icon" /></el-icon></span>
          <div class="module-heading">
            <router-link :to="module.path!" class="module-title">{{ module.menuName }}</router-link>
            <span>{{ module.children.length }} 项可用功能</span>
          </div>
        </div>
        <div class="entries">
          <router-link v-for="entry in module.children" :key="entry.path" :to="entry.path!" class="entry-link">
            <span class="entry-marker" aria-hidden="true"></span>
            <span class="entry-name">{{ entry.menuName }}</span>
            <el-icon class="entry-arrow" aria-hidden="true"><ArrowRight /></el-icon>
          </router-link>
        </div>
      </article>
    </nav>
    <el-empty v-if="!navigation.loading && !navigation.error && !modules.length" description="没有匹配的功能入口" />
  </section>
</template>
<script setup lang="ts">
import { computed, ref, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowRight, ChatDotRound, ChatLineSquare, Cpu, DataAnalysis, House,
  Lock, Menu as MenuIcon, Money, Search, Setting, ShoppingCart, Tools
} from '@element-plus/icons-vue'
import { useNavigationStore } from '@/store/navigation'
const route = useRoute()
const router = useRouter()
const navigation = useNavigationStore()
const keyword = ref('')

type ModuleVisual = { icon: Component; tone: string }
const moduleVisuals: Record<string, ModuleVisual> = {
  '/system': { icon: Setting, tone: 'blue' },
  '/property': { icon: House, tone: 'green' },
  '/service': { icon: Tools, tone: 'orange' },
  '/charge': { icon: Money, tone: 'red' },
  '/life': { icon: ShoppingCart, tone: 'teal' },
  '/guard': { icon: Lock, tone: 'indigo' },
  '/social': { icon: ChatDotRound, tone: 'rose' },
  '/report': { icon: DataAnalysis, tone: 'violet' },
  '/ai': { icon: Cpu, tone: 'gold' }
}
const isOverview = computed(() => route.path === '/workspace')
const pageTitle = computed(() => isOverview.value ? '功能导航' : String(route.meta.title || '功能导航'))
const pageDescription = computed(() => isOverview.value
  ? '按业务模块快速定位功能，或直接搜索功能名称。'
  : `查看${pageTitle.value}下的全部可用功能。`)
const modules = computed(() => navigation.menus
  .filter(m => isOverview.value || m.path === route.path)
  .map(m => {
    const query = keyword.value.trim().toLocaleLowerCase()
    const children = (m.children || []).filter(c =>
      `${m.menuName || ''} ${c.menuName || ''}`.toLocaleLowerCase().includes(query))
    return { ...m, children, visual: moduleVisuals[m.path || ''] || { icon: ChatLineSquare, tone: 'blue' } }
  })
  .filter(m => m.children.length))
const entryCount = computed(() => modules.value.reduce((count, module) => count + module.children.length, 0))
</script>
<style scoped>
.workspace-page { width: 100%; max-width: 1440px; margin: 0 auto; }
.page-heading { display: flex; align-items: center; justify-content: space-between; gap: 24px; margin-bottom: 20px; }
.heading-main { display: flex; align-items: center; min-width: 0; gap: 14px; }
.heading-icon { display: grid; flex: 0 0 44px; width: 44px; height: 44px; place-items: center; color: #1f5f99; background: #eaf2fb; border: 1px solid #d8e6f4; border-radius: 8px; font-size: 22px; }
.page-heading h2 { margin: 0 0 5px; color: #1f2937; font-size: 24px; line-height: 1.25; letter-spacing: 0; }
.page-heading p { margin: 0; color: #6b7280; font-size: 14px; line-height: 1.5; }
.summary { display: flex; flex: 0 0 auto; align-items: baseline; gap: 6px; color: #6b7280; font-size: 13px; }
.summary strong { color: #1f2937; font-size: 18px; }
.summary span { width: 1px; height: 14px; margin: 0 5px; background: #d1d5db; }
.toolbar { display: flex; align-items: center; gap: 14px; padding: 16px 0; margin-bottom: 8px; border-top: 1px solid #e5e7eb; }
.search { width: min(100%, 460px); }
.search-status { color: #6b7280; font-size: 13px; }
.modules { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; min-height: 120px; }
.module-card { position: relative; overflow: hidden; min-width: 0; padding: 20px; background: #fff; border: 1px solid #e2e8f0; border-radius: 7px; box-shadow: 0 2px 8px rgba(15, 23, 42, .04); transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease; --module-color: #2563a7; --module-soft: #eaf2fb; }
.module-card::before { position: absolute; inset: 0 auto 0 0; width: 3px; background: var(--module-color); content: ''; }
.module-card:hover { border-color: #cbd5e1; box-shadow: 0 8px 22px rgba(15, 23, 42, .08); transform: translateY(-2px); }
.module-card--green { --module-color: #2f855a; --module-soft: #e8f5ee; }
.module-card--orange { --module-color: #c05621; --module-soft: #fff1e8; }
.module-card--red { --module-color: #c2414f; --module-soft: #fdecef; }
.module-card--teal { --module-color: #167c80; --module-soft: #e6f5f4; }
.module-card--indigo { --module-color: #4f5fa8; --module-soft: #eceefa; }
.module-card--rose { --module-color: #b2456e; --module-soft: #faeaf0; }
.module-card--violet { --module-color: #76539b; --module-soft: #f1ebf7; }
.module-card--gold { --module-color: #9a6a13; --module-soft: #fbf2df; }
.module-header { display: flex; align-items: center; gap: 12px; padding-bottom: 16px; border-bottom: 1px solid #edf0f3; }
.module-icon { display: grid; flex: 0 0 40px; width: 40px; height: 40px; place-items: center; color: var(--module-color); background: var(--module-soft); border-radius: 7px; font-size: 20px; }
.module-heading { display: flex; min-width: 0; flex-direction: column; gap: 4px; }
.module-title { overflow: hidden; color: #1f2937; font-size: 16px; font-weight: 650; line-height: 1.35; text-decoration: none; text-overflow: ellipsis; white-space: nowrap; }
.module-title:hover { color: var(--module-color); }
.module-heading span { color: #8a94a3; font-size: 12px; }
.entries { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 7px; padding-top: 14px; }
.entry-link { display: flex; min-width: 0; min-height: 38px; align-items: center; gap: 8px; padding: 8px 9px; color: #4b5563; border-radius: 4px; font-size: 13px; line-height: 1.35; text-decoration: none; transition: color .16s ease, background-color .16s ease; }
.entry-link:hover, .entry-link:focus-visible { color: var(--module-color); background: var(--module-soft); outline: none; }
.entry-marker { flex: 0 0 5px; width: 5px; height: 5px; background: var(--module-color); border-radius: 50%; opacity: .72; }
.entry-name { overflow: hidden; min-width: 0; text-overflow: ellipsis; white-space: nowrap; }
.entry-arrow { flex: 0 0 auto; margin-left: auto; opacity: 0; transform: translateX(-3px); transition: opacity .16s ease, transform .16s ease; }
.entry-link:hover .entry-arrow, .entry-link:focus-visible .entry-arrow { opacity: 1; transform: translateX(0); }
@media (max-width: 720px) {
  .page-heading { align-items: flex-start; flex-direction: column; gap: 14px; }
  .toolbar { align-items: flex-start; flex-direction: column; }
  .search { width: 100%; }
  .modules { grid-template-columns: 1fr; }
}
@media (max-width: 420px) { .entries { grid-template-columns: 1fr; } }
</style>
