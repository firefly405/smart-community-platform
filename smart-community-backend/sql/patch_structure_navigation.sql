-- Structure phase: additive and repeatable; preserve disabled/hidden existing menus.
START TRANSACTION;
INSERT INTO sys_menu (parent_id, menu_name, menu_type, icon, path, sort, visible, status)
SELECT 0, 'AI服务', 1, 'ai', '/ai', 9, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 0 AND path IN ('/ai', 'ai'));

INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, permission, sort, visible, status)
SELECT p.id, '智能问答', 2, 'qa', 'ai/QaPage', 'ai:qa:list', 1, 1, 1
FROM sys_menu p WHERE p.parent_id = 0 AND p.path IN ('/ai', 'ai')
AND NOT EXISTS (SELECT 1 FROM sys_menu c WHERE c.parent_id = p.id AND c.path IN ('qa', '/ai/qa'));

INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, permission, sort, visible, status)
SELECT p.id, '知识库管理', 2, 'knowledge', 'ai/KnowledgeList', 'ai:knowledge:list', 2, 1, 1
FROM sys_menu p WHERE p.parent_id = 0 AND p.path IN ('/ai', 'ai')
AND NOT EXISTS (SELECT 1 FROM sys_menu c WHERE c.parent_id = p.id AND c.path IN ('knowledge', '/ai/knowledge'));

INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, permission, sort, visible, status)
SELECT p.id, '设备维保', 2, 'maintain', 'property/MaintainList', 'service:maintain:list', 7, 1, 1
FROM sys_menu p WHERE p.parent_id = 0 AND p.path IN ('/service', 'service')
AND NOT EXISTS (SELECT 1 FROM sys_menu c WHERE c.parent_id = p.id AND c.path IN ('maintain', '/service/maintain'));

INSERT INTO sys_menu (parent_id, menu_name, menu_type, icon, path, component, permission, sort, visible, status)
SELECT p.id, 'AI配置', 2, 'ai-config', 'ai-config', 'system/AiConfig', 'system:ai-config:edit', 8, 1, 1
FROM sys_menu p WHERE p.parent_id = 0 AND p.path IN ('/system', 'system')
AND NOT EXISTS (SELECT 1 FROM sys_menu c WHERE c.parent_id = p.id AND c.path IN ('ai-config', '/system/ai-config'));

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m
WHERE r.id = 1 AND (m.path = '/ai' OR
(m.path IN ('qa', 'knowledge') AND m.parent_id IN (SELECT id FROM sys_menu WHERE path IN ('/ai','ai'))) OR
(m.path = 'maintain' AND m.parent_id IN (SELECT id FROM sys_menu WHERE path IN ('/service','service'))) OR
(m.path = 'ai-config' AND m.parent_id IN (SELECT id FROM sys_menu WHERE path IN ('/system','system'))))
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);
COMMIT;
