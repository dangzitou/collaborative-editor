import { ref, computed } from 'vue'

const API_BASE = 'http://localhost:8080/api'

// 用户状态（全局单例）
const user = ref(null)
const token = ref(localStorage.getItem('token') || null)

// 初始化时从 localStorage 恢复用户信息
const savedUser = localStorage.getItem('user')
if (savedUser) {
  try {
    user.value = JSON.parse(savedUser)
  } catch (e) {
    localStorage.removeItem('user')
  }
}

export function useAuth() {
  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const loading = ref(false)
  const error = ref(null)

  /**
   * 用户注册
   */
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

  /**
   * 用户登录
   */
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
      // 保存用户信息和 token
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

  /**
   * 退出登录
   */
  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return {
    user,
    token,
    isLoggedIn,
    loading,
    error,
    register,
    login,
    logout
  }
}

