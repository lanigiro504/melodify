<script setup lang="ts">
/**
 * sys_role：分页、新建、编辑、删除。
 */
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import * as adminRolesApi from '@/api/adminRoles'
import type { SysRole } from '@/types/api'
import { unwrapResult } from '@/utils/apiResult'
import { showSubmitError } from '@/utils/showSubmitError'

defineOptions({ name: 'AdminRolesPage' })

const STATUS_ON = 1
const STATUS_OFF = 0

const loading = ref(false)
const rows = ref<SysRole[]>([])
const total = ref(0)
const pager = reactive({ current: 1, size: 10 })

const dlgVisible = ref(false)
const dlgSaving = ref(false)
const isEdit = ref(false)
const form = reactive<Partial<SysRole>>({
  id: undefined,
  roleName: '',
  roleKey: '',
  status: STATUS_ON,
  remark: '',
})

const dlgTitle = computed(() => (isEdit.value ? '编辑角色' : '新建角色'))

const fetchList = async () => {
  loading.value = true
  try {
    const pg = unwrapResult(await adminRolesApi.pageRoles(pager.current, pager.size))
    rows.value = pg.records ?? []
    total.value = pg.total
  } catch (e) {
    showSubmitError(e, '加载角色列表失败')
  } finally {
    loading.value = false
  }
}

const openCreate = (): void => {
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    roleName: '',
    roleKey: '',
    status: STATUS_ON,
    remark: '',
  })
  dlgVisible.value = true
}

const openEdit = (row: SysRole): void => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    roleName: row.roleName,
    roleKey: row.roleKey,
    status: row.status ?? STATUS_ON,
    remark: row.remark ?? '',
  })
  dlgVisible.value = true
}

const submitDialog = async (): Promise<void> => {
  const roleName = (form.roleName ?? '').trim()
  const roleKey = (form.roleKey ?? '').trim().toLowerCase()
  if (!roleName || !roleKey) {
    ElMessage.warning('请填写角色名称与角色键')
    return
  }
  dlgSaving.value = true
  try {
    if (isEdit.value && form.id != null) {
      unwrapResult(
        await adminRolesApi.updateRole(form.id, {
          roleName,
          roleKey,
          status: form.status,
          remark: form.remark?.trim() || '',
        }),
      )
      ElMessage.success('已保存')
    } else {
      unwrapResult(
        await adminRolesApi.createRole({
          roleName,
          roleKey,
          status: form.status,
          remark: form.remark?.trim() || '',
        }),
      )
      ElMessage.success('已创建')
    }
    dlgVisible.value = false
    await fetchList()
  } catch (e) {
    showSubmitError(e, '保存失败')
  } finally {
    dlgSaving.value = false
  }
}

const removeRow = async (row: SysRole): Promise<void> => {
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.roleName}」？（逻辑删除）`, '确认', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    unwrapResult(await adminRolesApi.deleteRole(row.id))
    ElMessage.success('已删除')
    await fetchList()
  } catch (e) {
    showSubmitError(e, '删除失败')
  }
}

onMounted(() => void fetchList())
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="openCreate">新建角色</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border stripe row-key="id">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="roleName" label="名称" min-width="120" />
      <el-table-column prop="roleKey" label="角色键" min-width="120">
        <template #default="{ row }">
          <code class="rk">{{ row.roleKey }}</code>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="88">
        <template #default="{ row }">
          <el-tag :type="row.status === STATUS_ON ? 'success' : 'info'">
            {{ row.status === STATUS_ON ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link :icon="Delete" @click="removeRow(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="pager"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pager.size"
      :current-page="pager.current"
      @current-change="
        (p: number) => {
          pager.current = p
          void fetchList()
        }
      "
    />

    <el-dialog v-model="dlgVisible" :title="dlgTitle" width="480px" destroy-on-close>
      <el-form label-width="88px">
        <el-form-item label="名称" required>
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="角色键" required>
          <el-input v-model="form.roleKey" :disabled="isEdit" placeholder="如 admin、common，新建后不建议改键" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="STATUS_ON">启用</el-radio>
            <el-radio :value="STATUS_OFF">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgVisible = false">取消</el-button>
        <el-button type="primary" :loading="dlgSaving" @click="submitDialog">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  background: #fff;
  padding: 1rem 1.25rem 1.5rem;
  border-radius: 8px;
}
.toolbar {
  margin-bottom: 1rem;
}
.pager {
  margin-top: 1rem;
  justify-content: flex-end;
}
.rk {
  font-size: 12px;
  padding: 2px 6px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}
</style>
