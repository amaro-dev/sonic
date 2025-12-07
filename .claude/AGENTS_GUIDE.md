# Sonic Distributed Agent System - User Guide

## Overview

You now have a **self-organizing team of 7 specialized agents** that coordinate automatically to develop the Sonic MVI
framework. Each agent has clear responsibilities, autonomy boundaries, and coordination protocols.

## The 7 Agents

### 1. **Core Agent** (`/agents/core.md`)

**What it does**: Implements framework logic and optimizations

**When it activates**:

- Files change: `StateManager.kt`, `*Middleware.kt`, etc.
- Issues labeled: `core-logic`
- You command: `/implement-feature`

**What it can do alone**:

- Add new middleware implementations
- Optimize algorithms
- Fix bugs (backward compatible)
- Add private utility functions

**What needs approval**:

- Change public interface signatures
- Remove public functions
- Break backward compatibility

**Example task**: "Core Agent, implement RetryMiddleware for automatic retry logic"

---

### 2. **Contract Agent** (`/agents/contract.md`)

**What it does**: Guards public APIs and manages deprecation

**When it activates**:

- Files change: `I*.kt` (interface files)
- Version milestones reached
- Issues labeled: `api-design`
- You command: `/review-pr`

**What it can do alone**:

- Add deprecation warnings
- Design backward-compatible API extensions
- Suggest new interface variants
- Document API evolution

**What needs approval**:

- Breaking API changes
- Removal of deprecated APIs
- Behavior changes

**Example task**: "Contract Agent, deprecate Screen class with warning for v0.6, plan ERROR for v0.8"

---

### 3. **Quality Agent** (`/agents/quality.md`)

**What it does**: Ensures correctness through testing and validation

**When it activates**:

- Code committed to library/src/main/
- Pull request opened
- Issues labeled: `bug`
- Manual: `/run-quality-gate`

**What it can do alone**:

- Write and run tests
- Generate coverage reports
- Run performance benchmarks
- Block PRs failing tests
- Run static analysis (ktlint, SonarQube)

**What needs approval**:

- Skip tests
- Reduce coverage threshold
- Disable quality gates

**Quality Gates**:

- Test pass rate: 100%
- Coverage: >85%
- Performance: No regression >10%
- Build: SUCCESS

**Example task**: "Quality Agent, run full test suite and report coverage"

---

### 4. **Integration Agent** (`/agents/integration.md`)

**What it does**: Manages builds, dependencies, and releases

**When it activates**:

- PR opened
- Release tag pushed
- Dependency updates available
- Manual: `/run-build`, `/release`

**What it can do alone**:

- Run builds
- Update patch/minor dependencies
- Publish to Maven Central
- Create GitHub releases
- Execute CI/CD pipelines

**What needs approval**:

- Major dependency updates
- Build configuration changes
- Release process changes

**Example task**: "Integration Agent, publish v0.6.0 to Maven Central"

---

### 5. **Documentation Agent** (`/agents/documentation.md`)

**What it does**: Keeps docs in sync with code

**When it activates**:

- Public API changes
- Feature implemented
- Issues ask for clarification
- Manual: `/update-docs`

**What it can do alone**:

- Update KDoc (javadoc comments)
- Create migration guides
- Update README and CHANGELOG
- Generate architecture documentation
- Create code examples

**What needs approval**:

- Major doc restructuring
- Removing documentation sections

**Quality**: All public APIs must have KDoc + examples

**Example task**: "Documentation Agent, create migration guide: Screen → bindState"

---

### 6. **Experience Agent** (`/agents/experience.md`)

**What it does**: Improves developer experience

**When it activates**:

- User reports confusion
- Common mistakes detected
- New feature needs sample
- Issues labeled: `usability`

**What it can do alone**:

- Improve error messages
- Create sample applications
- Add IDE configurations
- Design DSLs and builders
- Create code snippets

**What needs approval**:

- Breaking workflow changes
- New dependencies
- Platform support changes

**Example task**: "Experience Agent, create Notes sample app with database integration"

---

### 7. **Evolution Agent** (`/agents/evolution.md`)

**What it does**: Manages technical health and architecture

**When it activates**:

- Deprecation deadlines
- Technical debt detected
- Refactoring opportunities found
- Version milestones
- Manual: `/refactor`, `/release`

**What it can do alone**:

- Track deprecation timelines
- Identify refactoring opportunities
- Migrate internal code to new patterns
- Suggest improvements
- Monitor technical debt

**What needs approval**:

- Remove deprecated public APIs
- Major architectural refactoring
- Breaking changes

**Example task**: "Evolution Agent, remove Screen class (after v1.0 migration)"

---

## How to Interact with Agents

### Option 1: Automatic (Preferred for Routine Work)

**Agents activate automatically when**:

- You commit code → Quality Agent runs tests
- You open a PR → Quality + Integration + Documentation + Contract agents validate
- You push release tag → Evolution + Integration agents coordinate release
- Issues are created → Experience Agent triages

**You don't need to do anything** - agents coordinate themselves!

### Option 2: Manual Triggers (When You Want Specific Work)

**Basic commands**:

```bash
# Run quality checks
claude /run-quality-gate

# Run build and tests
claude /run-build

# Implement a feature (orchestrates Core → Quality → Docs → Integration)
claude /implement-feature "Add CacheMiddleware class"

# Review a PR (runs all validation agents in parallel)
claude /review-pr #42

# Update documentation
claude /update-docs

# Execute release workflow
claude /release v0.6.0

# Orchestrate complex task
claude /orchestrate "Implement Phase 1 modernization"
```

### Option 3: Directed Requests

**Ask specific agent**:

```bash
# Ask Core Agent to implement something
claude @core "Implement ErrorHandlingMiddleware"

# Ask Contract Agent to review API
claude @contract "Deprecate Screen class"

# Ask Documentation Agent to update docs
claude @documentation "Create migration guide for bindState"

# Ask Experience Agent to improve something
claude @experience "Improve error message for dispatcher validation"
```

---

## Typical Workflows

### Workflow 1: Implement a Feature

**Goal**: Add `CacheMiddleware` to the framework

**Process** (fully automated):

```
1. You: /implement-feature "Add CacheMiddleware class"

2. Core Agent:
   - Implements CacheMiddleware.kt
   - Adds to library/src/main/

3. Quality Agent:
   - Writes CacheMiddlewareTest.kt
   - Runs tests → ✅ Pass
   - Coverage: 92%

4. Documentation Agent:
   - Adds KDoc to CacheMiddleware
   - Creates example: "How to cache API responses"
   - Updates CHANGELOG

5. Integration Agent:
   - Runs full build → ✅ SUCCESS
   - Runs ktlint → ✅ PASS
   - All quality gates pass → ✅ READY TO MERGE

Result: Feature complete, tested, documented, ready for production
```

**Your involvement**: 0 (fully automated!)

---

### Workflow 2: Fix a Bug

**Goal**: StateManager not properly clearing cache on state update

**Process** (agent coordination):

```
1. Issue created: "StateManager cache not cleared"

2. Quality Agent:
   - Reproduces bug with test
   - Identifies root cause: Line 47 in StateManager.kt

3. Core Agent:
   - Fixes the bug (backward compatible)

4. Quality Agent:
   - Verifies test now passes
   - All regression tests still pass

5. Documentation Agent:
   - Adds explanation in CHANGELOG
   - Updates StateManager KDoc if needed

6. Integration Agent:
   - Publishes hotfix: v0.5.1
   - Tags release

Result: Bug fixed, tested, released
```

**Your involvement**: Watch the process (agents handle it)

---

### Workflow 3: Review Pull Request

**Goal**: Team member submits PR for new feature

**Process** (parallel validation):

```
1. PR opened

2. All agents activate in parallel:

   Quality Agent          Integration Agent        Documentation Agent     Contract Agent
   - Runs tests          - Builds project         - Checks docs complete  - Checks breaking changes
   - 42/42 ✅ pass       - ktlint: ✅ pass        - ✅ KDoc complete      - ✅ No breaking changes
   - Coverage: 87% ✅    - SonarQube: ✅ pass     - ✅ Examples updated    - ✅ API compatible

3. Orchestrator aggregates results:
   ✅ All gates passed
   ✅ Ready to merge

   Comment on PR: "@feature-author All quality gates passed!
   ✅ Tests: 42/42 (87% coverage)
   ✅ Build: SUCCESS
   ✅ Docs: Complete
   ✅ API: Compatible
   Ready to merge!"

Result: PR approved automatically
```

**Your involvement**: Merge the PR (or agents can auto-merge if configured)

---

### Workflow 4: Release to Maven Central

**Goal**: Release v0.6.0

**Process** (orchestrated):

```
1. You: /release v0.6.0

2. Evolution Agent:
   - Reviews deprecation timeline
   - ✅ Screen.kt is properly deprecated
   - ✅ Ready for release

3. Contract Agent:
   - Verifies no breaking changes
   - ✅ All APIs finalized
   - Creates release notes

4. Quality Agent (parallel):
   - Runs full test suite
   - ✅ 100% pass rate
   - ✅ Coverage 89%

5. Integration Agent (parallel):
   - Builds library
   - Generates Javadoc JAR
   - Signs artifacts with GPG
   - Publishes to Maven Central
   - ✅ v0.6.0 published

6. Orchestrator:
   - Creates GitHub release
   - Tags repository: v0.6.0
   - Updates documentation

Result: v0.6.0 live on Maven Central, GitHub, documentation updated
```

**Your involvement**: 1 command (`/release v0.6.0`)

---

## Quality Gates (Non-Negotiable)

These gates are **always enforced** by the Quality Agent:

✅ **Tests**: 100% pass rate (all tests must pass)
✅ **Coverage**: >85% minimum
✅ **Build**: `BUILD SUCCESSFUL`
✅ **ktlint**: Zero violations
✅ **SonarQube**: Quality gate passes
✅ **Performance**: No regression >10%

**If any gate fails**:

- PR is blocked from merging
- Responsible agent is notified
- Human is asked to fix and retry

---

## Safety Mechanisms

### 1. Automatic Rollback

If something goes wrong (e.g., build fails):

```
1. Integration Agent detects failure
2. Auto-rolls back to last good state
3. Creates issue: "Build failure in v0.6.0 release"
4. Notifies you with details
```

### 2. Circuit Breaker

If an agent fails 3 times in a row:

```
1. Agent enters "disabled" mode
2. All future actions require human approval
3. You investigate and fix
4. Agent re-enabled after successful human-approved action
```

### 3. Human Review Required For

- Breaking API changes
- Removal of deprecated APIs
- Major dependency upgrades
- Release to production
- Coverage threshold reduction

---

## Checking Agent Status

**See what agents are currently doing**:

```bash
# Check active agents
claude /status

# See agent logs
claude /logs agent-name

# See quality gate results
claude /quality-report

# See coordination map
claude /workflow-status
```

---

## Customizing Agent Behavior

### Adjust Autonomy Boundaries

Edit `.claude/guardrails/approved-actions.json`:

```json
{
  "core_agent": {
    "approved_without_review": [
      "implement_private_middleware_class",
      "optimize_internal_algorithm"
      // Add more pre-approved actions
    ]
  }
}
```

### Add New Triggers

Edit `.claude/config/agent-activation.json`:

```json
{
  "file_triggers": {
    "my_new_trigger": {
      "files": ["my/path/*.kt"],
      "agents": ["core", "quality"],
      "mode": "sequential"
    }
  }
}
```

### Disable an Agent

Comment out in agent definition, or:

```bash
# Temporarily disable an agent
claude @core --disable

# Re-enable
claude @core --enable
```

---

## Troubleshooting

### "PR blocked because tests failed"

**What happened**: Quality Agent ran tests, some failed
**How to fix**:

1. Look at test output in PR comment
2. Fix the failing code
3. Push changes → agents re-run tests automatically

### "Build succeeded but coverage dropped"

**What happened**: Coverage fell below 85%
**How to fix**:

1. Quality Agent reports which files lost coverage
2. Add tests for those files
3. Push changes → agents re-validate

### "Agent seems stuck"

**What to do**:

```bash
# Check agent logs
claude /logs agent-name

# Manually trigger restart
claude @agent-name --restart

# Check circuit breaker status
claude /circuit-breaker-status
```

---

## Best Practices

### 1. Use `/implement-feature` for Significant Work

```bash
claude /implement-feature "Add RetryMiddleware for resilience"
```

This orchestrates all agents in the right sequence.

### 2. Commit Early and Often

Agents run tests on each commit, catching issues fast.

### 3. Use Descriptive Commit Messages

Agents learn from commit messages to understand your intent.

### 4. Review Agent Feedback

Check PR comments from agents for suggestions.

### 5. Trust the Quality Gates

Don't skip tests. Tests catch bugs early.

---

## Getting Help

### Agent Documentation

- Core: `.claude/agents/core.md`
- Contract: `.claude/agents/contract.md`
- Quality: `.claude/agents/quality.md`
- Integration: `.claude/agents/integration.md`
- Documentation: `.claude/agents/documentation.md`
- Experience: `.claude/agents/experience.md`
- Evolution: `.claude/agents/evolution.md`

### Configuration

- Activation rules: `.claude/config/agent-activation.json`
- Guardrails: `.claude/guardrails/approved-actions.json`

### Questions?

```bash
claude /help agents
claude /help workflow
claude /help quality-gates
```

---

## Next Steps

1. **Try the automatic workflow**: Commit code, watch agents activate
2. **Try a manual workflow**: `/implement-feature "Add NewMiddleware"`
3. **Check agent coordination**: Look at PR comments from agents
4. **Review agent logs**: See what agents are doing
5. **Customize guardrails**: Adjust autonomy for your team

---

## The Agent Team is Now Ready!

You have a **fully-operational distributed development team** that:

✅ **Coordinates automatically** - agents work together without you directing each one
✅ **Maintains quality** - comprehensive testing and validation on every change
✅ **Keeps docs in sync** - documentation updates automatically with code
✅ **Manages releases** - from implementation to Maven Central publication
✅ **Improves ergonomics** - focuses on developer experience
✅ **Maintains health** - tracks technical debt and refactoring opportunities

**Start using them**! The more you commit, the more they help you.
