# Phase 01 人工验收说明

## 生成并保留产物

使用仓库外的原创 Phase 00 样本和指定输出目录：

```sh
FIXTURES=$(mktemp -d /tmp/music-tag-fixtures.XXXXXX)
MUSIC_TAG_FIXTURES_DIR="$FIXTURES" ./scripts/generate-fixtures.sh
MUSIC_TAG_FIXTURES_DIR="$FIXTURES" ./scripts/generate-phase-01-artifacts.sh /tmp/music-tag-phase-01-artifacts
```

脚本不会删除 `/tmp/music-tag-phase-01-artifacts`。目录应包含：

- `processed-sample.mp3`
- `processed-sample.flac`
- `report-mp3.json`
- `report-flac.json`
- `report-wav.json`（明确 `UNSUPPORTED_FORMAT`）
- `original-hashes.sha256`

媒体和运行数据不会写入 Git。

## 检查清单

1. 用浏览器打开 `/` 或 `/single`，上传 MP3/FLAC，确认单曲入口可见。
2. 通过 `/api/single/upload` 检查文件名、格式、大小和标签预览。
3. 提交三态 `KEEP/SET/REMOVE` 编辑计划，确认只设置的字段变化，其他字段保留。
4. 检查 MP3/FLAC 成品能被播放器/标签工具解析，报告含状态、版本标识和源哈希。
5. 上传 WAV 并请求修改，确认结果为 `FAILED`、错误码 `UNSUPPORTED_FORMAT`，不产生伪成功成品。
6. 尝试空文件、未知扩展名和带目录分隔符的文件名，确认请求拒绝。
7. 对照 `original-hashes.sha256`，确认原件未变化；重复处理应生成新的版本标识。
8. 确认报告不含服务器绝对路径、凭据或真实主机信息。

当前命令行产物生成已实际运行；浏览器和播放器人工操作在本环境为 `NOT RUN`。
