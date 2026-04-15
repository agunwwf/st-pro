import axios from 'axios'

const runtimeOrigin = typeof window !== 'undefined' ? window.location.origin : ''
const configuredApiBase = String(import.meta.env.VITE_API_BASE || runtimeOrigin).replace(/\/$/, '')
const configuredWsBase = String(
    import.meta.env.VITE_WS_BASE || configuredApiBase.replace(/^http/i, 'ws')
).replace(/\/$/, '')

const request = axios.create({
    baseURL: configuredApiBase
})

request.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
        config.headers.token = token
    }
    return config
})

request.interceptors.response.use(
    res => res,
    err => {
        if (err.code === 'ECONNABORTED') {
            return Promise.reject(new Error('请求超时，请检查后端服务或数据库状态'))
        }
        if (err.response?.status === 401) {
            localStorage.removeItem('token')
            localStorage.removeItem('user')
            window.location.href = '/login'
        }
        return Promise.reject(err)
    }
)

export const API_BASE_URL = configuredApiBase
export const WS_BASE_URL = configuredWsBase
export default request