import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const API_BASE = '/api'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || null)
  const loading = ref(false)
  const error = ref(null)

  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const currentUsername = computed(() => user.value?.nickname || user.value?.username || '')

  // 初始化时从 localStorage 恢复用户信息
  const savedUser = localStorage.getItem('user')
  if (savedUser) {
    try {
      user.value = JSON.parse(savedUser)
    } catch (e) {
      localStorage.removeItem('user')
    }
  }

  async function register(username, password, nickname) {
    loading.value = true
    error.value = null
    try {
      const res = await fetch(`${API_BASE}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password, nickname })
      })
      const data = await res.json()
      if (data.code !== 200) {
        throw new Error(data.message || '注册失败')
      }
      return true
    } catch (e) {
      error.value = e.message
      return false
    } finally {
      loading.value = false
    }
  }

  async function login(username, password) {
    loading.value = true
    error.value = null
    try {
      const res = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      })
      const data = await res.json()
      if (data.code !== 200) {
        throw new Error(data.message || '登录失败')
      }
      token.value = data.data.token
      user.value = {
        id: data.data.userId,
        username: data.data.username,
        nickname: data.data.nickname || data.data.username,
        avatar: data.data.avatar
      }
      localStorage.setItem('token', token.value)
      localStorage.setItem('user', JSON.stringify(user.value))
      return true
    } catch (e) {
      error.value = e.message
      return false
    } finally {
      loading.value = false
    }
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return {
    user, token, loading, error,
    isLoggedIn, currentUsername,
    register, login, logout
  }
})
