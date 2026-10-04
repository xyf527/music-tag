# Music Tag

Music Tag 是一个自托管的音乐标签整理工具，支持单曲编辑、批量导入、标签预览、歌词与封面写入、版本记录、ZIP 导出以及可选 MinIO 备份。应用不会覆盖原文件，处理结果始终保存为独立成品。

## 支持格式

| 格式 | 文字标签 | 歌词 | 封面 | 说明 |
|---|---:|---:|---:|---|
| MP3 | 支持 | 支持 | 支持 | ID3v2 |
| FLAC | 支持 | 支持 | 支持 | Vorbis Comment / PICTURE |
| WAV | 支持 | 支持 | 支持 | RIFF INFO 与内嵌 ID3；播放器兼容性可能不同 |
| M4A | 支持 | 支持 | 支持 | 仅开放已验证的 AAC-LC |
| OGG | 支持 | 支持 | 支持 | 仅开放 Vorbis |
| Opus | 不支持 | 不支持 | 不支持 | 明确拒绝，不生成虚假成品 |

应用会检查扩展名、容器和实际解析结果。M4A ALAC、MP4 视频、OGG Opus 及其他未验证变种不会作为已支持格式处理。

## 主要功能

- 中文响应式单曲工作台和修改预览。
- 标题、歌手、专辑、歌词和封面的保留、设置、删除。
- 多文件及目录批量导入、确定性匹配、冲突处理和人工绑定。
- 有界并发、部分成功、失败重试、刷新及重启恢复。
- 成品、JSON 报告和批量 ZIP 下载。
- 成品按 `歌曲名 - 歌手.格式` 命名，内部文件仍使用安全唯一标识。
- MySQL 持久化、Flyway 迁移、磁盘保护和文件生命周期管理。
- 可选 MinIO 备份、失败重试和稳定对象键。
- Docker Compose、健康检查、版本信息及 M710q 部署和回滚脚本。

## 快速运行

要求 Java 17。从 [GitHub Releases](https://github.com/xyf527/music-tag/releases) 下载 JAR 后运行：

```bash
java -jar music-tagger-1.0.0.jar
```

默认使用临时 H2 数据库和临时文件目录，适合体验。进程正常退出时会清理临时文件，重启后任务记录不会保留。浏览器打开 `http://localhost:8080`。

## 持久化运行

复制示例配置：

```bash
cp src/main/resources/application.example.yml application.yml
```

通过外部配置启动：

```bash
java -jar music-tagger-1.0.0.jar \
  --spring.config.additional-location=file:./application.yml
```

生产环境建议使用环境变量或权限为 `600` 的私密配置文件：

```text
MUSIC_DATABASE_ENABLED=true
SPRING_DATASOURCE_URL=jdbc:mysql://数据库地址:端口/数据库名
SPRING_DATASOURCE_USERNAME=数据库用户
SPRING_DATASOURCE_PASSWORD=数据库密码
SPRING_FLYWAY_ENABLED=true
MUSIC_STORAGE_ROOT=/持久化数据目录
```

启用 MinIO 时另外配置 `MUSIC_OPERATIONS_MINIO_ENABLED`、endpoint、access key、secret key、bucket 和 prefix。bucket 需要提前创建。配置文件、密码、真实地址和媒体文件不得提交到 Git。

## Docker 部署

填写服务器上的私密 `.env`：

```bash
cp .env.example .env
chmod 600 .env
docker compose --env-file .env up -d music-tag
```

健康与版本接口：

```text
GET /health/live
GET /health/ready
GET /api/version
```

`compose.yaml` 默认连接已有 MySQL，不会启动第二个 MySQL。若复用宿主机服务，容器中不能使用 `localhost`；可使用配置好的 host gateway。已有 MinIO 直接填写外部 endpoint，需要独立 MinIO 时才使用 `compose.minio.yaml`。

M710q 部署入口位于 `deploy/m710q/`。`scripts/build-v1-release.sh` 可生成 Linux/amd64 镜像和供 Alibaba Cloud Toolkit 上传的发布包。部署和回滚脚本不会删除数据库、Docker volume 或持久化目录。

## 从源码构建

```bash
mvn clean verify
mvn package
```

构建产物位于 `target/music-tagger-1.0.0.jar`。测试会在系统临时目录生成原创音频样本，不会把音乐、歌词或封面提交到仓库。

## 安全与版权

应用面向可信内网的单实例部署，当前不提供用户认证或公网访问防护。不要直接暴露到互联网。

本仓库不提供、托管或分发音乐、歌词、封面或第三方媒体，也不提供抓取、破解或绕过访问控制的功能。用户只能处理自己拥有版权、已获授权或法律允许处理的文件。

## License

[MIT License](LICENSE)
