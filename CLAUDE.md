# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Backend API Migration Platform** (后端接口迁移平台) - a system for gradual API migration with gray validation and parallel diff comparison. It enables safe, controlled migration from old APIs to new APIs through 7 progressive stages.

## Build Commands

```bash
# Compile the project
mvn clean compile

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Run a single test method
mvn test -Dtest=ClassName#methodName

# Package (build JARs)
mvn package

# Install to local Maven repository
mvn install

# Skip tests
mvn clean compile -DskipTests
```

## Architecture

### Module Structure
```
migration/
├── migration-sdk/              # SDK modules
│   ├── migration-sdk-core/    # Java SDK core
│   ├── migration-spring-boot-starter/  # Spring Boot Starter
│   └── migration-go/          # Go SDK
├── migration-admin/           # Admin backend
│   └── migration-admin-api/   # REST API service
├── migration-diff/            # Diff comparison service
└── pom.xml                    # Parent POM
```

### Migration Stages (7 States)
| Stage | Code | Behavior |
|-------|------|----------|
| OLD | 1 | Call old API only |
| VALIDATION_GRAY | 2 | Call both, diff enabled, return old |
| VALIDATION_ALL | 3 | Call both, diff enabled, return old |
| GO_LIVE_GRAY | 4 | Call both, diff enabled, return by gray rule |
| GO_LIVE_ALL | 5 | Call both, diff enabled, return new |
| DECOMMISSIONING_GRAY | 6 | Gray hit: new only; Miss: call both, diff, return new |
| DECOMMISSIONING_ALL | 7 | Call new API only |

### Data Flow
1. SDK pulls migration config from config center (or via API)
2. Based on current stage, SDK decides: call old, new, or both
3. If both called, SDK sends diff request to Diff service
4. Diff service compares responses using rules (IGNORE/TOLERANCE/SCRIPT/SORT)
5. SDK returns result based on stage rules

### Key Configuration Keys (in Config Center)
- `migration_{key}` - Migration task config
- `gray_{key}` - Gray rules (PERCENTAGE/BLACKLIST/WHITELIST/EXPRESSION)
- `diff_{key}` - Diff rules (IGNORE/TOLERANCE/SCRIPT/SORT)

## Development Standards

### Naming Conventions
- **JSON fields**: snake_case (e.g., `migration_key`, `has_diff`)
- **Enums**: UPPER_SNAKE_CASE (e.g., `PERCENTAGE`, `IGNORE`, `OLD`)
- **Java**: Follow Google Java Style, use Lombok

### API Style
- Non-RESTful; URL includes operation type
- Use POST for mutations, GET for queries
- Examples: `/api/v1/migration_task/create`, `/api/v1/diff`

### Response Format
```json
{
    "code": 0,
    "message": "success",
    "data": {}
}
```

### Gray Rule Types
- `PERCENTAGE`: Percentage-based (e.g., "30" = 30% traffic to new)
- `BLACKLIST`: Array of IDs to exclude (e.g., `["1001","1002"]`)
- `WHITELIST`: Array of IDs to include
- `EXPRESSION`: Custom expression (SpEL/Groovy)

### Diff Rule Types
- `IGNORE`: Skip comparison for field
- `TOLERANCE`: Allow numeric tolerance (e.g., "0.01")
- `SCRIPT`: Custom SpEL expression with `#oldValue/#newValue/#fieldPath/#diffType` variables
- `SORT`: Sort arrays by configured element field before diff

## Key Files

- `docs/架构设计文档.md` - Architecture design document
- `docs/需求分析文档.md` - Requirements specification
- `docs/开发规范.md` - Development standards and conventions

## Java SDK Usage

```java
// Create config
MigrationConfig config = new MigrationConfig();
config.setMigrationKey("user-getUser-api");

// Create client
MigrationClient client = new MigrationClient(config);

// Wrap methods
ExecuteFunction<User> execute = client.wrap(
    this::getUserOld,      // old method
    this::getUserNew,      // new method
    this::getUserFallback, // fallback (optional)
    paramHandler           // builds gray params
);

// Execute
User user = execute.apply("1001", 5);
```

## Spring Boot Starter Usage

```java
@EnableMigration
@SpringBootApplication
public class Application { }

// Use @Migration annotation
@Migration(
    key = "user-getUser-api",
    oldMethod = "getUserOld",
    newMethod = "getUserNew",
    paramHandler = UserParamHandler.class
)
@GetMapping("/user/{id}")
public User getUser(...) { return null; }
```

