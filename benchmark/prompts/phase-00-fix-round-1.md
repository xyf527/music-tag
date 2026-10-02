# Phase 00 Fix Round 1 — Codex

将以下正文发送给 Codex CLI：

```text
你正在执行 music-tag Phase 00 的 Fix Round 1。只在当前 `agent/codex` worktree 工作，不得读取 Claude 的目录、分支、报告或实现。

Judge 在提交 `002f6e1` 上复现到以下结果：

1. 使用仓库外合成 MP3 / FLAC / WAV 样本执行 Maven 验证时，2 tests 全部通过；三格式文本、MP3/FLAC 封面与歌词、原件 SHA-256、写后解析和解码 PCM 比较均通过。
2. `PHASE-00-VERIFICATION.md` 的复现顺序存在错误：脚本先把样本写入 `target/phase-00-samples`，随后 `mvn clean verify` 会删除样本并导致测试失败。
3. 当前测试使用 JUnit 临时目录，完成后没有保留可供用户在播放器和标签工具中检查的修改后 MP3 / FLAC / WAV。
4. 未按 `benchmark/templates/candidate-report.md` 提供模型、时间、before/after commit、实际测试和范围检查等完成汇报。

请完成以下修复：

- 修正文档和脚本，使从干净工作区开始的完整命令可复现；样本放在仓库外，或调整 clean / generate / verify 的安全顺序。
- 提供一个可复现的人工验收入口，在被 Git 忽略的位置生成并保留原始样本副本、修改后 MP3 / FLAC / WAV、原件哈希、成品哈希和能力结果。不得提交媒体文件。
- 文档写明人工成品路径、生成命令以及播放器/标签检查步骤。
- 重新执行完整 Maven 测试和构建，确保核心测试没有跳过。
- 按 candidate report 模板补齐 Phase 00 完成报告，真实记录本轮命令与结果。
- 检查没有媒体、Secret、构建产物或 IDEA 私有文件进入提交。
- 提交修复并汇报新 commit；完成后停止，不得开始 Phase 01，不得自行 push。
```
