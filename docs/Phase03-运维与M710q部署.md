# Phase 03 运维与 M710q 部署

本阶段是单实例。多副本需要任务租约、分布式锁、共享存储和备份/清理协调锁，本阶段未实现。应用没有身份系统，应在可信内网使用；运维 POST 接口也必须由部署网络控制访问。

## 存储与备份

`music.operations.minio-enabled` 默认 false，关闭时不需要任何 MinIO 参数，也不访问远程服务。开启时提供 endpoint、access-key、secret-key、bucket 和 prefix；对应环境变量为 `MUSIC_OPERATIONS_*`，Compose 使用 `.env` 的 `MINIO_*` 映射。bucket 需要用户提前创建，应用不会创建、删除 bucket，不设置任何 MinIO 生命周期，不操作配置前缀以外的对象。

本地标签校验、成品发布和数据库版本记录独立于备份。发布后只入队，定时器每分钟上传；启动时补建遗漏队列。状态在 `backup_job` 中，含 DISABLED/PENDING/RUNNING/SUCCEEDED/FAILED、attempts、error_code、next_retry、object_keys。失败退避重试最长一天，不限制总尝试次数；错误统一为不含地址和 Secret 的错误码。重启将 RUNNING 恢复为 PENDING。

对象键为 `{prefix}/{yyyy}/{MM}/{taskId}/version-{versionId}/{processed|lyrics|covers}/{sha256}`，manifest 在同级 `manifest.json`。manifest 包含资源、任务、版本引用及对象哈希、大小、键。单曲关联附件、批量实际绑定附件均备份；成品采用流式上传。SDK 先检查对象哈希元数据，一致时跳过上传。稳定键和 manifest 使部分成功后的重试不产生随机副本。对专属 bucket/prefix 授予 List/Get/Put 权限即可，不授予 Delete。不得配置 MinIO 自动过期规则。

`GET /api/tasks/{id}` 包含文件可用性和独立备份状态；`GET /api/tasks/{id}/backup` 和 `GET /api/batches/{id}/backup` 返回备份详情。`POST /api/versions/{id}/backup/retry` 立即重试指定版本。页面每五秒更新备份状态和备份失败暂缓清理说明。

## 生命周期、保护与恢复

`music.operations.retention` 默认 `30d`；原件使用资源创建时间、成品使用版本创建时间，各自到期清理。已开启备份时，未成功备份的版本及其原件/附件暂缓清理。活跃任务的原件及活跃批次的附件保留。报告与历史记录不自动删除；MinIO 对象不自动删除。工作目录在任务结束后清理；仍有发布日志的中断工作保留到恢复判断。

`POST /api/maintenance/cleanup?dryRun=true` 只统计，`dryRun=false` 正式执行，返回 deleted/skipped/failed（文件数量）。默认每小时执行正式清理。删除不跟随符号链接，拒绝存储根目录外路径；删除/缺失更新 file_status，下载进一步检查实际文件，状态接口不会提供不可用下载链接。批量附件保留数据库历史引用，实际读取会检查磁盘文件。重复执行安全。测试以注入 Clock 控制 30 天边界，不改变生产系统时间。

`music.operations.min-free-bytes` 和 `min-free-percent` 默认为 0，Compose 默认 1 GiB 和 5%。可用空间小于或等于任一阈值时，新单曲/批量上传暂停；查询、报告、成品下载继续运行。空间查询失败也暂停上传。中文告警不包含路径。上传入口检查是保护措施，不能预留整个并发上传容量，应保留足够余量。

发布前在 reports 持久化 `.publishing` 日志，再原子移动成品。启动验证日志中的成品哈希，补建版本、任务发布状态、批次项目及缺失报告；之后删除已恢复的日志。已有版本也校验哈希并修复发布关联。扫描成品、工作、报告和发布日志：孤立文件/无法确定的中断窗口登记 `recovery_issue`（只有哈希标识和错误码），保留文件，任务标记 NEEDS_REVIEW。重复恢复不创建重复版本或问题。缺失成品标记 MISSING。`GET /api/operations` 查询空间告警和人工恢复事项。人工检查应在服务器本地对照数据库路径，接口不输出路径。

## 健康和版本

- `GET /health/live`：进程存活。
- `GET /health/ready`：数据库、Flyway 成功状态、四个存储目录实际写入、必要备份配置；启用 MinIO 时检查 bucket 可达，关闭时不检查。
- `GET /api/version`：commit/buildTime/version，通过镜像构建参数 GIT_COMMIT/BUILD_TIME/BUILD_VERSION 注入；直接运行 jar 时用 MUSIC_BUILD_COMMIT/TIME/VERSION 环境变量。缺失值明确为 UNKNOWN。

私密 application.yml 不修改、不提交，且被 Maven resources 和 Docker build context 排除；发布包没有内置数据库配置。数据库与第三方音频库日志降低到 ERROR/OFF，异常/接口不记录 Secret 或服务器路径。

## Docker 与服务器准备

镜像使用 Java 17 多阶段构建，固定 Linux/amd64，运行 UID/GID 10001，8080 内部端口，宿主默认 18081。Compose 默认只启动应用，使用服务器已有 MySQL。`.env` 必须由服务器用户私下创建并 chmod 600；只把 `.env.example` 作为字段说明。

在服务器设置 `DEPLOY_ROOT`（部署资源目录）和 `DATA_ROOT`（独立持久化目录），创建 uploads/working/outputs/reports/logs 五个子目录并授权 UID/GID 10001 写入。不要把上传目录用于其他业务。安装 Docker Engine、Compose、curl、jq、ss、flock、GNU coreutils。配置现有数据库的专属应用账户、数据库和密码，预先备份已有数据库。Flyway 只升级表结构，不删除或重建数据库。

连接宿主机已发布的 MySQL 端口可设置 MYSQL_HOST=host.docker.internal（host-gateway），也可配置实际外部主机。连接同一 Compose 网络的服务使用服务名。容器中的 localhost 是应用容器自身。代码和 Git 只保存变量与无效示例。

已有 MinIO 时只填写外部 endpoint，禁止强制再启动实例。需要独立实例时，可人工执行 `docker compose --env-file .env -f compose.yaml -f compose.minio.yaml --profile minio up -d minio`，使用专属 MINIO_DATA_ROOT、凭据并创建 bucket；endpoint 在同网络中使用 `http://minio:9000`。自动部署脚本管理应用服务，MinIO profile 由用户管理。不要运行 `docker compose down -v` 或删除持久化目录。MinIO 无可用备份时本地成功不变，但 readiness 会提示依赖未就绪。

## Alibaba Cloud Toolkit Deploy to Host

按 [阿里云官方配置文档](https://help.aliyun.com/document_detail/167889.html) 在 Host 视图添加 M710q 并测试 SSH 连接，然后创建 Deploy to Host 运行配置。SSH 和地址只存在本机插件私密配置。

1. 本地选 Java 17，运行 `bash scripts/build-phase03-release.sh`。脚本验证、打包并构建 amd64 镜像，以完整 commit 为镜像 tag，将 image.tar、release.commit、Compose 和脚本组合为 `deployment-artifacts/music-tag-release.tar`。该目录忽略，构建脚本不推送和部署。当前 Docker 不可用时先运行 Maven 验证和打包，之后在有 Docker 的构建机执行此脚本。
2. Toolkit File 选择 Upload File，文件为上述 tar；Target Host 选已配置的 M710q；Target Directory 选私密 `DEPLOY_ROOT`。关闭平行运行。上传内容不包含 .env、application.yml、音乐、数据库或本机凭据。
3. After deploy 中使用自己的部署目录设置 DEPLOY_ROOT；执行下述命令（替换目录占位符，勿把真实目录提交）。首次部署前先准备私密 .env、持久化目录和权限。

```bash
export DEPLOY_ROOT=/REPLACE_WITH_DEPLOY_DIRECTORY
cd "$DEPLOY_ROOT"
tar -xf music-tag-release.tar
docker load -i image.tar
chmod +x deploy/m710q/*.sh
bash deploy/m710q/deploy.sh "$(cat release.commit)"
```

`.env` 的 MUSIC_TAG_IMAGE 设置为 `music-tag:<release.commit>`；每次上传新版本时更新该字段，不修改上一镜像。ENV_FILE 可指定部署目录之外、权限 600 的私密环境文件。Build 参数、上传 tag 和健康期望 commit 必须一致。

预检检查平台、Docker/Compose、环境文件、两种磁盘阈值、端口、目录及容器用户写权限。非本项目占用端口会失败；当前项目旧容器允许升级。部署通过 flock 串行执行，保存上一次已验证 image/commit 和部署历史到忽略目录 .deployment，不清理旧镜像。健康验证 Compose 运行状态、数据库迁移、持久化写入和 commit。升级/迁移/健康失败会尝试 `rollback.sh`；失败返回非零，即使回滚成功也不把本次部署伪装成功。

手动回滚：`bash deploy/m710q/rollback.sh`。它恢复 previous-image、验证 previous-commit，并保留数据库和文件。本次 V5 为追加表/列迁移，旧应用可继续读取；回滚不逆向删除新列。首次部署没有上一版本，只能报告失败并人工处理。今后破坏性迁移需要独立备份恢复方案。Toolkit 实际上传、首次容器启动与回滚验收：PENDING USER EXECUTION。

## 人工验收记录表（均需真实环境执行）

| 操作 | 预期 | 当前状态 |
|---|---|---|
| MinIO 关闭，单曲与批量 MP3/FLAC | 本地成功，备份 DISABLED | PENDING USER EXECUTION |
| 启用专属 bucket，处理带歌词/封面的文件 | 对象、manifest、哈希与数据库一致 | PENDING USER EXECUTION |
| 暂停 MinIO，再处理并重试，之后恢复 | 本地成功不回滚，FAILED 后 SUCCEEDED，稳定键 | PENDING USER EXECUTION |
| 用自动测试 Clock 执行边界与 dry-run；服务器清理 dry-run | 30 天边界准确，报告保留，失败备份暂缓 | PENDING USER EXECUTION |
| 将磁盘阈值设为高于剩余空间后重启 | 中文上传暂停，历史下载/查询可用 | PENDING USER EXECUTION |
| Toolkit 上传并 deploy.sh | Compose 升级，18081 页面与版本一致 | PENDING USER EXECUTION |
| `docker compose rm -sf music-tag` 后 `up -d music-tag` | 上传、成品、报告和外部 MySQL 历史仍在 | PENDING USER EXECUTION |
| 使用测试数据模拟发布日志/中断，重启两次 | 恢复幂等，不删除孤立文件 | PENDING USER EXECUTION |
| 错误镜像/错误 commit 触发部署失败 | 恢复上一版本，目录/数据库保留 | PENDING USER EXECUTION |

服务器验收日志应脱敏后保存；不要上传音乐或环境文件到 Git。真实 MySQL 测试仅经 `scripts/with-m710q-mysql.sh codex test -- ...`，不在 Mac 启动 MySQL/MinIO。
