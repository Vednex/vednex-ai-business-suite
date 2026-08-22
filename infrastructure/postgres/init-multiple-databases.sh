#!/bin/bash
set -e

notification_db="${NOTIFICATION_POSTGRES_DB:-notification_db}"
crm_db="${CRM_POSTGRES_DB:-crm_db}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set=notification_db="$notification_db" --set=crm_db="$crm_db" <<'EOSQL'
SELECT format('CREATE DATABASE %I', :'notification_db')
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = :'notification_db'
)\gexec

SELECT format('CREATE DATABASE %I', :'crm_db')
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = :'crm_db'
)\gexec
EOSQL
