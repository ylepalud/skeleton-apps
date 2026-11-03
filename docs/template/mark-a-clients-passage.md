# Mark a client's passage

A generated project keeps only the clients chosen at installation ([Choose the clients](start-a-new-project.md#choose-the-clients)). What belongs to one client is left out in two ways, and every change of the template keeps them true.

## Whole folders and files

A client's own folders and files are listed in `scripts/installer/src/clients.js` (`CLIENT_PATHS`): its app, its convention plugin and TestKit test, its workflow, its pages and ADRs. A new file that belongs to one client goes there, or under a folder already listed.

## Passages in shared files

Everything else, in a file every project keeps, is a marked block or line. The names are `web`, `cli`, `mobile`, and `native` for the shared module (kept with the CLI or mobile); `!web` keeps a passage only without the web app.

| Where | Block | One line |
| --- | --- | --- |
| YAML, shell, properties | `# client:mobile:start` ... `# client:mobile:end` | `- item  # client:mobile` |
| Kotlin, Gradle | `// client:mobile:start` ... `// client:mobile:end` | `line // client:mobile` |
| Markdown | `<!-- client:mobile:start -->` ... `<!-- client:mobile:end -->`, around whole sections or paragraphs | `sentence <!-- client:mobile -->`; in a table, inside the last cell: `cell <!-- client:mobile --> \|` |
| Mermaid | `%% client:web:start` ... `%% client:web:end` | |

A block marker stands alone on its line; blocks may nest. In Markdown, a block never sits inside a table or a list, which it would break: mark each row or item, or write a paragraph one sentence per line and mark the sentences. A marked numbered list counts `1.` for every item, so that no number goes missing.

## Checks

`npm run installer:test` generates all seven combinations from the template in memory and fails on an unpaired or unknown marker, a link to a page a project does not have, a path of a client it left out, a Gradle project it does not include, or a workflow it does not have. The `installer` workflow then builds three combinations for real on every pull request, and all seven every week ([ADR 0031](../architecture/decisions/0031-installer-clients-as-marked-blocks.md)).
