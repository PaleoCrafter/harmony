#!/bin/bash

set -e
set -u

echo "Creating import and data user for harmony"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
	    CREATE USER $DB_IMPORT_USER WITH PASSWORD '$DB_IMPORT_PASSWORD';
        CREATE DATABASE harmony OWNER $DB_IMPORT_USER;
	    GRANT ALL PRIVILEGES ON DATABASE harmony TO $DB_IMPORT_USER;

	    CREATE USER $DB_DATA_USER WITH PASSWORD '$DB_DATA_PASSWORD';
	    GRANT CONNECT ON DATABASE harmony TO $DB_DATA_USER;

	    \connect harmony
	    GRANT USAGE ON SCHEMA public TO $DB_DATA_USER;
	    GRANT SELECT ON ALL TABLES IN SCHEMA public TO $DB_DATA_USER;
	    GRANT INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO $DB_DATA_USER;

	    ALTER DEFAULT PRIVILEGES FOR ROLE $DB_IMPORT_USER IN SCHEMA public GRANT SELECT ON TABLES TO $DB_DATA_USER;
        ALTER DEFAULT PRIVILEGES FOR ROLE $DB_IMPORT_USER IN SCHEMA public GRANT INSERT, UPDATE, DELETE ON TABLES TO $DB_DATA_USER;
EOSQL

echo "Database users created"
