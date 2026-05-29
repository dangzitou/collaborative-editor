<script setup>
defineProps({ visible: Boolean, content: String })
defineEmits(['close'])
</script>

<template>
  <div v-if="visible" class="modal-overlay" @click="$emit('close')">
    <div class="modal-content" @click.stop style="max-width: 400px;">
      <h3>提示</h3>
      <p style="margin: 20px 0; color: #5f6368;">{{ content }}</p>
      <div class="modal-actions">
        <button class="btn-primary" @click="$emit('close')">确定</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: #fff; border-radius: 8px; padding: 24px; box-shadow: 0 4px 24px rgba(0,0,0,0.2); }
.modal-content h3 { margin: 0; font-size: 18px; color: #202124; }
.modal-actions { display: flex; justify-content: flex-end; margin-top: 24px; }
.btn-primary { padding: 8px 24px; background: #1a73e8; color: #fff; border: none; border-radius: 4px; font-size: 14px; font-weight: 500; cursor: pointer; }
.btn-primary:hover { background: #1557b0; }
</style>
