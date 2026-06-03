---
description: "Workflow reference — story lifecycle, role handoffs, Definition of Done, status labels, role-switch triggers, escalation rules. Load when coordinating phases, checking story status, or verifying DoD."
---

# Process & Workflow — Story Lifecycle & Handoffs

This document defines the end-to-end story lifecycle, handoff protocols between roles, Definition of Done, and how GitHub Copilot should coordinate between roles throughout a project.

---

## 🔁 Story Lifecycle (Full Flow)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         STORY LIFECYCLE                                     │
│                                                                             │
│  [Idea] ──► [BA: Epic & Story] ──► [Designer: UX/Wireframe]                │
│                                           │                                 │
│                                    [Architect: ADR]                         │
│                                           │                                 │
│                              ┌────────────┴────────────┐                   │
│                              │                         │                   │
│                       [Backend Dev]            [Frontend Dev]              │
│                              │                         │                   │
│                              └────────────┬────────────┘                   │
│                                           │                                 │
│                                    [AQA: Int. Tests]                        │
│                                           │                                 │
│                                  [Definition of Done ✅]                    │
│                                           │                                 │
│                                        [Merge]                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📌 Phase 0 — Project Kickoff

**Trigger:** User provides a project idea.

**Steps:**
1. **Copilot activates BA role.**
2. BA asks 3–5 clarifying questions about scope, users, and constraints.
3. After answers, BA produces:
   - Full list of **Epics** with goals and scope
   - **Initial backlog** of User Stories per Epic (ordered by priority)
4. User reviews and confirms the backlog.
5. BA produces full **Story documents** for Epic 1, Story 1 first.

**Output:** `docs/backlog/` folder with all Epic and Story files.

---

## 📌 Phase 1 — BA (Business Analysis)

**Input:** Raw idea or feature request  
**Output:** User Story document with Acceptance Criteria

**Handoff to Designer when:**
- [ ] Story has a clear user goal statement
- [ ] All Acceptance Criteria are written and testable
- [ ] Out-of-scope items are listed
- [ ] Open questions are resolved or noted
- [ ] Story is linked to parent Epic

---

## 📌 Phase 2 — Product Design

**Input:** User Story + Acceptance Criteria  
**Output:** UX flow + wireframes + interaction states

**Handoff to Architect when:**
- [ ] UX flow covers all Acceptance Criteria
- [ ] All affected screens have wireframes
- [ ] All interaction states are defined (loading, error, empty, success)
- [ ] Responsive behavior is described
- [ ] Component names and props are specified

---

## 📌 Phase 3 — Architecture

**Input:** Story + Design specs  
**Output:** Architecture Decision Record (ADR)

**Handoff to Backend + Frontend when:**
- [ ] ADR written and accepted
- [ ] Persistence strategy decided (SQL / NoSQL / Hybrid)
- [ ] DB schema or document schema defined
- [ ] API endpoints listed (method, path, payload shapes)
- [ ] Messaging topology defined if applicable
- [ ] External integrations documented if applicable
- [ ] Cross-cutting concerns specified (auth, error handling)

---

## 📌 Phase 4 — Backend Development

**Input:** ADR + Story + Design  
**Output:** Working Spring Boot implementation with OpenAPI docs

**Handoff to AQA when:**
- [ ] All domain entities/documents created
- [ ] All DB migrations written
- [ ] All service methods implemented
- [ ] All REST endpoints implemented with OpenAPI annotations
- [ ] Messaging producers/consumers implemented (if required)
- [ ] External API clients implemented (if required)
- [ ] Story ID referenced in all new classes

---

## 📌 Phase 5 — Frontend Development

**Input:** ADR + Wireframes + Story  
**Output:** Working React TypeScript UI connected to backend

**Handoff to AQA when:**
- [ ] All components match wireframes
- [ ] Loading, error, and empty states implemented
- [ ] Forms have validation (React Hook Form + Zod)
- [ ] All API calls wired correctly
- [ ] Responsive layout implemented
- [ ] Accessibility requirements met
- [ ] Story ID referenced in all new files

---

## 📌 Phase 6 — AQA (Integration Testing)

**Input:** Implemented story (backend + frontend)  
**Output:** Passing Testcontainers integration tests

**Story is DONE when:**
- [ ] Every AC has at least one passing integration test
- [ ] Happy paths, error cases, and edge cases are covered
- [ ] Messaging flows are tested (if applicable)
- [ ] External API flows are tested with WireMock
- [ ] All tests pass in CI
- [ ] Story ID referenced in test class

---

## ✅ Definition of Done (DoD)

A story is **DONE** only when ALL of the following are true:

### Business
- [ ] All Acceptance Criteria from the Story document are fulfilled
- [ ] Product Designer has reviewed the implemented UI against wireframes
- [ ] BA has confirmed the story delivers the stated user value

### Backend
- [ ] Domain model implemented and DB migration created
- [ ] Service layer implemented with proper transactions
- [ ] REST endpoints implemented and documented in OpenAPI/Swagger
- [ ] Messaging integration implemented (if required by ADR)
- [ ] External API integration implemented (if required by ADR)
- [ ] No secrets hardcoded — environment variables used  - [ ] `Dockerfile` uses multi-stage build (compilation inside Docker, not from host `target/`)
  - [ ] `.dockerignore` present and excludes `target/` and build artifacts
  - [ ] `docker-compose.yml` `build:` context points to source — no pre-built JAR required
### Frontend
- [ ] UI matches wireframes for all defined screen states
- [ ] All interaction states handled (loading, error, empty, success)
- [ ] Form validation implemented and matches AC requirements
- [ ] Responsive layout works on mobile, tablet, desktop
- [ ] No TypeScript `any` types used

### Quality
- [ ] Integration tests written covering ALL ACs
- [ ] All integration tests pass in CI (with real Testcontainers)
- [ ] No new compiler warnings introduced
- [ ] No hardcoded test data or skipped tests

### Code Quality
- [ ] All new code references the Story ID (`// Story: US-[NNN]`)
- [ ] No TODO comments left without tracking issue
- [ ] OpenAPI docs generated and accurate

---

## 🔀 Parallel vs Sequential Work

| Situation | Approach |
|-----------|----------|
| Backend and Frontend for the same story | **Parallel** — both can start after ADR is accepted |
| Multiple independent stories in the same Epic | **Parallel** — if no data dependencies |
| Story B depends on Story A's API | **Sequential** — A must be backend-complete before B starts |
| AQA writing tests | **Sequential** — must start after Backend is complete |

---

## 🗂️ Recommended Folder Structure

```
[project-root]/
  docs/
    backlog/
      EPIC-01-[name].md
      stories/
        US-001-[name].md
        US-002-[name].md
    design/
      US-001-[name]/
        ux-rationale.md
        ux-flow.md
        wireframes.md
        interaction-states.md
        microcopy.md
    architecture/
      system-overview.md
      data-model.md
      api-contracts.md
      ADR-001-[title].md
  backend/                          # Spring Boot project
    src/
      main/java/...
      test/java/...
    Dockerfile                        # Multi-stage: build JAR inside Docker, never from host target/
    .dockerignore                     # Excludes target/, .mvn/, .git/
  frontend/                         # React TypeScript project
    src/
      features/
      shared/
      app/
    Dockerfile                        # Multi-stage: npm install + build inside Docker
    nginx.conf                        # Nginx config for SPA routing + API proxy
    .dockerignore                     # Excludes node_modules/, dist/, .git/
  docker-compose.yml                # Full local stack — builds all app images from source
  .github/
    copilot-instructions.md
    instructions/
      01-ba.instructions.md
      02-product-designer.instructions.md
      03-architect.instructions.md
      04-backend-dev.instructions.md
      05-frontend-dev.instructions.md
      06-aqa.instructions.md
      07-workflow.instructions.md
```

---

## 🤖 Copilot Role Switching

When the user signals a role switch, Copilot must:

1. **Confirm the switch** explicitly: _"Switching to [Role] for Story US-[NNN]."_
2. **Load the context** from the previous role's output before starting.
3. **Only perform actions within the new role's scope.**
4. **Reference the Story ID** in all outputs.

### Role Switch Triggers

| User says... | Copilot activates... |
|--------------|----------------------|
| "Start with the idea: ..." | BA |
| "Design this story" / "Create wireframes for US-NNN" | Product Designer |
| "Design the architecture for US-NNN" | Architect |
| "Implement the backend for US-NNN" | Backend Developer |
| "Implement the frontend for US-NNN" | Frontend Developer |
| "Write tests for US-NNN" | AQA Engineer |
| "Orchestrate US-NNN" / "Run the pipeline for US-NNN" / "Take US-NNN from design to done" | Story Orchestrator |
| "What's the status of US-NNN?" | Workflow / Process check |

---

## 🎼 Orchestrated Delivery (Designer → AQA)

For a story that is already defined by the BA, the **Story Orchestrator** agent can drive the entire remaining pipeline automatically, delegating each phase to the specialist subagent and enforcing the handoff gate before advancing:

```
[Story Orchestrator]
   └► Product Designer  → gate: design handoff checklist
        └► Architect     → gate: architecture handoff checklist
             └► Backend Dev + Frontend Dev → gate: dev handoff checklists
                  └► AQA Engineer → gate: Definition of Done
```

**Rules for the Orchestrator:**
- It only starts once the BA has produced the story with testable ACs; otherwise it escalates back to the user/BA.
- It runs **one phase at a time**, verifies that phase's handoff checklist, and re-delegates on failure rather than proceeding.
- It updates the story status label as phases complete and reports the final DoD status.
- Use it for hands-off delivery; use **manual role switching** when you want to review and steer between phases.

---

## ⚠️ Escalation Rules

Copilot must **stop and ask** before proceeding when:

- An Acceptance Criterion is ambiguous or contradictory
- The ADR is missing required information (persistence, endpoints)
- The wireframe and the Story AC do not align
- Two stories have conflicting API contracts
- A story is larger than ~3 days of work (suggest splitting)
- An architectural decision would conflict with an existing ADR

---

## 📊 Story Status Tracking

Use the following status labels in story files:

| Status | Meaning |
|--------|---------|
| `🔵 BACKLOG` | Defined but not started |
| `🟡 IN DESIGN` | Being worked on by BA / Designer |
| `🟠 IN ARCHITECTURE` | ADR being drafted |
| `🔴 IN DEVELOPMENT` | Backend / Frontend coding in progress |
| `🟣 IN TESTING` | AQA writing/running integration tests |
| `✅ DONE` | All DoD criteria met |
| `🚫 BLOCKED` | Blocked by dependency or open question |
