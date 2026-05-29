<script setup>
import { watch } from 'vue'
import { useAuthStore } from './stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

// 监听登出，跳转首页
watch(() => authStore.isLoggedIn, (val) => {
  if (!val) router.push('/')
})
</script>

<template>
  <router-view />
</template>
