ALTER TABLE prompt_template ADD COLUMN IF NOT EXISTS template_code VARCHAR(100);

UPDATE prompt_template
SET template_code = CASE ability_type
  WHEN 'intent_recognition' THEN 'default_intent_recognition'
  WHEN 'requirement_extract' THEN 'default_requirement_extract'
  WHEN 'completeness_check' THEN 'default_completeness_check'
  WHEN 'reply_generate' THEN 'default_reply_generate'
  ELSE ability_type || '_' || version
END
WHERE template_code IS NULL OR template_code = '';

ALTER TABLE prompt_template ALTER COLUMN template_code SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_prompt_template_code ON prompt_template(template_code);

ALTER TABLE ai_model_config ADD COLUMN IF NOT EXISTS api_key_secret TEXT;

UPDATE ai_model_config
SET api_key_secret = api_key_env,
    api_key_env = 'OPENAI_API_KEY'
WHERE api_key_env LIKE 'sk-%'
  AND (api_key_secret IS NULL OR api_key_secret = '');

INSERT INTO prompt_template (
  id, ability_type, template_code, template_name, version, system_prompt, user_prompt, json_schema,
  status, is_default, created_by, updated_by
) VALUES (
  3000000000105,
  'intent_router',
  'default_intent_router',
  '默认AI路由识别模板',
  'v1.0',
  '你是 AI需求工作台的意图路由器。只输出符合 JSON Schema 的 JSON，不要输出 Markdown、解释或额外文本。你只判断意图、目标对象和下一步能力，不直接生成需求内容，不执行数据库写入。',
  '请基于用户最新输入、会话状态、当前候选需求和最近对话判断本轮意图。\n\n最新输入：\n{{latestMessage}}\n\n会话状态：{{sessionStatus}}\n当前阶段：{{currentStage}}\n\n当前候选需求：\n{{currentCandidatesJson}}\n\n最近对话：\n{{recentMessagesJson}}\n\n必须识别以下 intent：new_requirement、supplement_requirement、modify_requirement、confirm_requirement、reject_requirement、split_requirement、generate_card、query_requirement、small_talk、unclear。\n\nnextActions 只能从以下值选择：extract_requirement、check_completeness、generate_reply、generate_card、query_requirement。对于普通交流或无法判断，nextActions 只返回 ["generate_reply"]。如果用户输入过短、只有数字、只有符号或缺少业务语义，例如“1”，intent 必须为 unclear，且不要触发 extract_requirement。',
  $${"type":"object","required":["intent","confidence","targetType","targetId","mayContainMultipleRequirements","nextActions","reason"],"properties":{"intent":{"type":"string"},"confidence":{"type":"number"},"targetType":{"type":"string"},"targetId":{"type":["string","null"]},"mayContainMultipleRequirements":{"type":"boolean"},"nextActions":{"type":"array","items":{"type":"string"}},"reason":{"type":"string"}}}$$::jsonb,
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
) VALUES (
  3000000000205,
  'intent_router',
  'AI路由识别',
  TRUE,
  3000000000001,
  3000000000105,
  FALSE,
  'enabled',
  1,
  1
)
ON CONFLICT (ability_type) DO UPDATE
SET ability_name = EXCLUDED.ability_name,
    enabled = EXCLUDED.enabled,
    model_config_id = EXCLUDED.model_config_id,
    prompt_template_id = EXCLUDED.prompt_template_id,
    fallback_to_mock = EXCLUDED.fallback_to_mock,
    status = EXCLUDED.status,
    updated_by = EXCLUDED.updated_by,
    updated_at = CURRENT_TIMESTAMP;
