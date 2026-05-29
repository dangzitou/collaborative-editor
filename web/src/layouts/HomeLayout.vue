<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useDocumentStore } from '../stores/document'
import AuthModal from '../components/AuthModal.vue'
import CreateDocModal from '../components/CreateDocModal.vue'
import JoinDocModal from '../components/JoinDocModal.vue'
import DocumentListModal from '../components/DocumentListModal.vue'
import DeleteConfirmModal from '../components/DeleteConfirmModal.vue'
import MessageModal from '../components/MessageModal.vue'
import Navbar from '../components/Navbar.vue'

const router = useRouter()
const authStore = useAuthStore()
const docStore = useDocumentStore()

const showAuthModal = ref(false)
const showCreateModal = ref(false)
const showJoinModal = ref(false)
const showListModal = ref(false)
const showDeleteModal = ref(false)
const docToDelete = ref(null)
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

function openCreateModal() {
  if (!authStore.isLoggedIn) {
    showMessage('请先登录')
    showAuthModal.value = true
    return
  }
  showCreateModal.value = true
}

async function handleCreateDoc(title) {
  try {
    const doc = await docStore.createDocument(title)
    showCreateModal.value = false
    router.push(`/doc/${doc.docId}`)
  } catch (e) {
    showMessage('创建失败')
  }
}

async function handleFetchDocList() {
  if (!authStore.isLoggedIn) {
    showMessage('请先登录')
    showAuthModal.value = true
    return
  }
  await docStore.fetchDocList()
  showListModal.value = true
}

function openDoc(doc) {
  showListModal.value = false
  router.push(`/doc/${doc.docId}`)
}

function deleteDoc(doc) {
  docToDelete.value = doc
  showDeleteModal.value = true
}

async function confirmDelete() {
  if (!docToDelete.value) return
  try {
    await docStore.deleteDocument(docToDelete.value.docId)
    showDeleteModal.value = false
    docToDelete.value = null
  } catch (e) {
    showMessage('删除失败')
  }
}

async function handleJoinDoc(code) {
  try {
    const doc = await docStore.joinByInviteCode(code)
    showJoinModal.value = false
    router.push(`/doc/${doc.docId}`)
  } catch (e) {
    showMessage('加入失败')
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}

onMounted(() => {
  if (authStore.isLoggedIn) {
    docStore.fetchDocList()
  }
})
</script>

<template>
  <div class="app">
    <Navbar
      :is-home="true"
      @open-create="openCreateModal"
      @open-list="handleFetchDocList"
      @open-join="showJoinModal = true"
      @login="showAuthModal = true"
      @logout="handleLogout"
    />

    <main class="home-content">
      <div class="home-hero" v-if="!authStore.isLoggedIn">
        <h1>CoDoc</h1>
        <p>实时协作编辑器</p>
        <button class="btn-primary" @click="showAuthModal = true" style="width: auto; padding: 12px 48px;">开始使用</button>
      </div>
      <div class="home-docs" v-else>
        <div class="home-docs-header">
          <h2>我的文档</h2>
          <button class="btn-primary" @click="openCreateModal" style="width: auto; padding: 8px 24px;">新建文档</button>
        </div>
        <div class="home-doc-list">
          <div v-if="docStore.docList.length === 0" class="empty-tip">暂无文档，点击上方按钮创建</div>
          <div v-for="doc in docStore.docList" :key="doc.docId" class="home-doc-item" @click="openDoc(doc)">
            <div class="doc-item-icon">
              <svg viewBox="0 0 24 24" width="24" height="24" fill="#1a73e8">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
              </svg>
            </div>
            <div class="doc-item-info">
              <div class="doc-item-title">{{ doc.title }}</div>
              <div class="doc-item-time">{{ new Date(doc.updateTime).toLocaleString() }}</div>
            </div>
            <button class="delete-btn" @click.stop="deleteDoc(doc)" title="删除">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="#5f6368">
                <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </main>

    <AuthModal :visible="showAuthModal" @close="showAuthModal = false" />
    <CreateDocModal :visible="showCreateModal" @close="showCreateModal = false" @create="handleCreateDoc" />
    <JoinDocModal :visible="showJoinModal" @close="showJoinModal = false" @join="handleJoinDoc" />
    <DocumentListModal :visible="showListModal" :docs="docStore.docList" @close="showListModal = false" @open="openDoc" @delete="deleteDoc" @create="openCreateModal" />
    <DeleteConfirmModal :visible="showDeleteModal" :doc="docToDelete" @close="showDeleteModal = false" @confirm="confirmDelete" />
    <MessageModal :visible="showMessageModal" :content="messageContent" @close="handleMessageConfirm" />
  </div>
</template>

<style scoped>
.app {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f5f5f5;
}

.home-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px;
}

.home-hero {
  text-align: center;
}

.home-hero h1 {
  font-size: 48px;
  color: #1a73e8;
  margin-bottom: 8px;
}

.home-hero p {
  font-size: 18px;
  color: #5f6368;
  margin-bottom: 32px;
}

.home-docs {
  width: 100%;
  max-width: 800px;
}

.home-docs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.home-docs-header h2 {
  font-size: 20px;
  color: #202124;
}

.home-doc-list {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.12);
  overflow: hidden;
}

.home-doc-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f1f3f4;
  cursor: pointer;
  transition: background 0.2s;
}

.home-doc-item:last-child {
  border-bottom: none;
}

.home-doc-item:hover {
  background: #f8f9fa;
}

.doc-item-icon {
  margin-right: 16px;
  display: flex;
}

.doc-item-info {
  flex: 1;
}

.doc-item-title {
  font-size: 14px;
  font-weight: 500;
  color: #202124;
}

.doc-item-time {
  font-size: 12px;
  color: #5f6368;
  margin-top: 2px;
}

.empty-tip {
  padding: 32px;
  text-align: center;
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
}

.home-doc-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: #fce8e6;
}

.delete-btn:hover svg {
  fill: #ea4335;
}

.btn-primary {
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
</style>
