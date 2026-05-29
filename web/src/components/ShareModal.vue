<script setup>
defineProps({ visible: Boolean, inviteCode: String })
defineEmits(['close'])
</script>

<template>
  <div v-if="visible" class="modal-overlay" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <h3>邀请协作</h3>
      <p style="color: #5f6368; margin-bottom: 10px;">将此邀请码发送给好友，对方即可加入协作：</p>
      <div class="invite-code-box">{{ inviteCode }}</div>
      <div class="modal-actions">
        <button class="btn-primary" @click="$emit('close')">关闭</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: #fff; border-radius: 8px; padding: 24px; width: 400px; box-shadow: 0 4px 24px rgba(0,0,0,0.2); }
.modal-content h3 { margin: 0 0 20px 0; font-size: 18px; color: #202124; }
.invite-code-box { background: #f1f3f4; padding: 15px; border-radius: 4px; text-align: center; font-size: 24px; letter-spacing: 2px; font-family: monospace; margin-bottom: 20px; user-select: all; }
.modal-actions { display: flex; justify-content: flex-end; margin-top: 24px; }
.btn-primary { padding: 8px 24px; background: #1a73e8; color: #fff; border: none; border-radius: 4px; font-size: 14px; font-weight: 500; cursor: pointer; }
.btn-primary:hover { background: #1557b0; }
</style>
