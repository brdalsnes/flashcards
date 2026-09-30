# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Onboarding

This is a tutorial project. Keep things simple.

This is a flashcard application. It should serve flashcards used for repetition.

## Technologies
- Ktor (HTTP server)
- H2 (embedded database, planned), accessed via Exposed

## Domain model (example, edit as needed)

- **User**: a person studying cards (`id`, `name`). Keep auth out of scope for now; identify the user with a simple `userId` (e.g. a path segment or header).
- **Deck**: a named collection of cards (`id`, `name`). Decks and cards are shared content, not owned by a user.
- **Card**: a single flashcard belonging to one deck (`id`, `deckId`, `front`, `back`).
- **ReviewState**: spaced-repetition state for one user on one card (`userId`, `cardId`, `dueDate`, `intervalDays`). Unique on (`userId`, `cardId`). A correct answer increases the interval (e.g. doubles it); a wrong answer resets it to 1 day.

Review state is per user: the same card has independent schedules for different users. A card with no ReviewState row for a user is new for that user and counts as due immediately; the row is created on their first review.

A card is "due" for a user when it has no ReviewState or `dueDate <= today`. The study flow is: fetch the next due card, show the front, reveal the back, submit whether the answer was correct.

Planned endpoints (sketch):

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/users` | Create a user |
| `POST` | `/decks` | Create a deck |
| `GET` | `/decks/{id}/cards` | List cards in a deck |
| `POST` | `/decks/{id}/cards` | Add a card |
| `GET` | `/users/{userId}/decks/{id}/next` | Get the next due card for a user |
| `POST` | `/users/{userId}/cards/{cardId}/review` | Submit a review (`{"correct": true}`) |

## Commands

```bash
./gradlew run        # Start server on port 8080
./gradlew build      # Build the project
./gradlew test       # Run all tests
./gradlew test --tests "com.example.ServerTest.test root endpoint"  # Run a single test
```

## Architecture

This is a **Kotlin/Ktor** web server using the Netty engine, targeting JVM 21.

- `src/main/kotlin/main.kt` — Entry point; delegates to Ktor's `EngineMain`
- `src/main/kotlin/Routing.kt` — All HTTP routes defined as `Application` extension functions
- `src/main/resources/application.yaml` — Server config (port, which module functions to load)
- `src/test/kotlin/ServerTest.kt` — Tests use Ktor's `testApplication` harness (no real server started)

**Adding routes**: Define extension functions on `Application` in `Routing.kt` (or new files), then register them in `application.yaml` under `ktor.application.modules`.
