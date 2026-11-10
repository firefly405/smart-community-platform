-- P1 C-08/C-10: 服务预约菜单补丁（幂等）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `icon`, `path`, `component`, `permission`, `sort`, `visible`, `status`, `remark`)
SELECT 607, 6, '服务预约', 2, 'service-order', 'service-order', 'life/ServiceOrderList', 'life:service-order:list', 4, 1, 1, '服务预约'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 607 OR `menu_name` = '服务预约');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 607 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 607);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, 607 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 2 AND `menu_id` = 607);
