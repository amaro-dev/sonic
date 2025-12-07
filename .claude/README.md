# Sonic Distributed Agent System

Welcome! You now have a **self-organizing team of 7 specialized agents** that work together to develop, test, document,
and release the Sonic MVI framework.

## Quick Start

### 1. Understand the System (5 minutes)

Read in this order:

1. **This file** (you're reading it!)
2. **AGENTS_GUIDE.md** - How to use the agents (practical examples)
3. **ARCHITECTURE.md** - How the system works (technical details)

### 2. See It In Action (Immediately)

Just work normally:

```bash
# Make changes
echo "new code" >> library/src/main/java/dev/amaro/sonic/MyFeature.kt

# Commit and push
git add .
git commit -m "Add MyFeature"
git push

# Watch agents activate automatically!
# - Quality Agent runs tests
# - Integration Agent runs build
# - Documentation Agent checks docs
# - Contract Agent verifies API
```

### 3. Try a Workflow (10 minutes)

```bash
# Ask agents to implement a feature
claude /implement-feature "Add ErrorHandlingMiddleware"

# Watch the magic happen:
# 1. Core Agent implements the code
# 2. Quality Agent writes tests
# 3. Documentation Agent creates docs
# 4. Integration Agent runs build
# Result: Complete feature ready to merge!
```

---

## The 7 Agents at a Glance

| Agent             | What It Does               | When It Activates       | Example                  |
|-------------------|----------------------------|-------------------------|--------------------------|
| **Core**          | Implements framework logic | StateManager.kt changes | Optimize algorithm       |
| **Contract**      | Guards public APIs         | Interface file changes  | Deprecate old API        |
| **Quality**       | Tests and validates        | Code committed          | Run test suite           |
| **Integration**   | Builds and releases        | PR opened               | Publish to Maven Central |
| **Documentation** | Keeps docs in sync         | Feature implemented     | Update CHANGELOG         |
| **Experience**    | Improves DX                | Error message unhelpful | Better error message     |
| **Evolution**     | Manages technical health   | Deprecation deadline    | Remove old code          |

**In one sentence**: Core writes code → Quality tests it → Documentation explains it → Integration releases it →
Experience polishes it → Evolution maintains health → Contract guards APIs.

---

## 3 Ways to Use Agents

### Option 1: Automatic (Do Nothing!)

```bash
# Just work normally
git commit -m "Add feature"
git push

# Agents automatically activate and handle:
# ✅ Tests run
# ✅ Build succeeds
# ✅ Docs update
# ✅ Quality gates verified
# ✅ Ready to merge
```

### Option 2: Manual Trigger (Explicit Commands)

```bash
# Implement a feature (orchestrates all agents)
claude /implement-feature "Add CacheMiddleware"

# Review a PR (runs all validation agents)
claude /review-pr #42

# Run quality checks
claude /run-quality-gate

# Execute release
claude /release v0.6.0
```

### Option 3: Direct Request (Ask Specific Agent)

```bash
# Ask Core to implement something
claude @core "Implement RetryMiddleware"

# Ask Quality to run tests
claude @quality "Run full test suite"

# Ask Documentation to update docs
claude @documentation "Create migration guide"
```

---

## Key Concepts

### Autonomy (What Agents Can Do Alone)

**Core Agent can**:

- ✅ Implement new middleware
- ✅ Optimize algorithms
- ✅ Fix bugs
- ❌ Change public API (needs approval)

**Quality Agent can**:

- ✅ Write tests
- ✅ Run tests
- ✅ Generate coverage reports
- ❌ Skip tests (never!)

**Integration Agent can**:

- ✅ Run builds
- ✅ Update patch/minor dependencies
- ✅ Publish to Maven Central
- ❌ Update major dependencies (needs approval)

*(See AGENTS_GUIDE.md for complete autonomy matrix)*

### Coordination (How Agents Work Together)

**Sequential** (one after another):

```
Core implements → Quality tests → Documentation explains → Integration publishes
```

**Parallel** (all at once):

```
Quality tests
Integration builds      } All must pass → Ready to merge
Documentation checks
Contract verifies
```

### Quality Gates (Never Skipped)

These gates are **always enforced**:

- ✅ Tests: 100% pass rate
- ✅ Coverage: >85% minimum
- ✅ Build: SUCCESS
- ✅ ktlint: Zero violations
- ✅ SonarQube: Quality gate passes

If any gate fails → PR is blocked until fixed.

---

## File Structure

```
.claude/
├── README.md                          # This file!
├── AGENTS_GUIDE.md                    # How to use agents (practical)
├── ARCHITECTURE.md                    # How system works (technical)
│
├── agents/                            # Agent definitions
│   ├── core.md                        # Core implementation agent
│   ├── contract.md                    # API contract agent
│   ├── quality.md                     # Testing & validation agent
│   ├── integration.md                 # Build & release agent
│   ├── documentation.md               # Knowledge management agent
│   ├── experience.md                  # Developer ergonomics agent
│   └── evolution.md                   # Technical health agent
│
├── config/                            # System configuration
│   └── agent-activation.json          # Event-to-agent trigger mapping
│
├── guardrails/                        # Safety boundaries
│   └── approved-actions.json          # What agents can do autonomously
│
├── commands/                          # Slash commands (future)
│   ├── implement-feature.md           # /implement-feature
│   ├── review-pr.md                   # /review-pr
│   └── release.md                     # /release
│
└── hooks/                             # Git hooks (future)
    └── pre-commit                     # Activates Quality Agent
```

---

## Configuration Files Explained

### `.claude/agents/*.md` (Agent Definitions)

Each agent has a definition file describing:

- **Responsibility**: What domain does it own?
- **Autonomy**: What can it do alone?
- **Approval needed**: What requires human review?
- **Triggers**: When does it activate?
- **Coordination**: How does it work with other agents?

**Example**: `agents/core.md`

```markdown
# Core Agent
Domain: Core Framework Logic Implementation

Autonomy:
✅ Implement new middleware
✅ Optimize algorithms
✅ Fix bugs
❌ Change public API (needs approval)

Triggers:
- StateManager.kt changes
- Issues labeled: core-logic
- /implement-feature command
```

### `.claude/config/agent-activation.json`

Maps **triggers** to **agents**:

```json
{
  "file_triggers": {
    "library_core_impl": {
      "files": ["library/src/main/java/dev/amaro/sonic/*.kt"],
      "agents": ["core", "quality", "documentation"],
      "mode": "sequential"
    }
  }
}
```

**Translation**: "When library core files change, activate Core Agent → Quality Agent → Documentation Agent in
sequence."

### `.claude/guardrails/approved-actions.json`

Defines **pre-approved actions** (no human approval needed):

```json
{
  "core_agent": {
    "approved_without_review": [
      "implement_private_middleware_class",
      "optimize_internal_algorithm"
    ],
    "requires_human_review": [
      "change_public_interface_signature"
    ]
  }
}
```

---

## Common Workflows

### Workflow 1: Implement a Feature

```bash
# Command
claude /implement-feature "Add RetryMiddleware"

# What happens:
1. Core Agent implements RetryMiddleware.kt
2. Quality Agent writes tests → runs → ✅ passes
3. Documentation Agent writes docs + examples
4. Integration Agent runs build → ✅ succeeds
Result: Feature ready to merge!
```

### Workflow 2: Review a PR

```bash
# PR is opened automatically triggers:
Quality Agent (parallel):      Integration Agent (parallel):
- Runs tests ✅               - Builds project ✅
- Coverage 87% ✅             - ktlint passes ✅
                              - SonarQube passes ✅

Documentation Agent:          Contract Agent:
- Docs complete ✅            - No breaking changes ✅

Result: All gates pass → Ready to merge ✅
```

### Workflow 3: Release v0.6.0

```bash
# Command
claude /release v0.6.0

# What happens:
1. Evolution Agent: Verifies deprecation timeline
2. Contract Agent: Finalizes API documentation
3. Quality Agent: Full test suite ✅ passes
4. Integration Agent: Publishes to Maven Central ✅
Result: v0.6.0 live!
```

---

## Safety Mechanisms

### Automatic Rollback

If something goes wrong, agents automatically rollback and notify you:

```
Build fails → Rollback → Notify human → Create issue for investigation
```

### Circuit Breaker

If an agent fails 3 times in a row, it disables itself and requires human approval:

```
Failure 1 → Failure 2 → Failure 3 → DISABLED (requires human review to re-enable)
```

### Audit Trail

All agent actions are logged to `.claude/audit/` for transparency and debugging.

---

## Customization

### Adjust Agent Autonomy

Edit `.claude/guardrails/approved-actions.json` to give agents more/less autonomy:

```json
{
  "core_agent": {
    "approved_without_review": [
      "implement_private_middleware_class",
      // Add more pre-approved actions here
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
      "files": ["my/important/path/*.kt"],
      "agents": ["core", "quality"],
      "mode": "sequential"
    }
  }
}
```

### Create Custom Commands

Create files in `.claude/commands/`:

```bash
touch .claude/commands/my-command.md
# Define what /my-command does
```

---

## Troubleshooting

### "PR blocked because tests failed"

→ Look at PR comment from Quality Agent
→ Fix the code
→ Push changes → agents re-validate automatically

### "Build succeeded but coverage dropped"

→ Quality Agent reports which files lost coverage
→ Add tests for those files
→ Push changes → agents verify coverage improved

### "Agent seems stuck"

→ Check agent logs: `claude /logs agent-name`
→ Manually trigger restart: `claude @core --restart`
→ Check circuit breaker status: `claude /circuit-breaker-status`

### "I want to customize agent behavior"

→ Edit `.claude/agents/{name}.md` to redefine scope
→ Edit `.claude/guardrails/approved-actions.json` to adjust autonomy
→ Edit `.claude/config/agent-activation.json` to change triggers

---

## Key Files to Read

1. **Start here**: This README (overview)
2. **Learn how to use**: AGENTS_GUIDE.md (practical workflows)
3. **Understand architecture**: ARCHITECTURE.md (technical deep dive)
4. **Agent details**: agents/{name}.md (what each agent does)
5. **Configuration**: config/agent-activation.json (triggers and coordination)
6. **Safety**: guardrails/approved-actions.json (autonomy boundaries)

---

## The Agent Team

You have:

| Agent                  | Status                     |
|------------------------|----------------------------|
| 🧠 Core Agent          | ✅ Ready to implement       |
| 🛡️ Contract Agent     | ✅ Ready to guard APIs      |
| ✅ Quality Agent        | ✅ Ready to validate        |
| 🔧 Integration Agent   | ✅ Ready to build & release |
| 📚 Documentation Agent | ✅ Ready to document        |
| 💎 Experience Agent    | ✅ Ready to polish          |
| 🏗️ Evolution Agent    | ✅ Ready to maintain health |

**All agents are active and ready to work!**

---

## Your First Steps

### Step 1: Understand (5 minutes)

Read AGENTS_GUIDE.md to see practical examples

### Step 2: Try It (10 minutes)

```bash
# Make a small change
echo "// comment" >> library/src/main/java/dev/amaro/sonic/Test.kt

# Commit
git add .
git commit -m "Add test comment"
git push

# Watch agents activate in PR comments!
```

### Step 3: Use It (Ongoing)

```bash
# For significant work, use the orchestrator
claude /implement-feature "Add new feature name"

# For PRs, let agents auto-validate
# (Just push normally, agents handle it)

# For releases, use the release workflow
claude /release v0.6.0
```

---

## Questions?

- **How to use agents?** → Read AGENTS_GUIDE.md
- **How does it work?** → Read ARCHITECTURE.md
- **What can Agent X do?** → Read agents/{X}.md
- **How do I customize?** → Edit config/ and guardrails/ files
- **Agent seems broken?** → Check `.claude/audit/` logs

---

## You're Ready!

Your Sonic project now has a **distributed engineering team** that:

✅ Coordinates automatically
✅ Maintains quality gates
✅ Keeps documentation in sync
✅ Manages releases
✅ Improves ergonomics
✅ Maintains technical health
✅ Guards public APIs

**Start using the agents. They're ready to help!**
