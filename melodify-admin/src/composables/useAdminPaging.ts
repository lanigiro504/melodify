import { reactive, ref, type Ref } from 'vue'
import type { PageRecords, Result } from '@/types/api'
import { unwrapResult } from '@/utils/apiResult'
import { showSubmitError } from '@/utils/showSubmitError'

/** 与管理端后端 {@link PagingNormalize#MAX_PAGE_SIZE} 对齐的单页最大条数 */
export const ADMIN_MAX_PAGE_SIZE = 100

/** 分页列表加载：减少各运营页重复的 loading/unwrapping/messages 样板代码 */
export function useAdminPaging<T>() {
  const loading = ref(false)
  const rows = ref<T[]>([]) as Ref<T[]>
  const total = ref(0)
  const pager = reactive({ current: 1, size: 10 })

  const fetchPage = async (request: () => Promise<Result<PageRecords<T>>>, errorLabel: string) => {
    loading.value = true
    try {
      const pg = unwrapResult(await request())
      rows.value = pg.records ?? []
      total.value = pg.total
    } catch (e) {
      showSubmitError(e, errorLabel)
    } finally {
      loading.value = false
    }
  }

  return { loading, rows, total, pager, fetchPage }
}
