# API Gateway

## Purpose

The API Gateway is the browser-facing backend entry point for Vednex AI Business Suite. It lets the frontend call one stable base URL while backend services move behind internal service addresses.

Current request flow:

```text
Next.js :3000
  -> API Gateway :8080
  -> Identity Service :8081
```

## Gateway Responsibilities

- Route existing API paths to the Identity Service without changing request paths.
- Handle browser-facing CORS.
- Preserve or generate `X-Correlation-ID`.
- Validate RS256 access JWTs locally before forwarding protected Identity routes.
- Add compatible security headers.
- Log request metadata without tokens, passwords, cookies, or bodies.
- Expose its own `/actuator/health`.

## Identity Responsibilities

The Identity Service remains responsible for authentication, access JWT signing, its own JWT validation, refresh-token rotation, refresh-token reuse detection, user/company/membership validation, RBAC, tenant isolation, invitations, password recovery, subscriptions, and audit logging.

The Gateway does not store sessions, sign tokens, validate business permissions, call Identity for per-request token validation, or own a database.

## Route Table

| Path | Target |
| --- | --- |
| `/api/auth/**` | Identity Service |
| `/api/company/**` | Identity Service |
| `/api/users/**` | Identity Service |
| `/api/roles/**` | Identity Service |
| `/api/permissions/**` | Identity Service |
| `/api/subscriptions/**` | Identity Service |
| `/api/public/**` | Identity Service |
| `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | Identity Service |

Paths are forwarded unchanged. For example, `/api/auth/login` reaches Identity as `/api/auth/login`.

## Public And Protected Routes

Public routes stay public because the Gateway permits forwarding and Identity Service keeps its existing Spring Security rules:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/verify-email`
- `POST /api/auth/resend-verification`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `POST /api/company/invitations/{token}/accept`
- `/api/public/**`

Protected routes require a valid RS256 access JWT at the Gateway and still require Identity Service authorization, including `/api/users/me`, `/api/company/**`, `/api/company/users/**`, `/api/roles/**`, `/api/permissions/**`, `/api/subscriptions/**`, and `POST /api/auth/switch-company`.

## Ports

- Frontend: `3000`
- API Gateway: `8080`
- Identity Service: `8081`
- PostgreSQL: `5432`

## Local Development

Run PostgreSQL, then Identity Service, then API Gateway, then frontend.

The Gateway uses:

```text
IDENTITY_SERVICE_URL=http://localhost:8081
FRONTEND_ALLOWED_ORIGINS=http://localhost:3000
JWT_PUBLIC_KEY=<base64-or-pem-public-key>
```

The frontend should call:

```text
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

## Docker Communication

Docker Compose runs Gateway and Identity on the same Compose network. The Gateway reaches Identity through:

```text
http://identity-service:8081
```

No container IP addresses are hardcoded.

## CORS

Gateway CORS is configured with `FRONTEND_ALLOWED_ORIGINS`, defaulting to `http://localhost:3000`. It allows the current API methods, `Authorization`, `Content-Type`, and correlation headers, allows credentials, and exposes `X-Correlation-ID`.

Wildcard origins are not used with credentialed requests.

Identity Service keeps CORS support for internal/local compatibility, but its default allowed origin is narrowed to the Gateway at `http://localhost:8080`.

## Correlation IDs

The Gateway reads `X-Correlation-ID`. If it is a valid UUID, it is preserved. Otherwise the Gateway generates a new UUID. The value is added to downstream requests, responses, and Gateway request logs.

## Authentication Forwarding

Bearer tokens are forwarded unchanged. The Gateway validates protected requests locally with `JWT_PUBLIC_KEY` or `JWT_PUBLIC_KEY_FILE` and rejects missing, expired, malformed, wrong-signature, wrong-type, wrong-issuer, or wrong-audience access tokens with `401`.

The Gateway does not receive `JWT_PRIVATE_KEY` and cannot sign tokens. Identity Service signs access tokens with `JWT_PRIVATE_KEY` and validates protected Identity requests again with its public key.

Refresh tokens remain transported in JSON payloads/localStorage by the current frontend and Identity implementation. The Gateway does not implement refresh-token database logic.

## Future Services

Future services can be added behind the Gateway with new route groups, local RS256 JWT validation, and the same correlation ID propagation model. RabbitMQ and Notification Service remain separate migration steps.
