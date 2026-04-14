/**
 * 与 Streamlit 实验页对齐：带上主站 JWT，便于 iframe 内按用户隔离 session 并展示头像/昵称。
 */
export function buildStreamlitBaseUrl() {
  const configured = String(import.meta.env.VITE_ST_BASE || '/st').trim()
  if (!configured) return '/st/'
  if (/^https?:\/\//i.test(configured)) return configured.replace(/\/$/, '')
  const normalized = configured.startsWith('/') ? configured.replace(/\/$/, '') : `/${configured.replace(/\/$/, '')}`
  return `${normalized}/`
}

/**
 * @param {string} projectKey - app.py 中 project 参数：kmeans | logistic | neural | linear | text
 */
export function buildStreamlitProjectUrl(projectKey) {
  const baseUrl = buildStreamlitBaseUrl()
  const params = new URLSearchParams({ project: projectKey })
  const token = localStorage.getItem('token')
  if (token) {
    params.set('st_token', token)
  }
  return `${baseUrl}?${params.toString()}`
}
