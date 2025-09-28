-- Add foreign key constraints to ensure referential integrity

-- Add foreign key from bets to users
ALTER TABLE bets
ADD CONSTRAINT fk_bets_user_id
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Add foreign key from bets to events
ALTER TABLE bets
ADD CONSTRAINT fk_bets_event_id
FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE;

-- Add foreign key from event_drivers to events
ALTER TABLE event_drivers
ADD CONSTRAINT fk_event_drivers_event_id
FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE;

-- Add indexes for better performance
CREATE INDEX idx_bets_user_id ON bets(user_id);
CREATE INDEX idx_bets_event_id ON bets(event_id);
CREATE INDEX idx_bets_status ON bets(status);
CREATE INDEX idx_event_drivers_event_id ON event_drivers(event_id);
CREATE INDEX idx_events_year_country ON events(event_year, country);
