# migration-spring-boot-starter 模块设计

### 3.1 模块职责

提供Spring Boot集成能力，负责：
- 自动配置（打上@EnableMigration即可生效，无需配置文件）
- @Migration注解支持
- 代理oldMethod/newMethod指向的Bean方法

### 3.2 技术选型

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.0 | 基础框架 |
| Spring AOP | 6.1.0 | AOP支持 |
| migration-sdk-core | 1.0-SNAPSHOT | 依赖核心SDK |

### 3.3 核心注解设计

```java
/**
 * 启用迁移功能的注解
 * 打上此注解即可生效，无需额外配置
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MigrationAutoConfiguration.class)
public @interface EnableMigration {
}

/**
 * 迁移注解，标记在Controller方法上
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Migration {
    /**
     * 迁移任务key
     */
    String key();

    /**
     * 旧接口方法Bean名称
     */
    String oldMethod();

    /**
     * 新接口方法Bean名称
     */
    String newMethod();

    /**
     * 降级方法Bean名称（可选）
     */
    String fallBackMethod() default "";

    /**
     * 参数处理器类
     */
    Class<? extends ParamHandler> paramHandler() default ParamHandler.class;

    /**
     * 核心线程池大小
     */
    int corePoolSize() default 2;

    /**
     * 最大线程池大小
     */
    int maxPoolSize() default 10;

    /**
     * 队列容量
     */
    int queueCapacity() default 100;

    /**
     * 线程名前缀
     */
    String threadNamePrefix() default "migration-";
}
```

### 3.4 自动配置类

```java
/**
 * 自动配置类
 * 使用@ConditionalOnBean确保只有@EnableMigration触发时才配置
 */
@Configuration
public class MigrationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConfigClient configClient() {
        // 默认实现：从配置中心拉取
        // 可通过实现ConfigClient接口自定义
        return new ConfigCenterConfigClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public DiffServiceCaller diffServiceCaller() {
        // 默认实现：HTTP调用
        return new HttpDiffServiceCaller();
    }

    @Bean
    @ConditionalOnMissingBean
    public MigrationStrategyRegistry migrationStrategyRegistry(
            List<MigrationStrategy> strategies) {
        return new MigrationStrategyRegistry(strategies);
    }

    @Bean
    public MigrationInterceptor migrationInterceptor(
            ConfigClient configClient,
            DiffServiceCaller diffServiceCaller,
            MigrationStrategyRegistry strategyRegistry) {
        return new MigrationInterceptor(configClient, diffServiceCaller, strategyRegistry);
    }

    @Bean
    public MigrationAnnotationAdvisor migrationAnnotationAdvisor(MigrationInterceptor interceptor) {
        return new MigrationAnnotationAdvisor(interceptor);
    }
}
```

### 3.5 接入方使用示例

```java
// 接入方只需两步：
// 1. 在启动类上添加@EnableMigration注解
@EnableMigration
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

// 2. 在方法上使用@Migration注解
@RestController
public class UserController {

    @Migration(
        key = "user-getUser-api",
        oldMethod = "getUserOld",
        newMethod = "getUserNew",
        paramHandler = UserParamHandler.class
    )
    @GetMapping("/user/{id}")
    public User getUser(@PathVariable String id) {
        // 实际业务逻辑不需要在这里处理
        // 会被MigrationInterceptor代理
        return null;
    }

    // 实际的方法实现（被SDK调用）
    public User getUserOld(String id) { /* 旧接口逻辑 */ }
    public User getUserNew(String id) { /* 新接口逻辑 */ }
}

// 参数处理器
@Component
public class UserParamHandler implements ParamHandler {
    @Override
    public Map<String, Object> build(Object... args) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", args[0]);
        return params;
    }
}
```

### 3.6 模块依赖

```
migration-spring-boot-starter
├── spring-boot-starter
├── spring-boot-autoconfigure
├── spring-aop
└── migration-sdk-core
```

---
