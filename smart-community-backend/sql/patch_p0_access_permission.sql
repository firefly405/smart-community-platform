-- P0: 门禁权限表（演示闭环）
CREATE TABLE IF NOT EXISTS `guard_access_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `access_id` bigint NOT NULL COMMENT '门禁设备ID',
  `user_id` bigint NOT NULL COMMENT '授权用户/业主ID',
  `user_type` tinyint DEFAULT '1' COMMENT '用户类型 1-业主 2-家属 3-租户 4-访客',
  `start_time` datetime DEFAULT NULL COMMENT '有效期起',
  `end_time` datetime DEFAULT NULL COMMENT '有效期止',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_access_id` (`access_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门禁权限表';
