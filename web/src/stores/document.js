import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from './auth'

export const useDocumentStore = defineStore('document', () => {
  const currentDocId = ref('')
  const currentDocTitle = ref('未命名文档')
  const docList = ref([])
  const loading = ref(false)

  function getHeaders() {
    const authStore = useAuthStore()
    return {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + authStore.token
    }
  }

  async function fetchDocList() {
    loading.value = true
    try {
      const res = await fetch('/api/doc/list', { headers: getHeaders() })
      const data = await res.json()
      if (data.code === 200) {
        docList.value = data.data || []
      }
    } catch (e) {
      console.error('获取文档列表失败', e)
    } finally {
      loading.value = false
    }
  }

  async function createDocument(title) {
    try {
      const res = await fetch('/api/doc', {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({ title })
      })
      const data = await res.json()
      if (data.code === 200) {
        return data.data
      }
      throw new Error(data.message || '创建失败')
    } catch (e) {
      console.error('创建文档失败', e)
      throw e
    }
  }

  async function deleteDocument(docId) {
    try {
      const res = await fetch(`/api/doc/${docId}`, {
        method: 'DELETE',
        headers: getHeaders()
      })
      const data = await res.json()
      if (data.code === 200) {
        docList.value = docList.value.filter(d => d.docId !== docId)
        return true
      }
      throw new Error(data.message || '删除失败')
    } catch (e) {
      console.error('删除文档失败', e)
      throw e
    }
  }

  async function updateTitle(docId, title) {
    try {
      const res = await fetch(`/api/doc/${docId}/title`, {
        method: 'PUT',
        headers: getHeaders(),
        body: JSON.stringify({ title })
      })
      const data = await res.json()
      if (data.code === 200) {
        currentDocTitle.value = title
        return true
      }
      throw new Error(data.message || '更新失败')
    } catch (e) {
      console.error('更新标题失败', e)
      throw e
    }
  }

  async function createInviteCode(docId) {
    try {
      const res = await fetch(`/api/doc/${docId}/invite`, {
        method: 'POST',
        headers: getHeaders()
      })
      const data = await res.json()
      if (data.code === 200) {
        return data.data
      }
      throw new Error(data.message || '生成邀请码失败')
    } catch (e) {
      console.error('生成邀请码失败', e)
      throw e
    }
  }

  async function joinByInviteCode(code) {
    try {
      const res = await fetch('/api/doc/join', {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({ code })
      })
      const data = await res.json()
      if (data.code === 200) {
        return data.data
      }
      throw new Error(data.message || '加入失败')
    } catch (e) {
      console.error('加入协作失败', e)
      throw e
    }
  }

  function openDocument(docId, title) {
    currentDocId.value = docId
    currentDocTitle.value = title || '未命名文档'
  }

  return {
    currentDocId, currentDocTitle, docList, loading,
    fetchDocList, createDocument, deleteDocument, updateTitle,
    createInviteCode, joinByInviteCode, openDocument
  }
})
