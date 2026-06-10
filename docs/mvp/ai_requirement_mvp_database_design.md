# AI需求工作台 MVP 数据库结构说明

## 0. 设计原则

1. **核心业务数据进入数据库**：会话、消息、候选需求、正式需求、PRD、原型、AI Trace等必须结构化存储。
2. **大文本可数据库存储，附件走对象存储**：HTML原型内容可入库，上传附件、导出文件、历史快照可进入对象存储，数据库记录元信息。
3. **AI不直接写数据库**：AI只输出结构化JSON，后端校验后通过固定业务接口写库。
4. **会话与需求强绑定**：正式需求必须保留source_session_id和source_candidate_id。
5. **所有AI调用必须Trace**：模型、Prompt版本、输入、输出、耗时、Token、反馈均需记录。
6. **MVP优先PostgreSQL**：推荐使用PostgreSQL，JSON字段使用jsonb，后续可用pgvector扩展语义检索。

---

## 1. 表清单

| 序号 | 表名 | 中文名 | 说明 |
|---|---|---|---|
| 1 | sys_user | 用户表 | MVP基础用户信息 |
| 2 | product_line | 产品线表 | 需求归属产品线 |
| 3 | product_module | 产品模块表 | 产品线下模块 |
| 4 | conversation_session | 会话表 | 一次需求对话会话 |
| 5 | conversation_message | 会话消息表 | 用户/AI消息记录 |
| 6 | requirement_candidate | 候选需求表 | 对话中AI识别出的临时需求 |
| 7 | requirement_candidate_patch | 候选需求变更表 | 候选需求的增量更新记录 |
| 8 | requirement | 正式需求表 | 人工确认后的需求卡片 |
| 9 | requirement_version | 需求版本表 | 正式需求的历史版本 |
| 10 | requirement_relation | 需求关系表 | 相似、重复、拆分、合并关系 |
| 11 | prd | PRD主表 | 需求转PRD后的主对象 |
| 12 | prd_section | PRD章节表 | 结构化PRD章节 |
| 13 | prd_version | PRD版本表 | PRD历史快照 |
| 14 | prototype | HTML原型表 | PRD生成的页面原型 |
| 15 | prototype_version | HTML原型版本表 | 原型历史版本 |
| 16 | ai_model_config | AI模型配置表 | 模型供应商与参数 |
| 17 | ai_prompt_template | AI Prompt模板表 | Prompt版本化配置 |
| 18 | ai_skill_config | AI能力配置表 | Skill与模型、Prompt绑定 |
| 19 | ai_output_schema | AI输出格式表 | JSON Schema/HTML规则 |
| 20 | ai_trace | AI调用Trace表 | 每次AI调用记录 |
| 21 | ai_action_feedback | AI输出反馈表 | 用户对AI结果的反馈 |
| 22 | embedding_index | 语义索引表 | 需求、PRD、原型语义检索 |
| 23 | artifact | 产出物元数据表 | 附件、导出文件、快照文件 |

---

## 2. 全局字段约定

### 2.1 主键

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 雪花ID或数据库自增ID，建议统一使用雪花ID |

### 2.2 审计字段

所有核心业务表建议包含：

| 字段 | 类型 | 说明 |
|---|---|---|
| created_by | bigint | 创建人ID |
| created_at | timestamp | 创建时间 |
| updated_by | bigint | 更新人ID |
| updated_at | timestamp | 更新时间 |
| deleted | boolean | 逻辑删除标识，默认false |

### 2.3 状态字段

状态字段统一使用varchar(32)，避免直接使用数据库枚举，便于后续扩展。

### 2.4 JSON字段

| 字段后缀 | 类型 | 说明 |
|---|---|---|
| `_json` | jsonb | 结构化对象或数组 |

---

## 3. 基础主数据

### 3.1 sys_user 用户表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 用户ID | 主键 |
| username | varchar | 64 | 是 | - | 登录名 | 唯一 |
| display_name | varchar | 64 | 是 | - | 显示名 | 2-64字符 |
| email | varchar | 128 | 否 | null | 邮箱 | 邮箱格式 |
| role_code | varchar | 32 | 是 | user | 角色 | user/product/admin/ai_admin |
| status | varchar | 32 | 是 | enabled | 状态 | enabled/disabled |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |
| updated_at | timestamp | - | 是 | now() | 更新时间 | - |

索引：

- uk_sys_user_username(username)
- idx_sys_user_role(role_code)

---

### 3.2 product_line 产品线表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 产品线ID | 主键 |
| line_code | varchar | 64 | 是 | - | 产品线编码 | 唯一 |
| line_name | varchar | 100 | 是 | - | 产品线名称 | 2-100字符 |
| description | varchar | 500 | 否 | null | 描述 | - |
| status | varchar | 32 | 是 | enabled | 状态 | enabled/disabled |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |
| updated_by | bigint | - | 否 | null | 更新人 | - |
| updated_at | timestamp | - | 是 | now() | 更新时间 | - |
| deleted | boolean | - | 是 | false | 逻辑删除 | - |

索引：

- uk_product_line_code(line_code)
- idx_product_line_status(status)

---

### 3.3 product_module 产品模块表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 模块ID | 主键 |
| product_line_id | bigint | - | 是 | - | 产品线ID | 外键逻辑关联product_line.id |
| module_code | varchar | 64 | 是 | - | 模块编码 | 同产品线下唯一 |
| module_name | varchar | 100 | 是 | - | 模块名称 | 2-100字符 |
| parent_id | bigint | - | 否 | null | 父模块ID | 支持模块树 |
| description | varchar | 500 | 否 | null | 描述 | - |
| status | varchar | 32 | 是 | enabled | 状态 | enabled/disabled |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |
| updated_by | bigint | - | 否 | null | 更新人 | - |
| updated_at | timestamp | - | 是 | now() | 更新时间 | - |
| deleted | boolean | - | 是 | false | 逻辑删除 | - |

索引：

- uk_product_module_line_code(product_line_id, module_code)
- idx_product_module_line(product_line_id)
- idx_product_module_parent(parent_id)

---

## 4. 对话与候选需求

### 4.1 conversation_session 会话表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 会话ID | 主键 |
| title | varchar | 120 | 是 | - | 会话标题 | 默认首条消息摘要 |
| status | varchar | 32 | 是 | active | 会话状态 | active/closed/archived |
| current_stage | varchar | 32 | 是 | empty | 当前阶段 | empty/collecting/refining/candidate_ready/card_generated/closed |
| summary | text | - | 否 | null | 会话摘要 | AI生成，可更新 |
| last_message_at | timestamp | - | 否 | null | 最近消息时间 | - |
| candidate_count | integer | - | 是 | 0 | 候选需求数量 | >=0 |
| requirement_count | integer | - | 是 | 0 | 已生成正式需求数 | >=0 |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |
| updated_by | bigint | - | 否 | null | 更新人 | - |
| updated_at | timestamp | - | 是 | now() | 更新时间 | - |
| deleted | boolean | - | 是 | false | 逻辑删除 | - |

索引：

- idx_conversation_created_by(created_by)
- idx_conversation_status(status)
- idx_conversation_stage(current_stage)
- idx_conversation_last_message(last_message_at)

码值：

| 字段 | 值 | 说明 |
|---|---|---|
| status | active | 进行中 |
| status | closed | 已关闭 |
| status | archived | 已归档 |
| current_stage | empty | 空会话 |
| current_stage | collecting | 正在收集 |
| current_stage | refining | 需求澄清中 |
| current_stage | candidate_ready | 候选需求可生成卡片 |
| current_stage | card_generated | 已生成正式需求 |
| current_stage | closed | 会话结束 |

---

### 4.2 conversation_message 会话消息表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 消息ID | 主键 |
| session_id | bigint | - | 是 | - | 会话ID | 关联conversation_session.id |
| role | varchar | 16 | 是 | - | 消息角色 | user/assistant/system |
| message_type | varchar | 32 | 是 | text | 消息类型 | text/command/ai_reply/action_result/error |
| content | text | - | 是 | - | 消息正文 | 非空，最大10000字符 |
| command_name | varchar | 64 | 否 | null | 命令名称 | 当message_type=command时有效 |
| metadata_json | jsonb | - | 否 | null | 扩展信息 | 可记录客户端、引用对象等 |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |
| deleted | boolean | - | 是 | false | 逻辑删除 | - |

索引：

- idx_message_session_created(session_id, created_at)
- idx_message_role(role)
- idx_message_type(message_type)

码值：

| 字段 | 值 | 说明 |
|---|---|---|
| role | user | 用户消息 |
| role | assistant | AI回复 |
| role | system | 系统消息 |
| message_type | text | 普通文本 |
| message_type | command | /命令 |
| message_type | ai_reply | AI回复 |
| message_type | action_result | 按钮动作结果 |
| message_type | error | 异常消息 |

---

### 4.3 requirement_candidate 候选需求表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 候选需求ID | 主键 |
| session_id | bigint | - | 是 | - | 来源会话ID | 关联conversation_session.id |
| title | varchar | 120 | 是 | - | 候选需求标题 | 2-120字符 |
| status | varchar | 32 | 是 | draft | 状态 | draft/refining/ready_to_card/confirmed/converted/closed |
| content_json | jsonb | - | 是 | '{}' | 结构化需求内容 | 符合候选需求Schema |
| completeness_score | numeric | 5,2 | 是 | 0 | 完整度评分 | 0-100 |
| missing_items_json | jsonb | - | 否 | null | 缺失项 | 数组 |
| risky_items_json | jsonb | - | 否 | null | 风险项 | 数组 |
| suggested_questions_json | jsonb | - | 否 | null | 建议追问 | 数组，最多5个 |
| confidence | numeric | 5,4 | 否 | null | AI置信度 | 0-1 |
| created_from_message_id | bigint | - | 否 | null | 首次识别来源消息 | 关联conversation_message.id |
| converted_requirement_id | bigint | - | 否 | null | 转正式需求ID | 转换后回填 |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |
| updated_by | bigint | - | 否 | null | 更新人 | - |
| updated_at | timestamp | - | 是 | now() | 更新时间 | - |
| deleted | boolean | - | 是 | false | 逻辑删除 | - |

索引：

- idx_candidate_session(session_id)
- idx_candidate_status(status)
- idx_candidate_score(completeness_score)
- idx_candidate_created_from_msg(created_from_message_id)

content_json建议结构：

```json
{
  "title": "合同列表导出",
  "background": "",
  "businessGoal": "支持用户根据筛选条件导出合同数据",
  "userRoles": ["运营人员"],
  "scenarios": ["合同列表查询"],
  "scope": ["合同列表增加导出按钮"],
  "outOfScope": [],
  "businessRules": ["导出范围为当前筛选结果"],
  "fields": ["与列表字段一致"],
  "permissions": ["仅运营人员允许导出"],
  "process": [],
  "exceptionCases": [],
  "acceptanceCriteria": []
}
```

---

### 4.4 requirement_candidate_patch 候选需求变更表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | Patch ID | 主键 |
| candidate_id | bigint | - | 是 | - | 候选需求ID | 关联requirement_candidate.id |
| session_id | bigint | - | 是 | - | 会话ID | 冗余便于查询 |
| source_message_id | bigint | - | 否 | null | 来源消息ID | 关联conversation_message.id |
| patch_type | varchar | 32 | 是 | update | 变更类型 | create/update/merge/split/close |
| patch_json | jsonb | - | 是 | - | 变更内容 | JSON Patch或业务patch |
| before_json | jsonb | - | 否 | null | 变更前快照 | 可选 |
| after_json | jsonb | - | 否 | null | 变更后快照 | 可选 |
| ai_trace_id | bigint | - | 否 | null | AI Trace ID | 关联ai_trace.id |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |

索引：

- idx_candidate_patch_candidate(candidate_id, created_at)
- idx_candidate_patch_session(session_id)
- idx_candidate_patch_message(source_message_id)
- idx_candidate_patch_trace(ai_trace_id)

patch_json示例：

```json
{
  "operation": "update",
  "fields": {
    "permissions": ["仅运营人员允许导出"],
    "businessRules": ["导出范围为当前筛选结果"]
  }
}
```

---

## 5. 正式需求

### 5.1 requirement 正式需求表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 需求ID | 主键 |
| requirement_no | varchar | 32 | 是 | - | 需求编号 | 唯一，REQ-YYYY-NNNN |
| source_session_id | bigint | - | 是 | - | 来源会话ID | 强绑定 |
| source_candidate_id | bigint | - | 是 | - | 来源候选需求ID | 强绑定 |
| title | varchar | 120 | 是 | - | 标题 | 2-120字符 |
| product_line_id | bigint | - | 是 | - | 产品线ID | - |
| module_id | bigint | - | 是 | - | 模块ID | - |
| requirement_type | varchar | 32 | 是 | - | 需求类型 | new_feature/optimization/defect/permission/process/data/interface/report |
| priority | varchar | 16 | 否 | medium | 优先级 | low/medium/high/urgent |
| status | varchar | 32 | 是 | confirmed | 状态 | draft/confirmed/prd_generated/prototype_generated/closed/archived |
| content_json | jsonb | - | 是 | - | 结构化需求内容 | 必须符合正式需求Schema |
| completeness_score | numeric | 5,2 | 是 | 0 | 完整度 | 0-100 |
| current_version | integer | - | 是 | 1 | 当前版本号 | >=1 |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |
| updated_by | bigint | - | 否 | null | 更新人 | - |
| updated_at | timestamp | - | 是 | now() | 更新时间 | - |
| deleted | boolean | - | 是 | false | 逻辑删除 | - |

索引：

- uk_requirement_no(requirement_no)
- idx_requirement_source_session(source_session_id)
- idx_requirement_source_candidate(source_candidate_id)
- idx_requirement_product_module(product_line_id, module_id)
- idx_requirement_status(status)
- idx_requirement_type(requirement_type)
- idx_requirement_created(created_at)

---

### 5.2 requirement_version 需求版本表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 版本ID | 主键 |
| requirement_id | bigint | - | 是 | - | 需求ID | - |
| version_no | integer | - | 是 | - | 版本号 | 同需求下递增 |
| title | varchar | 120 | 是 | - | 版本标题 | - |
| content_json | jsonb | - | 是 | - | 版本内容 | 快照 |
| change_summary | varchar | 1000 | 否 | null | 变更摘要 | 可AI生成 |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |

索引：

- uk_requirement_version(requirement_id, version_no)
- idx_requirement_version_created(requirement_id, created_at)

---

### 5.3 requirement_relation 需求关系表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 关系ID | 主键 |
| source_requirement_id | bigint | - | 是 | - | 源需求ID | - |
| target_requirement_id | bigint | - | 是 | - | 目标需求ID | - |
| relation_type | varchar | 32 | 是 | - | 关系类型 | similar/duplicate/parent_child/split_from/merged_to/related |
| similarity_score | numeric | 5,4 | 否 | null | 相似度 | 0-1 |
| reason | varchar | 1000 | 否 | null | 关系原因 | AI或人工说明 |
| confirmed | boolean | - | 是 | false | 是否确认 | 人工确认 |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |

索引：

- idx_req_relation_source(source_requirement_id)
- idx_req_relation_target(target_requirement_id)
- idx_req_relation_type(relation_type)

---

## 6. PRD与HTML原型

### 6.1 prd PRD主表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | PRD ID | 主键 |
| prd_no | varchar | 32 | 是 | - | PRD编号 | 唯一，PRD-YYYY-NNNN |
| requirement_id | bigint | - | 是 | - | 关联需求ID | - |
| title | varchar | 120 | 是 | - | PRD标题 | 2-120字符 |
| product_line_id | bigint | - | 是 | - | 产品线ID | 冗余便于查询 |
| module_id | bigint | - | 是 | - | 模块ID | 冗余便于查询 |
| version | integer | - | 是 | 1 | 当前版本 | >=1 |
| status | varchar | 32 | 是 | draft | 状态 | draft/ai_generated/editing/reviewed/confirmed/need_review/archived |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |
| updated_by | bigint | - | 否 | null | 更新人 | - |
| updated_at | timestamp | - | 是 | now() | 更新时间 | - |
| deleted | boolean | - | 是 | false | 逻辑删除 | - |

索引：

- uk_prd_no(prd_no)
- idx_prd_requirement(requirement_id)
- idx_prd_product_module(product_line_id, module_id)
- idx_prd_status(status)

---

### 6.2 prd_section PRD章节表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 章节ID | 主键 |
| prd_id | bigint | - | 是 | - | PRD ID | - |
| section_type | varchar | 64 | 是 | - | 章节类型 | 枚举 |
| section_title | varchar | 120 | 是 | - | 章节标题 | - |
| content_markdown | text | - | 否 | null | Markdown内容 | - |
| content_json | jsonb | - | 否 | null | 结构化内容 | 与section_type匹配 |
| sort_order | integer | - | 是 | 0 | 排序 | >=0 |
| ai_generated | boolean | - | 是 | false | 是否AI生成 | - |
| confirmed | boolean | - | 是 | false | 是否确认 | - |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |
| updated_by | bigint | - | 否 | null | 更新人 | - |
| updated_at | timestamp | - | 是 | now() | 更新时间 | - |

索引：

- uk_prd_section_type(prd_id, section_type)
- idx_prd_section_order(prd_id, sort_order)

section_type码值：

| 值 | 说明 |
|---|---|
| basic_info | 基本信息 |
| background | 需求背景 |
| goal | 功能目标 |
| user_role | 用户角色 |
| scenario | 使用场景 |
| scope | 功能范围 |
| business_process | 业务流程 |
| page_description | 页面说明 |
| field_rule | 字段规则 |
| business_rule | 业务规则 |
| permission_rule | 权限规则 |
| exception_case | 异常场景 |
| acceptance_criteria | 验收标准 |
| change_log | 变更记录 |

---

### 6.3 prd_version PRD版本表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 版本ID | 主键 |
| prd_id | bigint | - | 是 | - | PRD ID | - |
| version_no | integer | - | 是 | - | 版本号 | 同PRD下递增 |
| snapshot_json | jsonb | - | 是 | - | PRD快照 | 包含主表和章节 |
| change_summary | varchar | 1000 | 否 | null | 变更摘要 | - |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |

索引：

- uk_prd_version(prd_id, version_no)

---

### 6.4 prototype HTML原型表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 原型ID | 主键 |
| prototype_no | varchar | 32 | 是 | - | 原型编号 | 唯一，PROT-YYYY-NNNN |
| prd_id | bigint | - | 是 | - | PRD ID | - |
| requirement_id | bigint | - | 是 | - | 需求ID | 冗余便于查询 |
| page_name | varchar | 80 | 是 | - | 页面名称 | 2-80字符 |
| page_type | varchar | 32 | 是 | - | 页面类型 | list/detail/form/modal/config/result |
| html_content | text | - | 是 | - | HTML内容 | 必须安全过滤 |
| description | varchar | 1000 | 否 | null | 说明 | - |
| version | integer | - | 是 | 1 | 当前版本 | >=1 |
| status | varchar | 32 | 是 | ai_generated | 状态 | draft/ai_generated/confirmed/adjusted/archived |
| generated_by_ai | boolean | - | 是 | true | 是否AI生成 | - |
| prompt_template_id | bigint | - | 否 | null | Prompt ID | - |
| prompt_version | varchar | 32 | 否 | null | Prompt版本 | - |
| model_name | varchar | 100 | 否 | null | 模型名称 | - |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |
| updated_by | bigint | - | 否 | null | 更新人 | - |
| updated_at | timestamp | - | 是 | now() | 更新时间 | - |
| deleted | boolean | - | 是 | false | 逻辑删除 | - |

索引：

- uk_prototype_no(prototype_no)
- idx_prototype_prd(prd_id)
- idx_prototype_requirement(requirement_id)
- idx_prototype_status(status)

---

### 6.5 prototype_version HTML原型版本表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 版本ID | 主键 |
| prototype_id | bigint | - | 是 | - | 原型ID | - |
| version_no | integer | - | 是 | - | 版本号 | 同原型下递增 |
| html_content | text | - | 是 | - | HTML快照 | - |
| change_summary | varchar | 1000 | 否 | null | 变更摘要 | - |
| ai_trace_id | bigint | - | 否 | null | AI Trace ID | - |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |

索引：

- uk_prototype_version(prototype_id, version_no)

---

## 7. AI配置与Trace

### 7.1 ai_model_config AI模型配置表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 模型配置ID | 主键 |
| provider | varchar | 64 | 是 | - | 供应商 | openai_compatible/private/local |
| model_name | varchar | 100 | 是 | - | 模型名称 | - |
| api_base_url | varchar | 500 | 是 | - | API地址 | URL格式 |
| api_key_cipher | varchar | 1000 | 否 | null | API Key密文 | 加密存储 |
| context_window | integer | - | 是 | 4096 | 上下文长度 | >=4096 |
| temperature | numeric | 3,2 | 是 | 0.20 | 温度 | 0-2 |
| max_output_tokens | integer | - | 是 | 4096 | 最大输出Token | >=512 |
| enabled | boolean | - | 是 | true | 是否启用 | - |
| is_default | boolean | - | 是 | false | 是否默认 | 同一能力建议唯一默认 |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |
| updated_by | bigint | - | 否 | null | 更新人 | - |
| updated_at | timestamp | - | 是 | now() | 更新时间 | - |

索引：

- idx_ai_model_enabled(enabled)
- idx_ai_model_provider(provider)

---

### 7.2 ai_prompt_template AI Prompt模板表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | Prompt ID | 主键 |
| prompt_name | varchar | 80 | 是 | - | 模板名称 | 2-80字符 |
| ability_type | varchar | 64 | 是 | - | 能力类型 | 枚举 |
| system_prompt | text | - | 是 | - | 系统提示词 | 非空 |
| user_prompt_template | text | - | 是 | - | 用户提示词模板 | 支持变量 |
| output_format | varchar | 32 | 是 | json | 输出格式 | json/markdown/html/text |
| output_schema_id | bigint | - | 否 | null | 输出格式ID | 关联ai_output_schema.id |
| version | varchar | 32 | 是 | v1.0 | 版本 | 语义版本 |
| status | varchar | 32 | 是 | draft | 状态 | draft/enabled/disabled |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |
| updated_by | bigint | - | 否 | null | 更新人 | - |
| updated_at | timestamp | - | 是 | now() | 更新时间 | - |
| deleted | boolean | - | 是 | false | 逻辑删除 | - |

索引：

- idx_prompt_ability(ability_type)
- idx_prompt_status(status)
- uk_prompt_name_version(prompt_name, version)

ability_type码值：

| 值 | 说明 |
|---|---|
| intent_recognition | 意图识别 |
| requirement_extract | 需求抽取 |
| requirement_split | 需求拆分 |
| completeness_check | 完整度检查 |
| reply_generate | 回复生成 |
| card_generate | 需求卡片生成 |
| prd_generate | PRD生成 |
| prd_check | PRD检查 |
| html_prototype_generate | HTML原型生成 |
| semantic_match_reason | 语义匹配原因生成 |

---

### 7.3 ai_skill_config AI能力配置表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | Skill ID | 主键 |
| skill_code | varchar | 64 | 是 | - | Skill编码 | 唯一 |
| skill_name | varchar | 100 | 是 | - | Skill名称 | - |
| ability_type | varchar | 64 | 是 | - | 能力类型 | 关联Prompt能力 |
| model_config_id | bigint | - | 是 | - | 模型配置ID | - |
| prompt_template_id | bigint | - | 是 | - | Prompt模板ID | - |
| require_human_confirm | boolean | - | 是 | true | 是否需要人工确认 | - |
| write_database | boolean | - | 是 | false | 是否写库 | MVP中由后端写库 |
| retry_times | integer | - | 是 | 0 | 失败重试次数 | 0-3 |
| timeout_ms | integer | - | 是 | 30000 | 超时时间 | >0 |
| enabled | boolean | - | 是 | true | 是否启用 | - |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |
| updated_by | bigint | - | 否 | null | 更新人 | - |
| updated_at | timestamp | - | 是 | now() | 更新时间 | - |

索引：

- uk_skill_code(skill_code)
- idx_skill_ability(ability_type)
- idx_skill_enabled(enabled)

---

### 7.4 ai_output_schema AI输出格式表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | Schema ID | 主键 |
| schema_name | varchar | 100 | 是 | - | Schema名称 | - |
| ability_type | varchar | 64 | 是 | - | 能力类型 | - |
| schema_json | jsonb | - | 是 | - | JSON Schema或规则 | - |
| status | varchar | 32 | 是 | enabled | 状态 | draft/enabled/disabled |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |
| updated_by | bigint | - | 否 | null | 更新人 | - |
| updated_at | timestamp | - | 是 | now() | 更新时间 | - |

索引：

- idx_output_schema_ability(ability_type)
- idx_output_schema_status(status)

---

### 7.5 ai_trace AI调用Trace表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | Trace ID | 主键 |
| trace_no | varchar | 40 | 是 | - | Trace编号 | 唯一，TR-YYYYMMDD-NNNN |
| session_id | bigint | - | 否 | null | 会话ID | 可为空 |
| business_object_type | varchar | 64 | 否 | null | 业务对象类型 | conversation/candidate/requirement/prd/prototype |
| business_object_id | bigint | - | 否 | null | 业务对象ID | - |
| ability_type | varchar | 64 | 是 | - | AI能力类型 | 枚举 |
| model_config_id | bigint | - | 否 | null | 模型配置ID | - |
| model_name | varchar | 100 | 是 | - | 模型名称 | - |
| prompt_template_id | bigint | - | 否 | null | Prompt ID | - |
| prompt_version | varchar | 32 | 否 | null | Prompt版本 | - |
| input_json | jsonb | - | 否 | null | 输入上下文 | 可脱敏存储 |
| output_json | jsonb | - | 否 | null | 结构化输出 | - |
| output_text | text | - | 否 | null | 文本输出 | - |
| token_input | integer | - | 否 | 0 | 输入Token | >=0 |
| token_output | integer | - | 否 | 0 | 输出Token | >=0 |
| duration_ms | integer | - | 否 | 0 | 耗时毫秒 | >=0 |
| status | varchar | 32 | 是 | created | 状态 | created/success/failed/timeout |
| error_code | varchar | 64 | 否 | null | 错误码 | - |
| error_message | text | - | 否 | null | 错误信息 | - |
| created_by | bigint | - | 是 | - | 调用人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |

索引：

- uk_ai_trace_no(trace_no)
- idx_ai_trace_session(session_id)
- idx_ai_trace_object(business_object_type, business_object_id)
- idx_ai_trace_ability(ability_type)
- idx_ai_trace_status(status)
- idx_ai_trace_created(created_at)

---

### 7.6 ai_action_feedback AI输出反馈表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 反馈ID | 主键 |
| ai_trace_id | bigint | - | 是 | - | Trace ID | - |
| business_object_type | varchar | 64 | 否 | null | 对象类型 | - |
| business_object_id | bigint | - | 否 | null | 对象ID | - |
| feedback_type | varchar | 32 | 是 | - | 反馈类型 | adopted/partial/rejected/regenerated |
| reason_code | varchar | 64 | 否 | null | 原因码 | incomplete/wrong_format/wrong_understanding/missing_rule/not_professional |
| comment | varchar | 1000 | 否 | null | 反馈说明 | - |
| created_by | bigint | - | 是 | - | 反馈人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |

索引：

- idx_feedback_trace(ai_trace_id)
- idx_feedback_object(business_object_type, business_object_id)
- idx_feedback_type(feedback_type)

---

## 8. 检索与产出物

### 8.1 embedding_index 语义索引表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 索引ID | 主键 |
| object_type | varchar | 64 | 是 | - | 对象类型 | requirement/prd/prd_section/prototype/conversation |
| object_id | bigint | - | 是 | - | 对象ID | - |
| chunk_no | integer | - | 是 | 1 | 分块序号 | >=1 |
| chunk_text | text | - | 是 | - | 分块文本 | - |
| embedding_vector | vector | - | 否 | null | 向量 | 使用pgvector时启用 |
| metadata_json | jsonb | - | 否 | null | 元数据 | 产品线、模块、状态等 |
| status | varchar | 32 | 是 | enabled | 状态 | enabled/disabled/rebuild_required |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |
| updated_at | timestamp | - | 是 | now() | 更新时间 | - |

索引：

- idx_embedding_object(object_type, object_id)
- idx_embedding_status(status)
- ivfflat/hnsw向量索引，视pgvector版本配置

---

### 8.2 artifact 产出物元数据表

| 字段 | 类型 | 长度 | 必填 | 默认值 | 说明 | 规则 |
|---|---:|---:|---|---|---|---|
| id | bigint | - | 是 | - | 产出物ID | 主键 |
| artifact_no | varchar | 40 | 是 | - | 产出物编号 | 唯一 |
| owner_type | varchar | 64 | 是 | - | 归属对象类型 | requirement/prd/prototype/conversation |
| owner_id | bigint | - | 是 | - | 归属对象ID | - |
| artifact_type | varchar | 64 | 是 | - | 类型 | html_export/attachment/markdown_export/snapshot |
| file_name | varchar | 255 | 是 | - | 文件名 | - |
| file_path | varchar | 1000 | 是 | - | 文件路径或对象存储Key | - |
| file_size | bigint | - | 否 | null | 文件大小 | >=0 |
| mime_type | varchar | 128 | 否 | null | MIME类型 | - |
| version_no | integer | - | 是 | 1 | 文件版本 | >=1 |
| status | varchar | 32 | 是 | enabled | 状态 | enabled/archived/deleted |
| created_by | bigint | - | 是 | - | 创建人 | - |
| created_at | timestamp | - | 是 | now() | 创建时间 | - |

索引：

- uk_artifact_no(artifact_no)
- idx_artifact_owner(owner_type, owner_id)
- idx_artifact_type(artifact_type)

---

## 9. 关键映射关系

| 关系 | 映射方式 |
|---|---|
| 会话 → 消息 | conversation_message.session_id |
| 会话 → 候选需求 | requirement_candidate.session_id |
| 消息 → 候选需求Patch | requirement_candidate_patch.source_message_id |
| 候选需求 → 正式需求 | requirement.source_candidate_id |
| 会话 → 正式需求 | requirement.source_session_id |
| 正式需求 → PRD | prd.requirement_id |
| PRD → PRD章节 | prd_section.prd_id |
| PRD → HTML原型 | prototype.prd_id |
| 正式需求 → HTML原型 | prototype.requirement_id |
| AI Trace → 业务对象 | ai_trace.business_object_type + business_object_id |
| AI Trace → 候选需求Patch | requirement_candidate_patch.ai_trace_id |
| AI Trace → 原型版本 | prototype_version.ai_trace_id |

---

## 10. 推荐DDL片段

> 以下为核心表示例DDL，实际开发可结合ORM或迁移工具生成。

```sql
CREATE TABLE conversation_session (
  id BIGINT PRIMARY KEY,
  title VARCHAR(120) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  current_stage VARCHAR(32) NOT NULL DEFAULT 'empty',
  summary TEXT,
  last_message_at TIMESTAMP,
  candidate_count INTEGER NOT NULL DEFAULT 0,
  requirement_count INTEGER NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE requirement_candidate (
  id BIGINT PRIMARY KEY,
  session_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  content_json JSONB NOT NULL DEFAULT '{}'::jsonb,
  completeness_score NUMERIC(5,2) NOT NULL DEFAULT 0,
  missing_items_json JSONB,
  risky_items_json JSONB,
  suggested_questions_json JSONB,
  confidence NUMERIC(5,4),
  created_from_message_id BIGINT,
  converted_requirement_id BIGINT,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE requirement (
  id BIGINT PRIMARY KEY,
  requirement_no VARCHAR(32) NOT NULL UNIQUE,
  source_session_id BIGINT NOT NULL,
  source_candidate_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  product_line_id BIGINT NOT NULL,
  module_id BIGINT NOT NULL,
  requirement_type VARCHAR(32) NOT NULL,
  priority VARCHAR(16) DEFAULT 'medium',
  status VARCHAR(32) NOT NULL DEFAULT 'confirmed',
  content_json JSONB NOT NULL,
  completeness_score NUMERIC(5,2) NOT NULL DEFAULT 0,
  current_version INTEGER NOT NULL DEFAULT 1,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE ai_trace (
  id BIGINT PRIMARY KEY,
  trace_no VARCHAR(40) NOT NULL UNIQUE,
  session_id BIGINT,
  business_object_type VARCHAR(64),
  business_object_id BIGINT,
  ability_type VARCHAR(64) NOT NULL,
  model_config_id BIGINT,
  model_name VARCHAR(100) NOT NULL,
  prompt_template_id BIGINT,
  prompt_version VARCHAR(32),
  input_json JSONB,
  output_json JSONB,
  output_text TEXT,
  token_input INTEGER DEFAULT 0,
  token_output INTEGER DEFAULT 0,
  duration_ms INTEGER DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'created',
  error_code VARCHAR(64),
  error_message TEXT,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## 11. MVP状态码汇总

### 11.1 requirement_type

| 值 | 中文 |
|---|---|
| new_feature | 新功能 |
| optimization | 功能优化 |
| defect | 缺陷反馈 |
| permission | 权限需求 |
| process | 流程需求 |
| data | 数据需求 |
| interface | 接口需求 |
| report | 报表需求 |

### 11.2 AI ability_type

| 值 | 中文 |
|---|---|
| intent_recognition | 意图识别 |
| requirement_extract | 需求抽取 |
| requirement_split | 需求拆分 |
| completeness_check | 完整度检查 |
| reply_generate | 回复生成 |
| card_generate | 需求卡片生成 |
| prd_generate | PRD生成 |
| prd_check | PRD检查 |
| html_prototype_generate | HTML原型生成 |
| semantic_match_reason | 语义匹配原因生成 |

### 11.3 feedback_type

| 值 | 中文 |
|---|---|
| adopted | 采纳 |
| partial | 部分采纳 |
| rejected | 不采纳 |
| regenerated | 重新生成 |

---

## 12. 后续扩展预留

| 后续能力 | 当前预留点 |
|---|---|
| 项目管理 | requirement中可增加project_id或通过关联表扩展 |
| 任务排期 | PRD确认后可转task模板 |
| 代码知识层 | 未来新增code_knowledge_card、code_relation等表 |
| 外部IM接入 | conversation_session增加source_channel字段 |
| MCP工具层 | 后端Action Registry可封装为MCP工具，但不暴露通用SQL |
| 权限体系增强 | 增加data_permission_policy、object_acl表 |

