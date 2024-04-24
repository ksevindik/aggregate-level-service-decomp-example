DROP TABLE IF EXISTS id_mappings;
DROP TABLE IF EXISTS players;
DROP TABLE IF EXISTS clubs;


CREATE TABLE  id_mappings (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                service_id BIGINT NOT NULL,
                                monolith_id BIGINT NOT NULL,
                              type_name VARCHAR(255) NOT NULL
);

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