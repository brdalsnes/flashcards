# Proposal

## Why

The project is a Ktor skeleton with a single `GET /` route and no persistence, JSON handling or domain model. A flashcard backend needs to serve decks and cards and schedule repetition per user. We want that built as a small, realistic study backend rather than a bare CRUD demo.

## What Changes

- Add H2 persistence accessed through Exposed, plus JSON serialization for the API.
- Add the domain model from `CLAUDE.md`: User, Deck, Card, ReviewState (unique on `userId` + `cardId`).
- Add the REST endpoints: create user, create deck, list and add cards in a deck, get the next due card for a user, submit a review.
- **Departs from the `CLAUDE.md` sketch:** reviews use a four-level grade (`again`, `hard`, `good`, `easy`) instead of `{"correct": true}`. Each grade maps to an interval rule (see design).
- **New beyond the sketch:** an append-only `Review` log records every submitted review (user, card, grade, time, resulting interval) next to the updated `ReviewState`.
- The scheduler is a pure function `(state, grade, today) -> newState`, so it can be tested without a server or database.
- Out of scope: authentication, deck ownership, typed cards (cloze, multiple choice), tags, media, daily new-card limits, and anything beyond the doubling-style interval rules.

## Capabilities

### New Capabilities
- `users`: create a study user and reject operations for unknown users.
- `decks-and-cards`: create decks, add cards to a deck, and list a deck's cards. Decks and cards are shared content.
- `study-reviews`: determine which cards are due for a user, serve the next due card, accept graded reviews, update per-user review state, and append to the review log.

### Modified Capabilities
<!-- None: openspec/specs/ is empty. -->

## Impact

- **Code:** new files under `src/main/kotlin` (database setup, tables, models, scheduler, routes); `Routing.kt` and `application.yaml` get new module registrations; tests are added next to `ServerTest.kt`.
- **Dependencies:** Exposed (core, JDBC), H2, and Ktor content negotiation with kotlinx.serialization (plus the Kotlin serialization Gradle plugin). Test dependencies are unchanged.
- **API:** the review request body is `{"grade": "again|hard|good|easy"}`. `CLAUDE.md` currently documents `{"correct": true}` and its "correct/wrong" interval wording needs updating when this lands.
- **Data:** an embedded H2 database. No migration concerns, since there is no existing data.
