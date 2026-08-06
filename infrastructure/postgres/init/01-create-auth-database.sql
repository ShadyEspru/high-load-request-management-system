SELECT 'CREATE DATABASE hlrms_auth OWNER hlrms'
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_database
    WHERE datname = 'hlrms_auth'
)\gexec
