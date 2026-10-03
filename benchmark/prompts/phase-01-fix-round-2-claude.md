# Phase 01 Fix Round 2 — Claude

只在当前 `agent/claude` worktree 中完成 Phase 01 第二轮修复。不得读取 Codex 候选的目录、分支、提交、报告、会话或实现。

固定配置：

- Model：`gpt-5.6-luna`
- Reasoning Effort：`medium`

本指令构成明确实施授权。核对分支、工作区和当前 HEAD 后直接实施，不等待方案确认。

## 数据库新规则

自本轮起，所有数据库访问、Flyway 和集成测试必须使用 M710q 既有 MySQL 8。禁止在 Mac 启动本地 MySQL、Docker MySQL 或 Testcontainers MySQL。本规则覆盖 Phase 01 历史任务文件中关于隔离本地实例的旧描述。

先阅读 `docs/M710Q-MySQL安全访问.md`。用户已经把直连 endpoint 与凭据写入 Git 忽略、权限受限的本机配置。只能通过以下形式运行测试：

```sh
./scripts/with-m710q-mysql.sh claude test -- <测试命令>
```

不得在回复、日志、报告或 Git 中显示或记录密码，不得运行 `env`、`printenv` 或输出完整 datasource 配置。直连配置未准备好时报告 `BLOCKED` 并停止，不得回退到 H2、内存仓库或本地容器冒充 MySQL 证据。若 Claude Code 请求该包装脚本的 Bash 权限，使用项目本地的精确 allow 规则授权该脚本；不得使用 `--dangerously-skip-permissions`。

## 必须完成

1. MySQL 必须成为唯一运行时事实来源。不得通过默认 `!mysql` profile 回退到内存 Repository。实现资源、任务和版本所需的保存与读取，`find` 不得固定返回空结果。
2. Flyway 表结构、Repository SQL、Java 类型和实际业务流程必须一致。使用 Claude 的 M710q 测试库完成真实集成测试，只能清理 `music_tag_claude_test`。
3. 完成浏览器上传、现有标签展示、标题/歌手/专辑编辑、歌词和封面 `KEEP / SET / REMOVE`、变更预览、执行、状态、成品下载和 JSON 报告下载。
4. Controller 不得接收客户端提交的服务器 `Path` 或完整 `AudioResource` 作为可信输入；客户端只提交资源 ID、版本 ID 和编辑 DTO，服务端从数据库加载受控记录。
5. 所有下载通过不透明 ID，规范化路径后确认位于配置根目录，拒绝 `..`、越界数据库路径和符号链接逃逸。任何响应和报告不得泄露绝对路径。
6. MP3 / FLAC 必须由应用真实改写、写后重读并验证歌词、封面和文本三态；WAV 不支持操作在预览阶段明确拒绝。
7. 验证原件 SHA-256 不变、处理后哈希变化、连续编辑产生父子版本、数据库状态和 JSON 报告一致。
8. 自动测试覆盖完整闭环、危险文件名、空文件、伪造格式、越界下载和路径脱敏。一个 WAV 拒绝测试不能作为完整证据。
9. 实际运行人工验收脚本，在仓库外保留真实处理后 MP3、FLAC、原件哈希和应用生成的 JSON 报告；不得复制原件或手写成功报告冒充处理结果。

## 验收与停止

- Java 17 下运行 `mvn clean verify` 和 `mvn package`。
- 通过安全包装脚本运行 M710q MySQL 集成测试，并完成真实浏览器闭环。
- 运行 `git diff --check`、Secret、绝对路径、媒体文件和私有配置扫描。
- 更新 `PHASE-01-CANDIDATE-REPORT.md`；未运行写 `NOT RUN`，失败写 `FAILED`，不得把 `PARTIAL` 描述为完成。
- 提交当前分支并保持工作区干净后停止。不要 push、部署、合并、rebase 或开始 Phase 02。
