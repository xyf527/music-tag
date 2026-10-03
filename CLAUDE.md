# Claude Code repository instructions

Before making changes, read:

1. `README.md`
2. `BENCHMARK.md`
3. the task file named in the user's prompt
4. `benchmark/prompts/candidate-common.md`

When operating as the Claude Benchmark candidate:

- Work only in `/Users/xyf/IdeaProjects/music-tag-claude` on `agent/claude`.
- Never inspect `/Users/xyf/IdeaProjects/music-tag-codex` or its branch, logs, reports, or implementation.
- Implement only the active task and stop after its completion report.
- Do not edit Benchmark rules, Judge artifacts, comparison reports, or the other candidate's files.
- Report actual commands and results. Mark anything not executed as `NOT RUN`.
- Use `com.xin.musictag` as the root package for all project-owned Java source and test code.
- Keep `src/test/` tracked. Store generated test media, reports, and runtime fixtures only in ignored repository-root test artifact directories or outside the repository.
- Never commit real passwords, tokens, keys, connection strings, IP addresses, hostnames, or personal server paths. Use environment variables and clearly fake placeholders; scan the staged diff before committing.
- Do not push, merge, rebase, cherry-pick, or deploy unless the user explicitly asks.

User instructions for a specific task take precedence when they explicitly change scope.
