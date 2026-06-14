import { defineStore } from 'pinia'
import { ref } from 'vue'

function readSavedUser() {
    try {
        return JSON.parse(localStorage.getItem('user') || 'null')
    } catch {
        return null
    }
}

// 'auth'라는 이름표를 단 창고를 정의
export const useAuthStore = defineStore('auth', () =>
{
    // -- state: 창고가 보관하는 데이터 --
    const token = ref(localStorage.getItem('token')) // 로그인 토큰. 새로고침해도 localStorage에서 복구
    const user = ref(readSavedUser()) // 유저 정보 {id, nickname} 들어갈 자리


    // -- actions: 데이터를 바꾸는 함수 --
    function login(newToken, newUser) {
        token.value = newToken
        user.value = newUser

        localStorage.setItem('token', newToken)
        localStorage.setItem('user', JSON.stringify(newUser))
    }

    function logout() {
        token.value = null
        user.value = null

        localStorage.removeItem('token')
        localStorage.removeItem('user')
    }

    // 바깥(컴포넌트)에서 쓸 것들을 내보냄
    return { token, user, login, logout }

})
