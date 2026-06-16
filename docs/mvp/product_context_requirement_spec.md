# AI需求工作台——产品知识上下文管理与结构化检索需求说明书

## 1. 文档信息

- 文档名称：产品知识上下文管理与结构化检索需求说明书
- 所属系统：AI需求工作台
- 版本：V1.0
- 状态：需求草案
- 核心目标：让AI在分析需求时，优先基于结构化产品知识进行精确查询，并在必要时使用语义检索兜底。

## 2. 背景与问题

当前系统已经具备 AI Router、AiOrchestrator、需求抽取、完整度检查、候选需求、正式需求卡片和 Trace，但需求分析主要参考当前会话，尚未充分利用历史产品信息。

系统需要补充一层“产品知识上下文”，解决以下问题：

- 当前需求属于哪个产品线、模块和功能；
- 是否与已有功能重复；
- 是否违反现行业务规则；
- 是否遗漏权限、状态、字段、流程约束；
- 当前描述是新增需求、规则变更还是历史事实说明；
- 本轮需求分析引用了哪些产品事实。

## 3. 产品目标

核心链路：

```text
用户输入
  ↓
intent_router
  ↓
产品实体识别
  ↓
标准名称与别名归一化
  ↓
结构化数据库查询
  ↓
上下文组装
  ↓
requirement_extract
  ↓
completeness_check
  ↓
reply_generate
```

设计原则：

1. 结构化查询优先；
2. 向量检索仅作为模糊表达和资料缺失时的兜底；
3. 模型只负责识别和建议，不直接查询或写数据库；
4. 后端负责参数校验、查询执行、权限和上下文组装；
5. AI输出必须区分事实、历史参考、推断、缺失项和冲突；
6. 每条上下文必须可追溯来源；
7. 无历史知识时仍可分析，但要提示“仅基于当前会话和通用规则”。

## 4. MVP范围

### 4.1 本期包含

- 产品线上下文配置；
- 模块和功能配置；
- 术语与别名配置；
- 业务规则配置；
- 实体识别能力；
- 结构化查询能力；
- 上下文组装能力；
- 需求分析自动注入上下文；
- 上下文命中结果预览；
- Trace记录识别与查询过程；
- 无命中、歧义、冲突处理。

### 4.2 本期不包含

- 大规模历史文档切片；
- 向量数据库和Embedding；
- 代码知识；
- 图数据库；
- MCP知识服务；
- 自动导入全部历史PRD。

## 5. 信息架构

```text
产品知识
├─ 产品线配置
├─ 模块与功能
├─ 术语与别名
├─ 业务规则
├─ 上下文调试
└─ 知识查询记录
```

需求对话页增加：当前识别产品线、模块、功能、置信度、命中规则、冲突、上下文详情和手工修正入口。

# 6. 页面一：产品线配置

## 6.1 功能概要

维护产品线级基础背景，作为实体识别和需求分析的一级上下文。

## 6.2 功能

- 列表、搜索、新增、编辑、启停；
- 查看模块数、功能数和知识完整度；
- 删除仅允许无关联数据时执行。

## 6.3 字段

| 字段 | 类型 | 必填 | 规则 |
|---|---|---:|---|
| 产品线编码 | varchar(64) | 是 | 唯一，只允许字母、数字、下划线 |
| 产品线名称 | varchar(120) | 是 | 2-120字符 |
| 产品定位 | text | 是 | 10-2000字符 |
| 目标用户 | jsonb | 否 | 数组 |
| 业务范围 | jsonb | 否 | 数组 |
| 关键词 | jsonb | 否 | 数组 |
| 状态 | varchar(20) | 是 | enabled/disabled |
| 排序号 | int | 是 | 0-9999 |

## 6.4 业务逻辑

- 停用产品线不参与识别和查询；
- 已被引用的数据不物理删除；
- 产品线名称变更时保留历史别名。

## 6.5 接口

```text
GET    /api/product-context/product-lines
POST   /api/product-context/product-lines
PUT    /api/product-context/product-lines/{id}
DELETE /api/product-context/product-lines/{id}
```

# 7. 页面二：模块与功能

## 7.1 层级

```text
产品线
  └─ 模块
      └─ 功能
          └─ 业务对象/操作
```

## 7.2 模块字段

| 字段 | 类型 | 必填 |
|---|---|---:|
| 所属产品线 | bigint | 是 |
| 模块编码 | varchar(64) | 是 |
| 模块名称 | varchar(120) | 是 |
| 模块说明 | text | 是 |
| 主要角色 | jsonb | 否 |
| 上游模块 | jsonb | 否 |
| 下游模块 | jsonb | 否 |
| 状态 | varchar(20) | 是 |

## 7.3 功能字段

| 字段 | 类型 | 必填 |
|---|---|---:|
| 所属模块 | bigint | 是 |
| 功能编码 | varchar(64) | 是 |
| 功能名称 | varchar(120) | 是 |
| 功能说明 | text | 是 |
| 业务对象 | varchar(120) | 否 |
| 操作类型 | varchar(64) | 否 |
| 关键词 | jsonb | 否 |
| 状态 | varchar(20) | 是 |

# 8. 页面三：术语与别名

维护标准术语和自然语言别名。例如：

| 标准术语 | 别名 |
|---|---|
| 到期清分 | 到期结算、融资到期处理 |
| 自动出金 | 自动划款、自动划转、系统出金 |
| 手动出金 | 人工划款、财务出金 |

业务逻辑：

1. 同一范围内标准术语不可重复；
2. 同一别名命中多个标准术语时标记歧义；
3. 歧义词不能直接驱动精确查询；
4. 必须提示用户选择候选项。

# 9. 页面四：业务规则

## 9.1 规则类型

- function_rule；
- field_rule；
- permission_rule；
- process_rule；
- state_rule；
- validation_rule；
- audit_rule；
- exception_rule。

## 9.2 字段

| 字段 | 类型 | 必填 |
|---|---|---:|
| 规则编码 | varchar(64) | 是 |
| 规则名称 | varchar(200) | 是 |
| 产品线 | bigint | 是 |
| 模块 | bigint | 否 |
| 功能 | bigint | 否 |
| 规则类型 | varchar(40) | 是 |
| 规则内容 | jsonb | 是 |
| 来源类型 | varchar(40) | 是 |
| 来源编号 | varchar(120) | 否 |
| 版本号 | varchar(32) | 是 |
| 生效状态 | varchar(20) | 是 |
| 生效时间 | timestamp | 否 |
| 失效时间 | timestamp | 否 |

只有 active 规则进入上下文。

# 10. 页面五：上下文调试

## 10.1 功能概要

验证用户输入能否识别产品线、模块、功能，并查看结构化查询和上下文组装结果。

示例输入：

```text
云信监管原本的到期清分逻辑为手动出金，
云数云租融资合并项目上线后变为自动出金。
```

预期实体识别：

```json
{
  "productLine": "云信监管",
  "module": "到期清分",
  "feature": "自动出金",
  "businessObject": "账户资金",
  "operation": "规则变更",
  "confidence": 0.94
}
```

支持：执行识别、手工修正、重新查询、查看来源、复制上下文JSON、模拟注入需求分析。

异常提示：

| 场景 | 提示 |
|---|---|
| 未识别产品线 | 未识别到明确产品线，本轮仅基于当前会话分析 |
| 模块有多个候选 | 识别到多个可能模块，请选择 |
| 无规则命中 | 未查询到有效业务规则 |
| 存在冲突 | 当前描述与现行规则存在冲突，请确认是否为规则变更 |
| 查询失败 | 上下文查询失败，请稍后重试 |

# 11. 需求对话页改造

右侧增加“本轮产品上下文”：

```text
产品线：云信监管
模块：到期清分
功能：自动出金
识别置信度：94%
命中规则：6条
历史需求：2条
冲突：1条
```

用户可以：

- 手工修正产品线、模块和功能；
- 查看上下文详情；
- 排除错误规则；
- 标记本轮必须参考规则；
- 未识别时手工选择。

# 12. AI能力设计

## 12.1 entity_resolver

输入：用户最新输入、当前候选需求、产品线/模块/功能候选字典、术语别名。

输出：

```json
{
  "productLineCode": "YUNXIN_REGULATION",
  "moduleCode": "MATURITY_CLEARING",
  "featureCode": "AUTO_WITHDRAWAL",
  "businessObject": "account_funds",
  "operationType": "rule_change",
  "confidence": 0.94,
  "ambiguities": [],
  "reason": "用户明确提到云信监管、到期清分和自动出金"
}
```

## 12.2 context_query_plan

由后端生成查询计划，不要求模型直接查数据库。

## 12.3 ContextBuilder输出

```json
{
  "productContext": {},
  "moduleContext": {},
  "featureContext": {},
  "terms": [],
  "activeRules": [],
  "historicalRequirements": [],
  "conflicts": [],
  "warnings": [],
  "sources": []
}
```

# 13. Prompt设计

Entity Resolver System Prompt：

```text
你是企业产品知识实体识别器。
你只能从系统提供的产品线、模块、功能和术语候选中选择，不得创造不存在的名称。
当存在多个候选时，必须输出 ambiguities，不得自行猜测。
只输出符合JSON Schema的JSON。
```

需求抽取新增变量：

```text
{{productContext}}
{{moduleContext}}
{{featureContext}}
{{activeRules}}
{{historicalRequirements}}
{{contextWarnings}}
```

完整度检查结果必须区分：

```json
{
  "confirmedFacts": [],
  "historicalReferences": [],
  "inferences": [],
  "missingInformation": [],
  "conflicts": []
}
```

# 14. 数据库结构

## product_context

```sql
id bigint primary key
product_line_id bigint not null
overview text not null
target_users jsonb
business_scope jsonb
keywords jsonb
status varchar(20) not null
created_at timestamp not null
updated_at timestamp not null
```

## module_context

```sql
id bigint primary key
module_id bigint not null
overview text not null
main_roles jsonb
upstream_modules jsonb
downstream_modules jsonb
status varchar(20) not null
created_at timestamp not null
updated_at timestamp not null
```

## product_feature

```sql
id bigint primary key
module_id bigint not null
feature_code varchar(64) not null
feature_name varchar(120) not null
description text not null
business_object varchar(120)
operation_type varchar(64)
keywords jsonb
status varchar(20) not null
```

## business_term

```sql
id bigint primary key
product_line_id bigint
module_id bigint
feature_id bigint
term_name varchar(120) not null
aliases jsonb not null
definition text not null
status varchar(20) not null
```

## business_rule

```sql
id bigint primary key
rule_code varchar(64) not null unique
rule_name varchar(200) not null
product_line_id bigint not null
module_id bigint
feature_id bigint
rule_type varchar(40) not null
rule_content jsonb not null
source_type varchar(40) not null
source_id varchar(120)
version varchar(32) not null
effective_status varchar(20) not null
effective_from timestamp
effective_to timestamp
```

## context_query_trace

```sql
id bigint primary key
session_id bigint
message_id bigint
entity_result jsonb
query_plan jsonb
matched_context jsonb
conflicts jsonb
warnings jsonb
duration_ms int
created_at timestamp not null
```

# 15. 权限

- 产品知识按产品线授权；
- 用户只能查看有权限产品线；
- entity_resolver 候选字典仅包含有权限数据；
- Trace不得泄露无权限知识；
- 编辑权限与查看权限分离。

# 16. 状态机

```text
产品知识：draft → enabled → disabled
业务规则：draft → active → expired / disabled
识别状态：unresolved → resolved / ambiguous / no_match
```

# 17. 验收标准

1. 可维护产品线、模块、功能、术语和规则；
2. 停用数据不参与识别；
3. 示例输入能识别云信监管、到期清分、自动出金和规则变更；
4. 后端根据结构化参数精确查询；
5. 每条结果可追溯来源；
6. requirement_extract 能读取上下文；
7. completeness_check 能基于历史规则发现缺失项；
8. 无命中不阻断对话；
9. 多候选要求用户确认；
10. 查询失败记录Trace；
11. 模型不能直接操作数据库。

# 18. 后续扩展

- 历史PRD导入；
- 知识文档切片；
- pgvector与Embedding；
- 语义检索和混合检索；
- 代码知识；
- 影响分析；
- MCP知识服务。

最终策略：结构化查询为主，关键词和别名匹配增强，语义检索兜底。
