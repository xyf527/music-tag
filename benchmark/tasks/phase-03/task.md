# Phase 03 — MinIO、生命周期与 M710q 部署

## 目标

在 Phase 02 已通过的单曲和批量闭环上，完成可选 MinIO 备份、30 天文件生命周期、磁盘阈值保护、启动恢复扫描，以及可通过 Alibaba Cloud Toolkit 触发的 M710q Docker Compose 部署和回滚流程。

本阶段只有 Codex 继续开发。Claude 已淘汰，不得读取、搜索、比较、引用或复制 Claude 的目录、分支、提交、报告、会话和实现。

## 固定起点

- Branch: `agent/codex`
- Accepted Implementation: `de9a205b113cff3f407eeaf64a225d64cb8be330`
- Baseline Tag: `benchmark/phase-03-codex-base`
- Java: 17
- Spring Boot: 3.x
- Target: Linux/amd64 M710q
- Candidate Model: `gpt-6.1-sol`
- Reasoning Effort: `low`

当前工作区允许存在用户未提交的私密 `src/main/resources/application.yml` 修改。必须原样保留，不得暂存、覆盖、还原、提交或输出其中的值。

## 必须完成

### 1. 可选 MinIO 备份

- MinIO 默认关闭；关闭时应用不得要求 endpoint、access key、secret key 或 bucket。
- 本地处理、写后验证和数据库发布成功后，再执行独立的 MinIO 备份；MinIO 失败不得把本地成功改成失败。
- 备份成品音频、关联歌词、封面及 manifest；对象键稳定、安全、可追踪，包含哈希和资源/任务/版本引用。
- 建议对象前缀：`music/{yyyy}/{MM}/{task-id}/processed/`、`lyrics/`、`covers/`、`manifest.json`。
- 备份状态独立持久化，至少区分未启用、待处理、进行中、成功、失败；记录错误码、重试次数、下次重试时间和对象键，不记录 Secret。
- 支持幂等重试；已经成功且哈希一致的对象不得重复产生不同副本。
- bucket 或顶级前缀通过配置提供；不得操作其他项目的 bucket/前缀。
- MinIO 对象不实施自动过期，由用户管理。

### 2. 文件生命周期

- 上传原件和成品自各自创建时起默认保留 30 天，期限可配置。
- 工作副本在任务结束后尽快清理；中断恢复所需的工作文件保留到恢复判定完成。
- JSON 报告和数据库历史长期保留。
- 已启用 MinIO 且应备份的文件在备份失败时暂缓本地清理，并在页面和状态接口显示原因。
- 清理任务必须可重复执行、支持 dry-run、记录删除/跳过/失败数量；不能跟随符号链接或越过配置存储根目录。
- 清理文件后更新数据库文件状态；数据库与磁盘不一致时不得伪装文件仍可下载。
- 测试使用可注入时钟，不依赖真实等待 30 天。

### 3. 磁盘保护与启动恢复

- 配置最小剩余字节数和/或最小剩余百分比；达到阈值时暂停新上传，已有下载和状态查询继续工作。
- 页面返回明确中文告警，不暴露绝对路径。
- 启动时扫描数据库、工作目录、成品、报告和临时发布标记，处理“文件已生成但数据库未更新”等中断窗口。
- 恢复操作必须幂等；无法自动判断时标记为需要人工处理，不得删除可能仍有价值的文件。
- 单实例运行；明确记录未来多副本需要租约、分布式锁和共享存储，本阶段不实现多副本。

### 4. 健康、版本与运维接口

- 提供不泄密的 liveness、readiness 和版本信息；readiness 检查数据库、存储目录及必要配置，MinIO 关闭时不检查 MinIO。
- 版本信息至少包含 Git commit、构建时间和分支/版本标识，通过构建参数注入，不硬编码个人信息。
- 日志不得打印数据库或 MinIO Secret、完整连接串及个人服务器路径。

### 5. Docker 与 Compose

- 提供多阶段 `Dockerfile` 和 `.dockerignore`，生成 Linux/amd64 可运行镜像；运行阶段使用非 root 用户。
- 提供 `compose.yaml`，默认只启动 music-tag 应用并连接 M710q 已有 MySQL，不启动第二个 MySQL。
- 提供可选的 MinIO Compose override/profile；若用户已有 MinIO，可以只配置外部 endpoint，禁止强制启动第二实例。
- 容器内部监听 8080，宿主机默认映射 18081；端口可配置。
- 上传、工作、成品、报告和日志必须挂载到宿主机持久化目录，容器重建后仍存在。
- MySQL/MinIO 地址必须由环境变量注入。容器内的 `localhost` 只代表应用容器自身；连接宿主机已发布端口时使用可配置 host gateway，连接同一 Compose 网络时使用服务名。
- 提供 `.env.example`，只含无效占位符和说明，不含真实地址或凭据。
- Compose 和脚本不得使用 `docker compose down -v`，不得删除数据库或持久化目录。

### 6. M710q 部署与回滚入口

- 在 `deploy/m710q/` 提供可审查的预检、部署、健康检查和回滚脚本，供 Alibaba Cloud Toolkit 上传后调用。
- 脚本以自身目录或环境变量解析部署根目录，不在 Git 中硬编码个人绝对路径。
- 部署前检查 Linux/amd64、Docker/Compose、磁盘、必要环境文件、目标端口和目录权限。
- 若 18081 被非本项目进程占用，停止并报告；若由当前项目旧容器占用，允许原地升级。
- 保留上一可运行镜像/版本和部署元数据；迁移或健康检查失败时恢复上一版本。
- 部署后验证 Compose 状态、健康接口、数据库迁移、持久化目录可写和版本 commit。
- 提供 Alibaba Cloud Toolkit `Deploy to Host` 配置文档：本地构建/上传内容、目标目录、After deploy 命令、服务器私密环境文件位置和首次部署步骤。

### 7. 测试与验收

自动测试至少覆盖：

- MinIO 关闭时完整业务不受影响。
- MinIO 成功、失败、本地成功不回滚、幂等重试和对象键隔离。
- 30 天边界、备份失败暂缓、dry-run、符号链接与路径边界。
- 磁盘阈值阻止上传但允许读取和下载。
- 启动恢复的主要中断窗口和重复执行。
- 健康接口不泄露 Secret。
- Docker/Compose 配置渲染、非 root、挂载、端口和外部 MySQL/MinIO 配置。
- 部署脚本预检、失败停止和回滚路径；不得真实删除数据库、volume 或持久化目录。

必须实际运行 Java 17：

```text
mvn clean verify
mvn package
git diff --check
```

如果本机有 Docker，可验证镜像构建和 Compose 配置；不得为了测试在 Mac 启动 MySQL 或长期运行 MinIO。M710q 上的真实 Docker、MySQL、MinIO 与重启恢复验收可标记 `PENDING USER EXECUTION`，但不得因此停止代码、测试、文档或提交。

## 明确不做

- 不实现 Kubernetes 清单、Helm、多副本、Redis、MQ、微服务、身份系统、URL 下载、第三方抓取或 AI。
- 不让 MinIO 成为本地处理成功的前置条件。
- 不删除或重建用户已有数据库。

## 人工验收

1. MinIO 关闭时完成单曲和批量流程。
2. 开启 MinIO 后处理 MP3/FLAC，确认对象、manifest、哈希和数据库状态。
3. 暂停 MinIO，确认本地处理仍成功且备份失败可重试；恢复后重试成功。
4. 使用可控测试时间执行清理 dry-run 和正式清理，确认 30 天边界及备份失败暂缓。
5. 模拟磁盘阈值，确认上传暂停、下载和查询仍可用。
6. 通过 Alibaba Cloud Toolkit 上传部署包并调用部署脚本，在 M710q 完成 Compose 更新。
7. 删除并重建应用容器，确认数据库、上传、成品和报告仍存在。
8. 验证 18081 页面、健康接口、版本 commit、日志、回滚和重启恢复。

## 完成与停止条件

- 在 `agent/codex` 提交全部 Phase 03 修改，除用户私密配置外工作区保持干净。
- 最终回复最前面不超过 8 行：状态、提交、测试、镜像/Compose、MinIO、M710q、人工产物和已知限制。
- 未运行写 `NOT RUN`，失败写 `FAILED`，禁止把部分完成写成通过。
- 完成后停止，不自行 push、部署、合并、rebase 或开始 Phase 04。
