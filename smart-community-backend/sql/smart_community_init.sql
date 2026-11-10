-- ============================================
-- 智慧社区综合管理平台 - 数据库完整初始化SQL
-- 版本: v1.0
-- 日期: 2026-05-17
-- 数据库: smart_community
-- 表总数: 57张
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS smart_community DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE smart_community;

-- ============================================
-- 1. 系统管理表 (14张)
-- ============================================

-- 1.1 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `gender` tinyint DEFAULT '0' COMMENT '性别 0-未知 1-男 2-女',
  `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `user_type` tinyint DEFAULT '5' COMMENT '用户类型 1-业主 2-家属 3-租户 4-物业人员 5-管理员',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 1.2 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `role_sort` int DEFAULT '0' COMMENT '显示顺序',
  `data_scope` tinyint DEFAULT '1' COMMENT '数据范围 1-全部 2-本部门 3-本部门及子部门 4-仅本人',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 1.3 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 1.4 菜单表
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `menu_type` tinyint DEFAULT '1' COMMENT '菜单类型 1-目录 2-菜单 3-按钮',
  `icon` varchar(100) DEFAULT NULL COMMENT '菜单图标',
  `path` varchar(200) DEFAULT NULL COMMENT '路由地址',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `permission` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `sort` int DEFAULT '0' COMMENT '显示顺序',
  `visible` tinyint DEFAULT '1' COMMENT '是否显示 0-隐藏 1-显示',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 1.5 角色菜单关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 1.6 部门表
CREATE TABLE IF NOT EXISTS `sys_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门ID',
  `dept_name` varchar(50) NOT NULL COMMENT '部门名称',
  `dept_code` varchar(50) DEFAULT NULL COMMENT '部门编码',
  `leader` varchar(50) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `sort` int DEFAULT '0' COMMENT '显示顺序',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 1.7 字典表
CREATE TABLE IF NOT EXISTS `sys_dict` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_type` varchar(50) NOT NULL COMMENT '字典类型',
  `dict_label` varchar(100) NOT NULL COMMENT '字典标签',
  `dict_value` varchar(100) NOT NULL COMMENT '字典值',
  `sort` int DEFAULT '0' COMMENT '显示顺序',
  `css_class` varchar(100) DEFAULT NULL COMMENT '样式属性',
  `list_class` varchar(100) DEFAULT NULL COMMENT '列表样式',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典表';

-- 1.8 系统配置表
CREATE TABLE IF NOT EXISTS `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_name` varchar(100) NOT NULL COMMENT '配置名称',
  `config_key` varchar(100) NOT NULL COMMENT '配置键名',
  `config_value` varchar(500) NOT NULL COMMENT '配置值',
  `config_type` tinyint DEFAULT 0 COMMENT '系统内置 1-是 0-否',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 1.9 操作日志表
CREATE TABLE IF NOT EXISTS `sys_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名',
  `operation` varchar(50) DEFAULT NULL COMMENT '操作类型',
  `method` varchar(200) DEFAULT NULL COMMENT '请求方法',
  `url` varchar(500) DEFAULT NULL COMMENT '请求地址',
  `ip` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `location` varchar(255) DEFAULT NULL COMMENT '操作地点',
  `params` text COMMENT '请求参数',
  `result` text COMMENT '返回结果',
  `status` tinyint DEFAULT '1' COMMENT '状态 1-成功 0-失败',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `execute_time` int DEFAULT NULL COMMENT '执行时长(毫秒)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 1.10 登录日志表
CREATE TABLE IF NOT EXISTS `sys_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名',
  `ip` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `location` varchar(255) DEFAULT NULL COMMENT '登录地点',
  `browser` varchar(100) DEFAULT NULL COMMENT '浏览器',
  `os` varchar(100) DEFAULT NULL COMMENT '操作系统',
  `status` tinyint DEFAULT '1' COMMENT '登录状态 1-成功 0-失败',
  `msg` varchar(500) DEFAULT NULL COMMENT '提示消息',
  `login_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_username` (`username`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- 1.11 文件表
CREATE TABLE IF NOT EXISTS `sys_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_path` varchar(500) NOT NULL COMMENT '文件存储路径',
  `file_url` varchar(500) DEFAULT NULL COMMENT '文件访问URL',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型',
  `mime_type` varchar(100) DEFAULT NULL COMMENT 'MIME类型',
  `md5` varchar(32) DEFAULT NULL COMMENT 'MD5校验值',
  `module` varchar(50) DEFAULT NULL COMMENT '所属模块',
  `business_id` bigint DEFAULT NULL COMMENT '业务ID',
  `create_by` bigint DEFAULT NULL COMMENT '上传者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_module_business` (`module`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- 1.12 系统通知公告表
CREATE TABLE IF NOT EXISTS `sys_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(200) NOT NULL COMMENT '公告标题',
  `content` text COMMENT '公告内容',
  `notice_type` tinyint DEFAULT '1' COMMENT '公告类型 1-系统通知 2-业务公告',
  `receiver_id` bigint DEFAULT NULL COMMENT '接收用户ID',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读 0-未读 1-已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知公告表';

-- 1.13 消息表
CREATE TABLE IF NOT EXISTS `sys_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sender_id` bigint DEFAULT NULL COMMENT '发送者ID',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID',
  `msg_type` tinyint DEFAULT '1' COMMENT '消息类型 1-系统消息 2-业务通知 3-互动消息',
  `title` varchar(200) DEFAULT NULL COMMENT '消息标题',
  `content` text COMMENT '消息内容',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读 0-未读 1-已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 1.14 短信记录表
CREATE TABLE IF NOT EXISTS `sys_sms_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `code` varchar(10) DEFAULT NULL COMMENT '验证码',
  `content` varchar(500) DEFAULT NULL COMMENT '短信内容',
  `sms_type` tinyint DEFAULT '1' COMMENT '短信类型 1-验证码 2-通知 3-营销',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-发送中 1-成功 2-失败',
  `send_result` varchar(500) DEFAULT NULL COMMENT '发送结果',
  `send_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_phone` (`phone`),
  KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信记录表';

-- ============================================
-- 2. 房屋管理表 (10张)
-- ============================================

-- 2.1 楼栋表
CREATE TABLE IF NOT EXISTS `base_building` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `building_no` varchar(20) NOT NULL COMMENT '楼栋编号',
  `building_name` varchar(50) NOT NULL COMMENT '楼栋名称',
  `unit_count` int DEFAULT '1' COMMENT '单元数',
  `floor_count` int DEFAULT '1' COMMENT '楼层数',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_building_no` (`building_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼栋表';

-- 2.2 单元表
CREATE TABLE IF NOT EXISTS `base_unit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `building_id` bigint NOT NULL COMMENT '楼栋ID',
  `unit_no` varchar(10) NOT NULL COMMENT '单元编号',
  `floor_count` int DEFAULT '1' COMMENT '楼层数',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_building_id` (`building_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单元表';

-- 2.3 房屋表
CREATE TABLE IF NOT EXISTS `base_house` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `building_id` bigint NOT NULL COMMENT '楼栋ID',
  `unit_id` bigint DEFAULT NULL COMMENT '单元ID',
  `house_no` varchar(20) NOT NULL COMMENT '房号',
  `floor` int DEFAULT NULL COMMENT '楼层',
  `house_type` varchar(20) DEFAULT NULL COMMENT '户型',
  `area` decimal(10,2) DEFAULT NULL COMMENT '建筑面积(平方米)',
  `inner_area` decimal(10,2) DEFAULT NULL COMMENT '套内面积(平方米)',
  `orientation` varchar(10) DEFAULT NULL COMMENT '朝向',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-空置 1-已售 2-已租 3-自住',
  `property_type` tinyint DEFAULT '1' COMMENT '产权类型 1-住宅 2-商铺 3-车位 4-仓库',
  `owner_id` bigint DEFAULT NULL COMMENT '业主ID',
  `delivery_date` date DEFAULT NULL COMMENT '交房日期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_building_unit_house` (`building_id`,`unit_id`,`house_no`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房屋表';

-- 2.4 业主表
CREATE TABLE IF NOT EXISTS `base_owner` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint DEFAULT NULL COMMENT '关联用户ID',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号',
  `gender` tinyint DEFAULT '0' COMMENT '性别',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `nation` varchar(20) DEFAULT NULL COMMENT '民族',
  `native_place` varchar(100) DEFAULT NULL COMMENT '籍贯',
  `occupation` varchar(50) DEFAULT NULL COMMENT '职业',
  `work_unit` varchar(100) DEFAULT NULL COMMENT '工作单位',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `emergency_contact` varchar(50) DEFAULT NULL COMMENT '紧急联系人',
  `emergency_phone` varchar(20) DEFAULT NULL COMMENT '紧急联系电话',
  `check_in_date` date DEFAULT NULL COMMENT '入住日期',
  `check_out_date` date DEFAULT NULL COMMENT '迁出日期',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-迁出 1-在住',
  `tags` varchar(200) DEFAULT NULL COMMENT '标签 JSON格式',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_id_card` (`id_card`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业主表';

-- 2.5 业主房屋关联表
CREATE TABLE IF NOT EXISTS `base_owner_house` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `owner_id` bigint NOT NULL COMMENT '业主ID',
  `house_id` bigint NOT NULL COMMENT '房屋ID',
  `relation_type` tinyint DEFAULT '1' COMMENT '关系类型 1-产权人 2-家属 3-租户',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_house_id` (`house_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业主房屋关联表';

-- 2.6 家属表
CREATE TABLE IF NOT EXISTS `base_family` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `owner_id` bigint NOT NULL COMMENT '业主ID',
  `real_name` varchar(50) NOT NULL COMMENT '姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号',
  `relation` varchar(20) DEFAULT NULL COMMENT '与业主关系',
  `gender` tinyint DEFAULT '0' COMMENT '性别',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-已移除 1-有效',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家属表';

-- 2.7 租户表
CREATE TABLE IF NOT EXISTS `base_tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `owner_id` bigint DEFAULT NULL COMMENT '关联业主ID',
  `house_id` bigint NOT NULL COMMENT '房屋ID',
  `real_name` varchar(50) NOT NULL COMMENT '姓名',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号',
  `gender` tinyint DEFAULT '0' COMMENT '性别',
  `lease_start_date` date DEFAULT NULL COMMENT '租期开始日期',
  `lease_end_date` date DEFAULT NULL COMMENT '租期结束日期',
  `monthly_rent` decimal(10,2) DEFAULT NULL COMMENT '月租金',
  `deposit` decimal(10,2) DEFAULT NULL COMMENT '押金',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-已退租 1-租赁中',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_house_id` (`house_id`),
  KEY `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

-- 2.8 车位表
CREATE TABLE IF NOT EXISTS `base_parking` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parking_no` varchar(20) NOT NULL COMMENT '车位编号',
  `parking_type` tinyint DEFAULT '1' COMMENT '车位类型 1-地上 2-地下',
  `area` varchar(50) DEFAULT NULL COMMENT '区域',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-空闲 1-已售 2-已租',
  `price` decimal(10,2) DEFAULT NULL COMMENT '售价',
  `monthly_rent` decimal(10,2) DEFAULT NULL COMMENT '月租金',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_parking_no` (`parking_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车位表';

-- 2.9 车辆表
CREATE TABLE IF NOT EXISTS `base_vehicle` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `owner_id` bigint NOT NULL COMMENT '业主ID',
  `parking_id` bigint DEFAULT NULL COMMENT '绑定车位ID',
  `plate_no` varchar(20) NOT NULL COMMENT '车牌号',
  `vehicle_type` varchar(20) DEFAULT NULL COMMENT '车辆类型',
  `brand` varchar(50) DEFAULT NULL COMMENT '品牌型号',
  `color` varchar(20) DEFAULT NULL COMMENT '颜色',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-禁用 1-正常',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plate_no` (`plate_no`),
  KEY `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆表';

-- 2.10 装修表
CREATE TABLE IF NOT EXISTS `base_decoration` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `owner_id` bigint NOT NULL COMMENT '业主ID',
  `house_id` bigint NOT NULL COMMENT '房屋ID',
  `decoration_no` varchar(32) NOT NULL COMMENT '装修单号',
  `company_name` varchar(100) DEFAULT NULL COMMENT '装修公司名称',
  `company_phone` varchar(20) DEFAULT NULL COMMENT '装修公司电话',
  `start_date` date DEFAULT NULL COMMENT '开工日期',
  `expect_end_date` date DEFAULT NULL COMMENT '预计完工日期',
  `actual_end_date` date DEFAULT NULL COMMENT '实际完工日期',
  `deposit_amount` decimal(10,2) DEFAULT NULL COMMENT '押金金额',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-待审批 1-已通过 2-施工中 3-已验收 4-已驳回 5-违规',
  `violation_record` varchar(500) DEFAULT NULL COMMENT '违规记录',
  `check_result` varchar(500) DEFAULT NULL COMMENT '验收结果',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_decoration_no` (`decoration_no`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_house_id` (`house_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='装修表';

-- ============================================
-- 3. 物业服务表 (8张)
-- ============================================

-- 3.1 报修表
CREATE TABLE IF NOT EXISTS `prop_repair` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `repair_no` varchar(32) NOT NULL COMMENT '报修单号',
  `owner_id` bigint NOT NULL COMMENT '业主ID',
  `house_id` bigint NOT NULL COMMENT '房屋ID',
  `repair_type` tinyint NOT NULL COMMENT '报修类型 1-水电 2-门窗 3-墙面 4-地面 5-公共设施 6-其他',
  `urgency` tinyint DEFAULT '1' COMMENT '紧急程度 1-一般 2-紧急 3-特急',
  `title` varchar(100) NOT NULL COMMENT '报修标题',
  `content` text COMMENT '报修内容',
  `images` varchar(1000) DEFAULT NULL COMMENT '图片URL列表 JSON',
  `contact_name` varchar(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `appointment_date` date DEFAULT NULL COMMENT '预约上门日期',
  `appointment_time` varchar(20) DEFAULT NULL COMMENT '预约时间段',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-待处理 1-已派单 2-处理中 3-已完成 4-已取消',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handler_name` varchar(50) DEFAULT NULL COMMENT '处理人姓名',
  `handle_result` text COMMENT '处理结果',
  `handle_images` varchar(1000) DEFAULT NULL COMMENT '处理图片',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `evaluate_score` tinyint DEFAULT NULL COMMENT '评价分数 1-5',
  `evaluate_content` varchar(500) DEFAULT NULL COMMENT '评价内容',
  `evaluate_time` datetime DEFAULT NULL COMMENT '评价时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_repair_no` (`repair_no`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_house_id` (`house_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修表';

-- 3.2 报修项目表
CREATE TABLE IF NOT EXISTS `prop_repair_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `repair_id` bigint NOT NULL COMMENT '报修ID',
  `item_name` varchar(100) NOT NULL COMMENT '维修项目名称',
  `material_cost` decimal(10,2) DEFAULT '0.00' COMMENT '材料费',
  `labor_cost` decimal(10,2) DEFAULT '0.00' COMMENT '人工费',
  `quantity` int DEFAULT '1' COMMENT '数量',
  `total_cost` decimal(10,2) DEFAULT '0.00' COMMENT '小计',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_repair_id` (`repair_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修项目表';

-- 3.3 投诉建议表
CREATE TABLE IF NOT EXISTS `prop_complaint` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `complaint_no` varchar(32) NOT NULL COMMENT '投诉单号',
  `owner_id` bigint NOT NULL COMMENT '业主ID',
  `house_id` bigint DEFAULT NULL COMMENT '房屋ID',
  `complaint_type` tinyint NOT NULL COMMENT '投诉类型 1-服务态度 2-环境卫生 3-设施设备 4-其他',
  `title` varchar(100) NOT NULL COMMENT '投诉标题',
  `content` text COMMENT '投诉内容',
  `images` varchar(1000) DEFAULT NULL COMMENT '图片URL列表',
  `contact_name` varchar(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-待处理 1-处理中 2-已完成 3-已关闭',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handler_name` varchar(50) DEFAULT NULL COMMENT '处理人姓名',
  `handle_result` text COMMENT '处理结果',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `reply_content` varchar(500) DEFAULT NULL COMMENT '回复内容',
  `reply_time` datetime DEFAULT NULL COMMENT '回复时间',
  `evaluate_score` tinyint DEFAULT NULL COMMENT '评价分数 1-5',
  `evaluate_time` datetime DEFAULT NULL COMMENT '评价时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_complaint_no` (`complaint_no`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投诉建议表';

-- 3.4 物业公告表
CREATE TABLE IF NOT EXISTS `prop_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `notice_no` varchar(32) NOT NULL COMMENT '公告编号',
  `title` varchar(200) NOT NULL COMMENT '公告标题',
  `content` text COMMENT '公告内容',
  `notice_type` tinyint DEFAULT '1' COMMENT '公告类型 1-通知 2-活动 3-紧急',
  `cover_image` varchar(255) DEFAULT NULL COMMENT '封面图片',
  `is_top` tinyint DEFAULT '0' COMMENT '是否置顶 0-否 1-是',
  `is_publish` tinyint DEFAULT '0' COMMENT '是否发布 0-草稿 1-已发布',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `publisher` varchar(50) DEFAULT NULL COMMENT '发布人',
  `view_count` int DEFAULT '0' COMMENT '阅读量',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-下架 1-上架',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_no` (`notice_no`),
  KEY `idx_is_publish` (`is_publish`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物业公告表';

-- 3.5 巡检表
CREATE TABLE IF NOT EXISTS `prop_inspection` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plan_id` bigint DEFAULT NULL COMMENT '巡检计划ID',
  `inspector_id` bigint NOT NULL COMMENT '巡检人员ID',
  `inspector_name` varchar(50) DEFAULT NULL COMMENT '巡检人员姓名',
  `inspection_date` date NOT NULL COMMENT '巡检日期',
  `area` varchar(100) DEFAULT NULL COMMENT '巡检区域',
  `items` text COMMENT '巡检项目JSON',
  `result` text COMMENT '巡检结果',
  `anomalies` text COMMENT '异常情况',
  `images` varchar(1000) DEFAULT NULL COMMENT '图片URL',
  `gps_lat` decimal(10,6) DEFAULT NULL COMMENT 'GPS纬度',
  `gps_lng` decimal(10,6) DEFAULT NULL COMMENT 'GPS经度',
  `check_time` datetime DEFAULT NULL COMMENT '打卡时间',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-未开始 1-进行中 2-已完成',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_inspector_id` (`inspector_id`),
  KEY `idx_inspection_date` (`inspection_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检表';

-- 3.6 保洁表
CREATE TABLE IF NOT EXISTS `prop_cleaning` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `cleaner_id` bigint NOT NULL COMMENT '保洁人员ID',
  `cleaner_name` varchar(50) DEFAULT NULL COMMENT '保洁人员姓名',
  `area` varchar(100) NOT NULL COMMENT '保洁区域',
  `task_date` date NOT NULL COMMENT '任务日期',
  `task_type` tinyint DEFAULT '1' COMMENT '任务类型 1-日常 2-专项 3-临时',
  `task_content` text COMMENT '任务内容',
  `images` varchar(1000) DEFAULT NULL COMMENT '图片URL',
  `quality_score` tinyint DEFAULT NULL COMMENT '质量评分 1-5',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-待处理 1-处理中 2-已完成',
  `check_time` datetime DEFAULT NULL COMMENT '检查时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_cleaner_id` (`cleaner_id`),
  KEY `idx_task_date` (`task_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保洁表';

-- 3.7 设备表
CREATE TABLE IF NOT EXISTS `prop_equipment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `equipment_no` varchar(32) NOT NULL COMMENT '设备编号',
  `equipment_name` varchar(100) NOT NULL COMMENT '设备名称',
  `equipment_type` tinyint NOT NULL COMMENT '设备类型 1-电梯 2-水泵 3-消防 4-监控 5-门禁 6-其他',
  `brand` varchar(100) DEFAULT NULL COMMENT '品牌',
  `model` varchar(100) DEFAULT NULL COMMENT '型号',
  `location` varchar(200) DEFAULT NULL COMMENT '安装位置',
  `install_date` date DEFAULT NULL COMMENT '安装日期',
  `warranty_date` date DEFAULT NULL COMMENT '质保到期日期',
  `life_years` int DEFAULT NULL COMMENT '设计寿命(年)',
  `supplier` varchar(100) DEFAULT NULL COMMENT '供应商',
  `supplier_phone` varchar(20) DEFAULT NULL COMMENT '供应商电话',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-停用 1-正常 2-维修中 3-报废',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_equipment_no` (`equipment_no`),
  KEY `idx_equipment_type` (`equipment_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

-- 3.8 设备维保表
CREATE TABLE IF NOT EXISTS `prop_equipment_maintain` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `equipment_id` bigint NOT NULL COMMENT '设备ID',
  `maintain_type` tinyint DEFAULT '1' COMMENT '维保类型 1-日常 2-月度 3-季度 4-年度 5-紧急',
  `maintain_date` date NOT NULL COMMENT '维保日期',
  `maintainer_name` varchar(50) DEFAULT NULL COMMENT '维保人员',
  `maintain_content` text COMMENT '维保内容',
  `maintain_result` varchar(500) DEFAULT NULL COMMENT '维保结果',
  `cost` decimal(10,2) DEFAULT NULL COMMENT '费用',
  `next_maintain_date` date DEFAULT NULL COMMENT '下次维保日期',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-计划中 1-已完成 2-已延期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_equipment_id` (`equipment_id`),
  KEY `idx_maintain_date` (`maintain_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备维保表';

-- ============================================
-- 4. 费用管理表 (5张)
-- ============================================

-- 4.1 费用项目表
CREATE TABLE IF NOT EXISTS `fee_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `item_name` varchar(50) NOT NULL COMMENT '费用项目名称',
  `item_code` varchar(50) NOT NULL COMMENT '费用项目编码',
  `item_type` tinyint NOT NULL COMMENT '费用类型 1-物业费 2-停车费 3-水电费 4-其他',
  `unit_price` decimal(10,4) DEFAULT NULL COMMENT '单价',
  `unit` varchar(20) DEFAULT NULL COMMENT '计量单位',
  `calc_type` tinyint DEFAULT '1' COMMENT '计算方式 1-按面积 2-按套 3-固定金额',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-停用 1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_code` (`item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用项目表';

-- 4.2 账单表
CREATE TABLE IF NOT EXISTS `fee_bill` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bill_no` varchar(32) NOT NULL COMMENT '账单号',
  `owner_id` bigint NOT NULL COMMENT '业主ID',
  `house_id` bigint NOT NULL COMMENT '房屋ID',
  `item_id` bigint NOT NULL COMMENT '费用项目ID',
  `item_name` varchar(50) DEFAULT NULL COMMENT '费用项目名称',
  `bill_type` tinyint NOT NULL COMMENT '账单类型 1-物业费 2-停车费 3-水电费 4-其他',
  `bill_period` varchar(20) NOT NULL COMMENT '账期',
  `bill_start_date` date NOT NULL COMMENT '计费开始日期',
  `bill_end_date` date NOT NULL COMMENT '计费结束日期',
  `amount` decimal(12,2) NOT NULL COMMENT '账单金额',
  `discount_amount` decimal(12,2) DEFAULT '0.00' COMMENT '优惠金额',
  `late_fee` decimal(12,2) DEFAULT '0.00' COMMENT '滞纳金',
  `payable_amount` decimal(12,2) NOT NULL COMMENT '应付金额',
  `paid_amount` decimal(12,2) DEFAULT '0.00' COMMENT '已付金额',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-未缴费 1-部分缴费 2-已缴清 3-已作废',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `pay_method` tinyint DEFAULT NULL COMMENT '支付方式 1-微信 2-支付宝 3-现金 4-银行转账',
  `pay_trade_no` varchar(64) DEFAULT NULL COMMENT '支付流水号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_no` (`bill_no`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_house_id` (`house_id`),
  KEY `idx_status` (`status`),
  KEY `idx_bill_period` (`bill_period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单表';

-- 4.3 缴费记录表
CREATE TABLE IF NOT EXISTS `fee_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `payment_no` varchar(32) NOT NULL COMMENT '缴费单号',
  `bill_id` bigint DEFAULT NULL COMMENT '账单ID',
  `owner_id` bigint NOT NULL COMMENT '业主ID',
  `house_id` bigint NOT NULL COMMENT '房屋ID',
  `pay_amount` decimal(12,2) NOT NULL COMMENT '缴费金额',
  `pay_method` tinyint NOT NULL COMMENT '支付方式 1-微信 2-支付宝 3-现金 4-银行转账',
  `pay_trade_no` varchar(64) DEFAULT NULL COMMENT '支付流水号',
  `pay_status` tinyint DEFAULT '1' COMMENT '支付状态 0-失败 1-成功 2-退款',
  `pay_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '支付时间',
  `invoice_url` varchar(500) DEFAULT NULL COMMENT '电子发票URL',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_pay_time` (`pay_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缴费记录表';

-- 4.4 押金表
CREATE TABLE IF NOT EXISTS `fee_deposit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `deposit_no` varchar(32) NOT NULL COMMENT '押金单号',
  `owner_id` bigint NOT NULL COMMENT '业主ID',
  `house_id` bigint DEFAULT NULL COMMENT '房屋ID',
  `deposit_type` tinyint NOT NULL COMMENT '押金类型 1-装修押金 2-车位押金 3-其他',
  `amount` decimal(10,2) NOT NULL COMMENT '押金金额',
  `refunded_amount` decimal(10,2) DEFAULT '0.00' COMMENT '已退金额',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-已收 1-部分退 2-已全退',
  `receive_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收款时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_deposit_no` (`deposit_no`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='押金表';

-- 4.5 退款表
CREATE TABLE IF NOT EXISTS `fee_refund` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `refund_no` varchar(32) NOT NULL COMMENT '退款单号',
  `owner_id` bigint NOT NULL COMMENT '业主ID',
  `deposit_id` bigint DEFAULT NULL COMMENT '关联押金ID',
  `bill_id` bigint DEFAULT NULL COMMENT '关联账单ID',
  `refund_type` tinyint NOT NULL COMMENT '退款类型 1-押金退款 2-费用退款 3-其他',
  `amount` decimal(10,2) NOT NULL COMMENT '退款金额',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-待审批 1-已通过 2-已退款 3-已驳回',
  `apply_reason` varchar(500) DEFAULT NULL COMMENT '申请原因',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '驳回原因',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款表';

-- ============================================
-- 5. 生活服务表 (9张)
-- ============================================

-- 5.1 商品表
CREATE TABLE IF NOT EXISTS `life_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_name` varchar(100) NOT NULL COMMENT '商品名称',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `product_image` varchar(500) DEFAULT NULL COMMENT '商品图片',
  `description` text COMMENT '商品描述',
  `price` decimal(10,2) NOT NULL COMMENT '售价',
  `origin_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `stock` int DEFAULT '0' COMMENT '库存',
  `sales` int DEFAULT '0' COMMENT '销量',
  `is_on_sale` tinyint DEFAULT '1' COMMENT '是否上架 0-下架 1-上架',
  `is_recommend` tinyint DEFAULT '0' COMMENT '是否推荐 0-否 1-是',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-停用 1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_is_on_sale` (`is_on_sale`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 5.2 订单表
CREATE TABLE IF NOT EXISTS `life_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `order_type` tinyint NOT NULL COMMENT '订单类型 1-商品 2-家政 3-租赁',
  `total_amount` decimal(12,2) NOT NULL COMMENT '订单总金额',
  `discount_amount` decimal(12,2) DEFAULT '0.00' COMMENT '优惠金额',
  `freight_amount` decimal(12,2) DEFAULT '0.00' COMMENT '运费',
  `payable_amount` decimal(12,2) NOT NULL COMMENT '应付金额',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-待付款 1-已付款 2-已发货 3-已完成 4-已取消 5-退款中 6-已退款',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `pay_method` tinyint DEFAULT NULL COMMENT '支付方式',
  `pay_trade_no` varchar(64) DEFAULT NULL COMMENT '支付流水号',
  `delivery_type` tinyint DEFAULT '1' COMMENT '配送方式 1-快递 2-自提 3-送货上门',
  `receiver_name` varchar(50) DEFAULT NULL COMMENT '收货人',
  `receiver_phone` varchar(20) DEFAULT NULL COMMENT '收货电话',
  `receiver_address` varchar(200) DEFAULT NULL COMMENT '收货地址',
  `delivery_time` datetime DEFAULT NULL COMMENT '发货时间',
  `receive_time` datetime DEFAULT NULL COMMENT '收货时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(200) DEFAULT NULL COMMENT '取消原因',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 5.3 订单明细表
CREATE TABLE IF NOT EXISTS `life_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `product_id` bigint DEFAULT NULL COMMENT '商品ID',
  `product_name` varchar(100) NOT NULL COMMENT '商品名称',
  `product_image` varchar(500) DEFAULT NULL COMMENT '商品图片',
  `price` decimal(10,2) NOT NULL COMMENT '单价',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '数量',
  `total_price` decimal(10,2) NOT NULL COMMENT '小计',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 5.4 家政服务表
CREATE TABLE IF NOT EXISTS `life_service` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `service_name` varchar(100) NOT NULL COMMENT '服务名称',
  `service_type` tinyint NOT NULL COMMENT '服务类型 1-保洁 2-保姆 3-月嫂 4-维修 5-搬家 6-其他',
  `service_image` varchar(500) DEFAULT NULL COMMENT '服务图片',
  `description` text COMMENT '服务描述',
  `price` decimal(10,2) NOT NULL COMMENT '服务价格',
  `unit` varchar(20) DEFAULT NULL COMMENT '计价单位',
  `is_on_sale` tinyint DEFAULT '1' COMMENT '是否上架 0-否 1-是',
  `staff_count` int DEFAULT '0' COMMENT '服务人员数',
  `sort` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_service_type` (`service_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家政服务表';

-- 5.5 家政订单表
CREATE TABLE IF NOT EXISTS `life_service_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `service_id` bigint NOT NULL COMMENT '服务项目ID',
  `service_name` varchar(100) DEFAULT NULL COMMENT '服务名称',
  `appointment_date` date NOT NULL COMMENT '预约日期',
  `appointment_time` varchar(20) DEFAULT NULL COMMENT '预约时间段',
  `contact_name` varchar(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `address` varchar(200) DEFAULT NULL COMMENT '服务地址',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-待接单 1-已接单 2-服务中 3-已完成 4-已取消 5-已评价',
  `staff_id` bigint DEFAULT NULL COMMENT '服务人员ID',
  `staff_name` varchar(50) DEFAULT NULL COMMENT '服务人员姓名',
  `evaluate_score` tinyint DEFAULT NULL COMMENT '评价分数 1-5',
  `evaluate_content` varchar(500) DEFAULT NULL COMMENT '评价内容',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_service_id` (`service_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家政订单表';

-- 5.6 租赁房源表
CREATE TABLE IF NOT EXISTS `life_rental` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rental_no` varchar(32) NOT NULL COMMENT '房源编号',
  `owner_id` bigint NOT NULL COMMENT '发布者ID',
  `house_id` bigint DEFAULT NULL COMMENT '关联房屋ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `description` text COMMENT '描述',
  `images` varchar(1000) DEFAULT NULL COMMENT '图片URL列表',
  `rental_type` tinyint NOT NULL COMMENT '租赁类型 1-整租 2-合租 3-短租',
  `monthly_rent` decimal(10,2) NOT NULL COMMENT '月租金',
  `deposit` decimal(10,2) DEFAULT NULL COMMENT '押金',
  `payment_method` varchar(100) DEFAULT NULL COMMENT '付款方式',
  `contact_name` varchar(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-待审核 1-已发布 2-已出租 3-已下架',
  `is_recommend` tinyint DEFAULT '0' COMMENT '是否推荐 0-否 1-是',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_no` (`rental_no`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁房源表';

-- 5.7 快递表
CREATE TABLE IF NOT EXISTS `life_express` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `express_no` varchar(32) NOT NULL COMMENT '快递单号',
  `receiver_id` bigint NOT NULL COMMENT '收件业主ID',
  `express_company` varchar(50) DEFAULT NULL COMMENT '快递公司',
  `tracking_no` varchar(100) DEFAULT NULL COMMENT '快递单号',
  `package_info` varchar(200) DEFAULT NULL COMMENT '包裹信息',
  `pickup_code` varchar(10) NOT NULL COMMENT '取件码',
  `arrive_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '到件时间',
  `pickup_time` datetime DEFAULT NULL COMMENT '取件时间',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-待取件 1-已取件 2-滞留 3-退回',
  `shelf_no` varchar(20) DEFAULT NULL COMMENT '货架编号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_express_no` (`express_no`),
  UNIQUE KEY `uk_pickup_code` (`pickup_code`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快递表';

-- 5.8 活动表
CREATE TABLE IF NOT EXISTS `life_activity` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(200) NOT NULL COMMENT '活动标题',
  `content` text COMMENT '活动内容',
  `images` varchar(1000) DEFAULT NULL COMMENT '图片URL列表',
  `activity_type` tinyint DEFAULT '1' COMMENT '活动类型 1-文体 2-公益 3-节日 4-讲座 5-其他',
  `location` varchar(200) DEFAULT NULL COMMENT '活动地点',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `sign_start_time` datetime DEFAULT NULL COMMENT '报名开始时间',
  `sign_end_time` datetime DEFAULT NULL COMMENT '报名截止时间',
  `max_participants` int DEFAULT NULL COMMENT '最大参与人数',
  `current_participants` int DEFAULT '0' COMMENT '当前报名人数',
  `contact_name` varchar(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-草稿 1-已发布 2-进行中 3-已结束 4-已取消',
  `is_top` tinyint DEFAULT '0' COMMENT '是否置顶 0-否 1-是',
  `view_count` int DEFAULT '0' COMMENT '浏览量',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

-- 5.9 活动报名表
CREATE TABLE IF NOT EXISTS `life_activity_join` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `user_id` bigint NOT NULL COMMENT '报名用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '用户姓名',
  `user_phone` varchar(20) DEFAULT NULL COMMENT '用户手机',
  `participant_count` int DEFAULT '1' COMMENT '参与人数',
  `sign_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  `sign_status` tinyint DEFAULT '1' COMMENT '签到状态 0-未签到 1-已签到',
  `sign_time_check` datetime DEFAULT NULL COMMENT '签到时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_user_id` (`user_id`),
  UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动报名表';

-- ============================================
-- 6. 安防管理表 (5张)
-- ============================================

-- 6.1 门禁设备表
CREATE TABLE IF NOT EXISTS `guard_access` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `access_no` varchar(32) NOT NULL COMMENT '门禁编号',
  `access_name` varchar(100) NOT NULL COMMENT '门禁名称',
  `access_type` tinyint DEFAULT '1' COMMENT '门禁类型 1-大门 2-单元门 3-电梯 4-车库',
  `location` varchar(200) DEFAULT NULL COMMENT '安装位置',
  `device_sn` varchar(50) DEFAULT NULL COMMENT '设备序列号',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-离线 1-在线 2-故障',
  `install_date` date DEFAULT NULL COMMENT '安装日期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_access_no` (`access_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门禁设备表';

-- 6.2 门禁记录表
CREATE TABLE IF NOT EXISTS `guard_access_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `access_id` bigint NOT NULL COMMENT '门禁设备ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `user_type` tinyint DEFAULT NULL COMMENT '用户类型 1-业主 2-家属 3-租户 4-访客',
  `access_type` tinyint NOT NULL COMMENT '通行类型 1-人脸识别 2-刷卡 3-密码 4-二维码 5-远程开门',
  `access_direction` tinyint NOT NULL COMMENT '进出方向 1-进 2-出',
  `access_time` datetime NOT NULL COMMENT '通行时间',
  `access_image` varchar(255) DEFAULT NULL COMMENT '抓拍图片',
  `access_result` tinyint DEFAULT '1' COMMENT '通行结果 1-成功 2-失败',
  `fail_reason` varchar(200) DEFAULT NULL COMMENT '失败原因',
  `device_sn` varchar(50) DEFAULT NULL COMMENT '设备序列号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_access_id` (`access_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_access_time` (`access_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门禁记录表';

-- 6.3 访客表
CREATE TABLE IF NOT EXISTS `guard_visitor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `visitor_no` varchar(32) NOT NULL COMMENT '访客单号',
  `owner_id` bigint NOT NULL COMMENT '邀请业主ID',
  `visitor_name` varchar(50) NOT NULL COMMENT '访客姓名',
  `visitor_phone` varchar(20) NOT NULL COMMENT '访客手机',
  `visitor_id_card` varchar(18) DEFAULT NULL COMMENT '访客身份证',
  `visitor_count` int DEFAULT '1' COMMENT '访客人数',
  `plate_no` varchar(20) DEFAULT NULL COMMENT '访客车牌',
  `visit_reason` varchar(200) DEFAULT NULL COMMENT '来访事由',
  `visit_start_time` datetime NOT NULL COMMENT '来访开始时间',
  `visit_end_time` datetime NOT NULL COMMENT '来访结束时间',
  `qrcode` varchar(500) DEFAULT NULL COMMENT '通行二维码',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-待审核 1-已通过 2-已拒绝 3-已签到 4-已签退 5-已过期',
  `approve_time` datetime DEFAULT NULL COMMENT '审核时间',
  `checkin_time` datetime DEFAULT NULL COMMENT '签到时间',
  `checkout_time` datetime DEFAULT NULL COMMENT '签退时间',
  `is_blacklist` tinyint DEFAULT '0' COMMENT '是否黑名单 0-否 1-是',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_visitor_no` (`visitor_no`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_status` (`status`),
  KEY `idx_visitor_phone` (`visitor_phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客表';

-- 6.4 监控设备表
CREATE TABLE IF NOT EXISTS `guard_camera` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `camera_no` varchar(32) NOT NULL COMMENT '摄像头编号',
  `camera_name` varchar(100) NOT NULL COMMENT '摄像头名称',
  `camera_type` tinyint DEFAULT '1' COMMENT '类型 1-枪机 2-球机 3-全景',
  `location` varchar(200) DEFAULT NULL COMMENT '安装位置',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `port` int DEFAULT NULL COMMENT '端口',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名',
  `password` varchar(100) DEFAULT NULL COMMENT '密码',
  `stream_url` varchar(500) DEFAULT NULL COMMENT '视频流地址',
  `resolution` varchar(20) DEFAULT NULL COMMENT '分辨率',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-离线 1-在线 2-故障',
  `install_date` date DEFAULT NULL COMMENT '安装日期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_camera_no` (`camera_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监控设备表';

-- 6.5 报警表
CREATE TABLE IF NOT EXISTS `guard_alarm` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `alarm_no` varchar(32) NOT NULL COMMENT '报警单号',
  `alarm_type` tinyint NOT NULL COMMENT '报警类型 1-消防 2-燃气 3-入侵 4-故障 5-紧急 6-其他',
  `device_id` bigint DEFAULT NULL COMMENT '触发设备ID',
  `device_name` varchar(100) DEFAULT NULL COMMENT '触发设备名称',
  `location` varchar(200) DEFAULT NULL COMMENT '报警位置',
  `alarm_level` tinyint DEFAULT '1' COMMENT '报警级别 1-一般 2-重要 3-紧急',
  `alarm_time` datetime NOT NULL COMMENT '报警时间',
  `description` varchar(500) DEFAULT NULL COMMENT '报警描述',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handler_name` varchar(50) DEFAULT NULL COMMENT '处理人姓名',
  `handle_result` text COMMENT '处理结果',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-待处理 1-处理中 2-已处理 3-误报',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alarm_no` (`alarm_no`),
  KEY `idx_alarm_type` (`alarm_type`),
  KEY `idx_status` (`status`),
  KEY `idx_alarm_time` (`alarm_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警表';

-- ============================================
-- 7. 社区互动表 (6张)
-- ============================================

-- 7.1 帖子表
CREATE TABLE IF NOT EXISTS `social_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_no` varchar(32) NOT NULL COMMENT '帖子编号',
  `user_id` bigint NOT NULL COMMENT '发布用户ID',
  `title` varchar(200) NOT NULL COMMENT '帖子标题',
  `content` text NOT NULL COMMENT '帖子内容',
  `images` varchar(1000) DEFAULT NULL COMMENT '图片URL列表',
  `category` tinyint DEFAULT '1' COMMENT '分类 1-求助 2-分享 3-交易 4-活动 5-吐槽 6-其他',
  `is_top` tinyint DEFAULT '0' COMMENT '是否置顶 0-否 1-是',
  `is_essence` tinyint DEFAULT '0' COMMENT '是否精华 0-否 1-是',
  `view_count` int DEFAULT '0' COMMENT '浏览量',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `comment_count` int DEFAULT '0' COMMENT '评论数',
  `favorite_count` int DEFAULT '0' COMMENT '收藏数',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-隐藏 1-显示 2-违规',
  `tags` varchar(200) DEFAULT NULL COMMENT '标签',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_no` (`post_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_category` (`category`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子表';

-- 7.2 评论表
CREATE TABLE IF NOT EXISTS `social_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `content` text NOT NULL COMMENT '评论内容',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-隐藏 1-显示',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 7.3 二手物品表
CREATE TABLE IF NOT EXISTS `social_secondhand` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `item_no` varchar(32) NOT NULL COMMENT '物品编号',
  `user_id` bigint NOT NULL COMMENT '发布用户ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `description` text COMMENT '描述',
  `images` varchar(1000) DEFAULT NULL COMMENT '图片URL列表',
  `category` tinyint DEFAULT '1' COMMENT '分类 1-家具 2-家电 3-数码 4-母婴 5-图书 6-其他',
  `price` decimal(10,2) NOT NULL COMMENT '价格',
  `origin_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `condition_level` tinyint DEFAULT '1' COMMENT '成色 1-全新 2-九成新 3-八成新 4-五成新',
  `contact_name` varchar(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `trade_method` tinyint DEFAULT '1' COMMENT '交易方式 1-自提 2-快递',
  `view_count` int DEFAULT '0' COMMENT '浏览量',
  `favorite_count` int DEFAULT '0' COMMENT '收藏数',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-在售 1-已售 2-已下架',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_no` (`item_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二手物品表';

-- 7.4 投票表
CREATE TABLE IF NOT EXISTS `social_vote` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `vote_no` varchar(32) NOT NULL COMMENT '投票编号',
  `user_id` bigint NOT NULL COMMENT '发起用户ID',
  `title` varchar(200) NOT NULL COMMENT '投票标题',
  `description` text COMMENT '投票描述',
  `vote_type` tinyint DEFAULT '1' COMMENT '投票类型 1-单选 2-多选',
  `is_anonymous` tinyint DEFAULT '0' COMMENT '是否匿名 0-否 1-是',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `total_votes` int DEFAULT '0' COMMENT '总投票数',
  `status` tinyint DEFAULT '0' COMMENT '状态 0-未开始 1-进行中 2-已结束',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vote_no` (`vote_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投票表';

-- 7.5 投票选项表
CREATE TABLE IF NOT EXISTS `social_vote_option` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `vote_id` bigint NOT NULL COMMENT '投票ID',
  `option_text` varchar(200) NOT NULL COMMENT '选项内容',
  `vote_count` int DEFAULT '0' COMMENT '得票数',
  `sort` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_vote_id` (`vote_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投票选项表';

-- 7.5.1 投票记录表
CREATE TABLE IF NOT EXISTS `social_vote_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `vote_id` bigint NOT NULL COMMENT '投票ID',
  `option_id` bigint NOT NULL COMMENT '选项ID',
  `user_id` bigint NOT NULL COMMENT '投票用户ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '投票用户名',
  `vote_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_vote_id` (`vote_id`),
  KEY `idx_option_id` (`option_id`),
  KEY `idx_user_id` (`user_id`),
  UNIQUE KEY `uk_vote_user` (`vote_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投票记录表';

-- 7.6 便民电话表
CREATE TABLE IF NOT EXISTS `social_phone` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category` varchar(50) NOT NULL COMMENT '分类 如医疗、教育、生活',
  `name` varchar(100) NOT NULL COMMENT '名称',
  `phone` varchar(20) NOT NULL COMMENT '电话号码',
  `address` varchar(200) DEFAULT NULL COMMENT '地址',
  `work_time` varchar(100) DEFAULT NULL COMMENT '工作时间',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `is_recommend` tinyint DEFAULT '0' COMMENT '是否推荐 0-否 1-是',
  `favorite_count` int DEFAULT '0' COMMENT '收藏次数',
  `status` tinyint DEFAULT '1' COMMENT '状态 0-停用 1-启用',
  `sort` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='便民电话表';
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

