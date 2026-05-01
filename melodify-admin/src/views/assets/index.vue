<script setup lang="ts">
import { FolderOpened } from '@element-plus/icons-vue'
import { onMounted, reactive } from 'vue'
import type { MusicAsset } from '@/types/api'
import * as adminMusicAssetsApi from '@/api/adminMusicAssets'
import { ADMIN_MAX_PAGE_SIZE, useAdminPaging } from '@/composables/useAdminPaging'
import { shortenText } from '@/utils/text'

defineOptions({ name: 'AdminAssetsPage' })

const { loading, rows, total, pager, fetchPage } = useAdminPaging<MusicAsset>()

const filters = reactive({
  userId: undefined as undefined | number,
  isPublic: undefined as undefined | number,
})

const publicOptions = [
  { label: '公开', value: 1 },
  { label: '非公开', value: 0 },
]

const loadList = () =>
  void fetchPage(
    () =>
      adminMusicAssetsApi.pageMusicAssetsAdmin(pager.current, pager.size, {
        ...(typeof filters.userId === 'number' ? { userId: filters.userId } : {}),
        ...(typeof filters.isPublic === 'number' ? { isPublic: filters.isPublic } : {}),
      }),
    '加载作品列表失败',
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
      <el-select v-model="filters.isPublic" class="filt" placeholder="公开性（可选）" clearable style="width: 140px">
        <el-option v-for="o in publicOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-button type="primary" :icon="FolderOpened" @click="onSearch">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border stripe row-key="id" size="small" empty-text="暂无数据">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="assetId" label="资产编号" min-width="170" />
      <el-table-column prop="userId" label="用户" width="88" />
      <el-table-column prop="taskId" label="任务PK" width="88" />
      <el-table-column label="标题" min-width="140">
        <template #default="{ row }">{{ shortenText(row.title ?? '', 40) }}</template>
      </el-table-column>
      <el-table-column label="试听地址" min-width="200">
        <template #default="{ row }">{{ shortenText(row.fileUrl ?? '', 48) }}</template>
      </el-table-column>
      <el-table-column label="公开" width="72">
        <template #default="{ row }">
          <el-tag :type="row.isPublic === 1 ? 'success' : 'info'" size="small">
            {{ row.isPublic === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="158" />
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
.pager {
  margin-top: 1rem;
  justify-content: flex-end;
}
</style>
