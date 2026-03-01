# 后端接口迁移平台 (Backend Migration Platform)

## 项目简介

**后端接口迁移平台**是一个为解决微服务架构或遗留系统升级场景中，业务底层接口从旧版本迁移到新版本时面临的高风险问题而设计的综合性解决方案。通过渐进式迁移、灰度验证、并行对比（Diff）等机制，它能够确保接口迁移过程的安全、平滑与可控，最大程度降低由于新接口Bug或不兼容导致的线上事故。

该平台提供了一个完整的生态闭环，包括易于集成的多语言SDK、负责数据比对的独立Diff服务、以及可视化的配置管理后台。

## 核心特性

- **平滑渐进式迁移**：将迁移过程标准化为7个渐进阶段，从单跑旧接口、验证灰度、验证全开、上线灰度、上线全开，再到停用灰度与停用全开，逐步释放流量，降低单次切换的风险。
- **并行比对验证 (Diff)**：在安全的阶段（如验证阶段）并发调用新旧接口，不仅不影响线上请求响应（始终返回旧接口结果），还能通过独立Diff服务实时比对自己定义的数据差异。
- **多维度灰度策略**：支持丰富的灰度规则，包括 **百分比（Percentage）**、**白名单（Whitelist）**、**黑名单（Blacklist）** 以及 **表达式（Expression）** 配置，精准控制流量走向。
- **灵活的Diff规则定制**：除了默认的完全比对外，业务方可以配置忽略字段、数值容差、SpEL脚本规则，以及数组排序规则（按指定字段排序后再比对）。
- **轻量级接入**：通过提供 Java 原生 SDK、Spring Boot Starter 以及 Go SDK，业务方只需极少量的代码或简单的注解（`@Migration`），即可无侵入式地赋予现有业务强大的迁移能力。
- **实时监控与回滚**：全状态统一由配置中心下发，支持向后任意极端状态的一键切换/回滚，并在管理后台实时监控Diff结果异常与错误抛出。

## 系统架构

平台由以下核心模块组成：

1. **migration-sdk**：提供多种语言/框架的接入终端。
   - `migration-sdk-core`：Java 核心 SDK，负责拉取配置、策略执行（旧方法/新方法调用决策）与异步Diff请求发送。
   - `migration-spring-boot-starter`：简化 Spring Boot 项目接入，提供 AOP 切面及自动装配。
   - `migration-go`：Go 语言版本 SDK。
2. **migration-diff**：独立的数据比对微服务。接收来自 SDK 的新旧响应 JSON，依据配置中心拉取的 Diff 规则，执行对比逻辑（支持 SpEL 脚本与数组排序规则），支持横向扩展以承受高并发。
3. **migration-admin**：可视化的控制台系统。
   - `migration-admin-api`：提供迁移任务管理、灰度规则管理、以及 Diff 记录聚合检索的后端服务。
   - `migration-admin-ui`：基于 Vue/React 构建的前端应用，提供配置与监控视图。
4. **配置中心**：系统重度依赖配置中心（如 Nacos / Apollo），用于存储所有的迁移状态、灰度规则、与 Diff 规则参数，确保状态秒级下发生效。

`docs/架构设计文档.md`

## 技术栈

- **核心语言/框架**：Java 17, Spring Boot 3.2.0, Go
- **配置与注册中心**：Nacos (默认) 或 Apollo
- **数据库/ORM**：MySQL 8.0, MyBatis-Plus 3.5.5
- **性能与并发**：Disruptor (SDK 端高性能异步 Diff 上报), Cglib/Spring AOP
- **其他关键依赖**：FastJSON2, Apache HttpClient, zjsonpatch, json-path, Knife4j

## 7个标准迁移阶段定义

平台的核心在于根据状态码路由流量：

| 阶段 | 状态码 | 请求特点 | 响应来源 | Diff比对 | 灰度判定 |
| --- | :---: | --- | --- | :---: | :---: |
| **单旧** (Old) | 1 | 仅调用旧接口 | 旧接口 | ✗ | ✗ |
| **验证-灰度** (Validation-gray) | 2 | 新旧接口并发请求 | 旧接口 | ✓ | ✓ |
| **验证-全开** (Validation-all) | 3 | 新旧接口并发请求 | 旧接口 | ✓ | ✗ |
| **上线-灰度** (Go-Live-gray) | 4 | 新旧接口并发请求 | 根据灰度命中情况返回 | ✓ | ✓ |
| **上线-全开** (Go-Live-all) | 5 | 新旧接口并发请求 | 新接口 | ✓ | ✗ |
| **停用-灰度** (Decommissioning-gray)| 6 | 命中只调新，未命中并发调用 | 新接口 | 仅未命中 | ✓ |
| **停用-全开** (Decommissioning-all) | 7 | 仅调用新接口 | 新接口 | ✗ | ✗ |

## 快速接入指南 (Spring Boot 为例)

1. **引入依赖**
   在您的 Spring Boot 项目 `pom.xml` 中引入 starter：
   ```xml
   <dependency>
       <groupId>xxx</groupId>
       <artifactId>migration-spring-boot-starter</artifactId>
       <version>1.0-SNAPSHOT</version>
   </dependency>
   ```

2. **启用功能**
   在启动类上添加 `@EnableMigration` 注解。
   ```java
   @EnableMigration
   @SpringBootApplication
   public class Application { ... }
   ```

3. **配置参数抽取类**
   实现 `ParamHandler` 接口，用于将原方法的参数转换为灰度规则（如白名单、百分比使用的 key 等）进行匹配所需使用的字典：
   ```java
   @Component
   public class UserParamHandler implements ParamHandler {
       @Override
       public Map<String, Object> build(Object... args) {
           Map<String, Object> param = new HashMap<>();
           param.put("userId", args[0]); 
           return param;
       }
   }
   ```

4. **标注源方法**
   在Controller 或 Service 方法上，使用 `@Migration` 包装新旧逻辑调用。您无需改变原始实现，平台会在切面层拦截、分发、Diff。
   ```java
   @RestController
   public class UserController {
       @Migration(
           key = "user-getUser-api",          // 迁移任务的全局唯一标识
           oldMethod = "getUserOld",          // 旧接口方法名
           newMethod = "getUserNew",          // 新接口方法名
           fallBackMethod = "getUserFallback",// 可选降级方法
           paramHandler = UserParamHandler.class
       )
       @GetMapping("/user/{id}")
       public User getUser(@PathVariable String id) { 
           return null; // 此处逻辑被代理接管，不需要实现
       }

       public User getUserOld(String id) { /* 旧接口实现 */ }
       public User getUserNew(String id) { /* 新版重构实现 */ }
       public User getUserFallback(String id, Exception e) { /* 异常降级 fallback ... */ }
   }
   ```

5. **配置控制台**
   在管理后台中依据 `user-getUser-api` 这个 key，创建任务并配置对应的 Diff 规则（容差、全比对等）以及灰度规则。

## 目录结构说明

```text
├── docs/                                  # 详尽的架构与模块设计文档
├── migration-sdk/                         # 多语言终端 SDK 包
│   ├── migration-sdk-core/                # Java 纯核心库，不依赖Spring
│   ├── migration-spring-boot-starter/     # 针对 Spring 生态的自动装配集成
│   └── migration-go/                      # 针对 Go 语言生态的支持
├── migration-admin/                       # 迁移任务配置控制台
│   ├── migration-admin-api/               # Spring Boot 后端接口，负责数据持久化与下发
│   └── migration-admin-ui/                # Vue/React 前端工程视图
└── migration-diff/                        # 专注于比对 JSON 及执行脚本的 Diff 引擎微服务
```

## 文档参考

本项目在 `docs/` 目录下提供极其详实的架构文档，如果您需要进行二次开发或深入理解架构细节，请参阅：
- [需求分析文档](docs/需求分析文档.md) : 功能细节与状态枚举详解。
- [架构设计文档](docs/架构设计文档.md) : 全局宏观流转架构与部署说明。
- [分模块架构设计与详细设计](docs/分模块架构设计与详细设计.md) : SDK执行策略树、性能优化处理(Disruptor)与数据库设计细节。
- 其他各模块特定的设计说明书。

## 开发规范 

所有的开发规范请参阅在 `docs/开发规范.md` 中详尽说明的代码质量、日志、分支及API错误码要求。

## 许可证

MIT License 

