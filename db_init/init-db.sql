CREATE DATABASE user_db;
CREATE DATABASE reservation_db;
CREATE DATABASE payment_db;
CREATE DATABASE notification_db;

-- We are using a single postgres container for multiple databases.
-- The username and password for these databases will be taken from POSTGRES_USER and POSTGRES_PASSWORD in .env
