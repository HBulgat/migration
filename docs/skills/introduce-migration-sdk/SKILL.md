---
name: introduce-migration-sdk
description: Guide and instructions on how to introduce and integrate the migration SDK into a Java (Spring Boot) or Go project.
license: MIT
compatibility: Java 17+, Spring Boot 3.2.0+, Go 1.18+
metadata:
  author: bulgat
  version: "1.0"
---

# 引入 Migration SDK 技能指南 (Introduce Migration SDK)

本技能旨在指导开发者或 AI Agent 如何在一个现有的 Java (Spring Boot) 或 Go 项目中引入并使用前后端接口迁移平台 (Migration) 的 SDK。

该迁移平台通过在代码层面代理请求，实现旧接口到新接口的渐进式迁移、灰度放量以及自动 Diff 比对功能。在使用前，系统需已部署完备的 `migration-admin` 和 `migration-diff` 服务，且配置中心（如 Nacos）工作正常。

---

## 🚀 Java 项目接入 (Spring Boot)

对于基于 Spring Boot 的 Java 项目，SDK 提供了自动装配（Starter）的能力，接入过程几乎是零侵入的。

### 1. 引入依赖

首先，在项目的 `pom.xml` 中引入 `migration-spring-boot-starter` 依赖：

```xml
<dependency>
    <groupId>top.bulgat</groupId>
    <artifactId>migration-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version> <!-- 推荐使用最新版本 -->
</dependency>
```

### 2. 启用迁移功能

在 Spring Boot 的启动类（带有 `@SpringBootApplication` 的类）上，加上 `@EnableMigration` 注解，激活 SDK 自动装配逻辑：

```java
import top.bulgat.migration.spring.boot.starter.annotation.EnableMigration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableMigration
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 实现灰度参数抽取规则 (ParamHandler)

平台支持根据业务参数进行灰度控制（例如依据 `userId`）。你需要实现 `ParamHandler` 接口，告诉 SDK 如何从被拦截的方法参数中提取这些灰度匹配键。

```java
import top.bulgat.migration.sdk.core.handler.ParamHandler;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserParamHandler implements ParamHandler {
    @Override
    public Map<String, Object> build(Object... args) {
        Map<String, Object> param = new HashMap<>();
        // 假设原接口的第一个参数是 userId
        param.put("userId", args[0]); 
        return param;
    }
}
```

### 4. 标注目标方法并提供新旧实现

在你的 Controller 或 Service 中的目标入口处加上 `@Migration` 注解。你无需在加上注解的原始方法里写实现，因为它会被 AOP 接管。但你需要在同一个 Bean 里提供旧版（`oldMethod`）和新版（`newMethod`）的对应实现方法。

```java
import top.bulgat.migration.spring.boot.starter.annotation.Migration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    /**
     * 迁移入口代理方法，实际逻辑由 SDK 分发。
     * key: 在 Admin 控制台全局唯一的任务标识
     */
    @Migration(
        key = "user-getUser-api",
        oldMethod = "getUserOld",
        newMethod = "getUserNew",
        fallBackMethod = "getUserFallback",
        paramHandler = UserParamHandler.class
    )
    @GetMapping("/user/{id}")
    public User getUser(@PathVariable String id) { 
        // 代理接管，此处返回 null 即可
        return null; 
    }

    public User getUserOld(String id) { 
        // 旧版接口逻辑
        return new User(id, "OldUser");
    }
    
    public User getUserNew(String id) { 
        // 新版重构接口逻辑
        return new User(id, "NewUser");
    }
    
    public User getUserFallback(String id, Exception e) { 
        // 可选：当新旧逻辑都不可用或发生致命错误时的降级逻辑
        return new User(id, "FallbackUser");
    }
}
```

---

## 🚀 Go 项目接入

对于 Go 项目，平台提供了一个轻量级的纯代码代理库。你需要手动包装你要执行的迁移函数。

### 1. 引入依赖

在你的 Go 项目路径下，使用 `go get` 拉取 SDK：

```bash
go get github.com/HBulgat/migration-sdk-go
```

### 2. 准备新旧方法与参数提取器

定义好原本的旧服务方法、新开发的服务方法、降级处理函数，以及能够提取灰度计算维度的提取器函数。**请注意：Go SDK 约定的被代理方法，统一需要接收 `args ...interface{}` 并返回 `(interface{}, error)`。**

```go
package main

import (
	"errors"
	"fmt"
)

// 你的业务模型
type User struct {
	ID   string `json:"id"`
	Name string `json:"name"`
}

// 1. 旧逻辑
func targetOld(args ...interface{}) (interface{}, error) {
	id := args[0].(string)
	return &User{ID: id, Name: "OldUser"}, nil
}

// 2. 新逻辑
func targetNew(args ...interface{}) (interface{}, error) {
	id := args[0].(string)
	return &User{ID: id, Name: "NewUser"}, nil
}

// 3. 降级逻辑
func targetFallback(err error, args ...interface{}) (interface{}, error) {
	fmt.Printf("[Fallback] error: %v\n", err)
	return nil, errors.New("fallback executed")
}

// 4. 参数提取逻辑 (如果不需要基于参数的灰度，可以直接返回空 map)
func userParamHandler(args ...interface{}) map[string]interface{} {
	return map[string]interface{}{
		"userId": args[0].(string),
	}
}
```

### 3. 初始化 Client 并执行

在你的入口或路由初始化位置，实例配置并代理具体执行方法：

```go
import (
	"log"
	
	"github.com/HBulgat/migration-sdk-go"
)

func main() {
	// A. 初始化配置
	// AdminUrl 与 DiffServiceUrl 指向目标服务的真实地址（或通过配置中心透传）
	config := &migration.Config{
		AdminUrl:       "https://migration.bulgat.top",
		DiffServiceUrl: "https://diff-migration.bulgat.top",
	}

	// B. 构造 SDK 客户端实例
	client := migration.NewClient(config)

	// C. 包装并生成执行代理闭包
	// 第一个参数 "user-getUser-api" 必须与 Admin 控制台上配置的迁移任务 key 保持一致
	executeFn := client.Wrap(
	    "user-getUser-api", 
	    targetOld, 
	    targetNew, 
	    targetFallback, 
	    userParamHandler,
	)

	// D. 业务在实际需获取数据的地方调用代理执行方法
	// 假设我们需要查询 userId = "1001" 的用户
	res, err := executeFn.Execute("1001")
	if err != nil {
		log.Printf("Execute failed: %v", err)
	} else {
		user := res.(*User)
		log.Printf("Execute success, user: %s", user.Name)
	}
}
```

---

## 📝 总结必读

无论 Java 或是 Go 的接入：
1. **任务 Key 一致性**：代码中的 `key` (如 `"user-getUser-api"`) 必须与管理后台新建任务时的任务名/迁移标识严格保持一致。
2. **入参要求**：参数提取器（`ParamHandler`）所需要的入参，取决于代码层面如何调用目标方法。务必确保能从中提取到正确的业务属性（例如 `userId` 或 `tenantId`）供平台下发的灰度策略进行计算。
3. **错误吞噬防范**：建议实现 Fallback/降级逻辑，在极端场景（如新旧接口同时 Panic/Exception 且网络组件不可用）兜底报错，防止上层链路雪崩。
