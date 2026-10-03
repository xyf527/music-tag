# Codex Phase 02 执行提示词

读取并严格执行 `benchmark/tasks/phase-02/task.md`，独立完成 Phase 02。

固定配置：

- Model：`gpt-5.6-luna`
- Reasoning Effort：`medium`
- Branch：`agent/codex`
- Baseline：`benchmark/phase-02-codex-base`
- Task SHA-256：`4553e4d680effe83c54dadf9af9a63065a52947f5e21c4f0cf195c304614a397`

本指令已经构成明确实施授权。完成路径、分支、HEAD、基线 tag 核对，并执行 `shasum -a 256 benchmark/tasks/phase-02/task.md` 确认结果与上述值一致后直接开发，不要再次等待方案确认。只有任务文件存在无法自行解决的实质冲突或外部条件完全阻塞时才询问。

当前 HEAD 会比基线 tag 多一个只包含 Phase 02 任务与提示词的主控交接提交，这是预期状态；功能实现差异以 `benchmark/phase-02-codex-base` 为起点统计，不要因此停止。

Claude 已淘汰。不得读取、搜索、比较、引用或复制 Claude 的目录、分支、提交、报告、会话和实现。

当前工作区允许存在用户未提交的私密 `src/main/resources/application.yml` 修改。必须原样保留，不得暂存、覆盖、还原、提交或在回复中输出其中的数据库信息。

必须使用新的 Flyway V2 迁移在现有 Phase 01 数据库上升级；不得删除或重建数据库，不得修改已发布的 V1。完成实现、全部测试、打包、安全扫描、人工验收产物生成和 Git 提交后停止。不要自行 push、部署、合并、rebase 或开始 Phase 03。
