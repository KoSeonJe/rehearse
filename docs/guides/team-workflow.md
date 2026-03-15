# Multi-Agent Team Workflow

## Team Composition

| Role | Agent | Responsibility |
|------|-------|----------------|
| PM | `orchestrator` | Task decomposition, sequencing, agent dispatch |
| Planning | `planner` | Requirements, user stories, acceptance criteria |
| Design | `designer` | UI/UX design, design tokens, component structure |
| Frontend | `frontend` | React implementation, state management, API integration |
| Backend | `backend` | Spring Boot API, DB, business logic |
| QA | `qa` | Test execution, bug detection, regression checks |
| Infra | `devops` | Build, Docker, CI/CD |

## Feature Development Workflow (Mixed Execution)

```
1. User: "Build XX feature" → invoke orchestrator
2. PM decomposes tasks, then sequential execution:
   - Designer → UI design → ⏸️ User review (Slack notification)
   - Backend → API design + implementation → ⏸️ User review (Slack notification)
   - Frontend → Implementation → Auto-proceed
   - QA → Testing → Auto-proceed
   - DevOps → Build verification → Slack completion report
```

**⏸️ Manual review**: Designer deliverables, Backend API design
**▶️ Auto-proceed**: Frontend, QA, DevOps

## Team Documents

| File | Purpose |
|------|---------|
| `.omc/notepads/team/handoffs.md` | Inter-agent handoff records |
| `.omc/notepads/team/issues.md` | Bug/issue tracking |
| `.omc/notepads/team/api-contracts.md` | Backend↔Frontend API contracts |
| `.omc/notepads/team/design-tokens.md` | Design system tokens |
| `.omc/notepads/team/decisions.md` | Architecture Decision Records (ADR) |

## Slack Notifications

- Config: `~/.claude/.omc-config.json`
- Triggers: session end, awaiting user input, session start, session keepalive
- Enable: `omc --slack` flag per session

## Slack Status Updates (Required)

- **All agents must post status on task start/completion**
- Format:
  ```
  [{AgentName}] {Status}
  - Task: {Summary of current work}
  - Status: Started | In Progress | Completed | Error
  - Changed files: {Key file list} (on completion)
  ```
- Uses existing Slack Webhook
