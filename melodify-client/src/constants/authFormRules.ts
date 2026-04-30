import type { FormRules } from 'element-plus'

/**
 * Element Plus 表单规则：与后端 DTO 校验保持一致，减少无效请求。
 * @see melodify-backend UserLoginDTO
 */
export const loginFormRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { max: 50, message: '用户名过长', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { max: 100, message: '密码过长', trigger: 'blur' },
  ],
}

/** @see melodify-backend UserRegisterDTO */
export const registerFormRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度须在 3～50 个字符之间', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度须在 6～100 个字符之间', trigger: 'blur' },
  ],
}
