SELECT 'CREATE DATABASE hlrms_transfer OWNER hlrms'
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_database
    WHERE datname = 'hlrms_transfer'
)\gexec
