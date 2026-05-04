import type { RouteLocationNormalizedLoaded } from 'vue-router'

const ADMIN_SITE_TITLE = 'Melodify 控制台'

export function composeAdminDocumentTitle(
  route: RouteLocationNormalizedLoaded,
  fallback = ADMIN_SITE_TITLE,
): string {
  const matched = route.matched
  for (let i = matched.length - 1; i >= 0; i--) {
    const t = matched[i]?.meta?.title
    if (typeof t === 'string' && t.trim()) return `${t.trim()} · ${ADMIN_SITE_TITLE}`
  }
  return fallback
}

export function resolveMatchedPageTitle(route: RouteLocationNormalizedLoaded, fallback = '工作台'): string {
  const matched = route.matched
  for (let i = matched.length - 1; i >= 0; i--) {
    const t = matched[i]?.meta?.title
    if (typeof t === 'string' && t.trim()) return t.trim()
  }
  return fallback
}
