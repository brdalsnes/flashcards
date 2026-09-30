# Design

## Context

The project is a bare Ktor/Netty skeleton (JVM 21, Kotlin) with one route and a module list in `application.yaml`. There is no JSON, persistence or domain code. See proposal.md for motivation and the specs for required behavior. This is a tutorial project, so each decision below favors the simplest option that still works.

## Goals / Non-Goals

**Goals:**
- Keep scheduling logic free of HTTP and database concerns so it can be unit-tested.
- Make the review write (state update plus log entry) atomic.
- Make date-dependent behavior testable without waiting for real days to pass.

**Non-Goals:**
- Authentication, ownership, migrations tooling, or a production-grade database.
- A pluggable scheduler. The grade-to-interval rule is one function that can be replaced later.

## Decisions

**Persistence: H2 with Exposed DSL.** One Exposed `Table` object per entity and plain `@Serializable` data classes for API payloads. Alternatives: the Exposed DAO (more magic, less visible SQL), SQLite (needs a native driver), in-memory maps (no real persistence to learn from). Tests use an in-memory H2 instance, and the dev server uses a file-backed one.

**Schema.** `users(id, name)`, `decks(id, name)`, `cards(id, deck_id -> decks, front, back)`, `review_states(user_id, card_id, due_date, interval_days)` with a unique index on `(user_id, card_id)`, and `reviews(id, user_id, card_id, grade, reviewed_at, resulting_interval_days)`. Ids are auto-incrementing integers. Cards are listed and served in id order, which matches the order they were added.

**Scheduler as a pure function.** `schedule(previousIntervalDays: Int?, grade: Grade, today: LocalDate) -> (intervalDays, dueDate)`. A null previous interval means a new card and is treated as 1. The mapping is again=1, hard=max(1, previous), good=previous*2, easy=previous*4. Alternatives: SM-2 or FSRS, which need more state (ease, stability) and are deferred. The review log keeps enough history to move to either later.

**Clock injection.** Routes take a `java.time.Clock` (system default in production, fixed in tests). "Today" is `LocalDate.now(clock)`, and `reviewed_at` is `Instant.now(clock)`. Tests can then check due dates exactly.

**Review write in one transaction.** The handler loads the current state, computes the new one, upserts `review_states` and inserts into `reviews` inside a single Exposed `transaction`. Validation (grade, user, card) happens before any write, and an exception rolls back both.

**Next-due query.** For a user and deck: first, cards that have a state with `due_date <= today`, ordered by `due_date` then card id; if none, the lowest-id card with no state for that user (a left join with null check). The response is always a JSON array of zero or one cards, so the client handles one shape and "nothing due" is an empty array rather than a special status. Two simple queries are clearer than one clever ordering expression, and a deck's card count is small.

**HTTP conventions.** JSON via Ktor content negotiation with kotlinx.serialization. Creates return `201` with the created object. Unknown user, deck or card returns `404`, and bad input returns `400`. "Nothing due" is `200` with an empty array. Because the user id is a path segment, every `/users/{userId}/...` route checks the user exists first.

**Code layout.** Follow the existing flat style in `src/main/kotlin`: `Database.kt` (connection and table creation), `Tables.kt`, `Models.kt`, `Scheduler.kt`, and one routes file per capability (`UserRoutes.kt`, `DeckRoutes.kt`, `StudyRoutes.kt`). Each is registered as a module in `application.yaml`, replacing the current `Routing.kt` hello route only if nothing else depends on it (the existing test does, so the root route stays).

## Risks / Trade-offs

- [Kotlin serialization needs a Gradle plugin plus versions in the catalog; Ktor, Exposed and H2 versions must be compatible with Kotlin 2.4.0] -> Add them to `gradle/libs.versions.toml` and verify with `./gradlew build` as the first task before writing domain code.
- [Doubling and quadrupling intervals grow without bound] -> Accepted for the tutorial. A cap can be added inside the scheduler function without touching the API.
- [`userId` in the path lets anyone act as anyone] -> Accepted and explicit; auth is out of scope. Moving the id to a header later would change paths.
- [Server-local "today" means the day boundary depends on the server's time zone] -> Accepted; the injected `Clock` fixes the zone in one place if that changes.
- [Stored `interval_days` could overflow after very many easy reviews] -> Extremely unlikely in a tutorial. Noted, not handled.
