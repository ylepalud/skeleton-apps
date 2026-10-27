# Use the web app

The web app keeps personal notes. Every note belongs to the account that wrote it: nobody else can read, change or delete it.

## Sign in

Open the app. If you are not signed in, it sends you to the identity provider, which asks for your credentials and brings you back to your notes. The session renews itself while the app is open; you sign in again only after a long absence or after signing out.

Locally, the provider is a mock that accepts any username: see [Run locally](../contributor/how-to/run-locally.md).

## Manage notes

| To | Do |
| --- | --- |
| write a note | fill in **Title** (required, up to 200 characters) and **Content** (up to 20,000), then **Add note** |
| read your notes | they are listed under **Your notes**, most recently updated first, a page at a time (**Previous**, **Next**) |
| change a note | **Edit**, change it, then **Save**; **Cancel** leaves it as it was |
| delete a note | **Delete**, then confirm with **Delete**, or **Keep** it; a deleted note cannot be restored |

When the API refuses a request, the app shows its message and, when there is one, a hint on what to do.

## Sign out

**Sign out** revokes your tokens and ends your session with the identity provider.
