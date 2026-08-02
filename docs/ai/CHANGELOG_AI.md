# AI Architecture Changelog

This is AI-oriented architecture history, not release notes.

## 2026-08-02 - HTML Exporter Throughput And Progress

Changed:

- Added exporter-side route indexes for hreflang and tool route lookup so static HTML export does not repeatedly scan the full route list while rendering category/tool pages.
- Added parallel HTML route writing for large projects. Configure with `VALIDO_ENGINE_EXPORT_CONCURRENCY`; default is up to 8 workers for route sets above 5,000 pages.
- Added exporter progress output for large projects. Configure with `VALIDO_ENGINE_EXPORT_PROGRESS_ITEMS`; default is every 5,000 written HTML routes.

Reason:

Large static products such as ValidoHub need the generic Engine publisher to stay bounded and observable.

Impact:

Direct ValidoHub `publish --site /Users/maxtkachenko/work/validohub/site.yaml` writes 75,075 Engine-owned HTML routes in about 10 seconds on the local machine, and the full ValidoHub release build dropped from about 7m29s to about 3m51s.

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
