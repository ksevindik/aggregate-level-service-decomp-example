DROP TABLE IF EXISTS players;
DROP TABLE IF EXISTS clubs;

CREATE TABLE clubs (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       country VARCHAR(255) NOT NULL,
                       president VARCHAR(255),
                       created TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       modified TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE players (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         club_id BIGINT,
                         name VARCHAR(255) NOT NULL,
                         country VARCHAR(255) NOT NULL,
                         rating INTEGER,
                         created TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                         modified TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                         FOREIGN KEY (club_id) REFERENCES clubs(id)
);

INSERT INTO clubs (id, name, country, president, created, modified) VALUES (1, 'FC Barcelona', 'Spain', 'Joan Laporta', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO clubs (id, name, country, president, created, modified) VALUES (2, 'Manchester United', 'England', 'Avram Glazer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO clubs (id, name, country, president, created, modified) VALUES (3, 'Bayern Munich', 'Germany', 'Herbert Hainer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assuming club_id values for FC Barcelona, Manchester United, and Bayern Munich are 1, 2, 3 respectively
-- Players for FC Barcelona
INSERT INTO players (id, club_id, name, country, rating, created, modified) VALUES (1, 1, 'Lionel Messi', 'Argentina', 93, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO players (id, club_id, name, country, rating, created, modified) VALUES (2, 1, 'Gerard Piqué', 'Spain', 86, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Players for Manchester United
INSERT INTO players (id, club_id, name, country, rating, created, modified) VALUES (3, 2, 'Cristiano Ronaldo', 'Portugal', 92, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO players (id, club_id, name, country, rating, created, modified) VALUES (4, 2, 'Marcus Rashford', 'England', 85, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Players for Bayern Munich
INSERT INTO players (id, club_id, name, country, rating, created, modified) VALUES (5, 3, 'Robert Lewandowski', 'Poland', 91, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO players (id, club_id, name, country, rating, created, modified) VALUES (6, 3, 'Thomas Müller', 'Germany', 87, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- A player without a club
INSERT INTO players (id, club_id, name, country, rating, created, modified) VALUES (7, null, 'Free Agent', 'No Country', 70, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);