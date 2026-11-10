# 智慧社区完整项目包：源码打开与课堂演示指南

此包包含打包时的本地工作区版本（含未提交源码），不是仅导出 Git 已提交版本。本文中的路径均相对于解压后的 `SmartCommunity` 目录。

## 1. 包含内容

- 管理端和“智慧社区”业主端的前端源码、前端 dist 构建文件及 node_modules。
- Java 后端源码、Maven 配置及可运行 JAR。
- 当前 PRD V2.0、历史 PRD、其他项目文档和功能结构图。
- Start-Community.cmd、一键启动脚本、自检脚本、打包脚本和使用说明。
- 数据库初始化/增量补丁/演示数据 SQL、示例图片、素材许可与来源清单。
- 端到端测试源码与现有依赖；不包含浏览器安装包及测试运行产物。

## 2. 先解压，再打开

1. 右键压缩包，选择“全部解压”。建议解压到较短的本地路径，例如 `D:\SmartCommunity`，减少 Windows 长路径问题。
2. 确认打开的文件夹内能看到 `Start-Community.cmd`、`smart-community-frontend`、`smart-community-backend` 和 `docs`。
3. 不要直接在压缩包预览窗口中运行启动器、打开 Maven 工程或修改源码。
4. 移动到其他电脑后使用包内 `Start-Community.cmd`，不要使用原电脑桌面的绝对路径快捷方式。

## 3. 所需软件环境

### 启动器新增：一键检测本机环境

双击 `Check-Environment.cmd` 可只检测环境，不启动前后端、不启动数据库、不编译，也不自动安装软件。它会运行软件版本、依赖及只读网络检查，生成并尝试用浏览器打开中文报告 `.runtime/launcher/environment-report.html`。正式双击 `Start-Community.cmd` 时也会先执行同一检测。

报告首先集中列出缺失、不兼容和需处理的项目，然后列出全部检查项及处理建议：

- 运行条件：Windows PowerShell、JDK 17、Node.js 18+、npm、项目源码与配置、Vite/Vue/Element Plus 和 esbuild 原生模块、后端 JAR、MySQL 连接与服务、8001/5174 端口、浏览器。
- 源码开发工具：VS Code、IntelliJ IDEA、Maven 3.9+、Git。工具从 PATH、常见安装目录及部分 Windows 安装登记查找，并非只检查桌面快捷方式。
- 可选/待验证：MySQL 命令行客户端、AI、Redis、IDE 插件；数据库账号、MySQL 服务版本、表结构及 IDE 项目 SDK/许可证不会因软件存在就被判定通过。

`BLOCK` 表示当前启动条件存在阻断，全部列出后停止；`WARN` 表示建议或可恢复事项；`VERIFY` 表示需要人工或启动后继续验证；`OK` 表示此项检查通过。缺少 VS Code、IDEA、Git 不阻止浏览器运行系统；后端需要重新编译且没有健康运行后端时，Maven 才作为阻断条件。

自定义/便携安装可能无法自动识别。检测不会扫描整个磁盘，不展示数据库密码或 API Key，不验证所有 IDE 插件或真实 AI 额度。数据库端口可达不等于认证和表结构已正确；请按报告建议及后端日志继续确认。`-NoBrowser` 可禁止自动打开报告和页面。

### 环境清单

| 软件 | 用途 | 要求 |
| --- | --- | --- |
| Windows | 本版一键启动脚本运行环境 | Windows PowerShell 5.1 可用 |
| JDK | 编译、运行后端 | JDK 17；当前工程使用预览特性，运行需 `--enable-preview` |
| Node.js | 运行前端开发服务 | 18+，本机验证使用 20.x |
| MySQL | 业务数据存储 | 8.0，需有项目数据库及可用账号 |
| Maven | 导入后端、源码编译 | 建议 3.9+，依赖首次下载可能需要联网 |
| Visual Studio Code | 查看、编辑前端源码 | 推荐安装 Vue - Official 扩展 |
| IntelliJ IDEA | 查看、调试 Java 后端 | Community 或 Ultimate 均可；Community 可从 main 方法启动 |
| 浏览器 | 展示实际系统 | Edge / Chrome |

运行页面不要求一直打开 VS Code 或 IDEA；它们是开发工具。页面由前端服务提供，业务接口由 Java 后端提供，数据保存在 MySQL。

包内不包含上述软件的机器级安装目录，不是无需配置环境的独立 EXE。前端 node_modules 来自本机 Windows；换系统/架构或依赖异常时，在前端目录执行 `npm ci` 重新安装。

## 4. 如何打开前端源码

1. 打开 VS Code，选择“文件 -> 打开文件夹”。
2. 选择解压后的 `smart-community-frontend` 文件夹。需要同时浏览文档时，也可以打开整个 `SmartCommunity` 文件夹。
3. 如提示工作区信任，仅在确认来源可信后允许。
4. 按下方目录查看代码，不要把 `dist` 当作原始源码修改。

| 路径 | 内容 |
| --- | --- |
| `src/views/community/` | 业主端：首页、报修、账单、智能问答、二手和社区帖子等 |
| `src/views/system/` | 系统管理、个人中心、AI 配置 |
| `src/views/property/` | 房屋、业主、车辆、报修等业务页面 |
| `src/views/charge/`、`life/`、`guard/`、`social/`、`report/` | 财务、生活、安防、互动、报表相关页面，均位于 src/views 下 |
| `src/router/index.ts` | 管理端/业主端路由与登录跳转 |
| `src/api/` | 调用后端接口的代码 |
| `src/components/` | 共用表格、弹窗、表单、图表等组件 |
| `package.json`、`vite.config.ts` | 依赖、运行命令、5174 端口与 8001 后端代理 |
| `dist/` | 已构建的网页文件，不是源码入口 |

若只想单独启动前端，在 VS Code 的终端进入前端目录，执行：

```powershell
npm run dev -- --host 127.0.0.1 --port 5174 --strictPort
```

首次依赖缺失时先运行 `npm ci`。前端正常显示登录页，不代表后端和数据库已经可用；接口仍需后端服务。若 5174 已被一键启动器占用，不要重复启动。

## 5. 如何打开后端源码

1. 打开 IntelliJ IDEA，选择“Open / 打开”。
2. 选择 `smart-community-backend/smart-community-parent/pom.xml`，按提示作为 Maven 项目打开。
3. 不要只打开仓库根目录的 `src/Main.java`，那个文件不是社区系统后端入口。
4. 等待 Maven 同步。父工程通过相邻目录关联 common、api、admin 模块；必须保持解压后的目录结构完整。
5. 在 Project Structure 中设置 Project SDK 为 JDK 17；Maven 导入/运行 JDK 也使用 17。语言级别按当前工程预览特性设置为 17 Preview。
6. 确认 Maven 工具窗口中能看到各模块，必要时执行 Reload All Maven Projects。

| 模块/路径 | 内容 |
| --- | --- |
| `smart-community-parent/pom.xml` | 父工程、依赖版本和多模块配置 |
| `smart-community-admin/src/main/java/com/smartcommunity/SmartCommunityAdminApplication.java` | Spring Boot 启动类 |
| `smart-community-admin/src/main/java/com/smartcommunity/controller/` | 登录、房屋、报修、业主服务、AI 等接口 |
| `smart-community-admin/src/main/java/com/smartcommunity/security/` | JWT、当前用户与权限校验 |
| `smart-community-common/src/main/java/com/smartcommunity/` | entity、mapper、service、payment、tool 等业务及公共代码 |
| `smart-community-admin/src/main/resources/application.yml` | 数据库、8001 端口、上传、AI 等运行配置 |
| `smart-community-api/` | API 模块，当前不是另一个需要单独启动的社区服务 |
| `sql/` | 数据库建表、种子数据与补丁 |

### 5.1 在 IDEA 中调试后端（可选）

正常课堂演示优先使用一键启动器。需要断点调试时，先确认 8001 没有运行旧后端，再在 IDEA 中为 `SmartCommunityAdminApplication` 建立 Application 运行配置：

- JRE：JDK 17。
- VM options：`--enable-preview`。
- Working directory：解压目录下的 `smart-community-backend/smart-community-admin`。
- Environment variables：按实际数据库设置 `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_DATABASE`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`；不要把真实密码投屏或提交。
- 使用 admin 模块的 classpath，运行 main 方法。

IDEA 不会因为目录里存在 `.env` 就自动读取它，需要在运行配置中明确设置；一键启动器则会读取后端的 `.env`。工作目录会影响 `.runtime/ai-config.properties` 与 `uploads` 的位置。

后端控制台出现启动成功日志、8001 接口可访问后，才能说明后端启动成功。不要同时启动 IDEA 后端和启动器中的另一个 Java 进程去争用 8001。

### 5.2 终端手动启动后端（可选）

下面示例假设解压路径为 `D:\SmartCommunity`，请按实际路径修改：

```powershell
cd D:\SmartCommunity\smart-community-backend
mvn -f smart-community-parent/pom.xml -DskipTests package
cd smart-community-admin
java --enable-preview -jar target/smart-community-admin-1.0.0-SNAPSHOT.jar
```

需要的数据库环境变量应提前在当前终端设置。`-DskipTests` 仅用于打包，不代表完整测试验收。包内已有 JAR 时可直接运行 Java 命令，但修改 Java 源码后必须重新打包，旧 JAR 不会自动更新。

## 6. 一键启动和数据库准备

### 6.1 当前电脑已配置环境、数据库时

双击解压目录内 `Start-Community.cmd`。它会检查环境和 MySQL、按需编译/启动后端、启动前端，并验证网页和 API 代理。成功后打开：

- 管理端：<http://127.0.0.1:5174/login>
- 业主端：<http://127.0.0.1:5174/community/login>

重复启动会复用健康服务，不强制结束现有进程。关闭启动器窗口不等于关闭后台服务。已运行的服务可能仍来自原项目目录；若要演示“解压包中的修改”，需先有序停止原项目服务，再从解压目录启动，不要把复用旧服务误认为运行了新代码。

### 6.2 换电脑或没有项目数据库时

1. 先安装并启动 MySQL，建立专用演示库及账号，参照项目 README 和 `sql/` 内脚本初始化。
2. 注意：`sql/import_all_portable.bat` 会删除并重建指定项目库！只允许在明确可重建的演示环境使用，不能对现有业务库直接执行。
3. 不要认为数据库初始化已经自动包含所有后续补丁；按所用版本核对补丁及图片脚本，例如 `patch_secondhand_images.sql`。执行前阅读 SQL、备份数据库并确认顺序，避免重复或破坏已有数据。
4. 参照 `smart-community-admin/.env.example`，在该目录配置自己的 `.env`。已有进程环境变量优先；值用 `NAME=value`，不写 shell 表达式。
5. 首次需要初始化管理员时按系统初始化页面完成；若已有演示用户，先测试准备好的管理账号和普通业主账号。注册一个账号不代表它自动拥有已绑定房屋。
6. 本包不含原机器真实 AI Key。需要 AI 演示时，由管理员在系统管理的 AI 配置中填写可用兼容 API、检测、保存并按提示重启。

本包不是当前 MySQL 实例的数据备份，不能保证新导入种子数据与原电脑当前业务记录完全相同。答辩前应在实际展示电脑完成一次从启动到登录的演练。

## 7. 如何向老师展示前后端

### 7.1 展示前准备

1. 提前启动并验证 MySQL、8001 后端和 5174 前端，检查所需演示记录及图片存在。
2. 提前验证一个管理账号、一个启用且已绑定业主档案的普通业主账号，避免现场才发现无房屋或密码错误。
3. 管理端与业主端使用不同浏览器，例如 Edge 管理端、Chrome 业主端；也可用普通窗口与独立隐私窗口。不要仅用同一浏览器的两个普通标签页切换身份，它们共用登录存储，可能互相覆盖。
4. 提前打开 PRD、功能结构图、VS Code 前端工程、IDEA 后端工程，关闭包含密钥、密码和居民敏感信息的窗口。
5. 如果计划展示断点，先安排后端由 IDEA 单独运行，不临时重复启动已有 JAR。

### 7.2 推荐演示顺序（约 10 至 15 分钟）

| 顺序 | 展示内容 | 操作与讲解重点 |
| --- | --- | --- |
| 1 | 项目整体结构 | 打开 `docs/智慧社区综合管理平台_PRD_V2.0_现状重编版.md` 或 `功能结构图.html`，说明管理端、业主端和共享后端 |
| 2 | 管理端页面 | 管理账号登录，展示功能导航、房屋与业主、账单、报修及统计页面；选择几个主流程，不必逐页点击 |
| 3 | 业主端页面 | 在另一浏览器登录普通业主，展示本人房屋、账单、报修、公告；说明普通用户没有管理员入口与权限 |
| 4 | 双端业务互通 | 业主提交一条明确标注“课堂演示”的报修，管理端找到同一单号，派单/处理/完成；业主端刷新查看结果并评价 |
| 5 | 邻里互动 | 展示二手物品图片和详情、公共帖子评论；说明交易为联系卖家，不是平台担保支付 |
| 6 | 后端接口 | 打开 `http://127.0.0.1:8001/doc.html` 查看接口分类，或浏览器 F12 的 Network 查看刚才业务请求和返回 |
| 7 | 前后端源码 | 从前端页面及 API 调用切到后端 Controller、Service、Mapper/Entity，解释同一流程如何落到数据库 |
| 8 | 权限与限制 | 指出业主仅能读本人数据；AI、真实支付和硬件接入边界，避免将演示能力说成生产能力 |

报修状态按页面允许的步骤推进，不跳过前态；派单时使用已准备的真实管理处理人员。演示数据只在授权演示库创建，不使用真实住户敏感问题，不为“重演”而清空数据库。

### 7.3 前端应该展示什么

前端就是浏览器中用户操作的界面，重点展示布局、表单校验、列表、详情、图表、跳转及错误反馈。可以在 VS Code 打开 `src/views/community/ResidentPage.vue` 和 `src/api/community.ts`，结合页面说明：“页面收集数据，通过 API 请求后端，再显示处理结果。”

按 F12 打开开发者工具，选择 Network 的 Fetch/XHR，执行一次查询或报修操作，展示请求路径、请求方法、状态和必要业务字段。不要投屏 Authorization 令牌、Cookie、完整联系方式或密码。

### 7.4 后端应该展示什么

后端不是另一个给住户使用的页面，而是 Java 服务和业务 API。展示建议：

1. IDEA 中打开 `SmartCommunityAdminApplication`，说明 Spring Boot 启动入口。
2. 打开 `ResidentController`，展示从登录身份解析业主、限制本人房屋以及创建报修的入口。
3. 打开 `AuthInterceptor`，说明身份校验和业主/管理端的接口边界；不宣称所有管理岗位细粒度权限已完成。
4. 打开 `PropRepairServiceImpl`，说明派单、处理、完成等合法状态变化。
5. 展示对应 Mapper/Entity 与数据库表关系；如使用数据库工具，只做经过授权的演示数据查询，不投屏敏感整表，也不现场随意执行 UPDATE/DELETE。
6. 展示后端接口文档 `http://127.0.0.1:8001/doc.html`。受保护业务接口需要登录取得 Bearer JWT，未认证调用返回 401/403 是正常安全行为，不是服务必然出错。

可以打开以下只读接口检查后端是否正常响应：

```text
http://127.0.0.1:8001/api/v1/setup/check
```

它返回 JSON，不是项目首页。接口检查成功只说明这一接口可用，不能替代全部业务验收。不要因为访问后端根地址没有界面，就认为后端没有启动。

### 7.5 可直接使用的项目介绍

“本项目采用前后端分离架构，前端使用 Vue 3、TypeScript、Element Plus 和 ECharts，后端使用 Java 17、Spring Boot 和 MyBatis-Plus，数据库是 MySQL。系统分为物业管理端和普通业主端，通过 HTTP API 与 JWT 身份认证连接。业主提交报修后，物业在管理端处理，双方操作的是同一条业务记录。当前包含档案、物业服务、费用、社区互动和 AI 等模块，部分外部能力仍属于演示或待接入范围。”

### 7.6 AI、支付与安防演示说明

- AI：仅在已配置有效 API 且供应商额度、网络正常时展示。可问“目前有多少空闲车位”，并与台账核对。回答是授权数据快照，不是传感器实时测量；业主 AI 不提供管理写操作。
- 支付：当前默认 Mock，明确讲“模拟支付，不发生真实资金流转”。业主账单当前仅查询，不要承诺在线缴费已完成。
- 安防：展示设备台账、访客和日志等现有能力，不把监控示意界面称为真实摄像头视频，不把日志核销称为实际开门。
- 统计：部分接口仍有占位指标，展示前按 PRD 的统计成熟度说明核对，不把固定示例值当作真实运营成绩。

## 8. 常见问题与日志

| 问题 | 检查方法 |
| --- | --- |
| 浏览器“拒绝连接” | 对应前端/后端端口没有健康服务，先启动并查看日志，不是单纯密码错误 |
| 登录页可见但接口报错 | 检查后端 8001、MySQL、前端代理与数据库账号 |
| 8001/5174 被占用 | 确认是否已有本项目运行，不要重复启动或随意结束其他进程 |
| 后端预览特性错误 | 使用 JDK 17，添加 `--enable-preview`，源码重新编译后再运行 |
| IDEA 同步失败 | 核对 Maven、网络、JDK 17 及父 POM 导入方式 |
| 业主页面没有私人数据 | 检查账号与有效业主档案/房屋关系，不要改用管理员假装业主 |
| 两端突然切换登录身份 | 使用不同浏览器/独立隐私窗口，避免共享 localStorage |
| AI 未启用或不可用 | 核对配置生效状态、重启、供应商权限/额度；普通业务可独立演示 |

启动器日志在项目 `.runtime/launcher/`。新启动服务会生成前后端 out/err 日志；复用的旧服务仍使用原日志位置。更多说明见 `docs/一键启动说明.md`。日志可能含 SQL 或业务信息，分享前必须脱敏。

## 9. 打包边界与校验

- 未打包当前 MySQL 实例的数据快照；包内 SQL 为初始化与演示材料，不是全量业务备份。
- 排除真实 `.env`、`.runtime`（包括 AI Key）、Git 历史、IDE 设置、日志、截图、私钥和桌面绝对路径快捷方式。
- 包内仍有演示凭据、演示数据和模拟支付，正式部署前必须修改默认凭据并安全加固。
- 前后端构建文件为打包时已有构建产物；本文更新未重新编译项目，也不声明所有功能验收通过。
- `PACKAGE-MANIFEST.json` 记录项目文件的相对路径、长度及 SHA-256；`PACKAGE-README.md` 和清单自身不在项目文件清单内。环境检测功能更新后，已同步更新变更文件的清单记录并校验全包；原有源码打开及课堂演示说明保留。
