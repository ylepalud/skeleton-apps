# Use the iOS app

The iOS app keeps the same personal notes as the web app and the Android app: a note written on one shows on the others, for the same account.

## Sign in

Open **Notes** and tap **Sign in**. iOS asks whether the app may use the provider to sign in, then shows the provider's page; once you sign in there, you are back on your notes. The session is kept in the device's Keychain, never synced to other devices: you sign in again only after signing out, or when the provider ends the session.

Locally, the provider is a mock that accepts any username: see [Run the iOS app](../contributor/how-to/run-the-ios-app.md).

## Manage notes

| To | Do |
| --- | --- |
| write a note | **New note**, fill in **Title** (required, up to 200 characters) and **Content** (up to 20,000), then **Add note** |
| read your notes | they are listed under **Your notes**, most recently updated first; **Load more** shows the next ones |
| change a note | tap it, change it, then **Save**; the back button leaves it as it was |
| delete a note | tap it, **Delete**, then confirm with **Delete**, or **Keep** it; a deleted note cannot be restored |

## Without network

The app keeps your notes on the device. Without network, it shows the notes it last read, under "Offline: these are the notes kept on this device.", and you keep writing, changing and deleting them: each change is labelled **Not synced** and leaves on its own once the network is back, or when you tap **Retry**. A note written offline gets its place on the server then, with nothing to redo.

## When a note changed elsewhere

If a note was changed elsewhere, on another device or in the web app, since the version your change is based on, nothing is overwritten. The note is labelled **Conflict: open to resolve**; open it to see both versions and choose:

| Choice | Result |
| --- | --- |
| **Keep mine** | your version replaces the other one |
| **Keep theirs** | the other version stays, yours is dropped |
| **Keep both** | the other version stays, and yours becomes a new note |

If you deleted a note that was changed elsewhere, choose **Delete anyway** or **Keep theirs**. If you changed a note that was deleted elsewhere, choose **Restore mine**, written again as a new note, or **Discard mine**.

## Sign out

**Sign out** forgets the session and the notes kept on this device. If some changes are not synced yet, the app asks first: **Sign out anyway** loses them, **Stay** keeps you signed in until they are sent. Signing in again with another account starts from an empty device.
