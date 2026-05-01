<script setup lang="ts">
/**
 * sys_user：分页、新建、编辑、删除；新建/修改密码由后端 MD5（与前台登录一致）。
 */
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import * as adminRolesApi from '@/api/adminRoles'
import * as adminUsersApi from '@/api/adminUsers'
import type { SysRole, SysUser } from '@/types/api'
import { unwrapResult } from '@/utils/apiResult'
import { showSubmitError } from '@/utils/showSubmitError'

defineOptions({ name: 'AdminUsersPage' })

const USER_STATUS_NORMAL = 1
const USER_STATUS_BANNED = 0

const ROLE_PAGE_SIZE = 200

const loading = ref(false)
const rows = ref<SysUser[]>([])
const total = ref(0)
const pager = reactive({ current: 1, size: 10 })
const roleOptions = ref<SysRole[]>([])
const dlgVisible = ref(false)
const dlgSaving = ref(false)
const isEdit = ref(false)
const form = reactive<Partial<SysUser> & { password?: string }>({
  id: undefined,
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  points: 0,
  roleId: 2,
  status: USER_STATUS_NORMAL,
})

const dlgTitle = computed(() => (isEdit.value ? '编辑用户' : '新建用户'))

const roleNameById = (rid: number | null | undefined) => {
  if (rid == null) return '—'
  const r = roleOptions.value.find((x) => x.id === rid)
  return r ? `${r.roleName} (${r.roleKey})` : String(rid)
}

const fetchRoles = async () => {
  const pg = unwrapResult(await adminRolesApi.pageRoles(1, ROLE_PAGE_SIZE))
  roleOptions.value = pg.records ?? []
}

const fetchList = async () => {
  loading.value = true
  try {
    const pg = unwrapResult(await adminUsersApi.pageUsers(pager.current, pager.size))
    rows.value = pg.records ?? []
    total.value = pg.total
  } catch (e) {
    showSubmitError(e, '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const openCreate = (): void => {
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    username: '',
    password: '',
    nickname: '',
    email: '',
    phone: '',
    points: 0,
    roleId: 2,
    status: USER_STATUS_NORMAL,
  })
  dlgVisible.value = true
}

const openEdit = (row: SysUser): void => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    nickname: row.nickname ?? '',
    email: row.email ?? '',
    phone: row.phone ?? '',
    points: row.points ?? 0,
    roleId: row.roleId ?? 2,
    status: row.status ?? USER_STATUS_NORMAL,
  })
  dlgVisible.value = true
}

const submitDialog = async (): Promise<void> => {
  const username = (form.username ?? '').trim()
  if (!username) {
    ElMessage.warning('请填写用户名')
    return
  }
  if (!isEdit.value) {
    if (!(form.password ?? '').trim()) {
      ElMessage.warning('请填写初始密码')
      return
    }
  }
  dlgSaving.value = true
  try {
    if (isEdit.value && form.id != null) {
      const body: Partial<SysUser> & { password?: string } = {
        username,
        nickname: form.nickname || undefined,
        email: form.email || undefined,
        phone: form.phone || undefined,
        points: form.points,
        roleId: form.roleId,
        status: form.status,
      }
      if ((form.password ?? '').trim()) {
        body.password = form.password
      }
      unwrapResult(await adminUsersApi.updateUser(form.id, body))
      ElMessage.success('已保存')
    } else {
      unwrapResult(
        await adminUsersApi.createUser({
          username,
          password: form.password,
          nickname: form.nickname || undefined,
          email: form.email || undefined,
          phone: form.phone || undefined,
          points: form.points,
          roleId: form.roleId,
          status: form.status,
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

const removeRow = async (row: SysUser): Promise<void> => {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」？（逻辑删除）`, '确认', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    unwrapResult(await adminUsersApi.deleteUser(row.id))
    ElMessage.success('已删除')
    await fetchList()
  } catch (e) {
    showSubmitError(e, '删除失败')
  }
}

onMounted(() => {
  void fetchRoles()
  void fetchList()
})
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="openCreate">新建用户</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border stripe row-key="id">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="100" />
      <el-table-column label="角色" min-width="160">
        <template #default="{ row }">
          {{ roleNameById(row.roleId) }}
        </template>
      </el-table-column>
      <el-table-column prop="points" label="积分" width="88" />
      <el-table-column label="状态" width="88">
        <template #default="{ row }">
          <el-tag :type="row.status === USER_STATUS_NORMAL ? 'success' : 'danger'">
            {{ row.status === USER_STATUS_NORMAL ? '正常' : '封禁' }}
          </el-tag>
        </template>
      </el-table-column>
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
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" :disabled="isEdit" autocomplete="off" />
        </el-form-item>
        <el-form-item :label="isEdit ? '新密码' : '初始密码'" :required="!isEdit">
          <el-input v-model="form.password" type="password" show-password autocomplete="new-password" :placeholder="isEdit ? '留空则不修改' : ''" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleId" placeholder="请选择" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="`${r.roleName} (${r.roleKey})`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="积分">
          <el-input-number v-model="form.points as number" :min="0" :step="10" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="USER_STATUS_NORMAL">正常</el-radio>
            <el-radio :value="USER_STATUS_BANNED">封禁</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="form.phone" />
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
</style>
