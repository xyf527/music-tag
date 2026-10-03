# Phase 01 共同启动提示词

你是 music-tag Codex / Claude Benchmark 的候选开发 Agent。

请在当前候选 worktree 中执行 `benchmark/tasks/phase-01/task.md` 定义的 Phase 01：单曲编辑最小闭环。

固定运行配置：

- Model: `gpt-5.6-luna`
- Reasoning Effort: `medium`
- Terminal: Ghostty

开始前依次核对：

```sh
pwd
git status --short --branch
git rev-parse HEAD
git branch --show-current
shasum -a 256 benchmark/tasks/phase-01/task.md
```

任务文件预期 SHA-256：

```text
497ab6a22e67dc6b701c8121a81c4122dfa8d5f02f3b98c9380fba141c2f093f
```

当前分支必须为 `agent/codex` 或 `agent/claude`，工作区必须干净。路径、分支或任务哈希不一致时停止并报告。

完整阅读：

1. `README.md`
2. `BENCHMARK.md`
3. 当前工具入口文件：Codex 读取 `AGENTS.md`，Claude 读取 `CLAUDE.md`
4. `benchmark/prompts/candidate-common.md`
5. `docs/Music-Tagger-V1-开发设计文档.md`
6. `benchmark/tasks/phase-01/task.md`

严格遵守候选隔离，只读取当前 worktree 和当前分支。不得读取、搜索、比较或引用另一候选目录、分支、提交、日志、报告、会话或实现。

本指令已经是明确实施授权。核对完成后直接独立实现、测试并提交 Phase 01，不要再次等待“确认方案”，也不要因常规实现选择询问用户。只有文档存在无法自行解决的实质冲突或外部条件完全阻塞时才提问。

优先交付真实可运行的单曲闭环和可人工检查的处理后文件。不得用一个主要业务类承载全部 Web、业务、音频、文件和数据库职责；不得因追求类数量创建无行为的空壳类。

所有项目自有 Java 主代码和测试代码必须迁移到 `com.xin.musictag`。保持 `src/test/` 纳入 Git，只忽略测试生成物。提交前扫描并清除真实 IP、主机名、密码、Token、Key、连接串、SSH 材料和个人路径，统一改用环境变量或明显无效的占位符。

最终回复先给不超过 8 行的摘要，再按候选报告模板提供证据。未运行写 `NOT RUN`，失败写 `FAILED`。完成提交后停止，不自行 push、部署、合并、rebase 或开始 Phase 02。
