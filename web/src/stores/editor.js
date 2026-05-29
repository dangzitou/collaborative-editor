import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useEditorStore = defineStore('editor', () => {
  const onlineUsers = ref([])
  const isConnected = ref(false)
  const pingDelay = ref(null)

  function addUser(user) {
    if (!onlineUsers.value.find(u => u.name === user.name)) {
      onlineUsers.value.push(user)
    }
  }

  function removeUser(username) {
    onlineUsers.value = onlineUsers.value.filter(u => u.name !== username)
  }

  function setUserList(users) {
    onlineUsers.value = users.map((name, i) => ({
      name,
      color: getUserColor(i)
    }))
  }

  function clearState() {
    onlineUsers.value = []
    isConnected.value = false
    pingDelay.value = null
  }

  function getUserColor(index) {
    const colors = ['#1a73e8', '#e8710a', '#0d652d', '#c5221f', '#8430ce', '#185abc', '#7b5800']
    return colors[index % colors.length]
  }

  return {
    onlineUsers, isConnected, pingDelay,
    addUser, removeUser, setUserList, clearState
  }
})
