# Engine Guardrails

These rules are immutable unless changed by an explicit architectural decision.

## Engine Must Remain Generic

Engine must not know product names or product-specific tool behavior.

Engine must not contain:

- browser tool logic
- product-specific UX
- product-specific CSS or JavaScript
- Base64 implementation logic
- URL implementation logic
- JSON implementation logic
- JWT, HTML, XML, CSV, Regex, or future workbench logic

## Product-Owned Responsibilities

Products own:

- JavaScript
- CSS
- plugins
- browser behavior
- product UX
- product content

## Engine-Owned Responsibilities

Engine owns:

- generation
- routing
- validation
- ProjectModel assembly
- ExportPlan creation
- publishing
- SEO
- sitemap
- robots
- search index
- generic asset copying

## Stop Conditions

Stop and ask before implementation if:

- a feature requires Engine to know a product tool
- a change adds browser behavior to Engine
- architecture and implementation disagree
- an exporter would need to load YAML or Markdown directly
- product-specific behavior appears easier to implement in Engine

Prefer preserving architecture over fast shortcuts.
