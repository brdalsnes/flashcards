# Tasks

## 1. Dependencies and JSON

- [x] 1.1 Add Exposed (core, JDBC), H2, Ktor content negotiation with kotlinx.serialization, and the Kotlin serialization Gradle plugin to `gradle/libs.versions.toml` and `build.gradle.kts`; verify `./gradlew build` succeeds and the existing `ServerTest` still passes
- [x] 1.2 Install content negotiation in a new module registered in `application.yaml`; verify with a test that a trivial `@Serializable` response from a test-only route is returned as JSON

## 2. Scheduler

- [x] 2.1 Implement the `Grade` enum and the pure `schedule(previousIntervalDays, grade, today)` function in `Scheduler.kt`; verify with unit tests covering every scenario under "Grade determines the next interval" (again, hard, good, easy, new card, hard minimum of 1 day)

## 3. Database and schema

- [x] 3.1 Add `Database.kt` (H2 connection, file-backed by default and overridable for tests) and `Tables.kt` with `users`, `decks`, `cards`, `review_states` (unique on `user_id`, `card_id`) and `reviews`; verify with a test that tables are created on startup and a duplicate `(user_id, card_id)` state insert fails
- [x] 3.2 Register the database setup as a module in `application.yaml`; verify `./gradlew run` starts without errors and `./gradlew test` still passes

## 4. Users

- [x] 4.1 Add request/response models and `POST /users` in `UserRoutes.kt` with name validation and a shared "user exists" check for `/users/{userId}/...` routes; verify with tests for 201 with generated id and 400 on blank or missing name
- [x] 4.2 Document the endpoint in `CLAUDE.md` if its behavior differs from the sketch; verify the table matches the implemented paths

## 5. Decks and cards

- [x] 5.1 Implement `POST /decks` in `DeckRoutes.kt`; verify with tests for 201 and 400 on blank name
- [x] 5.2 Implement `POST /decks/{id}/cards` and `GET /decks/{id}/cards`; verify with tests for 201, 404 on unknown deck, 400 on blank side, empty list, and oldest-first ordering

## 6. Study flow

- [x] 6.1 Inject a `java.time.Clock` into the study routes (system default, fixed in tests); verify a test app can be started with a fixed clock
- [x] 6.2 Implement `GET /users/{userId}/decks/{id}/next` in `StudyRoutes.kt` (due reviewed cards by `due_date` then id, then new cards by id), returning a JSON array of zero or one cards; verify with tests for new card, overdue before new, not yet due, nothing due (200 with empty array), unknown user or deck (404), and independent schedules for two users
- [x] 6.3 Implement `POST /users/{userId}/cards/{cardId}/review` validating the grade, user and card before writing, computing the new state with the scheduler, and upserting `review_states` and inserting into `reviews` in one transaction; verify with tests for first review creating state, later review updating the same row, each grade's interval, 400 on invalid grade, and 404 on unknown user or card with no log entry written
- [x] 6.4 Verify the review log: a test submits three reviews for one card and checks three `reviews` rows with the right grades and resulting intervals, and a test forces a failure after the state write to confirm neither the state change nor a log entry persists

## 7. Docs and integration

- [x] 7.1 Update `CLAUDE.md`: replace the `{"correct": true}` review body with the grade body, describe the grade-to-interval rule and the review log, and list the final file layout; verify by reading it against the implemented routes
- [x] 7.2 End-to-end check: with the server running, create a user, deck and cards via HTTP, then fetch the next card, review it, and confirm the schedule changes as specified; verify the observed responses match the specs
