import type { FormInstance } from 'element-plus'
import type { Ref } from 'vue'

/**
 * 触发 Element Plus 表单校验；未通过或 ref 未挂载时返回 false。
 */
export const validateFormRef = async (
  formRef: Ref<FormInstance | undefined>,
): Promise<boolean> => {
  const form = formRef.value
  if (!form) return false
  try {
    await form.validate()
    return true
  } catch {
    return false
  }
}
