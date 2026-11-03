# Use the CLI

The `notes` command keeps the same personal notes as the web app and the mobile apps, from a terminal on macOS or Linux.

## Sign in

```bash
notes login
```

If the service's identity provider supports it, `login` prints an address and a code: open the address on any device, phone included, type the code and sign in. Otherwise it opens the sign-in page in your browser; once you sign in there, the terminal says `Signed in.`

The session is kept in the macOS Keychain, or in the Linux desktop's keyring; on a Linux server without one, in a file only you can read.

Locally, the provider is a mock that accepts any username: see [Run the CLI](../contributor/how-to/run-the-cli.md).

## Manage notes

| To | Run |
| --- | --- |
| list your notes, most recently updated first | `notes list`, then `notes list --page 1` for the next ones |
| read a note | `notes show <id>` |
| write a note | `notes add --title "Groceries" --content "Milk"` (title required, up to 200 characters; content up to 20,000) |
| change a note | `notes edit <id> --title "..."`, `--content "..."`, or both |
| delete a note | `notes delete <id>`, then confirm with `y`; `--yes` skips the question |

`notes --help`, and `--help` after any command, describe the options.

## Without network

The CLI keeps your notes on the computer. When the service cannot be reached, `notes list` shows the notes it last read under "Offline", and changes are marked `[not synced]`. They leave with the next command that reaches the service, or with `notes sync`.

## When a note changed elsewhere

A change based on an older version is never written over the other one: the note is marked `[conflict]`. `notes show <id>` shows both sides and how to resolve it:

| Command | Result |
| --- | --- |
| `notes resolve <id> mine` | your version replaces the other one; for a note deleted elsewhere, it is written again |
| `notes resolve <id> theirs` | the other version stays, yours is dropped |
| `notes resolve <id> both` | the other version stays, and yours becomes a new note |

## Sign out

`notes logout` forgets the session and the notes kept on the computer. If some changes are not synced yet, it refuses and says how many: `notes logout --force` signs out anyway and loses them.
