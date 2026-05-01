<script setup lang="ts">
import { Histogram } from '@element-plus/icons-vue'
import { onMounted, reactive } from 'vue'
import type { PointLog } from '@/types/api'
import * as adminPointLogsApi from '@/api/adminPointLogs'
import { ADMIN_MAX_PAGE_SIZE, useAdminPaging } from '@/composables/useAdminPaging'
import { shortenText } from '@/utils/text'

defineOptions({ name: 'AdminPointLogsPage' })

const { loading, rows, total, pager, fetchPage } = useAdminPaging<PointLog>()

const filters = reactive({
  userId: undefined as undefined | number,
  bizType: '',
})

const loadList = () =>
  void fetchPage(
    () =>
      adminPointLogsApi.pagePointLogsAdmin(pager.current, pager.size, {
        ...(typeof filters.userId === 'number' ? { userId: filters.userId } : {}),
        ...(filters.bizType.trim() ? { bizType: filters.bizType.trim() } : {}),
      }),
    '加载积分流水失败',
  )

const onSearch = () => {
  pager.current = 1
  loadList()
}

onMounted(() => loadList())
</script>

<template>
  <div class="page card">
    <div class="toolbar">
      <el-input v-model.number="filters.userId" class="filt" clearable placeholder="用户 ID（可选）" type="number" />
      <el-input v-model="filters.bizType" class="filt wide" clearable placeholder="业务类型 bizType（如 recharge）" />
      <el-button type="primary" :icon="Histogram" @click="onSearch">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border stripe row-key="id" size="small" empty-text="暂无数据">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="logId" label="流水号" min-width="160" show-overflow-tooltip />
      <el-table-column prop="userId" label="用户" width="88" />
      <el-table-column prop="changeType" label="变更类型" width="88" />
      <el-table-column prop="amount" label="增减" width="72" />
      <el-table-column prop="balance" label="结余" width="88" />
      <el-table-column prop="bizType" label="业务类型" width="100" />
      <el-table-column prop="bizId" label="业务单号" min-width="140" />
      <el-table-column label="备注" min-width="160">
        <template #default="{ row }">{{ shortenText(row.remark ?? '', 48) }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" min-width="158" />
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
.filt {
  width: 160px;
}
.filt.wide {
  width: 220px;
}
.pager {
  margin-top: 1rem;
  justify-content: flex-end;
}
</style>
