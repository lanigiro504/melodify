<script setup lang="ts">
import { Goods, EditPen } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import type { PointProduct } from '@/types/api'
import * as adminPointProductsApi from '@/api/adminPointProducts'
import { unwrapResult } from '@/utils/apiResult'
import { ADMIN_MAX_PAGE_SIZE, useAdminPaging } from '@/composables/useAdminPaging'
import { showSubmitError } from '@/utils/showSubmitError'

defineOptions({ name: 'AdminPointProductsPage' })

const { loading, rows, total, pager, fetchPage } = useAdminPaging<PointProduct>()
const dlg = ref(false)
const saving = ref(false)
const editing = reactive<Partial<PointProduct>>({})

const filters = reactive({
  status: undefined as undefined | number,
})

const statusOptionsFilter = [
  { label: '上架', value: 1 },
  { label: '下架', value: 0 },
]

const loadList = () =>
  void fetchPage(
    () =>
      adminPointProductsApi.pagePointProductsAdmin(pager.current, pager.size, {
        ...(typeof filters.status === 'number' ? { status: filters.status } : {}),
      }),
    '加载积分商品失败',
  )

const onSearch = () => {
  pager.current = 1
  loadList()
}

const openEdit = (row: PointProduct) => {
  Object.assign(editing, JSON.parse(JSON.stringify(row)) as PointProduct)
  dlg.value = true
}

const saveEdit = async () => {
  if (editing.id == null) return
  saving.value = true
  try {
    unwrapResult(await adminPointProductsApi.updatePointProductAdmin(editing.id, { ...editing }))
    ElMessage.success('已保存')
    dlg.value = false
    await loadList()
  } catch (e) {
    showSubmitError(e, '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => loadList())
</script>

<template>
  <div class="page card">
    <div class="toolbar">
      <el-select v-model="filters.status" placeholder="上架状态（可选）" clearable style="width: 160px">
        <el-option v-for="o in statusOptionsFilter" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-button type="primary" :icon="Goods" @click="onSearch">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border stripe row-key="id" size="small">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="productCode" label="编码" width="120" />
      <el-table-column prop="productName" label="名称" min-width="140" />
      <el-table-column prop="points" label="积分" width="80" />
      <el-table-column prop="priceCent" label="价格(分)" width="88" />
      <el-table-column prop="sortOrder" label="排序" width="72" />
      <el-table-column label="状态" width="88">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="96" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :icon="EditPen" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="pager"
      background
      layout="total, sizes, prev, pager, next"
      :total="total"
      :page-size="pager.size"
      :current-page="pager.current"
      :page-sizes="[10, 20, 50, ADMIN_MAX_PAGE_SIZE]"
      @update:page-size="
        (s: number) => {
          pager.size = s
          pager.current = 1
          loadList()
        }
      "
      @current-change="
        (p: number) => {
          pager.current = p
          loadList()
        }
      "
    />

    <el-dialog v-model="dlg" title="编辑积分商品" width="480px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="编码">
          <el-input v-model="editing.productCode" disabled />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="editing.productName" />
        </el-form-item>
        <el-form-item label="积分数量">
          <el-input-number v-model="editing.points as number" :min="0" :step="10" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="价格(分)">
          <el-input-number v-model="editing.priceCent as number" :min="0" :step="100" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editing.sortOrder as number" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="上架">
          <el-radio-group v-model="editing.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  padding: 1rem 1.25rem 1.5rem;
}
.card {
  background: #fff;
  border-radius: 8px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 1rem;
  align-items: center;
}
.pager {
  margin-top: 1rem;
  justify-content: flex-end;
}
</style>
