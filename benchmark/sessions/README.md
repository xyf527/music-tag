# Candidate session exports

本目录保存可公开审阅的候选 CLI 对话快照。上传的是统一结构、经过脱敏的 JSON，不是工具原始会话文件。

每个文件只保留：

- 用户实际输入的任务、确认和继续指令。
- 候选助手对用户可见的文字回复。
- 会话 ID、原始文件 SHA-256、模型与推理等级等最小元数据。

明确排除：

- system / developer 指令、隐藏推理。
- 工具调用、命令输出、权限配置和环境快照。
- 用户主目录等本机绝对路径。
- 可能构成凭据的 token、API key、密码字段。

`raw_uploaded: false` 表示原始 JSONL 只保留在本机。两种 CLI 的原始格式和记录粒度不同，因此 JSON 中的消息数量不能直接当作效率分数。

当前文件：

- `phase-00/codex.json`
- `phase-00/claude.json`

