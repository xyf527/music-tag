# Phase 01 Interim Comparison

## 用户主观评测

| 观察项 | 用户结论 |
|---|---|
| 操作与确认次数 | Codex 更多，交互成本仍高 |
| 最终回复 | Claude 更好，内容更详细 |
| 目录结构与抽象 | Codex 更好，更符合用户期望；Claude 较乱 |
| Claude 基线 tag 中断 | 视为 Git / API 中转环境差异，不作主观扣分 |
| 完成速度 | Claude 更快 |
| 真实可用性 | Claude 当前无法完成真实使用体验；Codex 更接近可用 |
| **Phase 01 最终主观投票** | **Codex** |

用户本轮不再填写数值表格。最终判断以实际观察文字为准：虽然 Claude 速度更快、最终回复更详细，但本阶段主要关注目录结构、文件继承和抽象质量，Codex 明显更符合预期；Claude 尚无法正常使用，因此用户将票投给 Codex。

## Judge 客观复核

| 项目 | Codex | Claude |
|---|---|---|
| Candidate HEAD | `3a2a42f` | `3e12cb6` |
| 当前状态 | `FIX_ROUND_2` | `FIX_ROUND_2` |
| Java 17 Maven 验证 | 4 tests，通过 | 1 test，通过但覆盖不足 |
| MP3 / FLAC 真实改写 | 已验证 | 未验证；脚本产物与原件完全相同 |
| MySQL 8 | JDBC/Flyway 已实现；测试仍用 H2 | 未接入；运行时使用内存仓库 |
| Web 闭环 | 部分完成，缺歌词/封面三态 UI | 未形成完整流程 |
| 分层结构 | 清楚 | 外形存在，实际职责不完整 |
| 安全门禁 | 尚需补测试 | 失败：任意路径下载与绝对路径泄露 |
| 当前领先 | **Codex** | — |

## 阶段结论

Codex 当前客观领先，也获得用户主观票，但 Phase 01 尚未最终计分。两边都有任务规定的门禁缺口，Claude 还存在严重的真实性和安全问题，因此不能直接进入 Phase 02，也不能部署到 M710q。

下一步是分别执行 Fix Round 2。修复后由 Judge 重新运行完整验收；只有通过 Phase 01 门禁，才冻结 Phase 02 的共同任务。

## Fix Round 1 后续结论

| 项目 | Codex | Claude |
|---|---|---|
| Fix Round 1 HEAD | `d1d2fa3` | `8f24735` |
| Judge 普通测试 | 5 tests，通过 | 1 test，通过但覆盖严重不足 |
| 候选完成状态 | 接近完成 | 自报 `PARTIAL` |
| 剩余门禁 | 任务 API 路径泄露、下载根目录约束、M710q MySQL 复核 | MySQL 闭环、完整页面、真实产物、测试、最终检查 |
| 下一步 | Fix Round 2，小范围收尾 | Fix Round 2，完成核心功能 |

用户新增统一规则：后续所有数据库访问、建库、Flyway 和集成测试使用 M710q 既有 MySQL 8，Mac 不再启动本地 MySQL、Docker MySQL 或 Testcontainers MySQL。该规则不追溯否定此前已经完成的证据，但从 Fix Round 2 开始强制执行。
