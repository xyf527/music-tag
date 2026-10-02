# Phase 00 Fix Round 1 — Claude

将以下正文发送给 Claude Code CLI：

```text
你正在执行 music-tag Phase 00 的 Fix Round 1。只在当前 `agent/claude` worktree 工作，不得读取 Codex 的目录、分支、报告或实现。

Judge 在提交 `1a754b6` 上复现到以下结果：

1. 使用外部合成 MP3 执行 Maven 验证时构建通过，但结果为 2 tests、1 skipped。
2. 当前只实际验证 MP3 标题/歌手写入、写后解析和原件 SHA-256；FLAC、WAV、封面、歌词及解码 PCM 比较均为 NOT RUN，未达到共同 Phase 00 任务要求。
3. 当 `MUSIC_TAG_FIXTURES_DIR` 已设置但 `sample.mp3` 不存在时，测试直接 return，可能产生假通过。
4. 没有保留可供用户在播放器和标签工具中检查的修改后 MP3 / FLAC / WAV。
5. 能力矩阵存在 `git diff --check` 行尾空格；未按 `benchmark/templates/candidate-report.md` 提供标准完成汇报。

请严格回到 `benchmark/tasks/phase-00/task.md` 补齐本阶段，不得参考另一候选实现：

- 用外部合规样本或可复现的原创合成音调实际验证 MP3、FLAC、WAV。
- 验证基础文本读写；对真实支持的格式验证封面和歌词；WAV 的 ID3 / RIFF INFO、封面和歌词必须基于实际行为记录支持或不支持。
- 每种格式都验证写后重新解析、原件 SHA-256 不变，并使用可靠方式比较写前写后的音频流或解码内容；无法执行的项目必须失败或明确 NOT RUN，不能静默跳过。
- Fixture 缺失时测试必须明确失败或明确跳过并让完整验收失败，禁止直接 return 形成假通过。
- 提供可复现的人工验收入口，在 Git 忽略目录中保留修改后样本和哈希/能力报告，不得提交媒体文件。
- 重新执行完整 Maven 测试和构建，修复 diff check，并按 candidate report 模板补齐完成报告。
- 检查没有媒体、Secret、构建产物或 IDEA 私有文件进入提交。
- 提交修复并汇报新 commit；完成后停止，不得开始 Phase 01，不得自行 push。
```
