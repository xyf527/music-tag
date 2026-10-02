# Phase 00 音频格式能力矩阵

## 实际环境

| 项目 | 实际值 |
|---|---|
| Java | Temurin 17.0.18（通过 `JAVA_HOME=$HOME/.sdkman/candidates/java/17.0.18-tem` 验证） |
| Maven | Apache Maven 3.9.11 |
| Spring Boot | 3.5.6 |
| 标签库 | Jaudiotagger 2.0.1 |
| 测试素材 | `MUSIC_TAG_FIXTURES_DIR` 未设置；外部合规样本未提供 |
| 播放器 | NOT RUN |

Jaudiotagger 3.0.1 不存在于 Maven Central 的公开版本元数据，Maven 解析失败；因此未将其伪装为可用版本。2.0.1 是 Maven Central 可解析的发行版本，实际构建会验证其 API。

## 矩阵

状态只使用 `VERIFIED`、`UNSUPPORTED`、`FAILED`、`NOT RUN`。

| 格式 | 基础标签读取 | 文本字段写入/重读 | 封面 | 歌词/等价字段 | 写入后解析 | 音频流/解码比较 | 说明 |
|---|---|---|---|---|---|---|---| 
| MP3 | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | 未提供外部样本 |
| FLAC | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | 未提供外部样本 |
| WAV | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | NOT RUN | 未提供外部样本；ID3、RIFF INFO、歌词、封面均未宣称支持 |

### 自动化 PoC 当前边界

PoC 适配层已实际封装 Jaudiotagger 的读取、标题/歌手写入、重新解析和临时副本策略。自动测试在设置 `MUSIC_TAG_FIXTURES_DIR` 且存在 `sample.mp3` 时执行副本写入与原件 SHA-256 不变检查；其他格式和封面/歌词需要补充外部合规样本后才能进入真实验证。

由于当前没有样本，不能把 API 可调用性写成格式能力已通过，也不能把 WAV 关键能力写成支持。
