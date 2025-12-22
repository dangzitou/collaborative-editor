import { ref, readonly } from 'vue'

/**
 * WebSocket 连接管理 composable
 */
export function useWebSocket() {
  const socket = ref(null)
  const isConnected = ref(false)
  const messages = ref([])
  const connectionError = ref(null)
  let heartbeatTimer = null

  let messageId = 0
  const pingDelay = ref(null) // ms
  let lastPingSentAt = 0

  /**
   * 添加日志消息
   */
  function addMessage(type, content, raw = null) {
    messages.value.push({
      id: ++messageId,
      type, // 'sent' | 'received' | 'system' | 'error'
      content,
      raw,
      timestamp: new Date().toLocaleTimeString()
    })
  }

  /**
   * 连接 WebSocket
   * @param {string} wsUrl - 完整的 WebSocket URL
   * @param {object} options - 配置项 { onClose: (event) => {} }
   */
  function connect(wsUrl, options = {}) {
    if (socket.value && socket.value.readyState === WebSocket.OPEN) {
      addMessage('system', '已经连接，请先断开')
      return
    }

    addMessage('system', `正在连接: ${wsUrl}`)
    connectionError.value = null

    try {
      socket.value = new WebSocket(wsUrl)

      socket.value.onopen = () => {
        isConnected.value = true
        addMessage('system', 'WebSocket 连接成功')
        startHeartbeat()
      }

      socket.value.onmessage = (event) => {
        let displayContent = event.data
        let parsed = null
        
        try {
          parsed = JSON.parse(event.data)
          displayContent = JSON.stringify(parsed, null, 2)
        } catch (e) {
          // 保持原始文本
        }

        // 更宽松地识别 PONG 响应（兼容多种后端实现）
        let handledAsPong = false
        try {
          // parsed 对象优先判断
          if (parsed) {
            // 常见：后端返回 type: 'PONG'
            if (parsed.type === 'PONG') handledAsPong = true
            // 有的实现会回传 type:'PING' 但 data:'pong' 或类似
            if (!handledAsPong && parsed.type === 'PING' && parsed.sender === 'server' && typeof parsed.data === 'string' && /pong/i.test(parsed.data)) handledAsPong = true
          }

          // 原始文本也可能包含 pong 字样
          if (!handledAsPong && typeof displayContent === 'string' && /\bpong\b/i.test(displayContent)) handledAsPong = true

          if (handledAsPong) {
            if (lastPingSentAt) {
              pingDelay.value = Date.now() - lastPingSentAt
              lastPingSentAt = 0
            }
            addMessage('system', `PONG received, 延迟 ${pingDelay.value ?? '-'} ms`)
            console.debug('[WebSocket] PONG detected, delay:', pingDelay.value)
          } else {
            addMessage('received', displayContent, parsed || event.data)
          }
        } catch (e) {
          // 兜底：正常展示
          addMessage('received', displayContent, parsed || event.data)
        }
      }

      socket.value.onclose = (event) => {
        isConnected.value = false
        stopHeartbeat()
        addMessage('system', `连接关闭 (code: ${event.code}, reason: ${event.reason || '无'})`)
        socket.value = null
        
        if (options.onClose) {
          options.onClose(event)
        }
      }

      socket.value.onerror = () => {
        connectionError.value = '连接失败'
        addMessage('error', 'WebSocket 连接错误')
      }

    } catch (error) {
      connectionError.value = error.message
      addMessage('error', `连接失败: ${error.message}`)
    }
  }

  /**
   * 断开连接
   */
  function disconnect() {
    stopHeartbeat()
    if (socket.value) {
      socket.value.close()
      socket.value = null
    }
  }

  /**
   * 启动心跳
   */
  function startHeartbeat() {
    stopHeartbeat()
    // 立即发送一次 ping，后续按间隔发送
    const doPing = () => {
      if (socket.value && socket.value.readyState === WebSocket.OPEN) {
        const message = { type: 'PING', sender: 'system', data: 'ping' }
        try {
          lastPingSentAt = Date.now()
          const text = JSON.stringify(message)
          socket.value.send(text)
          addMessage('sent', text)
          console.debug('[WebSocket] PING sent')
        } catch (e) {
          lastPingSentAt = 0
        }
      }
    }

    doPing()
    heartbeatTimer = setInterval(doPing, 5000)
  }

  /**
   * 停止心跳
   */
  function stopHeartbeat() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  /**
   * 发送 JSON 消息
   */
  function sendJson(type, sender, data) {
    if (!socket.value || socket.value.readyState !== WebSocket.OPEN) {
      addMessage('error', '未连接到服务器')
      return false
    }

    const message = { type, sender, data }
    const json = JSON.stringify(message)
    socket.value.send(json)
    addMessage('sent', JSON.stringify(message, null, 2))
    return true
  }

  /**
   * 发送原始文本
   */
  function sendRaw(text) {
    if (!socket.value || socket.value.readyState !== WebSocket.OPEN) {
      addMessage('error', '未连接到服务器')
      return false
    }

    socket.value.send(text)
    addMessage('sent', text)
    return true
  }

  /**
   * 清空消息
   */
  function clearMessages() {
    messages.value = []
    messageId = 0
  }

  /**
   * 导出消息日志
   */
  function exportMessages() {
    const content = messages.value.map(m => 
      `[${m.timestamp}] [${m.type.toUpperCase()}]\n${m.content}`
    ).join('\n\n---\n\n')
    
    const blob = new Blob([content], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `codoc-log-${Date.now()}.txt`
    a.click()
    URL.revokeObjectURL(url)
  }

  return {
    isConnected: readonly(isConnected),
    messages: readonly(messages),
    connectionError: readonly(connectionError),
    pingDelay: readonly(pingDelay),
    connect,
    disconnect,
    sendJson,
    sendRaw,
    clearMessages,
    exportMessages
  }
}

