# Candidate Completion Report — Phase 04

## 身份与运行配置

- Candidate: Codex
- Harness: Codex CLI
- Model: 固定请求 `gpt-6.1-sol`；独立运行时遥测 `NOT VERIFIED`
- Reasoning Effort: 固定请求 `low`；独立运行时遥测 `NOT VERIFIED`
- Branch: `agent/codex`
- Worktree: 当前独立候选仓库，实际路径核对一致
- Task SHA-256: `a0ef802bddf466f69543d77eb84e00180b26c28bdc8c9426d2ea0da816f758f9`

## Git

- Before Commit: `3cc219780659353f5f6d029aea1a99b5fd068ef1`
- Baseline tag: `benchmark/phase-04-codex-base`，值为 `9195d756578ed27a034bb5f19575b12d1a5bbfe2`，祖先核对成功
- After Implementation / Release Commit: `615a55ab1f565ecfce1752f5c5a487cbc3ef766a`
- 最终交付 HEAD: 本报告单独提交，实际 SHA 记录于最终回复；不猜测本报告自身 SHA。
- Working Tree: 实现和发布包完成后，仅保留用户原有 `application.yml` 与 `scripts/build-phase03-release.sh` 修改。未暂存、读取其中值、覆盖、还原或提交这两份文件。

## 状态

- Status: `COMPLETED` / `READY_FOR_JUDGE`（候选交付完成，不宣布 V1 最终通过）
- Start: `2026-10-04T06:52:31Z`（首次时间核验；此前已完成入口读取和路径/哈希核对）
- End of implementation / validation / release checks: `2026-10-04T07:28:50Z`
- Recorded Wall Clock Time: 36 分 19 秒；报告提交结束时间以最终 Git 提交时间为准。

## 实际能力矩阵

| 格式 | 读取/文字标签/独立成品 | 歌词 KEEP/SET/REMOVE | 封面 KEEP/SET/REMOVE | 自动音频证据 | 人工播放器 |
|---|---|---|---|---|---|
| MP3 | VERIFIED | VERIFIED | VERIFIED | 写后 PCM 一致，回归通过 | PENDING USER EXECUTION |
| FLAC | VERIFIED | VERIFIED | VERIFIED | 写后 PCM 一致，回归通过 | PENDING USER EXECUTION |
| WAV RIFF INFO | VERIFIED | UNSUPPORTED | UNSUPPORTED | 文字标签写后 PCM 一致；不支持操作明确拒绝 | PENDING USER EXECUTION |
| M4A AAC-LC | VERIFIED | VERIFIED | VERIFIED | 独立生成已有标签样本，三态写后重读/PCM 一致/原件不变 | PENDING USER EXECUTION |
| OGG Vorbis | VERIFIED | VERIFIED | VERIFIED | 独立生成已有标签样本，三态写后重读/PCM 一致/原件不变 | PENDING USER EXECUTION |
| Opus | UNSUPPORTED | UNSUPPORTED | UNSUPPORTED | 原创 Opus 可解码，但库真实读取/各写入操作失败，原件不变 | NOT RUN，业务未开放 |

M4A 只开放本次验证的 AAC-LC，ALAC 明确拒绝。OGG 检查 Vorbis packet，不把 OpusHead 当作 Vorbis。自动解码不是人类试听，也不证明所有容器变种或播放器显示。样本使用 FFmpeg 9.0.2 生成原创正弦波和原创纯色 PNG；本机缺少 libvorbis，改用内置 Vorbis 双声道编码后完成真实验证，未用另一种格式替代 OGG 验证。

## 实现摘要

新增 M4A、OGG 适配器和显式拒绝的 Opus Provider。内容识别放在各适配器，Registry 统一提供细粒度能力；业务编排没有新增格式 switch/if 链。单曲/批量上传、附件操作、预览、写后校验、成品下载和 ZIP 使用同一来源。封面使用嵌入数据哈希校验，文字/歌词同时验证 KEEP/SET/REMOVE；不支持动作不会静默忽略。

复用既有任务、版本、报告、备份、生命周期和恢复逻辑。新报告记录格式、实际执行动作、能力、输出哈希、写后重读结果及待人工播放器验收状态。发布日志保留这些证据，恢复报告可重建；历史日志证据不足时明确 UNKNOWN_RECOVERED/HASH_ONLY_RECOVERED，不伪造验证。没有数据库迁移，V1–V5 字节不变。

README、最终验收表、新 V1 构建入口已交付。新构建入口遵循 JAVA_HOME、检查已提交源码、使用固定公开文件清单，不调用或修改用户本机 Phase 03 脚本。Docker 构建添加 Maven 缓存，运行阶段仍为非 root Java 17 Linux/amd64。

## 实际验证

Maven 实际使用 Java 17.0.18；Docker 运行 Java 17。所有生成媒体、日志、提取 JAR 与发布 tar 仅在系统临时目录或忽略的仓库 artifact 目录。

| 命令或操作 | 结果 | 证据 |
|---|---|---|
| 路径/分支/HEAD/tag、`shasum -a 256 benchmark/tasks/phase-04/task.md` | PASSED | 分支及哈希一致，预期主控交接提交 |
| `git merge-base --is-ancestor benchmark/phase-04-codex-base HEAD` | PASSED | exit 0 |
| `mvn -Dtest=Phase04FormatEvidenceTest,AudioTagHandlerTest test` | PASSED | M4A/OGG/Opus 独立验证及已有格式回归 |
| `mvn -Dtest=Phase04WorkflowIntegrationTest test` | PASSED | 新格式版本链、原件保护、混合批次、失败重试、ZIP/JSON、备份状态与清理 |
| `mvn clean verify`（最终及发布入口） | PASSED | 62 tests，0 failures，0 errors，17 skipped，BUILD SUCCESS |
| `mvn package`（最终及发布入口） | PASSED | 同一测试结果，可执行 jar 成功生成 |
| `node --test src/test/js/*.test.cjs` | PASSED | 2 tests；批量安全渲染、单曲能力控制和文本转义 |
| `node --check` 两个页面脚本、`bash -n` V1/部署入口 | PASSED | exit 0 |
| `git diff --check`、暂存 diff 检查和 Secret/IP/私密路径/文件范围扫描 | PASSED | 未包含私密 YAML、本机脚本改动、媒体、凭据或生成物 |
| V1–V5 与 baseline 的 diff | PASSED | 无变化，无新迁移 |
| 默认 Compose 与可选 MinIO override `config --quiet` | PASSED | 只使用 .env.example 与无效测试凭据，未启动数据库或 MinIO |
| `docker buildx build --platform linux/amd64 --load ...` | PASSED | 最终源码镜像构建成功；固定平台只产生 lint warning |
| 镜像 inspect / 临时 `java -version` | PASSED | linux/amd64、10001:10001、8080/tcp、Java 17 |
| `JAVA_HOME=... bash scripts/build-v1-release.sh` | PASSED | 实际验证、打包、镜像导出和 V1 发布 tar 全部完成，无 push/deploy |
| 归档 `tar -tf` 与镜像 JAR 提取检查 | PASSED | 固定 11 项公开资源；无 .env/application.yml/媒体/测试 fixture；三种 Provider 类存在；未启动的临时检查容器已移除，镜像保留 |
| 包装脚本 `scripts/with-m710q-mysql.sh codex test -- true` | PASSED（沙箱外） | TCP 预检成功；沙箱内不可达后按权限要求重试 |
| 包装脚本运行 `mvn -Dtest=MySqlPhase04IntegrationTest test` | FAILED | 当时 4 项继承闭环测试全部因 Communications link failure 失败；后续新增恢复测试真实 MySQL NOT RUN；真实验收 PENDING USER EXECUTION |
| 真实 MinIO、M710q 升级/健康/回滚/容器重建、人工播放器 | NOT RUN | PENDING USER EXECUTION；没有执行真实部署 |

开发初次格式探测因当前 FFmpeg 不含 libvorbis 而 FAILED，确认编码器后改用原生 Vorbis，M4A/OGG/Opus 分别重新实际执行并通过公开结论测试。此前启动 Docker 的初次检查曾因 daemon 未启动/沙箱 socket 权限 FAILED，启动本机 OrbStack 并按权限要求执行后构建成功。未启动 Mac MySQL、Docker MySQL、Testcontainers MySQL 或 MinIO，也没有把真实环境失败改记为通过。

## 发布产物

- `deployment-artifacts/v1/music-tag-v1-release.tar`（约 348 MiB，Git 忽略）
- `deployment-artifacts/v1/image.tar` 和 `release.commit`（Git 忽略）
- 镜像：`music-tag:615a55ab1f565ecfce1752f5c5a487cbc3ef766a`
- 镜像构建 commit 同上，构建时间 `2026-10-04T07:27:08Z`，标识 `agent/codex`。
- 本报告追加提交不改变发布源码；升级核对 `release.commit`，不要把文档提交 SHA 当作已构建镜像 tag。

## 范围与完整性检查

- 未读取、搜索、比较或复制其他候选开发材料。
- 未修改 Benchmark、Judge、主控报告或评分规则。
- 自有 Java 源码和测试全部位于 `com.xin.musictag`，`src/test/` 正常跟踪。
- 不提交真实媒体、凭据、个人服务器路径、生成物；两份用户修改保留。
- 未实现转码、在线播放、抓取、认证、K8s、多副本或其他超范围能力。
- 未 push、部署、merge、rebase 或 cherry-pick，不宣布 V1 最终通过。

## 已知问题与人工验收

Opus 不支持业务读写；WAV 歌词/封面不支持；M4A/OGG 支持限于已验证编码与容器边界。自动解码通过不等于所有播放器显示通过。真实 MySQL 通信尚未通过，MinIO/M710q/播放器验收待用户执行；应用容器真实 readiness 也须在已有数据库环境复现，不能据构建成功声明部署通过。单实例、可信内网、无身份系统限制延续 Phase 03。

最终用户步骤与逐项状态见 [验收清单](Phase04-Acceptance.md)，部署入口字段参考 [阿里云官方说明](https://help.aliyun.com/document_detail/167889.html)。最终 V1 裁决由项目经理在用户人工验收后给出。

## 人工介入

- Level: 0（任务与环境权限授权，无额外代码或实现提示）
- Details: 独立实现、验证与发布包构建；权限审批仅用于 HTTP 临时端口、连接检测、Docker 构建/临时检查和 worktree Git 索引写入。
