import type { RouteLocationNormalizedLoaded } from 'vue-router'

const SITE_TITLE_FALLBACK = 'Melodify · AI 音乐'

export function composeClientDocumentTitle(route: RouteLocationNormalizedLoaded): string {
  const segment =
    typeof route.meta.title === 'string' && route.meta.title.trim() ? route.meta.title.trim() : ''
  return segment ? `${segment} · Melodify` : SITE_TITLE_FALLBACK
}
