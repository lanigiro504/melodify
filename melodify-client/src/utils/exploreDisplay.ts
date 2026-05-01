import type { ExploreAssetItem } from '@/api/explore'

/** 全局播放器副标题：时长与赞数 */
export function exploreItemPlaySubtitle(item: ExploreAssetItem): string {
  const parts: string[] = []
  if (item.durationSec != null && item.durationSec > 0) {
    parts.push(`${item.durationSec} 秒`)
  }
  parts.push(`${item.likeCount} 赞`)
  return parts.join(' · ')
}
