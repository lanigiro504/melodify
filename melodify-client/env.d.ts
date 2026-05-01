/// <reference types="vite/client" />

import 'vue-router'

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    /** 浏览器标签页标题（不含站点后缀） */
    title?: string
  }
}
