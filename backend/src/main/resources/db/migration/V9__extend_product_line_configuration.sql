ALTER TABLE product_line
  ALTER COLUMN line_code DROP NOT NULL;

ALTER TABLE product_line
  ALTER COLUMN description TYPE TEXT;

ALTER TABLE product_line
  ADD COLUMN IF NOT EXISTS owners JSONB NOT NULL DEFAULT '["admin"]'::jsonb,
  ADD COLUMN IF NOT EXISTS product_type VARCHAR(40) NOT NULL DEFAULT 'face_to_customer',
  ADD COLUMN IF NOT EXISTS platforms JSONB NOT NULL DEFAULT '["yunlian_front"]'::jsonb,
  ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS is_processing BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE product_line
SET owners = '["admin"]'::jsonb
WHERE owners = '[]'::jsonb;

ALTER TABLE product_line
  ADD CONSTRAINT ck_product_line_type CHECK (product_type IN ('face_to_customer', 'internal', 'public_service', 'design_spec'));

CREATE UNIQUE INDEX IF NOT EXISTS uk_product_line_name_active
  ON product_line (LOWER(line_name))
  WHERE deleted = FALSE;
