# Phase 04 V1 升级与最终人工验收

本清单不表示已通过人工验收。所有真实 M710q、MinIO 和播放器操作均由用户执行并记录结果，当前为 PENDING USER EXECUTION。候选仅交付实现、自动验证和构建产物，不自动部署。

## 能力依据与限制

`Phase04FormatEvidenceTest` 对 M4A AAC-LC、OGG Vorbis 分别生成带已有标题/歌手/专辑的原创 2 秒正弦样本，复制成品，执行文字、歌词和封面 SET/KEEP/REMOVE，重读每项标签，比较写前/写后解码 PCM 和原件字节。Opus 独立生成 libopus 样本并确认可解码，但当前库真实读取失败，三态写入也失败且原件保持不变。Opus 全部业务能力明确 UNSUPPORTED，不提供复制成功或转码替代。

本机 FFmpeg 9.0.2 不含 libvorbis，使用内置 `vorbis`、双声道和 `-strict -2` 生成 OGG。该选项只用于测试素材生成，应用不会编码、转码或运行 FFmpeg。可在有 Java 17、Maven、FFmpeg（aac/vorbis/libopus/libmp3lame/flac/pcm_s16le/ALAC）的环境重现：`mvn -Dtest=Phase04FormatEvidenceTest,Phase04WorkflowIntegrationTest test`。测试源码被跟踪，生成音频/封面/报告不入 Git。

格式注册与能力来自同一 Provider Registry：单曲 policy、批量 policy、上传、预览、执行、附件绑定、下载扩展名均使用注册结果。M4A 只开放验证的 AAC-LC profile；ALAC 返回 UNSUPPORTED。OGG 识别 Vorbis identification packet，OpusHead 不作为 Vorbis 接收。WAV 保留 RIFF INFO 文字标签，歌词/封面继续不支持。

自动解码一致可证明测试样本音频未被标签修改破坏；不能代替所有编码变种、所有播放器的可用性和显示结论。

## 升级前

1. 在服务器备份现有 MySQL（由用户既有备份流程完成），确认持久化上传/成品/报告存在；不重建数据库。
2. 保留当前可运行镜像和 `.deployment/current-image`、current-commit；检查五个持久化子目录、UID/GID 10001 写权限、磁盘余量、18081 占用归属。
3. 私密 `.env` 留在服务器，权限 600；M4A/OGG 不需要新增环境参数。本阶段没有数据库变化，V1–V5 字节保持原样。
4. 本地 Java 17 运行 `bash scripts/build-v1-release.sh`；检查发布 tar 清单与 image metadata。用户原有本机 Phase 03 构建脚本保持原样，不参与 V1 包。
5. Toolkit Deploy to Host 使用 Upload File，上传 `deployment-artifacts/v1/music-tag-v1-release.tar`；主机和目录仅在插件私密配置中填写。官方字段说明见 [阿里云文档](https://help.aliyun.com/document_detail/167889.html)。

## After deploy 命令（目录占位符须自行替换）

```bash
export DEPLOY_ROOT=/REPLACE_WITH_DEPLOY_DIRECTORY
cd "$DEPLOY_ROOT"
tar -xf music-tag-v1-release.tar
docker load -i image.tar
# 在服务器私密环境文件中把 MUSIC_TAG_IMAGE 改为 music-tag:<release.commit>
chmod +x deploy/m710q/*.sh
bash deploy/m710q/deploy.sh "$(cat release.commit)"
```

新版本发布失败时脚本恢复上一已验证镜像；首次部署没有上一版本时明确失败。手动回滚使用 `bash deploy/m710q/rollback.sh`。不逆向删除迁移、不删除 MinIO、不清空 volume 或持久目录。不要同时运行多个应用实例。升级前 Phase 04 格式的数据库记录仍按通用资源/版本/任务表保存；旧镜像不支持新格式，回滚时应保留这些记录和成品，待恢复新版本后再处理/下载。

## 用户验收表

| 验收项 | 操作与预期 | 当前状态 |
|---|---|---|
| M4A AAC-LC | 上传带已有标签原件；SET/KEEP/REMOVE 标题/歌手/专辑、歌词/封面，下载重读，原件哈希不变 | PENDING USER EXECUTION |
| OGG Vorbis | 独立重复上述全部操作，不据 M4A 结果推断 OGG | PENDING USER EXECUTION |
| Opus | 上传 Opus 或把 Opus 改为 .ogg，分别明确 UNSUPPORTED/容器不匹配，不创建成功版本 | PENDING USER EXECUTION |
| WAV 与 M4A 边界 | WAV 附件/非 KEEP 操作提前提示与稳定拒绝；ALAC 或其他 AAC profile 不冒充已验证支持 | PENDING USER EXECUTION |
| 播放器 | 在用户实际播放器中试听 M4A/OGG 成品并检查文字、歌词及封面显示；记录播放器与版本、逐项结果 | PENDING USER EXECUTION |
| 混合批次 | MP3/FLAC/WAV/M4A/OGG/Opus、损坏文件、伪造扩展名，正确分类和部分成功；失败重试不重复已成功版本；ZIP/JSON 一致 | PENDING USER EXECUTION |
| MinIO 开/关/短暂停止 | 本地成功均不变；启用时新成品与实际绑定歌词/封面及 manifest 存在；失败后恢复幂等重试 | PENDING USER EXECUTION |
| 生命周期/磁盘 | 新格式按既有 30 天规则清理；备份失败保留；dry-run 不删；到期不可下载；低磁盘拒绝上传允许下载 | PENDING USER EXECUTION |
| Toolkit 升级 | readiness/live/version commit 正确，旧 MySQL 任务及持久文件保留，新页面能力矩阵一致 | PENDING USER EXECUTION |
| 重建/恢复/回滚 | 只重建应用容器（不删除挂载）；旧历史可下载；已有上一版本时模拟失败并恢复上一可运行版本 | PENDING USER EXECUTION |

验收证据只保存不含私密地址、绝对服务器路径和 Secret 的结果。播放器封面/歌词显示不一致时记录格式与播放器边界，不伪造成功。最终 V1 裁决由项目经理完成。
