# Phase 32 — Diagnostic Pipeline Reliability

## Objective
Improve the reliability and diagnosability of the existing Android diagnostic-event upload path. Preserve the current Supabase project, database schema, single Upstash Redis database, Auth behavior, and privacy boundaries.

## Initial production evidence
- Supabase project: `FORGE-TS-LAB` (`hzwyxvdeeetkxhijgxwy`), region `ap-southeast-1`, project status `ACTIVE_HEALTHY`.
- `forge-diagnostic-ingest` was ACTIVE at version 2.
- The inspected 24-hour log window contained 25 requests to `/functions/v1/forge-diagnostic-ingest`; all returned HTTP 503.
- The requests had both `execution_id` and `function_id`, which Supabase's 503 troubleshooting guide classifies as an application-level error rather than a boot failure or a platform-only failure.
- The function emitted successful boot events. The request payload size was 1,294 bytes, well below the 64 KiB function limit.
- The existing function had explicit 503 responses for missing server configuration and Redis rate-limiter failures. Its PostgREST insert fetch was not wrapped in an exception handler, so a network exception there could escape the handler and surface as an Edge Function error.
- The exact historical branch responsible for the 503s was not conclusively identified from the available historical logs. No secret values were inspected or exposed.

## Implementation
- Added safe stage markers for configuration, API-key validation, request parsing, event validation, Redis configuration/rate limiting and database insertion.
- Missing configuration logs only the names of absent environment variables, never their values.
- Redis endpoint validation, HTTP rejection, malformed JSON, command error and invalid result paths now emit distinct safe log events.
- PostgREST URL construction and network insertion are guarded. Network exceptions and non-2xx storage responses return the existing storage-unavailable 502 response instead of escaping as an unhandled function exception.
- A final handler boundary catches unexpected errors, logs the stage and exception class only, and returns a generic 500 response.
- No request body, installation identifier, stack trace, API key, bearer token, or service-role credential is written to diagnostic function logs.

## Preserved behavior and boundaries
- `verify_jwt=false` remains unchanged for this public ingest endpoint; the function continues to validate the supplied Supabase API key and apply the existing per-installation Redis limit.
- The daily installation limit remains 300 events.
- Redis remains mandatory for this endpoint's rate limiter. No fail-open bypass was introduced.
- Existing event validation, UUID/event deduplication, database table and service-role-only insert model remain unchanged.
- Android's local diagnostic queue, four-event batches and WorkManager exponential retry behavior remain unchanged.
- No schema migration, table/policy change, new Supabase project, new Redis database, credential rotation, or Auth change was made.

## Deployment
- GitHub source commit: `ff950a0448a4c7b972c1ca9032ddfec7bf737ef9`.
- README documentation commit: `dfdb5acd11d1ed9334daee19898e76168401aec6`.
- Supabase Edge Function `forge-diagnostic-ingest` deployed as version 3; `verify_jwt=false` preserved.
- No synthetic diagnostic event, manual endpoint test, or repeated build/test loop was run.

## Verification boundary
- Deployment API confirmed the function is ACTIVE at version 3.
- Runtime recovery is not yet confirmed. The next normal app diagnostic upload should produce stage-specific logs and either a successful 2xx response or a precise failure category.
- If the next normal upload reports missing configuration, add only the named missing secrets through the Supabase project secrets interface; do not paste secret values into GitHub or chat.
- If Redis reports an upstream rejection, inspect the Upstash REST response status and secret configuration without logging credentials.
- If PostgREST reports a storage rejection, inspect the status and table/schema contract. Do not weaken RLS or grant client access to the diagnostic table.
