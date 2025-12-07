# Sonic Distributed Agent Architecture

## Overview

This document describes the **7-agent distributed system** for developing the Sonic MVI framework. Agents are
self-organizing, coordinate automatically, and enforce quality gates across all development activities.

## Architecture at a Glance

```
┌─────────────────────────────────────────────────────────────────┐
│                    Distributed Agent System                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Core Agent         Contract Agent       Quality Agent           │
│  (Implement)        (Guard APIs)         (Test & Validate)       │
│       │                  │                    │                  │
│       └──────────────────┴────────────────────┘                  │
│                          │                                        │
│                ┌─────────┴──────────┐                            │
│                │                    │                            │
│         Integration Agent    Documentation Agent                 │
│         (Build & Release)    (Keep Docs Sync)                    │
│                │                    │                            │
│                └────────┬───────────┘                            │
│                         │                                        │
│                  ┌──────┴──────┐                                │
│                  │             │                                │
│         Experience Agent   Evolution Agent                      │
│         (Developer DX)    (Tech Health)                         │
│                                                                   │
│         ◄───────────────────────────────────┤                    │
│         Event Bus (automatic coordination)   │                   │
│                                              │                   │
├──────────────────────────────────────────────┤                   │
│  Orchestrator (manages complex workflows)    │                   │
│                                              │                   │
├──────────────────────────────────────────────┤                   │
│  Quality Gates (enforced on all work):       │                   │
│  ✅ Tests 100% pass ✅ Coverage >85%         │                   │
│  ✅ Build SUCCESS ✅ No regression >10%      │                   │
│  ✅ ktlint PASS ✅ SonarQube quality gate    │                   │
│                                              │                   │
├──────────────────────────────────────────────┤                   │
│  Safety Mechanisms:                          │                   │
│  🔄 Automatic rollback on failure            │                   │
│  🛑 Circuit breaker on repeated failures     │                   │
│  👤 Human approval for critical changes     │                   │
│                                              │                   │
└──────────────────────────────────────────────┘
```

## Agent Responsibility Matrix

| Agent             | Domain         | Key Responsibility           | Autonomy  | Approval               |
|-------------------|----------------|------------------------------|-----------|------------------------|
| **Core**          | Implementation | Implement framework logic    | High      | API changes only       |
| **Contract**      | APIs           | Guard public interfaces      | Medium    | Breaking changes only  |
| **Quality**       | Testing        | Ensure correctness           | Very High | Skip tests only        |
| **Integration**   | Build/Release  | Compile and publish          | High      | Major upgrades only    |
| **Documentation** | Knowledge      | Keep docs in sync            | High      | Major restructure only |
| **Experience**    | Ergonomics     | Improve developer experience | High      | Workflow changes only  |
| **Evolution**     | Health         | Manage technical debt        | Medium    | API removal only       |

## Coordination Patterns

### Pattern 1: Sequential Workflow (Dependency Chain)

Used when work must be done in order:

```
Feature Implementation:
  Core → Quality → Documentation → Integration

Deprecation:
  Evolution → Contract → Quality → Integration

Release:
  Evolution → Contract → Quality → Integration
```

**How it works**:

1. Agent 1 completes work
2. Creates handoff artifact in `.claude/handoffs/`
3. Agent 2 automatically activates
4. If Agent 2 finds issues → returns to Agent 1
5. Cycle continues until all agents pass

### Pattern 2: Parallel Workflow (Independent Validation)

Used when work is independent:

```
PR Validation:
  Quality ┐
  Integration ├→ All must pass → Merge ready
  Documentation ┘
  Contract ┘
```

**How it works**:

1. All agents activate simultaneously
2. Work independently
3. Orchestrator collects results
4. If all pass → Ready to merge
5. If any fail → Merge blocked

### Pattern 3: Orchestrated Workflow (Complex Task)

Used for high-level goals:

```
Goal: Implement Phase 1 (bindState)

Orchestrator:
  1. Activate Contract Agent → Design API
  2. Activate Core Agent → Implement
  3. Activate Quality Agent → Test
  4. Activate Documentation Agent → Document
  5. Activate Experience Agent → Create samples
  6. Activate Integration Agent → Publish
  7. Report complete ✅
```

## Event-Driven Activation

Agents activate automatically based on events:

### Git Events

```
Code Change → Quality Agent (run tests)
PR Opened → Quality + Integration + Documentation + Contract agents
Release Tag → Evolution + Integration agents
Issue Created → Experience Agent (triage)
```

### Manual Commands

```
/implement-feature → Orchestrator (sequential workflow)
/review-pr → Quality + Integration + Documentation + Contract agents
/run-quality-gate → Quality Agent
/release → Evolution + Contract + Integration agents
```

### File Triggers

```
StateManager.kt changed → Core + Quality + Documentation agents
I*.kt changed → Contract + Quality + Documentation + Evolution agents
Build.gradle changed → Integration agent
docs/*.md changed → Documentation agent
```

### Scheduled

```
Daily 9am → Integration (dependency check)
Weekly Monday → Evolution + Quality (health check)
Monthly 1st → Evolution (review technical debt)
```

## Quality Gate System

### Pre-Merge Quality Gate

Every PR must pass these gates **before merge**:

```
┌─────────────────────────────────────────┐
│         Quality Agent                    │
├─────────────────────────────────────────┤
│ ✅ Unit tests: 100% pass rate            │
│ ✅ Integration tests: All pass           │
│ ✅ Coverage: >85% minimum                │
│ ✅ Performance: No regression >10%       │
└─────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────┐
│      Integration Agent                   │
├─────────────────────────────────────────┤
│ ✅ Build: ./gradlew build SUCCESS        │
│ ✅ ktlint: Zero violations               │
│ ✅ SonarQube: Quality gate PASS           │
│ ✅ No security vulnerabilities           │
└─────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────┐
│   Documentation Agent                    │
├─────────────────────────────────────────┤
│ ✅ KDoc complete: All public APIs        │
│ ✅ Examples: Compile and work            │
│ ✅ CHANGELOG: Updated                    │
│ ✅ Migration guides: If breaking         │
└─────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────┐
│      Contract Agent                      │
├─────────────────────────────────────────┤
│ ✅ Breaking changes: None or documented  │
│ ✅ API compatibility: 100%               │
│ ✅ Semantic versioning: Correct          │
└─────────────────────────────────────────┘
         ↓
      MERGE ✅
```

### Failure Handling

**If any gate fails**:

```
1. Detection: Gate fails
   ↓
2. Report: Agent reports specific failure
   ↓
3. Block: Merge is blocked
   ↓
4. Notify: Developer is notified via PR comment
   ↓
5. Wait: Developer fixes issue
   ↓
6. Retry: Developer pushes changes
   ↓
7. Revalidate: All gates run again
   ↓
8. Success or repeat: Cycle continues until all pass
```

## Autonomy and Approval Levels

### Level 1: Full Autonomy (No Approval)

Agent can execute **immediately** and **independently**:

- Core: Implement private functions, optimize algorithms, fix bugs
- Quality: Write tests, run tests, generate reports
- Integration: Run builds, publish artifacts, manage releases
- Documentation: Update docs, create migration guides
- Experience: Improve error messages, create samples
- Evolution: Track technical debt, identify refactorings

### Level 2: Supervised Autonomy (Quality Gate Approval)

Agent can execute if **all quality gates pass**:

- Core: Add new public method (if tests pass)
- Contract: Design new API variant (if compatible)
- Integration: Publish to Maven Central (if tests pass)
- Evolution: Migrate internal code (if tests pass)

### Level 3: Human Approval Required

Agent **must get human approval** before executing:

- Breaking API changes
- Removal of deprecated APIs
- Major dependency upgrades
- Release to production
- Quality threshold reductions
- Architectural decisions

## Safety Mechanisms

### Automatic Rollback

```
1. Agent detects failure (build fail, tests fail, etc.)
   ↓
2. Rollback to last known good state
   ↓
3. Create incident report
   ↓
4. Notify human with details
   ↓
5. Create issue for investigation
   ↓
6. Log all actions to audit trail
```

### Circuit Breaker

```
Agent fails 3 times in a row:
  ↓
1. Enter "disabled" mode
   ↓
2. All future actions blocked
   ↓
3. Require human approval
   ↓
4. Notify human: "Agent disabled, needs review"
   ↓
5. Human investigates and fixes
   ↓
6. Re-enable after successful human-approved action
```

### Audit Trail

All agent actions logged to `.claude/audit/`:

```
.claude/audit/
├── 2025-12-07-core-agent.log
├── 2025-12-07-quality-agent.log
├── 2025-12-07-integration-agent.log
├── 2025-12-08-contract-agent.log
└── orchestrator.log
```

## Configuration Files

### `.claude/agents/*.md` (7 files)

Define agent responsibility, autonomy scope, and coordination:

- `core.md` - Core logic implementation
- `contract.md` - Public API management
- `quality.md` - Testing and validation
- `integration.md` - Build and release
- `documentation.md` - Knowledge management
- `experience.md` - Developer ergonomics
- `evolution.md` - Technical health

### `.claude/config/agent-activation.json`

Maps triggers (file changes, git events, commands) to agents:

```json
{
  "file_triggers": { ... },
  "git_event_triggers": { ... },
  "command_triggers": { ... },
  "scheduled_triggers": { ... },
  "workflow_definitions": { ... }
}
```

### `.claude/guardrails/approved-actions.json`

Defines what each agent can do **without approval**:

```json
{
  "core_agent": {
    "approved_without_review": [ ... ],
    "requires_human_review": [ ... ]
  }
}
```

## Workflow Examples

### Example 1: Implement Feature (Automated)

```
Goal: Add CacheMiddleware

1. You: /implement-feature "Add CacheMiddleware"
   ↓
2. Orchestrator activates agents in sequence
   ↓
3. Core Agent:
   - Implements CacheMiddleware.kt
   - Adds configuration options
   - Integrates with middleware chain
   ↓
4. Quality Agent:
   - Writes comprehensive tests
   - Verifies 100% pass
   - Confirms coverage >85%
   ↓
5. Documentation Agent:
   - Adds KDoc to CacheMiddleware
   - Creates usage example
   - Updates CHANGELOG
   ↓
6. Integration Agent:
   - Runs full build
   - Runs ktlint
   - Runs SonarQube
   - All gates pass ✅
   ↓
Result: Feature ready, tested, documented
Time: ~20 minutes (fully automated)
```

### Example 2: Release v1.0 (With Approval)

```
Goal: Release v1.0 (includes breaking changes)

1. You: /release v1.0
   ↓
2. Evolution Agent:
   - Reviews deprecation timeline
   - Screen class ready for removal? ✅
   - ⚠️ Escalates to human for approval
   ↓
3. Human reviews and approves breaking changes
   ↓
4. Contract Agent:
   - Finalizes API documentation
   - Documents breaking changes
   ↓
5. Core + Quality Agents (parallel):
   - Final implementation touches
   - Full test suite runs
   ↓
6. Integration Agent:
   - Builds all artifacts
   - Signs with GPG
   - Publishes to Maven Central
   - Creates GitHub release
   ↓
Result: v1.0 published
Time: ~45 minutes (including human review)
```

### Example 3: Bug Fix (Automated)

```
Goal: Fix StateManager cache bug

1. Issue created: "StateManager cache not cleared"
   ↓
2. Quality Agent:
   - Reproduces bug
   - Writes failing test
   ↓
3. Core Agent:
   - Fixes root cause
   - Verifies fix
   ↓
4. Quality Agent:
   - Re-runs tests → ✅ All pass
   - Runs regression tests → ✅ All pass
   ↓
5. Integration Agent:
   - Publishes hotfix v0.5.1
   ↓
6. Documentation Agent:
   - Updates CHANGELOG
   ↓
Result: Bug fixed and released
Time: ~10 minutes (fully automated)
```

## Usage Model

### Three Ways to Use Agents

**1. Automatic** (Recommended for routine work)

```bash
# Just work normally
git commit -m "Add feature"
git push
# Agents automatically validate, test, document
```

**2. Manual** (When you want to trigger specific workflow)

```bash
# Start a feature implementation
claude /implement-feature "Add RetryMiddleware"

# Review a PR
claude /review-pr #42

# Run quality checks
claude /run-quality-gate

# Release a version
claude /release v0.6.0
```

**3. Directed** (When you want specific agent to do something)

```bash
# Ask Core Agent to implement
claude @core "Implement CacheMiddleware"

# Ask Contract Agent to review API
claude @contract "Design StateBinding API"

# Ask Quality Agent to report coverage
claude @quality "Generate coverage report"
```

## Benefits of This Architecture

✅ **Comprehensive Coordination**: Agents work together automatically
✅ **Quality Enforcement**: Quality gates can't be skipped
✅ **Documentation Sync**: Docs update automatically with code
✅ **Scalable Autonomy**: 80%+ of routine work happens without human approval
✅ **Safety Built-in**: Rollback, circuit breaker, audit trail
✅ **Timeless**: Works for v0.5, v1.0, v5.0 (eternal concerns)
✅ **Fast Feedback**: Issues caught immediately on commit
✅ **Clear Responsibility**: Each agent has defined scope
✅ **Transparent**: All actions logged and auditable
✅ **Flexible**: Easily customizable guardrails and triggers

## Next Steps

1. **Review agent definitions**: Read `.claude/agents/*.md`
2. **Understand configuration**: Review `.claude/config/*.json`
3. **Try a workflow**: Use `/implement-feature` or `/review-pr`
4. **Observe coordination**: Watch agents work together in PR comments
5. **Customize guardrails**: Adjust autonomy for your team's needs

---

This architecture provides a **self-organizing engineering team** that stays focused, maintains quality, and evolves the
project responsibly.
