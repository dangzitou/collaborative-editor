<script setup>
defineProps({ visible: Boolean, doc: Object })
defineEmits(['close', 'confirm'])
</script>

<template>
  <div v-if="visible" class="modal-overlay" @click="$emit('close')">
    <div class="modal-content" @click.stop style="max-width: 400px;">
      <h3>删除文档</h3>
      <p style="margin: 20px 0; color: #5f6368;">确定要删除文档 "{{ doc?.title }}" 吗？此操作无法撤销。</p>
      <div class="modal-actions">
        <button class="btn-secondary" @click="$emit('close')">取消</button>
        <button class="btn-danger" @click="$emit('confirm')">删除</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: #fff; border-radius: 8px; padding: 24px; box-shadow: 0 4px 24px rgba(0,0,0,0.2); }
.modal-content h3 { margin: 0; font-size: 18px; color: #202124; }
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px; }
.btn-secondary { padding: 8px 24px; background: #fff; color: #5f6368; border: 1px solid #dadce0; border-radius: 4px; font-size: 14px; font-weight: 500; cursor: pointer; }
.btn-secondary:hover { background: #f1f3f4; }
.btn-danger { padding: 8px 24px; background: #ea4335; color: #fff; border: none; border-radius: 4px; font-size: 14px; font-weight: 500; cursor: pointer; }
.btn-danger:hover { background: #c5221f; }
</style>
