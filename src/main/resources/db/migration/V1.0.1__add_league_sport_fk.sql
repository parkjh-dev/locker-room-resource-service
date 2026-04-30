-- Phase 1: Sport cascading API 도입
-- FootballLeague / BaseballLeague 에 sport_id FK 추가하여
-- 종목·국가 cascading 조회를 자연스럽게 지원.

-- ───── football_leagues ─────
ALTER TABLE football_leagues ADD COLUMN sport_id BIGINT;
UPDATE football_leagues
   SET sport_id = (SELECT id FROM sports WHERE name_en = 'Football' LIMIT 1);
ALTER TABLE football_leagues MODIFY COLUMN sport_id BIGINT NOT NULL;
ALTER TABLE football_leagues
    ADD CONSTRAINT fk_football_leagues_sport
    FOREIGN KEY (sport_id) REFERENCES sports(id);
CREATE INDEX idx_football_leagues_sport ON football_leagues(sport_id);

-- ───── baseball_leagues ─────
ALTER TABLE baseball_leagues ADD COLUMN sport_id BIGINT;
UPDATE baseball_leagues
   SET sport_id = (SELECT id FROM sports WHERE name_en = 'Baseball' LIMIT 1);
ALTER TABLE baseball_leagues MODIFY COLUMN sport_id BIGINT NOT NULL;
ALTER TABLE baseball_leagues
    ADD CONSTRAINT fk_baseball_leagues_sport
    FOREIGN KEY (sport_id) REFERENCES sports(id);
CREATE INDEX idx_baseball_leagues_sport ON baseball_leagues(sport_id);
