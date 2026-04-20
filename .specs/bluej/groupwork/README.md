# Groupwork (Version Control)

Git-based collaboration and version control (67 files). Provides commit, push, pull, merge, and conflict resolution through an abstracted `Repository` interface implemented with Eclipse JGit.

---

## Key Interfaces and Classes

| Class | Purpose |
|-------|---------|
| `Repository` (interface) | Abstract version control operations |
| `TeamSettings` | Team/repository configuration |
| `TeamworkCommand` | Encapsulated VCS operations |
| `CommitAndPushFrame` | Commit/push UI dialog |

---

## Internal Organization

| Sub-package | Purpose |
|-------------|---------|
| `git/` | JGit-based Git implementation of Repository interface |
| `ui/` | Team UI: commit, update, status, history dialogs |
| `actions/` | Team-related menu actions |

---

## Dependencies

**External:** Eclipse JGit 7.1.0

Uses: `pkgmgr/` (project context), `utility/`
