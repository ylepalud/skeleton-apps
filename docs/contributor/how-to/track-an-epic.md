# Track an epic

An epic is a GitHub issue whose sub-issues deliver one goal, one pull request each. Its body carries a **Progress** section: four plain-text diagrams that show at a glance what is done, what depends on what, and what reaches `develop` next. The issue body is the single place for them, so updating them needs no commit.

## The Progress section

Put it between the epic's decisions and its "Done when", each block in its own fenced code block so GitHub keeps the alignment:

```text
## Progress

Updated at every step: pull request opened, CI result, merge, issue closed. Last update: <date> (<what changed>).
```

```text
 STATUS
 ══════════════════════════════════════════════════════════════════════
  #12  PR    feat(x): first piece .................... ✅ MERGED (squash, abc1234)
  #13  PR    feat(y): second piece ................... 🟢 CI green, in review

  EPIC #10  Goal of the epic ......................... 🟢 OPEN  (1/4)
  ├─ #11  first piece ............................... ✅ closed by #12
  ├─ #14  second piece .............................. 🟡 in review → PR #13
  ├─ #15  third piece ............................... ⚪ to do
  └─ #16  last piece ................................ ⚪ to do

  OUTSIDE THE EPIC
  └─ #17  deferred work ............................. 💤 deferred
```

```text
 DEPENDENCIES
 ══════════════════════════════════════════════════════════════════════
               ┌──────────────┐
               │ #11 first  ✅│
               └──────┬───────┘
              ┌───────┴────────┐
              ▼                ▼
       ┌────────────┐   ┌────────────┐
       │ #14 second │   │ #15 third  │
       └─────┬──────┘   └─────┬──────┘
             └───────┬────────┘
                     ▼
              ┌────────────┐
              │ #16 last   │
              └────────────┘
```

```text
 MERGES INTO develop: DONE, IN PROGRESS, NEXT
 ══════════════════════════════════════════════════════════════════════
 develop ──●─────────●─────◆─────◇─────◇──►
           │         │     │     │     │
         abc0000  abc1234  #14   #15   #16
         (base)    #12 ✅   🟡    ⚪    ⚪
                  squash   PR #13

   ●  merged     ◆  in progress     ◇  next (one branch and one PR each)

 Current branch:
   feat/second-piece  (from abc1234) → PR #13
     └─ def5678  feat(y): second piece
```

```text
 NEXT ACTIONS
 ══════════════════════════════════════════════════════════════════════
  1. Review and merge #13.
  2. #15 depends on #11 only: it can start now.
```

| Mark | Meaning |
| --- | --- |
| ✅ | merged or closed |
| 🟢 | open and healthy: CI green, or an epic in progress |
| 🟡 | in review |
| 🔵 | in progress, no pull request yet |
| ⚪ | to do |
| 💤 | deferred |
| ⛔ | closed without being done |
| ⚠ | blocked by something outside the repository, named in NEXT ACTIONS |

## When to update it

At every step of a sub-issue: a branch started, a pull request opened, a CI result, a merge, an issue closed. Refresh the "Last update" line each time, and the current branch block when commits change.

## How to update it

Read the current body first: someone may have edited it since. Change only the lines the step affects, then write the whole body back:

```bash
gh issue view 10 --json body -q .body > epic.md
# edit epic.md
gh issue edit 10 --body-file epic.md
```

- **Keep the columns aligned.** An emoji takes two columns: put it in place of two spaces, never inside a box of the dependency diagram.
- **Keep the English and the layout** of the blocks; only states, numbers and commits change.
- **Reuse the same diagrams** when reporting progress elsewhere, in a pull request or a conversation.
