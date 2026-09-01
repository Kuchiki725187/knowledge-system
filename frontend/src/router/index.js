import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/login', component: () => import('../views/LoginView.vue') },
  { path: '/register', component: () => import('../views/RegisterView.vue') },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/knowledge',
    children: [
      { path: 'plaza', component: () => import('../views/PlazaView.vue') },
      { path: 'knowledge', component: () => import('../views/KnowledgeListView.vue') },
      { path: 'knowledge/new', component: () => import('../views/KnowledgeEditView.vue') },
      { path: 'knowledge/:id/edit', component: () => import('../views/KnowledgeEditView.vue') },
      { path: 'knowledge/:id', component: () => import('../views/KnowledgeDetailView.vue') },
      { path: 'category', component: () => import('../views/CategoryView.vue') },
      { path: 'tag', component: () => import('../views/TagView.vue') },
      { path: 'favorite', component: () => import('../views/FavoriteView.vue') },
      { path: 'history', component: () => import('../views/HistoryView.vue') },
      { path: 'search', component: () => import('../views/SearchView.vue') },
      { path: 'stats', component: () => import('../views/StatsView.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const store = useUserStore()
  const publicPages = ['/login', '/register']
  if (!publicPages.includes(to.path) && !store.accessToken) {
    return '/login'
  }
})

export default router
