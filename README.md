# CoDoc - 实时协作编辑器

一个基于 WebSocket 的分布式实时协作文档系统。

GitHub 仓库: https://github.com/dangzitou/collaborative-editor

---

## 目录

1. [项目结构](#项目结构)
2. [技术栈](#技术栈)
3. [环境要求](#环境要求)
4. [数据存储配置](#数据存储配置)
5. [快速开始](#快速开始)
6. [后端开发指南](#后端开发指南)
7. [前端开发指南](#前端开发指南)
8. [Nginx 部署指南](#nginx-部署指南)
9. [WebSocket API](#websocket-api)
10. [REST API](#rest-api)
11. [协作开发流程](#协作开发流程)

---

## 项目结构

```
collaborative-editor/
├── server/                 # 后端服务 (Spring Boot)
│   ├── src/main/java/org/example/collaborative_editor/
│   │   ├── config/         # 全局配置 (WebSocket, Redis等)
│   │   ├── controller/     # REST API 控制器
│   │   ├── entity/         # 数据库实体类
│   │   ├── mapper/         # MyBatis Mapper 接口
│   │   ├── security/       # 安全配置与拦截器
│   │   ├── service/        # 业务逻辑层
│   │   ├── task/           # 定时任务 (Redis->MySQL同步)
│   │   ├── util/           # 工具类 (JWT等)
│   │   └── ws/             # WebSocket 服务端核心逻辑
│   ├── src/main/resources/
│   │   ├── mapper/         # MyBatis XML 文件
│   │   └── application.properties
│   └── pom.xml
├── web/                    # 前端客户端 (Vite + Vue 3)
│   ├── src/
│   │   ├── components/     # Vue UI 组件 (AuthModal等)
│   │   ├── composables/    # 组合式函数 (useWebSocket, useAuth)
│   │   ├── App.vue         # 主应用组件
│   │   ├── style.css       # 全局样式
│   │   └── main.js         # Vue 入口文件
│   ├── index.html          # HTML 模板
│   ├── vite.config.js      # Vite 配置
│   └── package.json
├── nginx/                  # Nginx 配置和运行目录
│   ├── conf/
│   │   ├── nginx.conf      # Nginx 配置文件
│   │   └── mime.types      # MIME 类型配置
│   ├── logs/               # 日志目录
│   ├── nginx.exe           # Nginx 主程序
│   └── start.bat           # 启动脚本
└── README.md
```

---

## 技术栈

### 后端
- Java 21
- Spring Boot 3.2.5
- WebSocket (JSR-356 / Jakarta WebSocket)
- Spring Security + JWT 认证
- MyBatis + MySQL 数据库
- Redis (缓存与实时数据缓冲)
- Jackson JSON
- SLF4J + Logback 日志

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
| Maven | 3.6+ | Java 项目构建工具 |
| Node.js | 18+ | 前端构建工具 |
| npm | 9+ | 包管理器 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 5.0+ | 缓存数据库 |

### Maven 安装说明

如果你的电脑未安装 Maven（运行 `mvn -v` 报错），请按照以下步骤安装：

1. **下载**: 访问 [Maven 官网](https://maven.apache.org/download.cgi) 下载 `Binary zip archive`。
2. **解压**: 将压缩包解压到任意目录（例如 `D:\maven`）。
3. **配置环境变量**:
   - 新建系统变量 `MAVEN_HOME`，值为解压目录。
   - 在 `Path` 变量中添加 `%MAVEN_HOME%\bin`。
4. **验证**: 打开新的 CMD/PowerShell，输入 `mvn -v`，显示版本号即成功。

### 可选

| 工具 | 说明 |
|------|------|
| IntelliJ IDEA | 推荐的 Java IDE |
| VS Code | 推荐的前端编辑器 |
| Nginx | 生产环境部署 |
| Git | 版本控制 |

---

## 数据存储配置

### 1. MySQL 配置

#### 1.1 启动 MySQL 服务

**注意：在连接数据库之前，必须确保 MySQL 服务已启动！**

- **Windows**:
  1. 按 `Win + R`，输入 `services.msc` 打开服务管理器。
  2. 找到 `MySQL` 服务（名称可能是 `MySQL80` 或 `MySQL`），右键点击“启动”。
  3. 或者以管理员身份打开 CMD/PowerShell，运行：`net start mysql`。

- **Linux/Mac**:
  ```bash
  sudo service mysql start
  # 或
  sudo systemctl start mysql
  ```

#### 1.2 连接并初始化

在 VS Code 终端（PowerShell）中输入：

```powershell
mysql -u root -p --default-character-set=utf8mb4
```

说明：
- `root` 替换为你自己的 MySQL 用户名
- 输入密码后进入 MySQL 命令行

#### 1.2 执行初始化脚本

在 MySQL 命令行中执行：

```sql
source server/src/main/resources/sql/init.sql
```

或者复制 `init.sql` 中的内容手动执行。

### 2. Redis 配置

本项目使用 Redis 作为实时文档内容的缓冲区（Write-Behind 模式），以提高写入性能。

#### 2.1 启动 Redis 服务

**注意：项目运行必须依赖 Redis，请务必先启动 Redis 服务！**

- **Windows**:
  1. 进入 Redis 安装目录。
  2. 双击 `redis-server.exe` 启动服务端（会出现一个黑窗口，**不要关闭**）。
  3. 或者在终端运行：`redis-server`。

- **Linux/Mac**:
  ```bash
  redis-server
  # 或后台启动
  redis-server --daemonize yes
  ```

默认端口为 `6379`。

#### 2.2 配置连接
在 `server/src/main/resources/application-dev.properties` (开发环境) 或 `application-prod.properties` (生产环境) 中配置：

```properties
# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=123456
spring.data.redis.database=1
```

### 3. 修改数据库连接配置

编辑 `server/src/main/resources/application-dev.properties`：

```properties
# 数据库连接配置
spring.datasource.url=jdbc:mysql://localhost:3306/codoc?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
spring.datasource.username=root
spring.datasource.password=1234
```

根据你的实际情况修改：
- `username`: 你的 MySQL 用户名
- `password`: 你的 MySQL 密码
- `url`: 如果端口不是 3306，需要修改

### 数据库表结构

#### 1. 用户表 (`user`)
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| username | VARCHAR(50) | 用户名 (唯一) |
| password | VARCHAR(255) | 密码 (加密) |
| nickname | VARCHAR(50) | 昵称 |
| email | VARCHAR(100) | 邮箱 (唯一) |
| avatar | VARCHAR(255) | 头像URL |
| status | TINYINT | 状态: 0-禁用, 1-正常 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### 2. 文档表 (`document`)
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| doc_id | VARCHAR(64) | 文档唯一标识 (唯一) |
| title | VARCHAR(255) | 文档标题 |
| content | LONGTEXT | 文档内容 |
| owner_id | BIGINT | 创建者ID |
| status | TINYINT | 状态: 0-删除, 1-正常 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| create_user | BIGINT | 创建人 |
| update_user | BIGINT | 修改人 |

#### 3. 协作者表 (`collaborator`)
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 |
| doc_id | VARCHAR(64) | 文档标识 |
| user_id | BIGINT | 用户ID |
| create_time | DATETIME | 创建时间 |

---

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/dangzitou/collaborative-editor.git
cd collaborative-editor
```

### 2. 配置数据库

参考上方 [数据库配置](#数据库配置) 章节，初始化数据库并修改连接配置。

### 3. 启动后端

确保你已安装 Maven 并配置了环境变量。

```bash
cd server
mvn spring-boot:run
```

> **注意**：
> - 如果你希望使用项目自带的 Maven Wrapper：
>   - **Windows (PowerShell)**: `.\mvnw spring-boot:run` (注意是反斜杠 `\` 且前面有 `.\`)
>   - **Linux/Mac**: `./mvnw spring-boot:run`
> - 如果报错 `CommandNotFound`，请检查是否已安装 Maven 或目录下是否存在 `mvnw` 文件。

后端服务将在 http://localhost:8080 启动。

### 4. 启动前端（开发模式）

```bash
cd web
npm install
npm run dev
```

前端将在 http://localhost:5173 启动，支持热更新。

### 5. 测试协作功能

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

### 说明

项目已包含完整的 Nginx 配置，克隆后无需额外配置，直接启动即可。

### 目录结构

```
nginx/
├── nginx.exe           # Nginx 主程序
├── conf/
│   ├── nginx.conf      # 服务器配置
│   └── mime.types      # MIME 类型
├── logs/               # 日志目录（首次运行自动创建）
├── temp/               # 临时目录（首次运行自动创建）
├── start.bat           # 启动脚本
├── stop.bat            # 停止脚本
├── reload.bat          # 重载配置
└── setup.bat           # 构建并复制前端文件
```

### 首次运行

首次运行需要创建运行时目录：

```bash
cd nginx
mkdir logs
mkdir temp
mkdir temp\client_body_temp temp\proxy_temp temp\fastcgi_temp temp\uwsgi_temp temp\scgi_temp
```

或者双击 `setup.bat` 自动完成。

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

## REST API

### 认证接口

#### 用户注册

```
POST /api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "password": "123456",
  "nickname": "测试用户"
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

#### 用户登录

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "123456"
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "avatar": null
  }
}
```

### 使用 Token

登录成功后，在后续请求中携带 Token：

```
Authorization: Bearer <token>
```

---

## 协作开发流程

### 团队分工

| 角色 | 负责内容 | 核心文件 |
|------|----------|----------|
| 后端核心 | OT/CRDT 算法、WebSocket 实时同步 | `EditorServer.java`、算法相关类 |
| 后端 CRUD | 用户/文档/协作者 API | `controller/`、`service/`、`mapper/` |
| 前端 | Vue 组件、UI 交互、API 对接 | `web/src/` |

### API 接口文档

接口文档位于 `docs/api.json`，使用 OpenAPI 3.0 格式。

导入到 Apifox：
1. 下载 Apifox: https://apifox.com
2. 创建项目 -> 导入数据 -> 选择 OpenAPI/Swagger
3. 上传 `docs/api.json` 文件
4. 导入后可查看所有接口，并使用 Mock 功能

接口开发状态：

| 模块 | 接口 | 状态 |
|------|------|------|
| 认证 | POST /api/auth/register | 已完成 |
| 认证 | POST /api/auth/login | 已完成 |
| 用户 | GET /api/user/info | 待开发 |
| 用户 | PUT /api/user/info | 待开发 |
| 文档 | GET /api/documents | 待开发 |
| 文档 | POST /api/documents | 待开发 |
| 文档 | GET /api/documents/{id} | 待开发 |
| 文档 | PUT /api/documents/{id} | 待开发 |
| 文档 | DELETE /api/documents/{id} | 待开发 |
| 协作 | GET /api/documents/{id}/collaborators | 待开发 |
| 协作 | POST /api/documents/{id}/collaborators | 待开发 |
| 协作 | DELETE /api/documents/{id}/collaborators/{userId} | 待开发 |

### 前后端并行开发

前端开发无需等待后端接口完成，使用 Apifox Mock 服务：

1. 在 Apifox 中打开接口
2. 点击「Mock」标签，复制 Mock URL
3. 前端调用 Mock URL 进行开发
4. 后端接口完成后，切换到真实 URL

前端 Mock 数据示例：

```javascript
// 开发时使用 Mock
const mockDocuments = [
  { id: 'doc-001', title: '项目需求', updatedAt: '2025-12-06' },
  { id: 'doc-002', title: '技术方案', updatedAt: '2025-12-05' }
]

// 接口完成后切换
// const { data } = await api.get('/api/documents')
```

### 后端 CRUD 开发指南

开发新接口的步骤：

1. 在 `dto/` 创建请求/响应 DTO
2. 在 `mapper/` 添加 Mapper 接口和 XML
3. 在 `service/` 添加 Service 接口和实现
4. 在 `controller/` 添加 Controller
5. 使用 `@Slf4j` 添加日志
6. 更新 `docs/api.json` 接口状态

代码规范：
- Controller 使用 `@Slf4j` 记录请求日志
- Service 使用 `@Slf4j` 记录业务逻辑日志
- 统一返回 `Result<T>` 格式
- 需要认证的接口从 SecurityContext 获取用户信息

示例：获取当前用户

```java
@GetMapping("/info")
public Result<User> getUserInfo() {
    UserPrincipal principal = (UserPrincipal) SecurityContextHolder
        .getContext().getAuthentication().getPrincipal();
    Long userId = principal.getUserId();
    // ... 查询用户信息
}
```

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

分支命名规范：
- `feature/xxx` - 新功能
- `fix/xxx` - Bug 修复
- `docs/xxx` - 文档更新

### 开发环境搭建步骤

1. 克隆项目
   ```bash
   git clone https://github.com/dangzitou/collaborative-editor.git
   ```

2. 配置数据库（参考 [数据库配置](#数据库配置)）

3. 安装后端依赖（IDEA 自动完成）

4. 安装前端依赖
   ```bash
   cd web
   npm install
   ```

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
5. 开发新接口前先在 `docs/api.json` 中定义接口规范

---

## 常见问题 (FAQ)

### 1. 内网穿透后访问显示 403 Forbidden

**现象**：启动 Nginx 后，将 `localhost:8080` 穿透到公网，其他设备访问显示 403。

**原因**：
- Nginx 监听 **80** 端口，负责提供前端页面和反向代理。
- 后端 (Spring Boot) 监听 **8080** 端口，只处理 API。
- 如果直接穿透 8080 端口，请求会绕过 Nginx 直接到达后端。后端没有配置静态页面，且可能有安全策略限制，导致 403 或 404。

**解决方法**：
- 修改内网穿透配置，将本地端口改为 **80**。
- 例如使用 cpolar/ngrok/frp 时，映射 `http 80`。

### 2. Windows 终端控制台中文乱码

**现象**：在 Windows PowerShell 中运行后端时，日志中的中文显示为乱码。

**解决方法**：

**方法一：强制 JVM 输出 GBK（推荐）**
顺应 Windows 默认的 GBK 编码，强制 Java 程序输出 GBK 日志。注意 PowerShell 中参数需要引号包裹：
```powershell
mvn spring-boot:run '-Dspring-boot.run.jvmArguments=-Dfile.encoding=GBK'
```

**方法二：修改终端编码**
先切换终端页码为 UTF-8，再运行：
```powershell
chcp 65001
mvn spring-boot:run
```

---

## License

MIT