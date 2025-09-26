INSERT INTO users (id, name, balance, created_at)
VALUES
    ('7dd48e26-bc67-47d6-974c-321374be722f', 'sreenath', 100.00, NOW()),
    ('2a575f7a-cc51-40ba-85aa-2f695edc2f9e', 'bharath', 100.00, NOW());

INSERT INTO events (id, name, country, event_year, session_type, start_time, outcome_driver_id)
VALUES
    ('7a91ca3e-8348-4712-8561-15457c44945a', 'Sprint - Spa-Francorchamps', 'Belgium', 2023, 'Race', '2023-07-29T15:05:00Z', NULL),
    ('3a803829-c43e-4c5c-9101-2ad430d9eff8', 'Grand Prix - Monza', 'Italy', 2023, 'Race', '2023-09-03T13:00:00Z', NULL);

-- Drivers for Belgium Sprint
INSERT INTO event_drivers (id, event_id, driver_id, full_name, odds)
VALUES
    ('dcada09c-31fd-42fe-9663-f6006f3324bb', '7a91ca3e-8348-4712-8561-15457c44945a', '44', 'Lewis Hamilton', 2),
    ('22cc8ebb-4a25-49b9-adc0-a36175e63848', '7a91ca3e-8348-4712-8561-15457c44945a', '33', 'Max Verstappen', 3),
    ('aa9f1111-dd4e-4636-b253-2e45f873b9ed', '7a91ca3e-8348-4712-8561-15457c44945a', '16', 'Charles Leclerc', 4);

-- Drivers for Monza GP
INSERT INTO event_drivers (id, event_id, driver_id, full_name, odds)
VALUES
    ('744ecbac-65a9-436e-9ce3-c8e1907dccf0', '3a803829-c43e-4c5c-9101-2ad430d9eff8', '55', 'Carlos Sainz', 2),
    ('169e1ea6-e704-4a2c-a16b-bfc55b1759eb', '3a803829-c43e-4c5c-9101-2ad430d9eff8', '11', 'Sergio Perez', 3),
    ('59f4441c-2c17-4da2-b2a6-e7ad632bddeb', '3a803829-c43e-4c5c-9101-2ad430d9eff8', '63', 'George Russell', 4);

INSERT INTO bets (id, user_id, event_id, driver_id, stake, odds, status, placed_at, settled_at)
VALUES
    ('b8d76a85-5a2f-4084-ae1a-101bd8ffab5b', '7dd48e26-bc67-47d6-974c-321374be722f', '7a91ca3e-8348-4712-8561-15457c44945a', '33', 10.00, 3, 'PENDING', NOW(), NULL),
    ('19c7144b-2c63-4a7f-a927-27d2e5b4c169', '2a575f7a-cc51-40ba-85aa-2f695edc2f9e', '7a91ca3e-8348-4712-8561-15457c44945a', '16', 20.00, 4, 'PENDING', NOW(), NULL);

INSERT INTO drivers (id, full_name, country, team) VALUES
                                                       ('44', 'Lewis Hamilton', 'United Kingdom', 'Mercedes'),
                                                       ('63', 'George Russell', 'United Kingdom', 'Mercedes'),
                                                       ('33', 'Max Verstappen', 'Netherlands', 'Red Bull Racing'),
                                                       ('11', 'Sergio Perez', 'Mexico', 'Red Bull Racing'),
                                                       ('16', 'Charles Leclerc', 'Monaco', 'Ferrari'),
                                                       ('55', 'Carlos Sainz', 'Spain', 'Ferrari'),
                                                       ('4', 'Lando Norris', 'United Kingdom', 'McLaren'),
                                                       ('81', 'Oscar Piastri', 'Australia', 'McLaren'),
                                                       ('14', 'Fernando Alonso', 'Spain', 'Aston Martin'),
                                                       ('18', 'Lance Stroll', 'Canada', 'Aston Martin');
