<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useDocumentStore } from '../stores/document'
import Navbar from '../components/Navbar.vue'
import EditorToolbar from '../components/EditorToolbar.vue'
import TipTapEditor from '../components/TipTapEditor.vue'
import ShareModal from '../components/ShareModal.vue'
import MessageModal from '../components/MessageModal.vue'
import AuthModal from '../components/AuthModal.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const docStore = useDocumentStore()

const showAuthModal = ref(false)
const showShareModal = ref(false)
const currentInviteCode = ref('')
const showMessageModal = ref(false)
const messageContent = ref('')
const messageCallback = ref(null)
const isEditingTitle = ref(false)
const isConnected = ref(false)
const pingDelay = ref(null)

const docId = ref('')
const docTitle = ref('未命名文档')
const onlineUsers = ref([])
const editorRef = ref(null)
const tiptapEditor = ref(null)

const currentUsername = computed(() => authStore.currentUsername || '游客')

function showMessage(msg, callback = null) {
  messageContent.value = msg
  messageCallback.value = callback
  showMessageModal.value = true
}

function handleMessageConfirm() {
  showMessageModal.value = false
  if (messageCallback.value) { messageCallback.value(); messageCallback.value = null }
}

async function updateDocTitle(newTitle) {
  isEditingTitle.value = false
  if (!docId.value || !newTitle) return
  docTitle.value = newTitle
  try { await docStore.updateTitle(docId.value, newTitle) } catch (e) { showMessage('修改标题失败') }
}

async function handleShare() {
  if (!docId.value) return
  try {
    const code = await docStore.createInviteCode(docId.value)
    currentInviteCode.value = code
    showShareModal.value = true
  } catch (e) { showMessage('生成邀请码失败') }
}

function onEditorConnected() {
  isConnected.value = true
  onlineUsers.value = [{ name: currentUsername.value, color: '#1a73e8' }]
}

function onEditorDisconnected() {
  isConnected.value = false
}

function getTipapEditor() {
  return tiptapEditor.value?.getEditor?.() || null
}

onMounted(async () => {
  const id = route.params.docId
  if (!id) { router.push('/'); return }
  docId.value = id
  try {
    const res = await fetch(`/api/doc/${id}`, { headers: { 'Authorization': 'Bearer ' + authStore.token } })
    const data = await res.json()
    if (data.code === 200) {
      docTitle.value = data.data.title
    } else {
      showMessage('文档不存在或已被删除', () => { router.push('/') })
    }
  } catch (e) {
    showMessage('加载文档失败', () => { router.push('/') })
  }
})
</script>

<template>
  <div class="app">
    <Navbar
      :is-home="false"
      :doc-title="docTitle"
      :is-connected="isConnected"
      :online-users="onlineUsers"
      :is-editing-title="isEditingTitle"
      @start-edit-title="isEditingTitle = true"
      @update-title="updateDocTitle"
      @share="handleShare"
      @login="showAuthModal = true"
      @logout="authStore.logout(); isConnected = false"
    />

    <EditorToolbar
      :editor="getTipapEditor()"
      :is-connected="isConnected"
      :ping-delay="pingDelay"
    />

    <main class="main-content">
      <div class="editor-container">
        <div class="paper">
          <TipTapEditor
            v-if="docId"
            ref="tiptapEditor"
            :doc-id="docId"
            :username="currentUsername"
            @connected="onEditorConnected"
            @disconnected="onEditorDisconnected"
          />
        </div>
      </div>
    </main>

    <AuthModal :visible="showAuthModal" @close="showAuthModal = false" />
    <ShareModal :visible="showShareModal" :invite-code="currentInviteCode" @close="showShareModal = false" />
    <MessageModal :visible="showMessageModal" :content="messageContent" @close="handleMessageConfirm" />
  </div>
</template>

<style scoped>
.app { display: flex; flex-direction: column; min-height: 100vh; background: #f5f5f5; }

.main-content { flex: 1; display: flex; overflow: hidden; }

.editor-container { flex: 1; display: flex; justify-content: center; padding: 32px; overflow: auto; }

.paper {
  width: 100%; max-width: 816px; min-height: calc(100vh - 200px);
  background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24); border-radius: 2px;
}

@media (max-width: 768px) {
  .editor-container { padding: 16px; }
}
</style>
