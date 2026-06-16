ALTER TABLE ai_ability_config
  ADD COLUMN IF NOT EXISTS execution_order INTEGER NOT NULL DEFAULT 100;

UPDATE ai_ability_config
SET execution_order = CASE ability_type
  WHEN 'requirement_split' THEN 10
  WHEN 'requirement_extract' THEN 20
  WHEN 'similar_requirement_search' THEN 25
  WHEN 'completeness_check' THEN 30
  WHEN 'card_generate' THEN 40
  WHEN 'reply_generate' THEN 100
  ELSE execution_order
END
WHERE execution_order = 100
  AND ability_type IN (
    'requirement_split',
    'requirement_extract',
    'similar_requirement_search',
    'completeness_check',
    'card_generate',
    'reply_generate'
  );

ALTER TABLE ai_ability_config
  ADD CONSTRAINT ck_ai_ability_execution_order_range CHECK (execution_order BETWEEN 0 AND 9999);
