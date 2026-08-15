# JWT Security

## Token Producer

`identity-service` is the only service that signs Vednex access tokens.

## Signing

Access tokens use `RS256`.

Identity Service loads:

```text
JWT_PRIVATE_KEY
JWT_PRIVATE_KEY_FILE
JWT_PUBLIC_KEY
JWT_PUBLIC_KEY_FILE
```

API Gateway and future business services load only:

```text
JWT_PUBLIC_KEY
JWT_PUBLIC_KEY_FILE
```

Do not share the private key with Gateway, CRM, Finance, Workforce, Support, AI, Analytics, or frontend code.

## Required Claims

Access tokens must contain:

```text
sub
companyId
membershipId
roles
permissions
type
iat
exp
iss
aud
```

Current values:

```text
type=access
iss=vednex-identity-service
aud=vednex-business-suite
```

`JWT_ISSUER` and `JWT_AUDIENCE` configure the issuer and audience.

## Consumers

Public-key consumers validate JWTs locally:

```text
API Gateway
CRM Service
Finance Service
Workforce Service
Support Service
AI Service
Analytics Service
```

Consumers must validate signature, expiry, token type, issuer, and audience. They must not call Identity Service for synchronous token validation on every request.

## Refresh Tokens

Refresh tokens remain opaque Identity-owned credentials. They are not JWTs, are hashed before storage, rotate on use, track token family, and remain protected by reuse detection.

## Development Keys

Use one of these local approaches:

```text
JWT_PRIVATE_KEY=<base64-or-pem-private-key>
JWT_PUBLIC_KEY=<base64-or-pem-public-key>
```

or mounted PEM files:

```text
JWT_PRIVATE_KEY_FILE=../../secrets/jwt-private.pem
JWT_PUBLIC_KEY_FILE=../../secrets/jwt-public.pem
```

Docker Compose mounts local `secrets/` at `/run/secrets/vednex` and defaults to:

```text
/run/secrets/vednex/jwt-private.pem
/run/secrets/vednex/jwt-public.pem
```

`secrets/`, `*.pem`, and `*.key` are ignored by Git.

## Header Contract

Gateway forwards `Authorization: Bearer <access-token>` unchanged. Downstream services should read tenant and permission information from the signed JWT claims, not from unsigned user or tenant headers.

## JWKS And Rotation

A JWKS endpoint such as `/.well-known/jwks.json` is deferred. Future rotation should add `kid` headers, expose multiple public keys during transition, and retire old keys after all issued access tokens have expired.
