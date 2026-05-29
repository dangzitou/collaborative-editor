<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '../stores/auth'

const emit = defineEmits(['logout'])
const authStore = useAuthStore()
const showMenu = ref(false)
const currentUsername = computed(() => authStore.currentUsername)

function getInitial(name) {
  return name ? name.charAt(0).toUpperCase() : '?'
}
</script>

<template>
  <div class="user-menu-wrapper">
    <button class="user-menu-btn" @click="showMenu = !showMenu">
      <div class="current-user-avatar">{{ getInitial(currentUsername) }}</div>
      <span class="current-user-name">{{ currentUsername }}</span>
    </button>
    <div v-if="showMenu" class="user-dropdown">
      <div class="dropdown-header">
        <div class="dropdown-avatar">{{ getInitial(currentUsername) }}</div>
        <div class="dropdown-info">
          <div class="dropdown-name">{{ currentUsername }}</div>
          <div class="dropdown-username">@{{ authStore.user?.username }}</div>
        </div>
      </div>
      <div class="dropdown-divider"></div>
      <button class="dropdown-item" @click="showMenu = false; $emit('logout')">退出登录</button>
    </div>
  </div>
</template>

<style scoped>
.user-menu-wrapper { position: relative; }

.user-menu-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 4px 8px; background: transparent; border: none; border-radius: 20px; cursor: pointer;
}
.user-menu-btn:hover { background: #f1f3f4; }

.current-user-avatar {
  width: 32px; height: 32px; border-radius: 50%; background: #1a73e8; color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 500;
}

.current-user-name {
  font-size: 14px; color: #202124;
  max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.user-dropdown {
  position: absolute; top: 100%; right: 0; margin-top: 8px;
  background: #fff; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.2);
  min-width: 200px; z-index: 100; overflow: hidden;
}

.dropdown-header { display: flex; align-items: center; gap: 12px; padding: 16px; }

.dropdown-avatar {
  width: 40px; height: 40px; border-radius: 50%; background: #1a73e8; color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 500;
}

.dropdown-info { flex: 1; }
.dropdown-name { font-size: 14px; font-weight: 500; color: #202124; }
.dropdown-username { font-size: 12px; color: #5f6368; }

.dropdown-divider { height: 1px; background: #e0e0e0; }

.dropdown-item {
  width: 100%; padding: 12px 16px; background: transparent; border: none;
  text-align: left; font-size: 14px; color: #202124; cursor: pointer;
}
.dropdown-item:hover { background: #f1f3f4; }
</style>
