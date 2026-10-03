# Phase 01 Judge Report — Codex

## 结论

- Status: `FAILED USER ACCEPTANCE / CONTINUATION BASELINE`
- Objective Score: `PENDING`
- Candidate HEAD: `3a2a42f3c6d1679f5f78212a79dbd17a1fea4af9`
- Baseline: `f555501981540f782480be3cd52df663208cb6c7`
- Judge Maven Verify: `PASSED`（4 tests，0 failures，0 errors，0 skipped）
- Judge Manual Artifacts: `PASSED WITH GAPS`

## 已验证结果

| 检查 | 结果 | 证据 |
|---|---|---|
| Java 包名 | PASSED | 项目自有代码位于 `com.xin.musictag` |
| 分层结构 | PASSED | Web、application、domain、persistence、tagging 职责清楚 |
| Maven 完整验证 | PASSED | Java 17 下 `mvn clean verify` 通过，4 tests |
| MP3 / FLAC 真实处理 | PASSED | 处理后文件哈希与原件不同，标签重读结果与报告一致 |
| 原件保护 | PASSED | 工作副本处理，原件未被覆盖 |
| JSON 报告 | PASSED | 报告不含服务器绝对路径 |
| 数据持久化实现 | PARTIAL | 有 JDBC Repository、Flyway 与 MySQL 驱动；测试仅使用 H2 MySQL mode |
| 浏览器完整编辑流程 | PARTIAL | 页面没有提供歌词/封面 `KEEP / SET / REMOVE` 三态操作 |

主控复核产物位于仓库外：`$TMP/music-tag-phase01-judge-codex-20261003`。该本机路径不进入 Git。

## Fix Round 1 必修项

1. 浏览器页面必须真实提供歌词和封面的 `KEEP / SET / REMOVE`，并支持设置、删除和只改单字段时保留其他字段。
2. 补齐浏览器上传、预览、变更预览、执行、下载成品和下载 JSON 报告的真实操作证据。
3. 使用隔离 MySQL 8 实例执行集成测试，不得以 H2 MySQL mode 代替任务要求的 MySQL 8。
4. 覆盖歌词和封面修改、删除、重读验证以及 WAV 能力拒绝。
5. 验证同一资源再次编辑会生成带正确父版本关系的新版本。
6. 人工验收目录增加明确的原件 SHA-256 记录，并保留处理后 MP3、FLAC 和对应 JSON。
7. 增加伪造扩展名、空文件、危险文件名和路径安全测试。

## 用户主观反馈

- 交互中仍是 Codex 的操作和确认更多。
- 用户认为 Codex 的目录结构、职责拆分与抽象更符合预期。
- 本阶段用户最终投票：`CODEX`。

## Fix Round 1 复核

- Candidate HEAD: `d1d2fa32d17fa8d52946ca324fb9f76d05abe2e6`
- Judge `mvn clean verify`: `PASSED`（5 tests，0 failures，0 errors，0 skipped）
- 候选提供了真实 MP3 / FLAC 产物、版本父子关系、三态 UI 和 MySQL 8 自测证据。
- 候选在新数据库规则发布前使用了 Mac Docker MySQL；不追溯扣分，但后续数据库验证必须改用 M710q。
- 安全复核发现 `/api/tasks/{id}` 直接序列化 `TaskRecord.reportPath`，会暴露服务器绝对路径。
- 版本下载仅验证输出文件名，没有确认数据库中的 `outputPath` 位于配置的输出根目录内。

因此本候选进入 `FIX_ROUND_2`，只处理路径 DTO、根目录约束和 M710q MySQL 验证，不扩大 Phase 01 范围。

## 最终人工验收结论

- Final Submitted HEAD: `7223181f7df1d234a2c4b6f5b60e215e43235da6`
- 用户主观评分：`1`
- 12.2 MB MP3 上传：`FAILED`（超过当前最大上传限制）
- 页面可用性与视觉完成度：`FAILED USER ACCEPTANCE`
- 手工 SQL 与 Flyway：`FAILED`（非空 schema 无 `flyway_schema_history`，需要用户删除并重建空数据库）
- 项目继续选择：仅作为后续修复基线保留，不视为 Phase 01 通过。

本地 worktree 中出现的真实数据库地址、账号和密码未提交、未推送。
