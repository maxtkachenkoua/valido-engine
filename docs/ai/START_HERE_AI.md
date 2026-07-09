# Start Here AI

This is the entry point for every AI working on Valido Engine.

Do not rely on chat history. Read repository documentation first.

## Reading Order

1. `docs/ai/START_HERE_AI.md`
2. `docs/ai/ENGINE_ARCHITECTURE.md`
3. `docs/ai/CURRENT_STATE.md`
4. `docs/ai/ENGINE_GUARDRAILS.md`
5. `docs/ai/DECISIONS.md`

## When To Modify Engine

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

If unsure, stop and ask whether the change is platform-level or product-level.
