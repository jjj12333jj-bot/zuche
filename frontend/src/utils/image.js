/**
 * 图片地址处理：车辆与轮播图统一放在 public/images 下，离线可用。
 * 任一图片 404 时回落到本地占位图，避免答辩现场出现破图。
 */
export const FALLBACK_IMAGE = '/images/placeholder.svg'
export const DEFAULT_AVATAR = '/images/avatar/default.svg'

export function resolveImage(url, fallback = FALLBACK_IMAGE) {
  if (!url) return fallback
  if (/^(https?:)?\/\//i.test(url) || url.startsWith('data:')) {
    return url
  }
  return url.startsWith('/') ? url : `/${url}`
}

export function onImageError(event) {
  const img = event?.target
  if (!img || img.dataset.fallback === '1') return
  img.dataset.fallback = '1'
  img.src = FALLBACK_IMAGE
}
