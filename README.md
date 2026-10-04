# music-tag

一个面向个人音乐收藏的本地音乐标签整理工具。项目计划使用 Java 17、Spring Boot 3、Thymeleaf、MySQL 8 和 Maven 构建，并以保护原始音频、修改前预览、修改后验证为基本原则。

完整需求和设计基线见 [Music-Tagger-V1-开发设计文档.md](docs/Music-Tagger-V1-开发设计文档.md)。文档中出现的“Codex”执行约束，在 Benchmark 中对 Codex CLI 与 Claude Code CLI 同等适用。

## V1 实际格式能力

支持范围由原创生成样本、Java 17 写后重读和 FFmpeg 解码结果决定。M4A 与 OGG 分别验证，Opus 未据其可解码性推断标签可写。以下歌词/封面“支持”包括 KEEP、SET、REMOVE；不支持操作返回 `UNSUPPORTED_FORMAT` 和中文说明。

| 格式与边界 | 读取/文字标签/成品 | 歌词三态 | 封面三态 | 音频验证 |
|---|---|---|---|---|
| MP3（ID3v2） | 支持 | 支持 | 支持 | 写前后解码 PCM 一致 |
| FLAC | 支持 | 支持 | 支持 | 写前后解码 PCM 一致 |
| WAV（RIFF INFO + 内嵌 ID3） | 支持 | 支持 | 支持 | 写前后解码 PCM 一致 |
| M4A（只开放 AAC-LC） | 支持 | 支持 | 支持 | 独立样本三态修改后解码 PCM 一致 |
| OGG（只开放 Vorbis） | 支持 | 支持 | 支持 | 独立样本三态修改后解码 PCM 一致 |
| Opus（含 OGG 中的 Opus） | UNSUPPORTED | UNSUPPORTED | UNSUPPORTED | 原创样本可解码，但 jaudiotagger 3.0.1 无标签读写支持 |

M4A 的 ALAC、未验证 AAC profile、MP4 视频及其他容器变种未开放。OGG Opus 不会作为 OGG Vorbis 接收。格式同时检查受控扩展名、容器标识和实际解析，不擅自转码。播放器的歌词/封面显示及听感仍需用户验收，自动解码通过不等同于人工播放器通过。运行时无需 FFmpeg；测试用 FFmpeg 生成合法样本和比较解码结果，当前验证版本为 9.0.2，使用原生 Vorbis 编码器（不要求 libvorbis）。所有媒体仅保存在系统临时目录或忽略目录。

`GET /api/upload-policy` 与 `GET /api/batches/policy` 提供同一 Registry 的能力矩阵，包含 readable、textTags、lyrics、artwork、output。Opus 作为显式 UNSUPPORTED 行展示，不出现在允许上传格式列表中。

## 单曲与批量使用

单曲工作台使用暖白/紫色界面；“预览变更”打开播放器式弹窗，显示计划中的歌曲名、歌手、封面和前四行歌词（隐藏 LRC 时间戳），并保留修改清单。弹窗是标签预览，不提供试听；可通过关闭按钮、继续编辑或 Escape 退出。生成成功后显示独立成品卡片和醒目的下载按钮；失败不会展示成功卡片。移动端弹窗采用上下布局。

单曲成品下载和批量 ZIP 音频统一命名为 `歌曲名 - 歌手.格式`，取对应成品的实际标签（不是最新版本或上传前标签）。无歌曲名时用原文件名，无歌手时用“未知歌手”；非法字符安全替换，ZIP 重名加 `(2)` 等序号。内部存储仍使用唯一文件名，不会覆盖其他版本。WAV 支持文字标签、歌词和封面 KEEP/SET/REMOVE，歌词及封面写入内嵌 ID3，文字标签同步 RIFF INFO。播放器须支持 WAV 内嵌 ID3 才能显示歌词/封面；播放器人工验收尚未执行。

打开 `/` 上传受支持音频，可选 LRC 和 JPG/PNG；上传成功后按后缀展示明显的格式能力提示。查看已有标签，选择保留/修改/删除，先预览再执行。写入发生在工作副本，写后重读校验文字、歌词和封面哈希，发布独立成品及版本历史，原件不修改。下载链接、JSON 报告和备份状态显示在结果区。没有变更的 KEEP 也会发布经过验证的独立版本，不能据文件字节恰好相同声称伪造成功。

打开 `/batch` 选择文件或目录。优先按同目录同基名匹配 LRC/封面，其次参考 LRC 标题/歌手；冲突须人工绑定、解绑或跳过。页面按每个音频的能力提供操作，WAV 也可绑定歌词及封面。确认不可变计划后执行，可恢复任务 ID，失败/中断项可重试，已成功项不重复生成版本。Opus、损坏输入和伪造扩展名分别显示不支持/失败，不拖累有效文件。ZIP 包含成功成品及报告，报告含格式、能力、实际执行动作、哈希和写后验证状态，不包含存储路径。

## 部署、升级与运维

### 数据库与 MinIO 开关

在私密 `application.yml` 的现有 `music` 节点下配置（不要创建重复节点）：

```yaml
music:
  database:
    enabled: false
  operations:
    minio-enabled: false
```

未配置数据库开关时默认使用内存模式，MinIO 默认关闭。数据库关闭时使用独立 H2 内存数据库，不连接 MySQL，任务和版本记录重启后丢失，并强制关闭 MinIO 备份（即使其开关为 true）。音频标签库需要真实文件，因此上传、工作副本、成品及报告使用隔离临时目录，而非纯 JVM 内存；正常退出自动删除，强制终止可能残留系统临时文件。此模式不使用配置中的持久化存储目录，也不恢复上次任务。不要用于需要长期保存的数据。

Flyway 默认关闭，内存模式不使用 Flyway，通过 SQL 初始化创建临时表。开启 MySQL 时需要已有完整表结构；如需自动迁移，须显式设置 `SPRING_FLYWAY_ENABLED=true`。IDE/Maven 的 `target/classes` 不包含私密 YAML；要加载它，可在启动参数加 `--spring.config.additional-location=file:./src/main/resources/application.yml`，不要将私密 YAML 打包。

数据库开启时使用原有 MySQL 和持久化目录；此时 `minio-enabled: false` 仅关闭远程备份，不删除本地成品。开关也可通过 `MUSIC_DATABASE_ENABLED`、`MUSIC_OPERATIONS_MINIO_ENABLED` 环境变量设置，修改后重启生效。私密 YAML 不进入发布 JAR，打包运行时请通过外部配置路径加载。参考 `src/main/resources/application.example.yml`；原有私密配置无需被覆盖。

仅支持单实例 Linux/amd64。Java 17 构建：`mvn clean verify`、`mvn package`。Docker 运行用户为 10001，容器监听 8080，宿主默认 18081。Compose 只启动应用，连接 M710q 已有 MySQL；不启动第二个 MySQL。上传、工作、成品、报告、日志均使用独立持久化挂载。

填写服务器私密 `.env`（权限 600），地址/密码只能通过环境变量注入。外部 MySQL/MinIO 不使用容器 localhost；连接宿主机可配置 host gateway，同一 Compose 网络使用服务名。MinIO 默认关闭，开启时配置专属 bucket/prefix；已有 MinIO 不再启动第二实例。可选 override 只供用户明确需要独立实例时使用。

`scripts/build-v1-release.sh` 生成 Linux/amd64 镜像和忽略目录 `deployment-artifacts/v1/music-tag-v1-release.tar`，不会读取或修改用户本地 Phase 03 构建脚本。发布包不包含私密 YAML、.env、媒体、数据库、IDE 配置或本机配置。构建参数注入 commit、时间及分支；`/api/version` 用于核对实际版本。使用 Alibaba Cloud Toolkit 的 Deploy to Host 上传并调用 `deploy/m710q/deploy.sh`；预检和健康失败会尝试回滚上一已验证镜像，保留 MySQL 和持久化文件。不得运行 `docker compose down -v`。详见 [部署说明](docs/Phase03-运维与M710q部署.md) 和 [Phase 04 升级/人工验收清单](docs/Phase04-Acceptance.md)。

本地发布成功后独立备份成品、实际绑定歌词/封面和 manifest。备份失败不改变本地成功状态，持久化重试并暂缓相关本地文件清理。原件/成品各自从创建时间默认保留 30 天；报告和数据库历史长期保留，MinIO 不自动过期。清理可 dry-run，返回并持久化删除/跳过/失败数量，拒绝符号链接和根目录外路径。工作副本在结束后清理，中断窗口保留至恢复判定。

启动扫描发布日志、数据库和存储，验证哈希后补齐可判断的版本/报告，无法判断的文件保留并登记人工事项。磁盘不足暂停新上传，下载和查询继续使用。健康接口为 `/health/live`、`/health/ready`，运维状态为 `/api/operations`；备份重试、清理和历史接口参见部署文档。应用没有身份系统，应由可信内网控制访问。未来迁移 K8s 单副本前须先保证外部 MySQL、持久化共享存储、Secret 注入、启动恢复与单写者约束；本版本不提供 K8s 清单或多副本能力。

## 常见错误与验收边界

- `UNSUPPORTED_FORMAT`：格式或操作未开放；Opus、M4A 非 AAC-LC 明确拒绝，不生成虚假成功版本。
- `CORRUPT_AUDIO`：容器与扩展名不匹配或解析失败，检查原文件。
- `VERIFY_FAILED`：写后重读与计划不一致，不发布该工作副本。
- `DISK_LOW`：暂停新上传，释放空间或检查配置阈值；历史下载/状态查询不受此限制。
- `BACKUP_UNAVAILABLE`：本地成品仍可用，检查专属 MinIO 配置并重试；本地清理暂缓。
- `FILE_IO_ERROR`：已清理、缺失或不安全路径的成品不可下载，状态接口不会伪装可用。

真实 M710q 升级、外部 MySQL/MinIO 和人工播放器验收以 [候选报告](docs/Phase04-Candidate-Report.md) 的实际结果为准；未执行项标记 PENDING USER EXECUTION。V1 最终是否通过由项目经理在用户人工验收后裁决。

本仓库同时用于产品开发和 Codex / Claude 同模型开发质量对比。

## 分支与工作区

- `agent/codex`
- `agent/claude`
- `main`：共同基线、Benchmark 规则、主控报告和最终确认内容

候选开发必须使用独立 Git worktree，不能在同一个目录轮流切换分支。

## 项目管理入口

- `PROJECT_CONTROLLER.md`：GPT-5.6 Sol 项目经理、裁判和 Benchmark 主控定位
- `BENCHMARK.md`：公平性、隔离、评分和验收规则
- `benchmark/PROGRESS.md`：当前进度与每轮状态
- `benchmark/prompts/`：主控、候选开发和裁判提示词
- `benchmark/templates/`：任务与报告模板

## 代码与安全约定

- 项目自有 Java 代码统一使用根包名 `com.xin.musictag`；后续包只能位于该根包之下。
- `src/test/` 是必须提交的测试源码，不能加入 `.gitignore`。项目根目录的 `/test/`、`/tests/`、`/test-output/`、`/test-results/`、`/.test-data/` 仅用于本地生成的测试媒体、临时数据和报告，必须忽略。
- Git 中禁止出现真实密码、Token、API Key、Access Key、SSH 密钥、数据库连接串、内网/公网 IP 或个人服务器地址。
- 主机、端口、数据库和凭据通过环境变量或服务器 Secret 注入，例如 `M710Q_HOST`、`SERVER_PORT`、`MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_DATABASE`、`MYSQL_USER`、`MYSQL_PASSWORD`。
- 开发、集成测试、建库和 Flyway 验证统一使用 M710q 上已有的 MySQL 8；Mac 不启动本地 MySQL、Docker MySQL 或 Testcontainers MySQL。两名候选使用各自独立的开发库和测试库。
- Mac 侧直连 endpoint 和凭据保存在权限为 `600` 的 `/.music-tag-local/mysql.conf`；该目录必须由 Git 忽略，不得提交。详见 `docs/M710Q-MySQL安全访问.md`。
- `.env`、`.env.*`、IDEA 私有配置和部署凭据不得提交；`.env.example` 只能使用明显无效的占位符，不得复制真实值。
- 日志、异常、页面、JSON 报告和测试输出不得泄露凭据、服务器绝对路径或真实主机地址。提交前必须进行 Secret、IP 和敏感文件扫描。

## 内容与版权声明

本仓库只提供源代码、项目文档和不包含受版权保护媒体的测试材料：

- 不提供、托管或分发任何音乐音频文件。
- 不提供、托管或分发任何专辑封面、歌词文件或歌词数据库。
- 不提供从音乐平台抓取、破解、绕过访问控制或下载受限内容的能力。
- 用户只能处理自己拥有版权、已经获得授权，或法律允许处理的文件。
- 贡献者不得向仓库提交未经授权的音频、封面、歌词或其他第三方媒体。
- 如需测试样本，应使用自行创作、公共领域或具有明确开放许可的素材，并记录来源和许可。

本项目是文件标签管理工具，不授予用户使用任何第三方音乐、封面或歌词的权利。使用者应自行确认其所在地适用的版权和数据法律。

## 开源许可

源代码与项目自有文档采用 [MIT License](LICENSE) 发布。第三方依赖和用户自行提供的媒体文件仍受各自许可或版权约束。
