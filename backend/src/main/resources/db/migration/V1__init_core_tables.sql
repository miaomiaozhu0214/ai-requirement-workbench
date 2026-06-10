CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  display_name VARCHAR(64) NOT NULL,
  email VARCHAR(128),
  role_code VARCHAR(32) NOT NULL DEFAULT 'user',
  status VARCHAR(32) NOT NULL DEFAULT 'enabled',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_user_role ON sys_user(role_code);

CREATE TABLE product_line (
  id BIGINT PRIMARY KEY,
  line_code VARCHAR(64) NOT NULL UNIQUE,
  line_name VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  status VARCHAR(32) NOT NULL DEFAULT 'enabled',
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_product_line_status ON product_line(status);

CREATE TABLE product_module (
  id BIGINT PRIMARY KEY,
  product_line_id BIGINT NOT NULL,
  module_code VARCHAR(64) NOT NULL,
  module_name VARCHAR(100) NOT NULL,
  parent_id BIGINT,
  description VARCHAR(500),
  status VARCHAR(32) NOT NULL DEFAULT 'enabled',
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT uk_product_module_line_code UNIQUE(product_line_id, module_code)
);

CREATE INDEX idx_product_module_line ON product_module(product_line_id);
CREATE INDEX idx_product_module_parent ON product_module(parent_id);

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

CREATE INDEX idx_conversation_created_by ON conversation_session(created_by);
CREATE INDEX idx_conversation_status ON conversation_session(status);
CREATE INDEX idx_conversation_stage ON conversation_session(current_stage);
CREATE INDEX idx_conversation_last_message ON conversation_session(last_message_at);

CREATE TABLE conversation_message (
  id BIGINT PRIMARY KEY,
  session_id BIGINT NOT NULL,
  role VARCHAR(16) NOT NULL,
  message_type VARCHAR(32) NOT NULL DEFAULT 'text',
  content TEXT NOT NULL,
  command_name VARCHAR(64),
  metadata_json JSONB,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_message_session_created ON conversation_message(session_id, created_at);
CREATE INDEX idx_message_role ON conversation_message(role);
CREATE INDEX idx_message_type ON conversation_message(message_type);

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

CREATE INDEX idx_candidate_session ON requirement_candidate(session_id);
CREATE INDEX idx_candidate_status ON requirement_candidate(status);
CREATE INDEX idx_candidate_score ON requirement_candidate(completeness_score);
CREATE INDEX idx_candidate_created_from_msg ON requirement_candidate(created_from_message_id);

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

CREATE INDEX idx_ai_trace_session ON ai_trace(session_id);
CREATE INDEX idx_ai_trace_object ON ai_trace(business_object_type, business_object_id);
CREATE INDEX idx_ai_trace_ability ON ai_trace(ability_type);
CREATE INDEX idx_ai_trace_status ON ai_trace(status);
CREATE INDEX idx_ai_trace_created ON ai_trace(created_at);

CREATE TABLE requirement_candidate_patch (
  id BIGINT PRIMARY KEY,
  candidate_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  source_message_id BIGINT,
  patch_type VARCHAR(32) NOT NULL DEFAULT 'update',
  patch_json JSONB NOT NULL,
  before_json JSONB,
  after_json JSONB,
  ai_trace_id BIGINT,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_candidate_patch_candidate ON requirement_candidate_patch(candidate_id, created_at);
CREATE INDEX idx_candidate_patch_session ON requirement_candidate_patch(session_id);
CREATE INDEX idx_candidate_patch_message ON requirement_candidate_patch(source_message_id);
CREATE INDEX idx_candidate_patch_trace ON requirement_candidate_patch(ai_trace_id);

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

CREATE INDEX idx_requirement_source_session ON requirement(source_session_id);
CREATE INDEX idx_requirement_source_candidate ON requirement(source_candidate_id);
CREATE INDEX idx_requirement_product_module ON requirement(product_line_id, module_id);
CREATE INDEX idx_requirement_status ON requirement(status);
CREATE INDEX idx_requirement_type ON requirement(requirement_type);
CREATE INDEX idx_requirement_created ON requirement(created_at);

CREATE TABLE requirement_version (
  id BIGINT PRIMARY KEY,
  requirement_id BIGINT NOT NULL,
  version_no INTEGER NOT NULL,
  title VARCHAR(120) NOT NULL,
  content_json JSONB NOT NULL,
  change_summary VARCHAR(1000),
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_requirement_version UNIQUE(requirement_id, version_no)
);

CREATE INDEX idx_requirement_version_created ON requirement_version(requirement_id, created_at);
