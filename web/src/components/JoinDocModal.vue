<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['close', 'join'])
const code = ref('')

watch(() => props.visible, (v) => { if (v) code.value = '' })
</script>

<template>
  <div v-if="visible" class="modal-overlay" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <h3>加入协作</h3>
      <div class="form-item">
        <label>邀请码</label>
        <input v-model="code" placeholder="请输入8位邀请码" @keyup.enter="$emit('join', code)" autofocus>
      </div>
      <div class="modal-actions">
        <button class="btn-secondary" @click="$emit('close')">取消</button>
        <button class="btn-primary" @click="$emit('join', code)">加入</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: #fff; border-radius: 8px; padding: 24px; width: 400px; box-shadow: 0 4px 24px rgba(0,0,0,0.2); }
.modal-content h3 { margin: 0 0 20px 0; font-size: 18px; color: #202124; }
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px; }
.form-item { margin-bottom: 12px; }
.form-item label { display: block; font-size: 12px; color: #5f6368; margin-bottom: 4px; }
.form-item input { width: 100%; padding: 8px 12px; border: 1px solid #dadce0; border-radius: 4px; font-size: 14px; box-sizing: border-box; }
.form-item input:focus { outline: none; border-color: #1a73e8; }
.btn-primary { padding: 8px 24px; background: #1a73e8; color: #fff; border: none; border-radius: 4px; font-size: 14px; font-weight: 500; cursor: pointer; }
.btn-primary:hover { background: #1557b0; }
.btn-secondary { padding: 8px 24px; background: #fff; color: #5f6368; border: 1px solid #dadce0; border-radius: 4px; font-size: 14px; font-weight: 500; cursor: pointer; }
.btn-secondary:hover { background: #f1f3f4; }
</style>
