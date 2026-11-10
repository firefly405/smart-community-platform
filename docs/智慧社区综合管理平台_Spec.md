# 智慧社区综合管理平台 - 技术规格文档（Spec）

**版本**: v1.0  
**日期**: 2026年5月  
**技术栈**: Vue 3 + SpringBoot 3.x + MySQL 8.0 + Redis  

---

## 目录

1. [文档概述](#1-文档概述)
2. [系统架构设计](#2-系统架构设计)
3. [技术选型说明](#3-技术选型说明)
4. [数据库设计](#4-数据库设计)
5. [接口设计规范](#5-接口设计规范)
6. [模块详细设计](#6-模块详细设计)
7. [安全设计](#7-安全设计)
8. [部署架构](#8-部署架构)
9. [附录](#9-附录)

---

## 1. 文档概述

### 1.1 文档目的
本文档详细描述智慧社区综合管理平台的技术实现方案，包括系统架构、数据库设计、接口规范、安全设计等内容，为开发团队提供技术实现指导。

### 1.2 适用范围
- 后端开发工程师
- 前端开发工程师
- 数据库管理员
- 运维工程师
- 测试工程师

### 1.3 参考文档
- 《智慧社区综合管理平台_PRD.md》
- 《阿里巴巴Java开发手册》
- 《RESTful API设计规范》

---

## 2. 系统架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              接入层                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │
│  │   Web管理端   │  │   业主APP    │  │  微信小程序  │  │  数据大屏  │  │
│  │   (Vue3)     │  │   (uni-app)  │  │   (原生)     │  │  (Vue3)    │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └─────┬──────┘  │
└─────────┼─────────────────┼─────────────────┼────────────────┼─────────┘
          │                 │                 │                │
          └─────────────────┴────────┬────────┴────────────────┘
                                     │
┌────────────────────────────────────┼────────────────────────────────────┐
│                              网关层                                  │
│              ┌─────────────────────┴─────────────────────┐            │
│              │           Nginx (负载均衡/静态资源)        │            │
│              └─────────────────────┬─────────────────────┘            │
│                                    │                                   │
│              ┌─────────────────────┴─────────────────────┐            │
│              │      Spring Cloud Gateway (API网关)       │            │
│              │  - 路由转发  - 限流熔断  - 认证鉴权        │            │
│              └─────────────────────┬─────────────────────┘            │
└────────────────────────────────────┼────────────────────────────────────┘
                                     │
┌────────────────────────────────────┼────────────────────────────────────┐
│                              业务服务层                                │
│  ┌─────────────────────────────────┼─────────────────────────────────┐ │
│  │                      SpringBoot微服务集群                         │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐            │ │
│  │  │用户服务  │ │房屋服务  │ │物业服务  │ │费用服务  │            │ │
│  │  │ user-svc │ │house-svc │ │ prop-svc │ │ fee-svc  │            │ │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘            │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐            │ │
│  │  │生活服务  │ │安防服务  │ │互动服务  │ │报表服务  │            │ │
│  │  │life-svc  │ │guard-svc │ │social-svc│ │report-svc│            │ │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘            │ │
│  └─────────────────────────────────┼─────────────────────────────────┘ │
│                                    │                                   │
│  ┌─────────────────────────────────┼─────────────────────────────────┐ │
│  │                      公共服务组件                                 │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐            │ │
│  │  │注册中心  │ │配置中心  │ │消息队列  │ │任务调度  │            │ │
│  │  │  Nacos   │ │  Nacos   │ │ RabbitMQ │ │XXL-Job   │            │ │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘            │ │
│  └──────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────┘
                                     │
┌────────────────────────────────────┼────────────────────────────────────┐
│                              数据存储层                                │
│  ┌─────────────────────────────────┼─────────────────────────────────┐ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐            │ │
│  │  │  MySQL   │ │  Redis   │ │  MinIO   │ │Elasticsearch│          │ │
│  │  │ 主从集群 │ │  集群    │ │对象存储  │ │ 搜索引擎  │            │ │
│  │  │ (主库+2从)│ │ (哨兵模式)│ │          │ │           │            │ │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘            │ │
│  └──────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────┘
```

### 2.2 服务拆分

| 服务名 | 服务标识 | 职责 | 端口 |
|--------|----------|------|------|
| 用户服务 | user-service | 用户注册登录、权限管理、组织架构 | 8001 |
| 房屋服务 | house-service | 房屋、业主、车位管理 | 8002 |
| 物业服务 | property-service | 报修、投诉、公告、巡检 | 8003 |
| 费用服务 | fee-service | 费用计算、账单、支付 | 8004 |
| 生活服务 | life-service | 商城、家政、租赁、快递 | 8005 |
| 安防服务 | security-service | 门禁、访客、监控、预警 | 8006 |
| 互动服务 | social-service | 论坛、二手交易、投票 | 8007 |
| 报表服务 | report-service | 数据统计、分析报表 | 8008 |
| 文件服务 | file-service | 文件上传下载、图片处理 | 8009 |
| 消息服务 | message-service | 短信、推送、站内信 | 8010 |

---

## 3. 技术选型说明

### 3.1 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| SpringBoot | 3.2.x | 基础框架 |
| SpringCloud | 2023.x | 微服务框架 |
| SpringSecurity | 6.x | 安全认证 |
| MyBatis-Plus | 3.5.x | ORM框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7.x | 缓存/会话 |
| RabbitMQ | 3.12 | 消息队列 |
| Elasticsearch | 8.x | 搜索引擎 |
| MinIO | 最新版 | 对象存储 |
| XXL-Job | 2.4.x | 分布式任务调度 |
| Nacos | 2.2.x | 注册中心/配置中心 |
| Sentinel | 1.8.x | 流量控制 |
| Seata | 1.7.x | 分布式事务 |
| Knife4j | 4.x | API文档 |
| Maven | 3.9.x | 构建工具 |

### 3.2 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.x | 前端框架 |
| TypeScript | 5.x | 类型语言 |
| Vite | 5.x | 构建工具 |
| Element Plus | 2.5.x | UI组件库 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由管理 |
| Axios | 1.6.x | HTTP请求 |
| ECharts | 5.x | 图表库 |
| uni-app | 3.x | 跨端开发 |

### 3.3 运维技术栈

| 技术 | 用途 |
|------|------|
| Docker | 容器化部署 |
| Kubernetes | 容器编排 |
| Jenkins | CI/CD |
| Prometheus | 监控采集 |
| Grafana | 监控可视化 |
| ELK Stack | 日志收集分析 |
| Nginx | 反向代理/负载均衡 |

---

## 4. 数据库设计

### 4.1 数据库规划

**主数据库**: `smart_community` (MySQL 8.0)

| 序号 | 数据库表名 | 中文名 | 所属模块 | 数据量预估 |
|------|-----------|--------|----------|------------|
| 1 | sys_user | 用户表 | 用户管理 | 10万+ |
| 2 | sys_role | 角色表 | 用户管理 | 50+ |
| 3 | sys_user_role | 用户角色关联表 | 用户管理 | 10万+ |
| 4 | sys_menu | 菜单表 | 用户管理 | 200+ |
| 5 | sys_role_menu | 角色菜单关联表 | 用户管理 | 1000+ |
| 6 | sys_dept | 部门表 | 用户管理 | 100+ |
| 7 | sys_dict | 字典表 | 系统管理 | 500+ |
| 8 | sys_config | 配置表 | 系统管理 | 100+ |
| 9 | sys_log | 操作日志表 | 系统管理 | 1000万+ |
| 10 | sys_login_log | 登录日志表 | 系统管理 | 100万+ |
| 11 | sys_file | 文件表 | 文件管理 | 10万+ |
| 12 | sys_notice | 通知公告表 | 系统管理 | 1000+ |
| 13 | sys_message | 消息表 | 消息管理 | 100万+ |
| 14 | sys_sms_log | 短信记录表 | 消息管理 | 100万+ |
| 15 | base_building | 楼栋表 | 房屋管理 | 100+ |
| 16 | base_unit | 单元表 | 房屋管理 | 500+ |
| 17 | base_house | 房屋表 | 房屋管理 | 1万+ |
| 18 | base_owner | 业主表 | 房屋管理 | 1万+ |
| 19 | base_owner_house | 业主房屋关联表 | 房屋管理 | 1.2万+ |
| 20 | base_family | 家属表 | 房屋管理 | 2万+ |
| 21 | base_tenant | 租户表 | 房屋管理 | 5000+ |
| 22 | base_parking | 车位表 | 车位管理 | 5000+ |
| 23 | base_vehicle | 车辆表 | 车位管理 | 8000+ |
| 24 | base_decoration | 装修表 | 房屋管理 | 2000+ |
| 25 | prop_repair | 报修表 | 物业服务 | 5万+ |
| 26 | prop_repair_item | 报修项目表 | 物业服务 | 10万+ |
| 27 | prop_complaint | 投诉表 | 物业服务 | 1万+ |
| 28 | prop_notice | 物业公告表 | 物业服务 | 5000+ |
| 29 | prop_inspection | 巡检表 | 物业服务 | 10万+ |
| 30 | prop_cleaning | 保洁表 | 物业服务 | 5万+ |
| 31 | prop_equipment | 设备表 | 物业服务 | 1000+ |
| 32 | prop_equipment_maintain | 设备维保表 | 物业服务 | 1万+ |
| 33 | fee_item | 费用项目表 | 费用管理 | 20+ |
| 34 | fee_bill | 账单表 | 费用管理 | 50万+ |
| 35 | fee_payment | 缴费记录表 | 费用管理 | 40万+ |
| 36 | fee_deposit | 押金表 | 费用管理 | 2万+ |
| 37 | fee_refund | 退款表 | 费用管理 | 5000+ |
| 38 | life_product | 商品表 | 生活服务 | 5000+ |
| 39 | life_order | 订单表 | 生活服务 | 10万+ |
| 40 | life_order_item | 订单明细表 | 生活服务 | 30万+ |
| 41 | life_service | 家政服务表 | 生活服务 | 1000+ |
| 42 | life_service_order | 家政订单表 | 生活服务 | 5000+ |
| 43 | life_rental | 租赁房源表 | 生活服务 | 2000+ |
| 44 | life_express | 快递表 | 生活服务 | 20万+ |
| 45 | life_activity | 活动表 | 生活服务 | 500+ |
| 46 | life_activity_join | 活动报名表 | 生活服务 | 1万+ |
| 47 | guard_access | 门禁表 | 安防管理 | 100+ |
| 48 | guard_access_log | 门禁记录表 | 安防管理 | 1000万+ |
| 49 | guard_visitor | 访客表 | 安防管理 | 10万+ |
| 50 | guard_camera | 监控表 | 安防管理 | 500+ |
| 51 | guard_alarm | 报警表 | 安防管理 | 1万+ |
| 52 | social_post | 帖子表 | 社区互动 | 10万+ |
| 53 | social_comment | 评论表 | 社区互动 | 50万+ |
| 54 | social_secondhand | 二手物品表 | 社区互动 | 5万+ |
| 55 | social_vote | 投票表 | 社区互动 | 1000+ |
| 56 | social_vote_option | 投票选项表 | 社区互动 | 5000+ |
| 57 | social_phone | 便民电话表 | 社区互动 | 500+ |

**总计: 57张表** (满足≥15张要求)

### 4.2 核心表结构设计

#### 4.2.1 用户表 (sys_user)

```sql
CREATE TABLE `sys_user` (
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
  `user_type` tinyint DEFAULT '1' COMMENT '用户类型 1-业主 2-家属 3-租户 4-物业人员 5-管理员',
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
```

#### 4.2.2 角色表 (sys_role)

```sql
CREATE TABLE `sys_role` (
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
```

#### 4.2.3 房屋表 (base_house)

```sql
CREATE TABLE `base_house` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `building_id` bigint NOT NULL COMMENT '楼栋ID',
  `unit_id` bigint DEFAULT NULL COMMENT '单元ID',
  `house_no` varchar(20) NOT NULL COMMENT '房号',
  `floor` int DEFAULT NULL COMMENT '楼层',
  `house_type` varchar(20) DEFAULT NULL COMMENT '户型 如一室一厅',
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
```

#### 4.2.4 业主表 (base_owner)

```sql
CREATE TABLE `base_owner` (
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
```

#### 4.2.5 报修表 (prop_repair)

```sql
CREATE TABLE `prop_repair` (
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
```

#### 4.2.6 账单表 (fee_bill)

```sql
CREATE TABLE `fee_bill` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bill_no` varchar(32) NOT NULL COMMENT '账单号',
  `owner_id` bigint NOT NULL COMMENT '业主ID',
  `house_id` bigint NOT NULL COMMENT '房屋ID',
  `bill_type` tinyint NOT NULL COMMENT '账单类型 1-物业费 2-停车费 3-水电费 4-其他',
  `bill_period` varchar(20) NOT NULL COMMENT '账期 如2026-05',
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
```

#### 4.2.7 订单表 (life_order)

```sql
CREATE TABLE `life_order` (
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
```

#### 4.2.8 门禁记录表 (guard_access_log)

```sql
CREATE TABLE `guard_access_log` (
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
```

### 4.3 数据库设计规范

#### 4.3.1 命名规范
- **表名**: 小写，下划线分隔，模块前缀+表名，如`sys_user`、`prop_repair`
- **字段名**: 小写，下划线分隔，如`create_time`、`user_name`
- **索引名**: `uk_`唯一索引，`idx_`普通索引，如`uk_username`、`idx_status`

#### 4.3.2 字段规范
- **主键**: `id`，bigint类型，自增
- **逻辑删除**: `deleted`字段，tinyint类型，0-未删除 1-已删除
- **创建信息**: `create_by`、`create_time`
- **更新信息**: `update_by`、`update_time`
- **状态字段**: `status`，tinyint类型

#### 4.3.3 索引规范
- 主键、外键必须建索引
- 查询条件字段建索引
- 排序字段建索引
- 避免过多索引（单表不超过5个）

---

## 5. 接口设计规范

### 5.1 RESTful API规范

#### 5.1.1 URL设计
```
/api/{version}/{module}/{resource}/{action}

示例:
/api/v1/user/info              # 获取用户信息
/api/v1/house/list             # 获取房屋列表
/api/v1/repair/create          # 创建报修
/api/v1/bill/pay               # 账单支付
```

#### 5.1.2 HTTP方法
| 方法 | 用途 |
|------|------|
| GET | 获取资源 |
| POST | 创建资源 |
| PUT | 更新资源（全量）|
| PATCH | 更新资源（部分）|
| DELETE | 删除资源 |

#### 5.1.3 状态码
| 状态码 | 含义 |
|--------|------|
| 200 | 请求成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 5.2 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    // 业务数据
  },
  "timestamp": 1715904000000
}
```

### 5.3 分页格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 10
  }
}
```

### 5.4 核心接口列表

| 接口 | 方法 | URL | 说明 |
|------|------|-----|------|
| 用户登录 | POST | /api/v1/auth/login | 用户登录 |
| 用户注册 | POST | /api/v1/auth/register | 用户注册 |
| 获取用户信息 | GET | /api/v1/user/info | 获取当前用户信息 |
| 修改用户信息 | PUT | /api/v1/user/info | 修改用户信息 |
| 获取房屋列表 | GET | /api/v1/house/list | 获取房屋列表 |
| 获取房屋详情 | GET | /api/v1/house/{id} | 获取房屋详情 |
| 创建报修 | POST | /api/v1/repair/create | 创建报修工单 |
| 获取报修列表 | GET | /api/v1/repair/list | 获取报修列表 |
| 获取报修详情 | GET | /api/v1/repair/{id} | 获取报修详情 |
| 更新报修状态 | PUT | /api/v1/repair/{id}/status | 更新报修状态 |
| 获取账单列表 | GET | /api/v1/bill/list | 获取账单列表 |
| 账单支付 | POST | /api/v1/bill/pay | 账单支付 |
| 获取公告列表 | GET | /api/v1/notice/list | 获取公告列表 |
| 创建访客预约 | POST | /api/v1/visitor/create | 创建访客预约 |
| 获取访客列表 | GET | /api/v1/visitor/list | 获取访客列表 |
| 文件上传 | POST | /api/v1/file/upload | 文件上传 |
| 发送验证码 | POST | /api/v1/sms/send | 发送短信验证码 |

---

## 6. 模块详细设计

### 6.1 用户权限模块

#### 6.1.1 认证流程
```
用户登录 -> 校验账号密码 -> 生成JWT Token -> 返回Token
         -> 登录失败 -> 记录失败次数 -> 超过5次锁定30分钟
```

#### 6.1.2 鉴权流程
```
请求API -> 网关拦截 -> 验证Token -> 解析用户信息 -> 鉴权检查 -> 放行/拒绝
```

#### 6.1.3 权限模型
采用RBAC（基于角色的访问控制）模型：
- 用户(User) <-> 角色(Role) <-> 权限(Permission)
- 支持数据权限控制（全部/部门/个人）

### 6.2 报修模块

#### 6.2.1 业务流程
```
业主提交报修 -> 系统自动派单 -> 维修人员接单 -> 上门维修
            -> 业主确认完成 -> 业主评价 -> 流程结束
```

#### 6.2.2 状态流转
```
待处理(0) -> 已派单(1) -> 处理中(2) -> 已完成(3)
         -> 已取消(4)
```

### 6.3 费用模块

#### 6.3.1 账单生成流程
```
定时任务 -> 查询费用标准 -> 计算费用 -> 生成账单 -> 推送通知
```

#### 6.3.2 支付流程
```
选择账单 -> 确认支付 -> 调用支付接口 -> 支付回调 -> 更新账单状态
       -> 生成缴费记录 -> 发送支付通知
```

### 6.4 门禁模块

#### 6.4.1 开门流程
```
人脸识别/刷卡/二维码 -> 设备上传识别结果 -> 校验权限 -> 返回开门指令
                     -> 记录通行日志
```

---

## 7. 安全设计

### 7.1 认证机制
- **JWT Token**: 用户登录后颁发JWT Token
- **Token刷新**: 支持Token自动刷新机制
- **Token失效**: 支持主动登出使Token失效

### 7.2 密码安全
- **加密算法**: BCrypt（自适应哈希，自动加盐）
- **密码强度**: 必须包含字母和数字，长度8-20位

### 7.3 接口安全
- **HTTPS**: 全站HTTPS传输
- **防重放攻击**: 请求时间戳校验，5分钟有效期
- **接口限流**: 基于Sentinel的限流保护

### 7.4 数据安全
- **敏感数据加密**: 身份证号、手机号等加密存储
- **SQL注入防护**: 使用MyBatis参数化查询
- **XSS防护**: 前端输入过滤，后端输出转义

### 7.5 日志审计
- **操作日志**: 记录关键操作（增删改）
- **登录日志**: 记录登录登出行为
- **异常日志**: 记录系统异常信息

---

## 8. 部署架构

### 8.1 服务器规划

| 服务器 | 配置 | 部署服务 |
|--------|------|----------|
| Web服务器 | 4核8G | Nginx |
| 应用服务器1 | 8核16G | Gateway + 业务服务 |
| 应用服务器2 | 8核16G | Gateway + 业务服务 |
| 数据库服务器 | 16核32G | MySQL主库 |
| 数据库从库 | 8核16G | MySQL从库 |
| 缓存服务器 | 4核8G | Redis集群 |
| 文件服务器 | 4核8G | MinIO |
| 消息服务器 | 4核8G | RabbitMQ |

### 8.2 部署拓扑

```
                    ┌─────────────┐
                    │   用户请求   │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │    Nginx    │
                    │  (负载均衡)  │
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
    ┌──────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
    │   Gateway   │ │   Gateway   │ │   Gateway   │
    │   服务1     │ │   服务2     │ │   服务3     │
    └──────┬──────┘ └──────┬──────┘ └──────┬──────┘
           │               │               │
           └───────────────┼───────────────┘
                           │
                    ┌──────▼──────┐
                    │  微服务集群  │
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐        ┌────▼────┐        ┌────▼────┐
   │  MySQL  │        │  Redis  │        │  MinIO  │
   │  主从   │        │  集群   │        │  对象存储│
   └─────────┘        └─────────┘        └─────────┘
```

### 8.3 技术栈版本

| 组件 | 版本 | 部署方式 |
|------|------|----------|
| JDK | 17 | 容器部署 |
| MySQL | 8.0.36 | 物理机/云数据库 |
| Redis | 7.2 | 容器部署 |
| Nginx | 1.24 | 容器部署 |
| RabbitMQ | 3.12 | 容器部署 |
| Nacos | 2.2.x | 容器部署 |

---

## 9. 附录

### 9.1 项目结构

```
smart-community/
├── smart-community-parent/          # 父工程
│   └── pom.xml
├── smart-community-gateway/         # 网关服务
├── smart-community-common/          # 公共模块
│   ├── common-core/                 # 核心工具类
│   ├── common-security/             # 安全模块
│   ├── common-mybatis/              # MyBatis封装
│   └── common-redis/                # Redis封装
├── smart-community-api/             # API接口定义
├── smart-community-service/         # 业务服务
│   ├── user-service/                # 用户服务
│   ├── house-service/               # 房屋服务
│   ├── property-service/            # 物业服务
│   ├── fee-service/                 # 费用服务
│   ├── life-service/                # 生活服务
│   ├── security-service/            # 安防服务
│   ├── social-service/              # 互动服务
│   └── report-service/              # 报表服务
├── smart-community-admin/           # 管理后台前端
├── smart-community-app/             # 业主APP前端
└── smart-community-wx/              # 微信小程序
```

### 9.2 开发环境

| 工具 | 版本 | 说明 |
|------|------|------|
| IntelliJ IDEA | 2023.x | IDE |
| Maven | 3.9.x | 构建工具 |
| Node.js | 18.x | 前端运行环境 |
| npm | 9.x | 包管理器 |
| Git | 2.x | 版本控制 |

### 9.3 代码规范

- 遵循阿里巴巴Java开发手册
- 使用Google Java Format代码格式化
- 类注释、方法注释完整
- 单元测试覆盖率≥60%

### 9.4 修订记录

| 版本 | 日期 | 修订人 | 修订内容 |
|------|------|--------|----------|
| v1.0 | 2026-05 | - | 初稿 |

---

**文档结束**
