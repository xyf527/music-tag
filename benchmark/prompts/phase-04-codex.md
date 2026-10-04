# Codex Phase 04 执行提示词

读取并严格执行 `benchmark/tasks/phase-04/task.md`，完成 V1 最后一阶段 Phase 04。

固定配置：

- Model：`gpt-6.1-sol`
- Reasoning Effort：`low`
- Branch：`agent/codex`
- Baseline：`benchmark/phase-04-codex-base`
- Task SHA-256：`a0ef802bddf466f69543d77eb84e00180b26c28bdc8c9426d2ea0da816f758f9`

本指令已经构成完整实施授权。完成路径、分支、HEAD、基线 tag 核对，并执行 `shasum -a 256 benchmark/tasks/phase-04/task.md` 确认结果与上述值一致后直接开发，不要输出方案或等待确认。当前 HEAD 会比基线多一个只包含 Phase 04 任务和提示词的主控交接提交，这是预期状态。

Claude 已淘汰。不得读取、搜索、比较、引用或复制 Claude 的任何开发材料。

当前未提交的私密 `src/main/resources/application.yml` 和用户本机构建脚本修改必须原样保留，不得暂存、覆盖、还原、提交或输出其中的信息。

真实验证决定支持范围。M4A、OGG、Opus 必须分别验证；不能实现或不能证明的能力应明确标记 `UNSUPPORTED`，不得伪造成功。外部 M710q、MySQL、MinIO 或播放器验收无法执行时，继续完成其余实现、自动测试、镜像、发布包和文档，仅如实标记 `PENDING USER EXECUTION`。

完成实现、测试、打包、候选报告和 Git 提交后停止。不要自行 push、部署、合并、rebase，最终 V1 裁决由项目经理完成。
