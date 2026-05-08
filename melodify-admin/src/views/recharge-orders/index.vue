<script setup lang="ts">
import { Sell } from '@element-plus/icons-vue'
import { onMounted, reactive } from 'vue'
import type { RechargeOrder } from '@/types/api'
import * as adminRechargeOrdersApi from '@/api/adminRechargeOrders'
import { RECHARGE_ORDER_STATUS, rechargeOrderStatusLabel } from '@/constants/recharge'
import { ADMIN_MAX_PAGE_SIZE, useAdminPaging } from '@/composables/useAdminPaging'
import { formatDateTimeZh } from '@/utils/formatDateTime'

defineOptions({ name: 'AdminRechargeOrdersPage' })

const { loading, rows, total, pager, fetchPage } = useAdminPaging<RechargeOrder>()

const filters = reactive({
  userId: undefined as undefined | number,
  status: undefined as undefined | number,
})

const statusOptions = [
  { label: '待支付', value: RECHARGE_ORDER_STATUS.PENDING },
  { label: '已支付', value: RECHARGE_ORDER_STATUS.PAID },
]

const loadList = () =>
  void fetchPage(
    () =>
      adminRechargeOrdersApi.pageRechargeOrdersAdmin(pager.current, pager.size, {
        ...(typeof filters.userId === 'number' ? { userId: filters.userId } : {}),
        ...(typeof filters.status === 'number' ? { status: filters.status } : {}),
      }),
    '加载充值订单失败',
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
      <el-select v-model="filters.status" class="filt" placeholder="订单状态（可选）" clearable style="width: 140px">
        <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-button type="primary" :icon="Sell" @click="onSearch">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border stripe row-key="id" size="small" empty-text="暂无数据">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
      <el-table-column prop="userId" label="用户" width="88" />
      <el-table-column prop="productName" label="商品" min-width="120" />
      <el-table-column prop="points" label="积分" width="72" />
      <el-table-column prop="amountCent" label="金额(分)" width="88" />
      <el-table-column label="状态" width="88">
        <template #default="{ row }">
          <el-tag :type="row.status === RECHARGE_ORDER_STATUS.PAID ? 'success' : 'warning'" size="small">
            {{ rechargeOrderStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付时间" min-width="172">
        <template #default="{ row }">
          {{ formatDateTimeZh(row.paidAt) }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="172">
        <template #default="{ row }">
          {{ formatDateTimeZh(row.createTime) }}
        </template>
      </el-table-column>
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
