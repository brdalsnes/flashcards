# decks-and-cards Specification

## Purpose

Defines the shared study content: decks and the cards inside them. Decks and cards are not owned by any user, so every user studies the same content.

## Requirements

### Requirement: Create a deck
The system SHALL allow a client to create a deck by providing a non-blank `name`, and SHALL respond with the created deck including its generated `id`.

#### Scenario: Successful creation
- **WHEN** a client sends `POST /decks` with `{"name": "Norwegian verbs"}`
- **THEN** the system responds `201 Created` with a JSON body containing a new `id` and the `name`

#### Scenario: Blank name rejected
- **WHEN** a client sends `POST /decks` with a blank or missing `name`
- **THEN** the system responds `400 Bad Request` and creates no deck

### Requirement: Add a card to a deck
The system SHALL allow a client to add a card to an existing deck by providing a non-blank `front` and `back`, and SHALL respond with the created card including its generated `id` and its `deckId`.

#### Scenario: Successful add
- **WHEN** a client sends `POST /decks/{id}/cards` with `{"front": "hund", "back": "dog"}` for an existing deck
- **THEN** the system responds `201 Created` with a JSON body containing a new `id`, the `deckId`, `front` and `back`

#### Scenario: Deck does not exist
- **WHEN** a client adds a card to a deck id that does not exist
- **THEN** the system responds `404 Not Found` and creates no card

#### Scenario: Blank side rejected
- **WHEN** a client adds a card whose `front` or `back` is blank or missing
- **THEN** the system responds `400 Bad Request` and creates no card

### Requirement: List cards in a deck
The system SHALL return all cards belonging to a deck, in the order they were added.

#### Scenario: Deck with cards
- **WHEN** a client sends `GET /decks/{id}/cards` for a deck that has cards
- **THEN** the system responds `200 OK` with a JSON array of those cards, oldest first

#### Scenario: Empty deck
- **WHEN** a client lists the cards of an existing deck with no cards
- **THEN** the system responds `200 OK` with an empty JSON array

#### Scenario: Deck does not exist
- **WHEN** a client lists the cards of a deck id that does not exist
- **THEN** the system responds `404 Not Found`
