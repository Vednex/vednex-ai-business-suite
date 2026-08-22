from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "crm-service"
    app_env: str = "development"
    app_debug: bool = True
    app_host: str = "0.0.0.0"
    app_port: int = 8083
    api_prefix: str = "/api/crm"
    database_url: str = "postgresql+psycopg://vednex:vednex@localhost:5432/crm_db"
    jwt_public_key: str = ""
    jwt_public_key_file: str = ""
    jwt_issuer: str = "vednex-identity-service"
    jwt_audience: str = "vednex-business-suite"
    cors_allowed_origins: str = ""
    log_level: str = "INFO"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
