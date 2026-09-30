# users Specification

## Purpose

Defines how study users are created and identified. Users are identified by a plain id with no authentication, so that per-user review schedules can be kept apart.

## Requirements

### Requirement: Create a user
The system SHALL allow a client to create a user by providing a non-blank `name`, and SHALL respond with the created user including its generated `id`.

#### Scenario: Successful creation
- **WHEN** a client sends `POST /users` with `{"name": "Ada"}`
- **THEN** the system responds `201 Created` with a JSON body containing a new `id` and `name` "Ada"

#### Scenario: Blank name rejected
- **WHEN** a client sends `POST /users` with a blank or missing `name`
- **THEN** the system responds `400 Bad Request` and creates no user

### Requirement: Unknown users are rejected
Every endpoint addressed by a `userId` SHALL respond `404 Not Found` when no user with that id exists, and SHALL NOT create the user or change any state.

#### Scenario: Next card for unknown user
- **WHEN** a client requests the next due card for a `userId` that does not exist
- **THEN** the system responds `404 Not Found`

#### Scenario: Review as unknown user
- **WHEN** a client submits a review under a `userId` that does not exist
- **THEN** the system responds `404 Not Found` and records no review state or log entry
