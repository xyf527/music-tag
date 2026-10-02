# Phase 00 播放器人工验收

当前环境没有外部合规音频样本，因此播放器验收状态为 `NOT RUN`。不得将自动生成或没有实际试听的文件记录为人工通过。

获得合法、可复现的 MP3、FLAC、WAV 样本后，对每个格式的工作副本执行：

1. 记录原始 SHA-256，并确认只对工作副本写入。
2. 在支持该格式的播放器中打开成品，确认能够从头到尾播放且无解码错误。
3. 检查标题、歌手、专辑等文本是否显示为预期值。
4. 检查封面是否显示，并记录 JPEG/PNG、尺寸和播放器名称。
5. 检查歌词字段是否可见；同步滚动能力单独记录，不因歌词文本可见而推断同步支持。
6. WAV 额外分别记录 ID3 与 RIFF INFO 的显示情况，以及歌词和封面的播放器表现。
7. 关闭播放器后重新打开，确认结果可重复。
8. 重新计算原始文件 SHA-256，必须与步骤 1 一致。

记录表：

| 格式 | 播放器/版本 | 可播放 | 文本 | 封面 | 歌词 | 同步歌词 | WAV ID3 | WAV RIFF INFO | 原件哈希不变 | 备注 |
|---|---|---|---|---|---|---|---|---|---|---|
| MP3 | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | N/A | N/A | NOT RUN | 无样本 |
| FLAC | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | N/A | N/A | NOT RUN | 无样本 |
| WAV | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | 无样本 |
