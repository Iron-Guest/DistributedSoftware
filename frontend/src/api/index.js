import axios from 'axios'

const api = axios.create({
  // 开发环境: 通过 Nginx (80) 或直接连后端 (8081)
  // 生产环境 (Nginx反向代理): 使用相对路径
  baseURL: import.meta.env.DEV ? 'http://localhost/api' : '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('请求失败:', error)
    return Promise.reject(error)
  }
)

export function login(data) {
  return api.post('/user/login', data)
}

export function register(data) {
  return api.post('/user/register', data)
}

export function getUserInfo(id) {
  return api.get(`/user/${id}`)
}

export function getGoodsList(params) {
  return api.get('/goods', { params })
}

export function getGoodsDetail(id) {
  return api.get(`/goods/${id}`)
}

export function createGoods(data) {
  return api.post('/goods', data)
}

export function deleteGoods(id) {
  return api.delete(`/goods/${id}`)
}

export function createOrder(data) {
  return api.post('/order', data)
}

export function getOrderDetail(id) {
  return api.get(`/order/${id}`)
}

export function getUserOrders(userId) {
  return api.get(`/order/user/${userId}`)
}

export function cancelOrder(id) {
  return api.put(`/order/${id}/cancel`)
}

export function getStock(goodsId) {
  return api.get(`/stock/${goodsId}`)
}

export default api