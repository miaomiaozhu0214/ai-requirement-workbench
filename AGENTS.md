# AGENTS.md

## 项目定位

本项目是“AI需求工作台 MVP”，只实现需求相关能力，不实现项目管理、任务排期、工时管理、代码知识库、外部聊天工具接入。

MVP核心链路：
网页对话框 → AI需求识别 → 候选需求卡片 → 正式需求卡片 → 结构化PRD → HTML原型 → 查询与AI配置/Trace。

## 必读文档

实现前必须优先阅读以下文档：

- docs/mvp/ai_requirement_mvp_function_spec.md
- docs/mvp/ai_requirement_mvp_database_design.md
- docs/mvp/ai_requirement_mvp_data_relationship.html
- docs/mvp/ai_requirement_mvp_prototype.html

不要擅自扩展 MVP 范围。未在文档中明确的功能，先按最小可用方式实现，并在实现说明中记录假设。

## 技术约束

前端：
- 使用 Vue 3 + TypeScript + Vite。
- 页面结构参考 docs/mvp/ai_requirement_mvp_prototype.html。
- 组件应按业务模块拆分，不要把全部逻辑写在一个页面。
- 表单必须包含字段校验、错误提示、Toast、弹窗确认。
- API 调用统一封装在 src/api。
- 状态码、枚举、字段选项统一管理。
- frontend/src 目录只允许保留源文件，不允许提交或保留 .js、.js.map 等 TypeScript/Vue 编译产物。
- Vite 在解析无扩展名 import 时可能优先命中过期的 .js 文件，而不是 .ts/.vue 源文件，导致路由、API 或组件运行旧代码。若发现页面行为与 TypeScript 源码不一致，必须优先检查 frontend/src 下是否混入 .js/.js.map。

后端：
- 使用 Spring Boot 3 + Java 17。
- 数据库使用 PostgreSQL。
- 先实现 REST API，不接 MCP。
- AI 调用先通过可替换的 AiClient 接口封装，允许用 Mock 实现跑通流程。
- 大模型只返回结构化 JSON，数据库写入必须由后端服务完成。
- 所有 AI 调用必须记录 ai_trace。
- 候选需求与会话强绑定。
- 正式需求必须来源于候选需求，由用户确认生成。
- 所有对话消息处理必须先经过 intent_router。禁止在用户发送消息后直接调用需求抽取、完整度检查、回复生成或正式需求生成。
- intent_router 只负责判断意图、目标对象和建议动作；不得直接写数据库或替代后续能力。
- 后端 Orchestrator 是唯一的 AI 能力编排入口，负责组装会话上下文、候选需求上下文、最近消息上下文，校验 Router 建议动作是否允许执行，调用后续能力，记录每个能力的 ai_trace，并完成候选需求、补丁、正式需求和 assistant 回复消息的数据库写入。
- Router 返回的 nextActions 是建议动作集合和原始顺序。后端必须根据 AI能力配置中的 executionOrder 生成实际执行计划：executionOrder 越小越先执行；executionOrder 相同时必须保持 Router 返回的原始顺序。禁止完全依赖 LLM 返回顺序决定存在依赖关系的业务动作。

数据库：
- 表结构以 docs/mvp/ai_requirement_mvp_database_design.md 为准。
- 所有业务主表必须包含 id、created_at、updated_at。
- 状态字段必须使用文档定义的码值。
- JSON 字段用于存储结构化需求内容、AI输入输出、缺失项等。
- 不允许让 AI 直接生成 SQL 执行生产写入逻辑。

测试：
- 端到端验收测试必须优先验证真实 LLM 链路。除非显式配置 Mock，否则不得静默使用 Mock。对话测试必须检查 AI Trace，确认每轮用户输入都先经过 intent_router，再由后端 Orchestrator 编排后续能力。
- 前端构建必须执行源码清洁检查，确保 frontend/src 下不存在 .js 或 .js.map 编译产物。
- Trace 页面验收必须同时验证 API 和 UI：API 返回完整字段，UI 能显示 intent_router、requirement_extract、completeness_check、reply_generate、card_generate 的调用记录。

数据库迁移规则：
- 已经执行过并提交的 Flyway migration 文件 V*_*.sql 禁止修改，包括注释、空行、格式化调整。
- 如需调整表结构、初始化数据或补充说明，必须新增下一个版本号的 migration 文件，例如 V7__xxx.sql。

## 中文注释规范

本项目面向内部产品、研发、测试协作，核心业务代码需要具备中文注释。

注释原则：

- 不为显而易见的代码添加废话注释。
- 重点为 AI Router、AI Orchestrator、Prompt选择、上下文组装、候选需求状态流转、正式需求生成、AI Trace、真实LLM/Mock切换、数据库写入边界等核心逻辑补充中文说明。
- 注释应说明“为什么这样设计”和“业务含义”，不要简单重复代码。
- 大模型不得直接写数据库，后端负责校验、状态控制、Trace记录和持久化，该边界必须在相关代码中保留注释。
- 前端关键交互流程，如消息发送、候选需求刷新、生成正式需求卡片、Trace展示，也应保留必要中文注释。

## 项目文档规范

每次进行了改动后，需要在项目下的docs目录下创建Guideline_xx.md文件，其中xx按递增版本号顺序记录

记录内容包括：

-本次改动的更新日期。
-功能清单，包括功能名称、页面路由路径、功能是新增还是修改还是删除、功能权限所属角色。
-文件及文件目录清单，每个文件及目录带有中文说明。
-接口清单，包括接口名称、接口路径、接口简要说明。
-状态机。
-数据库字段清单，包括字段编码、字段中文名称、长度、字段类型、是否必填、码值、备注（如有）。

## 完成标准

每个任务完成后必须：

- 说明改动了哪些文件。
- 说明如何启动前端和后端。
- 说明如何验证功能。
- 补充必要的测试或 Mock 数据。
- 运行 lint/build/test，无法运行时说明原因。
- 不要删除 docs/mvp 下的需求文档。
