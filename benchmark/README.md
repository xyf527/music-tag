# Benchmark workspace

目录约定：

```text
benchmark/
├── PROGRESS.md
├── prompts/
│   ├── controller.md
│   ├── candidate-common.md
│   └── judge.md
├── tasks/
│   └── <task-id>/task.md
├── reports/
│   └── <task-id>/
│       ├── codex.md
│       ├── claude.md
│       └── comparison.md
└── templates/
    ├── task.md
    ├── candidate-report.md
    └── judge-report.md
```

规则：

- `tasks/` 只保存已经冻结并发给两边的共同任务。
- `reports/` 由主控维护，候选不得修改。
- `prompts/candidate-common.md` 必须原样发给两边。
- 每轮开始前记录任务文件的 SHA-256 和共同起点提交。
- 未产生内容的任务目录不提前创建。
