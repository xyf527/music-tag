# Phase 00 Preliminary Comparison

## 当前状态

Phase 00 尚未结束，两边均进入 `FIX_ROUND_1`，因此当前不宣布冠军，也不生成最终分数。

| 项目 | Codex | Claude |
|---|---|---|
| Candidate HEAD | `002f6e1` | `1a754b6` |
| Maven 构建 | PASSED | PASSED WITH INCOMPLETE COVERAGE |
| MP3 | VERIFIED | PARTIALLY VERIFIED |
| FLAC | VERIFIED | NOT RUN |
| WAV | VERIFIED TEXT / ARTWORK & LYRICS UNSUPPORTED | NOT RUN |
| 封面与歌词 | MP3 / FLAC VERIFIED | NOT RUN |
| 原件保护 | VERIFIED | VERIFIED FOR MP3 |
| 解码 PCM | VERIFIED | NOT RUN |
| 可复现文档 | FAILED COMMAND ORDER | INCOMPLETE |
| 人工播放器验收 | NOT RUN | NOT RUN |

## 初步观察

Codex 当前在技术完成度上明显领先，已经形成三格式读写、原件哈希和解码内容验证。主要缺口是复现命令错误、人工验收产物缺失和标准汇报缺失。

Claude 的优点是没有掩盖未验证项目，但当前交付只覆盖 MP3 文本字段与原件哈希，未满足 Phase 00 对 MP3、FLAC、WAV、封面、歌词和解码验证的共同要求。测试在 fixture 缺失时还存在假通过路径。

主观开发体验尚未由用户填写。最终比较会同时保留主控客观证据和用户主观体验，不用其中一项覆盖另一项。

## 下一出口

1. 两边分别完成 Fix Round 1。
2. 主控用同一份仓库外合成样本重新执行测试。
3. 用户按 `manual-acceptance.md` 完成 CLI 体验和播放器检查。
4. 主控生成最终报告和分数。
5. Phase 00 通过后才发布 Phase 01 共同任务。
