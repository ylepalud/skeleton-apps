"""The acceptance tests of skeleton-apps, as data: one section per area, one entry per test.

Each test has an id, a title, its preconditions, its steps (a step starting with "$ " is a command)
and its expected results. A section of a client is a marked block: a project generated without that
client does not have it (docs/template/mark-a-clients-passage.md in the template).
"""

# An outage of the API, as the clients see it: the container that answers on port 8088 stops.
# client:web:start
OUTAGE = (
    "To simulate an outage, the tests stop the web container, which serves the API on 8088, then "
    "start it again."
)
OUTAGE_STOP = "$ docker compose stop web"
OUTAGE_START = "$ docker compose start web"
OUTAGE_WAIT = "Wait about five seconds"
# client:web:end
# client:!web:start
# OUTAGE = "To simulate an outage, the tests stop the API container, which answers on 8088, then start it again."
# OUTAGE_STOP = "$ docker compose stop api"
# OUTAGE_START = "$ docker compose start api"
# OUTAGE_WAIT = "Wait about thirty seconds, until the API is ready again"
# client:!web:end

# Each test: id, title, pre (preconditions), steps (list of str; lines starting with "$ " are
# commands), expect (list of str).

STACK = {
    "id": "S",
    "title": "Local stack",
    "intro": (
        "Every test runs against the local stack only: PostgreSQL, the mock OpenID Connect "
        "provider and the API, with the web app in front of it when the project has one, started "
        "by Gradle with Docker Compose. Run the commands from the root of the repository, on the "
        "develop branch."
    ),
    "tests": [
        {
            "id": "S-01",
            "title": "Prerequisites are installed",
            "pre": "A Mac with Apple Silicon. Nothing of the project is running.",
            "steps": [
                "Check Docker with Compose v2 and Git:",
                "$ docker compose version",
                "$ git status -sb",
                "Check the Android SDK variable:",  # client:mobile
                "$ echo $ANDROID_HOME",  # client:mobile
                "Check Xcode and XcodeGen for the iOS tests:",  # client:mobile
                "$ xcode-select -p && xcodegen --version",  # client:mobile
            ],
            "expect": [
                "docker compose prints a version 2.x or later.",
                "git shows the develop branch, with no local change.",
                "ANDROID_HOME points to the Android command-line tools (for example /opt/homebrew/share/android-commandlinetools).",  # client:mobile
                "xcode-select prints /Applications/Xcode.app/Contents/Developer, and xcodegen prints its version.",  # client:mobile
                "Java is not required: Gradle downloads its own JDK (Temurin 25).",
            ],
        },
        {
            "id": "S-02",
            "title": "The whole stack starts",
            "pre": "S-01 passed. Ports 8088, 8090 and 8091 are free.",
            "steps": [
                "Build the images and start the stack:",
                "$ ./gradlew startLocalStack",
            ],
            "expect": [
                "The build ends with BUILD SUCCESSFUL.",
                "The last line reads: Local stack ready: http://localhost:8088 (identity provider: http://localhost:8090).",
            ],
        },
        {
            "id": "S-03",
            "title": "Every container runs",
            "pre": "S-02 passed.",
            "steps": ["$ docker compose --profile app ps"],
            "expect": [
                "postgres, mock-oauth2-server and api are listed, running; postgres and api report healthy.",
                "web is listed too, running.",  # client:web
            ],
        },
        {
            "id": "S-04",
            "title": "The API is ready, the identity provider answers",
            "pre": "S-02 passed.",
            "steps": [
                "$ curl -s http://localhost:8091/actuator/health/readiness",
                "$ curl -s http://localhost:8090/default/.well-known/openid-configuration",
            ],
            "expect": [
                'The readiness probe answers {"status":"UP"}.',
                'The discovery document is JSON, with "issuer": "http://localhost:8090/default" and an authorization_endpoint and a token_endpoint under that issuer.',
            ],
        },
        {
            "id": "S-05",
            "title": "No anonymous access",
            "pre": "S-02 passed.",
            "steps": ["$ curl -si http://localhost:8088/api/v1/notes"],
            "expect": ["The API answers HTTP 401 with an empty body: no route is open without a token."],
        },
        # client:web:start
        {
            "id": "S-06",
            "title": "The web app is served safely",
            "pre": "S-02 passed.",
            "steps": ["$ curl -sI http://localhost:8088/"],
            "expect": [
                "The web root answers HTTP 200 with Content-Type: text/html.",
                "The response carries a Content-Security-Policy that includes frame-ancestors 'none' and object-src 'none', and X-Content-Type-Options: nosniff.",
            ],
        },
        # client:web:end
    ],
}

API = {
    "id": "B",
    "title": "API with Bruno",
    "intro": (
        "The Bruno collection in apps/bruno is generated from the OpenAPI contract and runs as the "
        "user alice, with a client credentials token from the mock provider. The curl tests "
        "after it check, step by step, what the collection only smokes: versions, validation, "
        "ownership and limits. In the commands, B is http://localhost:8088/api/v1."
    ),
    "tests": [
        {
            "id": "B-01",
            "title": "The collection runs green from the command line",
            "pre": "The stack is running (S-02).",
            "steps": [
                "$ cd apps/bruno",
                "$ npm ci",
                "$ npm run smoke",
                "$ cd ../..",
            ],
            "expect": [
                "The execution summary shows Status PASS.",
                "Requests: 5 (5 Passed). Tests: 5/5.",
                "No request answers 401, 403 or a 5xx.",
            ],
        },
        {
            "id": "B-02",
            "title": "The collection runs in the Bruno app",
            "pre": "The Bruno desktop app is installed. The stack is running.",
            "steps": [
                "In Bruno, open the collection folder apps/bruno.",
                "Select the environment Stack (top right).",
                "Open notes > Write a new note and send it.",
                "Open notes > List the caller's notes, most recently updated first and send it.",
            ],
            "expect": [
                "Bruno fetches a token by itself (OAuth2, client credentials) before the first request.",
                "Write a new note answers 201 Created, with an ETag header and the note in JSON.",
                "The list answers 200, with the note in items.",
            ],
        },
        {
            "id": "B-03",
            "title": "A token for a test user",
            "pre": "The stack is running.",
            "steps": [
                "Set the API address, then get a token for alice and one for bob:",
                "$ B=http://localhost:8088/api/v1",
                "$ tok() { curl -s -X POST localhost:8090/default/token -d grant_type=client_credentials -d client_id=$1 -d client_secret=x | python3 -c 'import sys,json;print(json.load(sys.stdin)[\"access_token\"])'; }",
                "$ ALICE=$(tok alice); BOB=$(tok bob)",
                "$ echo ${ALICE:0:20}",
            ],
            "expect": [
                "Both variables hold a JWT (three dot-separated parts starting with eyJ).",
                "The token's subject is the client id: alice and bob are two different owners.",
            ],
        },
        {
            "id": "B-04",
            "title": "Write a note: 201, Location and ETag",
            "pre": "B-03 done in the same terminal.",
            "steps": [
                "$ curl -si -X POST $B/notes -H \"Authorization: Bearer $ALICE\" -H 'Content-Type: application/json' -d '{\"title\":\"Acceptance\",\"content\":\"First\"}'",
                "Copy the id of the answer into a variable:",
                "$ ID=<the id>",
            ],
            "expect": [
                "HTTP 201, with Location: /api/v1/notes/<id> and ETag: \"0\".",
                "The body holds id, title Acceptance, content First, createdAt, updatedAt and version 0.",
            ],
        },
        {
            "id": "B-05",
            "title": "Read and list the note",
            "pre": "B-04 done.",
            "steps": [
                "$ curl -si $B/notes/$ID -H \"Authorization: Bearer $ALICE\"",
                "$ curl -s \"$B/notes?page=0&size=20\" -H \"Authorization: Bearer $ALICE\"",
            ],
            "expect": [
                "The read answers 200 with ETag: \"0\" and the same note.",
                "The list answers 200 with items, page 0, size 20, totalItems and hasNext; the note is first: most recently updated first.",
            ],
        },
        {
            "id": "B-06",
            "title": "A change needs If-Match (428)",
            "pre": "B-04 done.",
            "steps": [
                "$ curl -s -X PUT $B/notes/$ID -H \"Authorization: Bearer $ALICE\" -H 'Content-Type: application/json' -d '{\"title\":\"Changed\",\"content\":\"Second\"}' -w ' %{http_code}\\n'",
            ],
            "expect": [
                "HTTP 428, a problem+json body with errorCode VERSION_REQUIRED, detail This change needs an If-Match header. and a hint to send the ETag last read.",
                "The note is unchanged.",
            ],
        },
        {
            "id": "B-07",
            "title": "A change on the current version succeeds (200, new ETag)",
            "pre": "B-06 done.",
            "steps": [
                "$ curl -si -X PUT $B/notes/$ID -H \"Authorization: Bearer $ALICE\" -H 'If-Match: \"0\"' -H 'Content-Type: application/json' -d '{\"title\":\"Changed\",\"content\":\"Second\"}'",
            ],
            "expect": [
                "HTTP 200 with ETag: \"1\"; the body shows title Changed and version 1.",
            ],
        },
        {
            "id": "B-08",
            "title": "A change on an outdated version is refused (412)",
            "pre": "B-07 done.",
            "steps": [
                "Send a change still based on version 0:",
                "$ curl -s -X PUT $B/notes/$ID -H \"Authorization: Bearer $ALICE\" -H 'If-Match: \"0\"' -H 'Content-Type: application/json' -d '{\"title\":\"Stale\",\"content\":\"x\"}' -w ' %{http_code}\\n'",
                "$ curl -s $B/notes/$ID -H \"Authorization: Bearer $ALICE\"",
            ],
            "expect": [
                "HTTP 412, errorCode VERSION_MISMATCH, detail Note <id> is at version 1, not 0.",
                "Reading the note again still shows title Changed, version 1: nothing was overwritten.",
            ],
        },
        {
            "id": "B-09",
            "title": "Invalid input is refused (400)",
            "pre": "B-03 done.",
            "steps": [
                "$ curl -s -X POST $B/notes -H \"Authorization: Bearer $ALICE\" -H 'Content-Type: application/json' -d '{\"title\":\"\",\"content\":\"x\"}' -w ' %{http_code}\\n'",
                "$ curl -s -X POST $B/notes -H \"Authorization: Bearer $ALICE\" -H 'Content-Type: application/json' -d '{\"title\":\"   \",\"content\":\"x\"}' -w ' %{http_code}\\n'",
                "$ curl -s \"$B/notes?size=500\" -H \"Authorization: Bearer $ALICE\" -w ' %{http_code}\\n'",
            ],
            "expect": [
                "An empty title answers 400, detail Invalid request content. (refused by the contract).",
                "A blank title answers 400 with errorCode CONSTRAINT_VIOLATION and detail A title must be non-blank and at most 200 characters long (refused by the domain).",
                "A page of 500 answers 400, detail size must be less than or equal to 100.",
            ],
        },
        {
            "id": "B-10",
            "title": "Someone else's note does not exist (404)",
            "pre": "B-04 done, as alice.",
            "steps": [
                "$ curl -s $B/notes/$ID -H \"Authorization: Bearer $BOB\" -w ' %{http_code}\\n'",
                "$ curl -s $B/notes -H \"Authorization: Bearer $BOB\"",
            ],
            "expect": [
                "bob reading alice's note gets 404, errorCode NOTE_NOT_FOUND: exactly as for a missing note.",
                "bob's list does not contain alice's note.",
            ],
        },
        {
            "id": "B-11",
            "title": "A body over 64 KiB is refused (413)",
            "pre": "B-03 done.",
            "steps": [
                "$ python3 -c 'import json;print(json.dumps({\"title\":\"Big\",\"content\":\"x\"*70000}))' > big.json",
                "$ curl -s -X POST $B/notes -H \"Authorization: Bearer $ALICE\" -H 'Content-Type: application/json' --data-binary @big.json -o /dev/null -w '%{http_code}\\n'; rm big.json",
            ],
            "expect": ["HTTP 413, before the request reaches the API."],
        },
        {
            "id": "B-12",
            "title": "Delete needs If-Match, then the note is gone",
            "pre": "B-07 done (the note is at version 1).",
            "steps": [
                "$ curl -s -X DELETE $B/notes/$ID -H \"Authorization: Bearer $ALICE\" -w ' %{http_code}\\n'",
                "$ curl -s -X DELETE $B/notes/$ID -H \"Authorization: Bearer $ALICE\" -H 'If-Match: \"1\"' -w '%{http_code}\\n'",
                "$ curl -s $B/notes/$ID -H \"Authorization: Bearer $ALICE\" -w ' %{http_code}\\n'",
            ],
            "expect": [
                "Without If-Match: 428 VERSION_REQUIRED, and the note stays.",
                "With If-Match \"1\": 204 No Content.",
                "Reading it again: 404 NOTE_NOT_FOUND.",
            ],
        },
    ],
}

# client:web:start
WEB = {
    "id": "W",
    "title": "Web application",
    "intro": (
        "The Angular app is served by nginx on http://localhost:8088. Use a desktop browser "
        "(Chrome, Firefox or Safari), ideally in a private window. The mock provider accepts any "
        "username: the username becomes the owner of the notes, so alice in the browser is the "
        "same owner as alice in the API tests."
    ),
    "tests": [
        {
            "id": "W-01",
            "title": "Opening the app asks to sign in",
            "pre": "The stack is running. No session in the browser.",
            "steps": ["Open http://localhost:8088."],
            "expect": [
                "The browser is sent to the provider's page on localhost:8090 (address starting with http://localhost:8090/default/authorize).",
                "The page shows a username field and a Sign-in button.",
            ],
        },
        {
            "id": "W-02",
            "title": "Sign in and see your notes",
            "pre": "W-01 done.",
            "steps": ["Type alice as username, leave the other field empty, then click Sign-in."],
            "expect": [
                "The browser comes back to http://localhost:8088.",
                "The page shows the New note form (Title, Content, Add note) and Your notes (N), with alice's notes, or No notes yet.",
                "A Sign out button is visible.",
            ],
        },
        {
            "id": "W-03",
            "title": "A note needs a title",
            "pre": "Signed in as alice.",
            "steps": ["Leave Title empty and click Add note."],
            "expect": [
                "The message A title is required, up to 200 characters. appears under Title.",
                "No note is created: the count of Your notes does not change.",
            ],
        },
        {
            "id": "W-04",
            "title": "Write a note",
            "pre": "Signed in as alice.",
            "steps": ["Title: Groceries. Content: Milk, eggs. Click Add note."],
            "expect": [
                "The note appears first under Your notes, with its title and content.",
                "The count goes up by one; the form is empty again.",
            ],
        },
        {
            "id": "W-05",
            "title": "Edit a note, or cancel",
            "pre": "W-04 done.",
            "steps": [
                "On Groceries, click Edit. The heading becomes Edit note and the form holds the note.",
                "Change the title to Weekend groceries, click Save.",
                "Click Edit again, change the title, then click Cancel.",
            ],
            "expect": [
                "After Save, the note shows Weekend groceries and moves first.",
                "After Cancel, the note keeps Weekend groceries and the form is back to New note.",
            ],
        },
        {
            "id": "W-06",
            "title": "A note changed elsewhere is not overwritten",
            "pre": "Signed in as alice, Weekend groceries exists.",
            "steps": [
                "Open http://localhost:8088 in a second tab (same session).",
                "In tab 1, click Edit on Weekend groceries.",
                "In tab 2, edit the same note, set the title to From tab 2, Save.",
                "In tab 1, set the title to From tab 1, Save.",
            ],
            "expect": [
                "Tab 1 shows the alert This note was changed elsewhere. It now shows the latest version.",
                "The form of tab 1 now holds From tab 2: tab 1's change was not saved.",
                "After a reload, both tabs list From tab 2.",
            ],
        },
        {
            "id": "W-07",
            "title": "Delete only once confirmed",
            "pre": "Signed in as alice, at least one note.",
            "steps": [
                "Click Delete on a note: Delete this note? appears with Delete and Keep.",
                "Click Keep. Then Delete again, and confirm with Delete.",
            ],
            "expect": [
                "After Keep, the note stays.",
                "After Delete, the note leaves the list and the count goes down by one.",
            ],
        },
        {
            "id": "W-08",
            "title": "Notes come ten per page",
            "pre": "Signed in as alice. B-03 done in a terminal (ALICE token).",
            "steps": [
                "Create twelve notes for alice from the terminal:",
                "$ for i in $(seq 1 12); do curl -s -o /dev/null -X POST $B/notes -H \"Authorization: Bearer $ALICE\" -H 'Content-Type: application/json' -d \"{\\\"title\\\":\\\"Page note $i\\\",\\\"content\\\":\\\"\\\"}\"; done",
                "Reload the web app, then use Next and Previous.",
            ],
            "expect": [
                "The first page lists ten notes, Page note 12 first; Previous is disabled.",
                "Next shows the remaining notes; on the last page Next is disabled.",
                "The count in Your notes (N) is the total, not the page size.",
            ],
        },
        {
            "id": "W-09",
            "title": "Each user sees only their notes",
            "pre": "Signed in as alice with notes.",
            "steps": ["Click Sign out, open http://localhost:8088 again, sign in as bob."],
            "expect": [
                "bob sees none of alice's notes (No notes yet., unless bob wrote some in B-10).",
                "Signing back in as alice shows her notes again.",
            ],
        },
        {
            "id": "W-10",
            "title": "Sign out ends the session",
            "pre": "Signed in.",
            "steps": ["Click Sign out, then open http://localhost:8088 again."],
            "expect": [
                "Sign out leads back to the provider's sign-in page.",
                "Opening the app again asks to sign in: the session is over.",
            ],
        },
        {
            "id": "W-11",
            "title": "No error in the browser console",
            "pre": "Signed in, the browser's developer tools open on Console.",
            "steps": ["Repeat W-04 to W-07 while watching the console."],
            "expect": ["No error and no Content-Security-Policy violation is logged."],
        },
    ],
}
# client:web:end

# client:native:start
# The helper commands of the native clients' tests: a change made elsewhere, through the API, and
# the notes the API holds. They need the B and ALICE variables of B-03.
ELSEWHERE = [
    "Change the note through the API, as another device would (B-03 done in this terminal):",
    "$ ID=$(curl -s \"$B/notes?size=100\" -H \"Authorization: Bearer $ALICE\" | python3 -c 'import sys,json;print(next(n[\"id\"] for n in json.load(sys.stdin)[\"items\"] if n[\"title\"]==sys.argv[1]))' \"Written offline\")",
    "$ V=$(curl -s $B/notes/$ID -H \"Authorization: Bearer $ALICE\" | python3 -c 'import sys,json;print(json.load(sys.stdin)[\"version\"])')",
    "$ curl -s -X PUT $B/notes/$ID -H \"Authorization: Bearer $ALICE\" -H \"If-Match: \\\"$V\\\"\" -H 'Content-Type: application/json' -d '{\"title\":\"Theirs (API)\",\"content\":\"\"}' -o /dev/null -w '%{http_code}\\n'",
]
API_TITLES = "$ curl -s \"$B/notes?size=100\" -H \"Authorization: Bearer $ALICE\" | python3 -c 'import sys,json;[print(n[\"title\"]) for n in json.load(sys.stdin)[\"items\"]]'"
# client:native:end

# client:mobile:start
IOS = {
    "id": "I",
    "title": "iOS app on the simulator",
    "intro": (
        "The SwiftUI app runs on the iPhone 17 simulator and reaches the stack on localhost "
        "directly: the simulator shares the Mac's network. Gradle generates the Xcode project, "
        "builds the shared Kotlin module and the app, and launches it. " + OUTAGE + " Keep a "
        "terminal at the root of the repository with B-03's variables, for the API checks."
    ),
    "tests": [
        {
            "id": "I-01",
            "title": "The app builds and launches",
            "pre": "The stack is running. Xcode, XcodeGen and the iPhone 17 simulator are installed.",
            "steps": [
                "$ ./gradlew iosRun",
                "If several runtimes have an iPhone 17, pass its identifier:",
                "$ xcrun simctl list devices available | grep 'iPhone 17'",
                "$ ./gradlew iosRun -PiosSimulator=<device identifier>",
            ],
            "expect": [
                "The build ends with BUILD SUCCESSFUL; the simulator boots.",
                "The Notes app opens on its sign-in screen with a Sign in button.",
            ],
        },
        {
            "id": "I-02",
            "title": "Cancelling the sign-in is handled",
            "pre": "I-01 done.",
            "steps": ["Tap Sign in, then Cancel in the system dialog (or close the sign-in sheet)."],
            "expect": ["The app stays on the sign-in screen and shows Sign-in cancelled."],
        },
        {
            "id": "I-03",
            "title": "Sign in and see your notes",
            "pre": "I-01 done.",
            "steps": [
                "Tap Sign in. iOS asks whether Notes may use localhost to sign in: tap Continue.",
                "On the provider's page, type alice and tap Sign-in.",
            ],
            "expect": [
                "Back in the app, the list is titled Your notes (N) and shows alice's notes: the same titles as the API lists for alice.",
                "The toolbar shows Sign out and New note.",
            ],
        },
        {
            "id": "I-04",
            "title": "Write, edit and delete a note",
            "pre": "Signed in as alice.",
            "steps": [
                "Tap New note. Title: From iPhone. Content: Simulator. Tap Add note.",
                "Tap the note, change the title to From iPhone, edited, tap Save.",
                "Check what the API holds:",
                API_TITLES,
                "Open the note again, tap Delete; Delete this note? appears: tap Keep, then Delete again and confirm.",
            ],
            "expect": [
                "Add note returns to the list, with the note first; the count goes up.",
                "Save shows the new title in the list, and the API lists From iPhone, edited.",
                "Keep leaves the note; the confirmed Delete removes it from the list.",
            ],
        },
        {
            "id": "I-05",
            "title": "An empty title is refused without calling the API",
            "pre": "Signed in.",
            "steps": ["Tap New note, leave Title empty, tap Add note."],
            "expect": ["The editor shows title: must not be empty and stays open; no note is created."],
        },
        {
            "id": "I-06",
            "title": "Offline: notes kept on the device, changes queued",
            "pre": "Signed in, the list shows notes.",
            "steps": [
                OUTAGE_STOP,
                "In the app, tap New note, Title: Written offline, tap Add note.",
            ],
            "expect": [
                "The list shows Offline: these are the notes kept on this device. with a Retry button.",
                "The notes read before are still listed; Written offline appears labelled Not synced.",
            ],
        },
        {
            "id": "I-07",
            "title": "Back online: the queued change reaches the API",
            "pre": "I-06 done.",
            "steps": [
                OUTAGE_START,
                OUTAGE_WAIT + ", then tap Retry in the app.",
                API_TITLES,
            ],
            "expect": [
                "The Offline line and the Not synced label disappear.",
                "The API lists Written offline.",
            ],
        },
        {
            "id": "I-08",
            "title": "A conflict is shown and resolved",
            "pre": "Signed in as alice; the note Written offline exists and is synced (I-07).",
            "steps": [
                OUTAGE_STOP,
                "On the iPhone, change Written offline to Mine (iPhone), Save: it is labelled Not synced.",
                OUTAGE_START,
                OUTAGE_WAIT + ".",
            ] + ELSEWHERE + [
                "On the iPhone, tap Retry, then open the note.",
                "Tap Keep both.",
                API_TITLES,
            ],
            "expect": [
                "The change through the API answers 200.",
                "After Retry, the note is labelled Conflict: open to resolve.",
                "The note shows This note was changed elsewhere., then Yours (Mine (iPhone)) and Theirs (Theirs (API)), with Keep mine, Keep theirs and Keep both.",
                "Keep both leaves two notes, Mine (iPhone) and Theirs (API), on the iPhone and in the API's list.",
                "Optional: repeat with Keep mine (the API then holds Mine) and with Keep theirs (the iPhone then shows Theirs).",
            ],
        },
        {
            "id": "I-09",
            "title": "The session survives a relaunch",
            "pre": "Signed in.",
            "steps": ["Quit the app from the app switcher, then open Notes again."],
            "expect": ["The app opens on Your notes without asking to sign in."],
        },
        {
            "id": "I-10",
            "title": "Signing out with changes not synced asks first",
            "pre": "Signed in.",
            "steps": [
                OUTAGE_STOP,
                "Add a note, then tap Sign out.",
                "Tap Stay. Tap Sign out again, then Sign out anyway.",
                OUTAGE_START,
            ],
            "expect": [
                "An alert reads Changes not synced yet: 1. Signing out loses them., with Sign out anyway and Stay.",
                "Stay keeps the session; Sign out anyway leads to the sign-in screen.",
            ],
        },
    ],
}

ANDROID = {
    "id": "A",
    "title": "Android app on the emulator",
    "intro": (
        "The Compose app runs on an Android emulator. adb reverse makes the emulator's localhost "
        "the Mac's, so the tokens carry the issuer the API checks; 10.0.2.2 would not work. "
        + OUTAGE + " Keep a terminal at the root of the repository with B-03's variables."
    ),
    "tests": [
        {
            "id": "A-01",
            "title": "An emulator is available",
            "pre": "ANDROID_HOME is set (S-01).",
            "steps": [
                "Once, install a system image and create a device:",
                "$ sdkmanager --sdk_root=\"$ANDROID_HOME\" \"emulator\" \"system-images;android-36;google_apis_playstore;arm64-v8a\"",
                "$ avdmanager create avd -n notes -k \"system-images;android-36;google_apis_playstore;arm64-v8a\" -d pixel_7",
                "Every time, start it:",
                "$ emulator -avd notes &",
                "$ adb devices",
            ],
            "expect": [
                "The emulator window opens on the Android home screen.",
                "adb devices lists one device, emulator-5554, as device.",
            ],
        },
        {
            "id": "A-02",
            "title": "The app installs",
            "pre": "A-01 done, the stack is running.",
            "steps": [
                "$ adb reverse tcp:8088 tcp:8088",
                "$ adb reverse tcp:8090 tcp:8090",
                "$ ./gradlew :apps:clients:android:installDebug",
            ],
            "expect": [
                "Each adb reverse prints the port (8088, then 8090).",
                "The build ends with BUILD SUCCESSFUL and Installed on 1 device.",
                "Notes appears in the emulator's app list.",
            ],
        },
        {
            "id": "A-03",
            "title": "Cancelling the sign-in is handled",
            "pre": "A-02 done.",
            "steps": ["Open Notes, tap Sign in, then close the browser tab with its X (or the back button)."],
            "expect": ["The app shows Sign-in cancelled. and stays on the sign-in screen."],
        },
        {
            "id": "A-04",
            "title": "Sign in and see your notes",
            "pre": "A-02 done.",
            "steps": [
                "Tap Sign in. The first time, Chrome may show its welcome screen: choose to use it without an account.",
                "On the provider's page, type alice and tap Sign-in.",
            ],
            "expect": [
                "Back in the app, the top bar reads Your notes (N), with alice's notes, and a Sign out action.",
                "A New note button is shown at the bottom right.",
            ],
        },
        {
            "id": "A-05",
            "title": "Write, edit and delete a note",
            "pre": "Signed in as alice.",
            "steps": [
                "Tap New note. Title: From Android. Content: Emulator. Tap Add note.",
                "Tap the note, change the title to From Android, edited, tap Save.",
                "Check what the API holds:",
                API_TITLES,
                "Open the note again, tap Delete; Delete this note? appears: tap Keep, then Delete and confirm with Delete.",
            ],
            "expect": [
                "Add note returns to the list with the note first; the count goes up.",
                "Save shows the new title in the list, and the API lists From Android, edited.",
                "Keep leaves the note; the confirmed Delete removes it.",
            ],
        },
        {
            "id": "A-06",
            "title": "An empty title is refused without calling the API",
            "pre": "Signed in.",
            "steps": ["Tap New note, leave Title empty, tap Add note."],
            "expect": ["The editor shows title: must not be empty and stays open; no note is created."],
        },
        {
            "id": "A-07",
            "title": "Offline, then back online",
            "pre": "Signed in, the list shows notes. If I-06 left a note titled Written offline, delete it first.",
            "steps": [
                OUTAGE_STOP,
                "Tap New note, Title: Written offline, tap Add note.",
                OUTAGE_START,
                OUTAGE_WAIT + ", then tap Retry.",
                API_TITLES,
            ],
            "expect": [
                "While offline: Offline: these are the notes kept on this device. with Retry; the new note is labelled Not synced.",
                "After Retry: the offline line and the label disappear; the API lists Written offline.",
            ],
        },
        {
            "id": "A-08",
            "title": "A conflict is shown and resolved",
            "pre": "Signed in as alice; Written offline exists and is synced (A-07).",
            "steps": [
                OUTAGE_STOP,
                "On Android, change the note to Mine (Android), Save: labelled Not synced.",
                OUTAGE_START,
                OUTAGE_WAIT + ".",
            ] + ELSEWHERE + [
                "On Android, tap Retry, open the note, tap Keep mine.",
                API_TITLES,
            ],
            "expect": [
                "The change through the API answers 200.",
                "After Retry, the note is labelled Conflict: open to resolve.",
                "The note shows This note was changed elsewhere., Yours and Theirs, and Keep mine, Keep theirs, Keep both.",
                "Keep mine: Android and the API both hold Mine (Android).",
            ],
        },
        {
            "id": "A-09",
            "title": "Signing out with changes not synced asks first",
            "pre": "Signed in.",
            "steps": [
                OUTAGE_STOP,
                "Add a note, then tap Sign out.",
                "Tap Stay, then Sign out again and Sign out anyway.",
                OUTAGE_START,
            ],
            "expect": [
                "A dialog reads Changes not synced yet: 1. Signing out loses them., with Sign out anyway and Stay.",
                "Stay keeps the session; Sign out anyway leads back to Sign in.",
            ],
        },
    ],
}
# client:mobile:end

# client:cli:start
CLI = {
    "id": "C",
    "title": "Command line on macOS",
    "intro": (
        "notes is a Kotlin/Native binary. Against the local stack, the provider has no device "
        "flow, so notes login opens the sign-in page in the browser and waits for its redirect "
        "on 127.0.0.1:8770. Tokens go to the login Keychain (item dev.ylp.notes), the notes and "
        "pending changes to ~/.local/share/notes. In the commands, notes is the alias of C-01. "
        + OUTAGE
    ),
    "tests": [
        {
            "id": "C-01",
            "title": "The binary builds",
            "pre": "At the root of the repository; the stack may be stopped.",
            "steps": [
                "$ ./gradlew :apps:clients:cli:linkReleaseExecutableMacosArm64",
                "$ alias notes=\"$PWD/apps/clients/cli/build/bin/macosArm64/releaseExecutable/notes.kexe\"",
                "$ notes --help",
            ],
            "expect": [
                "BUILD SUCCESSFUL, and the file notes.kexe exists.",
                "The help lists login, logout, list, show, add, edit, delete, sync and resolve.",
            ],
        },
        {
            "id": "C-02",
            "title": "Without a session, a command asks to sign in",
            "pre": "The stack is running. Start clean: notes logout --force.",
            "steps": ["$ notes list; echo \"exit $?\""],
            "expect": ["Not signed in: run notes login. and exit 1."],
        },
        {
            "id": "C-03",
            "title": "Sign in in the browser",
            "pre": "C-02 done.",
            "steps": [
                "$ notes login",
                "The browser opens the provider's page (the terminal also prints its address). Type alice, click Sign-in.",
                "$ security find-generic-password -s dev.ylp.notes -a tokens > /dev/null && echo stored",
            ],
            "expect": [
                "The browser shows Signed in. You can close this tab and go back to the terminal.",
                "The terminal prints Signed in.; nothing else is logged.",
                "The Keychain check prints stored. The tokens never appear in a command line.",
            ],
        },
        {
            "id": "C-04",
            "title": "List, add, show, edit",
            "pre": "Signed in as alice.",
            "steps": [
                "$ notes list",
                "$ notes add --title \"From the CLI\" --content \"Terminal\"",
                "$ notes show <id printed by add>",
                "$ notes edit <id> --title \"From the CLI, edited\"",
                API_TITLES,
            ],
            "expect": [
                "list prints Your notes (N), then one line per note: id and title, alice's notes from every client.",
                "add prints Added <id>.; show prints the title, the content and id <id>, version 0.",
                "edit prints Saved.; the API lists From the CLI, edited.",
            ],
        },
        {
            "id": "C-05",
            "title": "Delete asks first",
            "pre": "C-04 done.",
            "steps": [
                "$ notes delete <id>   (answer n)",
                "$ notes delete <id>   (answer y)",
            ],
            "expect": [
                "The CLI asks Delete \"From the CLI, edited\"? and waits; answering n prints Kept. and the note stays.",
                "y prints Deleted. and the note leaves notes list. --yes skips the question.",
            ],
        },
        {
            "id": "C-06",
            "title": "Offline, then sync",
            "pre": "Signed in. If an earlier section left a note titled Written offline, delete it first.",
            "steps": [
                OUTAGE_STOP,
                "$ notes add --title \"Written offline\"",
                "$ notes list",
                "$ notes sync; echo \"exit $?\"",
                OUTAGE_START,
                OUTAGE_WAIT + ".",
                "$ notes sync",
            ],
            "expect": [
                "add prints Added <id>. Not synced yet: it leaves once the service can be reached.",
                "list starts with Offline: these are the notes kept on this computer. and marks the note [not synced].",
                "The first sync fails: The service cannot be reached: changes not synced yet: 1. and exit 1.",
                "Once the service is back, sync prints Synced.",
            ],
        },
        {
            "id": "C-07",
            "title": "A conflict is shown and resolved",
            "pre": "Signed in; Written offline exists and is synced (C-06).",
            "steps": [
                OUTAGE_STOP,
                "$ notes edit <id> --title \"Mine (CLI)\"",
                OUTAGE_START,
                OUTAGE_WAIT + ".",
            ] + ELSEWHERE + [
                "$ notes sync",
                "$ notes list",
                "$ notes show <id>",
                "$ notes resolve <id> both",
                API_TITLES,
            ],
            "expect": [
                "The change through the API answers 200.",
                "sync prints Synced. Changes in conflict: 1, see notes list.",
                "list marks the note [conflict].",
                "show prints This note was changed elsewhere., Yours: Mine (CLI), Theirs: Theirs (API), and Resolve with: notes resolve <id> <mine, theirs, both>.",
                "resolve prints Resolved.; the API lists both Mine (CLI) and Theirs (API).",
            ],
        },
        {
            "id": "C-08",
            "title": "Sign out keeps changes not synced unless forced",
            "pre": "Signed in.",
            "steps": [
                OUTAGE_STOP,
                "$ notes add --title \"Pending\"",
                "$ notes logout; echo \"exit $?\"",
                "$ notes logout --force",
                "$ security find-generic-password -s dev.ylp.notes -a tokens > /dev/null || echo gone",
                OUTAGE_START,
            ],
            "expect": [
                "logout refuses: Changes not synced yet: 1. Signing out loses them: run notes logout --force to do it anyway. and exit 1.",
                "logout --force prints Signed out.",
                "The Keychain check prints gone: the tokens are removed.",
            ],
        },
    ],
}
# client:cli:end

TEARDOWN = {
    "id": "T",
    "title": "Teardown",
    "intro": "Stop what the tests started and leave the machine clean.",
    "tests": [
        {
            "id": "T-01",
            "title": "Everything stops cleanly",
            "pre": "All other tests done.",
            "steps": [
                "Stop the emulator and the simulator:",  # client:mobile
                "$ adb emu kill",  # client:mobile
                "$ xcrun simctl shutdown all",  # client:mobile
                "Stop the stack:",
                "$ ./gradlew stopLocalStack",
                "To also delete the test data: ./gradlew resetDatabase",
            ],
            "expect": [
                "stopLocalStack ends with BUILD SUCCESSFUL; docker compose --profile app ps lists nothing.",
                "The database volume is kept unless resetDatabase was run.",
            ],
        },
    ],
}

SECTIONS = [
    STACK,
    API,
    WEB,  # client:web
    IOS,  # client:mobile
    ANDROID,  # client:mobile
    CLI,  # client:cli
    TEARDOWN,
]
