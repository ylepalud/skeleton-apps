# API errors

Every error is an RFC 9457 problem (`application/problem+json`), declared once in `contracts/api/common.yaml` as `Problem`.

| Field | Content |
| --- | --- |
| `type` | `urn:problem:<error code in lower case>` for business and technical errors |
| `title`, `status` | The HTTP status |
| `detail` | A message for humans; never internal details |
| `instance` | The request path |
| `errorCode` | Stable machine-readable code, e.g. `NOTE_NOT_FOUND`, `CONSTRAINT_VIOLATION` |
| `hint` | What the caller can do about it |

## From a business error to a status

A business error is a `BusinessException` carrying a reason (an enum implementing `BusinessErrorReason`). The reason states its category, which knows nothing about HTTP; `ProblemDetailsExceptionHandler` maps the category to the status.

| Category | Status | Example |
| --- | --- | --- |
| `NOT_FOUND` | 404 | `NOTE_NOT_FOUND`: the note does not exist or belongs to someone else |
| `INVALID` | 400 | `CONSTRAINT_VIOLATION`: a value object refused its value |
| `CONFLICT` | 409 | |
| `RULE_VIOLATION` (default) | 422 | |

## Other errors

| Cause | Status | `errorCode` |
| --- | --- | --- |
| Missing or invalid token | 401, with `WWW-Authenticate`, no body | |
| Request body over 64 KiB | 413 | |
| Malformed request, contract constraint (size, length, page bounds) | 400 | |
| `TechnicalException` (a dependency failed) | 503 | its reason, e.g. `INFRASTRUCTURE_FAILURE` |
| Anything unexpected | 500, `detail` "An unexpected error occurred." | |
