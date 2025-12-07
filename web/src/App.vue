<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useWebSocket } from './composables/useWebSocket'
import { useAuth } from './composables/useAuth'
import AuthModal from './components/AuthModal.vue'

const {
  isConnected,
  messages,
  connect,
  disconnect,
  sendJson
} = useWebSocket()

const { user, token, isLoggedIn, logout } = useAuth()

// 登录弹窗
const showAuthModal = ref(false)
const showUserMenu = ref(false)

// 文档信息
const docTitle = ref('未命名文档')
const docId = ref('doc-001')
const serverUrl = ref('ws://localhost:8080/editor/')
const content = ref('')
const isEditingTitle = ref(false)

// 监听登录状态，自动加载文档
watch(isLoggedIn, async (newVal) => {
  if (newVal) {
    // 如果已有 docId，尝试连接
    if (docId.value && docId.value !== 'doc-001') {
      handleConnect()
    } else {
      // 否则加载最近的文档
      try {
        const res = await fetch('http://localhost:8080/api/doc/list', {
          headers: { 'Authorization': 'Bearer ' + token.value }
        })
        const data = await res.json()
        if (data.code === 200 && data.data && data.data.length > 0) {
          openDoc(data.data[0])
        }
      } catch (e) {
        console.error(e)
      }
    }
  }
})

// 当前用户名（登录后使用真实用户名）
const currentUsername = computed(() => {
  return isLoggedIn.value ? (user.value?.nickname || user.value?.username) : '游客'
})

// 模拟在线用户
const onlineUsers = ref([])

// 连接处理
function handleConnect() {
  const wsUrl = serverUrl.value + docId.value + '?token=' + token.value
  connect(wsUrl)
  onlineUsers.value = [{ name: currentUsername.value, color: '#1a73e8' }]
}

function handleDisconnect() {
  disconnect()
  onlineUsers.value = []
}

// 用户菜单
function handleLogout() {
  logout()
  showUserMenu.value = false
  if (isConnected.value) {
    disconnect()
  }
}

// 编辑内容
let isReceiving = false
function handleInput() {
  if (!isReceiving && isConnected.value) {
    sendJson('EDIT', currentUsername.value, content.value)
  }
}

// 监听消息
watch(() => messages.value, (newMessages) => {
  if (newMessages.length === 0) return
  const lastMsg = newMessages[newMessages.length - 1]
  if (lastMsg.type === 'received' && lastMsg.raw) {
    const data = typeof lastMsg.raw === 'object' ? lastMsg.raw : null
    if (data && (data.type === 'SYNC' || data.type === 'EDIT')) {
      isReceiving = true
      content.value = data.data || ''
      // 添加协作者
      if (data.sender && data.sender !== 'server' && !onlineUsers.value.find(u => u.name === data.sender)) {
        const colors = ['#ea4335', '#34a853', '#fbbc04', '#9c27b0', '#ff5722']
        onlineUsers.value.push({
          name: data.sender,
          color: colors[onlineUsers.value.length % colors.length]
        })
      }
      setTimeout(() => { isReceiving = false }, 50)
    }
  }
}, { deep: true })

// 获取用户首字母
function getInitial(name) {
  return name.charAt(0).toUpperCase()
}

// 列表和创建弹窗
const showCreateModal = ref(false)
const newDocTitle = ref('')
const showListModal = ref(false)
const docList = ref([])
const showDeleteModal = ref(false)
const docToDelete = ref(null)
const showJoinModal = ref(false)
const inviteCode = ref('')
const showShareModal = ref(false)
const currentInviteCode = ref('')

// 通用消息弹窗
const showMessageModal = ref(false)
const messageContent = ref('')

function showMessage(msg) {
  messageContent.value = msg
  showMessageModal.value = true
}

async function openCreateModal() {
  if (!isLoggedIn.value) {
    showMessage('请先登录')
    showAuthModal.value = true
    return
  }
  newDocTitle.value = '未命名文档'
  showCreateModal.value = true
}

async function handleCreateDoc() {
  if (!newDocTitle.value) return

  try {
    const res = await fetch('http://localhost:8080/api/doc', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token.value
      },
      body: JSON.stringify({ title: newDocTitle.value })
    })
    const data = await res.json()
    if (data.code === 200) {
      docId.value = data.data.docId
      docTitle.value = data.data.title
      content.value = ''
      
      // Update URL
      const url = new URL(window.location)
      url.searchParams.set('docId', docId.value)
      window.history.pushState({}, '', url)
      
      // Reconnect
      if (isConnected.value) disconnect()
      handleConnect()
      
      showCreateModal.value = false
    } else {
      showMessage(data.message || '创建失败')
    }
  } catch (e) {
    console.error(e)
    showMessage('创建失败')
  }
}

async function fetchDocList() {
  if (!isLoggedIn.value) {
    showMessage('请先登录')
    showAuthModal.value = true
    return
  }
  try {
    const res = await fetch('http://localhost:8080/api/doc/list', {
      headers: { 'Authorization': 'Bearer ' + token.value }
    })
    const data = await res.json()
    if (data.code === 200) {
      docList.value = data.data
      showListModal.value = true
    } else {
      showMessage(data.message)
    }
  } catch (e) {
    console.error(e)
    showMessage('获取列表失败')
  }
}

function deleteDoc(doc) {
  docToDelete.value = doc
  showDeleteModal.value = true
}

async function handleJoinDoc() {
  if (!inviteCode.value) return
  try {
    const res = await fetch('http://localhost:8080/api/doc/join', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token.value
      },
      body: JSON.stringify({ code: inviteCode.value })
    })
    const data = await res.json()
    if (data.code === 200) {
      openDoc(data.data)
      showJoinModal.value = false
      inviteCode.value = ''
    } else {
      showMessage(data.message || '加入失败')
    }
  } catch (e) {
    console.error(e)
    showMessage('加入失败')
  }
}

async function handleShare() {
  if (!docId.value) return
  try {
    const res = await fetch(`http://localhost:8080/api/doc/${docId.value}/invite`, {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + token.value }
    })
    const data = await res.json()
    if (data.code === 200) {
      currentInviteCode.value = data.data
      showShareModal.value = true
    } else {
      showMessage(data.message || '生成邀请码失败')
    }
  } catch (e) {
    console.error(e)
    showMessage('生成邀请码失败')
  }
}

async function confirmDelete() {
  if (!docToDelete.value) return
  
  const doc = docToDelete.value
  try {
    const res = await fetch(`http://localhost:8080/api/doc/${doc.docId}`, {
      method: 'DELETE',
      headers: { 'Authorization': 'Bearer ' + token.value }
    })
    const data = await res.json()
    if (data.code === 200) {
      // Remove from list
      docList.value = docList.value.filter(d => d.docId !== doc.docId)
      // If current doc is deleted, clear it
      if (docId.value === doc.docId) {
          docId.value = ''
          docTitle.value = '未命名文档'
          content.value = ''
          disconnect()
      }
      showDeleteModal.value = false
      docToDelete.value = null
    } else {
      showMessage(data.message || '删除失败')
    }
  } catch (e) {
    console.error(e)
    showMessage('删除失败')
  }
}

function openDoc(doc) {
  docId.value = doc.docId
  docTitle.value = doc.title
  // 切换文档时先清空内容，等待WebSocket或API加载
  content.value = '' 
  
  const url = new URL(window.location)
  url.searchParams.set('docId', docId.value)
  window.history.pushState({}, '', url)
  
  if (isConnected.value) disconnect()
  handleConnect()
  
  showListModal.value = false
}

onMounted(async () => {
  const params = new URLSearchParams(window.location.search)
  const id = params.get('docId')
  if (id) {
    docId.value = id
    // Fetch doc info
    try {
        const res = await fetch(`http://localhost:8080/api/doc/${id}`, {
             headers: { 'Authorization': 'Bearer ' + token.value }
        })
        const data = await res.json()
        if (data.code === 200) {
            docTitle.value = data.data.title
            content.value = data.data.content || ''
        }
    } catch(e) {
        console.error(e)
    }
    // Connect if we have a docId
    handleConnect()
  } else if (isLoggedIn.value) {
    // Auto load latest doc
    try {
      const res = await fetch('http://localhost:8080/api/doc/list', {
        headers: { 'Authorization': 'Bearer ' + token.value }
      })
      const data = await res.json()
      if (data.code === 200 && data.data && data.data.length > 0) {
        openDoc(data.data[0])
      }
    } catch (e) {
      console.error(e)
    }
  }
})
</script>

<template>
  <div class="app">
    <!-- 顶部导航栏 -->
    <header class="navbar">
      <div class="navbar-left">
        <div class="logo">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm4 18H6V4h7v5h5v11z"/>
          </svg>
        </div>
        <div v-if="isLoggedIn" style="display: flex; gap: 8px; margin-right: 15px;">
          <button class="btn-primary" @click="openCreateModal" style="padding: 6px 12px; font-size: 14px;">新建</button>
          <button class="btn-secondary" @click="fetchDocList" style="padding: 6px 12px; font-size: 14px;">我的文档</button>
          <button class="btn-secondary" @click="showJoinModal = true" style="padding: 6px 12px; font-size: 14px;">加入协作</button>
        </div>
        <div class="doc-info">
          <div class="doc-title" v-if="!isEditingTitle" @click="isEditingTitle = true">
            {{ docTitle }}
          </div>
          <input
            v-else
            v-model="docTitle"
            class="doc-title-input"
            @blur="isEditingTitle = false"
            @keyup.enter="isEditingTitle = false"
            autofocus
          >
          <div class="doc-status">
            <span v-if="isConnected" class="status-saved">已同步</span>
            <span v-else class="status-offline">离线</span>
          </div>
        </div>
      </div>

      <div class="navbar-right">
        <!-- 分享按钮 -->
        <button v-if="isConnected" class="btn-primary" @click="handleShare" style="margin-right: 10px; padding: 6px 16px;">
          分享
        </button>

        <!-- 在线用户 -->
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

        <!-- 登录/用户信息 -->
        <div v-if="!isLoggedIn" class="auth-buttons">
          <button class="btn-login" @click="showAuthModal = true">登录</button>
        </div>
        <div v-else class="user-menu-wrapper">
          <button class="user-menu-btn" @click="showUserMenu = !showUserMenu">
            <div class="current-user-avatar">{{ getInitial(currentUsername) }}</div>
            <span class="current-user-name">{{ currentUsername }}</span>
          </button>
          <div v-if="showUserMenu" class="user-dropdown">
            <div class="dropdown-header">
              <div class="dropdown-avatar">{{ getInitial(currentUsername) }}</div>
              <div class="dropdown-info">
                <div class="dropdown-name">{{ currentUsername }}</div>
                <div class="dropdown-username">@{{ user?.username }}</div>
              </div>
            </div>
            <div class="dropdown-divider"></div>
            <button class="dropdown-item" @click="handleLogout">退出登录</button>
          </div>
        </div>
      </div>
    </header>

    <!-- 登录弹窗 -->
    <AuthModal :visible="showAuthModal" @close="showAuthModal = false" />

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-group">
        <button class="tool-btn" title="撤销">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
            <path d="M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z"/>
          </svg>
        </button>
        <button class="tool-btn" title="重做">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
            <path d="M18.4 10.6C16.55 8.99 14.15 8 11.5 8c-4.65 0-8.58 3.03-9.96 7.22L3.9 16c1.05-3.19 4.05-5.5 7.6-5.5 1.95 0 3.73.72 5.12 1.88L13 16h9V7l-3.6 3.6z"/>
          </svg>
        </button>
      </div>
      <div class="toolbar-divider"></div>
      <div class="toolbar-group">
        <button class="tool-btn" title="加粗">B</button>
        <button class="tool-btn" title="斜体"><em>I</em></button>
        <button class="tool-btn" title="下划线"><u>U</u></button>
      </div>
      <div class="toolbar-divider"></div>
      <div class="toolbar-group">
        <select class="tool-select">
          <option>正文</option>
          <option>标题1</option>
          <option>标题2</option>
          <option>标题3</option>
        </select>
      </div>
    </div>

    <!-- 主体 -->
    <main class="main-content">
      <!-- 编辑区域 -->
      <div class="editor-container">
        <div class="paper">
          <textarea
            v-model="content"
            class="editor"
            :placeholder="isConnected ? '开始输入内容...' : '请先登录或选择文档'"
            :disabled="!isConnected"
            @input="handleInput"
          ></textarea>
        </div>
      </div>
    </main>

    <!-- 创建文档弹窗 -->
    <div v-if="showCreateModal" class="modal-overlay" @click="showCreateModal = false">
      <div class="modal-content" @click.stop>
        <h3>新建文档</h3>
        <div class="form-item">
          <label>文档标题</label>
          <input v-model="newDocTitle" placeholder="请输入文档标题" @keyup.enter="handleCreateDoc" autofocus>
        </div>
        <div class="modal-actions">
          <button class="btn-secondary" @click="showCreateModal = false">取消</button>
          <button class="btn-primary" @click="handleCreateDoc">创建</button>
        </div>
      </div>
    </div>

    <!-- 加入协作弹窗 -->
    <div v-if="showJoinModal" class="modal-overlay" @click="showJoinModal = false">
      <div class="modal-content" @click.stop>
        <h3>加入协作</h3>
        <div class="form-item">
          <label>邀请码</label>
          <input v-model="inviteCode" placeholder="请输入8位邀请码" @keyup.enter="handleJoinDoc" autofocus>
        </div>
        <div class="modal-actions">
          <button class="btn-secondary" @click="showJoinModal = false">取消</button>
          <button class="btn-primary" @click="handleJoinDoc">加入</button>
        </div>
      </div>
    </div>

    <!-- 分享弹窗 -->
    <div v-if="showShareModal" class="modal-overlay" @click="showShareModal = false">
      <div class="modal-content" @click.stop>
        <h3>邀请协作</h3>
        <p style="color: #5f6368; margin-bottom: 10px;">将此邀请码发送给好友，对方即可加入协作：</p>
        <div class="invite-code-box" style="background: #f1f3f4; padding: 15px; border-radius: 4px; text-align: center; font-size: 24px; letter-spacing: 2px; font-family: monospace; margin-bottom: 20px; user-select: all;">
          {{ currentInviteCode }}
        </div>
        <div class="modal-actions">
          <button class="btn-primary" @click="showShareModal = false">关闭</button>
        </div>
      </div>
    </div>

    <!-- 文档列表弹窗 -->
    <div v-if="showListModal" class="modal-overlay" @click="showListModal = false">
      <div class="modal-content list-modal" @click.stop>
        <h3>我的文档</h3>
        <div class="doc-list">
          <div v-if="docList.length === 0" class="empty-tip">暂无文档</div>
          <div v-else v-for="doc in docList" :key="doc.docId" class="doc-item" @click="openDoc(doc)">
            <div class="doc-item-icon">
              <svg viewBox="0 0 24 24" width="24" height="24" fill="#1a73e8">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
              </svg>
            </div>
            <div class="doc-item-info">
              <div class="doc-item-title">{{ doc.title }}</div>
              <div class="doc-item-time">更新时间: {{ new Date(doc.updateTime).toLocaleString() }}</div>
            </div>
            <button class="delete-btn" @click.stop="deleteDoc(doc)" title="删除">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="#5f6368">
                <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
              </svg>
            </button>
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn-secondary" @click="showListModal = false">关闭</button>
        </div>
      </div>
    </div>

    <!-- 删除确认弹窗 -->
    <div v-if="showDeleteModal" class="modal-overlay" @click="showDeleteModal = false">
      <div class="modal-content" @click.stop style="max-width: 400px;">
        <h3>删除文档</h3>
        <p style="margin: 20px 0; color: #5f6368;">确定要删除文档 "{{ docToDelete?.title }}" 吗？此操作无法撤销。</p>
        <div class="modal-actions">
          <button class="btn-secondary" @click="showDeleteModal = false">取消</button>
          <button class="btn-danger" @click="confirmDelete">删除</button>
        </div>
      </div>
    </div>

    <!-- 消息提示弹窗 -->
    <div v-if="showMessageModal" class="modal-overlay" @click="showMessageModal = false">
      <div class="modal-content" @click.stop style="max-width: 400px;">
        <h3>提示</h3>
        <p style="margin: 20px 0; color: #5f6368;">{{ messageContent }}</p>
        <div class="modal-actions">
          <button class="btn-primary" @click="showMessageModal = false">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.app {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f5f5f5;
}

/* 导航栏 */
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

.logo {
  color: #1a73e8;
  display: flex;
}

.doc-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.doc-title {
  font-size: 18px;
  font-weight: 500;
  color: #202124;
  cursor: pointer;
  padding: 2px 8px;
  margin: -2px -8px;
  border-radius: 4px;
}

.doc-title:hover {
  background: #f1f3f4;
}

.doc-title-input {
  font-size: 18px;
  font-weight: 500;
  border: none;
  outline: none;
  padding: 2px 8px;
  margin: -2px -8px;
  border-radius: 4px;
  background: #e8f0fe;
}

.doc-status {
  font-size: 12px;
  color: #5f6368;
}

.status-saved {
  color: #34a853;
}

.status-offline {
  color: #ea4335;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.online-users {
  display: flex;
  align-items: center;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  margin-left: -8px;
  border: 2px solid #fff;
}

.user-avatar:first-child {
  margin-left: 0;
}

.user-count {
  margin-left: 8px;
  font-size: 13px;
  color: #5f6368;
}

/* 登录按钮 */
.auth-buttons {
  display: flex;
  gap: 8px;
}

.btn-login {
  padding: 8px 24px;
  background: #1a73e8;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.btn-login:hover {
  background: #1557b0;
}

/* 用户菜单 */
.user-menu-wrapper {
  position: relative;
}

.user-menu-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  background: transparent;
  border: none;
  border-radius: 20px;
  cursor: pointer;
}

.user-menu-btn:hover {
  background: #f1f3f4;
}

.current-user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #1a73e8;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
}

.current-user-name {
  font-size: 14px;
  color: #202124;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.2);
  min-width: 200px;
  z-index: 100;
  overflow: hidden;
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
}

.dropdown-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #1a73e8;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 500;
}

.dropdown-info {
  flex: 1;
}

.dropdown-name {
  font-size: 14px;
  font-weight: 500;
  color: #202124;
}

.dropdown-username {
  font-size: 12px;
  color: #5f6368;
}

.dropdown-divider {
  height: 1px;
  background: #e0e0e0;
}

.dropdown-item {
  width: 100%;
  padding: 12px 16px;
  background: transparent;
  border: none;
  text-align: left;
  font-size: 14px;
  color: #202124;
  cursor: pointer;
}

.dropdown-item:hover {
  background: #f1f3f4;
}

.btn-icon {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5f6368;
}

.btn-icon:hover {
  background: #f1f3f4;
}

/* 工具栏 */
.toolbar {
  height: 40px;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 4px;
}

.doc-title-input {
  min-width: 200px;
}

.btn-secondary {
  padding: 8px 24px;
  background: #fff;
  color: #5f6368;
  border: 1px solid #dadce0;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
}

.btn-secondary:hover {
  background: #f1f3f4;
  color: #202124;
}

/* 弹窗样式 */
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

.modal-content {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  width: 400px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
}

.modal-content h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #202124;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.modal-actions button {
  width: auto;
  padding: 8px 24px;
}

/* 列表弹窗 */
.list-modal {
  width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.doc-list {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  margin-bottom: 16px;
}

.empty-tip {
  padding: 32px;
  text-align: center;
  color: #5f6368;
}

.doc-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f1f3f4;
  cursor: pointer;
  transition: background 0.2s;
}

.doc-item:last-child {
  border-bottom: none;
}

.doc-item:hover {
  background: #f8f9fa;
}

.doc-item-icon {
  margin-right: 16px;
  display: flex;
  align-items: center;
}

.doc-item-info {
  flex: 1;
}

.doc-item-title {
  font-size: 14px;
  font-weight: 500;
  color: #202124;
  margin-bottom: 4px;
}

.doc-item-time {
  font-size: 12px;
  color: #5f6368;
}

.delete-btn {
  background: none;
  border: none;
  padding: 8px;
  border-radius: 50%;
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.doc-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: #fce8e6;
}

.delete-btn:hover svg {
  fill: #ea4335;
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 2px;
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  background: #e0e0e0;
  margin: 0 8px;
}

.tool-btn {
  min-width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #444;
  font-size: 14px;
  font-weight: 600;
}

.tool-btn:hover {
  background: #f1f3f4;
}

.tool-select {
  height: 32px;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #444;
  cursor: pointer;
  padding: 0 8px;
  border-radius: 4px;
}

.tool-select:hover {
  background: #f1f3f4;
}

/* 主体 */
.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 调试面板 */
.debug-panel {
  width: 0;
  background: #fff;
  border-right: 1px solid #e0e0e0;
  overflow: hidden;
  transition: width 0.3s ease;
  display: flex;
  flex-direction: column;
}

.debug-panel.visible {
  width: 300px;
}

.debug-section {
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.debug-section h3 {
  font-size: 14px;
  font-weight: 500;
  color: #202124;
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.badge {
  background: #e8f0fe;
  color: #1a73e8;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.form-item {
  margin-bottom: 12px;
}

.form-item label {
  display: block;
  font-size: 12px;
  color: #5f6368;
  margin-bottom: 4px;
}

.form-item input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dadce0;
  border-radius: 4px;
  font-size: 14px;
}

.form-item input:focus {
  outline: none;
  border-color: #1a73e8;
}

.form-item input:disabled {
  background: #f1f3f4;
}

.btn-primary {
  width: 100%;
  padding: 10px;
  background: #1a73e8;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.btn-primary:hover {
  background: #1557b0;
}

.btn-danger {
  width: 100%;
  padding: 10px;
  background: #ea4335;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.btn-danger:hover {
  background: #c5221f;
}

.log-list {
  max-height: 300px;
  overflow-y: auto;
}

.log-item {
  font-size: 12px;
  padding: 6px 8px;
  border-radius: 4px;
  margin-bottom: 4px;
  display: flex;
  gap: 8px;
  background: #f8f9fa;
}

.log-item.sent {
  background: #e8f0fe;
}

.log-item.received {
  background: #e6f4ea;
}

.log-item.system {
  background: #fef7e0;
}

.log-type {
  font-weight: 500;
  flex-shrink: 0;
}

.log-content {
  color: #5f6368;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 编辑器容器 */
.editor-container {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 32px;
  overflow: auto;
}

.paper {
  width: 100%;
  max-width: 816px;
  min-height: calc(100vh - 200px);
  background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24);
  border-radius: 2px;
}

.editor {
  width: 100%;
  height: 100%;
  min-height: calc(100vh - 200px);
  padding: 72px 96px;
  border: none;
  outline: none;
  font-family: 'Arial', sans-serif;
  font-size: 16px;
  line-height: 1.8;
  color: #202124;
  resize: none;
}

.editor::placeholder {
  color: #9aa0a6;
}

.editor:disabled {
  background: #fafafa;
  cursor: not-allowed;
}

/* 响应式 */
@media (max-width: 768px) {
  .editor {
    padding: 32px;
  }

  .editor-container {
    padding: 16px;
  }

  .debug-panel.visible {
    position: absolute;
    right: 0;
    top: 96px;
    bottom: 0;
    z-index: 100;
    box-shadow: -2px 0 8px rgba(0,0,0,0.15);
  }
}
</style>
