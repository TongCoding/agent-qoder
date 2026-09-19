<script setup lang="ts">
/**
 * 用户创建 / 编辑弹窗。
 *
 * 与后端 `UserCreateRequest` / `UserUpdateRequest` 的约束保持一致。
 * 编辑模式下用户名只读展示：后端将其视为业务主键，创建后不允许修改。
 */
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { createUser, updateUser } from '@/api/user'
import { MOBILE_QUERY, useMediaQuery } from '@/composables/useMediaQuery'
import { ApiError } from '@/types/api'
import { USER_ROLE_OPTIONS, USER_STATUS_OPTIONS } from '@/utils/dict'

import type { UserCreatePayload, UserInfo, UserRole, UserStatus } from '@/types/user'

const props = defineProps<{
  /** 待编辑的用户，为 null 表示创建模式 */
  user?: UserInfo | null
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

/** 服务端返回的字段级错误（如用户名重复 40901、邮箱重复 40902） */
const serverErrors = reactive<Record<string, string>>({})

const dialogWidth = computed(() => (isMobile.value ? '92%' : '680px'))

const isEdit = computed(() => Boolean(props.user?.id))

const dialogTitle = computed(() => (isEdit.value ? '编辑用户' : '新建用户'))

/** 表单数据模型，username 仅创建模式提交 */
interface UserFormModel {
  username: string
  nickname: string
  email: string
  phone: string
  department: string
  role: UserRole
  status: UserStatus
  remark: string
}

function createEmptyForm(): UserFormModel {
  return {
    username: '',
    nickname: '',
    email: '',
    phone: '',
    department: '',
    role: 'MEMBER',
    status: 'ACTIVE',
    remark: '',
  }
}

const form = reactive<UserFormModel>(createEmptyForm())

/** 部门候选项，仅作输入辅助，允许自由填写 */
const DEPARTMENT_SUGGESTIONS = ['研发部', '产品部', '设计部', '测试部', '运维部', '市场部', '架构组']

/** el-autocomplete 的建议查询回调签名 */
type SuggestionCallback = (data: { value: string }[]) => void

/** 按关键字过滤部门候选项，关键字为空时返回全部 */
function queryDepartments(keyword: string, callback: SuggestionCallback): void {
  const matched = keyword
    ? DEPARTMENT_SUGGESTIONS.filter((item) => item.includes(keyword))
    : DEPARTMENT_SUGGESTIONS
  callback(matched.map((value) => ({ value })))
}

/**
 * 校验规则。
 *
 * 用户名规则对应后端 `@Size(min=4,max=50)` + `@Pattern("^[a-zA-Z0-9_]+$")`；
 * 手机号对应 `@Pattern("^$|^1[3-9]\\d{9}$")`。
 */
const rules = computed<FormRules<UserFormModel>>(() => ({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 50, message: '用户名长度必须在 4-50 个字符之间', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' },
  ],
  nickname: [{ max: 50, message: '昵称长度不能超过 50 个字符', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] },
    { max: 100, message: '邮箱长度不能超过 100 个字符', trigger: 'blur' },
  ],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  department: [{ max: 50, message: '部门名称长度不能超过 50 个字符', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择账号状态', trigger: 'change' }],
  remark: [{ max: 255, message: '备注长度不能超过 255 个字符', trigger: 'blur' }],
}))

function fillForm(user: UserInfo | null): void {
  Object.assign(form, createEmptyForm())
  if (!user) {
    return
  }
  form.username = user.username
  form.nickname = user.nickname ?? ''
  form.email = user.email
  form.phone = user.phone ?? ''
  form.department = user.department ?? ''
  form.role = user.role
  form.status = user.status
  form.remark = user.remark ?? ''
}

watch(visible, (opened) => {
  if (opened) {
    Object.keys(serverErrors).forEach((key) => delete serverErrors[key])
    fillForm(props.user ?? null)
    void formRef.value?.clearValidate()
  }
})

/** 空字符串统一转 null，与后端可选字段语义对齐 */
function blankToNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) {
    return
  }

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  Object.keys(serverErrors).forEach((key) => delete serverErrors[key])

  submitting.value = true
  try {
    if (isEdit.value && props.user) {
      await updateUser(props.user.id, {
        nickname: blankToNull(form.nickname),
        email: form.email.trim(),
        phone: blankToNull(form.phone),
        department: blankToNull(form.department),
        role: form.role,
        status: form.status,
        remark: blankToNull(form.remark),
      })
    } else {
      const payload: UserCreatePayload = {
        username: form.username.trim(),
        nickname: blankToNull(form.nickname),
        email: form.email.trim(),
        phone: blankToNull(form.phone),
        department: blankToNull(form.department),
        role: form.role,
        status: form.status,
        remark: blankToNull(form.remark),
      }
      await createUser(payload)
    }

    ElMessage.success(isEdit.value ? '用户已更新' : '用户已创建')
    visible.value = false
    emit('success')
  } catch (error) {
    if (error instanceof ApiError) {
      // 唯一性冲突（40901 用户名已存在 / 40902 邮箱已存在）会带字段明细
      error.details.forEach((item) => {
        serverErrors[item.field] = item.message
      })
      ElMessage.error(error.message)
    }
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
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" label-position="right">
      <el-form-item label="用户名" prop="username" :error="serverErrors.username">
        <el-input
          v-model="form.username"
          placeholder="4-50 位字母、数字或下划线"
          :disabled="isEdit"
          maxlength="50"
          clearable
        />
        <div v-if="isEdit" class="app-text-secondary">用户名创建后不可修改</div>
      </el-form-item>

      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="昵称" prop="nickname" :error="serverErrors.nickname">
            <el-input v-model="form.nickname" placeholder="展示名称" maxlength="50" clearable />
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12">
          <el-form-item label="邮箱" prop="email" :error="serverErrors.email">
            <el-input v-model="form.email" placeholder="name@example.com" maxlength="100" clearable />
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12">
          <el-form-item label="手机号" prop="phone" :error="serverErrors.phone">
            <el-input v-model="form.phone" placeholder="选填，11 位手机号" maxlength="11" clearable />
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12">
          <el-form-item label="部门" prop="department" :error="serverErrors.department">
            <el-autocomplete
              v-model="form.department"
              :fetch-suggestions="queryDepartments"
              placeholder="选填"
              clearable
              style="width: 100%"
            />
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12">
          <el-form-item label="角色" prop="role" :error="serverErrors.role">
            <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%">
              <el-option
                v-for="item in USER_ROLE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              >
                <el-tag :type="item.tagType" size="small" effect="light">{{ item.label }}</el-tag>
                <span v-if="item.hint" class="user-form__option-extra app-text-secondary">
                  {{ item.hint }}
                </span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12">
          <el-form-item label="账号状态" prop="status" :error="serverErrors.status">
            <el-radio-group v-model="form.status">
              <el-radio-button
                v-for="item in USER_STATUS_OPTIONS"
                :key="item.value"
                :value="item.value"
              >
                {{ item.label }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="备注" prop="remark" :error="serverErrors.remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="3"
          placeholder="选填，例如入职时间、职责说明"
          maxlength="255"
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
.user-form__option-extra {
  float: right;
  margin-left: 16px;
  font-size: 12px;
}
</style>
