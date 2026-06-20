import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    redirect: '/home/index',
    children: [
      {
        path: 'index',
        name: 'HomePage',
        component: () => import('../views/HomePage.vue')
      },
      {
        path: 'goods/:id',
        name: 'GoodsDetail',
        component: () => import('../views/GoodsDetail.vue')
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('../views/OrderList.vue')
      },
      {
        path: 'admin',
        name: 'AdminPage',
        component: () => import('../views/AdminPage.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router