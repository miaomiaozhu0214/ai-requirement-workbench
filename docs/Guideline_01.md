# Guideline_01 - 全量功能库

更新时间：2026-06-16

## 功能清单

| 功能 | 前端路由 | 使用角色 | 说明 |
|---|---|---|---|
| 全量功能库 | `/feature-library` | 内部产品、研发、测试 | 按产品线维护功能树、结构化内容块和节点变更历史 |

本阶段只实现功能树与结构化信息管理，不接入 AI 自动解析、文档处理流水线、Cursor/Kimi 问答、向量数据库或图片上传。

## 文件与目录

| 路径 | 说明 |
|---|---|
| `backend/src/main/resources/db/migration/V8__add_feature_library_tables.sql` | 新增功能节点、内容块、变更历史三张表 |
| `backend/src/main/java/com/example/airequirementworkbench/feature/entity` | 功能库 JPA 实体 |
| `backend/src/main/java/com/example/airequirementworkbench/feature/repository` | 功能库 Repository 查询与排序辅助 |
| `backend/src/main/java/com/example/airequirementworkbench/feature/service/FeatureLibraryService.java` | 功能树查询、新增、编辑、移动、软删除、内容块维护和历史写入 |
| `backend/src/main/java/com/example/airequirementworkbench/feature/controller/FeatureLibraryController.java` | 功能库 REST API，统一返回 `ApiResponse` |
| `frontend/src/api/featureLibraryApi.ts` | 前端功能库 API 封装 |
| `frontend/src/views/FeatureLibraryView.vue` | 功能库管理页面 |
| `frontend/src/types/index.ts` | 功能节点、内容块、历史记录类型定义 |
| `frontend/src/router/index.ts` | 新增 `/feature-library` 路由 |
| `frontend/src/App.vue` | 侧边栏新增“全量功能库”入口 |

## API 清单

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/feature-library/tree?productLineId=&keyword=` | 查询产品线功能树，过滤软删除节点和内容块 |
| POST | `/api/feature-library/nodes` | 新增根节点或子节点 |
| PUT | `/api/feature-library/nodes/{id}` | 编辑节点名称、描述、类型，可调整父节点 |
| PUT | `/api/feature-library/nodes/{id}/move` | 上移、下移或跨父节点移动并重排同级 `sort_order` |
| DELETE | `/api/feature-library/nodes/{id}` | 软删除节点及所有子孙节点 |
| GET | `/api/feature-library/nodes/{id}/history` | 查询节点变更历史，按时间倒序 |
| POST | `/api/feature-library/nodes/{id}/content-blocks` | 新增结构化内容块 |
| PUT | `/api/feature-library/content-blocks/{blockId}` | 编辑结构化内容块 |
| DELETE | `/api/feature-library/content-blocks/{blockId}` | 软删除结构化内容块 |

## 状态机

### feature_node.status

| 状态 | 业务含义 | 进入时机 |
|---|---|---|
| `added` | 新增节点 | 创建功能节点 |
| `modified` | 已修改 | 编辑基础信息或移动排序/父节点 |
| `deleted` | 已删除 | 软删除当前节点及其子孙节点 |
| `unchanged` | 未变化 | 预留给后续导入/对比场景，本阶段不会主动设置 |

### feature_history.operation_type

| 操作 | 业务含义 |
|---|---|
| `added` | 新增功能节点 |
| `modified` | 修改节点名称、描述或类型 |
| `deleted` | 软删除节点 |
| `moved` | 调整父节点或同级排序 |

### feature_content_block.block_type

| 类型 | 业务含义 |
|---|---|
| `overview` | 功能概述 |
| `rule` | 规则说明 |
| `field` | 字段清单 |
| `api` | 接口说明 |
| `screenshot` | 截图或页面说明 |

## 数据库字段

### feature_node

| 字段 | 说明 |
|---|---|
| `id` | 主键，沿用当前项目 `IdGenerator` 生成的 bigint ID |
| `product_line_id` | 归属产品线，复用已有 `product_line` 主数据 |
| `parent_id` | 父节点 ID，为空表示根节点 |
| `name` | 节点名称，同一产品线同一父节点下未删除节点不可重复 |
| `description` | 节点描述 |
| `node_type` | 节点类型：`module` / `feature` |
| `status` | 节点状态：`added` / `modified` / `deleted` / `unchanged` |
| `sort_order` | 同级排序，从 0 开始重排 |
| `deleted` | 软删除标记 |
| `created_at` / `updated_at` | 创建与更新时间 |

### feature_content_block

| 字段 | 说明 |
|---|---|
| `id` | 主键，沿用当前项目 `IdGenerator` 生成的 bigint ID |
| `feature_id` | 所属功能节点 ID |
| `block_type` | 内容块类型：`overview` / `rule` / `field` / `api` / `screenshot` |
| `title` | 内容块标题 |
| `content` | 结构化内容正文 |
| `metadata` | JSONB 元数据，用于保存字段清单等可结构化信息 |
| `source_ref` | 来源引用，例如文档章节、接口编号或页面说明 |
| `sort_order` | 节点内内容块排序 |
| `deleted` | 软删除标记 |
| `created_at` / `updated_at` | 创建与更新时间 |

### feature_history

| 字段 | 说明 |
|---|---|
| `id` | 主键，沿用当前项目 `IdGenerator` 生成的 bigint ID |
| `feature_id` | 对应功能节点 ID |
| `operation_type` | 操作类型：`added` / `modified` / `deleted` / `moved` |
| `description` | 变更说明 |
| `operator_id` | 操作人 ID，本阶段沿用当前项目 mock user 配置 |
| `created_at` | 变更时间 |

## 关键规则

- 查询功能树时只返回 `deleted=false` 的节点和内容块。
- 新增节点时，`sort_order` 使用当前同级最大值 + 1。
- 移动节点时，禁止移动到自己或自己的子孙节点下。
- 移动节点后，目标父节点下的同级节点会按 0 开始重排；跨父节点移动时，原父节点同级节点也会重排。
- 删除节点采用软删除，会同时软删除所有子孙节点并写入变更历史。
- 内容块操作暂不写入 `feature_history`，避免把结构化说明编辑和功能树结构变更混在一起。
