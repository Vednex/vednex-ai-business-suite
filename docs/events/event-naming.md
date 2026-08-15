# Event Naming

Vednex domain events use:

```text
domain.entity.action.version
```

Rules:

- Use lowercase dot-separated names.
- Start with the producing bounded context, such as `identity`.
- Use a business entity name, such as `user`, `company`, or `email.verification`.
- Use a past-tense action for facts that already happened.
- End with an immutable version suffix such as `v1`.

Initial Identity event names:

```text
identity.company.created.v1
identity.user.registered.v1
identity.email.verification.requested.v1
identity.password.reset.requested.v1
identity.user.invited.v1
identity.user.login.succeeded.v1
identity.user.login.failed.v1
```

Currently emitted Identity events:

```text
identity.company.created.v1
identity.user.registered.v1
identity.email.verification.requested.v1
identity.password.reset.requested.v1
identity.user.invited.v1
```

Login events are named for future use but are not emitted yet.
