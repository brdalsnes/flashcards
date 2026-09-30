# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Onboarding

This is a tutorial project. Keep things simple.

This is a flashcard application. It should serve flashcards used for repetition.

## Technologies
- Ktor (HTTP server)
- H2 (embedded database, file-backed at `./data/flashcards`, in-memory in tests), accessed via Exposed (DSL)
- kotlinx.serialization for JSON

## Domain model (example, edit as needed)

- **User**: a person studying cards (`id`, `name`). Keep auth out of scope for now; identify the user with a simple `userId` (e.g. a path segment or header).
- **Deck**: a named collection of cards (`id`, `name`). Decks and cards are shared content, not owned by a user.
- **Card**: a single flashcard belonging to one deck (`id`, `deckId`, `front`, `back`).
- **ReviewState**: spaced-repetition state for one user on one card (`userId`, `cardId`, `dueDate`, `intervalDays`). Unique on (`userId`, `cardId`). Answers are graded (`again`, `hard`, `good`, `easy`), and the grade sets the new interval from the previous one (a card with no state counts as 1 day): `again` = 1 day, `hard` = unchanged (minimum 1), `good` = doubled, `easy` = times 4. `dueDate` is today plus the new interval.
- **Review**: append-only log, one row per submitted review (`id`, `userId`, `cardId`, `grade`, `reviewedAt`, `resultingIntervalDays`). Written in the same transaction as the ReviewState update.

Review state is per user: the same card has independent schedules for different users. A card with no ReviewState row for a user is new for that user and counts as due immediately; the row is created on their first review.

A card is "due" for a user when it has no ReviewState or `dueDate <= today`. The study flow is: fetch the next due card, show the front, reveal the back, submit a grade. `next` returns due cards the user has reviewed first (earliest `dueDate`), then new cards in the order they were added.

Endpoints:

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/users` | Create a user |
| `POST` | `/decks` | Create a deck |
| `GET` | `/decks/{id}/cards` | List cards in a deck |
| `POST` | `/decks/{id}/cards` | Add a card |
| `GET` | `/users/{userId}/decks/{id}/next` | Get the next due card for a user: `200` with an array of zero or one cards |
| `POST` | `/users/{userId}/cards/{cardId}/review` | Submit a review (`{"grade": "again\|hard\|good\|easy"}`); returns the new state |

Unknown users, decks and cards return `404`; blank names, blank card sides and invalid grades return `400`.

## Commands

```bash
./gradlew run        # Start server on port 8080
./gradlew run --args="-port=8081"  # Start on another port if 8080 is taken
./gradlew build      # Build the project
./gradlew test       # Run all tests
./gradlew test --tests "com.example.ServerTest.test root endpoint"  # Run a single test
```

## Architecture

This is a **Kotlin/Ktor** web server using the Netty engine, targeting JVM 21.

- `src/main/kotlin/main.kt` — Entry point; delegates to Ktor's `EngineMain`
- `src/main/kotlin/Routing.kt` — The root `GET /` route
- `src/main/kotlin/UserRoutes.kt`, `DeckRoutes.kt`, `StudyRoutes.kt` — Routes per area, each an `Application` extension function
- `src/main/kotlin/Scheduler.kt` — `Grade` and the pure `schedule(previousIntervalDays, grade, today)` function
- `src/main/kotlin/Database.kt`, `Tables.kt` — H2 connection and Exposed tables
- `src/main/kotlin/Models.kt` — JSON request/response classes
- `src/main/kotlin/Serialization.kt` — Content negotiation setup
- `src/main/resources/application.yaml` — Server config (port, database url, which module functions to load)
- `src/test/kotlin/` — Tests use Ktor's `testApplication` harness (no real server started); `TestSupport.kt` has `testConfig()` (in-memory database, optional fixed clock) and request helpers

"Today" comes from a `java.time.Clock`: the system clock by default, or whatever a test stores under `ClockKey`.

**Adding routes**: Define extension functions on `Application` in a routes file (or new files), then register them in `application.yaml` under `ktor.application.modules`.
