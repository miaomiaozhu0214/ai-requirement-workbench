<<<<<<< HEAD
# ai-requirement-workbench
一个基于ai驱动流程及分析的产品需求管理平台，涵盖了需求录入及澄清、需求生成及管理、项目管理等功能
=======
# AI需求工作台 MVP

第一阶段实现范围：网页对话、Mock AI 需求识别、候选需求卡片、候选需求转正式需求卡片、需求池查看。

## 目录

```text
backend/   Spring Boot 3 + Java 17 后端
frontend/  Vue 3 + TypeScript + Vite 前端
docs/mvp/  MVP设计文档
```

## 启动数据库

```bash
docker compose up -d postgres
```

默认连接信息：

```text
DB_URL=jdbc:postgresql://localhost:5432/ai_requirement_workbench
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

## 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认端口：`http://localhost:8080`。

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`。

## 验证闭环

1. 打开 `http://localhost:5173`。
2. 进入“需求对话”。
3. 新建会话。
4. 输入：`客户希望合同列表可以导出，而且详情页要展示审批记录。`
5. 右侧应出现候选需求卡片。
6. 继续输入：`导出范围是当前筛选结果，仅运营人员可导出，字段与列表一致，单次最多5000条，需要记录日志。`
7. 候选卡片完整度和已识别信息应更新。
8. 点击“生成需求卡片”，补充产品线、模块、需求类型并确认。
9. 进入“需求池”，应看到正式需求卡片。

## 验证命令

```bash
cd frontend
npm run build
```

```bash
cd backend
mvn test
```

## 第一阶段边界

本阶段不实现项目管理、工时管理、代码知识库、外部聊天工具接入、PRD生成、HTML原型生成、真实大模型接入。
>>>>>>> 10c5974 (feat: implement AI requirement workbench MVP)
