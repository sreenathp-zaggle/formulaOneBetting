-- Users table
CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       name VARCHAR(255),
                       balance DECIMAL(19,2) NOT NULL,
                       created_at TIMESTAMP NOT NULL
);

-- Events table
CREATE TABLE events (
                        id UUID PRIMARY KEY,
                        name VARCHAR(255),
                        country VARCHAR(255),
                        event_year INTEGER,
                        session_type VARCHAR(255),
                        start_time TIMESTAMP,
                        outcome_driver_id INTEGER
);

-- Event drivers table
CREATE TABLE event_drivers (
                               id UUID PRIMARY KEY,
                               event_id UUID NOT NULL,
                               driver_id INTEGER NOT NULL,
                               full_name VARCHAR(255),
                               odds INTEGER,
                               UNIQUE(event_id, driver_id)
);

-- Bets table
CREATE TABLE bets (
                      id UUID PRIMARY KEY,
                      user_id UUID NOT NULL,
                      event_id UUID NOT NULL,
                      driver_id INTEGER NOT NULL,
                      stake DECIMAL(19,2),
                      odds INTEGER,
                      status VARCHAR(50),
                      placed_at TIMESTAMP,
                      settled_at TIMESTAMP
);
