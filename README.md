# Yu AI Agent

一个基于 Spring Boot + Spring AI 的 AI Agent 项目，用户可以针对有关恋爱方面问题进行提问，此agent支持大模型对话、工具调用、RAG 知识库问答、联网搜索、文件操作、PDF 生成，以及前端聊天界面。

## 项目亮点

- 基于 Spring AI 接入 DashScope/Qwen 大模型
- 实现 Agent 执行流程，包括 ReAct、工具调用、多轮对话记忆
- 支持 RAG 知识库问答，可接入 PgVector 向量数据库
- 封装多种工具能力：网页搜索、网页抓取、文件操作、PDF 生成、资源下载等
- 提供 MCP Server 图片搜索工具，支持 Agent 扩展调用
- 前后端分离，前端使用 Vue + Vite 实现聊天交互界面
- 敏感配置使用环境变量管理，避免 API Key 泄露

## 技术栈

后端：

- Java 21
- Spring Boot
- Spring AI
- DashScope / Qwen
- PgVector
- Maven

前端：

- Vue
- Vite
- JavaScript

其他：

- MCP Server
- PostgreSQL
- Docker

## 项目结构

```text
yu-ai-agent
├── src/main/java/com/yupi/yuaiagent
│   ├── agent              # Agent 核心逻辑
│   ├── app                # AI 应用入口
│   ├── controller         # 接口层
│   ├── rag                # RAG 知识库相关
│   └── tools              # 工具调用能力
├── src/main/resources
│   ├── application.yml
│   ├── application-prod.yml
│   └── document           # 知识库文档
├── yu-ai-agent-frontend   # 前端项目
└── yu-image-search-mcp-server # 图片搜索 MCP 服务
