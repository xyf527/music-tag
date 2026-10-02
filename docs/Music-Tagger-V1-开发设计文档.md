# Music Tagger V1：项目需求与开发设计文档

> 状态：需求基线 / 供 Codex 分阶段实施  
> 日期：2026-09-29  
> 定位：个人使用、部署于 Lenovo M710q 的 Java 模块化单体音乐标签工具。  
> 原则：**原件不可变、处理前可预览、执行后可验证、任务可追踪、模块可替换。**

## 0. 给 Codex 的执行约束

1. **先读取本文件及当前阶段明确指定的相关代码**；不要递归扫描整个项目，不要改动无关模块。
2. 接到需求先提供：涉及文件、修改方案、接口变更、风险、测试与验收步骤；**在获得用户确认之前不修改代码**。
3. 每次只实施文档所列的一个阶段。不得擅自提前引入 URL 下载、AI 识别、微服务、Redis、消息队列或未获确认的数据库实现。
4. 先完成可以运行的最小闭环，再逐步增加适配器；不能以“代码能编译”替代真实音频样本和播放器的验收。
5. 对依赖库、具体版本及 WAV 字段读写能力，先用 PoC 和自动化测试核实；**禁止凭想象调用不存在的 API**。
6. 不得覆盖原始音乐、删除尚在使用的文件，或在失败时静默吞掉异常。

## 1. 产品范围与明确不做的事情

### 1.1 V1 功能

- **单曲编辑**：上传一首音频；歌词（LRC）和封面（JPEG/PNG）均可选；展示已存在的标题、歌手、专辑、年份、时长、封面与歌词；允许仅修改任意一个字段或资源。
- **批量导入**：清晰区分「单曲编辑」与「批量导入」。批量模式支持多选文件或文件夹（可保留相对路径）；导入后先识别、匹配、分类展示，由用户逐行确认，再统一执行。
- **多格式**：V1 必须验证 MP3、FLAC、WAV 的读取、标签修改、封面与歌词嵌入及重新读取；M4A、OGG、Opus、AIFF、APE、WMA 等作为扩展测试，不得假称“全部兼容”。
- **文件保护**：原始上传文件保持不变；在任务工作副本中写入并验证；每次成功编辑创建新的成品版本，不覆盖旧版。
- **校验与交付**：重新读取标签核对预期，验证音频可解析、必要时比较音频流/解码后内容；单曲下载、批量 ZIP 下载及机器可读任务报告。
- **详细结果**：每首歌展示处理阶段、成功或失败、分类错误码、可读原因与必要的修复建议；批量允许部分成功。
- **记录与备份**：MySQL 持久化任务、资源、版本和备份状态；每项任务生成 JSON 报告作为快照/下载产物；MinIO 可配置开启，保存成品及歌词/封面，备份与处理分别计状态。
- **文件生命周期**：上传原件和成品自各自创建时起保存 30 天；工作副本任务结束后尽快清理；JSON 报告和数据库历史记录长期保存；MinIO 对象由用户手动管理；已开启备份但备份失败时暂缓清除相关文件，并发出显著提示。

### 1.2 V1 不实现

- 不抓取腾讯音乐、网易云音乐等第三方平台的受限资源。
- 不下载任意 URL；不调用 AI 生成歌词、时间戳或封面（仅预留扩展契约）。
- 不对音频重编码、不自动转码成其他格式、不保证所有播放器同步显示 LRC。
- 不做多人账户、支付、复杂权限管理、微服务、Kafka/RocketMQ/Redis 或 MongoDB/PostgreSQL 的实际适配器。

## 2. 最终技术选型

| 层面 | V1 选型 | 原因及边界 |
|---|---|---|
| 运行环境 | Java 17、Spring Boot 3.x、Maven | 熟悉的 Java 技术栈，单体部署 |
| 页面 | Thymeleaf + 少量原生 JavaScript / Fetch | 前后端一体；不额外维护 Vue 项目 |
| 音频元数据 | 先验证 Jaudiotagger 的 Java 17 兼容发行版，再按格式封装 | 锁定经测试的版本；WAV 做专项验证，无法实现的字段须报告 |
| 图片处理 | Java ImageIO + 格式适配层 | 校验 JPEG、PNG 内容/大小/像素和兼容性；必要时转换成目标格式 |
| 数据库 | MySQL 8.x + MyBatis-Plus + Flyway | 以 MySQL 为唯一主库；Flyway 管理 V1 数据库结构 |
| 数据库抽象 | 业务 Repository 接口 + MySQL Adapter | 数据库可替换不等于代码零改动；避免业务代码依赖 Mapper |
| 文件存储 | 服务器本地持久化目录 + Java NIO | 不将音频二进制塞进 MySQL |
| 备份 | MinIO Java SDK，可选 | 仅在本地校验完成后异步/延后归档，失败可重试 |
| 批处理 | Spring 管理的有界线程池 | V1 限制同时执行批次和单曲并发，不引入 MQ |
| 交付与部署 | 多阶段 Dockerfile 构建镜像；M710q Docker Compose 部署；保留 Kubernetes 部署能力 | MySQL 连接 M710q 已有实例；所有状态及数据独立于容器镜像 |
| 测试 | JUnit 5、Spring Boot Test、真实音频样本 | 以读写往返测试、故障注入和实际播放器验证为主 |

> 依赖库 API、具体版本号以及 WAV 歌词/封面的行为属于开发前验证项，不能在未验证时标为已实现。

## 3. 核心页面与交互

### 3.1 首页

两个清晰入口：

1. **单曲编辑**：上传单个音乐文件；可以单独追加一份 LRC、一张封面，或不上传任何额外资源，仅编辑已存在的标签。
2. **批量导入**：上传多个文件或整个文件夹。显示文件计数、格式识别、待匹配数量与潜在冲突数。

另有任务历史、音乐版本、备份状态及基础设置入口。上传与处理进度必须有状态展示。

### 3.2 单曲编辑页

- 展示音频基本信息：原始文件名、实际格式、时长、采样率、比特率（如适用）、文件大小和已有标签。
- 左侧显示现有封面和新封面预览；支持保留、替换、删除。
- 歌词区显示既有内容，允许上传 `.lrc` 或直接编辑文本；支持保留、替换、删除；解析 `[mm:ss.xx]` 等常见时间戳并提供格式校验结果。
- 标签字段包含：歌曲名、歌手、专辑、专辑歌手、年份、曲风、音轨号等；未触及字段原样保留。
- 提交前显示前后差异，包括保留项、修改项、删除项与格式能力警告。
- 处理完成后显示最终校验和下载入口；失败时保留编辑草稿，便于更正后重试。

### 3.3 批量匹配预览

分类：**完全匹配**、**部分匹配**、**存在冲突/待确认**、**未匹配资源**、**格式不支持/损坏**。

- 每行：音乐文件、当前标签、候选 LRC、候选封面、匹配依据、警告、处理动作（执行/跳过）。
- 自动匹配仅在有**唯一且强确定性**依据时应用：优先同一相对目录下忽略扩展名的完整文件名；其次是音频标签与 LRC `[ti:]`/`[ar:]` 的明确一致。
- 不用简单模糊相似度自动绑定。若同名翻唱、现场版等导致歧义，交由用户处理；一份 LRC/图片拟分配给多首音乐时显示冲突，并支持用户主动确认共享。
- 部分匹配**不是错误**：可只为音乐补充歌词、只补封面，或只改元数据。
- 未匹配的 LRC、封面单独列出，可手动指定目标音乐，或不参与本次任务。
- 用户点击「确认并执行」后，保存不可变 `ProcessingPlan` 快照；不可在后台悄悄更改匹配。

浏览器目录上传通常只提供相对路径，不应推测或保存 Mac 的绝对路径；服务端自建安全存储路径。

## 4. 可扩展架构：契约、抽象基类、注册表、配置

### 4.1 扩展约定

- 业务功能定义稳定接口；**确有可共享逻辑时，配套抽象基类**；具体 Provider/Handler 继承抽象类或直接实现接口。
- Spring 自动扫描具体 Bean；注册表按 `providerKey` 或 `format` 选择实现，重复键时启动失败，未知键明确报错，禁止静默 fallback。
- `@ConfigurationProperties` 将 YAML 映射为类型安全配置；`enabled` 控制是否可用，`default-provider` 仅决定用户未选时默认来源；不要误以为同一任务的每项资源必须共用一个 Provider。
- 核心任务编排依赖接口，不知道来源是浏览器、URL、AI，或底层标签库的具体实现。
- 不为“将来或许需要”创建无行为的空实现；V1 只交付必要实现及清晰扩展接口。

### 4.2 六类扩展点

| 接口 | 抽象基类 | V1 具体实现 | 以后可新增 |
|---|---|---|---|
| `AudioTagHandler` | `AbstractAudioTagHandler` | `Mp3TagHandler`、`FlacTagHandler`、`WavTagHandler` | M4A、OGG、Opus 等 |
| `ResourceProvider` | `AbstractResourceProvider` | `LocalUploadResourceProvider` | URL、服务器目录、NAS |
| `LyricsProvider` | `AbstractLyricsProvider` | `LocalLrcLyricsProvider` | URL LRC、AI 转录和时间戳生成 |
| `CoverProvider` | **`AbstractCoverProvider`（必须）** | `LocalCoverProvider` | URL、AI 图片来源 |
| `AudioResourceMatcher` | `AbstractAudioResourceMatcher`（可选） | 严格文件名/标签匹配策略 | 用户确认的其他策略 |
| `BackupProvider` | `AbstractBackupProvider`（按共享逻辑需要） | `MinioBackupProvider` | NAS、其他对象存储 |

`AbstractCoverProvider` 必须承担共用图片检查、最大体积/像素限制、类型识别、解码验证、异常规范化。具体来源类仅负责**获得图片**；图片重编码/目标标签格式兼容由独立 `ArtworkProcessor` 执行，避免每种来源重复实现。

`ResourceProvider` 获取资源字节流/临时资源；`LyricsProvider` 返回结构化歌词（内容、编码、时间戳、来源等）。两者不要混用：未来 AI 可能直接返回 `LyricsContent`，而非文件路径。

### 4.3 关键领域模型与契约（示意）

```java
public interface AudioTagHandler {
    String format();
    AudioCapabilities capabilities();
    AudioMetadata read(Path audioFile);
    void write(Path workingCopy, TagWritePlan plan);
}

public interface CoverProvider {
    String providerKey();
    CoverContent load(CoverRequest request);
}

public abstract class AbstractCoverProvider implements CoverProvider {
    @Override
    public final CoverContent load(CoverRequest request) {
        CoverContent content = doLoad(request);
        validateImage(content);
        return content;
    }
    protected abstract CoverContent doLoad(CoverRequest request);
    protected void validateImage(CoverContent content) {
        // 共用文件大小、实际 MIME、图片解码和像素限制
    }
}
```

统一模型：

- `Resource`：ID、类型（AUDIO/LYRICS/COVER）、来源 Provider、原始文件名、可信内部路径、相对导入路径、大小、SHA-256、创建/到期时间。
- `AudioMetadata`：可读写标签及原始字段、音频技术属性、现有歌词/封面描述；不把通用模型限制为某个库的 Tag 类型。
- `LyricsContent`：原始文本、字符编码、解析后的时间戳行、来源与可选置信度。
- `CoverContent`：实际图片类型、二进制/流来源、宽高、哈希。
- `TagWritePlan`：各字段 `KEEP/SET/REMOVE` 三态、歌词和封面的保留/替换/删除、原始资源与版本引用。
- `ProcessingPlan`：不可变快照；批量时每首歌都有独立 `TagWritePlan`。
- `ProcessingResult`：阶段、状态、错误码、安全的错误说明、技术日志关联 ID、成品/版本 ID、校验和备份状态。

**注意：**原件保护、临时文件创建、版本管理与校验归 `ProcessingOrchestrator`，而不是塞入格式处理器父类。

### 4.4 自动注册示意

```java
@Component
public final class AudioTagHandlerRegistry {
    private final Map<String, AudioTagHandler> handlers;
    public AudioTagHandlerRegistry(List<AudioTagHandler> implementations) {
        this.handlers = implementations.stream()
            .collect(Collectors.toUnmodifiableMap(
                it -> it.format().toLowerCase(Locale.ROOT),
                Function.identity()));
    }
    public AudioTagHandler require(String format) {
        AudioTagHandler result = handlers.get(format.toLowerCase(Locale.ROOT));
        if (result == null) throw new UnsupportedAudioFormatException(format);
        return result;
    }
}
```

上例仅演示依赖注册；生产代码再加空值检查、启用配置过滤和错误信息。新增格式的一般步骤：新建类、注册 Spring Bean、追加配置及兼容性测试；**不修改中心 switch**。

## 5. 数据库架构最终选型

**V1 采用业务 Repository 接口 + MySQL 适配器 + MyBatis-Plus；不引入全能 `AbstractDatabase`。**

原因：关系数据库可共享 SQL/JDBC/Mapper 基础设施，但 MongoDB 文档模型、事务边界和查询语义并不等价。跨库真正稳定的是业务契约，不是底层 SQL 抽象类。

设计：

- 各业务模块 `domain/repository` 定义 `ResourceRepository`、`TaskRepository`、`MusicVersionRepository`、`BackupRecordRepository` 等接口。
- `persistence/mysql` 下存放 MyBatis-Plus entity、Mapper、类型转换器和各 Repository 的 MySQL 实现；这是 V1 唯一实际装配的持久化适配器。
- 日后 MySQL → PostgreSQL：评估复用关系型实现、SQL 方言、驱动、Flyway 迁移脚本；**若没有实质差异，不必为继承硬建空子类**。
- 日后 MongoDB：实现同样的业务 Repository 契约，独立维护文档映射、索引、事务与数据迁移；不能承诺“只改一行 YAML 零代码迁移”。
- 数据库选择使用 `music.persistence.provider: mysql` 和条件装配，每次仅启用一套主库适配器。连接配置、Mapper 扫描、迁移与事务管理要成套切换。

### 5.1 初步数据表（Flyway V1 按此落地）

| 表 | 主要字段 / 目的 |
|---|---|
| `music_resource` | `id, kind, provider, original_filename, relative_path, storage_path, sha256, byte_size, detected_format, created_at, expires_at, deleted_at`；管理音频/LRC/封面，不存文件二进制 |
| `music_metadata` | `id, resource_id, title, artist, album, album_artist, release_year, genre, track_number, duration_ms, sample_rate, bitrate, detected_lyrics, artwork_resource_id, created_at`；长歌词按受控字段或独立对象管理 |
| `processing_task` | `id, mode, status, total_count, success_count, failure_count, submitted_at, started_at, completed_at, report_path` |
| `task_item` | `id, task_id, audio_resource_id, lyrics_resource_id, cover_resource_id, match_reason, plan_json, status, current_stage, error_code, error_message, diagnostic_id, output_version_id`；支持匹配确认快照与分项失败 |
| `music_version` | `id, source_resource_id, parent_version_id, task_item_id, version_no, output_path, sha256, metadata_snapshot_json, created_at, expires_at, deleted_at`；新版本不覆盖旧版本 |
| `backup_record` | `id, version_id, provider, bucket, object_key, status, retry_count, last_error_code, last_error_message, last_attempt_at, completed_at`；关联歌词/封面对象键可用明细字段或单独明细表 |

对关键查询建立索引，如任务状态/时间、资源哈希、版本父子关系、备份状态/重试时间。主键策略由 Flyway 和 Java 映射统一约定，推荐 `BIGINT` 或 UUID，**同一实体全链路保持同类型**。数据库使用 `utf8mb4`；时间统一按 UTC 存储，界面本地化显示。严禁把凭据写入数据库记录和 JSON 报告。

### 5.2 JSON 与 MySQL 的一致性

MySQL 是任务、版本和备份状态的**唯一事实来源**。原先确认的 JSON 功能保留：每个任务完成时（及用户主动导出时）由数据库状态生成 `report.json`，记录成功/失败、资源路径或标识、错误分类、阶段和备份结果。JSON 是可重建快照/导出，不做第二套可独立更新的任务数据库。若生成失败，可从数据库重试导出。

### 5.3 文件与数据库不支持同一个原子事务

处理阶段：`PREPARED → WRITING → VALIDATING → PUBLISHING → SUCCEEDED/FAILED`，备份另外计状态。先在任务目录创建安全副本，完成写入与校验后，将成品原子移动至结果目录（同文件系统内优先）；再提交数据库状态。使用临时标记和启动恢复扫描，处理“文件已生成但 DB 未更新”等中断窗口。严禁把数据库事务当成 MinIO 或文件系统的事务。

## 6. 任务状态、错误分类与恢复

### 6.1 任务状态

任务：`UPLOADED → MATCHING → AWAITING_CONFIRMATION → QUEUED → RUNNING → SUCCESS/PARTIAL_SUCCESS/FAILED/CANCELLED`。每首 `task_item` 有独立状态及当前阶段。已确认计划必须冻结；失败歌曲可以根据原任务信息创建**新版本/新尝试**，不能覆盖旧记录。

备份独立状态：`NOT_REQUESTED/PENDING/RUNNING/SUCCESS/FAILED/RETRYING`。备份失败不得将已成功的本地标签写入改判为失败。

### 6.2 面向用户的错误码

| 错误码 | 分类 | 用户可见说明方向 |
|---|---|---|
| `UNSUPPORTED_FORMAT` | 格式 | 文件格式或所需标签字段暂不支持；原件安全保留 |
| `CORRUPT_AUDIO` | 输入文件 | 音频解析失败；提示检查源文件 |
| `INVALID_LRC` | 歌词 | 编码、文本内容或时间戳格式异常；给出可定位的行数 |
| `INVALID_COVER` | 封面 | 图片内容损坏、格式/尺寸超限 |
| `MATCH_CONFLICT` | 匹配 | 多候选或重复绑定，需要人工确认 |
| `TAG_WRITE_FAILED` | 标签写入 | 音频标签库写入失败，记录实际阶段和底层异常关联 ID |
| `VERIFY_FAILED` | 结果校验 | 写入后读取与预期不一致或音频完整性检测失败 |
| `FILE_IO_ERROR` | 文件系统 | 路径、读写权限或介质异常 |
| `INSUFFICIENT_STORAGE` | 存储 | 空间不足；未开始危险写入 |
| `PERSISTENCE_FAILED` | 数据库 | 记录保存失败，任务等待恢复检查 |
| `BACKUP_FAILED` | MinIO | 备份异常，可单独重试，本地成品不受影响 |
| `INTERNAL_ERROR` | 服务 | 未分类服务异常，仅向前端暴露安全描述和诊断 ID |

每项失败必须有 `errorCode + stage + resourceId + userMessage + diagnosticId`；服务端日志保存完整堆栈，界面不得暴露密钥和不必要的绝对路径。用户可查看批量成功/失败统计、单项详情、校验摘要及 ZIP 下载。

### 6.3 恢复策略

- 服务启动后扫描未终止任务和任务目录，对照数据库检查成品和校验结果，幂等恢复或标记需要人工重试。
- 每首歌曲独立隔离失败，批量部分失败不回滚其他已成功成品。
- 备份网络异常使用有限次数、带退避的重试；格式损坏、冲突等非瞬时错误不自动重试。

## 7. 音频格式与实际兼容性

- **MP3**：确认 ID3v2 标签、图片 APIC、歌词 USLT 中的 LRC 文本；同步歌词可另测 SYLT，但 V1 不保证所有播放器支持。
- **FLAC**：确认 Vorbis Comment 及 PICTURE 元数据块的读写；测试实际播放器能否解释内嵌 LRC 时间戳。
- **WAV（V1 必须）**：专项验证 RIFF INFO、WAV 内 ID3 标签的读取/写入行为和播放器兼容性；V1 最低门槛为能写入并重新读取预期歌词和封面，且音频仍可正常播放；如果选定库不能满足，先用可验证适配实现补齐再宣布支持。
- **其他格式**：M4A、OGG、Opus、AIFF、APE、WMA 等单独实验、逐项标注支持能力，不能用统一接口掩盖具体限制。
- **LRC**：文本文件，不等于随意的 `.txt` 一定是合法 LRC。默认 UTF-8，识别 BOM/常见编码时应明确告知；保留时间戳、行顺序及元信息。
- **封面**：V1 接受 JPEG/PNG；校验真实文件类型及可解码性；根据目标标签限制做必要图片格式转换，但不改变原上传封面。
- **无损**：修改标签不转码音频；整文件哈希必然变化，不将其当成音频一致性依据。必须准备实际 MP3、FLAC、WAV 测试样本和播放器验收矩阵。

## 8. 文件目录、保留及 MinIO 备份

```text
/srv/music-tagger/
  uploads/{resource-id}/...
  working/{task-id}/...
  outputs/{music-id}/{version-id}/...
  reports/{task-id}/report.json
  logs/...
```

- `uploads`、`outputs` 和 `reports` 均为 Docker 卷或宿主机挂载的持久化目录；禁止存入镜像层/项目 Git 目录。
- 文件名仅用于显示和匹配，服务端存储路径始终由 ID 生成；验证文件真实类型、大小、相对路径和目录穿越风险。
- 原始文件自上传完成起 30 天，成品自生成起各自 30 天；任务工作副本结束后清理，失败时按诊断需求短期保留但不得无限增长。
- MySQL 历史记录长期保存；本地文件清理后将资源标记 `deleted_at`，下载按钮失效但保留历史元数据和失败原因。
- 如果用户启用 MinIO 备份且备份未成功，到期时**暂缓清理该文件并提示**；当磁盘接近阈值时暂停上传并告警，不能无限无提示保留至磁盘耗尽。MinIO 备份对象不做自动过期，由用户自行管理。
- MinIO 归档建议：`music/{yyyy}/{MM}/{task-id}/processed/`、`lyrics/`、`covers/`、`manifest.json`；每对象保存哈希和源资源引用。MinIO 若部署在同一物理盘上，不属于硬件故障意义上的异地备份。

## 9. 后端建议目录：按业务模块而不是全局三层

```text
src/main/java/com/example/musictagger/
├── MusicTaggerApplication.java
├── common/                     # 通用异常/响应/配置/安全工具；不塞业务 Mapper
│   ├── exception/
│   ├── response/
│   └── config/
├── resource/
│   ├── controller/
│   ├── application/
│   ├── domain/                 # Resource + ResourceProvider + ResourceRepository
│   └── infrastructure/         # AbstractResourceProvider + 本地上传
├── matching/
│   ├── application/
│   ├── domain/
│   └── infrastructure/
├── lyrics/
│   ├── application/
│   ├── domain/                 # LyricsProvider、LyricsContent、解析规则
│   └── infrastructure/         # AbstractLyricsProvider、LocalLrcLyricsProvider
├── artwork/
│   ├── application/
│   ├── domain/                 # CoverProvider、CoverContent
│   └── infrastructure/         # AbstractCoverProvider、LocalCoverProvider、ArtworkProcessor
├── tagging/
│   ├── controller/
│   ├── application/            # TaggingService、ProcessingOrchestrator
│   ├── domain/                 # AudioTagHandler、TagWritePlan、AudioMetadata
│   └── infrastructure/         # MP3、FLAC、WAV Handler 与自动注册表
├── task/
│   ├── controller/
│   ├── application/
│   └── domain/                 # Task、TaskItem、TaskRepository
├── version/
│   ├── application/
│   └── domain/                 # MusicVersion、MusicVersionRepository
├── backup/
│   ├── controller/
│   ├── application/
│   ├── domain/                 # BackupProvider、BackupRecordRepository
│   └── infrastructure/         # MinIO 实现
├── persistence/
│   └── mysql/
│       ├── entity/
│       ├── mapper/
│       ├── converter/
│       └── repository/         # 实现各业务模块的 Repository 接口
└── web/
    └── controller/            # Thymeleaf 页面路由

src/main/resources/
├── templates/
├── static/
├── db/migration/              # Flyway
└── application.yml

tests/fixtures/                # 真实测试音乐/LRC/图片（仅可合法使用的样本）
Dockerfile                    # 多阶段构建，固定 Java 17 运行环境
.dockerignore
compose.yaml                  # M710q 部署，使用已有 MySQL
deploy/k8s/                  # 后续阶段才创建实际 K8s 清单
README.md
```

没有业务意义的包不必提前创建。模块依赖：`controller → application → domain interfaces ← infrastructure`；模块间通过公开服务、领域模型与接口协作，禁止跨模块直接访问对方的 Mapper。

## 10. 配置范例（仅为约定，密钥由环境注入）

```yaml
music:
  tagging:
    formats:
      mp3: {enabled: true}
      flac: {enabled: true}
      wav: {enabled: true}
  resource:
    default-provider: local
    local: {enabled: true}
    url: {enabled: false}       # 保留约定，V1 不实现 Bean
  lyrics:
    default-provider: local
    local: {enabled: true}
    ai: {enabled: false}        # 保留约定，V1 不接大模型
  cover:
    default-provider: local
    local: {enabled: true}
    url: {enabled: false}
    ai: {enabled: false}
  persistence:
    provider: mysql
  backup:
    provider: minio
    enabled: ${MUSIC_BACKUP_ENABLED:false}
    endpoint: ${MINIO_ENDPOINT:}
    bucket: ${MINIO_BUCKET:music-backup}
  file:
    data-root: /srv/music-tagger
    retention-days: 30
    pause-upload-below-free-gb: 10
  processing:
    concurrent-batches: 1
    concurrent-files: 2
    max-upload-mb: 500

spring:
  datasource:
    url: ${MYSQL_URL}
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
  flyway:
    enabled: true
```

MinIO `access-key`/`secret-key` 与其他密钥只通过环境变量或安全凭据注入；配置不允许因关闭备份而强制要求填写 MinIO 连接信息。配置键改变应有迁移说明。

## 11. API 草案

| 方法与路径 | 用途 |
|---|---|
| `POST /api/resources/upload` | 单/多文件上传、格式校验，返回资源 ID |
| `POST /api/imports/match` | 对一批资源生成匹配预览；无文件写入 |
| `PUT /api/imports/{id}/plan` | 人工修正并保存待执行方案 |
| `POST /api/imports/{id}/confirm` | 冻结方案、创建执行任务 |
| `GET /api/resources/{id}/metadata` | 查看现有音频标签及能力 |
| `POST /api/tasks/single` | 单曲处理，同一编排器 |
| `GET /api/tasks/{id}` | 任务概况、进度、详细错误 |
| `GET /api/tasks/{id}/items` | 批量单项结果 |
| `GET /api/versions/{id}/download` | 下载某一成品版本 |
| `GET /api/tasks/{id}/download` | 成功成品和报告打包 ZIP |
| `GET /api/tasks/{id}/report` | 下载 JSON 报告 |
| `POST /api/tasks/{id}/backup/retry` | 备份失败后手动重试 |
| `GET /api/history` | 查询历史任务和版本 |

HTTP 请求响应应使用明确 DTO 和校验；禁止暴露服务器绝对路径给前端。异步任务使用任务 ID 轮询即可，V1 不必引入 WebSocket。

## 12. M710q 部署、Docker 镜像与 Kubernetes 兼容性

### 12.1 唯一的 V1 目标部署环境

目标宿主机为用户自己的 **Lenovo M710q**（此前记录：i7-6700T、32 GB 内存、256 GB 硬盘、Ubuntu 24.04；曾使用内网 IP `192.168.2.2`）。**部署前核实当前 IP、剩余磁盘、CPU 架构、Docker、既有 MySQL 和可选 MinIO 的实际运行状态**；不能将历史 IP 或容器网络设置当成已确认的实时配置。

V1 交付必须包含能够独立构建、打标签、推送和运行的 **Linux/amd64 Docker 镜像**；默认在 M710q 上通过 **Docker Compose** 运行 Java 应用，不要求宿主机直接安装 Java。Mac 用浏览器访问 M710q 暴露的服务端口，浏览器上传的音乐实际存储在 M710q 的持久化数据盘中。

**MySQL 使用 M710q 上已经运行的实例**，由 `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_DATABASE`、`MYSQL_USER`、`MYSQL_PASSWORD` 配置；确认已有 MySQL 的实际宿主机/容器网络及连接方式。V1 的默认 `compose.yaml` **只启动 music-tagger 应用，不再自动创建第二份 MySQL**；如将来需要一体化部署，可另建独立的 Compose override/示例，而不能覆盖现有数据库。Flyway 仅在项目专用数据库上执行受版本管理的迁移，禁止修改其他项目的库。数据库用户按最小权限创建，密码通过环境文件或 Docker Secret 注入，`.env` 不提交 Git。

MinIO 同样优先复用 M710q 上已有实例；未确认现有实例或未开启备份时不得强制启动第二个 MinIO。要区分容器内访问地址和 Mac 浏览器访问地址：容器中的 `localhost` 指向当前容器自身，不等于 M710q 宿主机或另一个容器。

### 12.2 Docker 镜像与 Compose 验收

- 根目录交付多阶段 `Dockerfile`、`.dockerignore`、`compose.yaml`、`.env.example`，并在 README 中写明构建、启动、停止、查看日志、升级、健康检查和回滚命令；若已有镜像仓库，镜像地址和版本标签由配置传入，不写死个人账号。
- 构建阶段使用锁定版本的 Maven + JDK 17，运行阶段使用精简的 Java 17 JRE 镜像；优先非 root 用户、固定镜像版本、合理的 JVM 内存限制及 HTTP 健康检查，避免将密钥、音乐样本和生成文件打进镜像。
- `compose.yaml` 将 `/srv/music-tagger/uploads`、`outputs`、`reports`、`logs` 等映射到宿主机持久化目录；`working` 也应具备可恢复所需的持久化能力。容器重建、镜像升级不会删除音乐、任务报告或既有数据库。
- 应用启动时先验证 MySQL 连接与数据库迁移；数据库暂时不可用必须明确显示启动或健康状态异常，不静默回退到其他数据库。区分存活检查与就绪检查；处理中的任务在重启后通过数据库和磁盘状态恢复。
- V1 在目标 M710q 上完成 `docker compose up -d`、上传 → 标签处理 → 下载、数据库持久化、重启恢复、30 天清理及 MinIO 开关的端到端验证；所有部署资源和镜像架构应适配宿主机。
- Java 堆内存初始 512 MB～1 GB，最多 1 个批次、2 首并发；配置可调。根据实际文件规模监测磁盘、堆和响应时间。
- 任务开始前估算原件、工作副本、成品和可能的同机备份占用；磁盘空间不足应在开始前拒绝。
- 仅在可信内网开放；将来外网访问再添加认证、HTTPS、限流和 URL 获取防 SSRF 校验。
- 上传校验实际 MIME/格式、最大大小、解压/ZIP 安全、目录穿越和文件权限；所有异常日志脱敏。

### 12.3 为将来的 Kubernetes 部署保留兼容性（V1 不强制上 K8s）

- **同一份 Docker 镜像**将来可在 Kubernetes Deployment 中运行；业务代码不得依赖 Docker Compose 的特定主机名、容器 ID 或硬编码的宿主机 IP。使用环境变量、ConfigMap/Secret 思路管理差异，并为之后交付 K8s manifest 或 Helm chart 预留目录 `deploy/k8s/`。
- 镜像保持无状态：音乐上传、成品、需要恢复的临时工作文件和报告存储在外部持久化卷；MySQL、MinIO 使用外部服务地址。未来在 K8s 中用 PVC 或 NAS 挂载对应数据目录，不将持久文件写在 Pod 临时文件系统或镜像层中。
- V1 是单实例任务执行：未来直接把 Deployment 扩成多个副本可能导致重复执行、共享目录冲突和清理任务竞争。**在实现数据库任务领取/租约、并发幂等、共享存储和分布式清理锁之前，K8s 维持 `replicas: 1`**；就绪探针、优雅停机和任务恢复需真实测试。
- PostgreSQL/MongoDB 替换只涉及预留的 Repository 适配器设计，与是否部署在 Docker/K8s 无关；**容器化并不意味着数据库可自动迁移**。
- K8s 是后续部署实验阶段而非 V1 必交的集群，现阶段必须保证代码/镜像兼容，并在 README 中说明未来迁移前提；不为此引入微服务、Redis 或消息队列。

## 13. 按阶段实施与验收

### 阶段 0：技术验证（不可跳过）
- 锁定 Java 17 可用的标签库版本及构建依赖。
- 用真实 MP3、FLAC、WAV 样本验证读取、封面写入、LRC 写入、重新读取及播放；WAV 特别记录 ID3/RIFF INFO 的实际能力。
- 形成格式能力矩阵和失败样本列表；若 WAV 关键字段无法满足，先提出适配方案及权衡，不能直接声称完成。

### 阶段 1：单曲最小闭环
- 搭建模块化单体、Flyway 首版建表、MySQL Repository、本地上传、抽象类与自动注册表。
- 完成读取、差异预览、单曲编辑、工作副本写入、校验、新版本保存、数据库记录、下载及 JSON 报告。
- 验收：只改一个字段时其他标签不丢；原件 SHA-256 不变；每次编辑有新版本；格式失败时有可定位的错误信息。

### 阶段 2：批量导入
- 多文件与目录导入、强确定性匹配、冲突/未匹配单独展示、人工确认和任务快照。
- 有界并发、部分成功、进度查看、ZIP 下载、失败重试。
- 验收：同名不同版本不误配；刷新后仍可恢复待确认任务；单曲失败不阻断其他歌曲。

### 阶段 3：MinIO 与生命周期
- MinIO 可选备份、独立状态及失败重试、30 天清理、备份失败暂缓、磁盘阈值保护、启动恢复扫描。
- 验收：MinIO 故障不影响本地成功状态；恢复后可补备份；DB 记录与清理后的文件状态一致。
- 交付镜像和 M710q `compose.yaml`，连接已有 MySQL；容器删除重建后数据仍可用，记录镜像版本与回滚过程。

### 阶段 4：兼容性扩展
- 测试 M4A、OGG、Opus 等格式并记录能力矩阵；仅对通过真实验证的格式开放入口。
- 根据实际需要追加 Provider 或适配器。暂不实现 URL/AI。

## 14. 交付完成标准

- 单曲和批量页面能实际操作；结果含成功/失败、具体阶段、错误码与修复提示。
- 所有宣传支持的格式都通过读写往返和音频可播放测试，WAV 单独验收；同步滚动歌词的播放器兼容性须单列说明。
- 原文件不被覆盖，编辑生成独立版本，下载和 JSON 报告正常。
- MySQL 是唯一任务事实来源；JSON 可从数据库重新导出；MinIO 关闭时不影响正常流程。
- 30 天清理、备份失败暂缓、磁盘保护、异常恢复经过自动化或可复现的手工测试。
- 新增一个示例格式/资源来源时，不需修改原有任务编排和中心 switch，仅扩展接口/子类、Bean 注册、配置及测试。
- Docker 镜像可在 M710q 上复现构建和运行，Compose 连接 M710q 既有 MySQL、复用可选 MinIO，数据挂载独立于容器生命周期；K8s 兼容边界已说明，后续保留单副本迁移路径。
