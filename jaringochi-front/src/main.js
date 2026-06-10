// 참고: import 순서
// 보통 외부 라이브러리(vue, pinia) -> 내 파일(router, App) -> CSS 순으로 정리

import { createApp } from 'vue' 
import { createPinia } from 'pinia'   
import router from './router'          // 폴더명만 적어도 자동으로 index.js 확인됨
import App from './App.vue'
import './style.css'



createApp(App) // App.vue로 앱을 만든다
  .use(createPinia())   // Pinia 장착 -> 어느 컴포넌트서나 store 사용 가능 (사용자 관련 정보를 어느 컴포넌트에서든 열기 위해)
  .use(router)          // 라우터 장착 -> <router-view>/<router-link> 작동
  .mount('#app')  // (항상 맨 마지막) index.html의 <div id="app">에 끼워 넣어 화면에 띄운다 