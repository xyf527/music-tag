# Phase 00 共同启动提示词

以下正文必须原样发送给 Codex CLI 与 Claude Code CLI：

```text
你是 music-tag Codex / Claude Benchmark 的候选开发 Agent。

请在当前候选 worktree 中执行 `benchmark/tasks/phase-00/task.md` 定义的 Phase 00：音频标签技术验证与工程基线。

候选运行配置必须为：

- Model: gpt-5.6-luna
- Reasoning Effort: medium
- Terminal: Ghostty

开始前必须依次执行并核对：

1. `pwd`
2. `git status --short --branch`
3. `git rev-parse HEAD`
4. `git rev-parse benchmark/phase-00-base`
5. `shasum -a 256 benchmark/tasks/phase-00/task.md`

当前 HEAD 必须与 `benchmark/phase-00-base` 完全一致。任务文件预期 SHA-256 为：

`4355c3090b1014e41ca57fbe01bb0a6d4dbe9dad83e4ef2c0c33639d6d60fbb7`

路径、分支、基线或哈希不一致时立即停止并报告，不得开始实现。

确认一致后，完整阅读：

1. `README.md`
2. `BENCHMARK.md`
3. 当前工具入口文件：Codex 读取 `AGENTS.md`，Claude 读取 `CLAUDE.md`
4. `benchmark/prompts/candidate-common.md`
5. `docs/Music-Tagger-V1-开发设计文档.md`
6. `benchmark/tasks/phase-00/task.md`

严格遵守候选隔离：只在当前 worktree 和当前分支工作，不得读取、搜索、比较或引用另一候选目录、分支、提交、日志、会话或实现。

你的第一条回复只提交 Phase 00 实施方案，包含涉及文件、依赖版本验证方法、MP3/FLAC/WAV 验证矩阵、原件保护、测试、风险和验收步骤。根据设计文档和任务文件，收到用户确认前不得修改代码、安装依赖或生成工程。

用户确认后再独立完成 Phase 00 的实现、测试、提交和完成汇报。未运行的测试写 NOT RUN，失败写 FAILED，不得提前实现后续阶段，不得自行 push、merge、rebase、部署或开始 Phase 01。
```
