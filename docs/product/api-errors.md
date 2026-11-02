# API errors

Every error is an RFC 9457 problem (`application/problem+json`), declared once in `apps/contracts/api/common.yaml` as `Problem`.

| Field | Content |
| --- | --- |
| `type` | `urn:problem:<error code in lower case>` for business and technical errors |
| `title`, `status` | The HTTP status |
| `detail` | A message for humans; never internal details |
| `instance` | The request path |
| `errorCode` | Stable machine-readable code, e.g. `NOTE_NOT_FOUND`, `CONSTRAINT_VIOLATION` |
| `hint` | What the caller can do about it |

## Statuses

| Status | Meaning | `errorCode` |
| --- | --- | --- |
| 400 | The request is malformed, or a value breaks a constraint of the contract (size, length, page bounds) or of the domain | `CONSTRAINT_VIOLATION` when the domain refused the value |
| 401 | The token is missing or invalid; `WWW-Authenticate` says why, there is no body | |
| 404 | The resource does not exist, or belongs to someone else: the two cases are indistinguishable on purpose | e.g. `NOTE_NOT_FOUND` |
| 409 | The request conflicts with the current state of the resource | the module's reason |
| 412 | The resource changed since the version named in `If-Match`: nothing was changed. Read it again, then apply the change to its current version | `VERSION_MISMATCH` |
| 413 | The request body is over 64 KiB | |
| 422 | A business rule refused the request | the module's reason |
| 428 | The request changes a versioned resource without `If-Match`: send the `ETag` last read | `VERSION_REQUIRED` |
| 500 | Anything unexpected; `detail` is "An unexpected error occurred." | |
| 503 | A dependency failed; retrying later may succeed | e.g. `INFRASTRUCTURE_FAILURE` |

How the API turns a business error into one of these statuses: [Architecture overview](../architecture/overview.md#domain-modelling).

## Versions

A note carries a `version`, and a response about one note carries it as its `ETag` (`"3"`). Changing or deleting a note requires `If-Match` with the ETag last read; `If-Match: *` changes it whatever its version. The version grows by one at each change, so a client that sends an older one gets a 412 rather than overwriting a change it never saw.
