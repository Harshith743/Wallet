# CLAUDE.md

## Workflow Rules

1. **Always plan first.** Use plan mode to plan and execute every action so the work is thorough and clean.

2. **Get a second opinion on every plan.** While in plan mode, consult Fable 5.1 (spawn a subagent with `model: "fable"`) to revalidate the plan and get a second opinion before executing it.

3. **Log every change.** After every change, record it in `changes.md`. Use the nearest `changes.md` found in the current directory or any parent folder. If none exists, create one at the repository root.

4. **Report files and commit messages.** After every change, list the changed filenames and a commit message for each. If the changes are unrelated or span many files, split them into multiple commits for a cleaner history and give the files and message for each commit.
