# Phase 01 Judge Report — Codex

## 结论

- Status: `FIX_ROUND_1`
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

