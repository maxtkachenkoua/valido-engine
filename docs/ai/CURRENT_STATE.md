# Current State

Valido Engine MVP is a Maven multi-module Java platform for static tool-site generation.

## Modules

- `valido-core`: domain model, value types, diagnostics, validation results, algorithm contracts, algorithm metadata, `ProjectModel`, `RouteModel`, `ExportPlan`.
- `valido-content`: YAML and Markdown loading, raw DSL DTOs, content validation, mapping into core models.
- `valido-generator`: orchestration, route generation, related link resolution, `ProjectModel` assembly, `ExportPlan` creation, generation report.
- `valido-exporter-html`: static HTML export, sitemap, robots, search index, generic site asset copying.
- `valido-exporter-api`: static `api-metadata.json` export.
- `valido-cli`: `doctor`, `stats`, `publish`, output formatting, exit codes.
- `valido-algorithms`: algorithm module boundary and test contracts.
- `valido-examples`: architecture-compliant example content.

## Current Generic Capabilities

- YAML DSL loading.
- Markdown content loading.
- Required field, unknown field, duplicate ID, broken reference, algorithm binding, and capability compatibility validation.
- Deterministic route generation.
- Related link resolution.
- Export planning.
- Static HTML export.
- Static API metadata export.
- Sitemap, robots, and search index generation.
- Generic copying of site-owned assets.
- CLI doctor, stats, and publish.

## Extension Points

- Add generic core model fields only through explicit architecture decisions.
- Add content DSL support only when it is platform-level.
- Add exporters only when they consume `ProjectModel` and `ExportPlan`.
- Add product browser behavior in product repositories, not Engine.

## Known Limitations

- No runtime REST API.
- No database.
- No runtime HTML rendering.
- No Java execution for browser-capable product tools.
- CLI `--output` parsing exists, but generation uses `site.yaml` output until generic override behavior is implemented.
