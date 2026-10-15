# Project Agent & Workflow Rules

## Terminal & Execution Rules
1. Never run terminal commands automatically without explicit permission.
2. Give all terminal queries next to each other in one single command block/line; never break them into multiple consecutive blocks unless separate terminals are strictly required.
3. User executes all commands manually by default.
4. Git Commits & Pushes: Always create 2 separate commits and pushes for Android and Backend independently:
   - First stage Android changes, commit, and push to the `android` branch.
   - Then stage Backend changes, commit, and push to the `backend` branch.
5. Always check and update .gitignore files whenever a prompt/task is finished to ensure build artifacts, IDE files, local secrets, and caches are properly excluded while keeping tracked source files safe.

## Code & Architecture
1. No huge files over 300 lines unless necessary; keep code modular.
2. Production-ready folder structure; no toy code.
3. Validate all inputs, cleanly use async/await, and never expose secrets.
