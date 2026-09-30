# Spec Delta

## Purpose

Defines spaced-repetition studying: which cards are due for a user, how graded reviews reschedule a card, and the permanent history of reviews. Schedules are independent per user.

## ADDED Requirements

### Requirement: Due cards
A card SHALL be due for a user when that user has no review state for it, or when the review state's `dueDate` is today or earlier. Review state SHALL be kept per user and card, so the same card has independent schedules for different users.

#### Scenario: New card is due
- **WHEN** a user has never reviewed a card
- **THEN** the card is due for that user

#### Scenario: Reviewed card not yet due
- **WHEN** a user's review state for a card has a `dueDate` after today
- **THEN** the card is not due for that user

#### Scenario: Schedules are independent
- **WHEN** one user reviews a card and another user has not
- **THEN** the card is scheduled for the first user and is still new and due for the second

### Requirement: Next due card
The system SHALL return the next due card in a deck for a user via `GET /users/{userId}/decks/{id}/next`. Among due cards, those the user has reviewed SHALL come first, ordered by earliest `dueDate`, followed by new cards in the order they were added. The response SHALL always be `200 OK` with a JSON array that holds at most one card, each with `id`, `front` and `back`: the first due card, or an empty array when no card is due.

#### Scenario: Due card available
- **WHEN** a user requests the next card for a deck that has due cards
- **THEN** the system responds `200 OK` with an array containing only the first due card according to the ordering

#### Scenario: Overdue before new
- **WHEN** a deck has one new card and one reviewed card that is due
- **THEN** the array contains the reviewed, due card

#### Scenario: Nothing due
- **WHEN** a user requests the next card and no card in the deck is due
- **THEN** the system responds `200 OK` with an empty JSON array

#### Scenario: Deck does not exist
- **WHEN** a user requests the next card for a deck id that does not exist
- **THEN** the system responds `404 Not Found`

### Requirement: Submit a graded review
The system SHALL accept a review via `POST /users/{userId}/cards/{cardId}/review` with a body `{"grade": "<grade>"}`, where `<grade>` is one of `again`, `hard`, `good` or `easy`. On success it SHALL create or update the user's review state for that card and respond with the new state, including `dueDate` and `intervalDays`.

#### Scenario: First review creates state
- **WHEN** a user reviews a card they have never reviewed
- **THEN** a review state is created for that user and card and returned

#### Scenario: Later review updates state
- **WHEN** a user reviews a card that already has a review state
- **THEN** the existing state is updated and no second state exists for that user and card

#### Scenario: Invalid grade
- **WHEN** the body contains a grade other than the four allowed values, or no grade
- **THEN** the system responds `400 Bad Request` and changes nothing

#### Scenario: Card does not exist
- **WHEN** a user reviews a card id that does not exist
- **THEN** the system responds `404 Not Found` and changes nothing

### Requirement: Grade determines the next interval
The new interval SHALL be computed from the previous interval in days, where a card with no review state counts as having a previous interval of 1 day: `again` sets the interval to 1 day; `hard` keeps the previous interval, with a minimum of 1 day; `good` doubles it; `easy` multiplies it by 4. The new `dueDate` SHALL be today plus the new interval.

#### Scenario: Again resets
- **WHEN** a user grades `again` on a card whose interval is 8 days
- **THEN** the new interval is 1 day and the `dueDate` is tomorrow

#### Scenario: Hard keeps the interval
- **WHEN** a user grades `hard` on a card whose interval is 4 days
- **THEN** the new interval is 4 days

#### Scenario: Good doubles
- **WHEN** a user grades `good` on a card whose interval is 4 days
- **THEN** the new interval is 8 days

#### Scenario: Easy quadruples
- **WHEN** a user grades `easy` on a card whose interval is 4 days
- **THEN** the new interval is 16 days

#### Scenario: New card first review
- **WHEN** a user grades `good` on a card they have never reviewed
- **THEN** the new interval is 2 days and the `dueDate` is two days from today

### Requirement: Review log
The system SHALL append one permanent log entry for every successful review, recording the user, the card, the grade, the time of the review and the resulting interval. Log entries SHALL NOT be changed or removed by later reviews, and a review SHALL either update the review state and write its log entry together or do neither.

#### Scenario: Entry per review
- **WHEN** a user submits three successful reviews for the same card
- **THEN** three log entries exist for that user and card, each with the grade and resulting interval of its own review

#### Scenario: Failed review leaves no entry
- **WHEN** a review is rejected because of an invalid grade or an unknown user or card
- **THEN** no log entry is written
