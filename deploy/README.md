# 服务迁移平台 - 部署指南

本目录包含用于部署“服务迁移平台”的 Docker Compose 配置文件。

## 前置条件

以下组件必须提前部署，并且能够被 Docker 网络访问到：
1. **Nacos** (注册中心与配置中心)
2. **MySQL** (数据库)

## 配置修改

你需要在 `deploy/.env` 中配置以下环境变量，确切指向您已部署的 Nacos 和 MySQL 实例：

- `HOST_IP`: Docker 所在宿主机的真实 IP，例如 `192.168.1.100`
- `MIGRATION_NACOS_SERVER_ADDR`: 例如 `host.docker.internal:8848`
- `MIGRATION_NACOS_NAMESPACE`: 例如 `migration`
- `MIGRATION_NACOS_USERNAME`: 例如 `nacos`
- `MIGRATION_NACOS_PASSWORD`: 例如 `nacos` 
- `MIGRATION_DB_URL`: MySQL 的 JDBC URL 连接串，例如 `jdbc:mysql://host.docker.internal:3306/migration?...`
- `MIGRATION_DB_USERNAME`: MySQL 用户名
- `MIGRATION_DB_PASSWORD`: MySQL 密码

> 当前部署方式会通过 `extra_hosts` 将 `host.docker.internal` 映射到 `HOST_IP`。如果宿主机 IP 发生变化，请同步更新 `deploy/.env` 并重建容器。

## 构建与启动

1. **编译打包项目**：
   在启动服务之前，您必须先在项目根目录构建好所有后端服务的 JAR 包和前端：
   ```bash
   mvn clean package -DskipTests
   ```

2. **使用 Docker Compose 启动服务**：
   在 `deploy` 目录下执行构建与启动命令：
   ```bash
   docker-compose up -d --build
   ```

## Accessing the Platform

- **Admin UI 管理后台前端**: http://localhost
- **Admin API 接口文档**: http://localhost:8080/doc.html (Knife4j)
- **Diff API 接口文档**:  http://localhost:8081/doc.html (Knife4j)

## 环境变量配置

请复制 `.env.example` 文件并重命名为 `.env`，然后在其中填入实际的 Nacos 和 MySQL 连接信息。`.env` 包含敏感信息，已在 `.gitignore` 中配置忽略，不会提交到代码仓库中。

## 停止服务

当需要停止服务时，请在 `deploy` 目录下执行：
```bash
docker-compose down
```
