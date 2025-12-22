// 防止 docId 被清空时所有状态残留
watch(docId, (newVal) => {
  if (!newVal) {
    docTitle.value = '未命名文档'
    content.value = ''
    onlineUsers.value = []
    remoteCursors.value = {}
    isEditingTitle.value = false
  }
})
<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useWebSocket } from './composables/useWebSocket'
import { useAuth } from './composables/useAuth'
import AuthModal from './components/AuthModal.vue'

const {
  isConnected,
  messages,
  connect,
  disconnect,
  sendJson,
  pingDelay,
  clearMessages
} = useWebSocket()

const { user, token, isLoggedIn, logout } = useAuth()

// 登录弹窗
const showAuthModal = ref(false)
const showUserMenu = ref(false)

// 文档信息
const docTitle = ref('未命名文档')
const docId = ref('doc-001')
const previousDoc = ref(null)
const pendingDoc = ref(null)
const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
const host = window.location.host
const serverUrl = ref(`${protocol}//${host}/editor/`)
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
        const res = await fetch('/api/doc/list', {
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
}, { immediate: true })

// 当前用户名（登录后使用真实用户名）
const currentUsername = computed(() => {
  return isLoggedIn.value ? (user.value?.nickname || user.value?.username) : '游客'
})

// 模拟在线用户
const onlineUsers = ref([])

// 远程光标
const remoteCursors = ref({})
const editorRef = ref(null)
const foreColorInput = ref(null)
const hiliteColorInput = ref(null)

// 当前样式状态
const currentStyle = ref({
  bold: false,
  italic: false,
  underline: false,
  strikeThrough: false,
  justifyLeft: false,
  justifyCenter: false,
  justifyRight: false,
  fontName: 'Arial',
  fontSize: '3',
  foreColor: '#000000',
  hiliteColor: 'transparent'
})

// 辅助函数：RGB转Hex
function rgbToHex(rgb) {
  if (!rgb || rgb === 'transparent' || rgb === 'rgba(0, 0, 0, 0)') return ''
  if (rgb.startsWith('#')) return rgb
  
  const sep = rgb.indexOf(",") > -1 ? "," : " ";
  const parts = rgb.substr(4).split(")")[0].split(sep);
  
  let r = (+parts[0]).toString(16),
      g = (+parts[1]).toString(16),
      b = (+parts[2]).toString(16);
  
  if (r.length == 1) r = "0" + r;
  if (g.length == 1) g = "0" + g;
  if (b.length == 1) b = "0" + b;
  
  return "#" + r + g + b;
}

// 更新工具栏状态
function updateToolbarState() {
  if (!document.queryCommandState) return
  
  currentStyle.value.bold = document.queryCommandState('bold')
  currentStyle.value.italic = document.queryCommandState('italic')
  currentStyle.value.underline = document.queryCommandState('underline')
  currentStyle.value.strikeThrough = document.queryCommandState('strikeThrough')
  currentStyle.value.justifyLeft = document.queryCommandState('justifyLeft')
  currentStyle.value.justifyCenter = document.queryCommandState('justifyCenter')
  currentStyle.value.justifyRight = document.queryCommandState('justifyRight')
  
  const fontName = document.queryCommandValue('fontName')
  if (fontName) {
      currentStyle.value.fontName = fontName.replace(/['"]/g, '')
  }
  currentStyle.value.fontSize = document.queryCommandValue('fontSize') || '3'
  
  const foreColor = document.queryCommandValue('foreColor')
  currentStyle.value.foreColor = rgbToHex(foreColor) || '#000000'
  
  const hiliteColor = document.queryCommandValue('hiliteColor') || document.queryCommandValue('backColor')
  currentStyle.value.hiliteColor = rgbToHex(hiliteColor) || 'transparent'
}

let savedRange = null
function saveSelection() {
  const sel = window.getSelection()
  if (sel.rangeCount > 0 && editorRef.value) {
    const range = sel.getRangeAt(0)
    // 确保选区在编辑器内部
    if (editorRef.value.contains(range.commonAncestorContainer)) {
      savedRange = range.cloneRange()
    }
  }
}

function restoreSelection() {
  if (savedRange) {
    const sel = window.getSelection()
    sel.removeAllRanges()
    sel.addRange(savedRange)
  }
}

function openColorPicker(type) {
  saveSelection()
  if (type === 'foreColor' && foreColorInput.value) {
    foreColorInput.value.click()
  } else if (type === 'hiliteColor' && hiliteColorInput.value) {
    hiliteColorInput.value.click()
  }
}

function onColorInput(command, value) {
  restoreSelection()
  // 仅预览，不强制聚焦，避免干扰颜色选择器
  document.execCommand(command, false, value)
  // 关键：execCommand 可能会改变 DOM 结构（例如拆分 span），导致旧的 range 失效
  // execCommand 执行后通常会自动更新选区到新结构上，所以我们需要重新保存这个有效的选区
  saveSelection()
  updateToolbarState()
}

function onColorChange(command, value) {
  restoreSelection()
  // 最终确认，执行命令并触发同步
  execCommand(command, value)
}

function execCommand(command, value = null) {
  document.execCommand(command, false, value)
  if (editorRef.value) {
    editorRef.value.focus()
    handleInput()
    updateToolbarState()
  }
}

// 远程光标相关函数已移除，暂不支持 HTML 光标同步
function handleScroll() {}

// 监听内容变化
watch(content, (newVal) => {
  if (editorRef.value && editorRef.value.innerHTML !== newVal) {
    editorRef.value.innerHTML = newVal
  }
  // 内容变化后，远程光标位置可能需要更新（虽然这里没有重新计算，但至少不会报错）
  nextTick(() => {
      Object.keys(remoteCursors.value).forEach(updateCursorPosition)
  })
})

// 连接处理
function handleConnect() {
  const wsUrl = serverUrl.value + docId.value + '?token=' + token.value + '&username=' + encodeURIComponent(currentUsername.value)
  connect(wsUrl, {
    onOpen: () => {
      try {
        if (pendingDoc.value && pendingDoc.value.docId === docId.value) {
          docTitle.value = pendingDoc.value.title || '未命名文档'
          pendingDoc.value = null
        }
      } catch (e) { console.warn(e) }
    },
    onClose: (event) => {
      // 优先 reason 判断
      if (event.reason === '无权限操作此文档') {
        showMessage('无权限操作此文档', () => {
          // 如果存在上一个文档，恢复到上一个文档并刷新标题与内容
          if (previousDoc.value && previousDoc.value.docId) {
            // 先断开当前连接（若有）
            if (isConnected.value) disconnect()

            docId.value = previousDoc.value.docId
            docTitle.value = previousDoc.value.title || '未命名文档'
            content.value = previousDoc.value.content || ''

            // 更新 URL
            const url = new URL(window.location)
            url.searchParams.set('docId', docId.value)
            window.history.replaceState({}, '', url)

            // 重新连接
            handleConnect()
            // 清除 previousDoc，避免循环
            previousDoc.value = null
            return
          }

          // 如果没有 previousDoc，尝试从 sessionStorage 恢复上次成功打开的文档
          try {
            const raw = sessionStorage.getItem('lastDoc')
            if (raw) {
              const last = JSON.parse(raw)
              if (last && last.docId) {
                // 恢复到 lastDoc
                docId.value = last.docId
                docTitle.value = last.title || '未命名文档'
                content.value = last.content || ''
                const url = new URL(window.location)
                url.searchParams.set('docId', docId.value)
                window.history.replaceState({}, '', url)
                // 重新连接
                if (isConnected.value) disconnect()
                handleConnect()
                return
              }
            }
          } catch (e) {
            console.warn('读取 sessionStorage.lastDoc 失败', e)
          }

          // 否则清空当前文档并移除 URL，停留首页样式
          docId.value = ''
          docTitle.value = '未命名文档'
          content.value = ''
          const url = new URL(window.location)
          url.searchParams.delete('docId')
          window.history.replaceState({}, '', url)
        })
        return
      }
      if (event.reason === 'Invalid Token') {
        showMessage('登录过期请重新登录', () => {
          logout()
          showAuthModal.value = true
        })
        return
      }
      // 兼容旧逻辑
      if (event.code === 1008) {
        logout()
        setTimeout(() => {
          alert('登录已过期，请重新登录')
          showAuthModal.value = true
        }, 100)
        return
      }
      if (event.code === 1003 || event.reason === "Document not found") {
        showMessage('文档不存在或已被删除', () => {
          window.location.href = '/'
        })
      }
    }
  })
  remoteCursors.value = {}
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
  if (!editorRef.value) return
  const newContent = editorRef.value.innerHTML
  
  // 只有当内容真正改变时才发送
  if (content.value !== newContent) {
      content.value = newContent
      if (!isReceiving && isConnected.value) {
        sendJson('EDIT', currentUsername.value, content.value)
      }
  }
}

// 远程光标相关函数
// 获取节点在父节点中的索引
function getNodeIndex(node) {
  let i = 0;
  while( (node = node.previousSibling) ) {
    i++;
  }
  return i;
}

// 获取光标位置（路径 + 偏移）
function getCursorLocation() {
  const selection = window.getSelection();
  if (!selection.rangeCount) return null;
  const range = selection.getRangeAt(0);
  const root = editorRef.value;
  
  if (!root || !root.contains(range.startContainer)) return null;

  let node = range.startContainer;
  const path = [];
  
  while (node !== root) {
    path.unshift(getNodeIndex(node));
    node = node.parentNode;
  }
  
  return {
    path: path,
    offset: range.startOffset
  };
}

// 根据路径获取节点
function getNodeByPath(root, path) {
  let node = root;
  for (const index of path) {
    if (!node || !node.childNodes || index >= node.childNodes.length) return null;
    node = node.childNodes[index];
  }
  return node;
}

// 计算坐标
function getCoordinates(path, offset) {
  const root = editorRef.value;
  if (!root) return null;
  const node = getNodeByPath(root, path);
  if (!node) return null;

  try {
    const range = document.createRange();
    
    // 处理文本节点
    if (node.nodeType === 3) {
        const safeOffset = Math.min(offset, node.length);
        range.setStart(node, safeOffset);
        range.setEnd(node, safeOffset);
        const rects = range.getClientRects();
        if (rects.length > 0) return rects[0];
    } 
    // 处理元素节点
    else if (node.nodeType === 1) {
        const safeOffset = Math.min(offset, node.childNodes.length);
        // 尝试插入临时元素测量
        const span = document.createElement('span');
        span.textContent = '\u200b'; // Zero-width space
        
        if (safeOffset >= node.childNodes.length) {
            node.appendChild(span);
        } else {
            node.insertBefore(span, node.childNodes[safeOffset]);
        }
        
        const rect = span.getBoundingClientRect();
        span.remove();
        return rect;
    }
  } catch (e) {
    console.error('Error calculating cursor coordinates', e);
  }
  return null;
}

function updateCursorPosition(username) {
    const cursor = remoteCursors.value[username];
    if (!cursor || !cursor.data) return;
    
    try {
        const loc = typeof cursor.data === 'string' ? JSON.parse(cursor.data) : cursor.data;
        const rect = getCoordinates(loc.path, loc.offset);
        
        if (rect) {
            const paper = editorRef.value.parentElement;
            const paperRect = paper.getBoundingClientRect();
            
            cursor.top = rect.top - paperRect.top;
            cursor.left = rect.left - paperRect.left;
            cursor.height = rect.height || 20;
            cursor.visible = true;
        } else {
            cursor.visible = false;
        }
    } catch (e) {
        console.error(e);
    }
}

function handleCursorMove(e) {
  if (!isConnected.value) return;
  
  // 更新工具栏状态
  updateToolbarState();

  const loc = getCursorLocation();
  if (loc) {
    sendJson('CURSOR', currentUsername.value, JSON.stringify(loc));
  }
}

// 获取当前光标相对于纯文本的偏移量
function getCursorTextOffset() {
  const sel = window.getSelection();
  if (!sel.rangeCount) return 0;
  const range = sel.getRangeAt(0);
  
  // 插入标记字符
  const marker = document.createTextNode('\uFEFF');
  range.insertNode(marker);
  const text = editorRef.value.textContent;
  const offset = text.indexOf('\uFEFF');
  marker.remove();
  editorRef.value.normalize(); // 合并被分割的文本节点
  return offset === -1 ? 0 : offset;
}

// 根据纯文本偏移量设置光标
function setCursorByTextOffset(offset) {
  const root = editorRef.value;
  if (!root) return;
  
  const nodeIterator = document.createNodeIterator(root, NodeFilter.SHOW_TEXT);
  let currentNode;
  let currentOffset = 0;
  
  while ((currentNode = nodeIterator.nextNode())) {
    const length = currentNode.textContent.length;
    if (currentOffset + length >= offset) {
      const range = document.createRange();
      const pos = Math.max(0, Math.min(length, offset - currentOffset));
      range.setStart(currentNode, pos);
      range.collapse(true);
      const sel = window.getSelection();
      sel.removeAllRanges();
      sel.addRange(range);
      return;
    }
    currentOffset += length;
  }
  
  // Fallback: set to end
  const range = document.createRange();
  range.selectNodeContents(root);
  range.collapse(false);
  const sel = window.getSelection();
  sel.removeAllRanges();
  sel.addRange(range);
}

// 辅助函数：更新内容
function updateContentPreservingCursor(newHtml) {
  if (!editorRef.value) return;
  if (editorRef.value.innerHTML === newHtml) return;

  // 1. 保存当前光标位置（基于文本偏移）
  const savedOffset = getCursorTextOffset();
  const oldText = editorRef.value.textContent;
  
  // 2. 更新内容
  editorRef.value.innerHTML = newHtml;
  content.value = newHtml;
  
  const newText = editorRef.value.textContent;
  
  // 3. 计算文本差异，调整光标位置
  let start = 0;
  while (start < oldText.length && start < newText.length && oldText[start] === newText[start]) {
    start++;
  }
  
  const lengthDiff = newText.length - oldText.length;
  
  let newOffset = savedOffset;
  if (savedOffset > start) {
    newOffset = Math.max(start, savedOffset + lengthDiff);
  }
  
  // 4. 恢复光标
  setCursorByTextOffset(newOffset);
}

// 监听消息
watch(() => messages.value, (newMessages) => {
  if (newMessages.length === 0) return
  const lastMsg = newMessages[newMessages.length - 1]
  if (lastMsg.type === 'received' && lastMsg.raw) {
    const data = typeof lastMsg.raw === 'object' ? lastMsg.raw : null
    if (!data) return

    const colors = ['#ea4335', '#34a853', '#fbbc04', '#9c27b0', '#ff5722']

    if (data.type === 'SYNC' || data.type === 'EDIT') {
      isReceiving = true
      if (data.type === 'EDIT') {
        updateContentPreservingCursor(data.data || '')
      } else {
        content.value = data.data || ''
      }
      // 如果存在 pendingDoc，说明我们刚刚切换到此 doc 并等待加载，加载成功后应用标题
      try {
        if (pendingDoc.value && pendingDoc.value.docId === docId.value) {
          docTitle.value = pendingDoc.value.title || docTitle.value
          pendingDoc.value = null
        }
      } catch (e) { console.warn(e) }
      // 成功从服务端同步到内容时，记录为最近打开的文档（用于回退）
      try {
        const last = {
          docId: docId.value,
          title: docTitle.value,
          content: content.value
        }
        sessionStorage.setItem('lastDoc', JSON.stringify(last))
      } catch (e) {
        console.warn('无法写入 sessionStorage.lastDoc', e)
      }
      // 添加协作者 (兼容旧逻辑，防止 USER_LIST 失败)
      if (data.sender && data.sender !== 'server' && !onlineUsers.value.find(u => u.name === data.sender)) {
        onlineUsers.value.push({
          name: data.sender,
          color: colors[onlineUsers.value.length % colors.length]
        })
      }
      setTimeout(() => { isReceiving = false }, 50)
    } else if (data.type === 'USER_JOIN') {
      if (data.sender && data.sender !== 'server' && !onlineUsers.value.find(u => u.name === data.sender)) {
        onlineUsers.value.push({
          name: data.sender,
          color: colors[onlineUsers.value.length % colors.length]
        })
      }
    } else if (data.type === 'USER_LEAVE') {
      onlineUsers.value = onlineUsers.value.filter(u => u.name !== data.sender)
      delete remoteCursors.value[data.sender]
    } else if (data.type === 'CURSOR') {
      if (data.sender && data.sender !== currentUsername.value) {
        let user = onlineUsers.value.find(u => u.name === data.sender)
        if (!user) {
          user = { name: data.sender, color: colors[onlineUsers.value.length % colors.length] }
          onlineUsers.value.push(user)
        }
        
        const cursor = remoteCursors.value[data.sender] || {}
        cursor.data = data.data
        cursor.color = user.color
        cursor.name = data.sender
        remoteCursors.value[data.sender] = cursor
        
        updateCursorPosition(data.sender)
      }
    } else if (data.type === 'USER_LIST') {
      try {
        const users = JSON.parse(data.data)
        if (Array.isArray(users)) {
          onlineUsers.value = users.map((name, index) => ({
            name: name,
            color: colors[index % colors.length]
          }))
        }
      } catch (e) {
        console.error('解析用户列表失败', e)
      }
    } else if (data.type === 'DOC_DELETED') {
        showMessage('当前文档已被删除', async () => {
            // 尝试获取最新文档列表并跳转到第一个
            try {
                const res = await fetch('/api/doc/list', {
                    headers: { 'Authorization': 'Bearer ' + token.value }
                })
                const resData = await res.json()
                if (resData.code === 200 && resData.data && resData.data.length > 0) {
                    openDoc(resData.data[0])
                } else {
                    // 如果没有文档了，回到主页（或者清空状态）
                    window.location.href = '/'
                }
            } catch (e) {
                window.location.href = '/'
            }
        })
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
const messageCallback = ref(null)

function showMessage(msg, callback = null) {
  messageContent.value = msg
  messageCallback.value = callback
  showMessageModal.value = true
}

function handleMessageConfirm() {
  showMessageModal.value = false
  if (messageCallback.value) {
    messageCallback.value()
    messageCallback.value = null
  }
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
    const res = await fetch('/api/doc', {
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
    const res = await fetch('/api/doc/list', {
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
    const res = await fetch('/api/doc/join', {
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
    const res = await fetch(`/api/doc/${docId.value}/invite`, {
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
    const res = await fetch(`/api/doc/${doc.docId}`, {
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
  // 记录上一个已打开的文档（用于在权限不足时回退）
  if (docId.value && docId.value !== 'doc-001') {
    previousDoc.value = {
      docId: docId.value,
      title: docTitle.value,
      content: content.value
    }
  }

  docId.value = doc.docId
  // 延迟显示标题：在连接成功或收到 SYNC 后再应用，避免在无权限时短暂显示目标标题
  pendingDoc.value = { docId: doc.docId, title: doc.title }
  // 切换文档时先清空内容，等待WebSocket或API加载
  content.value = '' 
  
  const url = new URL(window.location)
  url.searchParams.set('docId', docId.value)
  window.history.pushState({}, '', url)
  
  if (isConnected.value) disconnect()
  clearMessages()
  
  // 切换文档时彻底清空状态
  onlineUsers.value = []
  remoteCursors.value = {}
  
  handleConnect()
  
  showListModal.value = false
}

onMounted(async () => {
  const params = new URLSearchParams(window.location.search)
  const id = params.get('docId')
  if (id) {
    // 在从 URL 打开文档前，记录当前为上一个文档（如果有）
    if (docId.value && docId.value !== 'doc-001') {
      previousDoc.value = {
        docId: docId.value,
        title: docTitle.value,
        content: content.value
      }
    }

    docId.value = id
    // Fetch doc info
    try {
        const res = await fetch(`/api/doc/${id}`, {
             headers: { 'Authorization': 'Bearer ' + token.value }
        })
        const data = await res.json()
        if (data.code === 200) {
            docTitle.value = data.data.title
            content.value = data.data.content || ''
            // 将此成功加载的文档记为最近打开
            try {
              sessionStorage.setItem('lastDoc', JSON.stringify({ docId: id, title: docTitle.value, content: content.value }))
            } catch (e) {
              console.warn('无法写入 sessionStorage.lastDoc', e)
            }
            // Connect if we have a docId
            handleConnect()
        } else {
            showMessage('文档不存在或已被删除', () => {
                window.location.href = '/'
            })
        }
    } catch(e) {
        console.error(e)
        showMessage('加载文档失败', () => {
            window.location.href = '/'
        })
    }
  } else if (isLoggedIn.value) {
    // Auto load latest doc
    try {
      const res = await fetch('/api/doc/list', {
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

async function updateDocTitle() {
  isEditingTitle.value = false
  if (!docId.value || !docTitle.value) return
  
  try {
    const res = await fetch(`/api/doc/${docId.value}/title`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token.value
      },
      body: JSON.stringify({ title: docTitle.value })
    })
    const data = await res.json()
    if (data.code !== 200) {
      showMessage(data.message || '修改标题失败')
    }
  } catch (e) {
    console.error(e)
    showMessage('修改标题失败')
  }
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
            @blur="updateDocTitle"
            @keyup.enter="updateDocTitle"
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
        <button class="tool-btn" title="撤销" @mousedown.prevent @click="execCommand('undo')">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
            <path d="M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z"/>
          </svg>
        </button>
        <button class="tool-btn" title="重做" @mousedown.prevent @click="execCommand('redo')">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
            <path d="M18.4 10.6C16.55 8.99 14.15 8 11.5 8c-4.65 0-8.58 3.03-9.96 7.22L3.9 16c1.05-3.19 4.05-5.5 7.6-5.5 1.95 0 3.73.72 5.12 1.88L13 16h9V7l-3.6 3.6z"/>
          </svg>
        </button>
      </div>
      <div class="toolbar-divider"></div>
      <div class="toolbar-group">
        <select class="tool-select" :value="currentStyle.fontName" @change="execCommand('fontName', $event.target.value)">
          <option value="Arial">Arial</option>
          <option value="Times New Roman">Times New Roman</option>
          <option value="Courier New">Courier New</option>
          <option value="Georgia">Georgia</option>
          <option value="Verdana">Verdana</option>
          <option value="Microsoft YaHei">微软雅黑</option>
          <option value="SimSun">宋体</option>
          <option value="SimHei">黑体</option>
          <option value="KaiTi">楷体</option>
          <option value="FangSong">仿宋</option>
          <option value="STHeiti">华文黑体</option>
          <option value="STKaiti">华文楷体</option>
          <option value="STSong">华文宋体</option>
          <option value="STFangsong">华文仿宋</option>
          <option value="STXinwei">华文新魏</option>
          <option value="STXingkai">华文行楷</option>
          <option value="STLiti">华文隶书</option>
        </select>
        <select class="tool-select" :value="currentStyle.fontSize" @change="execCommand('fontSize', $event.target.value)">
          <option value="1">小号</option>
          <option value="2">中号</option>
          <option value="3">大号</option>
          <option value="4">特大</option>
          <option value="5">超大</option>
          <option value="6">巨大</option>
          <option value="7">最大</option>
        </select>
      </div>
      <div class="toolbar-divider"></div>
      <div class="toolbar-group">
        <button class="tool-btn" :class="{ active: currentStyle.bold }" title="加粗" @mousedown.prevent @click="execCommand('bold')"><b>B</b></button>
        <button class="tool-btn" :class="{ active: currentStyle.italic }" title="斜体" @mousedown.prevent @click="execCommand('italic')"><em>I</em></button>
        <button class="tool-btn" :class="{ active: currentStyle.underline }" title="下划线" @mousedown.prevent @click="execCommand('underline')"><u>U</u></button>
        <button class="tool-btn" :class="{ active: currentStyle.strikeThrough }" title="删除线" @mousedown.prevent @click="execCommand('strikeThrough')"><s>S</s></button>
      </div>
      <div class="toolbar-divider"></div>
      <div class="toolbar-group">
        <button class="tool-btn" title="文字颜色" @mousedown.prevent="openColorPicker('foreColor')">
            <span style="font-weight: bold; color: #000;">A</span>
            <div :style="{ position: 'absolute', bottom: '4px', left: '4px', right: '4px', height: '3px', background: currentStyle.foreColor }"></div>
        </button>
        <input 
            ref="foreColorInput"
            type="color" 
            style="visibility: hidden; position: absolute; width: 0; height: 0;" 
            @input="onColorInput('foreColor', $event.target.value)"
            @change="onColorChange('foreColor', $event.target.value)"
        />
        
        <button class="tool-btn" title="背景颜色" @mousedown.prevent="openColorPicker('hiliteColor')">
            <span style="font-weight: bold; background: #000; color: #fff; padding: 0 2px;">A</span>
            <div :style="{ position: 'absolute', bottom: '4px', left: '4px', right: '4px', height: '3px', background: currentStyle.hiliteColor }"></div>
        </button>
        <input 
            ref="hiliteColorInput"
            type="color" 
            style="visibility: hidden; position: absolute; width: 0; height: 0;" 
            @input="onColorInput('hiliteColor', $event.target.value)"
            @change="onColorChange('hiliteColor', $event.target.value)"
        />
      </div>
      <div class="toolbar-divider"></div>
      <div class="toolbar-group">
        <button class="tool-btn" :class="{ active: currentStyle.justifyLeft }" title="左对齐" @mousedown.prevent @click="execCommand('justifyLeft')">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M15 15H3v2h12v-2zm0-8H3v2h12V7zM3 13h18v-2H3v2zm0 8h18v-2H3v2zM3 3v2h18V3H3z"/></svg>
        </button>
        <button class="tool-btn" :class="{ active: currentStyle.justifyCenter }" title="居中" @mousedown.prevent @click="execCommand('justifyCenter')">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M7 15v2h10v-2H7zm-4 6h18v-2H3v2zm0-8h18v-2H3v2zm4-6v2h10V7H7zM3 3v2h18V3H3z"/></svg>
        </button>
        <button class="tool-btn" :class="{ active: currentStyle.justifyRight }" title="右对齐" @mousedown.prevent @click="execCommand('justifyRight')">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M3 21h18v-2H3v2zm6-4h12v-2H9v2zm-6-4h18v-2H3v2zm6-4h12V7H9v2zM3 3v2h18V3H3z"/></svg>
        </button>
      </div>
      <div class="toolbar-divider"></div>
      <div class="toolbar-group">
        <select class="tool-select" @change="execCommand('formatBlock', $event.target.value)">
          <option value="p">正文</option>
          <option value="h1">标题1</option>
          <option value="h2">标题2</option>
          <option value="h3">标题3</option>
        </select>
      </div>
      <div class="toolbar-right-info" style="margin-left: auto; display:flex; align-items:center; gap:12px; color: #5f6368; font-size:13px;">
        <div v-if="isConnected">延迟: {{ pingDelay !== null ? pingDelay : '-' }} ms</div>
        <div v-else>未连接</div>
      </div>
    </div>

    <!-- 主体 -->
    <main class="main-content">
      <!-- 编辑区域 -->
      <div class="editor-container">
        <div class="paper" style="position: relative;">
          <div
            ref="editorRef"
            class="editor"
            contenteditable="true"
            spellcheck="false"
            :placeholder="isConnected ? '开始输入内容...' : '请先登录或选择文档'"
            @input="handleInput"
            @keyup="handleCursorMove"
            @click="handleCursorMove"
            @blur="handleCursorMove"
          ></div>

          <!-- 远程光标 -->
          <div
            v-for="(cursor, username) in remoteCursors"
            :key="username"
            class="remote-cursor"
            v-show="cursor.visible !== false"
            :style="{
              top: cursor.top + 'px',
              left: cursor.left + 'px',
              height: cursor.height + 'px',
              backgroundColor: cursor.color
            }"
          >
            <div class="cursor-flag" :style="{ backgroundColor: cursor.color }">
              {{ getInitial(cursor.name) }}
            </div>
          </div>
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
              <div class="doc-item-owner" v-if="doc.ownerName">创建者: {{ doc.ownerName }}</div>
            </div>
            <button class="delete-btn" @click.stop="deleteDoc(doc)" title="删除">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="#5f6368">
                <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
              </svg>
            </button>
          </div>
        </div>
        <div class="modal-actions" style="justify-content: space-between;">
          <button class="btn-primary" @click="showListModal = false; openCreateModal()">新建文档</button>
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
    <div v-if="showMessageModal" class="modal-overlay" @click="handleMessageConfirm">
      <div class="modal-content" @click.stop style="max-width: 400px;">
        <h3>提示</h3>
        <p style="margin: 20px 0; color: #5f6368;">{{ messageContent }}</p>
        <div class="modal-actions">
          <button class="btn-primary" @click="handleMessageConfirm">确定</button>
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

.doc-item-owner {
  font-size: 12px;
  color: #5f6368;
  margin-top: 2px;
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
  position: relative;
}

.tool-btn:hover {
  background: #f1f3f4;
}

.tool-btn.active {
  background: #e8f0fe;
  color: #1a73e8;
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
  overflow-y: auto;
}

.editor[contenteditable]:empty:before {
  content: attr(placeholder);
  color: #9aa0a6;
  display: block;
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

.remote-cursor {
  position: absolute;
  width: 2px;
  pointer-events: none;
  z-index: 10;
  transition: all 0.1s ease;
}

.cursor-flag {
  position: absolute;
  top: -24px;
  left: -9px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  color: white;
  font-size: 12px;
  line-height: 20px;
  text-align: center;
  font-weight: bold;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}
</style>
