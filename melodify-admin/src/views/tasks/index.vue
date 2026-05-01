<script setup lang="ts">
import { Cpu } from '@element-plus/icons-vue'
import { onMounted, reactive } from 'vue'
import type { MusicTask } from '@/types/api'
import * as adminMusicTasksApi from '@/api/adminMusicTasks'
import { MUSIC_TASK_STATUS, musicTaskStatusLabel } from '@/constants/musicTask'
import { ADMIN_MAX_PAGE_SIZE, useAdminPaging } from '@/composables/useAdminPaging'
import { shortenText } from '@/utils/text'

defineOptions({ name: 'AdminTasksPage' })

const { loading, rows, total, pager, fetchPage } = useAdminPaging<MusicTask>()

const filters = reactive({
  userId: undefined as undefined | number,
  status: undefined as undefined | number,
})

const statusOptions = [
  { label: '排队', value: MUSIC_TASK_STATUS.QUEUED },
  { label: '生成中', value: MUSIC_TASK_STATUS.GENERATING },
  { label: '已完成', value: MUSIC_TASK_STATUS.SUCCEEDED },
  { label: '失败', value: MUSIC_TASK_STATUS.FAILED },
  { label: '已取消', value: MUSIC_TASK_STATUS.CANCELLED },
]

const loadList = () =>
  void fetchPage(
    () =>
      adminMusicTasksApi.pageMusicTasksAdmin(pager.current, pager.size, {
        ...(typeof filters.userId === 'number' ? { userId: filters.userId } : {}),
        ...(typeof filters.status === 'number' ? { status: filters.status } : {}),
      }),
    '加载生成任务失败',
  )

const onSearch = () => {
  pager.current = 1
  loadList()
}

onMounted(() => loadList())
</script>

<template>
  <div class="admin-page-board">
    <div class="admin-toolbar">
      <el-input v-model.number="filters.userId" class="filt" clearable placeholder="用户 ID（可选）" type="number" />
      <el-select v-model="filters.status" class="filt" placeholder="任务状态（可选）" clearable style="width: 160px">
        <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-button type="primary" :icon="Cpu" @click="onSearch">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border stripe row-key="id" size="small" empty-text="暂无数据">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="taskId" label="业务编号" min-width="180" show-overflow-tooltip />
      <el-table-column prop="userId" label="用户" width="88" />
      <el-table-column prop="modelCode" label="模型" width="120" />
      <el-table-column label="提示词" min-width="200">
        <template #default="{ row }">
          {{ shortenText(row.prompt ?? '', 80) }}
        </template>
      </el-table-column>
      <el-table-column prop="costPoints" label="扣积分" width="80" />
      <el-table-column label="状态" width="88">
        <template #default="{ row }">
          <el-tag
            :type="
              row.status === MUSIC_TASK_STATUS.SUCCEEDED ? 'success' : row.status === MUSIC_TASK_STATUS.FAILED ? 'danger' : 'info'
            "
            size="small"
          >
            {{ musicTaskStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="158" />
    </el-table>
    <el-pagination
      class="admin-pager"
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
.filt {
  width: 160px;
}
</style>
