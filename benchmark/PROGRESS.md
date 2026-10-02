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
- Codex Status: `FIX_ROUND_1`
- Claude Status: `FIX_ROUND_1`
- Last Verified At: `2026-10-02 (Asia/Shanghai)`

### Phase 00 当前验收

#### Codex

- Candidate HEAD: `002f6e1`
- Working Tree: `CLEAN`（忽略的 Maven / IDEA 本地产物不在提交中）
- Judge Maven Verify: `PASSED`（外部合成 MP3 / FLAC / WAV，2 tests，0 failures，0 skipped）
- 原件 SHA-256 与解码 PCM 对比：`VERIFIED BY AUTOMATED TEST`
- 复现文档：`FAILED`（文档先在 `target/` 生成样本，再运行 `mvn clean verify`，样本会被 `clean` 删除）
- 播放器人工验收：`NOT RUN`
- Completion Report 模板：`MISSING`

#### Claude

- Candidate HEAD: `1a754b6`
- Working Tree: `CLEAN`（忽略的 Maven / IDEA 本地产物不在提交中）
- Judge Maven Verify: `PASSED WITH INCOMPLETE COVERAGE`（外部合成 MP3，2 tests，1 skipped）
- MP3 工作副本与原件 SHA-256：`VERIFIED BY AUTOMATED TEST`
- FLAC / WAV / 封面 / 歌词 / 解码 PCM：`NOT RUN`
- 缺失 fixture 时测试可能直接返回成功：`FAILED QUALITY GATE`
- 播放器人工验收：`NOT RUN`
- Completion Report 模板：`MISSING`

- Next Gate: 两边分别完成 `benchmark/prompts/phase-00-fix-round-1-*.md`，主控重新验收并由用户完成手动体验清单。

## 用户主观体验记录

等待用户按 `benchmark/reports/phase-00/manual-acceptance.md` 记录 Codex 与 Claude 的实际 CLI 体验。

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
