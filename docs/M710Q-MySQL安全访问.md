# M710q MySQL 开发与凭据方案

## 固定规则

自 2026-10-03 起，本项目的开发、建库、Flyway、集成测试和部署运行统一使用 M710q 上已有的 MySQL 8。

- Mac 不启动本地 MySQL、Docker MySQL 或 Testcontainers MySQL。
- Codex 与 Claude 可以共用同一个 MySQL 服务实例，但不能共用 database/schema。
- 每个候选分别使用开发库和测试库，测试不得清空开发库。
- 真实主机、端口、密码和连接串不进入 Git、聊天、提示词、IDEA 工程配置或 Shell 历史。

| Candidate | 开发库 | 测试库 | 建议用户 |
|---|---|---|---|
| Codex | `music_tag_codex` | `music_tag_codex_test` | `music_tag_codex_user` |
| Claude | `music_tag_claude` | `music_tag_claude_test` | `music_tag_claude_user` |

两个用户只授予各自两个库所需的权限。不得授予全局管理权限，也不得允许一个候选访问另一候选的库。创建 database 和用户属于一次性管理操作，应在 M710q 上由用户使用管理员身份完成；Agent 只使用候选专用账号执行本候选 Flyway 和业务访问。

## 当前采用直接连接与 macOS 钥匙串

当前按用户决定，在可信局域网内由 Mac 直接访问 M710q 的 MySQL 监听端口，不要求启动 SSH 隧道。真实地址和端口只写入每个 worktree 的 `/.music-tag-local/mysql.conf`；该目录被 Git 忽略，不进入提交。

数据库密码只录入一次并保存在 macOS 登录钥匙串。仓库脚本在启动 Maven 或应用时读取密码并只注入该子进程，不创建 `.env` 文件，也不在命令行中出现密码。

任何能够运行数据库客户端或应用的进程，运行期间都必然能够使用该账号权限。因此仍需依靠候选独立账号、独立库和最小权限限制风险。Agent 不得执行 `env`、`printenv`、调试转储或任何会输出密码的命令。

## 一次性准备

### 1. 保存本机直连地址

在每个 worktree 中运行：

```sh
./scripts/configure-mysql-direct.sh
```

按提示输入真实地址、端口、账号和 SSL 模式。配置保存在 Git 忽略目录中。若 MySQL 不支持 TLS，可由用户明确把 SSL 模式改为 `PREFERRED`；优先使用 `REQUIRED`。

### 2. 在 M710q 创建独立库和最小权限用户

在 M710q 上使用 MySQL 管理员完成四个库和两个用户的创建。密码使用密码管理器生成的随机强密码。字符集使用 `utf8mb4`。

Codex 用户只获得：

- `music_tag_codex.*`
- `music_tag_codex_test.*`

Claude 用户只获得：

- `music_tag_claude.*`
- `music_tag_claude_test.*`

建库和授权完成后退出管理员会话。管理员密码不交给 Agent。

### 3. 将候选密码录入 macOS 钥匙串

在各自 worktree 中运行一次：

```sh
./scripts/store-mysql-password-in-keychain.sh codex
./scripts/store-mysql-password-in-keychain.sh claude
```

脚本会由系统安全工具交互式提示密码。输入不会写入 Shell 历史。不要把密码作为脚本参数，也不要在聊天中发送密码。

### 4. 可选的 SSH 隧道

若以后不再允许 MySQL 局域网直连，可以删除本地 direct 配置并改用 `scripts/open-m710q-mysql-tunnel.sh`。当前开发流程不要求启动隧道。

## Agent 日常使用

候选不需要知道密码，只运行包装脚本。例如：

```sh
./scripts/with-m710q-mysql.sh codex test -- mvn -Dtest=MySqlPhase01IntegrationTest test
./scripts/with-m710q-mysql.sh claude dev -- mvn spring-boot:run
```

包装脚本会读取本机忽略的 endpoint，随后设置通用 `MYSQL_*`、Spring datasource 和现有 Phase 01 集成测试变量。连接串包含 `createDatabaseIfNotExist=true`，当前账号具备权限时可以建立本候选 database。命令结束后密码不会保存到仓库文件。

## 安全检查

- Git 中不得出现 `.env`、真实地址、真实端口、密码或完整连接串。
- 日志和测试报告不得打印 datasource URL、密码或完整环境变量。
- 运行迁移前核对 database 名称必须属于当前候选。
- 测试清理只允许作用于当前候选的 `_test` 库。
- 直连端点和数据库失败时明确停止，不得回退到 H2、内存仓库或本地容器并伪装通过。
- 部署到 M710q 后，生产运行密码保存在服务器权限受限的环境文件或 Secret 中，由部署进程读取，不从 Mac 钥匙串复制进 Git 或部署包。
