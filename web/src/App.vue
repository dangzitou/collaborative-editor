<script setup>
import { ref } from 'vue'
import { useWebSocket } from './composables/useWebSocket'

const {
  isConnected,
  messages,
  connect,
  disconnect,
  sendJson
} = useWebSocket()

// 文档信息
const docTitle = ref('未命名文档')
const docId = ref('doc-001')
const serverUrl = ref('ws://localhost:8080/editor/')
const username = ref('用户' + Math.floor(Math.random() * 1000))
const content = ref('')
const isEditingTitle = ref(false)
const showDebugPanel = ref(false)

// 模拟在线用户
const onlineUsers = ref([])

// 连接处理
function handleConnect() {
  connect(serverUrl.value + docId.value)
  onlineUsers.value = [{ name: username.value, color: '#1a73e8' }]
}

function handleDisconnect() {
  disconnect()
  onlineUsers.value = []
}

// 编辑内容
let isReceiving = false
function handleInput() {
  if (!isReceiving && isConnected.value) {
    sendJson('EDIT', username.value, content.value)
  }
}

// 监听消息
import { watch } from 'vue'
watch(() => messages.value, (newMessages) => {
  if (newMessages.length === 0) return
  const lastMsg = newMessages[newMessages.length - 1]
  if (lastMsg.type === 'received' && lastMsg.raw) {
    const data = typeof lastMsg.raw === 'object' ? lastMsg.raw : null
    if (data && (data.type === 'SYNC' || data.type === 'EDIT')) {
      isReceiving = true
      content.value = data.data || ''
      // 添加协作者
      if (data.sender && !onlineUsers.value.find(u => u.name === data.sender)) {
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

        <!-- 调试按钮 -->
        <button class="btn-icon" @click="showDebugPanel = !showDebugPanel" title="调试面板">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 6c1.4 0 2.8 1.1 2.8 2.5V11c.6.3 1 .8 1 1.5v3c0 .8-.7 1.5-1.5 1.5h-4.5c-.8 0-1.5-.7-1.5-1.5v-3c0-.7.4-1.2 1-1.5V9.5C9.2 8.1 10.6 7 12 7zm0 1.2c-.8 0-1.5.5-1.5 1.3v1.5h3V9.5c0-.8-.7-1.3-1.5-1.3z"/>
          </svg>
        </button>
      </div>
    </header>

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
      <!-- 调试面板 -->
      <aside class="debug-panel" :class="{ visible: showDebugPanel }">
        <div class="debug-section">
          <h3>连接设置</h3>
          <div class="form-item">
            <label>服务器</label>
            <input v-model="serverUrl" :disabled="isConnected">
          </div>
          <div class="form-item">
            <label>文档ID</label>
            <input v-model="docId" :disabled="isConnected">
          </div>
          <div class="form-item">
            <label>用户名</label>
            <input v-model="username" :disabled="isConnected">
          </div>
          <button
            v-if="!isConnected"
            class="btn-primary"
            @click="handleConnect"
          >
            连接
          </button>
          <button
            v-else
            class="btn-danger"
            @click="handleDisconnect"
          >
            断开
          </button>
        </div>

        <div class="debug-section">
          <h3>消息日志 <span class="badge">{{ messages.length }}</span></h3>
          <div class="log-list">
            <div
              v-for="(msg, i) in messages.slice(-20).reverse()"
              :key="i"
              class="log-item"
              :class="msg.type"
            >
              <span class="log-type">{{ msg.type === 'sent' ? '发' : msg.type === 'received' ? '收' : '系' }}</span>
              <span class="log-content">{{ msg.content.substring(0, 50) }}{{ msg.content.length > 50 ? '...' : '' }}</span>
            </div>
          </div>
        </div>
      </aside>

      <!-- 编辑区域 -->
      <div class="editor-container">
        <div class="paper">
          <textarea
            v-model="content"
            class="editor"
            :placeholder="isConnected ? '开始输入内容...' : '请先在右侧调试面板连接服务器'"
            :disabled="!isConnected"
            @input="handleInput"
          ></textarea>
        </div>
      </div>
    </main>
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
