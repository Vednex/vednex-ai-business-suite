#!/bin/bash
set -e

notification_db="${NOTIFICATION_POSTGRES_DB:-notification_db}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set=notification_db="$notification_db" <<'EOSQL'
SELECT format('CREATE DATABASE %I', :'notification_db')
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = :'notification_db'
)\gexec
EOSQL
