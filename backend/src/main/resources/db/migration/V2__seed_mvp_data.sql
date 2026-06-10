INSERT INTO sys_user (id, username, display_name, email, role_code, status)
VALUES (1, 'admin', '朱迪', 'admin@example.com', 'admin', 'enabled')
ON CONFLICT (id) DO NOTHING;

INSERT INTO product_line (id, line_code, line_name, description, created_by, updated_by)
VALUES
  (1001, 'scenario_2', '场景2.0', 'MVP默认产品线', 1, 1),
  (1002, 'cloud_lease', '云租', 'MVP示例产品线', 1, 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO product_module (id, product_line_id, module_code, module_name, description, created_by, updated_by)
VALUES
  (2001, 1001, 'contract', '合同管理', '合同列表、详情、导出等能力', 1, 1),
  (2002, 1001, 'order', '订单管理', '订单状态、查询和流转能力', 1, 1),
  (2003, 1002, 'quota', '额度审批', '额度审批与查询能力', 1, 1)
ON CONFLICT (id) DO NOTHING;
