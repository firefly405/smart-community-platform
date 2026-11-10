<template>
  <section class="property-heading">
    <div class="heading-row">
      <div class="heading-copy">
        <span class="heading-icon" aria-hidden="true">
          <el-icon><component :is="current.icon" /></el-icon>
        </span>
        <div>
          <span class="eyebrow">房产物业</span>
          <h1>{{ current.label }}</h1>
          <p>{{ current.summary }}</p>
        </div>
      </div>
      <div v-if="$slots.meta" class="heading-meta"><slot name="meta" /></div>
    </div>

    <nav class="section-nav" aria-label="房产物业功能">
      <router-link
        v-for="item in pages"
        :key="item.path"
        :to="item.path"
        :class="{ active: route.path === item.path }"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </router-link>
    </nav>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Avatar, Brush, Grid, House, OfficeBuilding, Place, Suitcase, UserFilled, Van
} from '@element-plus/icons-vue'

const route = useRoute()
const pages = [
  { path: '/property/building', label: '楼栋管理', summary: '维护小区楼栋与楼层基础档案', icon: OfficeBuilding },
  { path: '/property/unit', label: '单元管理', summary: '按楼栋维护单元和楼层结构', icon: Grid },
  { path: '/property/house', label: '房屋管理', summary: '管理房屋档案、产权和居住状态', icon: House },
  { path: '/property/owner', label: '业主管理', summary: '维护业主资料并关联名下房屋', icon: UserFilled },
  { path: '/property/family', label: '家属管理', summary: '按业主查看和维护家庭成员', icon: Avatar },
  { path: '/property/tenant', label: '租户管理', summary: '跟踪租户、租期和租金信息', icon: Suitcase },
  { path: '/property/parking', label: '车位管理', summary: '维护车位资源、状态和价格', icon: Place },
  { path: '/property/vehicle', label: '车辆管理', summary: '管理业主车辆与车位绑定', icon: Van },
  { path: '/property/decoration', label: '装修管理', summary: '跟踪装修审批、押金和验收进度', icon: Brush }
]
const current = computed(() => pages.find(item => item.path === route.path) || pages[0])
</script>

<style scoped>
.property-heading { margin-bottom: 18px; }
.heading-row { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 16px; }
.heading-copy { display: flex; align-items: center; min-width: 0; gap: 13px; }
.heading-icon { display: grid; flex: 0 0 42px; width: 42px; height: 42px; place-items: center; color: #14685b; background: #e9f5f2; border: 1px solid #cfe8e1; border-radius: 7px; font-size: 21px; }
.eyebrow { display: block; margin-bottom: 2px; color: #7b8794; font-size: 12px; }
h1 { margin: 0; color: #1f2937; font-size: 22px; line-height: 1.3; letter-spacing: 0; }
p { margin: 3px 0 0; color: #667085; font-size: 13px; }
.heading-meta { display: flex; align-items: center; gap: 14px; color: #667085; font-size: 13px; }
.heading-meta :deep(strong) { color: #1f2937; font-size: 18px; }
.section-nav { display: flex; overflow-x: auto; border-bottom: 1px solid #dfe4ea; scrollbar-width: none; }
.section-nav::-webkit-scrollbar { display: none; }
.section-nav a { display: inline-flex; flex: 0 0 auto; align-items: center; gap: 6px; padding: 10px 13px; color: #667085; border-bottom: 2px solid transparent; font-size: 13px; text-decoration: none; }
.section-nav a:hover { color: #14685b; background: #f1f7f5; }
.section-nav a.active { color: #14685b; border-bottom-color: #19816f; font-weight: 600; }

:global(.property-page .pro-table-search) { padding: 14px 16px 0; border: 1px solid #e2e8ef; border-radius: 6px; background: #f8fafb; }
:global(.property-page .pro-table-search .el-form-item) { margin-bottom: 14px; }
:global(.property-page .pro-table-toolbar) { align-items: center; min-height: 32px; margin-bottom: 12px; }
:global(.property-page .el-table) { overflow: hidden; border-radius: 6px; }
:global(.property-page .el-table th.el-table__cell) { color: #344054; background: #f6f8fa; font-weight: 600; }
:global(.property-page .pro-table-pagination) { padding-top: 2px; }
:global(.property-page .el-dialog) { max-width: calc(100vw - 24px); border-radius: 7px; }

@media (max-width: 720px) {
  .heading-row { align-items: flex-start; flex-direction: column; gap: 10px; }
  .heading-meta { padding-left: 55px; }
  .section-nav a { padding: 9px 11px; }
  :global(.property-page .pro-table-search .el-form) { display: grid; grid-template-columns: 1fr; }
  :global(.property-page .pro-table-search .el-form-item) { margin-right: 0; }
  :global(.property-page .pro-table-search .el-form-item__content),
  :global(.property-page .pro-table-search .el-input),
  :global(.property-page .pro-table-search .el-select) { width: 100% !important; }
  :global(.property-page .el-table__header-wrapper tr th.el-table-fixed-column--right),
  :global(.property-page .el-table__body-wrapper tr td.el-table-fixed-column--right) {
    position: relative !important;
    right: auto !important;
    z-index: var(--el-table-index) !important;
  }
  :global(.property-page .el-table__fixed-right-patch) { display: none; }
  :global(.property-page .pro-table-pagination) { overflow-x: auto; justify-content: flex-start; }
}
</style>
