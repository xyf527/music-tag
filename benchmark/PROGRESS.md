# Benchmark Progress

## 固定配置

- Controller: GPT-5.6 Sol
- Controller Reasoning Effort: medium
- Controller Chat: `music-tag｜项目经理・裁判・Benchmark 主控`
- Codex Worktree: `/Users/xyf/IdeaProjects/music-tag-codex`
- Claude Worktree: `/Users/xyf/IdeaProjects/music-tag-claude`
- Candidate Model: `gpt-5.6-luna`
- Candidate Reasoning Effort: `medium`
- Candidate Harnesses: Codex CLI / Claude Code CLI
- Terminal: Ghostty

## 当前状态

- Current Task: `Phase 01 — 单曲编辑最小闭环`
- Phase 01 Task SHA-256: `497ab6a22e67dc6b701c8121a81c4122dfa8d5f02f3b98c9380fba141c2f093f`
- Codex Phase 01 Baseline: `f555501` / tag `benchmark/phase-01-codex-base`（上一实现 HEAD `ef0d5de`）
- Claude Phase 01 Baseline: `b7c5018` / tag `benchmark/phase-01-claude-base`（上一实现 HEAD `862fcc3`）
- Codex Status: `NOT_STARTED`
- Claude Status: `NOT_STARTED`
- Last Updated At: `2026-10-03 (Asia/Shanghai)`

### Phase 00 最终验收

#### Codex

- Candidate HEAD: `ef0d5de`
- Working Tree: `CLEAN`（忽略的 Maven / IDEA 本地产物不在提交中）
- Judge Maven Verify: `PASSED`（外部合成 MP3 / FLAC / WAV，核心测试 2，人工产物测试 1，均无失败或跳过）
- 原件 SHA-256 与解码 PCM 对比：`VERIFIED BY AUTOMATED TEST`
- 复现文档与保留产物：`PASSED`
- 播放器人工验收：`NOT RUN`
- Completion Report 模板：`PRESENT`

#### Claude

- Candidate HEAD: `862fcc3`
- Working Tree: `CLEAN`（忽略的 Maven / IDEA 本地产物不在提交中）
- Judge Maven Test: `PASSED`（外部合成 MP3 / FLAC / WAV，2 tests，0 failures，0 skipped）
- MP3 / FLAC 标签、封面、歌词与原件 SHA-256：`VERIFIED BY AUTOMATED TEST`
- WAV：`VERIFIED READ / WRITE UNSUPPORTED BY SELECTED LIBRARY`
- 解码 PCM：`NOT RUN`
- 缺失 fixture：`ASSERTION FAILURE`，不再假通过
- 播放器人工验收：`NOT RUN`
- Completion Report 模板：`PRESENT IN CLI SESSION`

- Phase 00 Objective Winner: `CODEX`
- Phase 00 User Experience Winner: `CLAUDE`
- Phase 00 Overall User Choice: `CLAUDE`
- Next Gate: 发布 Phase 01 前保留播放器人工观察项，并由主控冻结下一阶段共同任务。

## 用户主观体验记录

- 用户最终体验票：`CLAUDE`
- 主要原因：提问更少、交互阻力更低，用户感知的自主调试体验更好。
- Claude 改进点：最终汇报偏长，缺少明显摘要。
- Codex 优点：测试、复现和验证闭环更完整。
- Codex 改进点：确认与权限打断影响使用体验。
- 用户主观总分：Codex `25/40`，Claude `26/40`。
- 最终主观投票：`CLAUDE`。
- Claude 持续风险：除启动类外当前只有一个主要实现类，且没有稳定导出处理后文件供人工检查；项目最终仍缺核心功能或维持单类堆积时判定 `FAILED / DISQUALIFIED`。

## M710q 部署预检

- Verified At: `2026-10-02 (Asia/Shanghai)`
- Host: `${M710Q_HOST}`（真实地址不进入 Git）
- `/home/xyf/deploy/music-tag/music-tag-codex`: `EXISTS`
- `/home/xyf/deploy/music-tag/music-tag-claude`: `EXISTS`
- Host Port `18081`: `AVAILABLE`（检查时无监听）
- Host Port `18082`: `AVAILABLE`（检查时无监听）
- MySQL Host Port `3307`: `LISTENING`
- 注：端口状态可能变化，每次实际部署前仍须重新检查。

## 历史

| Task | Codex | Claude | Comparison | Finalized At |
|---|---|---|---|---|
| Phase 00 | PASSED, 96 | PASSED, 84 | Technical: Codex; UX and user choice: Claude | 2026-10-02 |
