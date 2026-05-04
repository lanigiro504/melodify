import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import {
  Coin,
  Cpu,
  FolderOpened,
  Goods,
  Key,
  Postcard,
  Sell,
  UserFilled,
} from '@element-plus/icons-vue'

export const ADMIN_ROUTE_NAMES = {
  LOGIN: 'admin-login',
  USERS: 'admin-users',
  ROLES: 'admin-roles',
  TASKS: 'admin-tasks',
  ASSETS: 'admin-assets',
  POINT_LOGS: 'admin-point-logs',
  RECHARGE_ORDERS: 'admin-recharge-orders',
  POINT_PRODUCTS: 'admin-point-products',
} as const

/** 控制台根路径重定向的子路径片段（无前导 /） */
export const ADMIN_DEFAULT_CHILD_PATH = 'tasks'

export type AdminNavLeaf = {
  path: string
  name: string
  title: string
  icon: Component
  loader: () => Promise<unknown>
}

export type AdminNavSection =
  | { type: 'item'; leaf: AdminNavLeaf }
  | {
      type: 'submenu'
      index: string
      title: string
      icon: Component
      children: AdminNavLeaf[]
    }

export const adminNavSections: AdminNavSection[] = [
  {
    type: 'item',
    leaf: {
      path: 'users',
      name: ADMIN_ROUTE_NAMES.USERS,
      title: '用户管理',
      icon: UserFilled,
      loader: () => import('@/views/users/index.vue'),
    },
  },
  {
    type: 'item',
    leaf: {
      path: 'roles',
      name: ADMIN_ROUTE_NAMES.ROLES,
      title: '角色管理',
      icon: Key,
      loader: () => import('@/views/roles/index.vue'),
    },
  },
  {
    type: 'submenu',
    index: 'ops',
    title: '运营管理',
    icon: Postcard,
    children: [
      {
        path: 'tasks',
        name: ADMIN_ROUTE_NAMES.TASKS,
        title: '生成任务',
        icon: Cpu,
        loader: () => import('@/views/tasks/index.vue'),
      },
      {
        path: 'assets',
        name: ADMIN_ROUTE_NAMES.ASSETS,
        title: '成品资产',
        icon: FolderOpened,
        loader: () => import('@/views/assets/index.vue'),
      },
      {
        path: 'point-logs',
        name: ADMIN_ROUTE_NAMES.POINT_LOGS,
        title: '积分流水',
        icon: Coin,
        loader: () => import('@/views/point-logs/index.vue'),
      },
      {
        path: 'recharge-orders',
        name: ADMIN_ROUTE_NAMES.RECHARGE_ORDERS,
        title: '充值订单',
        icon: Sell,
        loader: () => import('@/views/recharge-orders/index.vue'),
      },
      {
        path: 'point-products',
        name: ADMIN_ROUTE_NAMES.POINT_PRODUCTS,
        title: '积分商品',
        icon: Goods,
        loader: () => import('@/views/point-products/index.vue'),
      },
    ],
  },
]

function collectLeaves(sections: AdminNavSection[]): AdminNavLeaf[] {
  const out: AdminNavLeaf[] = []
  for (const s of sections) {
    if (s.type === 'item') out.push(s.leaf)
    else out.push(...s.children)
  }
  return out
}

export function buildAdminShellChildRoutes(): RouteRecordRaw[] {
  const leaves = collectLeaves(adminNavSections)
  const defaultLeaf = leaves.find((l) => l.path === ADMIN_DEFAULT_CHILD_PATH)
  if (!defaultLeaf) {
    throw new Error(
      `ADMIN_DEFAULT_CHILD_PATH "${ADMIN_DEFAULT_CHILD_PATH}" does not match any nav leaf segment`,
    )
  }
  return [
    { path: '', redirect: { name: defaultLeaf.name } },
    ...leaves.map((leaf) => ({
      path: leaf.path,
      name: leaf.name,
      component: leaf.loader,
      meta: { title: leaf.title },
    })),
  ]
}

export function adminSubmenuDefaultOpenIndexes(): string[] {
  return adminNavSections.flatMap((s) => (s.type === 'submenu' ? [s.index] : []))
}
