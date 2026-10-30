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

When the API cannot be reached, the app says so and offers **Retry**.

## Sign out

**Sign out** forgets the session on this device.
