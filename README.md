# AI需求工作台 MVP

AI需求工作台 MVP 面向需求录入、澄清、识别和结构化管理。当前已完成从网页对话到候选需求卡片、正式需求卡片入库、需求池查询、AI 配置与 Trace 追溯的闭环。

## 已实现能力

- Vue 3 + TypeScript + Vite 前端。
- Spring Boot 3 + Java 17 后端。
- PostgreSQL 数据库与 Flyway migration。
- 会话列表、新建会话、消息发送、消息展示、删除会话。
- AI Router 前置链路：每轮用户输入先进入 `intent_router`，再由后端 Orchestrator 编排后续能力。
- AI 能力支持配置执行顺序：`executionOrder` 越小越先执行，顺序相同时保留 Router 原始动作顺序。
- 真实 LLM 接入与配置，默认使用 `openai`；Mock 仅在显式配置 `AI_PROVIDER=mock` 时启用。
- 模型配置、Prompt 模板配置、AI 能力配置页面。
- 默认 Prompt 模板：`default_intent_router`、`default_requirement_extract`、`default_completeness_check`、`default_reply_generate`、`default_card_generate`。
- 候选需求创建、更新、完整度检查、缺失信息和建议问题展示。
- 候选需求生成正式需求卡片，并保存到需求池。
- AI Trace 记录与页面展示，包括模型、Prompt 编码、版本、输入摘要、输出 JSON、耗时、Token 和错误信息。
- 前端源码清洁检查，防止 `frontend/src` 下混入 `.js` 或 `.js.map` 编译产物。

## 目录结构

```text
backend/            Spring Boot 后端工程
frontend/           Vue 前端工程
docs/mvp/           MVP 设计文档
docs/test-cases/    验收用例文档
start-all.sh        一键启动数据库、后端和前端
```

## 一键启动

默认启动真实 LLM 模式：

```bash
./start-all.sh
```

显式启用 Mock 模式：

```bash
AI_PROVIDER=mock ./start-all.sh
```

默认访问地址：

```text
前端：http://127.0.0.1:5174
后端：http://127.0.0.1:8080
```

## 数据库

默认 PostgreSQL 连接信息：

```text
DB_URL=jdbc:postgresql://localhost:5432/ai_requirement_workbench
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

如需单独启动数据库：

```bash
docker compose up -d postgres
```

## 手动启动

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

## 核心验证流程

1. 打开 `http://127.0.0.1:5174`。
2. 进入“需求对话”，新建会话。
3. 输入：`客户希望合同列表支持导出`。
4. 系统应先调用 AI Router，并生成候选需求卡片。
5. 继续输入：`导出当前筛选结果，只允许运营人员，字段和列表一致`。
6. 同一候选需求应被更新，完整度和已识别信息提升。
7. 点击“生成需求卡片”，确认后生成正式需求。
8. 进入“需求池”，应能查询到正式需求卡片。
9. 进入“AI Trace”，应能看到 `intent_router`、`requirement_extract`、`completeness_check`、`reply_generate`、`card_generate` 等调用记录。

## 验证命令

前端：

```bash
cd frontend
npm run build
```

后端：

```bash
mvn test
```

## MVP 边界

当前 MVP 聚焦需求相关能力，不实现项目管理、任务排期、工时管理、代码知识库、外部聊天工具接入。PRD 结构化生成和 HTML 原型生成属于后续阶段能力。
