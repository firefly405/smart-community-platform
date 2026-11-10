# 智慧社区综合管理平台

面向社区场景的前后端分离演示项目，涵盖系统管理、物业服务、收费账务、生活服务、社区互动与 AI 问答等模块。

## 本地运行

1. 安装项目所需的 JDK、Maven、Node.js 和 MySQL。
2. 根据 `smart-community-backend/smart-community-admin/.env.example` 创建本地 `.env`，填写自己的数据库密码，并生成独立的 `JWT_SECRET`。`.env` 已被 Git 忽略；不要提交真实密钥。
3. 执行 `Check-Environment.cmd` 检查依赖，再按 `PACKAGE-README.md` 或 `Start-Community.cmd` 启动本地演示环境。

## 公开仓库说明

公开快照不含本机 `.env`、运行时目录、端到端截图或批量测试数据 SQL。请只使用虚构的测试数据；部署到真实社区前，应更换所有本地开发凭据并另行完成安全配置。
