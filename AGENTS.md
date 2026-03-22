# 后端接口迁移平台（migration）架构总览（AGENTS）

本文件为代理与开发者提供高层架构概览，帮助快速理解代码结构与关键约束，便于开发、重构、测试与排障。内容基于仓库内代码与文档，避免泛化描述。

**项目概览**
- 多模块单仓库，后端以 Spring Boot 为主，配合前端 Vue3 管理界面与多语言 SDK（Java/Go）。父 POM 管理版本与依赖，核心业务按 DDD 四层组织。
- 顶层 Maven 模块：`pom.xml:15` 指定 `migration-sdk`、`migration-admin`、`migration-diff-service` 三大模块。
- 管理后台（Admin）：
  - 后端 API：迁移任务、灰度规则、Diff 规则的读写与推送（配置中心），Diff 结果分页查询与统计；采用 DDD 分层（interfaces/application/domain/infrastructure）。
  - 前端 UI：Vue3 + Element Plus + Pinia + Router，提供任务/规则配置、Diff 可视化与统计。
- Diff 服务：接收 SDK 异步上报的新旧响应，依据配置中心存储的 Diff 规则进行比对与落库（仅 Diff 记录落库）。
- SDK：
  - Java Core + Spring Boot Starter：拉取迁移与灰度配置，按迁移状态选择旧/新/并发调用，并异步触发 Diff；Starter 提供 `@EnableMigration` 与 `@Migration` 注解接入。
  - Go SDK：与 Java SDK 策略一致（命名与枚举遵循 Go 规范）。
- 配置中心：使用 Nacos，Admin 推送（publish），Diff/SDK 拉取（getConfig）。Admin 实现见 `migration-admin/.../NacosConfigCenterGateway.java:16`，Diff 规则拉取见 `migration-diff-service/.../NacosDiffRuleRepository.java:25`。

**构建与命令**
- 后端全仓构建：在仓库根目录执行（来源：`deploy/README.md:24` 与 `deploy/deploy.sh:12`）
  - `mvn clean package -DskipTests`
- Docker 编排（`deploy` 目录）：
  - 构建与启动：`docker-compose up -d --build`（`deploy/README.md:31`，`deploy/deploy.sh:28`）
  - 需准备 `.env` 填充 Nacos 与 MySQL 变量（`deploy/README.md:42`）。核心环境项在 `deploy/docker-compose.yml:13-18,31-35`：
    - `SPRING_PROFILES_ACTIVE=prod`
    - `NACOS_SERVER_ADDR`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- 前端 UI（`migration-admin/migration-admin-ui/package.json:6-10`）：
  - 开发：`npm run dev`
  - 构建：`npm run build`
  - 预览：`npm run preview`
- 访问入口（`deploy/README.md:36-41`）：
  - Admin UI: `http://localhost`
  - Admin API 文档（Knife4j）: `http://localhost:8080/doc.html`
  - Diff API 文档（Knife4j）: `http://localhost:8081/doc.html`

**代码风格与约束**
- JSON 字段强制使用下划线命名（snake_case），例如 `migration_key`、`has_diff`（`docs/开发规范.md:16-47`）。
- 枚举命名：
  - Java/前端使用 `UPPER_SNAKE_CASE`（如 `IGNORE`, `TOLERANCE`），Go 使用 `PascalCase`（`docs/开发规范.md:49-58`）。
- 模块名与服务注册名：目录/Artifact 使用 `kebab-case`，服务名使用点分隔（如 `migration-admin-api` ↔ `migration.admin.api`，`docs/开发规范.md:61-71`）。
- Java 风格：遵循 Google Java Style，4 空格缩进，行长 120，UTF-8，广泛使用 Lombok（`docs/开发规范.md:76-83`）。
- DDD 强约束：
  - Admin 与 Diff 服务按 `interfaces`/`application`/`domain`/`infrastructure` 分层；`interfaces` 禁止直接访问 Mapper/DAO（`docs/分模块架构设计与详细设计.md:937-951,1539-1553`）。
- API 风格：不使用 RESTful 资源化；路径体现动作，优先 POST（`docs/开发规范.md:526-544`）。统一响应格式 `code/message/data` 与分页 `PageResult`（`docs/开发规范.md:546-586,562-577`）。

**测试**
- Java 测试依赖：`junit-jupiter` 与 `spring-boot-starter-test`（Admin：`migration-admin/migration-admin-api/pom.xml:86-94`；Diff：`migration-diff-service/pom.xml:78-86`）。
- Java 测试组织：按模块目录的 `.../test/java/...`，类名以 `*Test.java` 结尾，包含应用服务、装配器、控制器等（例如 Admin 与 Diff 相关测试目录结构在仓库中已建立）。
- Go SDK：包含 `*_test.go` 用例（如 `migration-sdk-go/gray/matcher_test.go`、`strategy/handlers_test.go`）。
- 执行建议：
  - Maven：按模块执行 `mvn -pl <module> test`（基于 Maven 标准行为）。
  - Go：在 `migration-sdk-go` 目录执行 `go test ./...`（标准 Go 测试约定）。

**安全**
- 管理后台认证：提供 JWT 拦截器与令牌生成器（`migration-admin/.../JwtSecurityInterceptor.java:15`、`JwtTokenProvider.java:13`）。
  - 当前拦截器 `preHandle` 对无效/缺失令牌未拦截，直接 `return true`（`JwtSecurityInterceptor.java:26-38`）；生产环境需启用未认证返回（类内 `sendUnauthorizedResponse` 已实现，注释在 `JwtSecurityInterceptor.java:36-44`）。
  - 令牌签名密钥来源 `AuthProperties.jwtSecret`，过期时间由配置控制（`JwtTokenProvider.java:19-34`）。
- 配置与密钥管理：
  - 所有敏感信息（数据库口令、Nacos 账号等）应通过环境变量与配置中心注入；避免硬编码与提交到 Git（规范与样例见 `docs/开发规范.md:323-326`）。
  - 生产配置通过环境注入：`application-prod.yml` 使用占位符读取环境变量（`migration-admin/.../application-prod.yml:8-19`、`migration-diff-service/.../application-prod.yml:8-18`）。
- 接口暴露：Diff 服务 API 仅供内部 SDK 调用，建议内网访问与鉴权（见 `docs/分模块架构设计与详细设计.md:1501-1521`）。

**配置管理**
- Spring Profile：默认 `local`（`migration-admin/.../application.yml:11-12`、`migration-diff-service/.../application.yml:11-12`）。Docker 部署强制 `prod`（`deploy/docker-compose.yml:13,31`）。
- Admin 配置中心网关：
  - Nacos 连接参数通过 `migration.nacos.*`（`NacosConfigCenterGateway.java:21-35`），提供 `publish/getConfig/delete` 封装，默认组 `DEFAULT_GROUP`（`ConfigCenterGateway.java:10-28`）。
- Diff 规则拉取：
  - DataId 优先前缀 `migration_`，组优先 `DIFF_RULE_GROUP`，回退 `DEFAULT_GROUP`，兼容历史前缀 `diff_`（`NacosDiffRuleRepository.java:27-33,71-77`）。
  - 规则 JSON 字段必须为 snake_case（`NacosDiffRuleRepository.java:130-136`）。
- 数据库：仅 Diff 记录落库（表结构样例在设计文档中，Admin 的 MyBatis-Plus 映射与实体见对应 `infrastructure/persistence` 与 `mapper`）。

**部署与运行**
- 依赖前置：需具备可访问的 Nacos 与 MySQL（`deploy/README.md:7-10`）。
- 环境变量：在 `deploy/.env` 中设置，Docker Compose 的后端服务读取同名变量（`deploy/docker-compose.yml:13-18,31-35`）。
- 容器编排：三服务镜像分别来自对应模块目录的 `Dockerfile`；Admin UI 通过 `nginx.conf` 挂载（`deploy/docker-compose.yml:38-51`）。
- 便捷部署脚本：`deploy/deploy.sh` 自动构建与编排，包含停止旧容器与启动新容器（`deploy/deploy.sh:22-29`）。

**关键开发约束速览**
- 迁移状态机（7 阶段）与灰度策略由 SDK 实现，Admin 负责配置推送；Diff 服务只读规则并产出记录（详见设计文档各模块章节）。
- API 约定统一走 `POST` 动作路径与分页响应；UI 与后端字段遵循 snake_case；跨语言枚举遵循各自命名规范。
- Admin/Diff 均使用 Knife4j 生成 OpenAPI 文档（依赖见各模块 POM）。
- 规则执行器：支持 `IGNORE/TOLERANCE/SCRIPT/SORT` 类型；路径匹配支持精确与通配（参考 Diff 设计文档示例）。

**附注**
- 仓库未发现 Cursor 规则（`.cursor/rules/`）、Copilot 规则（`.github/copilot-instructions.md`）或 Trae 规则（`.trae/rules/`）；如后续新增，应将关键约束补充至本文件。
- 若存在 `AGENT.md`（单数），应重命名为 `AGENTS.md` 并在此补充改进建议；当前仓库未检测到该文件。

