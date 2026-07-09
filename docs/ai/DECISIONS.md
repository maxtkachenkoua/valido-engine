# Decisions

Append new decisions. Never rewrite history.

## Decision: Architecture-First Engine

Reason:

The Engine exists to preserve platform boundaries before feature growth.

Alternatives considered:

- Implement product needs directly as they appear.

Consequences:

- New platform features must respect module boundaries.
- Ambiguous architecture changes require explicit decisions.

Status:

Accepted.

## Decision: Generic Engine

Reason:

Valido Engine should support ValidoHub and future static tool products.

Alternatives considered:

- Make Engine a ValidoHub-specific generator.

Consequences:

- Engine must not contain product-specific browser logic, CSS, or workbench plugins.
- Products own behavior.

Status:

Accepted.

## Decision: Product-Owned Browser Assets

Reason:

Browser workbenches evolve as product UX. Keeping them in products preserves Engine as infrastructure.

Alternatives considered:

- Store browser plugins in Engine.
- Add tool-specific template switches.

Consequences:

- Engine copies site-owned assets generically.
- Products provide JavaScript, CSS, and plugins.

Status:

Accepted.

## Decision: ExportPlan As Boundary

Reason:

Exporters should write planned artifacts from prepared models, not discover content or invent routes.

Alternatives considered:

- Let exporters build routes or read content DSL.

Consequences:

- Generator owns route and artifact planning.
- Exporters consume `ProjectModel` and `ExportPlan`.

Status:

Accepted.
