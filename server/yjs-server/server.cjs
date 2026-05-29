const http = require('http')
const WebSocket = require('ws')
const { setupWSConnection } = require('y-websocket/bin/utils')

const PORT = process.env.YJS_PORT || 1234

const server = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'text/plain' })
  res.end('CoDoc Yjs WebSocket Server')
})

const wss = new WebSocket.Server({ noServer: true })

server.on('upgrade', (request, socket, head) => {
  // 可选：在此验证 ticket（从 URL query 参数获取）
  // 当前实现：信任 Nginx 代理层的认证，直接允许连接
  const url = new URL(request.url, `http://${request.headers.host}`)
  const docId = url.pathname.slice(1) // 去掉前导 /

  wss.handleUpgrade(request, socket, head, (ws) => {
    wss.emit('connection', ws, request)
  })
})

wss.on('connection', (conn, req) => {
  const url = new URL(req.url, `http://${req.headers.host}`)
  const docName = url.pathname.slice(1) || 'default'
  setupWSConnection(conn, req, { docName })
  console.log(`[Yjs] Client connected to room: ${docName}`)
})

server.listen(PORT, () => {
  console.log(`[Yjs] WebSocket server running on ws://localhost:${PORT}`)
})
