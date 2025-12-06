# CoDoc - 实时协作编辑器

一个基于 WebSocket 的分布式实时协作文档系统。

GitHub 仓库: https://github.com/dangzitou/collaborative-editor

---

## 目录

1. [项目结构](#项目结构)
2. [技术栈](#技术栈)
3. [环境要求](#环境要求)
4. [快速开始](#快速开始)
5. [后端开发指南](#后端开发指南)
6. [前端开发指南](#前端开发指南)
7. [Nginx 部署指南](#nginx-部署指南)
8. [WebSocket API](#websocket-api)
9. [协作开发流程](#协作开发流程)

---

## 项目结构

```
collaborative-editor/
├── server/                 # 后端服务 (Spring Boot)
│   ├── src/
│   │   └── main/
│   │       ├── java/       # Java 源码
│   │       └── resources/  # 配置文件
│   ├── pom.xml
│   └── .gitignore
├── web/                    # 前端客户端 (Vite + Vue 3)
│   ├── src/
│   │   ├── composables/    # 组合式函数
│   │   ├── App.vue         # 主组件
│   │   ├── style.css       # 全局样式
│   │   └── main.js         # 入口文件
│   ├── dist/               # 构建产物（需 npm run build 生成）
│   ├── package.json
│   └── .gitignore
├── nginx/                  # Nginx 配置和运行目录
│   ├── conf/
│   │   ├── nginx.conf      # Nginx 配置文件
│   │   └── mime.types      # MIME 类型配置（需下载）
│   ├── logs/               # 日志目录
│   ├── temp/               # 临时文件目录
│   ├── html/               # 备用静态文件目录
│   ├── nginx.exe           # Nginx 主程序（需下载）
│   ├── start.bat           # 启动脚本
│   ├── stop.bat            # 停止脚本
│   └── reload.bat          # 重载配置脚本
└── README.md
```

---

## 技术栈

### 后端
- Java 21
- Spring Boot 3.2.5
- WebSocket (JSR-356 / Jakarta WebSocket)
- Jackson JSON

### 前端
- Vite 7.x
- Vue 3 (Composition API)
- 原生 CSS

### 部署
- Nginx（静态文件服务 + WebSocket 代理 + 负载均衡）

---

## 环境要求

### 必需

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 21+ | 后端运行环境 |
| Node.js | 18+ | 前端构建工具 |
| npm | 9+ | 包管理器 |

### 可选

| 工具 | 说明 |
|------|------|
| IntelliJ IDEA | 推荐的 Java IDE |
| VS Code | 推荐的前端编辑器 |
| Nginx | 生产环境部署 |
| Git | 版本控制 |

---

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/dangzitou/collaborative-editor.git
cd collaborative-editor
```

### 2. 启动后端

```bash
cd server
./mvnw spring-boot:run
```

后端服务将在 http://localhost:8080 启动。

### 3. 启动前端（开发模式）

```bash
cd web
npm install
npm run dev
```

前端将在 http://localhost:5173 启动，支持热更新。

### 4. 测试协作功能

1. 打开浏览器访问 http://localhost:5173
2. 点击右上角调试按钮打开调试面板
3. 输入文档 ID（如 `doc-001`），点击连接
4. 打开新标签页，使用相同的文档 ID 连接
5. 在任意一个标签页输入内容，另一个标签页会实时同步

---

## 后端开发指南

### 使用 IntelliJ IDEA 运行

1. 打开 IDEA，选择 `File` -> `Open`
2. 选择 `collaborative-editor/server` 目录
3. 等待 Maven 依赖下载完成
4. 找到 `CollabEditorApplication.java` 文件
5. 右键点击，选择 `Run 'CollabEditorApplication'`

或者点击类名旁边的绿色运行按钮。

### 使用命令行运行

```bash
cd server

# Windows
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

### 打包部署

```bash
cd server
./mvnw clean package -DskipTests

# 运行 jar 包
java -jar target/collaborative-editor-1.0-SNAPSHOT.jar
```

### 核心代码文件

| 文件 | 说明 |
|------|------|
| `src/main/java/.../CollabEditorApplication.java` | 应用入口 |
| `src/main/java/.../ws/EditorServer.java` | WebSocket 服务端点 |
| `src/main/java/.../ws/Message.java` | 消息模型 |
| `src/main/java/.../config/WebSocketConfig.java` | WebSocket 配置 |
| `src/main/resources/application.properties` | 应用配置 |

### 配置说明

`application.properties` 配置项：

```properties
server.address=0.0.0.0    # 监听地址
server.port=8080          # 监听端口
```

---

## 前端开发指南

### 开发模式

开发时使用 Vite 开发服务器，支持热更新：

```bash
cd web
npm install    # 首次运行需要安装依赖
npm run dev    # 启动开发服务器
```

访问 http://localhost:5173

修改代码后浏览器会自动刷新。

### 构建生产版本

```bash
cd web
npm run build
```

构建产物输出到 `web/dist` 目录。

### 代码修改后的流程

1. 开发模式（推荐）：
   - 使用 `npm run dev`，修改代码后自动刷新

2. 生产模式（Nginx 部署）：
   - 修改代码后运行 `npm run build`
   - 刷新浏览器（Ctrl+F5 清除缓存）

### 核心代码文件

| 文件 | 说明 |
|------|------|
| `src/main.js` | 应用入口 |
| `src/App.vue` | 主组件（包含完整界面） |
| `src/composables/useWebSocket.js` | WebSocket 连接管理 |
| `src/style.css` | 全局样式 |

---

## Nginx 部署指南

### 下载 Nginx

1. 访问 http://nginx.org/en/download.html
2. 下载 Windows 版本（如 `nginx-1.24.0.zip`）
3. 解压后，复制以下文件到 `nginx/` 目录：
   - `nginx.exe` -> `nginx/nginx.exe`
   - `conf/mime.types` -> `nginx/conf/mime.types`

### 目录结构

确保 `nginx/` 目录结构如下：

```
nginx/
├── nginx.exe           # Nginx 主程序（需下载）
├── conf/
│   ├── nginx.conf      # 配置文件（已存在）
│   └── mime.types      # MIME 类型（需下载）
├── logs/               # 日志目录
├── temp/               # 临时文件目录
├── start.bat           # 启动脚本
├── stop.bat            # 停止脚本
└── reload.bat          # 重载配置
```

### 启动 Nginx

方法一：双击 `nginx/nginx.exe`

方法二：双击 `nginx/start.bat`

方法三：命令行

```bash
cd nginx
.\nginx.exe
```

### 停止 Nginx

```bash
cd nginx
.\nginx.exe -s stop
```

或双击 `nginx/stop.bat`

### 重载配置

修改 `nginx/conf/nginx.conf` 后：

```bash
cd nginx
.\nginx.exe -s reload
```

或双击 `nginx/reload.bat`

### 访问地址

启动 Nginx 后访问 http://localhost

### 配置说明

`nginx/conf/nginx.conf` 关键配置：

```nginx
# 后端服务器组（负载均衡）
upstream codoc_backend {
    ip_hash;                    # WebSocket 会话保持
    server 127.0.0.1:8080;      # 后端服务地址
}

server {
    listen 80;                  # 监听端口

    # 前端静态文件
    location / {
        root ../web/dist;       # 指向构建产物目录
        try_files $uri $uri/ /index.html;
    }

    # WebSocket 代理
    location /editor/ {
        proxy_pass http://codoc_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### 负载均衡扩展

添加多个后端实例：

```nginx
upstream codoc_backend {
    ip_hash;
    server 127.0.0.1:8080;
    server 127.0.0.1:8081;
    server 127.0.0.1:8082;
}
```

---

## WebSocket API

### 连接端点

```
ws://localhost:8080/editor/{docId}
```

`{docId}` 是文档 ID，相同 ID 的用户共享同一文档。

### 消息格式

所有消息使用 JSON 格式：

```json
{
  "type": "EDIT",
  "sender": "用户名",
  "data": "文档内容"
}
```

### 消息类型

| 类型 | 方向 | 说明 |
|------|------|------|
| EDIT | 客户端 -> 服务器 -> 其他客户端 | 编辑内容广播 |
| SYNC | 服务器 -> 客户端 | 新用户加入时同步当前内容 |

### 通信流程

1. 客户端连接到 `/editor/{docId}`
2. 如果文档已有内容，服务器发送 SYNC 消息
3. 客户端编辑时发送 EDIT 消息
4. 服务器将 EDIT 消息广播给同文档的其他用户
5. 客户端断开时自动从会话中移除

---

## 协作开发流程

### Git 工作流

```bash
# 1. 拉取最新代码
git pull origin master

# 2. 创建功能分支
git checkout -b feature/your-feature

# 3. 开发并提交
git add .
git commit -m "描述你的修改"

# 4. 推送分支
git push origin feature/your-feature

# 5. 创建 Pull Request
```

### 开发环境搭建步骤

1. 克隆项目
   ```bash
   git clone https://github.com/dangzitou/collaborative-editor.git
   ```

2. 安装后端依赖（IDEA 自动完成）

3. 安装前端依赖
   ```bash
   cd web
   npm install
   ```

4. 下载 Nginx（可选，用于生产模式测试）

### 日常开发命令

| 任务 | 命令 |
|------|------|
| 启动后端 | IDEA 运行 或 `cd server && ./mvnw spring-boot:run` |
| 启动前端（开发） | `cd web && npm run dev` |
| 构建前端 | `cd web && npm run build` |
| 启动 Nginx | `cd nginx && .\nginx.exe` |
| 停止 Nginx | `cd nginx && .\nginx.exe -s stop` |

### 注意事项

1. 后端代码修改后需要重启服务
2. 前端开发模式下修改代码会自动刷新
3. 使用 Nginx 部署时，前端代码修改后需要运行 `npm run build`
4. 多人协作时注意拉取最新代码

---

## License

MIT