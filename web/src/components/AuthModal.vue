<script setup>
import { ref } from 'vue'
import { useAuth } from '../composables/useAuth'

const props = defineProps({
  visible: Boolean
})

const emit = defineEmits(['close', 'success'])

const { login, register, loading, error } = useAuth()

const isLoginMode = ref(true)
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const nickname = ref('')
const localError = ref('')

function toggleMode() {
  isLoginMode.value = !isLoginMode.value
  localError.value = ''
}

async function handleSubmit() {
  localError.value = ''
  
  if (!username.value || !password.value) {
    localError.value = '请填写用户名和密码'
    return
  }
  
  if (isLoginMode.value) {
    // 登录
    const success = await login(username.value, password.value)
    if (success) {
      emit('success')
      emit('close')
      resetForm()
    } else {
      localError.value = error.value
    }
  } else {
    // 注册
    if (password.value !== confirmPassword.value) {
      localError.value = '两次输入的密码不一致'
      return
    }
    if (password.value.length < 6) {
      localError.value = '密码长度至少6位'
      return
    }
    const success = await register(username.value, password.value, nickname.value || username.value)
    if (success) {
      // 注册成功后自动登录
      const loginSuccess = await login(username.value, password.value)
      if (loginSuccess) {
        emit('success')
        emit('close')
        resetForm()
      }
    } else {
      localError.value = error.value
    }
  }
}

function resetForm() {
  username.value = ''
  password.value = ''
  confirmPassword.value = ''
  nickname.value = ''
  localError.value = ''
}

function handleClose() {
  emit('close')
  resetForm()
}
</script>

<template>
  <div v-if="visible" class="modal-overlay" @click.self="handleClose">
    <div class="modal">
      <button class="modal-close" @click="handleClose">x</button>
      
      <h2>{{ isLoginMode ? '登录' : '注册' }}</h2>
      
      <form @submit.prevent="handleSubmit">
        <div class="form-item">
          <label>用户名</label>
          <input v-model="username" type="text" placeholder="请输入用户名" />
        </div>
        
        <div v-if="!isLoginMode" class="form-item">
          <label>昵称（可选）</label>
          <input v-model="nickname" type="text" placeholder="请输入昵称" />
        </div>
        
        <div class="form-item">
          <label>密码</label>
          <input v-model="password" type="password" placeholder="请输入密码" />
        </div>
        
        <div v-if="!isLoginMode" class="form-item">
          <label>确认密码</label>
          <input v-model="confirmPassword" type="password" placeholder="请再次输入密码" />
        </div>
        
        <div v-if="localError" class="error-message">{{ localError }}</div>
        
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? '处理中...' : (isLoginMode ? '登录' : '注册') }}
        </button>
      </form>
      
      <div class="toggle-mode">
        <span v-if="isLoginMode">
          还没有账号？<a href="#" @click.prevent="toggleMode">立即注册</a>
        </span>
        <span v-else>
          已有账号？<a href="#" @click.prevent="toggleMode">立即登录</a>
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: #fff;
  border-radius: 8px;
  padding: 32px;
  width: 360px;
  position: relative;
}

.modal-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  font-size: 18px;
  cursor: pointer;
  color: #5f6368;
  border-radius: 50%;
}

.modal-close:hover {
  background: #f1f3f4;
}

h2 {
  margin: 0 0 24px 0;
  font-size: 24px;
  font-weight: 500;
  color: #202124;
  text-align: center;
}

.form-item {
  margin-bottom: 16px;
}

.form-item label {
  display: block;
  font-size: 13px;
  color: #5f6368;
  margin-bottom: 6px;
}

.form-item input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-item input:focus {
  outline: none;
  border-color: #1a73e8;
}

.error-message {
  color: #ea4335;
  font-size: 13px;
  margin-bottom: 16px;
  text-align: center;
}

.btn-primary {
  width: 100%;
  padding: 12px;
  background: #1a73e8;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
}

.btn-primary:hover {
  background: #1557b0;
}

.btn-primary:disabled {
  background: #94c4f7;
  cursor: not-allowed;
}

.toggle-mode {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: #5f6368;
}

.toggle-mode a {
  color: #1a73e8;
  text-decoration: none;
}

.toggle-mode a:hover {
  text-decoration: underline;
}
</style>

