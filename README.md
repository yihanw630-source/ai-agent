# AI Agent

基于 Spring Boot + Spring AI 的 AI Agent 项目，用户可以针对有关恋爱问题进行询问，此agent支持大模型对话、工具调用、RAG 知识库问答、联网搜索、文件操作、PDF 生成，并提供 Vue 前端聊天界面和图片搜索 MCP 服务。

## 技术栈

- Java 17
- Spring Boot
- Spring AI / DashScope / Qwen
- PostgreSQL / PgVector
- Vue 3 / Vite
- MCP Server

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- PostgreSQL，可选
- DashScope API Key

## 配置环境变量

项目不会提交真实 API Key。首次运行前，请复制 `.env.example` 为 `.env`，并填写自己的配置：

```bash
cp .env.example .env
```

Windows CMD 可使用：

```cmd
copy .env.example .env
```

`.env` 示例：

```env
DASHSCOPE_API_KEY=你的 DashScope API Key
DB_URL=jdbc:postgresql://localhost:5432/yu_ai_agent
DB_USERNAME=my_user
DB_PASSWORD=你的数据库密码
SEARCH_API_KEY=你的搜索 API Key
AMAP_MAPS_API_KEY=你的高德地图 API Key
PEXELS_API_KEY=你的 Pexels API Key
```

如果只想先启动基础项目，可以先保持 PgVector 关闭：

```yaml
app:
  rag:
    pgvector:
      enabled: false
    initialize-on-startup: false
```

## 启动后端

在项目根目录执行：

```bash
./mvnw spring-boot:run
```

Windows CMD：

```cmd
mvnw.cmd spring-boot:run
```

后端默认地址：

```text
http://localhost:8123/api
```

接口文档：

```text
http://localhost:8123/api/swagger-ui.html
```

## 启动前端

进入前端目录：

```bash
cd yu-ai-agent-frontend
npm install
npm run dev
```

前端默认访问地址：

```text
http://localhost:5173
```

## 启动图片搜索 MCP 服务

如果需要使用图片搜索 MCP 工具，进入 MCP 服务目录：

```bash
cd yu-image-search-mcp-server
../mvnw spring-boot:run
```

Windows CMD：

```cmd
cd yu-image-search-mcp-server
..\mvnw.cmd spring-boot:run
```

MCP 服务默认地址：

```text
http://localhost:8127
```

## 常见问题

### API Key 未配置

如果未配置 `DASHSCOPE_API_KEY`、`SEARCH_API_KEY`、`PEXELS_API_KEY` 等变量，对应的大模型、联网搜索、图片搜索功能会调用失败。请先根据 `.env.example` 配置本地 `.env`。

### 数据库连接失败

如果本地没有 PostgreSQL，可以先关闭 PgVector 相关功能，只运行基础对话和工具调用功能。

### JAVA_HOME 未配置

如果启动时报 `JAVA_HOME environment variable is not defined correctly`，请安装 JDK 17+，并配置 `JAVA_HOME`。

## 核心代码入口

- Agent 主流程：`src/main/java/com/yupi/yuaiagent/agent`
- 工具注册：`src/main/java/com/yupi/yuaiagent/tools/ToolRegistration.java`
- RAG 配置：`src/main/java/com/yupi/yuaiagent/rag`
- 前端聊天页面：`yu-ai-agent-frontend/src/components/ChatRoom.vue`
- 图片搜索 MCP 服务：`yu-image-search-mcp-server`
