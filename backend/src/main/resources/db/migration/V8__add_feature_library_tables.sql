CREATE TABLE IF NOT EXISTS feature_node (
  id BIGINT PRIMARY KEY,
  product_line_id BIGINT NOT NULL,
  parent_id BIGINT NULL,
  name VARCHAR(200) NOT NULL,
  description TEXT NULL,
  node_type VARCHAR(40) NOT NULL,
  status VARCHAR(40) NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_feature_node_type CHECK (node_type IN ('module', 'feature')),
  CONSTRAINT ck_feature_node_status CHECK (status IN ('added', 'modified', 'deleted', 'unchanged'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_feature_node_sibling_name_active
  ON feature_node (product_line_id, COALESCE(parent_id, 0), LOWER(name))
  WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_feature_node_product_line
  ON feature_node (product_line_id, deleted, sort_order);

CREATE INDEX IF NOT EXISTS idx_feature_node_parent
  ON feature_node (parent_id, deleted, sort_order);

CREATE TABLE IF NOT EXISTS feature_content_block (
  id BIGINT PRIMARY KEY,
  feature_id BIGINT NOT NULL,
  block_type VARCHAR(40) NOT NULL,
  title VARCHAR(200) NULL,
  content TEXT NOT NULL,
  metadata JSONB NULL,
  source_ref VARCHAR(200) NULL,
  sort_order INTEGER NOT NULL DEFAULT 0,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_feature_content_block_type CHECK (block_type IN ('overview', 'rule', 'field', 'api', 'screenshot'))
);

CREATE INDEX IF NOT EXISTS idx_feature_content_block_feature
  ON feature_content_block (feature_id, deleted, sort_order);

CREATE TABLE IF NOT EXISTS feature_history (
  id BIGINT PRIMARY KEY,
  feature_id BIGINT NOT NULL,
  operation_type VARCHAR(40) NOT NULL,
  description TEXT NOT NULL,
  operator_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_feature_history_operation CHECK (operation_type IN ('added', 'modified', 'deleted', 'moved'))
);

CREATE INDEX IF NOT EXISTS idx_feature_history_feature
  ON feature_history (feature_id, created_at DESC);
