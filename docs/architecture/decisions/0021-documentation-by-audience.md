# 0021. Documentation organised by audience

- Status: accepted
- Date: 2026-10-25

## Context

The documentation followed Diataxis at the top level (tutorials, how-to guides, reference, explanation). Almost every page addressed contributors, so the people who use the product, start a project from the template or keep the pipeline running had no place of their own.

## Options

- One folder per reader (product, template, contributor, operations, architecture), Diataxis inside a section only where its content calls for it
- Diataxis folders kept, readers built only in the navigation of a site
- One folder per reader with Diataxis inside every section

## Decision

Chosen: one folder per reader (product, template, contributor, operations, architecture), Diataxis inside a section only where its content calls for it.

## Consequences

- A reader finds the pages written for them, on GitHub and on the site alike.
- The contributor section keeps tutorials, how-to guides and reference apart; the small sections stay flat.
- A page that serves two readers lives in one section and is linked from the other.
- In a generated project, the template section is replaced by a page naming the template version.

More: [Documentation](../../README.md).
