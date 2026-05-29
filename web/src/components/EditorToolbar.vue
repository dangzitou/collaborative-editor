<script setup>
import { computed } from 'vue'

const props = defineProps({
  editor: { type: Object, default: null },
  isConnected: { type: Boolean, default: false },
  pingDelay: { type: Number, default: null }
})

const FONT_FAMILIES = [
  { label: 'Arial', value: 'Arial' },
  { label: 'Times New Roman', value: 'Times New Roman' },
  { label: 'Courier New', value: 'Courier New' },
  { label: 'Georgia', value: 'Georgia' },
  { label: 'Verdana', value: 'Verdana' },
  { label: '微软雅黑', value: 'Microsoft YaHei' },
  { label: '宋体', value: 'SimSun' },
  { label: '黑体', value: 'SimHei' },
  { label: '楷体', value: 'KaiTi' },
  { label: '仿宋', value: 'FangSong' },
  { label: '华文黑体', value: 'STHeiti' },
  { label: '华文楷体', value: 'STKaiti' },
  { label: '华文宋体', value: 'STSong' },
  { label: '华文仿宋', value: 'STFangsong' }
]

const FONT_SIZES = [
  { label: '小号', value: '12px' },
  { label: '中号', value: '14px' },
  { label: '大号', value: '16px' },
  { label: '特大', value: '18px' },
  { label: '超大', value: '24px' },
  { label: '巨大', value: '32px' },
  { label: '最大', value: '48px' }
]

const currentFontFamily = computed(() => {
  if (!props.editor) return 'Arial'
  return props.editor.getAttributes('textStyle').fontFamily || 'Arial'
})

const currentFontSize = computed(() => {
  if (!props.editor) return '16px'
  return props.editor.getAttributes('textStyle').fontSize || '16px'
})

function run(command) {
  if (props.editor) command()
}
</script>

<template>
  <div class="toolbar" v-if="editor">
    <div class="toolbar-group">
      <button class="tool-btn" title="撤销" @click="run(() => editor.chain().focus().undo().run())">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z"/></svg>
      </button>
      <button class="tool-btn" title="重做" @click="run(() => editor.chain().focus().redo().run())">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M18.4 10.6C16.55 8.99 14.15 8 11.5 8c-4.65 0-8.58 3.03-9.96 7.22L3.9 16c1.05-3.19 4.05-5.5 7.6-5.5 1.95 0 3.73.72 5.12 1.88L13 16h9V7l-3.6 3.6z"/></svg>
      </button>
    </div>
    <div class="toolbar-divider"></div>
    <div class="toolbar-group">
      <select class="tool-select" :value="currentFontFamily" @change="run(() => editor.chain().focus().setFontFamily($event.target.value).run())">
        <option v-for="f in FONT_FAMILIES" :key="f.value" :value="f.value">{{ f.label }}</option>
      </select>
      <select class="tool-select" :value="currentFontSize" @change="run(() => editor.chain().focus().setFontSize($event.target.value).run())">
        <option v-for="f in FONT_SIZES" :key="f.value" :value="f.value">{{ f.label }}</option>
      </select>
    </div>
    <div class="toolbar-divider"></div>
    <div class="toolbar-group">
      <button class="tool-btn" :class="{ active: editor.isActive('bold') }" title="加粗" @click="run(() => editor.chain().focus().toggleBold().run())"><b>B</b></button>
      <button class="tool-btn" :class="{ active: editor.isActive('italic') }" title="斜体" @click="run(() => editor.chain().focus().toggleItalic().run())"><em>I</em></button>
      <button class="tool-btn" :class="{ active: editor.isActive('underline') }" title="下划线" @click="run(() => editor.chain().focus().toggleUnderline().run())"><u>U</u></button>
      <button class="tool-btn" :class="{ active: editor.isActive('strike') }" title="删除线" @click="run(() => editor.chain().focus().toggleStrike().run())"><s>S</s></button>
    </div>
    <div class="toolbar-divider"></div>
    <div class="toolbar-group">
      <button class="tool-btn" title="文字颜色">
        <span style="font-weight: bold; color: #000;">A</span>
        <div :style="{ position: 'absolute', bottom: '4px', left: '4px', right: '4px', height: '3px', background: editor.getAttributes('textStyle').color || '#000000' }"></div>
        <input type="color" :value="editor.getAttributes('textStyle').color || '#000000'" style="position: absolute; opacity: 0; width: 100%; height: 100%; cursor: pointer;"
          @input="run(() => editor.chain().focus().setColor($event.target.value).run())" />
      </button>
      <button class="tool-btn" title="背景颜色">
        <span style="font-weight: bold; background: #000; color: #fff; padding: 0 2px;">A</span>
        <div :style="{ position: 'absolute', bottom: '4px', left: '4px', right: '4px', height: '3px', background: editor.getAttributes('highlight').color || 'transparent' }"></div>
        <input type="color" :value="editor.getAttributes('highlight').color || '#ffff00'" style="position: absolute; opacity: 0; width: 100%; height: 100%; cursor: pointer;"
          @input="run(() => editor.chain().focus().toggleHighlight({ color: $event.target.value }).run())" />
      </button>
    </div>
    <div class="toolbar-divider"></div>
    <div class="toolbar-group">
      <button class="tool-btn" :class="{ active: editor.isActive({ textAlign: 'left' }) }" title="左对齐" @click="run(() => editor.chain().focus().setTextAlign('left').run())">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M15 15H3v2h12v-2zm0-8H3v2h12V7zM3 13h18v-2H3v2zm0 8h18v-2H3v2zM3 3v2h18V3H3z"/></svg>
      </button>
      <button class="tool-btn" :class="{ active: editor.isActive({ textAlign: 'center' }) }" title="居中" @click="run(() => editor.chain().focus().setTextAlign('center').run())">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M7 15v2h10v-2H7zm-4 6h18v-2H3v2zm0-8h18v-2H3v2zm4-6v2h10V7H7zM3 3v2h18V3H3z"/></svg>
      </button>
      <button class="tool-btn" :class="{ active: editor.isActive({ textAlign: 'right' }) }" title="右对齐" @click="run(() => editor.chain().focus().setTextAlign('right').run())">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M3 21h18v-2H3v2zm6-4h12v-2H9v2zm-6-4h18v-2H3v2zm6-4h12V7H9v2zM3 3v2h18V3H3z"/></svg>
      </button>
    </div>
    <div class="toolbar-divider"></div>
    <div class="toolbar-group">
      <select class="tool-select"
        :value="editor.isActive('heading', { level: 1 }) ? 'h1' : editor.isActive('heading', { level: 2 }) ? 'h2' : editor.isActive('heading', { level: 3 }) ? 'h3' : 'p'"
        @change="run(() => {
          if ($event.target.value === 'p') editor.chain().focus().setParagraph().run()
          else editor.chain().focus().toggleHeading({ level: parseInt($event.target.value[1]) }).run()
        })">
        <option value="p">正文</option>
        <option value="h1">标题1</option>
        <option value="h2">标题2</option>
        <option value="h3">标题3</option>
      </select>
    </div>
    <div class="toolbar-right-info">
      <div v-if="isConnected">延迟: {{ pingDelay !== null ? pingDelay : '-' }} ms</div>
      <div v-else>未连接</div>
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  height: 40px; background: #fff; border-bottom: 1px solid #e0e0e0;
  display: flex; align-items: center; padding: 0 16px; gap: 4px;
}
.toolbar-group { display: flex; align-items: center; gap: 2px; }
.toolbar-divider { width: 1px; height: 24px; background: #e0e0e0; margin: 0 8px; }
.toolbar-right-info { margin-left: auto; display: flex; align-items: center; gap: 12px; color: #5f6368; font-size: 13px; }

.tool-btn {
  min-width: 32px; height: 32px; border: none; background: transparent; border-radius: 4px;
  cursor: pointer; display: flex; align-items: center; justify-content: center; color: #444; font-size: 14px; font-weight: 600; position: relative;
}
.tool-btn:hover { background: #f1f3f4; }
.tool-btn.active { background: #e8f0fe; color: #1a73e8; }

.tool-select {
  height: 32px; border: none; background: transparent; font-size: 14px; color: #444;
  cursor: pointer; padding: 0 8px; border-radius: 4px;
}
.tool-select:hover { background: #f1f3f4; }
</style>
