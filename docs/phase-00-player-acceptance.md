# Phase 00 播放器和标签工具人工验收

## 可复现入口

在仓库外生成原创样本和修改后副本：

```sh
FIXTURES=$(mktemp -d /tmp/music-tag-fixtures.XXXXXX)
MUSIC_TAG_FIXTURES_DIR="$FIXTURES" ./scripts/generate-fixtures.sh
MUSIC_TAG_FIXTURES_DIR="$FIXTURES" mvn test
```

测试产生的修改后副本位于系统临时目录，测试结束后通常由系统清理。若需要人工检查，应将 `AudioTagProofOfConceptTest` 的临时目录复制到 Git 忽略目录（例如 `private-media/phase-00-round-1/`），不得将媒体文件提交到 Git，并用 `shasum -a 256` 保存源文件和副本哈希。

建议使用 VLC、foobar2000、MusicBrainz Picard 或其他本地标签工具打开每种修改后副本，记录工具和版本。

## 检查项目

1. MP3、FLAC、WAV 是否能从头到尾播放且无解码错误。
2. MP3/FLAC 的标题和歌手是否显示为 `Phase 00 <format>` / `Phase 00 artist`。
3. MP3 的 APIC、FLAC 的 PICTURE 是否显示 PNG 封面。
4. MP3 的 USLT、FLAC 的 `LYRICS` 是否可见；同步滚动能力必须单独记录。
5. WAV 单独检查：本轮 Jaudiotagger 写入返回 `UNSUPPORTED`，不应把 GenericTag 读取能力误认为 ID3/RIFF INFO 写入能力。
6. 用 `ffprobe` 或播放器确认写后音频仍可解析。
7. 重新计算外部源文件 SHA-256，必须与写入前一致。

## 当前人工结果

| 格式 | 播放器/标签工具 | 可播放 | 文本 | 封面 | 歌词 | 同步歌词 | WAV ID3 | WAV RIFF INFO | 原件哈希不变 |
|---|---|---|---|---|---|---|---|---|---|
| MP3 | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | N/A | N/A | 自动测试 VERIFIED |
| FLAC | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | N/A | N/A | 自动测试 VERIFIED |
| WAV | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | UNSUPPORTED | UNSUPPORTED | 自动测试 VERIFIED |

本环境未实际打开播放器，因此人工播放器验收仍为 `NOT RUN`，不伪装为通过。
