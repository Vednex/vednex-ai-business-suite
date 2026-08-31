from dataclasses import dataclass
from pathlib import Path
from uuid import UUID

from fastapi import HTTPException, Request, status
from jose import JWTError, jwt

from app.core.config import get_settings


@dataclass
class CurrentUser:
    user_id: UUID
    company_id: UUID
    roles: list[str]
    permissions: list[str]


def _load_public_key() -> str:
    settings = get_settings()
    if settings.jwt_public_key:
        return settings.jwt_public_key
    if settings.jwt_public_key_file:
        key_path = Path(settings.jwt_public_key_file)
        if key_path.exists():
            return key_path.read_text(encoding="utf-8")
    raise RuntimeError("JWT public key not configured")


def _decode_token(token: str) -> dict:
    settings = get_settings()
    public_key = _load_public_key()
    return jwt.decode(
        token,
        public_key,
        algorithms=["RS256"],
        issuer=settings.jwt_issuer,
        audience=settings.jwt_audience,
    )


async def get_current_user(request: Request) -> CurrentUser:
    auth_header = request.headers.get("Authorization", "")
    if not auth_header.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing or invalid Authorization header",
        )

    token = auth_header[7:]
    try:
        payload = _decode_token(token)
    except JWTError as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=f"Invalid token: {e}",
        )

    sub = payload.get("sub")
    company_id = payload.get("companyId")
    if not sub or not company_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token missing required claims",
        )

    return CurrentUser(
        user_id=UUID(sub),
        company_id=UUID(company_id),
        roles=payload.get("roles", []),
        permissions=payload.get("permissions", []),
    )


def require_permission(user: CurrentUser, permission: str) -> None:
    if permission not in user.permissions:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=f"Missing permission: {permission}",
        )
