# Phase 01 Fix Round 2 — Codex

只在当前 `agent/codex` worktree 中完成 Phase 01 第二轮修复。不得读取 Claude 候选的目录、分支、提交、报告、会话或实现。

固定配置：

- Model：`gpt-5.6-luna`
- Reasoning Effort：`medium`

本指令构成明确实施授权。核对分支、工作区和当前 HEAD 后直接实施，不等待方案确认。

## 数据库新规则

自本轮起，所有数据库访问、Flyway 和集成测试必须使用 M710q 既有 MySQL 8。禁止在 Mac 启动本地 MySQL、Docker MySQL 或 Testcontainers MySQL。本规则覆盖 Phase 01 历史任务文件中关于隔离本地实例的旧描述。

先阅读 `docs/M710Q-MySQL安全访问.md`。用户已经把直连 endpoint 与凭据写入 Git 忽略、权限受限的本机配置。只能通过以下形式运行测试：

```sh
./scripts/with-m710q-mysql.sh codex test -- <测试命令>
```

不得在回复、日志、报告或 Git 中显示或记录密码，不得运行 `env`、`printenv` 或输出完整 datasource 配置。直连配置未准备好时报告 `BLOCKED` 并停止，不得回退到 H2、内存仓库或本地容器冒充 MySQL 证据。

## 必须修复

1. `/api/tasks/{id}` 不得直接返回含 `reportPath` 的领域对象。改用明确 DTO，只返回前端需要的标识、状态、阶段、错误码、成品版本和相对 API 链接。
2. 审计所有 JSON、错误响应、页面和报告，确保不泄露 `storagePath`、`coverPath`、`lyricsPath`、`outputPath`、`reportPath` 或其他服务器绝对路径。
3. 版本下载不能只校验文件名。读取前必须将路径规范化，并确认目标普通文件位于配置的 outputs 根目录之内；拒绝越界路径和符号链接逃逸。
4. 报告下载同样必须确认目标位于配置的 reports 根目录之内。
5. 增加自动测试：任务 DTO 不含绝对路径；恶意数据库路径、`..` 路径和符号链接不能越界下载；正常成品与报告仍可下载。
6. 使用 Codex 的 M710q 测试库运行 Flyway 和真实上传到报告的 MySQL 集成测试。测试只能清理 `music_tag_codex_test`，不得访问开发库或 Claude 数据库。

## 验收与停止

- Java 17 下运行完整普通测试和打包。
- 通过安全包装脚本运行 M710q MySQL 集成测试。
- 运行 `git diff --check`、Secret、绝对路径、媒体文件和私有配置扫描。
- 更新 `PHASE-01-CANDIDATE-REPORT.md`，如实写出 M710q 测试方式和结果，但不得写真实主机、端口、连接串或密码。
- 提交当前分支并保持工作区干净后停止。不要 push、部署、合并、rebase 或开始 Phase 02。
