# music-tag

音乐标签工具项目。

本仓库同时用于产品开发和 Codex / Claude 同模型开发质量对比。

## 分支与工作区

- `agent/codex`
- `agent/claude`
- `main`：共同基线、Benchmark 规则、主控报告和最终确认内容

候选开发必须使用独立 Git worktree，不能在同一个目录轮流切换分支。

## 项目管理入口

- `PROJECT_CONTROLLER.md`：GPT-5.6 Sol 项目经理、裁判和 Benchmark 主控定位
- `BENCHMARK.md`：公平性、隔离、评分和验收规则
- `benchmark/PROGRESS.md`：当前进度与每轮状态
- `benchmark/prompts/`：主控、候选开发和裁判提示词
- `benchmark/templates/`：任务与报告模板

远程仓库将在 GitHub 仓库创建后配置。
