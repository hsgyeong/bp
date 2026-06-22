import { createRouter, createWebHistory } from 'vue-router'

import NotFoundView from '@/views/NotFoundView.vue'
import HomeView from '@/views/HomeView.vue'
import LedgerView from '@/views/LedgerView.vue'
import StatsView from '@/views/StatsView.vue'
import MoreView from '@/views/MoreView.vue'
import LoginView from '@/views/LoginView.vue'
import SignupView from '@/views/SignupView.vue'
import CategoryView from '@/views/CategoryView.vue'
import MyPageView from '@/views/MyPageView.vue'
import ProfileEditView from '@/views/ProfileEditView.vue'
import BudgetView from '@/views/BudgetView.vue'
import TransactionFormView from '@/views/TransactionFormView.vue'
import GulbiRewardView from '@/views/GulbiRewardView.vue'


// 주소 <-> 화면 짝짓는 표
const routes = [
  { path: '/login',     name: 'login',    component: LoginView },
  { path: '/signup',    name: 'signup',   component: SignupView },
  { path: '/',          name: 'home',     component: HomeView, meta: { requiresAuth: true } },
  { path: '/ledger',    name: 'ledger',   component: LedgerView, meta: { requiresAuth: true } },
  { path: '/ledger/new', name:'transaction-new', component: TransactionFormView, meta: { requiresAuth: true } },
  { path: '/ledger/:id/edit', name: 'transaction-edit', component: TransactionFormView, props: true, meta: { requiresAuth: true } },
  { path: '/stats',     name: 'stats',    component: StatsView, meta: { requiresAuth: true } },
  { path: '/more',      name: 'more',     component: MoreView, meta: { requiresAuth: true } },
  { path: '/me', name: 'me', component: MyPageView, meta: { requiresAuth: true } },
  { path: '/me/edit', name: 'profile-edit', component: ProfileEditView, meta: { requiresAuth: true } },
  { path: '/categories', name: 'categories', component: CategoryView, meta: { requiresAuth: true } },
  { path: '/budget',    name: 'budget',   component: BudgetView, meta: { requiresAuth: true } }, 
  { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView,},
  { path: '/gulbi-reward/:weeklyBudgetId', name: 'gulbi-reward',     
    component: GulbiRewardView, props: true, meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),  
  routes,                        // 위에서 만든 표
})

// 라우트 가드: 로그인이 필요한 화면은 토큰이 있을 때만 통과
router.beforeEach((to) => {
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  return true   // true = 통과
})

export default router
