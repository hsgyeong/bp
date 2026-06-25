-- 알림 이벤트 유형 확장 (BUDGET → BUDGET/DRAW/REPORT)
-- 기존 라이브 DB(jaringochi)에 적용. schema.sql 은 신규 생성용이므로 이미 반영됨.
-- 적용:  mysql -ussafy -pssafy jaringochi < 이 파일

ALTER TABLE `notification`
  ADD COLUMN `type` VARCHAR(20) NOT NULL DEFAULT 'BUDGET'
    COMMENT 'BUDGET=예산임계치 / DRAW=옷뽑기기회 / REPORT=월레포트' AFTER `user_id`,
  ADD COLUMN `report_year`  INT NULL COMMENT '레포트 대상 연도 (REPORT 전용)' AFTER `ratio`,
  ADD COLUMN `report_month` INT NULL COMMENT '레포트 대상 월 (REPORT 전용)' AFTER `report_year`,
  MODIFY COLUMN `weekly_budget_id` BIGINT NULL COMMENT '어느 주 예산 기준 (BUDGET/DRAW). REPORT는 NULL',
  MODIFY COLUMN `threshold` SMALLINT NULL COMMENT '25/50/75/100/125/150 (BUDGET 전용)',
  ADD UNIQUE KEY `uq_report_period` (`user_id`, `type`, `report_year`, `report_month`);
