# Start Here AI

This is the entry point for every AI working on Valido Engine.

Do not rely on chat history. Read repository documentation first.

If you arrived here from the repository root, keep `AGENTS.md` in mind as the canonical short entrypoint. If you did not read it yet, read `AGENTS.md` now.

## Reading Order

1. `docs/ai/START_HERE_AI.md`
2. `docs/ai/ENGINE_ARCHITECTURE.md`
3. `docs/ai/CURRENT_STATE.md`
4. `docs/ai/ENGINE_GUARDRAILS.md`
5. `docs/ai/DECISIONS.md`

## When To Modify Engine

Valido Engine is the platform.

Modify Engine only when the change is generic platform behavior:

- content loading
- validation
- route generation
- ProjectModel assembly
- ExportPlan creation
- static export
- publishing
- SEO, sitemap, robots, search index
- generic asset copying

## When Work Belongs In ValidoHub

Work belongs in ValidoHub when it is product-specific:

- browser tool behavior
- product JavaScript
- product CSS
- workbench plugins
- Base64, URL, JSON, or future tool UX
- product copy or content

If a requested change is specific to ValidoHub, browser UX, a developer tool, Base64, URL, JSON, JWT, HTML, XML, Regex, or any future Workbench, stop. Verify whether the work belongs in the product repository instead.

Engine should evolve only when a generic capability is required.

If unsure, stop and ask whether the change is platform-level or product-level.
