# ResolveNow — design reference

Files in this folder are the saved analysis and target design from the Oct 2026 planning session. They are **not** the running code. Implementation has not started from this plan yet.

| File | What it is |
|---|---|
| [PROBLEM_STATEMENT.md](PROBLEM_STATEMENT.md) | Official brief (Enterprise Issue Escalation & Service Desk) |
| [CURRENT_FLOW.md](CURRENT_FLOW.md) | How the **existing** endpoints, tables, and Feign chain actually behave |
| [TARGET_FLOW_AND_DATABASE.md](TARGET_FLOW_AND_DATABASE.md) | Locked target flow, roles, status machine, and DB (complaint names from the brief) |
| [CANVAS.md](CANVAS.md) | Where the interactive canvas lives + what it shows |

**Live canvas in Cursor** (open beside chat):

[ResolveNow complaint lifecycle](C:\Users\sharv\.cursor\projects\c-Users-sharv-Soa-final-project-ResolveNow\canvases\resolvenow-target-flow.canvas.tsx)

An early canvas draft used `request` instead of `complaint`. The live canvas and [TARGET_FLOW_AND_DATABASE.md](TARGET_FLOW_AND_DATABASE.md) now keep **complaint** names from the brief.

Cursor plan file (same content as the target doc):

`C:\Users\sharv\.cursor\plans\resolvenow_target_flow_8796faed.plan.md`
