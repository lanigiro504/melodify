<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import {
  buildSimulatedNotify,
  createRechargeOrder,
  listPointProducts,
  notifySimulatedPay,
} from '@/api/recharge'
import { useAuthStore } from '@/stores/auth'
import type { PointProduct, RechargeOrder } from '@/types/api'
import { unwrapResult } from '@/utils/apiResult'
import { showSubmitError } from '@/utils/showSubmitError'

defineOptions({ name: 'RechargePage' })

const auth = useAuthStore()
const loading = ref(false)
const paying = ref<number | null>(null)
const products = ref<PointProduct[]>([])
const latestOrder = ref<RechargeOrder | null>(null)

const yuan = (cent: number) => `¥${(cent / 100).toFixed(2)}`

async function fetchProducts() {
  loading.value = true
  try {
    products.value = unwrapResult(await listPointProducts())
  } catch (e) {
    showSubmitError(e, '加载积分商品失败')
  } finally {
    loading.value = false
  }
}

async function pay(product: PointProduct) {
  paying.value = product.id
  try {
    const order = unwrapResult(await createRechargeOrder(product.id))
    const payload = unwrapResult(await buildSimulatedNotify(order.orderNo))
    latestOrder.value = unwrapResult(await notifySimulatedPay(payload))
    await auth.refreshMe()
    ElMessage.success('模拟支付成功，积分已到账')
  } catch (e) {
    showSubmitError(e, '模拟支付失败')
  } finally {
    paying.value = null
  }
}

onMounted(() => void fetchProducts())
</script>

<template>
  <div class="page-stack">
    <section class="page-hero melodify-glass-card">
      <div>
        <p class="page-eyebrow">Points</p>
        <h1 class="page-title page-title--lg">积分充值</h1>
        <p class="page-desc page-desc--wide">
          当前采用模拟支付：下单后生成带签名的支付通知，再由后端验签、防重放并完成积分到账。
        </p>
      </div>
      <div class="balance-card">
        <span>当前积分</span>
        <strong>{{ auth.currentUser?.points ?? 0 }}</strong>
      </div>
    </section>

    <section v-loading="loading" class="section-grid section-grid--3">
      <article v-for="item in products" :key="item.id" class="product-card soft-card">
        <p>{{ item.productName }}</p>
        <h2>{{ item.points }} 积分</h2>
        <span>{{ yuan(item.priceCent) }}</span>
        <el-button
          type="primary"
          round
          :loading="paying === item.id"
          @click="pay(item)"
        >
          模拟支付
        </el-button>
      </article>
    </section>

    <section v-if="latestOrder" class="melodify-glass-card order-card">
      <p class="page-eyebrow">Latest Order</p>
      <h2>最近订单已完成</h2>
      <div class="order-grid">
        <span>订单号：{{ latestOrder.orderNo }}</span>
        <span>商品：{{ latestOrder.productName }}</span>
        <span>到账：{{ latestOrder.points }} 积分</span>
        <span>金额：{{ yuan(latestOrder.amountCent) }}</span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.balance-card {
  min-width: 9rem;
  padding: 1rem 1.25rem;
  border-radius: 1.25rem;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.balance-card span,
.product-card p {
  margin: 0;
  color: var(--melodify-muted);
}

.balance-card strong {
  display: block;
  margin-top: 0.2rem;
  color: var(--melodify-strong);
  font-size: 2rem;
  font-weight: 900;
}

.product-card {
  padding: 1.4rem;
}

.product-card h2 {
  margin: 0.5rem 0 0.2rem;
  color: var(--melodify-strong);
  font-size: 1.8rem;
}

.product-card span {
  display: block;
  margin-bottom: 1rem;
  color: var(--el-color-primary);
  font-weight: 900;
}

.order-card {
  padding: 1.3rem;
}

.order-card h2 {
  margin: 0.2rem 0 1rem;
  color: var(--melodify-strong);
}

.order-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.6rem 1rem;
  color: var(--melodify-muted);
}

@media (max-width: 720px) {
  .order-grid {
    grid-template-columns: 1fr;
  }
}
</style>
