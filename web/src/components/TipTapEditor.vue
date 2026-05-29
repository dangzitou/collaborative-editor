<script setup>
import { onMounted, onUnmounted, watch, ref } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Collaboration from '@tiptap/extension-collaboration'
import CollaborationCursor from '@tiptap/extension-collaboration-cursor'
import Color from '@tiptap/extension-color'
import TextStyle from '@tiptap/extension-text-style'
import Highlight from '@tiptap/extension-highlight'
import TextAlign from '@tiptap/extension-text-align'
import FontFamily from '@tiptap/extension-font-family'
import Underline from '@tiptap/extension-underline'
import Placeholder from '@tiptap/extension-placeholder'
import { FontSize } from '../extensions/FontSize'
import * as Y from 'yjs'
import { WebsocketProvider } from 'y-websocket'

const props = defineProps({
  docId: { type: String, required: true },
  username: { type: String, default: 'Anonymous' },
  userColor: { type: String, default: '#1a73e8' },
  editable: { type: Boolean, default: true }
})

const emit = defineEmits(['update', 'connected', 'disconnected'])

const editor = ref(null)
let ydoc = null
let provider = null

function getColorForUser(name) {
  const colors = ['#1a73e8', '#e8710a', '#0d652d', '#c5221f', '#8430ce', '#185abc', '#7b5800']
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return colors[Math.abs(hash) % colors.length]
}

function initEditor() {
  cleanup()

  ydoc = new Y.Doc()
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsHost = window.location.host
  const roomName = `doc:${props.docId}`

  // 开发环境直连 y-websocket server，生产环境通过 Nginx 代理
  let wsUrl
  if (import.meta.env.DEV) {
    wsUrl = `ws://localhost:1234/${roomName}`
  } else {
    wsUrl = `${wsProtocol}//${wsHost}/yjs/${roomName}`
  }

  provider = new WebsocketProvider(wsUrl, roomName, ydoc)

  provider.on('status', (event) => {
    if (event.status === 'connected') {
      emit('connected')
    } else {
      emit('disconnected')
    }
  })

  provider.awareness.setLocalStateField('user', {
    name: props.username,
    color: getColorForUser(props.username)
  })

  const extensions = [
    StarterKit.configure({ history: false }), // 禁用内置 history，由 Yjs 处理
    Collaboration.configure({ document: ydoc }),
    CollaborationCursor.configure({
      provider,
      user: {
        name: props.username,
        color: getColorForUser(props.username)
      }
    }),
    Color,
    TextStyle,
    Highlight.configure({ multicolor: true }),
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
    FontFamily,
    FontSize,
    Underline,
    Placeholder.configure({ placeholder: '开始输入内容...' })
  ]

  editor.value = useEditor({
    extensions,
    editable: props.editable,
    onUpdate: ({ editor: ed }) => {
      emit('update', ed.getHTML())
    }
  })
}

function cleanup() {
  if (provider) {
    provider.destroy()
    provider = null
  }
  if (ydoc) {
    ydoc.destroy()
    ydoc = null
  }
  if (editor.value) {
    editor.value.destroy()
    editor.value = null
  }
}

// 当 docId 变化时重新初始化
watch(() => props.docId, (newId) => {
  if (newId) initEditor()
})

// 当 editable 变化时更新
watch(() => props.editable, (val) => {
  if (editor.value) editor.value.setEditable(val)
})

onMounted(() => {
  if (props.docId) initEditor()
})

onUnmounted(() => {
  cleanup()
})

// 暴露 editor 实例给父组件（用于 toolbar 操作）
defineExpose({
  getEditor: () => editor.value
})
</script>

<template>
  <div class="tiptap-editor-wrapper">
    <EditorContent v-if="editor" :editor="editor" class="tiptap-editor" />
  </div>
</template>

<style scoped>
.tiptap-editor-wrapper {
  width: 100%;
  min-height: calc(100vh - 200px);
}

.tiptap-editor :deep(.tiptap) {
  width: 100%;
  min-height: calc(100vh - 200px);
  padding: 72px 96px;
  border: none;
  outline: none;
  font-family: 'Arial', sans-serif;
  font-size: 16px;
  line-height: 1.8;
  color: #202124;
}

.tiptap-editor :deep(.tiptap p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  color: #9aa0a6;
  pointer-events: none;
  height: 0;
}

.tiptap-editor :deep(.tiptap) > * + * {
  margin-top: 0.75em;
}

.tiptap-editor :deep(.tiptap h1) { font-size: 2em; font-weight: 700; }
.tiptap-editor :deep(.tiptap h2) { font-size: 1.5em; font-weight: 600; }
.tiptap-editor :deep(.tiptap h3) { font-size: 1.25em; font-weight: 600; }

/* 远程光标样式 */
.tiptap-editor :deep(.collaboration-cursor__caret) {
  position: relative;
  margin-left: -1px;
  margin-right: -1px;
  border-left: 1px solid;
  border-right: 1px solid;
  word-break: normal;
  pointer-events: none;
}

.tiptap-editor :deep(.collaboration-cursor__label) {
  position: absolute;
  top: -1.4em;
  left: -1px;
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
  line-height: normal;
  padding: 0.1rem 0.3rem;
  border-radius: 3px 3px 3px 0;
  white-space: nowrap;
  user-select: none;
  color: #fff;
}

@media (max-width: 768px) {
  .tiptap-editor :deep(.tiptap) { padding: 32px; }
}
</style>
