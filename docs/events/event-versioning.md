# Event Versioning

Event versions are immutable.

Rules:

- Breaking payload changes require a new event type version, for example `identity.user.registered.v2`.
- Existing `v1` consumers must continue receiving the old schema until the event is retired through an explicit migration.
- New optional fields may be added to an existing version if consumers can ignore unknown fields.
- Consumers must ignore unknown optional fields.
- Removing fields, renaming fields, changing field types, or changing semantics is breaking.
- Routing keys include the version suffix.

Schema examples live in:

```text
docs/events/schemas/
```
