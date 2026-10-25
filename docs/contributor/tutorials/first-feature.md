# Add your first feature

In this tutorial you let users **pin** a note so that it stays at the top of their list. The change is small but crosses every layer, in the order the repository expects: contract, collection, domain, use cases, persistence, REST adapter, web app, tests. Expect about an hour.

You need the repository building (`./gradlew check` passes) and Docker running. Work on a branch:

```bash
git switch -c feat/pin-notes
```

## 1. Change the contract

The contract comes first. In `contracts/api/notes.yaml`, add `pinned` to `NoteRequest`, optional with a default so that existing clients keep working:

```yaml
    NoteRequest:
      # ...
      properties:
        # title, content...
        pinned:
          type: boolean
          description: Pinned notes come first in the list.
          default: false
          example: false
```

Add it to `Note` too, this time required (a response always says whether the note is pinned):

```yaml
    Note:
      type: object
      required: [id, title, content, pinned, createdAt, updatedAt]
      properties:
        # ...
        pinned:
          type: boolean
          example: false
```

Update the explicit request examples of `createNote` and `updateNote` (add `pinned: false` and `pinned: true`), and the `listNotes` summary, since the order changes: `List the caller's notes, pinned ones first, then most recently updated first.`

## 2. Regenerate the collection

```bash
./gradlew :bruno:updateCollection
```

The requests in `bruno/notes` now carry `pinned`. The list request's file is renamed after its new summary. Commit these files with the contract: `check` fails when the collection drifts from it.

## 3. Let the compiler guide you

```bash
./gradlew :services:api:compileJava
```

It fails in `NoteRestMapper`: the generated `NoteDto` constructor now expects `pinned`. The contract drives the code; the next steps make the domain able to answer.

## 4. The domain and the use cases

In `notes/domain/Note.java`, add the state and let the existing business methods carry it:

```java
  private boolean pinned;

  public static Note create(
      UUIDGenerator uuidGenerator, OwnerId owner, Title title, Content content, boolean pinned, Clock clock) { ... }

  public static Note restore(
      NoteId id, OwnerId owner, Title title, Content content, boolean pinned, Instant createdAt, Instant updatedAt) { ... }

  /** Replaces the title, content and pin, and records when the note was last changed. */
  public void edit(Title title, Content content, boolean pinned, Clock clock) { ... }

  /** Returns whether this note comes first in the list. */
  public boolean isPinned() {
    return pinned;
  }
```

The use cases receive it through their commands:

```java
public record CreateNoteCommand(OwnerId owner, Title title, Content content, boolean pinned) {}
public record UpdateNoteCommand(OwnerId owner, NoteId noteId, Title title, Content content, boolean pinned) {}
```

and pass `command.pinned()` to `Note.create` and `note.edit`. Update the Javadoc of `NoteRepository.findByOwner` and `ListNotesUseCase` to state the new order: the port's contract changes, not only its implementation.

## 5. Persistence

Never change an applied changeset: add one. Create `services/api/src/main/resources/db/changelog/notes/002-add-pinned.yaml`:

```yaml
databaseChangeLog:
  - changeSet:
      id: notes-002-add-pinned
      author: you
      changes:
        - addColumn:
            schemaName: notes
            tableName: notes
            columns:
              - column:
                  name: pinned
                  type: boolean
                  defaultValueBoolean: false
                  constraints:
                    nullable: false
```

and include it after `001-create-notes.yaml` in `db.changelog-master.yaml`. Existing notes get `false`.

In `adapter/out/postgres/NoteJpaEntity.java`, add the column and map it both ways:

```java
  @Column(nullable = false)
  private boolean pinned;
```

`fromDomain` passes `note.isPinned()`, `toDomain` passes `pinned` to `Note.restore`. Hibernate validates the mapping against the schema at startup, so a mismatch fails the integration tests.

Then the order, in `PostgresNoteRepository`:

```java
  private static final Sort PINNED_THEN_MOST_RECENTLY_UPDATED_FIRST =
      Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("updatedAt"), Sort.Order.asc("id"));
```

## 6. The REST adapter

`NoteRestMapper.toDto` passes `note.isPinned()` to the `NoteDto` constructor. In `NotesController`, `pinned` is optional in the request, so the generated getter is `@Nullable` and NullAway insists that you decide what absent means:

```java
  // Optional in the contract, so that existing clients keep working: absent means not pinned.
  private static boolean isPinned(NoteRequestDto request) {
    return Boolean.TRUE.equals(request.getPinned());
  }
```

Pass `isPinned(request)` to both commands. `./gradlew :services:api:compileJava` now passes.

## 7. Tests for the API

```bash
./gradlew :services:api:compileTestJava :services:api:compileIntegrationTestJava
```

The compiler lists every test that builds a note or a command; add the new argument there. `NoteTestDataBuilder` gets a `pinned()` method so tests can ask for a pinned note. Then add tests for the behaviour, not only for the compiler:

- `NoteTest`: a new note is not pinned, `edit` pins it.
- `NoteRepositoryContractTest`: a new case, "pinned notes come first, whatever their update time", and `isPinned` in the round-trip assertions. Being a port contract, it runs against the PostgreSQL adapter in `PostgresNoteRepositoryTest`.
- `NoteUseCasesTest` and `NotesControllerTest`: pass and check `pinned`.

```bash
./gradlew spotlessApply
./gradlew :services:api:check
```

## 8. The web app

The TypeScript client is regenerated on every build, so `Note` now has `pinned` and `NoteRequest` an optional `pinned`.

In `notes/notes-page.ts`, add the control to the form and fill it when editing:

```ts
    pinned: [false],
// ...
    this.form.setValue({ title: note.title, content: note.content, pinned: note.pinned });
```

In `notes-page.html`, a checkbox in the form and a badge in the list:

```html
    <label class="flex items-center gap-2 text-sm font-medium text-slate-700">
      <input type="checkbox" formControlName="pinned" class="size-4 accent-indigo-700" />
      Pin to the top
    </label>
```

```html
            @if (note.pinned) {
              <span class="ml-2 rounded bg-indigo-100 px-1.5 py-0.5 text-xs font-medium text-indigo-800">Pinned</span>
            }
```

The in-memory `FakeNotesClient` (`notes-client.fake.ts`) used by the tests must behave like the API: store `pinned` (`request.pinned ?? false`) and list pinned notes first. Then a test in `notes-page.spec.ts`: edit the second note, tick "Pin to the top", save, and check it now comes first with its badge.

```bash
./gradlew :services:api-app:check
```

## 9. See it work

```bash
./gradlew check
./gradlew startLocalStack
```

Open http://localhost:8088, write two notes, pin the older one: it moves to the top. Then run the end-to-end tests and replay the collection:

```bash
E2E_BASE_URL=http://localhost:8088 ./gradlew :services:api-app:npmRunE2e -PrunE2e=true
cd bruno && npm ci && npm run smoke
```

`./gradlew stopLocalStack` when you are done.

## What you practised

- The contract changed first, and the compiler then pointed at every place to update.
- The domain owns the rule; only the adapters know about JSON, JPA and SQL.
- A schema change is a new changeset, never an edited one.
- The port's contract test proves the new ordering against the real database.
- An optional field keeps existing clients working; NullAway made the default explicit.

Commit with a Conventional Commit (`feat(notes): let users pin notes`) and open a pull request to `develop`.
