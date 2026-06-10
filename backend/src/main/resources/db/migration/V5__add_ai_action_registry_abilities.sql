UPDATE prompt_template
SET user_prompt = '请基于用户最新输入、会话状态、当前候选需求和最近对话判断本轮意图。\n\n最新输入：\n{{latestMessage}}\n\n会话状态：{{sessionStatus}}\n当前阶段：{{currentStage}}\n\n当前候选需求：\n{{currentCandidatesJson}}\n\n最近对话：\n{{recentMessagesJson}}\n\n必须识别以下 intent：new_requirement、supplement_requirement、modify_requirement、confirm_requirement、reject_requirement、split_requirement、generate_card、query_requirement、small_talk、unclear。\n\nnextActions 只能从以下值选择：extract_requirement、extract_requirement_info、split_requirement、check_completeness、generate_reply、generate_requirement_card、search_similar_requirements。对于普通交流或无法判断，nextActions 只返回 ["generate_reply"]。如果用户输入过短、只有数字、只有符号或缺少业务语义，例如“1”，intent 必须为 unclear，且不要触发 extract_requirement。没有候选需求时不要建议 generate_requirement_card。低置信度时只建议 generate_reply。',
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 1
WHERE template_code = 'default_intent_router';

INSERT INTO prompt_template (
  id, ability_type, template_code, template_name, version, system_prompt, user_prompt, json_schema,
  status, is_default, created_by, updated_by
) VALUES
(
  3000000000106,
  'requirement_split',
  'default_requirement_split',
  '需求拆分默认模板',
  'v1.0',
  '你是资深产品经理。你负责把复合需求拆成多个独立候选需求 patch。只输出符合 JSON Schema 的 JSON，不要输出 Markdown、解释或额外文本。',
  '请根据用户最新消息和当前候选需求，将复合诉求拆分为多个候选需求 patch。\n\n最新消息：\n{{latestMessage}}\n\n当前候选需求：\n{{currentCandidatesJson}}\n\n要求：每个 patch 必须有独立 title、operation、fields、confidence。fields 放结构化信息，例如 background、businessGoal、scenarios、scope、businessRules、permissions、fields、acceptanceCriteria、exceptionCases。',
  $${"type":"object","required":["patches","reason"],"properties":{"patches":{"type":"array","items":{"type":"object","required":["title","operation","fields","confidence"],"properties":{"title":{"type":"string"},"operation":{"type":"string"},"fields":{"type":"object"},"confidence":{"type":"number"}}}},"reason":{"type":"string"}}}$$::jsonb,
  'enabled',
  TRUE,
  1,
  1
),
(
  3000000000107,
  'card_generate',
  'default_card_generate',
  '需求卡片生成默认模板',
  'v1.0',
  '你是严谨的需求卡片生成助手。你只基于候选需求生成需求卡片草稿，不直接创建数据库正式需求。只输出符合 JSON Schema 的 JSON，不要输出 Markdown、解释或额外文本。',
  '请根据候选需求生成正式需求卡片草稿。\n\n候选需求：\n{{candidateJson}}\n\n要求：如果缺少产品线、模块、标题、业务目标、范围、验收标准等关键字段，应放入 missingRequiredFields，并将 readyToCreateRequirement 设为 false。',
  $${"type":"object","required":["title","content","missingRequiredFields","readyToCreateRequirement","reason"],"properties":{"title":{"type":"string"},"content":{"type":"object"},"missingRequiredFields":{"type":"array","items":{"type":"string"}},"readyToCreateRequirement":{"type":"boolean"},"reason":{"type":"string"}}}$$::jsonb,
  'enabled',
  TRUE,
  1,
  1
),
(
  3000000000108,
  'similar_requirement_search',
  'default_similar_requirement_search',
  '相似需求搜索默认模板',
  'v1.0',
  '你是需求检索助手。你根据用户输入和已有需求摘要判断相似需求。只输出符合 JSON Schema 的 JSON，不要输出 Markdown、解释或额外文本。',
  '请从已有需求中找出与用户输入相似的需求。\n\n最新消息：\n{{latestMessage}}\n\n已有需求：\n{{existingRequirementsJson}}\n\n要求：最多返回 5 条。similarity 为 0-1 的数字，reason 说明相似原因。',
  $${"type":"object","required":["items","summary"],"properties":{"items":{"type":"array","items":{"type":"object","required":["requirementId","requirementNo","title","similarity","reason"],"properties":{"requirementId":{"type":"integer"},"requirementNo":{"type":"string"},"title":{"type":"string"},"similarity":{"type":"number"},"reason":{"type":"string"}}}},"summary":{"type":"string"}}}$$::jsonb,
  'enabled',
  TRUE,
  1,
  1
)
ON CONFLICT (ability_type, version) DO UPDATE
SET template_code = EXCLUDED.template_code,
    template_name = EXCLUDED.template_name,
    system_prompt = EXCLUDED.system_prompt,
    user_prompt = EXCLUDED.user_prompt,
    json_schema = EXCLUDED.json_schema,
    status = EXCLUDED.status,
    is_default = EXCLUDED.is_default,
    updated_by = EXCLUDED.updated_by,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO ai_ability_config (
  id, ability_type, ability_name, enabled, model_config_id, prompt_template_id,
  fallback_to_mock, status, created_by, updated_by
) VALUES
  (3000000000206, 'requirement_split', '需求拆分', TRUE, 3000000000001, 3000000000106, FALSE, 'enabled', 1, 1),
  (3000000000207, 'card_generate', '需求卡片生成', TRUE, 3000000000001, 3000000000107, FALSE, 'enabled', 1, 1),
  (3000000000208, 'similar_requirement_search', '相似需求搜索', TRUE, 3000000000001, 3000000000108, FALSE, 'enabled', 1, 1)
ON CONFLICT (ability_type) DO UPDATE
SET ability_name = EXCLUDED.ability_name,
    enabled = EXCLUDED.enabled,
    model_config_id = EXCLUDED.model_config_id,
    prompt_template_id = EXCLUDED.prompt_template_id,
    fallback_to_mock = EXCLUDED.fallback_to_mock,
    status = EXCLUDED.status,
    updated_by = EXCLUDED.updated_by,
    updated_at = CURRENT_TIMESTAMP;
