<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :fullscreen="fullscreen"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :destroy-on-close="true"
    @close="handleClose"
  >
    <ProForm
      v-if="visible"
      ref="formRef"
      :model="formModel"
      :fields="effectiveFields"
      :label-width="labelWidth"
      :col-span="colSpan"
      :show-actions="false"
      :disabled="effectiveReadonly"
    />

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">{{ cancelText }}</el-button>
        <el-button v-if="!effectiveReadonly" type="primary" :loading="loading" @click="handleConfirm">
          {{ confirmText }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, reactive, computed } from 'vue'
import ProForm from './ProForm.vue'
import type { FormField } from './ProForm.vue'

const props = withDefaults(defineProps<{
  modelValue?: boolean
  visible?: boolean          // legacy alias for modelValue
  title?: string
  fields?: FormField[]
  formFields?: FormField[]   // legacy alias for fields
  labelWidth?: string
  colSpan?: number
  width?: string
  fullscreen?: boolean
  loading?: boolean
  readonly?: boolean
  showSubmit?: boolean
  confirmText?: string
  cancelText?: string
  editData?: Record<string, any> | null
  formData?: Record<string, any> | null  // legacy alias for editData
}>(), {
  modelValue: undefined,
  visible: undefined,
  fields: undefined,
  showSubmit: undefined,
  title: '弹窗',
  labelWidth: '100px',
  colSpan: 12,
  width: '600px',
  fullscreen: false,
  loading: false,
  readonly: false,
  confirmText: '确定',
  cancelText: '取消',
  editData: null
})

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  'update:visible': [visible: boolean]   // legacy
  confirm: [data: Record<string, any>]
  close: []
  cancel: []  // legacy alias for close
}>()

// Resolve legacy prop aliases
const effectiveModelValue = computed(() => props.modelValue ?? props.visible ?? false)
const effectiveFields = computed(() => props.fields ?? props.formFields ?? [])
const effectiveEditData = computed(() => props.editData ?? props.formData ?? null)
const effectiveReadonly = computed(() => props.readonly || props.showSubmit === false)

const visible = ref(false)
const formRef = ref()
const formModel = reactive<Record<string, any>>({})

watch(() => effectiveModelValue.value, (val) => {
  visible.value = val
  if (val) {
    initForm()
  }
}, { immediate: true })

watch(visible, (val) => {
  emit('update:modelValue', val)
  emit('update:visible', val)
})

function initForm() {
  // Clear all keys
  Object.keys(formModel).forEach(k => delete formModel[k])

  const editData = effectiveEditData.value
  if (editData) {
    Object.assign(formModel, editData)
  } else {
    effectiveFields.value.forEach(field => {
      if (formModel[field.prop] === undefined) {
        formModel[field.prop] = null
      }
    })
  }
}

async function handleConfirm() {
  try {
    await formRef.value?.validate()
    // Legacy callers submit their formData directly instead of consuming the event payload.
    if (props.formData) Object.assign(props.formData, formModel)
    emit('confirm', { ...formModel })
  } catch {}
}

function handleClose() {
  visible.value = false
  emit('close')
  emit('cancel')
}

defineExpose({ formModel, formRef })
</script>

<style lang="scss" scoped>
.dialog-footer {
  text-align: right;
}
</style>
