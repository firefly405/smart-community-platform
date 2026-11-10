<template>
  <div class="pro-table">
    <div v-if="showSearch && availableSearchFields.length > 0" class="pro-table-search">
      <el-form :model="searchForm" inline>
        <el-form-item v-for="field in availableSearchFields" :key="field.prop" :label="field.label">
          <el-input
            v-if="field.type === 'input' || field.type === 'number'"
            v-model="searchForm[field.prop]"
            :placeholder="field.placeholder || '请输入'"
            clearable
            style="width: 180px"
          />
          <el-select
            v-else-if="field.type === 'select'"
            v-model="searchForm[field.prop]"
            :placeholder="field.placeholder || '请选择'"
            :filterable="field.filterable"
            clearable
            style="width: 180px"
          >
            <el-option
              v-for="opt in field.options"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-date-picker
            v-else-if="field.type === 'date'"
            v-model="searchForm[field.prop]"
            type="date"
            :placeholder="field.placeholder || '选择日期'"
            value-format="YYYY-MM-DD"
            style="width: 180px"
          />
          <el-date-picker
            v-else-if="field.type === 'daterange'"
            v-model="searchForm[field.prop]"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="showToolbar" class="pro-table-toolbar">
      <slot name="toolbar">
        <el-button v-if="showAdd && handlers.onAdd" type="primary" @click="$emit('add')">
          <el-icon><Plus /></el-icon>新增
        </el-button>
        <el-button v-if="showBatchDelete" type="danger" :disabled="selectedRows.length === 0" @click="$emit('batch-delete', selectedRows)">
          <el-icon><Delete /></el-icon>批量删除
        </el-button>
        <el-button v-if="showExport" @click="$emit('export', searchForm)">
          <el-icon><Download /></el-icon>导出
        </el-button>
      </slot>
    </div>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="data"
      :border="border"
      :stripe="stripe"
      :height="height"
      :row-key="rowKey"
      @selection-change="handleSelectionChange"
      @sort-change="handleSortChange"
    >
      <el-table-column v-if="showSelection" type="selection" width="50" align="center" />
      <el-table-column v-if="showIndex" type="index" width="60" label="#" align="center" />

      <template v-for="col in columns" :key="col.prop">
        <el-table-column
          v-if="!col.hidden"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          :align="col.align || 'left'"
          :sortable="col.sortable"
          :fixed="col.fixed"
          :show-overflow-tooltip="col.showOverflow !== false"
          :formatter="col.formatter"
        >
          <template v-if="col.slot" #default="{ row, $index }">
            <slot :name="col.slot" :row="row" :index="$index" />
          </template>
        </el-table-column>
      </template>

      <el-table-column v-if="$slots.actions || (showEdit && handlers.onEdit) || (showDelete && handlers.onDelete)" label="操作" :width="actionsWidth" align="center" fixed="right">
        <template #default="{ row, $index }">
          <el-button v-if="showEdit && handlers.onEdit" type="primary" link size="small" @click="$emit('edit', row)">编辑</el-button>
          <slot name="actions" :row="row" :index="$index" />
          <el-popconfirm v-if="showDelete && handlers.onDelete" title="确定删除？" @confirm="$emit('delete', row)">
            <template #reference>
              <el-button type="danger" link size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
      <el-table-column v-if="relations[route.path]" label="关联" width="120" fixed="right">
        <template #default="{ row }"><BusinessLinks :row="row" /></template>
      </el-table-column>
    </el-table>

    <div v-if="showPagination" class="pro-table-pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="currentPageSize"
        :page-sizes="pageSizes"
        :total="total"
        :layout="paginationLayout"
        background
        @size-change="handlePageSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed, getCurrentInstance } from 'vue'
import { useRoute } from 'vue-router'
import BusinessLinks from '@/components/BusinessLinks.vue'
import { relations, associationQuery } from '@/navigation/business'

interface SearchField {
  prop: string
  label: string
  type?: 'input' | 'number' | 'select' | 'date' | 'daterange'
  placeholder?: string
  options?: { label: string; value: any }[]
  filterable?: boolean
}

interface Column {
  prop: string
  label: string
  width?: string | number
  minWidth?: string | number
  align?: string
  sortable?: boolean | string
  fixed?: boolean | string
  hidden?: boolean
  slot?: string
  showOverflow?: boolean
  formatter?: (row: any, column: any, cellValue: any, index: number) => any
}

const props = withDefaults(defineProps<{
  data: any[]
  columns: Column[]
  total?: number
  loading?: boolean
  border?: boolean
  stripe?: boolean
  height?: string | number
  rowKey?: string
  showIndex?: boolean
  showSelection?: boolean
  showSearch?: boolean
  showToolbar?: boolean
  showAdd?: boolean
  showEdit?: boolean
  showDelete?: boolean
  showBatchDelete?: boolean
  showExport?: boolean
  showPagination?: boolean
  searchFields?: SearchField[]
  actionsWidth?: string | number
  paginationLayout?: string
  pageSizes?: number[]
  page?: number
  pageSize?: number
}>(), {
  total: 0,
  loading: false,
  border: true,
  stripe: true,
  rowKey: 'id',
  showIndex: true,
  showSelection: false,
  showSearch: true,
  showToolbar: true,
  showAdd: true,
  showEdit: true,
  showDelete: true,
  showBatchDelete: false,
  showExport: false,
  showPagination: true,
  searchFields: () => [],
  actionsWidth: 180,
  paginationLayout: 'total, sizes, prev, pager, next, jumper',
  pageSizes: () => [10, 20, 50, 100],
  page: 1,
  pageSize: 10
})

const emit = defineEmits<{
  add: []
  edit: [row: any]
  delete: [row: any]
  'batch-delete': [rows: any[]]
  export: [searchForm: any]
  search: [params: { page: number; pageSize: number }]
  'update:page': [page: number]
  'update:pageSize': [pageSize: number]
  'selection-change': [rows: any[]]
}>()

const route = useRoute()
const handlers = getCurrentInstance()!.vnode.props || {}
const availableSearchFields = computed(() => props.searchFields.filter(f => !(f.prop in associationQuery(route.path, route.query))))
const tableRef = ref()
const selectedRows = ref<any[]>([])
const currentPage = ref(props.page)
const currentPageSize = ref(props.pageSize)
const searchForm = reactive<Record<string, any>>({})

// sync props
watch(() => props.page, (v) => { currentPage.value = v })
watch(() => props.pageSize, (v) => { currentPageSize.value = v })

function handleSearch() {
  currentPage.value = 1
  emitSearch()
}

function handleReset() {
  Object.keys(searchForm).forEach(k => { searchForm[k] = undefined })
  currentPage.value = 1
  emitSearch()
}

function handlePageChange(page: number) {
  currentPage.value = page
  emit('update:page', page)
  emitSearch()
}

function handlePageSizeChange(size: number) {
  currentPageSize.value = size
  currentPage.value = 1
  emit('update:pageSize', size)
  emitSearch()
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
  emit('selection-change', rows)
}

function handleSortChange(_sort?: any) {
  // trigger re-search with sort params
}

function emitSearch() {
  emit('search', {
    page: currentPage.value,
    pageSize: currentPageSize.value,
    ...searchForm
  })
}

defineExpose({ searchForm, tableRef })
</script>

<style lang="scss" scoped>
.pro-table {
  &-search {
    padding: 14px 16px 0;
    background: #f8fafb;
    border: 1px solid #e2e8ef;
    border-radius: 6px;
    margin-bottom: 16px;
  }

  &-toolbar {
    min-height: 32px;
    margin-bottom: 12px;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }

  &-pagination {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}

:deep(.el-table) { overflow: hidden; border-radius: 6px; }
:deep(.el-table th.el-table__cell) { color: #344054; background: #f6f8fa; font-weight: 600; }
:deep(.el-table .cell) { line-height: 1.45; }

@media (max-width: 720px) {
  .pro-table-search :deep(.el-form) { display: grid; grid-template-columns: 1fr; }
  .pro-table-search :deep(.el-form-item) { margin-right: 0; margin-bottom: 14px; }
  .pro-table-search :deep(.el-form-item__content),
  .pro-table-search :deep(.el-input),
  .pro-table-search :deep(.el-select),
  .pro-table-search :deep(.el-date-editor) { width: 100% !important; }
  .pro-table-pagination { overflow-x: auto; justify-content: flex-start; padding-bottom: 2px; }
  :deep(.el-table__header-wrapper tr th.el-table-fixed-column--right),
  :deep(.el-table__body-wrapper tr td.el-table-fixed-column--right) {
    position: relative !important;
    right: auto !important;
    z-index: var(--el-table-index) !important;
  }
  :deep(.el-table__fixed-right-patch) { display: none; }
}
</style>
