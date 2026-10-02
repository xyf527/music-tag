# Phase 00 音频格式能力矩阵

## 验证方式

使用 `scripts/generate-fixtures.sh` 在仓库外生成 1 秒原创 440 Hz 正弦音调：WAV 由 FFmpeg PCM 编码，MP3 使用 `libmp3lame`，FLAC 使用 FFmpeg FLAC 编码；同时生成 1x1 PNG 和 LRC。执行方式：

```sh
FIXTURES=$(mktemp -d /tmp/music-tag-fixtures.XXXXXX)
MUSIC_TAG_FIXTURES_DIR="$FIXTURES" ./scripts/generate-fixtures.sh
MUSIC_TAG_FIXTURES_DIR="$FIXTURES" mvn test
rm -rf "$FIXTURES"
```

素材不进入 Git。测试为每种格式建立临时副本，记录并校验源文件 SHA-256 未改变；修改后重新读取标签并验证音频仍可解析。

## 实际环境与依赖

| 项目 | 实际值 |
|---|---|
| Java | Temurin 17.0.18 |
| Maven | Apache Maven 3.9.11 |
| Spring Boot | 3.5.6 |
| 标签库 | Jaudiotagger 2.0.1 |
| 测试工具 | FFmpeg（生成外部原创样本）；Jaudiotagger（读写/重读） |
| 播放器 | NOT RUN |

Jaudiotagger 3.0.1 不存在于 Maven Central 公开版本元数据，实际解析失败；2.0.1 可解析并用于本轮验证。

## 矩阵

| 格式 | 基础读取 | 文本写入/重读 | 封面 | 歌词/等价字段 | 写后解析 | 音频流/解码比较 | 状态与实际结果 |
|---|---|---|---|---|---|---|---|
| MP3 | VERIFIED | VERIFIED | VERIFIED（ID3 APIC，PNG） | VERIFIED（ID3 USLT，LRC 文本） | VERIFIED | NOT RUN（未集成独立 PCM 解码比较） | 读写往返通过 |
| FLAC | VERIFIED | VERIFIED | VERIFIED（Vorbis PICTURE，PNG） | VERIFIED（Vorbis Comment LYRICS） | VERIFIED | NOT RUN（未集成独立 PCM 解码比较） | 读写往返通过 |
| WAV | VERIFIED（可解析） | UNSUPPORTED（GenericTag `Not implemented for this format`） | UNSUPPORTED（同一写入路径不支持） | UNSUPPORTED（同一写入路径不支持） | VERIFIED（原始副本仍可解析） | NOT RUN（未集成独立 PCM 解码比较） | Jaudiotagger 2.0.1 不能满足 WAV 写入门槛；未声称支持 |

WAV 专项：当前库将 WAV 作为可读取的 GenericTag，但写入标题、歌词或封面时抛出 `UnsupportedOperationException: Not implemented for this format`。因此 RIFF INFO、WAV 内 ID3、歌词和封面均记录为 `UNSUPPORTED`，需要后续独立 WAV 适配方案，不能在本阶段宣称 V1 WAV 支持。

## 测试状态

设置外部目录后，缺少任一 `sample.mp3`、`sample.flac`、`sample.wav` 或 `cover.png` 会由测试断言失败；不会再通过直接 `return` 形成假通过。未设置目录时测试明确为 assumption skip，完整验收命令应使用上述生成脚本后运行。
