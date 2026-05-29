<script setup>
defineProps({
  visible: Boolean,
  docs: { type: Array, default: () => [] }
})
defineEmits(['close', 'open', 'delete', 'create'])
</script>

<template>
  <div v-if="visible" class="modal-overlay" @click="$emit('close')">
    <div class="modal-content list-modal" @click.stop>
      <h3>我的文档</h3>
      <div class="doc-list">
        <div v-if="docs.length === 0" class="empty-tip">暂无文档</div>
        <div v-for="doc in docs" :key="doc.docId" class="doc-item" @click="$emit('open', doc)">
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
          <button class="delete-btn" @click.stop="$emit('delete', doc)" title="删除">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="#5f6368">
              <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
            </svg>
          </button>
        </div>
      </div>
      <div class="modal-actions" style="justify-content: space-between;">
        <button class="btn-primary" @click="$emit('close'); $emit('create')">新建文档</button>
        <button class="btn-secondary" @click="$emit('close')">关闭</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: #fff; border-radius: 8px; padding: 24px; box-shadow: 0 4px 24px rgba(0,0,0,0.2); }
.modal-content h3 { margin: 0 0 20px 0; font-size: 18px; color: #202124; }
.list-modal { width: 600px; max-height: 80vh; display: flex; flex-direction: column; }
.doc-list { flex: 1; overflow-y: auto; border: 1px solid #e0e0e0; border-radius: 4px; margin-bottom: 16px; }
.empty-tip { padding: 32px; text-align: center; color: #5f6368; }
.doc-item { display: flex; align-items: center; padding: 12px 16px; border-bottom: 1px solid #f1f3f4; cursor: pointer; transition: background 0.2s; }
.doc-item:last-child { border-bottom: none; }
.doc-item:hover { background: #f8f9fa; }
.doc-item-icon { margin-right: 16px; display: flex; align-items: center; }
.doc-item-info { flex: 1; }
.doc-item-title { font-size: 14px; font-weight: 500; color: #202124; margin-bottom: 4px; }
.doc-item-time { font-size: 12px; color: #5f6368; }
.doc-item-owner { font-size: 12px; color: #5f6368; margin-top: 2px; }
.delete-btn { background: none; border: none; padding: 8px; border-radius: 50%; cursor: pointer; opacity: 0; transition: all 0.2s; display: flex; align-items: center; justify-content: center; }
.doc-item:hover .delete-btn { opacity: 1; }
.delete-btn:hover { background: #fce8e6; }
.delete-btn:hover svg { fill: #ea4335; }
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px; }
.btn-primary { padding: 8px 24px; background: #1a73e8; color: #fff; border: none; border-radius: 4px; font-size: 14px; font-weight: 500; cursor: pointer; }
.btn-primary:hover { background: #1557b0; }
.btn-secondary { padding: 8px 24px; background: #fff; color: #5f6368; border: 1px solid #dadce0; border-radius: 4px; font-size: 14px; font-weight: 500; cursor: pointer; }
.btn-secondary:hover { background: #f1f3f4; }
</style>
