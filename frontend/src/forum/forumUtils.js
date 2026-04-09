export const SECTION_LABELS = {
  knowledge: '知识分享',
  'q-a': '问答',
  help: '求助',
  notes: '笔记'
}

export function sectionLabel(section) {
  return SECTION_LABELS[section] || section || '论坛'
}

/** 将后端返回的 /uploads/... 转为可访问的绝对地址（与 axios baseURL 一致） */
export function resolveMediaUrl(path) {
  if (!path || typeof path !== 'string') return ''
  const p = path.trim()
  if (!p) return ''
  if (/^https?:\/\//i.test(p)) return p
  const base = (typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE)
    ? String(import.meta.env.VITE_API_BASE).replace(/\/$/, '')
    : 'http://localhost:8080'
  return `${base}${p.startsWith('/') ? p : `/${p}`}`
}

export function formatPostTime(createTime) {
  if (!createTime) return ''
  try {
    return new Date(createTime).toLocaleString('zh-CN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return String(createTime)
  }
}
