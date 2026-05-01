export const RECHARGE_ORDER_STATUS = {
  PENDING: 0,
  PAID: 1,
} as const

export const rechargeOrderStatusLabel = (status: number | null | undefined): string => {
  if (status === RECHARGE_ORDER_STATUS.PAID) return '已支付'
  if (status === RECHARGE_ORDER_STATUS.PENDING) return '待支付'
  return '未知'
}
