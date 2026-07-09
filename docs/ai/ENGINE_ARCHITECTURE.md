# Engine Architecture

Valido Engine is an architecture-first static site generation platform for tool sites.

It is intentionally generic. It knows how to load content, validate it, assemble a `ProjectModel`, plan exports, generate static artifacts, and publish them. It must not know product-specific browser behavior.

## Philosophy

- Engine generates.
- Products own behavior.
- Architecture changes require explicit decisions.
- The core model is the contract between loading, generation, exporting, and CLI.

## Generator Pipeline

1. `valido-content` loads YAML and Markdown.
2. `ContentValidator` validates required fields, unknown fields, duplicate IDs, broken references, algorithm binding, and capability compatibility.
3. `valido-generator` assembles `ProjectModel`.
4. `RouteGenerator` creates deterministic routes.
5. `RelatedLinkResolver` resolves related links.
6. `ExportPlanBuilder` creates `ExportPlan`.
7. Exporters consume `ProjectModel` and `ExportPlan`.

## Core Models

`valido-core` owns stable contracts:

- domain/value types
- diagnostics and validation results
- algorithm metadata and contracts
- `ProjectModel`
- `RouteModel`
- `ExportPlan`

Other modules depend inward on core.

## Exporters

Exporters are static artifact writers.

- HTML exporter writes static pages, sitemap, robots, search index, and copies site-owned assets.
- API exporter writes `api-metadata.json`.

Exporters must consume prepared models. They must not load DSL directly or implement product browser behavior.

## CLI And Publishing

`valido-cli` exposes `doctor`, `stats`, `publish`, and MVP placeholder commands where defined.

`publish` runs generation and static export. Runtime HTML rendering, REST APIs, databases, and server-side execution are outside Engine MVP scope.

## Product Separation

ValidoHub is one product. Future products should be possible.

Engine must stay generic. Products own JavaScript, CSS, plugins, and browser-side workbench behavior.
