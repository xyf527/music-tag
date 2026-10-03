# Phase 02 批量导入

首页的“批量导入”入口进入中文工作台。可分别选择多个文件或目录；目录上传保留经过校验的相对路径。可用任务 ID 或页面 URL 恢复草稿和进度。

## 匹配与确认

同目录、完整基名匹配优先；没有精确歌词候选时，要求音频标题和歌手同时与 LRC 的 ti/ar 标签一致。匹配会统一检查所有音频的候选占用情况：多候选、重复占用、目录歧义或标签冲突均须人工确认，不会让 MP3 和同名 FLAC 共享附件。

每首音频的歌词、封面下拉框可绑定、改绑或选“无绑定”解绑。被其他音频占用的附件必须先解绑。确认时锁定草稿并保存 ProcessingPlan；确认后 API 和页面均拒绝修改绑定。执行使用保存的音频 ID、歌词文本、封面资源 ID 和校验摘要；不再匹配。原始附件内容发生变化时明确失败。

## 执行与下载

全局固定线程池逐项执行，默认 2，最大 4。单项失败独立记录错误码、诊断 ID 和中文提示；其他项目继续处理。FAILED 和 INTERRUPTED 可重试，成功项目保持原版本。版本表的批项唯一键保证已发布版本在重启恢复和重试时被复用。启动恢复会辨认已发布结果，并把仍在处理或等待的执行项标记为中断。

ZIP 使用 StreamingResponseBody，逐个从受控成品路径复制到响应流。稳定的 item-ID 文件名防止路径穿越和重名。report.json 来自持久化任务事实，与单独报告 API 一致。失败项不会作为成品进入 ZIP。

## 配置与 API

| 环境变量 | 默认值 |
|---|---:|
| MUSIC_BATCH_CONCURRENCY | 2（最大 4） |
| MUSIC_MAX_BATCH_FILES | 500 |
| MUSIC_MAX_BATCH_BYTES | 2147483648 |
| MUSIC_MAX_UPLOAD_BYTES | 524288000 |
| MUSIC_MAX_REQUEST_BYTES | 576716800 |

工作台显示批次数量、总大小、单音频上限和并发。HTTP multipart 总请求上限独立生效；处理更大批次时需同时配置请求上限。超限、路径异常和空文件有中文反馈。动态显示使用 DOM/textContent。

| API | 用途 |
|---|---|
| GET /api/batches/policy | 限制和并发 |
| POST /api/batches | files + paths multipart 导入 |
| GET /api/batches/{id} | 恢复任务及进度 |
| POST /api/batches/{id}/items/{itemId}/bindings | JSON：lyricsItemId、coverItemId，null 表示解绑 |
| POST /api/batches/{id}/confirm | skipIds 参数，保存不可变计划 |
| POST /api/batches/{id}/execute | 执行确认计划 |
| POST /api/batches/{id}/retry | 仅重试失败和中断项 |
| GET /api/batches/{id}/zip | 流式 ZIP |
| GET /api/batches/{id}/report | 持久化事实报告 |

新增 V4，V1/V2/V3 保持原字节；数据库和已有记录均保留。V4 之前的实验性旧格式计划会被明确拒绝执行，应重新导入确认，避免静默改变计划。

## 本轮逐项验证

| 要求 | 证据 | 结果 |
|---|---|---|
| 1. 唯一 LRC 标题/歌手匹配 | 唯一、多歌词、多音频、缺歌手和标签冲突回归 | 通过 |
| 2. 人工绑定/改绑/解绑 | HTTP API、中文下拉框、重复绑定及跨批次拒绝 | 通过 |
| 3. 不可变快照 | 确认后修改绑定行和歌词内容，执行仍写入原快照；确认与绑定 API 锁定 | 通过 |
| 4. 单项有界并发 | 同时启动证明达到 2/4 并发；配置 99 被限制为 4；独立失败隔离 | 通过 |
| 5. 重试和成功幂等 | 缺失原文件失败后恢复重试；中断恢复；已发布版本复用，版本数量不增加 | 通过 |
| 6. 流式 ZIP | MockMvc asyncStarted/asyncDispatch；真实浏览器下载；内容和报告逐项比对 | 通过 |
| 7. 双文件入口 | 实际多选、实际目录上传；目录 multipart 文件名兼容回归 | 通过 |
| 8. 页面安全 | 运行实际 JS，危险路径、字段和错误文本不能生成 HTML 元素 | 通过 |
| 9. 完整回归 | Java 17 clean verify、package；真实 MySQL 批处理专项 | 通过 |
| 10. 增量迁移 | 仅 V4；已有 V3 数据升级单测；真实 MySQL V1–V4 历史验证 | 通过 |

Java 17 的 mvn clean verify 和 mvn package 都执行完整本地测试：22 项通过。需要注入凭据的 MySQL 专项在默认运行中跳过，另通过包装脚本实际执行 11 项并全部通过，不将 H2 结果作为 MySQL 证据。git diff --check 通过。

浏览器实际执行了上传、冲突展示、人工绑定/解绑、确认、两项成功处理、刷新恢复、ZIP 下载、目录选择，以及重启服务后恢复已完成任务。桌面和移动视口截图保存在忽略的 output/playwright/ 中。

验收样本为 ffmpeg 原创正弦波音频、自绘或纯色图片和原创歌词。仓库外保留成功 MP3/FLAC、完整和部分失败 ZIP、对应 JSON 报告；测试生成物不提交。生成产物可运行：

```sh
mvn clean verify -Dphase02.artifacts.dir="$ARTIFACT_DIR"
mvn package
git diff --check
```

真实 MySQL 使用既有本机私密配置：

```sh
scripts/with-m710q-mysql.sh codex test -- mvn test \
  -Dtest=MySqlPhase02IntegrationTest \
  '-DargLine=-DsocksProxyHost= -Djava.net.useSystemProxies=false'
```

本轮起点 9aa854b，分支 agent/codex。用户请求的运行配置为 gpt-6.1-sol / low；运行端的模型身份和推理元数据未核验（NOT VERIFIED）。未访问另一候选材料，未更改 Benchmark/Judge，私密 application.yml 未纳入提交。
