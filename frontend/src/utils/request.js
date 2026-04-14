import axios from 'axios'

const request = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 15000
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

export default request