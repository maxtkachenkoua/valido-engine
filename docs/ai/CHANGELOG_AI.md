# AI Architecture Changelog

This is AI-oriented architecture history, not release notes.

## Phase 1: Core Skeleton

Added:

- Maven multi-module structure.
- Core domain contracts.
- Content loading and validation.
- Generator orchestration.
- Basic CLI.
- Algorithm and exporter module boundaries.

Reason:

Create the architecture foundation before feature work.

Impact:

Future changes must respect module boundaries.

## Phase 2: Static Generation

Added:

- Route generation.
- Related link resolution.
- HTML ExportPlan wiring.
- Static HTML export.
- Sitemap, robots, search index.
- CLI publish.
- API metadata export.

Reason:

Make Engine capable of producing a deployable static site.

Impact:

`ProjectModel` and `ExportPlan` became the core boundary between generator and exporters.

## Phase 4: Product Asset Separation

Changed:

- Site-owned assets moved to product repositories.
- Engine copies assets generically.
- Product-specific browser behavior moved out of Engine.

Reason:

Engine must remain a generic platform. ValidoHub owns product UX.

Impact:

Future tools should not require Engine changes unless a generic platform capability is missing.
