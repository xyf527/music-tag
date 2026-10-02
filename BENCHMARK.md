# music-tag Codex / Claude Benchmark

## 1. 目标

在开发同一个 `music-tag` 工具的过程中，对比 Codex CLI 与 Claude Code CLI 在相同候选模型下的工程质量和协作体验。

Benchmark 同时记录：

- GPT-5.6 Sol 主控基于可复现证据给出的客观评判。
- 用户在 Ghostty 中实际使用两套 CLI 的主观开发体验。

两类结果分别呈现。主观体验不能伪装成自动测试结论，客观分数也不能代替用户体验。

## 2. 固定角色

| 角色 | 工具 | 分支 | 工作目录 |
|---|---|---|---|
| 主控 / Judge | GPT-5.6 Sol medium | `main` | `/Users/xyf/IdeaProjects/music-tag` |
| Candidate A | Codex CLI | `agent/codex` | `/Users/xyf/IdeaProjects/music-tag-codex` |
| Candidate B | Claude Code CLI | `agent/claude` | `/Users/xyf/IdeaProjects/music-tag-claude` |

候选模型名称与推理等级记录在 `benchmark/PROGRESS.md`。每一轮两边必须完全一致；未核实则写 `NOT VERIFIED`。

## 3. 公平性要求

每轮必须满足：

- 同一个起始提交。
- 同一个任务文件，字节级一致并记录 SHA-256。
- 同一个候选模型、推理等级和上下文规则。
- 相同的时间口径、依赖条件、测试环境和人工帮助等级。
- 相同的公开验收标准和最多修复轮数。
- 不允许交叉读取两个候选 worktree。

若任务分发错误、文件缺失或哈希不一致，该次运行作废，不计为候选失败。

## 4. 分支规则

- `main` 保存共同基线、公共任务、Judge 产物和最终报告。
- `agent/codex` 只接受 Codex CLI 的产品实现提交。
- `agent/claude` 只接受 Claude Code CLI 的产品实现提交。
- 每个任务开始前记录 `before_commit`，完成后记录 `after_commit`。
- 候选不得自行 merge、rebase、cherry-pick、push 或查看另一候选分支内容，除非用户结束 Benchmark 并明确授权。
- 用户创建 GitHub 仓库后，由主控在获得明确指令时配置 remote 和推送。

## 5. 任务与提示词

- 每轮唯一需求文件：`benchmark/tasks/<task-id>/task.md`
- 两边共享提示词：`benchmark/prompts/candidate-common.md`
- 候选最终汇报使用：`benchmark/templates/candidate-report.md`
- Judge 报告使用：`benchmark/templates/judge-report.md`

任务文件必须明确允许范围、禁止范围、完成标准、公开验证方式和停止条件。任何只给某一候选的额外实现建议都算人工介入。

## 6. 人工介入等级

| 等级 | 含义 |
|---:|---|
| 0 | 无人工帮助 |
| 1 | 回答候选主动提出的需求澄清 |
| 2 | 返回候选可自行看到的失败命令和错误输出 |
| 3 | 指出问题所在文件或模块 |
| 4 | 提供具体修复方案或代码方向 |
| 5 | 人类或主控直接修改候选实现 |

每次介入必须记录对象、时间、等级和内容。等级 5 的任务不能作为纯候选能力结果。

## 7. 客观验收

每轮开始前在任务文件中冻结具体权重。默认维度如下：

| 维度 | 默认权重 |
|---|---:|
| 功能与需求正确性 | 35 |
| 自动测试与回归质量 | 20 |
| 代码结构与可维护性 | 15 |
| 可靠性、安全性与边界处理 | 10 |
| 范围纪律与复杂度控制 | 10 |
| 文档、可运行性与交付真实性 | 10 |

客观验收至少区分：候选自测、主控复现、Judge 检查和人工操作验收。没有运行的项目必须写 `NOT RUN`。

以下情况单独记录，必要时判定本轮无效或失败：

- 修改、删除或绕过 Judge 测试。
- 伪造测试、运行时间、模型身份或完成状态。
- 读取另一候选实现。
- 提交 Secret、个人凭据或不应入库的数据。
- 明显超出任务范围，导致比较失真。

## 8. 用户主观体验

用户分别按 1–5 分记录：

1. 首次理解需求的准确度。
2. 提问是否必要且清楚。
3. 权限请求和中断是否合理。
4. 自主调试与恢复能力。
5. 过程透明度和日志可读性。
6. 完成汇报是否真实、易核验。
7. 整体协作顺畅度。

主观体验单独报告原始分与备注，不由主控代填，也不倒推修改客观证据。

## 9. 修复与停止

- 默认最多三轮候选自修复。
- 两边获得同等级反馈，但只接收各自实际错误。
- 达到上限后记录 `PARTIAL` 或 `FAILED`，不得无限修复到分数失去意义。
- 当前任务未形成最终报告前，不启动下一项对比任务。

## 10. 状态

允许状态：`NOT_STARTED`、`IN_PROGRESS`、`READY_FOR_JUDGE`、`FIX_ROUND_1`、`FIX_ROUND_2`、`FIX_ROUND_3`、`PASSED`、`PARTIAL`、`FAILED`、`BLOCKED`。

候选声明完成只能进入 `READY_FOR_JUDGE`，主控实际验收通过后才可进入 `PASSED`。
