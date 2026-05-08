# Melodify 项目说明（初始化与测试）

本目录为**工程级文档**入口；数据库建表与种子数据的**唯一权威脚本**为同目录下的 [`init.sql`](./init.sql)。

## 数据库

1. 创建数据库（名称需与后端配置一致，默认 `melodify`）：

   ```sql
   CREATE DATABASE melodify CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. 在 MySQL 客户端中执行：

   ```text
   source path/to/docs/init.sql
   ```

   或在图形化工具中直接运行 `init.sql` 全文。脚本包含表结构、基础角色、示例积分商品等。

3. 若从旧库升级，脚本末尾有可选的 `ALTER` / `UPDATE` 备忘，按需单独执行。

后端默认连接串见 `melodify-backend/src/main/resources/application.yml` 中的 `spring.datasource`，本地请改为你的账号密码（或通过已加入 `.gitignore` 的 `application-local.yml` 覆盖）。

## 后端 `melodify-backend`

- **JDK**：与 `pom.xml` 一致（当前为 Java 17）。
- **本地机密/联调**：将 `src/main/resources/application-local.example.yml` 复制为同目录 `application-local.yml`（已被忽略提交），按需填写 Suno 等项；也可用环境变量覆盖，例如：
  - `SUNO_API_KEY`：Suno API 密钥（`provider=auto` 且存在密钥时会走远端生成）。
  - `MELODIFY_SIMULATED_PAY_HMAC_SECRET`：模拟支付回调验签密钥（需与发起支付侧一致）。
- **运行**：`mvn spring-boot:run`（或 IDE 运行 `MelodifyBackendApplication`）。

## 前端

- **用户端** `melodify-client`：`npm ci` / `npm install` 后 `npm run dev`。
- **管理端** `melodify-admin`：同上。

具体技术栈见各子项目下的 `package.json` 与各自 `README.md`。

## 自动化测试（后端）

在 `melodify-backend` 目录执行：

```bash
mvn test
```

说明：

- **单元 / 轻量测试**（不连真实 MySQL）：例如 JWT 签发解析、`PublicExploreController` 的 `MockMvc` 独立测试，不依赖 Docker，**始终执行**。
- **集成测试**（`MySQL` + `docs/init.sql`）：使用 [Testcontainers](https://java.testcontainers.org/) 启动 MySQL 8，并在容器内执行与 `docs/init.sql` **同一份**脚本（构建时由 Maven 将仓库根目录 `docs/init.sql` 拷入测试 classpath 的 `db/init.sql`，**无需在仓库里维护第二份 DDL**）。
- **未安装 Docker 或未启动 Docker 守护进程**时，上述集成测试将**跳过**（`@Testcontainers(disabledWithoutDocker = true)`），`mvn test` 仍应 **BUILD SUCCESS**，以保证在无容器环境也能通过 CI 或本地检查。
