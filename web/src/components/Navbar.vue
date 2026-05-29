<script setup>
import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'

const props = defineProps({
  isHome: { type: Boolean, default: false },
  docTitle: { type: String, default: '' },
  isConnected: { type: Boolean, default: false },
  onlineUsers: { type: Array, default: () => [] },
  isEditingTitle: { type: Boolean, default: false }
})

const emit = defineEmits([
  'open-create', 'open-list', 'open-join', 'login', 'logout',
  'share', 'update-title', 'start-edit-title', 'cancel-edit-title'
])

const authStore = useAuthStore()
const currentUsername = computed(() => authStore.currentUsername || '游客')

function getInitial(name) {
  return name ? name.charAt(0).toUpperCase() : '?'
}
</script>

<template>
  <header class="navbar">
    <div class="navbar-left">
      <div class="logo">
        <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm4 18H6V4h7v5h5v11z"/>
        </svg>
      </div>
      <div v-if="authStore.isLoggedIn && isHome" style="display: flex; gap: 8px; margin-right: 15px;">
        <button class="btn-primary" @click="$emit('open-create')" style="padding: 6px 12px; font-size: 14px;">新建</button>
        <button class="btn-secondary" @click="$emit('open-list')" style="padding: 6px 12px; font-size: 14px;">我的文档</button>
        <button class="btn-secondary" @click="$emit('open-join')" style="padding: 6px 12px; font-size: 14px;">加入协作</button>
      </div>
      <div class="doc-info" v-if="!isHome">
        <div class="doc-title" v-if="!isEditingTitle" @click="$emit('start-edit-title')">
          {{ docTitle }}
        </div>
        <input
          v-else
          :value="docTitle"
          class="doc-title-input"
          @blur="$emit('update-title', $event.target.value)"
          @keyup.enter="$emit('update-title', $event.target.value)"
          autofocus
        >
        <div class="doc-status">
          <span v-if="isConnected" class="status-saved">已同步</span>
          <span v-else class="status-offline">离线</span>
        </div>
      </div>
    </div>

    <div class="navbar-right">
      <button v-if="isConnected && !isHome" class="btn-primary" @click="$emit('share')" style="margin-right: 10px; padding: 6px 16px;">
        分享
      </button>

      <div class="online-users" v-if="onlineUsers.length > 0">
        <div
          v-for="user in onlineUsers.slice(0, 5)"
          :key="user.name"
          class="user-avatar"
          :style="{ background: user.color }"
          :title="user.name"
        >
          {{ getInitial(user.name) }}
        </div>
        <div v-if="onlineUsers.length > 5" class="user-count">
          +{{ onlineUsers.length - 5 }}
        </div>
      </div>

      <div v-if="!authStore.isLoggedIn" class="auth-buttons">
        <button class="btn-login" @click="$emit('login')">登录</button>
      </div>
      <UserMenu v-else @logout="$emit('logout')" />
    </div>
  </header>
</template>

<script>
import UserMenu from './UserMenu.vue'
export default { components: { UserMenu } }
</script>

<style scoped>
.navbar {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo { color: #1a73e8; display: flex; }

.doc-info { display: flex; flex-direction: column; gap: 2px; }

.doc-title {
  font-size: 18px; font-weight: 500; color: #202124;
  cursor: pointer; padding: 2px 8px; margin: -2px -8px; border-radius: 4px;
}
.doc-title:hover { background: #f1f3f4; }

.doc-title-input {
  font-size: 18px; font-weight: 500; border: none; outline: none;
  padding: 2px 8px; margin: -2px -8px; border-radius: 4px; background: #e8f0fe;
}

.doc-status { font-size: 12px; color: #5f6368; }
.status-saved { color: #34a853; }
.status-offline { color: #ea4335; }

.navbar-right { display: flex; align-items: center; gap: 16px; }

.online-users { display: flex; align-items: center; }

.user-avatar {
  width: 32px; height: 32px; border-radius: 50%; color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 500; margin-left: -8px; border: 2px solid #fff;
}
.user-avatar:first-child { margin-left: 0; }

.user-count { margin-left: 8px; font-size: 13px; color: #5f6368; }

.auth-buttons { display: flex; gap: 8px; }

.btn-login {
  padding: 8px 24px; background: #1a73e8; color: #fff;
  border: none; border-radius: 4px; font-size: 14px; font-weight: 500; cursor: pointer;
}
.btn-login:hover { background: #1557b0; }

.btn-primary {
  background: #1a73e8; color: #fff; border: none;
  border-radius: 4px; font-size: 14px; font-weight: 500; cursor: pointer;
}
.btn-primary:hover { background: #1557b0; }

.btn-secondary {
  background: #fff; color: #5f6368; border: 1px solid #dadce0;
  border-radius: 4px; font-size: 14px; font-weight: 500; cursor: pointer; white-space: nowrap;
}
.btn-secondary:hover { background: #f1f3f4; color: #202124; }
</style>
