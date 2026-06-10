# AI需求工作台 MVP 需求功能说明

## 0. 文档说明

### 0.1 MVP定位

MVP版本仅建设需求生产相关能力：通过网页对话收集需求，利用AI完成需求识别、追问、拆分和结构化，生成可确认的需求卡片；在需求卡片基础上生成结构化PRD和HTML原型，并支持需求、PRD、原型的查询复用；同时建设模型配置、Prompt模板、AI能力编排和Trace记录等AI基础能力。

### 0.2 MVP边界

本版本包含：

- 网页对话框。
- AI需求意图识别。
- AI需求抽取、拆分、追问、完整度检查。
- 候选需求卡片实时维护。
- 正式需求卡片生成与查询。
- 结构化PRD生成、编辑、检查。
- HTML原型生成、预览、版本保存。
- AI模型、Prompt、Skill配置。
- AI调用Trace。

本版本不包含：

- 项目管理、任务排期、工时管理。
- 代码知识卡片。
- 外部聊天工具接入。
- Git、接口平台、测试平台等外部系统接入。
- 复杂组织权限和多人协同编辑。

---

# 1. 首页工作台

## 1.1 功能概要

首页工作台用于展示MVP需求生产线的整体运行状态，帮助用户快速进入待处理事项，包括待补充需求、待确认需求、已生成PRD、已生成HTML原型、最近对话和AI待处理事项。

## 1.2 流程图

```text
用户进入系统
  ↓
加载首页统计数据
  ↓
展示待处理需求、最近会话、AI待确认事项
  ↓
用户选择进入需求对话 / 需求池 / AI配置中心
```

## 1.3 业务逻辑

1. 首页只展示当前用户有权限查看的数据。
2. 统计口径以需求生产线状态为主，不展示项目类统计。
3. AI待处理事项包括：
   - 候选需求完整度不足。
   - AI生成内容待人工确认。
   - 相似需求待处理。
   - Prompt模板有草稿未发布。
   - AI调用失败待处理。
4. 点击待处理事项后跳转到对应对象页面。

## 1.4 数据权限

| 数据对象 | 权限规则 |
|---|---|
| 会话 | 仅创建人、管理员、被授权用户可见 |
| 需求卡片 | 按产品线、模块、创建人控制 |
| PRD | 继承需求权限 |
| 原型 | 继承PRD权限 |
| AI Trace | 普通用户仅可查看自己触发的Trace，管理员可查看全部 |

## 1.5 操作权限

| 操作 | 权限 |
|---|---|
| 新建需求对话 | 登录用户 |
| 查看统计 | 登录用户 |
| 处理AI待确认事项 | 对应对象编辑权限 |
| 进入AI配置中心 | AI配置管理员 |

## 1.6 调用接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/dashboard/summary` | GET | 获取首页统计 |
| `/api/dashboard/recent-sessions` | GET | 获取最近会话 |
| `/api/dashboard/pending-ai-actions` | GET | 获取AI待处理事项 |

## 1.7 字段清单

| 字段 | 类型 | 说明 | 校验 |
|---|---|---|---|
| pendingRequirementCount | integer | 待补充需求数 | >=0 |
| pendingConfirmCount | integer | 待确认需求数 | >=0 |
| prdCount | integer | 已生成PRD数 | >=0 |
| prototypeCount | integer | 已生成原型数 | >=0 |
| recentSessions | array | 最近会话 | 最多10条 |
| pendingAiActions | array | AI待处理事项 | 最多20条 |

## 1.8 提示词设计

首页不直接调用AI，仅展示AI产生的待处理事项。

## 1.9 数据库表结构

涉及表：

- conversation_session
- requirement_candidate
- requirement
- prd
- prototype
- ai_trace
- ai_action_feedback

## 1.10 状态机

首页本身无独立状态机，展示对象来源于会话、候选需求、正式需求、PRD、原型和AI Trace的状态。

---

# 2. 需求对话页

## 2.1 功能概要

需求对话页是MVP的核心入口。用户通过自然语言表达需求，系统在每轮对话后进行AI路由识别、需求抽取、候选需求更新、完整度检查和追问回复。页面右侧实时展示候选需求卡片。

## 2.2 页面结构

页面采用三栏布局：

```text
左侧：历史会话列表
中间：对话窗口
右侧：候选需求卡片列表
```

## 2.3 流程图

```text
用户输入消息
  ↓
后端保存原始消息
  ↓
AI意图识别
  ↓
后端根据意图选择Action
  ↓
AI需求抽取/拆分
  ↓
生成候选需求patch
  ↓
后端校验并更新候选需求
  ↓
AI完整度检查
  ↓
AI生成追问或下一步建议
  ↓
保存AI回复消息
  ↓
前端刷新对话与右侧候选卡片
```

## 2.4 业务逻辑

### 2.4.1 会话创建

1. 用户点击“新建会话”。
2. 系统创建conversation_session。
3. 会话初始状态为 `empty`。
4. 用户第一条有效消息写入conversation_message。

### 2.4.2 AI路由识别

每次用户发送消息后，默认进行意图识别。优先级如下：

```text
按钮动作 > /命令 > 规则判断 > AI意图识别 > 当前会话状态兜底规则
```

AI路由只返回意图、目标对象、建议动作和置信度，不直接执行数据库写入。

### 2.4.3 候选需求识别

系统支持一个会话生成多个候选需求。AI如果识别到用户一句话中包含多个独立需求，应返回拆分建议，由系统创建多个requirement_candidate。

### 2.4.4 候选需求增量更新

候选需求不应每轮重建，而是通过patch机制增量更新。每一次AI抽取结果写入requirement_candidate_patch，并更新requirement_candidate.content_json。

### 2.4.5 完整度检查

每次候选需求更新后，系统调用完整度检查能力，输出：

- completeness_score
- missing_items
- risky_items
- suggested_questions
- ready_to_generate_card

### 2.4.6 正式需求生成

候选需求达到可生成条件后，用户可以点击“生成需求卡片”。系统弹窗要求确认产品线、模块、需求标题等关键字段。确认后生成正式requirement，并保留source_session_id与source_candidate_id。

## 2.5 异常提示

| 场景 | 提示 |
|---|---|
| 输入为空 | 请输入需求内容或命令 |
| AI服务失败 | AI服务暂时不可用，请稍后重试或切换备用模型 |
| 意图识别置信度低 | 我不确定你是否在补充需求，请确认是否要继续完善当前候选需求 |
| 候选需求不存在 | 当前没有可更新的候选需求，请先描述需求 |
| 生成卡片必填项缺失 | 请补充产品线、模块和需求标题 |

## 2.6 数据权限

| 数据 | 权限 |
|---|---|
| 会话 | 创建人可见；管理员可见；授权用户可见 |
| 候选需求 | 继承会话权限 |
| 正式需求 | 生成后按产品线/模块权限控制 |

## 2.7 操作权限

| 操作 | 权限 |
|---|---|
| 新建会话 | 登录用户 |
| 发送消息 | 会话创建人或协作者 |
| 关闭候选需求 | 会话创建人、产品负责人、管理员 |
| 生成正式需求卡片 | 产品角色、管理员 |
| 删除会话 | 会话创建人、管理员；已生成正式需求的会话不可物理删除 |

## 2.8 调用接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/conversations` | POST | 新建会话 |
| `/api/conversations/{id}` | GET | 获取会话详情 |
| `/api/conversations/{id}/messages` | POST | 发送消息 |
| `/api/conversations/{id}/candidates` | GET | 获取候选需求 |
| `/api/candidates/{id}/close` | POST | 关闭候选需求 |
| `/api/candidates/{id}/generate-card` | POST | 候选需求生成正式需求 |
| `/api/ai/actions/route` | POST | AI意图识别 |
| `/api/ai/actions/extract-requirement` | POST | 需求抽取 |
| `/api/ai/actions/check-completeness` | POST | 完整度检查 |
| `/api/ai/actions/generate-reply` | POST | 生成回复 |

## 2.9 字段清单

### 2.9.1 会话字段

| 字段 | 类型 | 必填 | 说明 | 校验 |
|---|---|---|---|---|
| title | varchar(120) | 是 | 会话标题 | 默认为首条消息摘要 |
| status | varchar(32) | 是 | 会话状态 | 枚举 |
| summary | text | 否 | 会话摘要 | AI生成，人工可编辑 |
| current_stage | varchar(32) | 是 | 当前阶段 | 枚举 |

### 2.9.2 消息字段

| 字段 | 类型 | 必填 | 说明 | 校验 |
|---|---|---|---|---|
| role | varchar(16) | 是 | user/assistant/system | 枚举 |
| content | text | 是 | 消息内容 | 非空，最大10000字符 |
| message_type | varchar(32) | 是 | text/command/ai_reply/action_result | 枚举 |

### 2.9.3 候选需求字段

| 字段 | 类型 | 必填 | 说明 | 校验 |
|---|---|---|---|---|
| title | varchar(120) | 是 | 候选需求标题 | 2-120字符 |
| status | varchar(32) | 是 | 候选需求状态 | 枚举 |
| content_json | jsonb | 是 | 结构化内容 | 必须符合schema |
| completeness_score | numeric(5,2) | 是 | 完整度评分 | 0-100 |
| missing_items_json | jsonb | 否 | 缺失项 | 数组 |
| confidence | numeric(5,2) | 否 | AI置信度 | 0-1 |

## 2.10 提示词设计

### 2.10.1 意图识别Prompt

输入：

- 当前会话摘要。
- 当前候选需求列表。
- 最近N轮对话。
- 用户最新输入。

输出JSON：

```json
{
  "intent": "new_requirement",
  "confidence": 0.93,
  "targetType": "conversation",
  "targetId": null,
  "mayContainMultipleRequirements": true,
  "nextActions": ["extract_requirement", "split_requirement", "check_completeness", "generate_reply"],
  "reason": "用户表达了两个功能诉求"
}
```

### 2.10.2 需求抽取Prompt

输出JSON：

```json
{
  "extractedRequirements": [
    {
      "targetRequirementId": "cand_001",
      "operation": "update",
      "patch": {
        "permissions": ["仅运营人员可导出"],
        "businessRules": ["导出范围为当前筛选结果"]
      },
      "confidence": 0.88
    }
  ],
  "newRequirementCandidates": []
}
```

### 2.10.3 完整度检查Prompt

输出JSON：

```json
{
  "completenessScore": 72,
  "missingItems": ["导出字段范围未明确", "异常提示未明确"],
  "riskyItems": ["未说明管理员是否可导出"],
  "suggestedQuestions": ["导出字段是否与列表字段一致？"],
  "readyToGenerateCard": false
}
```

## 2.11 数据库表结构

涉及表：

- conversation_session
- conversation_message
- requirement_candidate
- requirement_candidate_patch
- requirement
- ai_trace

## 2.12 状态机

### 2.12.1 会话状态机

```text
empty
  ↓ 用户发送有效消息
collecting
  ↓ 识别到候选需求
refining
  ↓ 候选需求可生成卡片
candidate_ready
  ↓ 用户确认生成正式需求
card_generated
  ↓ 用户关闭会话
closed
```

### 2.12.2 候选需求状态机

```text
draft
  ↓ AI识别并创建
refining
  ↓ 完整度达到阈值
ready_to_card
  ↓ 用户确认
confirmed
  ↓ 生成正式需求
converted
```

---

# 3. 需求池列表页

## 3.1 功能概要

需求池用于管理已经从候选需求生成的正式需求卡片。支持按产品线、模块、状态、类型、完整度、关键字和语义进行查询。

## 3.2 流程图

```text
用户进入需求池
  ↓
加载有权限的需求列表
  ↓
用户输入查询条件
  ↓
系统执行条件查询/关键词查询/语义查询
  ↓
展示需求卡片列表
  ↓
用户进入需求详情或继续生成PRD
```

## 3.3 业务逻辑

1. 正式需求必须由候选需求确认生成。
2. 需求列表默认按更新时间倒序。
3. 支持查询来源会话、关联PRD、关联原型。
4. 不允许物理删除已经生成PRD的需求，只允许关闭或废弃。

## 3.4 数据权限

需求列表按产品线、模块、创建人、授权范围进行过滤。

## 3.5 操作权限

| 操作 | 权限 |
|---|---|
| 查看需求 | 产品线权限 |
| 编辑需求 | 产品负责人、需求创建人、管理员 |
| 关闭需求 | 产品负责人、管理员 |
| 生成PRD | 产品角色、管理员 |

## 3.6 调用接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/requirements` | GET | 查询需求列表 |
| `/api/requirements/{id}` | GET | 查看需求详情 |
| `/api/requirements/{id}` | PUT | 修改需求 |
| `/api/requirements/{id}/close` | POST | 关闭需求 |
| `/api/requirements/search/semantic` | POST | 语义查询需求 |

## 3.7 字段清单

| 字段 | 说明 | 校验 |
|---|---|---|
| requirement_no | 需求编号 | 系统生成，唯一 |
| title | 标题 | 必填，2-120字符 |
| product_line_id | 产品线 | 必填 |
| module_id | 模块 | 必填 |
| requirement_type | 类型 | 枚举 |
| status | 状态 | 枚举 |
| completeness_score | 完整度 | 0-100 |
| content_json | 结构化需求内容 | 必须符合schema |

## 3.8 提示词设计

需求池列表本身不生成内容，但语义查询会调用Embedding和相似度分析Prompt。

### 相似需求分析Prompt

输入：用户查询、召回的需求摘要。

输出：匹配原因、相似点、差异点、建议动作。

## 3.9 数据库表结构

涉及表：

- requirement
- requirement_version
- requirement_relation
- prd
- prototype
- embedding_index

## 3.10 状态机

```text
draft
  ↓ 人工确认
confirmed
  ↓ 生成PRD
prd_generated
  ↓ 生成HTML原型
prototype_generated
  ↓ 不再推进
closed
```

---

# 4. 需求详情页

## 4.1 功能概要

需求详情页展示正式需求卡片的结构化内容、来源会话、AI分析过程、相似需求、关联PRD、关联HTML原型、版本记录和操作记录。

## 4.2 流程图

```text
用户打开需求详情
  ↓
加载需求主数据
  ↓
加载来源会话与候选需求patch
  ↓
加载关联PRD、原型、Trace
  ↓
用户编辑/生成PRD/查看原型/保存版本
```

## 4.3 业务逻辑

1. 需求必须可以追溯到source_session_id和source_candidate_id。
2. 用户修改需求后生成requirement_version。
3. 需求内容变更后，关联PRD状态应标记为“可能需更新”。
4. 若需求已关闭，不允许生成PRD和原型。

## 4.4 数据权限

继承需求池权限，并额外控制来源会话展示：用户必须同时具备需求查看权限和会话查看权限，才能看到完整原始对话；否则仅展示脱敏摘要。

## 4.5 操作权限

| 操作 | 权限 |
|---|---|
| 查看详情 | 产品线权限 |
| 编辑内容 | 编辑权限 |
| 保存版本 | 编辑权限 |
| 生成PRD | 产品角色 |
| 查看Trace | 创建人、管理员 |

## 4.6 调用接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/requirements/{id}` | GET | 需求详情 |
| `/api/requirements/{id}/versions` | GET | 版本列表 |
| `/api/requirements/{id}/versions` | POST | 保存新版本 |
| `/api/requirements/{id}/source` | GET | 获取来源会话和patch |
| `/api/requirements/{id}/generate-prd` | POST | 生成PRD |

## 4.7 字段清单

需求详情复用requirement.content_json结构，核心字段包括：

- background
- businessGoal
- userRoles
- scenarios
- scope
- outOfScope
- businessRules
- fields
- permissions
- process
- exceptionCases
- acceptanceCriteria

## 4.8 提示词设计

### 需求版本摘要Prompt

当用户保存版本时，AI可以根据前后内容差异生成change_summary。

## 4.9 数据库表结构

涉及表：

- requirement
- requirement_version
- conversation_session
- conversation_message
- requirement_candidate_patch
- ai_trace

## 4.10 状态机

需求详情页不独立维护状态，使用requirement.status。

---

# 5. PRD工作台

## 5.1 功能概要

PRD工作台用于从需求卡片生成结构化PRD，并支持分章节编辑、AI补全、AI完整性检查、AI一致性检查和版本保存。

## 5.2 页面结构

```text
左侧：PRD目录
中间：章节编辑区
右侧：AI检查建议
```

## 5.3 流程图

```text
需求卡片
  ↓
AI生成PRD草稿
  ↓
按章节写入prd_section
  ↓
用户编辑章节
  ↓
AI检查完整性/一致性/可开发性/可测试性
  ↓
用户保存版本
  ↓
PRD确认
```

## 5.4 业务逻辑

1. PRD底层必须按章节结构化存储，不只保存整篇Markdown。
2. 每个章节可单独AI补全和AI检查。
3. PRD保存时应更新prd_version。
4. PRD确认后才允许生成正式HTML原型。
5. 若需求卡片被修改，PRD状态变为 `need_review`。

## 5.5 数据权限

PRD继承关联需求的数据权限。

## 5.6 操作权限

| 操作 | 权限 |
|---|---|
| 生成PRD | 产品角色 |
| 编辑PRD | 产品角色、管理员 |
| AI补全章节 | 编辑权限 |
| AI检查PRD | 查看权限 |
| 确认PRD | 产品负责人、管理员 |
| 生成HTML原型 | 已确认PRD + 编辑权限 |

## 5.7 调用接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/prds` | POST | 创建PRD |
| `/api/prds/{id}` | GET | 获取PRD详情 |
| `/api/prds/{id}/sections` | GET | 获取章节 |
| `/api/prds/{id}/sections/{sectionId}` | PUT | 更新章节 |
| `/api/prds/{id}/generate` | POST | AI生成PRD |
| `/api/prds/{id}/check` | POST | AI检查PRD |
| `/api/prds/{id}/confirm` | POST | 确认PRD |
| `/api/prds/{id}/generate-prototype` | POST | 生成HTML原型 |

## 5.8 字段清单

### PRD主表字段

| 字段 | 说明 | 校验 |
|---|---|---|
| title | PRD标题 | 必填，2-120字符 |
| requirement_id | 关联需求 | 必填 |
| version | 版本号 | 系统生成 |
| status | 状态 | 枚举 |

### PRD章节字段

| 字段 | 说明 | 校验 |
|---|---|---|
| section_type | 章节类型 | 枚举 |
| section_title | 章节标题 | 必填 |
| content_markdown | Markdown内容 | 可为空，但确认前必须完整 |
| content_json | 结构化内容 | 与章节类型匹配 |
| sort_order | 排序 | 正整数 |
| ai_generated | 是否AI生成 | boolean |
| confirmed | 是否确认 | boolean |

## 5.9 提示词设计

### 5.9.1 PRD生成Prompt

输入：需求卡片content_json、来源会话摘要、相似需求摘要。

输出：按章节输出结构化JSON和Markdown。

### 5.9.2 PRD检查Prompt

检查维度：

- 完整性。
- 一致性。
- 可开发性。
- 可测试性。

输出：问题清单、严重程度、定位章节、修改建议。

## 5.10 数据库表结构

涉及表：

- prd
- prd_section
- prd_version
- requirement
- ai_trace

## 5.11 状态机

```text
draft
  ↓ AI生成草稿
ai_generated
  ↓ 用户编辑保存
editing
  ↓ AI检查通过
reviewed
  ↓ 人工确认
confirmed
  ↓ 需求变更
need_review
  ↓ 废弃
archived
```

---

# 6. HTML原型页

## 6.1 功能概要

HTML原型页用于根据结构化PRD生成页面原型，支持页面清单、HTML预览、源码查看、局部调整、重新生成、版本保存。

## 6.2 流程图

```text
已确认PRD
  ↓
AI识别页面清单
  ↓
AI生成HTML页面
  ↓
系统保存prototype记录
  ↓
用户预览
  ↓
用户局部调整或重新生成
  ↓
保存新版本
```

## 6.3 业务逻辑

1. 一个PRD可生成多个HTML原型页面。
2. 每个原型必须记录来源PRD、来源章节、Prompt版本、模型名称和生成时间。
3. 原型调整不覆盖历史版本，生成新prototype_version。
4. MVP阶段HTML原型用于表达业务结构，不作为最终前端代码。

## 6.4 数据权限

继承PRD权限。

## 6.5 操作权限

| 操作 | 权限 |
|---|---|
| 查看原型 | PRD查看权限 |
| 生成原型 | PRD编辑权限 |
| 局部调整 | PRD编辑权限 |
| 保存版本 | PRD编辑权限 |
| 下载HTML | PRD查看权限 |

## 6.6 调用接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/prototypes` | POST | 创建原型 |
| `/api/prototypes/{id}` | GET | 获取原型详情 |
| `/api/prototypes/{id}/versions` | GET | 原型版本 |
| `/api/prototypes/{id}/regenerate` | POST | 重新生成 |
| `/api/prototypes/{id}/adjust` | POST | 局部调整 |
| `/api/prototypes/{id}/download` | GET | 下载HTML |

## 6.7 字段清单

| 字段 | 说明 | 校验 |
|---|---|---|
| page_name | 页面名称 | 必填，2-80字符 |
| page_type | 页面类型 | list/detail/form/modal/config/result |
| html_content | HTML内容 | 非空，需经过安全过滤 |
| description | 页面说明 | 可选 |
| version | 版本号 | 系统生成 |
| status | 状态 | 枚举 |
| generated_by_ai | 是否AI生成 | boolean |
| prompt_version | Prompt版本 | 必填 |
| model_name | 模型名称 | 必填 |

## 6.8 提示词设计

### HTML原型生成Prompt

输入：

- PRD结构化章节。
- 页面说明。
- 字段规则。
- 权限规则。
- 业务规则。

输出：

```json
{
  "pages": [
    {
      "pageName": "合同列表页",
      "pageType": "list",
      "description": "用于合同查询和导出",
      "html": "..."
    }
  ]
}
```

## 6.9 数据库表结构

涉及表：

- prototype
- prototype_version
- prd
- ai_trace
- artifact

## 6.10 状态机

```text
draft
  ↓ AI生成
ai_generated
  ↓ 用户保存
confirmed
  ↓ 局部调整
adjusted
  ↓ 废弃
archived
```

---

# 7. 查询中心

## 7.1 功能概要

查询中心提供统一搜索能力，支持对话、需求卡片、PRD、HTML原型和AI Trace的条件查询、关键词查询和语义查询。

## 7.2 流程图

```text
用户输入查询条件
  ↓
字段校验
  ↓
条件查询 / 关键词查询 / 语义检索
  ↓
结果聚合
  ↓
权限过滤
  ↓
展示匹配原因和关联对象
```

## 7.3 业务逻辑

1. 关键词查询必须进行非法字符校验。
2. 语义查询先调用Embedding，再召回相似对象。
3. 查询结果必须按权限过滤。
4. 语义查询结果应展示匹配原因。

## 7.4 数据权限

结果级权限过滤，用户无权查看的对象不返回。

## 7.5 操作权限

| 操作 | 权限 |
|---|---|
| 查询 | 登录用户 |
| 查看详情 | 对象查看权限 |
| 导出结果 | 管理员或授权用户 |

## 7.6 调用接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/search` | GET | 关键词/条件查询 |
| `/api/search/semantic` | POST | 语义查询 |
| `/api/search/suggest` | GET | 搜索建议 |

## 7.7 字段清单

| 字段 | 说明 | 校验 |
|---|---|---|
| keyword | 查询关键字 | 2-80字符，禁止SQL危险字符 |
| object_type | 查询对象 | conversation/requirement/prd/prototype/trace |
| product_line_id | 产品线 | 可选 |
| module_id | 模块 | 可选 |
| status | 状态 | 可选 |

## 7.8 提示词设计

### 匹配原因生成Prompt

输入：查询语句、召回对象摘要。

输出：匹配原因、相关字段、建议打开对象。

## 7.9 数据库表结构

涉及表：

- requirement
- prd
- prd_section
- prototype
- conversation_session
- conversation_message
- ai_trace
- embedding_index

## 7.10 状态机

查询中心无独立状态机。

---

# 8. AI配置中心

## 8.1 功能概要

AI配置中心用于配置模型、Prompt模板、Skill能力、输出格式和Embedding能力。MVP阶段不允许Prompt写死在代码中，所有AI能力都必须可配置、可版本化、可Trace。

## 8.2 流程图

```text
管理员进入AI配置中心
  ↓
配置模型供应商和模型参数
  ↓
配置Prompt模板和输出格式
  ↓
绑定Skill与模型/Prompt
  ↓
发布配置版本
  ↓
AI调用按最新启用版本执行
```

## 8.3 业务逻辑

1. Prompt模板支持草稿、启用、停用状态。
2. 修改已启用Prompt时，应创建新版本，不直接覆盖线上版本。
3. Skill必须绑定模型和Prompt。
4. 输出格式必须配置JSON Schema或HTML输出规则。
5. AI调用时记录模型、Prompt版本、Skill配置版本。

## 8.4 数据权限

AI配置仅管理员可见；普通用户不可查看API Key。

## 8.5 操作权限

| 操作 | 权限 |
|---|---|
| 查看配置 | AI配置管理员 |
| 新增模型 | AI配置管理员 |
| 修改Prompt | AI配置管理员 |
| 发布Prompt | AI配置管理员 + 审核权限 |
| 停用模型 | 超级管理员 |

## 8.6 调用接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/ai/models` | GET/POST | 模型配置 |
| `/api/ai/prompts` | GET/POST | Prompt模板 |
| `/api/ai/prompts/{id}/publish` | POST | 发布模板 |
| `/api/ai/skills` | GET/POST | Skill配置 |
| `/api/ai/output-schemas` | GET/POST | 输出格式配置 |

## 8.7 字段清单

### 模型配置字段

| 字段 | 说明 | 校验 |
|---|---|---|
| provider | 模型供应商 | 必填 |
| model_name | 模型名称 | 必填 |
| api_base_url | API地址 | URL格式 |
| api_key_cipher | API Key密文 | 加密存储 |
| context_window | 上下文长度 | >=4096 |
| temperature | 温度 | 0-2 |
| max_output_tokens | 最大输出 | >=512 |
| enabled | 是否启用 | boolean |

### Prompt模板字段

| 字段 | 说明 | 校验 |
|---|---|---|
| prompt_name | 模板名称 | 必填，2-80字符 |
| ability_type | 能力类型 | 枚举 |
| system_prompt | 系统提示词 | 必填 |
| user_prompt_template | 用户提示词模板 | 必填 |
| output_format | 输出格式 | JSON/HTML/Markdown |
| version | 版本 | 系统生成 |
| status | 状态 | draft/enabled/disabled |

## 8.8 提示词设计

AI配置中心本身不调用业务AI，但提供Prompt维护能力。

## 8.9 数据库表结构

涉及表：

- ai_model_config
- ai_prompt_template
- ai_skill_config
- ai_output_schema

## 8.10 状态机

### Prompt状态机

```text
draft
  ↓ 发布
enabled
  ↓ 停用
disabled
  ↓ 新建版本
draft
```

---

# 9. AI Trace记录页

## 9.1 功能概要

AI Trace记录页展示每次AI调用的输入、输出、模型、Prompt版本、耗时、Token消耗、状态、错误信息和用户反馈。用于调试Prompt、评估模型效果和审计AI行为。

## 9.2 流程图

```text
任意AI能力被调用
  ↓
后端生成trace_id
  ↓
记录输入上下文和Prompt版本
  ↓
调用模型
  ↓
记录输出、耗时、Token和状态
  ↓
用户反馈采纳情况
  ↓
Trace页查询和分析
```

## 9.3 业务逻辑

1. 所有AI调用必须记录Trace。
2. Trace中的敏感内容按权限脱敏展示。
3. 用户对AI输出可反馈：采纳、部分采纳、不采纳、重新生成。
4. AI调用失败时必须记录错误信息。
5. Trace不允许普通用户删除。

## 9.4 数据权限

| 用户 | 可查看范围 |
|---|---|
| 普通用户 | 自己触发的Trace |
| 产品负责人 | 所属产品线Trace |
| AI管理员 | 全部Trace |

## 9.5 操作权限

| 操作 | 权限 |
|---|---|
| 查看Trace | 对象查看权限 |
| 查看完整输入输出 | AI管理员或创建人 |
| 导出Trace | AI管理员 |
| 标记反馈 | 触发用户或对象编辑人 |

## 9.6 调用接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/ai/traces` | GET | 查询Trace |
| `/api/ai/traces/{id}` | GET | 查看Trace详情 |
| `/api/ai/traces/{id}/feedback` | POST | 提交反馈 |
| `/api/ai/traces/export` | POST | 导出Trace |

## 9.7 字段清单

| 字段 | 说明 | 校验 |
|---|---|---|
| ability_type | AI能力类型 | 枚举 |
| model_name | 模型名称 | 必填 |
| prompt_template_id | Prompt模板ID | 必填 |
| prompt_version | Prompt版本 | 必填 |
| input_json | 输入 | jsonb |
| output_json | 输出 | jsonb |
| token_input | 输入Token | >=0 |
| token_output | 输出Token | >=0 |
| duration_ms | 耗时 | >=0 |
| status | 状态 | success/failed/timeout |
| user_feedback | 用户反馈 | adopted/partial/rejected/regenerated |

## 9.8 提示词设计

Trace页不调用AI，但为所有Prompt和Skill优化提供数据基础。

## 9.9 数据库表结构

涉及表：

- ai_trace
- ai_action_feedback
- ai_prompt_template
- ai_model_config

## 9.10 状态机

### AI Trace状态机

```text
created
  ↓ 调用成功
success
  ↓ 用户反馈
feedback_recorded

created
  ↓ 调用失败
failed

created
  ↓ 超时
timeout
```

---

# 10. 全局前端校验规则

| 场景 | 校验规则 |
|---|---|
| 文本输入 | 去除首尾空格，禁止仅空格 |
| 标题 | 2-120字符，需求标题建议不超过60字符 |
| 搜索框 | 2-80字符，禁止单引号、双引号、分号、反斜杠 |
| 模型温度 | 0-2 |
| 上下文长度 | 不小于4096 |
| 最大输出Token | 不小于512 |
| JSON输出 | 必须符合配置的JSON Schema |
| HTML原型 | 必须经过XSS过滤，不允许script标签直接执行 |

---

# 11. 全局异常处理

| 异常 | 处理方式 |
|---|---|
| AI服务不可用 | Toast错误提示，记录ai_trace失败状态 |
| Prompt输出JSON解析失败 | 标记Trace失败，提示用户重新生成 |
| 数据库写入失败 | 回滚当前事务，保留原始消息 |
| 权限不足 | 弹窗或Toast提示无权限 |
| 会话不存在 | 跳转会话列表并提示 |
| 需求已关闭 | 禁止编辑和转PRD |
| PRD未确认 | 禁止生成正式原型 |

