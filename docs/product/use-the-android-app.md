# Use the Android app

The Android app keeps the same personal notes as the web app: a note written on one shows on the other, for the same account.

## Sign in

Open **Notes** and tap **Sign in**. The identity provider's page opens in a browser tab inside the app; once you sign in there, you are back on your notes. The session is kept on the device, encrypted with a key that never leaves it: you sign in again only after signing out, or when the provider ends the session.

Locally, the provider is a mock that accepts any username: see [Run the Android app](../contributor/how-to/run-the-android-app.md).

## Manage notes

| To | Do |
| --- | --- |
| write a note | **New note**, fill in **Title** (required, up to 200 characters) and **Content** (up to 20,000), then **Add note** |
| read your notes | they are listed under **Your notes**, most recently updated first; **Load more** shows the next ones |
| change a note | tap it, change it, then **Save**; the system back button leaves it as it was |
| delete a note | tap it, **Delete**, then confirm with **Delete**, or **Keep** it; a deleted note cannot be restored |

When the API cannot be reached, the app says so and offers **Retry**.

## Sign out

**Sign out** forgets the session on this device.
