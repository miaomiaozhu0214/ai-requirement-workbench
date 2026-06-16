# Guideline_02 - 产品线配置

更新时间：2026-06-16

## 功能清单

| 功能 | 前端路由 | 使用角色 | 说明 |
|---|---|---|---|
| 产品线配置 | `/product-lines` | 内部产品、研发、测试 | 维护全量功能库和需求分析使用的产品线基础信息 |

本阶段只迁移产品线基础配置，不迁移基线文档在线编辑、文档生成/下载、Neo4j 图谱清理、知识快照、AI/Kimi/Cursor 相关逻辑，也不扩展复杂权限模型。

## 文件与目录

| 路径 | 说明 |
|---|---|
| `backend/src/main/resources/db/migration/V9__extend_product_line_configuration.sql` | 在已有 `product_line` 表上补齐产品线配置字段 |
| `backend/src/main/java/com/example/airequirementworkbench/masterdata/entity/ProductLine.java` | 复用并扩展产品线实体 |
| `backend/src/main/java/com/example/airequirementworkbench/masterdata/controller/ProductLineConfigController.java` | 产品线配置 REST API |
| `backend/src/main/java/com/example/airequirementworkbench/masterdata/service/ProductLineConfigService.java` | 产品线创建、编辑、软删除、搜索和校验逻辑 |
| `backend/src/main/java/com/example/airequirementworkbench/masterdata/dto/ProductLineConfigDtos.java` | 产品线配置请求和响应 DTO |
| `frontend/src/api/productLineApi.ts` | 前端产品线配置 API 封装 |
| `frontend/src/views/ProductLineConfigView.vue` | 产品线配置页面 |
| `frontend/src/views/FeatureLibraryView.vue` | 全量功能库改为从产品线配置接口加载产品线 |
| `frontend/src/router/index.ts` | 新增 `/product-lines` 路由 |
| `frontend/src/App.vue` | 侧边栏新增“产品线配置”入口 |

## API 清单

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/product-lines?keyword=` | 查询未删除产品线，支持名称、编码、负责人模糊匹配 |
| GET | `/api/product-lines/{id}` | 查询单个产品线详情 |
| POST | `/api/product-lines` | 新增产品线 |
| PUT | `/api/product-lines/{id}` | 更新产品线基础信息 |
| DELETE | `/api/product-lines/{id}` | 软删除产品线 |

接口响应统一使用当前项目 `ApiResponse`，前端通过 `unwrap` 解包。

## 状态机

### product_line.status

| 状态 | 业务含义 | 进入时机 |
|---|---|---|
| `enabled` | 可用产品线 | 新增产品线或历史默认数据 |
| `disabled` | 停用产品线 | 当前用于软删除后的兼容状态 |

### product_line.deleted

| 值 | 业务含义 |
|---|---|
| `false` | 正常展示，可被需求和功能库选择 |
| `true` | 已软删除，不在产品线配置和全量功能库下拉中展示 |

## 枚举

### product_type

| 值 | 标签 |
|---|---|
| `face_to_customer` | 面客产品 |
| `internal` | 内部产品 |
| `public_service` | 公共服务 |
| `design_spec` | 设计规范 |

### platforms

| 值 | 标签 |
|---|---|
| `yunlian_front` | 云链前台 |
| `yunzu_front` | 云租前台 |
| `yunzu_app` | 云租APP |
| `middle_platform` | 中台 |
| `yunlian_back` | 云链后台 |
| `yunzu_back` | 云租后台 |
| `lianxin` | 链信 |
| `lianxin_app` | 链信APP |

## 数据库字段

### product_line

| 字段 | 说明 |
|---|---|
| `id` | 主键，沿用当前项目 `IdGenerator` 生成的 bigint ID |
| `line_code` | 产品线编码，可为空；保留历史唯一编码约束 |
| `line_name` | 产品线名称，未删除产品线中唯一 |
| `owners` | JSONB 字符串数组，当前阶段以负责人名称维护 |
| `product_type` | 产品线类型 |
| `platforms` | JSONB 字符串数组，表示涉及平台 |
| `description` | 业务介绍，最长 2000 字符 |
| `version` | 预留版本号，本阶段默认 0 |
| `is_processing` | 预留处理状态，本阶段默认 false |
| `status` | 兼容已有主数据接口的启停状态 |
| `deleted` | 软删除标记 |
| `created_at` / `updated_at` | 创建与更新时间 |

## 关键规则

- 产品线配置优先复用已有 `product_line` 表，不创建重复产品线表。
- 新增和编辑时，`line_name` 在未删除产品线中必须唯一。
- `owners` 会去空格、去空值、去重，至少保留一个负责人。
- `platforms` 至少选择一个，并校验在约定枚举内。
- 当前阶段不新增 `UserProductLine` 或复杂角色关系，`canEdit` / `canDelete` 统一返回 `true`。
- 全量功能库页面通过 `/api/product-lines` 加载产品线下拉；没有产品线时提示先进入产品线配置新增。
