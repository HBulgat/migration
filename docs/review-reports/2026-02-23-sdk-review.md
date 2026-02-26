# 2026-02-23 SDK Review Report

## Scope
- Modules: `migration-sdk-core`, `migration-spring-boot-starter`
- Reference docs:
  - `docs/migration-sdk-core??.md`
  - `docs/migration-spring-boot-starter??.md`
  - `docs/????.md`

## Result
This review found **3 issues** (High: 1, Medium: 1, Low: 1).

## Findings (ordered by severity)

### [High] Duplicate old-method invocation in fallback path for GO_LIVE_ALL / DECOMMISSIONING_GRAY
- Locations:
  - `migration-sdk/migration-sdk-core/src/main/java/top/bulgat/migration/sdk/core/client/MigrationClient.java:157`
  - `migration-sdk/migration-sdk-core/src/main/java/top/bulgat/migration/sdk/core/strategy/GoLiveAllStrategy.java:36`
  - `migration-sdk/migration-sdk-core/src/main/java/top/bulgat/migration/sdk/core/strategy/DecommissioningGrayStrategy.java:46`
- Problem:
  - Default fallback is `oldMethod` (`safeFallback`).
  - In GO_LIVE_ALL / DECOMMISSIONING_GRAY (miss path), old method is already invoked once in concurrent execution.
  - If new method fails, fallback calls old method again, which can cause duplicated side effects.
- Evidence:
  - `migration-sdk/migration-spring-boot-starter/src/test/java/top/bulgat/migration/sdk/starter/aop/MigrationInterceptorTest.java:203`
- Recommendation:
  - Reuse already computed old result when available; call fallback only when old result is also unavailable/failed.

### [Medium] Fallback method resolution only scans public methods
- Location:
  - `migration-sdk/migration-spring-boot-starter/src/main/java/top/bulgat/migration/sdk/starter/aop/MigrationInterceptor.java:259`
- Problem:
  - Fallback lookup uses `targetClass.getMethods()` (public-only).
  - This blocks `private/protected` fallback methods with `(..., Exception/Throwable)` signature.
  - It is inconsistent with later invocation via `ReflectionUtils.makeAccessible`.
- Recommendation:
  - Use declared-method scanning (`getDeclaredMethods` + parent traversal, or `ReflectionUtils` helper), then invoke with `makeAccessible`.

### [Low] Comment language does not fully match development guideline
- Example locations:
  - `migration-sdk/migration-sdk-core/src/main/java/top/bulgat/migration/sdk/core/client/MigrationClient.java:29`
  - `migration-sdk/migration-spring-boot-starter/src/main/java/top/bulgat/migration/sdk/starter/aop/MigrationInterceptor.java:35`
- Problem:
  - `docs/????.md` says comments should preferably be in Chinese.
  - Current core/starter comments are mostly English.
- Recommendation:
  - Gradually switch to Chinese (or bilingual) comments once encoding handling is stable.

## Extra tests added in this round
- ParamHandler path (reflection-created handler):
  - `migration-sdk/migration-spring-boot-starter/src/test/java/top/bulgat/migration/sdk/starter/aop/MigrationInterceptorTest.java:57`
- Exception path (new fails without explicit fallback method):
  - `migration-sdk/migration-spring-boot-starter/src/test/java/top/bulgat/migration/sdk/starter/aop/MigrationInterceptorTest.java:180`
- Verification command:
  - `mvn -pl migration-sdk/migration-sdk-core,migration-sdk/migration-spring-boot-starter -am test -q`
  - Result: passed
