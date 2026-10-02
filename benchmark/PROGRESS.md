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

- Current Task: `Phase 00 — 音频标签技术验证与工程基线`
- Common Baseline Commit: `5fc560cd07d9ed22074ace3ab20d77e56b7fa317`
- Task SHA-256: `4355c3090b1014e41ca57fbe01bb0a6d4dbe9dad83e4ef2c0c33639d6d60fbb7`
- Codex Status: `PASSED` — objective score `96/100`
- Claude Status: `PASSED` — objective score `84/100`
- Last Verified At: `2026-10-02 (Asia/Shanghai)`

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
- 七个维度的 1–5 原始分：`NOT PROVIDED`，主控不代填。

## M710q 部署预检

- Verified At: `2026-10-02 (Asia/Shanghai)`
- Host: `192.168.2.2`
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
