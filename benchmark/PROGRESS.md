# Benchmark Progress

## 固定配置

- Controller: GPT-5.6 Sol
- Controller Reasoning Effort: medium
- Controller Chat: `music-tag｜项目经理・裁判・Benchmark 主控`
- Codex Worktree: `${MUSIC_TAG_CODEX_WORKTREE}`
- Claude Worktree: `${MUSIC_TAG_CLAUDE_WORKTREE}`
- Candidate Model: `gpt-5.6-luna`
- Candidate Reasoning Effort: `medium`
- Candidate Harnesses: Codex CLI / Claude Code CLI
- Terminal: Ghostty

## 当前状态

- Current Task: `Phase 01 — 单曲编辑最小闭环`
- Phase 01 Task SHA-256: `497ab6a22e67dc6b701c8121a81c4122dfa8d5f02f3b98c9380fba141c2f093f`
- Codex Phase 01 Baseline: `f555501` / tag `benchmark/phase-01-codex-base`（上一实现 HEAD `ef0d5de`）
- Claude Phase 01 Baseline: `b7c5018` / tag `benchmark/phase-01-claude-base`（上一实现 HEAD `862fcc3`）
- Codex Phase 01 Candidate HEAD: `d1d2fa32d17fa8d52946ca324fb9f76d05abe2e6`
- Claude Phase 01 Candidate HEAD: `8f2473501207a5bafe4572d0fb665a9649f2c10d`
- Codex Status: `FIX_ROUND_2`
- Claude Status: `FIX_ROUND_2`
- Last Updated At: `2026-10-03 (Asia/Shanghai)`

### Phase 01 初轮复核

#### Codex

- Java 17 `mvn clean verify`: `PASSED`（4 tests）
- 真实 MP3 / FLAC 处理与报告：`PASSED`
- 分层结构：`PASSED`
- 浏览器歌词/封面三态：`INCOMPLETE`
- 隔离 MySQL 8 集成测试：`NOT RUN`（当前使用 H2 MySQL mode）
- 连续版本父子关系与安全负向测试：`INCOMPLETE`

#### Claude

- Java 17 `mvn clean verify`: `PASSED BUT INSUFFICIENT`（仅 1 test）
- 真实 MP3 / FLAC 人工产物：`FAILED`（脚本复制原件，哈希相同）
- MySQL / Flyway 运行路径：`FAILED`（仍为内存仓库）
- 浏览器完整闭环：`FAILED`
- 任意路径下载与绝对路径泄露：`SECURITY GATE FAILED`

- Phase 01 Interim Objective Leader: `CODEX`
- Phase 01 User Choice: `CODEX`
- Phase 01 Final Score: `PENDING FIX ROUND 1`
- M710q / Alibaba Cloud Toolkit Deployment: `NOT READY`

### Phase 01 Fix Round 1 复核

- Codex Judge `mvn clean verify`: `PASSED`（5 tests）。三态页面、版本链和真实产物已补齐；仍需修复任务 API 绝对路径泄露、下载根目录约束，并按新规则在 M710q MySQL 复核。
- Claude Judge `mvn clean verify`: `PASSED BUT INSUFFICIENT`（1 test）。候选自报 `PARTIAL`；MySQL 读取、完整页面、浏览器闭环和真实人工产物仍未完成。
- Next Gate: 两边分别执行 Phase 01 Fix Round 2；通过后才允许冻结 Phase 02。

### 数据库环境规则（自 2026-10-03 生效）

- 所有数据库访问、建库、Flyway 和集成测试统一使用 M710q 既有 MySQL 8。
- Mac 不启动本地 MySQL、Docker MySQL 或 Testcontainers MySQL。
- Codex / Claude 各自使用独立开发库和独立测试库。
- Mac 密码保存在 macOS 钥匙串，通过 `scripts/with-m710q-mysql.sh` 临时注入；不得发到聊天或写入 Git。

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

### Phase 01 用户反馈

- 操作与确认：Codex 仍更多。
- 最终回复：用户更喜欢 Claude 的详细程度。
- 目录结构与抽象：Codex 更符合预期，Claude 较乱。
- 速度：Claude 更快。
- Claude 基线 tag 中断：用户视为 Git / API 中转差异，不作主观扣分。
- 由于 Claude 当前无法进行真实使用体验，本阶段最终主观投票：`CODEX`。

## M710q 部署预检

- Verified At: `2026-10-02 (Asia/Shanghai)`
- Host: `${M710Q_HOST}`（真实地址不进入 Git）
- `${MUSIC_TAG_DEPLOY_ROOT}/music-tag-codex`: `EXISTS`
- `${MUSIC_TAG_DEPLOY_ROOT}/music-tag-claude`: `EXISTS`
- Host Port `18081`: `AVAILABLE`（检查时无监听）
- Host Port `18082`: `AVAILABLE`（检查时无监听）
- MySQL Host Port `${M710Q_MYSQL_PORT}`: `LISTENING`（实际值不进入 Git）
- 注：端口状态可能变化，每次实际部署前仍须重新检查。

## 历史

| Task | Codex | Claude | Comparison | Finalized At |
|---|---|---|---|---|
| Phase 00 | PASSED, 96 | PASSED, 84 | Technical: Codex; UX and user choice: Claude | 2026-10-02 |
| Phase 01 initial | FIX_ROUND_1 | FIX_ROUND_1 | Interim technical and user choice: Codex | 2026-10-03 |
