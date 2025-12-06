# CoDoc - 实时协作编辑器

一个基于 WebSocket 的分布式实时协作文档系统。

## 🏗️ 项目结构

```
collaborative-editor/
├── server/                 # 后端服务 (Spring Boot)
│   ├── src/
│   │   └── main/
│   │       ├── java/       # Java 源码
│   │       └── resources/  # 配置文件
│   └── pom.xml
└── web/                    # 前端调试客户端 (Vite + Vue 3)
    ├── src/
    │   ├── components/     # Vue 组件
    │   ├── composables/    # 组合式函数
    │   ├── App.vue
    │   └── main.js
    └── package.json
```

## 🛠️ 技术栈

### 后端
- **Java 21**
- **Spring Boot 3.2.5**
- **WebSocket (JSR-356)**
- **Jackson** - JSON 序列化

### 前端
- **Vite 7.x** - 构建工具
- **Vue 3** - 前端框架
- **Composition API** - 组合式 API

## 🚀 快速开始

### 启动后端服务

```bash
cd server
./mvnw spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

### 启动前端调试客户端

```bash
cd web
npm install
npm run dev
```

前端将在 `http://localhost:5173` 启动。

## 📡 WebSocket API

### 连接端点

```
ws://localhost:8080/editor/{docId}
```

- `{docId}`: 文档 ID，用于区分不同的协作文档

### 消息格式

所有消息使用 JSON 格式：

```json
{
  "type": "EDIT | SYNC",
  "sender": "用户标识",
  "data": "文档内容"
}
```

### 消息类型

| 类型 | 方向 | 说明 |
|------|------|------|
| `EDIT` | 客户端 → 服务器 | 用户编辑文档内容 |
| `SYNC` | 服务器 → 客户端 | 服务器同步文档内容给新加入的用户 |

### 通信流程

1. **连接建立**: 客户端连接到 `/editor/{docId}`
2. **内容同步**: 如果文档已有内容，服务器发送 `SYNC` 消息
3. **实时编辑**: 客户端发送 `EDIT` 消息，服务器广播给同文档的其他用户
4. **断开连接**: 客户端断开时自动从文档会话中移除

## 🔧 调试客户端功能

- ✅ 连接/断开 WebSocket
- ✅ 自定义服务器地址和文档 ID
- ✅ 实时编辑器 - 内容自动同步
- ✅ 发送 JSON 格式消息
- ✅ 发送原始文本消息
- ✅ 消息日志 - 查看所有通信记录
- ✅ 导出日志功能
- ✅ 支持暗色模式

## Nginx 部署

### 构建前端

```bash
cd web
npm run build
```

构建产物在 `web/dist` 目录。

### Nginx 配置

项目提供了两个配置文件：

| 文件 | 用途 |
|------|------|
| `nginx/nginx.conf` | 生产环境配置 |
| `nginx/nginx-dev.conf` | 开发环境配置 |

### 生产环境部署

1. 修改 `nginx/nginx.conf` 中的路径：

```nginx
root /your/path/to/collaborative-editor/web/dist;
```

2. 复制配置到 nginx：

```bash
# Linux/Mac
sudo cp nginx/nginx.conf /etc/nginx/conf.d/codoc.conf
sudo nginx -s reload

# Windows
copy nginx\nginx.conf C:\nginx\conf\codoc.conf
nginx -s reload
```

3. 访问 `http://localhost`

### 负载均衡扩展

在 `nginx.conf` 的 `upstream` 块中添加更多后端服务器：

```nginx
upstream codoc_backend {
    ip_hash;  # WebSocket 会话保持

    server 127.0.0.1:8080 weight=1;
    server 127.0.0.1:8081 weight=1;
    server 127.0.0.1:8082 weight=1;
}
```

负载均衡策略：
- `ip_hash` - 基于客户端 IP（WebSocket 推荐）
- `least_conn` - 最少连接数
- 默认轮询 (round-robin)

## 开发说明

### 后端开发

WebSocket 服务端点定义在：
```
server/src/main/java/org/example/collaborative_editor/ws/EditorServer.java
```

消息模型：
```
server/src/main/java/org/example/collaborative_editor/ws/Message.java
```

### 前端开发

主要组件：
- `ConnectionPanel.vue` - 连接配置面板
- `MessagePanel.vue` - 消息发送面板
- `EditorPanel.vue` - 实时编辑器
- `LogPanel.vue` - 消息日志面板

WebSocket 逻辑封装在：
```
web/src/composables/useWebSocket.js
```

## 📄 License

MIT