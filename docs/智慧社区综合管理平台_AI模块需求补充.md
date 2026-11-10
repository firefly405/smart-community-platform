# 智慧社区综合管理平台 - AI智能问答模块需求补充

**版本**: v1.0  
**日期**: 2026年5月19日  

---

## 1. 模块概述

在现有平台功能基础上，新增**AI智能问答模块**，使社区居民和管理人员可通过自然语言对话的方式，查询社区动态数据（车位、账单、报修、活动等）和社区静态知识（办事指南、规则制度等）。

## 2. 技术方案

### 2.1 技术栈

| 组件 | 技术选型 |
|------|---------|
| API平台 | 管理员配置的 OpenAI 兼容 API（如硅基流动） |
| SDK | Spring AI OpenAI (`1.0.0-M5`) |
| 向量嵌入 | 通过 `/v1/models` 自动识别；无向量模型时降级为普通问答 |
| 对话生成 | 通过 `/v1/models` 自动识别，可在配置页手动调整 |
| 向量存储 | SimpleVectorStore（Spring AI内置内存向量库） |
| 函数调用 | Spring AI Function Calling（15个工具函数） |

### 2.2 架构设计

采用 **RAG（检索增强生成）+ Function Calling（工具调用）** 混合架构：

```
用户提问
    ↓
LLM 意图识别（已配置的对话模型）
    ├── 静态知识类问题 → RAG向量检索 → 拼接上下文 → LLM生成
    └── 动态数据类问题 → Function Calling → 执行SQL查询 → LLM生成
    ↓
SSE 流式返回 / JSON同步返回
    ↓
持久化到 sys_qa_log
```

### 2.3 RAG 知识库

静态知识通过 `sys_knowledge_doc` 表管理，预置12条社区知识文档：
- 物业管理：报修流程、投诉渠道、服务中心信息
- 费用管理：物业费缴纳指南、车位费用说明
- 安防管理：门禁使用指南、访客登记流程
- 生活服务：快递收发说明、社区活动指南
- 社区互动：帖子规范、二手交易规则
- 综合信息：FAQ、紧急联系方式

系统启动时自动加载所有启用文档到 SimpleVectorStore。

### 2.4 Function Calling 工具

共15个工具函数，覆盖全部57张业务表：

| # | 工具名 | 查询能力 | 涉及表 |
|---|--------|---------|--------|
| 1 | ParkingQueryTool | 车位查询 | base_parking, base_vehicle |
| 2 | BillQueryTool | 账单查询 | fee_bill, fee_item |
| 3 | RepairQueryTool | 报修查询 | prop_repair, prop_repair_item |
| 4 | ComplaintQueryTool | 投诉查询 | prop_complaint |
| 5 | NoticeQueryTool | 通知公告 | prop_notice |
| 6 | ActivityQueryTool | 社区活动 | life_activity |
| 7 | ExpressQueryTool | 快递信息 | life_express |
| 8 | HouseInfoQueryTool | 房屋信息 | base_house, base_owner, base_family, base_tenant |
| 9 | LifeServiceQueryTool | 便民服务 | life_service, life_rental, life_product, social_phone |
| 10 | VisitorQueryTool | 访客记录 | guard_visitor |
| 11 | AccessLogQueryTool | 门禁通行 | guard_access_log |
| 12 | PostQueryTool | 社区帖子 | social_post |
| 13 | SecondhandQueryTool | 二手交易 | social_secondhand |
| 14 | VoteQueryTool | 社区投票 | social_vote, social_vote_option |
| 15 | EquipmentQueryTool | 设备设施 | prop_equipment, prop_equipment_maintain |
| 16 | AlarmQueryTool | 报警记录 | guard_alarm |
| 17 | InspectionQueryTool | 巡检保洁 | prop_inspection, prop_cleaning |

> 注：LifeServiceQueryTool 内部包含4个子工具（Services, Phones, Products, Rentals），SocialQueryTool 拆分为3个独立工具（Post, Secondhand, Vote）。

## 3. 数据库设计

### 3.1 新增表

| 表名 | 说明 |
|------|------|
| `sys_qa_log` | 问答记录表（question, answer, sources, user_id, user_name） |
| `sys_knowledge_doc` | 知识库文档表（title, content, category, embedding_id, status） |

### 3.2 API 接口

| Method | 路径 | 说明 |
|--------|------|------|
| POST | `/api/v1/qa/ask` | 流式问答（SSE） |
| POST | `/api/v1/qa/ask-sync` | 同步问答（JSON） |
| GET | `/api/v1/qa/knowledge/list` | 知识库列表 |
| POST | `/api/v1/qa/knowledge` | 新增知识文档 |
| PUT | `/api/v1/qa/knowledge` | 编辑知识文档 |
| DELETE | `/api/v1/qa/knowledge/{id}` | 删除知识文档 |
| POST | `/api/v1/qa/knowledge/reload` | 重建向量索引 |

## 4. 前端设计

### 4.1 智能问答页面 (QaPage.vue)

- 类ChatGPT聊天界面，气泡式对话
- 快捷提问入口（预设常见问题）
- AI加载中的打字指示器动画
- 支持Enter快捷发送

### 4.2 知识库管理 (KnowledgeList.vue)

- ProTable标准CRUD列表
- 分类筛选（物业/安防/费用/生活/社交/综合）
- "重建向量索引"按钮（调用后端reload API）

### 4.3 侧边栏菜单

新增 "AI服务" 菜单组，含"智能问答"和"知识库管理"两个子项。

## 5. 配置说明

```yaml
# application.yml
spring:
  ai:
    openai:
      api-key: ${AI_API_KEY:disabled}
      base-url: ${AI_PROVIDER_BASE_URL:https://api.openai.com}
      chat:
        enabled: ${AI_ENABLED:false}
        options:
          model: ${AI_CHAT_MODEL:gpt-4o-mini}
      embedding:
        enabled: ${AI_EMBEDDING_ENABLED:false}
        options:
          model: ${AI_EMBEDDING_MODEL:text-embedding-3-small}
```

管理员可在「系统管理 -> AI配置」中只填写 API Base URL 和 API Key，系统自动识别模型并写入本地运行时配置。

## 6. 预算评估

- 费用取决于管理员选择的 API 服务商和模型，免费额度以服务商控制台为准
- 对话与向量模型从同一 OpenAI 兼容端点自动识别

## 7. 文件清单

| 层 | 新增文件数 | 修改文件数 |
|----|-----------|-----------|
| SQL | 1 | 0 |
| 后端 Java | 25 | 3 (pom.xml × 2 + application.yml) |
| 前端 Vue/TS | 4 | 2 (router + layout) |
| 文档 | 1 | 1 (开发计划追加) |
| **合计** | **31** | **6** |
