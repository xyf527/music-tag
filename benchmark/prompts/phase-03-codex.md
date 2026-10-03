# Codex Phase 03 执行提示词

读取并严格执行 `benchmark/tasks/phase-03/task.md`，完成 Phase 03。

固定配置：

- Model：`gpt-6.1-sol`
- Reasoning Effort：`low`
- Branch：`agent/codex`
- Baseline：`benchmark/phase-03-codex-base`
- Task SHA-256：`eeec442304680e11be37ac0be64a02b78680c3e9cb7eda52da71fde2617d5d94`

本指令已经构成完整实施授权。完成路径、分支、HEAD、基线 tag 核对，并执行 `shasum -a 256 benchmark/tasks/phase-03/task.md` 确认结果与上述值一致后直接开发，不要输出方案或等待确认。当前 HEAD 会比基线多一个只包含 Phase 03 任务和提示词的主控交接提交，这是预期状态。

Claude 已淘汰。不得读取、搜索、比较、引用或复制 Claude 的任何开发材料。

当前未提交的私密 `src/main/resources/application.yml` 必须原样保留，不得暂存、覆盖、还原、提交或输出其中的信息。

外部 Docker、M710q 或 MinIO 无法访问时，继续完成全部实现、自动测试、部署资源和文档，仅把对应真实环境验收写为 `PENDING USER EXECUTION`，不得提前停止或以 `PARTIAL` 代替仍可完成的工作。

完成实现、测试、打包、部署资源、人工验收说明和 Git 提交后停止。不要自行 push、部署、合并、rebase 或开始 Phase 04。
