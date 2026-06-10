import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'
import LedgerView from '@/views/LedgerView.vue'
import StatsView from '@/views/StatsView.vue'
import MoreView from '@/views/MoreView.vue'
import LoginView from '@/views/LoginView.vue'
import SignupView from '@/views/SignupView.vue'
import CategoryView from '@/views/CategoryView.vue'

// 주소 <-> 화면 짝짓는 표
const routes = [
  { path: '/',          name: 'home',     component: HomeView },
  { path: '/ledger',    name: 'ledger',   component: LedgerView },
  { path: '/stats',     name: 'stats',    component: StatsView },
  { path: '/more',      name: 'more',     component: MoreView },
  { path: '/login',     name: 'login',    component: LoginView },
  { path: '/signup',    name: 'signup',   component: SignupView },
  { path: '/categories', name: 'categories', component: CategoryView },
]

const router = createRouter({
  history: createWebHistory(),  
  routes,                        // 위에서 만든 표
})

// 라우트 가드: 화면 들어가기 전 검문소 (지금은 전부 통과)
router.beforeEach((to, from) => {
  // * 6/9 JWT 학습 후: 로그인 안 했으면 /login 으로 보내는 코드가 여기 들어감
  return true   // true = 통과
})

export default router