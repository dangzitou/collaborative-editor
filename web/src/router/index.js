import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../layouts/HomeLayout.vue')
  },
  {
    path: '/doc/:docId',
    name: 'Editor',
    component: () => import('../layouts/EditorLayout.vue')
  },
  // 兼容旧的 ?docId=xxx 格式
  {
    path: '/editor',
    redirect: (to) => {
      const docId = to.query.docId
      return docId ? `/doc/${docId}` : '/'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
