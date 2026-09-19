<script setup lang="ts">
/**
 * 待办创建 / 编辑弹窗。
 *
 * 同一个组件承担两种模式：传入 `todo` 为编辑，为空则为创建。
 * 前端校验规则与后端 `TodoCreateRequest` / `TodoUpdateRequest` 上的注解一一对应，
 * 但**不依赖前端校验通过就万事大吉**——服务端的字段级错误同样会被回填到对应表单项上。
 */
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { createTodo, updateTodo } from '@/api/todo'
import { useUserOptions } from '@/composables/useUserOptions'
import { MOBILE_QUERY, useMediaQuery } from '@/composables/useMediaQuery'
import { ApiError } from '@/types/api'
import { TODO_PRIORITY_OPTIONS, TODO_STATUS_OPTIONS } from '@/utils/dict'
import { today } from '@/utils/format'

import type { TodoItem, TodoPriority, TodoStatus, TodoUpdatePayload } from '@/types/todo'

const props = defineProps<{
  /** 待编辑的待办，为 null 表示创建模式 */
  todo?: TodoItem | null
}>()

const emit = defineEmits<{
  /** 保存成功，父组件据此刷新列表 */
  success: []
}>()

/** 弹窗显隐，使用 v-model 双向绑定 */
const visible = defineModel<boolean>({ required: true })

const formRef = ref<FormInstance>()
const submitting = ref(false)
const isMobile = useMediaQuery(MOBILE_QUERY)

/** 服务端返回的字段级错误，绑定到 el-form-item 的 error 属性上 */
const serverErrors = reactive<Record<string, string>>({})

const { options: userOptions, loading: userOptionsLoading } = useUserOptions()

/** 弹窗宽度：移动端接近全屏，桌面端固定宽度 */
const dialogWidth = computed(() => (isMobile.value ? '92%' : '640px'))

const isEdit = computed(() => Boolean(props.todo?.id))

const dialogTitle = computed(() => (isEdit.value ? '编辑待办' : '新建待办'))

/** 表单数据模型 */
interface TodoFormModel {
  title: string
  description: string
  status: TodoStatus
  priority: TodoPriority
  dueDate: string | null
  assigneeId: number | null
}

function createEmptyForm(): TodoFormModel {
  return {
    title: '',
    description: '',
    status: 'PENDING',
    priority: 'MEDIUM',
    // 默认给一个今天的截止日期，减少录入成本；用户可清空表示无期限
    dueDate: today(),
    assigneeId: null,
  }
}

const form = reactive<TodoFormModel>(createEmptyForm())

/**
 * 校验规则。
 *
 * 与后端约束保持同步：title 必填且 ≤120，description ≤1000。
 */
const rules: FormRules<TodoFormModel> = {
  title: [
    { required: true, message: '请输入待办标题', trigger: 'blur' },
    { max: 120, message: '标题长度不能超过 120 个字符', trigger: 'blur' },
  ],
  description: [{ max: 1000, message: '描述长度不能超过 1000 个字符', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
}

/** 把待办数据填入表单；null 时重置为空白表单 */
function fillForm(todo: TodoItem | null): void {
  Object.assign(form, createEmptyForm())
  if (!todo) {
    return
  }
  form.title = todo.title
  form.description = todo.description ?? ''
  form.status = todo.status
  form.priority = todo.priority
  form.dueDate = todo.dueDate ?? null
  form.assigneeId = todo.assignee?.id ?? null
}

// 每次打开弹窗都重新填充，避免残留上一次的编辑内容
watch(visible, (opened) => {
  if (opened) {
    Object.keys(serverErrors).forEach((key) => delete serverErrors[key])
    fillForm(props.todo ?? null)
    // 弹窗刚打开时 DOM 尚未挂载完成，需要在下一帧清除上一次的校验红字
    void formRef.value?.clearValidate()
  }
})

/** 组装提交载荷，空字符串统一转 null 以匹配后端的可选字段语义 */
function buildPayload(): TodoUpdatePayload {
  return {
    title: form.title.trim(),
    description: form.description.trim() || null,
    status: form.status,
    priority: form.priority,
    dueDate: form.dueDate || null,
    assigneeId: form.assigneeId,
  }
}

/** 把服务端返回的字段错误写入 serverErrors */
function applyServerErrors(error: ApiError): void {
  error.details.forEach((item) => {
    serverErrors[item.field] = item.message
  })
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) {
    return
  }

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  // 清除上一次的服务端错误，避免新旧提示叠加
  Object.keys(serverErrors).forEach((key) => delete serverErrors[key])

  submitting.value = true
  try {
    const payload = buildPayload()
    if (isEdit.value && props.todo) {
      await updateTodo(props.todo.id, payload)
    } else {
      await createTodo(payload)
    }

    ElMessage.success(isEdit.value ? '待办已更新' : '待办已创建')
    visible.value = false
    emit('success')
  } catch (error) {
    if (error instanceof ApiError) {
      applyServerErrors(error)
      ElMessage.error(error.details.length > 0 ? '提交失败，请检查表单标注项' : error.message)
    }
    // 非 ApiError 的异常已由 Axios 拦截器提示，此处不再重复弹窗
  } finally {
    submitting.value = false
  }
}

function handleClose(): void {
  visible.value = false
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    :width="dialogWidth"
    :close-on-click-modal="false"
    append-to-body
    destroy-on-close
    class="todo-form-dialog"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" label-position="right">
      <el-form-item label="标题" prop="title" :error="serverErrors.title">
        <el-input
          v-model="form.title"
          placeholder="请输入待办标题"
          maxlength="120"
          show-word-limit
          clearable
          autofocus
        />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="状态" prop="status" :error="serverErrors.status">
            <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
              <el-option
                v-for="item in TODO_STATUS_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12">
          <el-form-item label="优先级" prop="priority" :error="serverErrors.priority">
            <el-select v-model="form.priority" placeholder="请选择优先级" style="width: 100%">
              <el-option
                v-for="item in TODO_PRIORITY_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12">
          <el-form-item label="截止日期" prop="dueDate" :error="serverErrors.dueDate">
            <el-date-picker
              v-model="form.dueDate"
              type="date"
              placeholder="选择截止日期"
              value-format="YYYY-MM-DD"
              format="YYYY-MM-DD"
              clearable
              style="width: 100%"
            />
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12">
          <el-form-item label="负责人" prop="assigneeId" :error="serverErrors.assigneeId">
            <el-select
              v-model="form.assigneeId"
              placeholder="暂不分配"
              :loading="userOptionsLoading"
              filterable
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="user in userOptions"
                :key="user.id"
                :label="user.nickname || user.username"
                :value="user.id"
              >
                <span>{{ user.nickname || user.username }}</span>
                <span class="todo-form-dialog__option-extra app-text-secondary">
                  {{ user.department || user.username }}
                </span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="描述" prop="description" :error="serverErrors.description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="补充任务背景、验收标准等信息"
          maxlength="1000"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-space :size="12">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '确认创建' }}
        </el-button>
      </el-space>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.todo-form-dialog__option-extra {
  float: right;
  margin-left: 16px;
  font-size: 12px;
}
</style>

<style lang="scss">
// 弹窗通过 append-to-body 挂到 body 下，scoped 样式无法命中，故用全局样式微调
.todo-form-dialog {
  .el-dialog__body {
    padding-top: 10px;
  }
}
</style>
