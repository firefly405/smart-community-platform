<template>
  <div class="page-container">
    <ProTable
      :data="tableData"
      :columns="columns"
      :total="total"
      :loading="loading"
      :page="page"
      :page-size="pageSize"
      @search="handleSearch"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
      @update:page="(p: number) => page = p"
      @update:page-size="(s: number) => pageSize = s"
    >
      <template #status="{ row }">
        <el-tag :type="row.isOnSale === 1 ? 'success' : 'info'" size="small">
          {{ row.isOnSale === 1 ? '上架' : '下架' }}
        </el-tag>
      </template>
    </ProTable>

    <ProDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :fields="formFields"
      :edit-data="editData"
      :loading="submitting"
      @confirm="handleSubmit"
      @close="editData = null"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ProTable from '@/components/ProTable.vue'
import ProDialog from '@/components/ProDialog.vue'
import type { FormField } from '@/components/ProForm.vue'
import { getProductList, createProduct, updateProduct, deleteProduct } from '@/api/life'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('新增商品')
const editData = ref<any>(null)

const categoryOptions = [
  { label: '生鲜蔬果', value: 1 },
  { label: '粮油调味', value: 2 },
  { label: '日用品', value: 3 },
  { label: '零食饮料', value: 4 },
  { label: '家居厨具', value: 5 },
  { label: '儿童用品', value: 6 }
]

function categoryName(row: any) {
  return categoryOptions.find(item => item.value === row.categoryId)?.label || '-'
}

const columns = [
  { prop: 'productName', label: '商品名称', width: 160 },
  { prop: 'categoryId', label: '分类', width: 100, formatter: categoryName },
  { prop: 'price', label: '售价', width: 100 },
  { prop: 'originPrice', label: '原价', width: 100 },
  { prop: 'stock', label: '库存', width: 80 },
  { prop: 'sales', label: '销量', width: 80 },
  { prop: 'isOnSale', label: '状态', width: 80, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const formFields: FormField[] = [
  { prop: 'productName', label: '商品名称', required: true },
  { prop: 'categoryId', label: '分类', type: 'select', options: categoryOptions, required: true },
  { prop: 'price', label: '售价', type: 'number', required: true },
  { prop: 'originPrice', label: '原价', type: 'number' },
  { prop: 'stock', label: '库存', type: 'number', required: true },
  { prop: 'productImage', label: '图片URL', type: 'textarea' },
  { prop: 'description', label: '描述', type: 'textarea' },
  { prop: 'isOnSale', label: '状态', type: 'switch', activeValue: 1, inactiveValue: 0 }
]

async function loadData(searchParams?: any) {
  loading.value = true
  try {
    const res = await getProductList({ page: page.value, pageSize: pageSize.value, ...searchParams })
    tableData.value = res.records
    total.value = res.total
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}

function handleSearch(params: any) {
  page.value = params.page
  pageSize.value = params.pageSize
  loadData(params)
}

function handleAdd() {
  editData.value = null
  dialogTitle.value = '新增商品'
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editData.value = { ...row }
  dialogTitle.value = '编辑商品'
  dialogVisible.value = true
}

async function handleSubmit(data: any) {
  submitting.value = true
  try {
    if (data.id) {
      await updateProduct(data)
      ElMessage.success('修改成功')
    } else {
      await createProduct(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    editData.value = null
    loadData()
  } catch { /* handled by interceptor */ }
  finally { submitting.value = false }
}

async function handleDelete(row: any) {
  await deleteProduct(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>
