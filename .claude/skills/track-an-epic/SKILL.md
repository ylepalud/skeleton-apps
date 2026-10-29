---
name: track-an-epic
description: Keep an epic's Progress diagrams up to date. Use when a sub-issue of an epic starts, opens or merges a pull request, gets a CI result or closes, or when reporting an epic's progress.
---

Follow [docs/contributor/how-to/track-an-epic.md](../../../docs/contributor/how-to/track-an-epic.md): read the epic's current body, change only the lines the step affects, write it back with `gh issue edit <epic> --body-file`.

Done when:
- the STATUS, DEPENDENCIES, MERGES and NEXT ACTIONS blocks all reflect the step, and their columns stay aligned;
- the "Last update" line names the date and the step;
- an epic without a Progress section gets one, from the page's template.
