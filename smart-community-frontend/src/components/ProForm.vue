<template>
  <el-form
    ref="formRef"
    :model="model"
    :rules="rules"
    :label-width="labelWidth"
    :label-position="labelPosition"
    :size="size"
    :disabled="disabled"
    @submit.prevent
  >
    <el-row :gutter="20">
      <template v-for="field in fields" :key="field.prop">
        <el-col v-if="!field.hidden" :span="field.span || colSpan">
          <el-form-item :label="field.label" :prop="field.prop">
            <!-- Input -->
            <el-input
              v-if="field.type === 'input' || !field.type"
              v-model="model[field.prop]"
              :type="field.inputType || 'text'"
              :placeholder="field.placeholder || '请输入' + field.label"
              :disabled="field.disabled"
              :maxlength="field.maxlength"
              :show-word-limit="field.showWordLimit"
              clearable
            />

            <!-- Textarea -->
            <el-input
              v-else-if="field.type === 'textarea'"
              v-model="model[field.prop]"
              type="textarea"
              :placeholder="field.placeholder || '请输入' + field.label"
              :rows="field.rows || 3"
              :maxlength="field.maxlength"
              :show-word-limit="field.showWordLimit"
              clearable
            />

            <!-- Select -->
            <el-select
              v-else-if="field.type === 'select'"
              v-model="model[field.prop]"
              :placeholder="field.placeholder || '请选择' + field.label"
              :disabled="field.disabled"
              :multiple="field.multiple"
              :filterable="field.filterable"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="opt in field.options"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
                :disabled="opt.disabled"
              />
            </el-select>

            <!-- Radio -->
            <el-radio-group
              v-else-if="field.type === 'radio'"
              v-model="model[field.prop]"
              :disabled="field.disabled"
            >
              <el-radio v-for="opt in field.options" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </el-radio>
            </el-radio-group>

            <!-- Checkbox -->
            <el-checkbox-group
              v-else-if="field.type === 'checkbox'"
              v-model="model[field.prop]"
              :disabled="field.disabled"
            >
              <el-checkbox v-for="opt in field.options" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </el-checkbox>
            </el-checkbox-group>

            <!-- Switch -->
            <el-switch
              v-else-if="field.type === 'switch'"
              v-model="model[field.prop]"
              :disabled="field.disabled"
              :active-value="field.activeValue ?? 1"
              :inactive-value="field.inactiveValue ?? 0"
            />

            <!-- Date -->
            <el-date-picker
              v-else-if="field.type === 'date'"
              v-model="model[field.prop]"
              type="date"
              :placeholder="field.placeholder || '选择日期'"
              value-format="YYYY-MM-DD"
              :disabled="field.disabled"
              style="width: 100%"
            />

            <!-- DateTime -->
            <el-date-picker
              v-else-if="field.type === 'datetime'"
              v-model="model[field.prop]"
              type="datetime"
              :placeholder="field.placeholder || '选择时间'"
              value-format="YYYY-MM-DD HH:mm:ss"
              :disabled="field.disabled"
              style="width: 100%"
            />

            <!-- Number -->
            <el-input-number
              v-else-if="field.type === 'number'"
              v-model="model[field.prop]"
              :min="field.min"
              :max="field.max"
              :step="field.step ?? 1"
              :precision="field.precision"
              :disabled="field.disabled"
              style="width: 100%"
            />

            <!-- Upload -->
            <el-upload
              v-else-if="field.type === 'upload'"
              :action="field.action || '#'"
              :auto-upload="false"
              list-type="picture-card"
              :file-list="model[field.prop]"
              :disabled="field.disabled"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>

            <!-- TreeSelect -->
            <el-tree-select
              v-else-if="field.type === 'tree'"
              v-model="model[field.prop]"
              :data="field.options"
              :props="field.treeProps || { label: 'name', children: 'children' }"
              :placeholder="field.placeholder || '请选择' + field.label"
              :multiple="field.multiple"
              :filterable="field.filterable"
              check-strictly
              clearable
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </template>
    </el-row>

    <div v-if="showActions" class="pro-form-actions">
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">{{ submitText }}</el-button>
      <el-button @click="handleReset">{{ cancelText }}</el-button>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'

export interface FormField {
  prop: string
  label: string
  type?: 'input' | 'textarea' | 'select' | 'radio' | 'checkbox' | 'switch' | 'date' | 'datetime' | 'number' | 'upload' | 'tree'
  placeholder?: string
  disabled?: boolean
  span?: number
  required?: boolean
  inputType?: string
  rows?: number
  maxlength?: number
  showWordLimit?: boolean
  options?: any[]
  multiple?: boolean
  filterable?: boolean
  min?: number
  max?: number
  step?: number
  precision?: number
  action?: string
  treeProps?: any
  activeValue?: any
  inactiveValue?: any
  hidden?: boolean
  rules?: any[]
}

const props = withDefaults(defineProps<{
  model: Record<string, any>
  fields: FormField[]
  rules?: Record<string, any>
  labelWidth?: string
  labelPosition?: 'right' | 'left' | 'top'
  size?: '' | 'small' | 'default' | 'large'
  disabled?: boolean
  colSpan?: number
  showActions?: boolean
  submitText?: string
  cancelText?: string
  submitLoading?: boolean
}>(), {
  labelWidth: '100px',
  labelPosition: 'right',
  colSpan: 12,
  showActions: true,
  submitText: '确定',
  cancelText: '取消',
  submitLoading: false
})

const emit = defineEmits<{
  submit: []
  reset: []
}>()

const formRef = ref()

// Build rules from fields
const rules = reactive<Record<string, any>>({})
props.fields.forEach(field => {
  if (field.required && !field.disabled) {
    rules[field.prop] = [
      { required: true, message: '请输入' + field.label, trigger: 'blur' }
    ]
  } else if (field.rules) {
    rules[field.prop] = field.rules
  }
})

function handleSubmit() {
  formRef.value?.validate((valid: boolean) => {
    if (valid) {
      emit('submit')
    }
  })
}

function handleReset() {
  formRef.value?.resetFields()
  emit('reset')
}

async function validate() {
  return formRef.value?.validate()
}

function resetFields() {
  formRef.value?.resetFields()
}

function clearValidate() {
  formRef.value?.clearValidate()
}

defineExpose({ validate, resetFields, clearValidate, formRef })
</script>

<style lang="scss" scoped>
.pro-form-actions {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
  margin-top: 10px;
}
</style>
