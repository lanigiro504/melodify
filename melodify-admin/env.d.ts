/// <reference types="vite/client" />

export {}

declare module 'vue-router' {
  interface RouteMeta {
    /** 页面标题：顶栏与 document.title */
    title?: string
    public?: boolean
    requiresAuth?: boolean
  }
}
