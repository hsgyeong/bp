-- ═══════════════════════════════════════════════
-- 가계부 앱 (Jaringochi) - 전체 스키마
-- 실행: 위에서부터 순서대로
-- ═══════════════════════════════════════════════

-- ─────────────────────────────────────────────
-- [1] 데이터베이스 생성
-- ─────────────────────────────────────────────
CREATE DATABASE IF NOT EXISTS `jaringochi`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `jaringochi`;

-- ─────────────────────────────────────────────
-- [재실행 대비] 기존 테이블 제거 (개발용 — mode=always)
-- FK 체크를 잠시 꺼서 순서 상관없이 DROP
-- ─────────────────────────────────────────────
SET FOREIGN_KEY_CHECKS = 0;
-- refresh_token은 user를 참조하므로 user보다 먼저 삭제해야 한다.
DROP TABLE IF EXISTS `refresh_token`;
DROP TABLE IF EXISTS `monthly_report`;
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `transaction`;
DROP TABLE IF EXISTS `weekly_budget`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `user`;
SET FOREIGN_KEY_CHECKS = 1;

-- ─────────────────────────────────────────────
-- [2] 테이블 생성
-- ─────────────────────────────────────────────

-- 사용자
CREATE TABLE `user` (
  `id`         BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '사용자 ID',
  `email`      VARCHAR(255) UNIQUE NOT NULL            COMMENT '로그인 이메일',
  `password`   VARCHAR(255) NOT NULL                   COMMENT '암호화 저장',
  `nickname`   VARCHAR(50)  NOT NULL                   COMMENT '표시 이름',
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP  COMMENT '가입 시각',
  `deleted_at` DATETIME     NULL                       COMMENT 'NULL=활성 / 값=탈퇴 시점(soft delete)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 카테고리
CREATE TABLE `category` (
  `id`            BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '카테고리 ID',
  `user_id`       BIGINT       NULL                       COMMENT 'NULL이면 기본 제공 카테고리',
  `name`          VARCHAR(50)  NOT NULL                   COMMENT '월급, 식비 등',
  `type`          TINYINT      NOT NULL                   COMMENT '1=수입 / 2=지출',
  `display_order` INT          DEFAULT 0                  COMMENT '수입/지출 그룹 내 표시 순서',
  `is_active`     TINYINT(1)      NOT NULL DEFAULT 1         COMMENT '1=사용 / 0=숨김(soft delete)',
  `icon`          VARCHAR(255) NULL                       COMMENT '(선택) 아이콘',
  `color`         VARCHAR(20)  NULL                       COMMENT '(선택) 차트 색상',
  `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP  COMMENT '생성 시각',
  `updated_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 거래 내역 (수입·지출 통합)
CREATE TABLE `transaction` (
  `id`          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '거래 ID',
  `user_id`     BIGINT        NOT NULL                   COMMENT '소유자',
  `category_id` BIGINT        NOT NULL                   COMMENT '분류',
  `amount`      DECIMAL(12,2) NOT NULL                   COMMENT '금액',
  `type`        TINYINT       NOT NULL                   COMMENT '1=수입 / 2=지출',
  `memo`        VARCHAR(255)  NULL                       COMMENT '메모',
  `date`        DATE          NOT NULL                   COMMENT '거래 일자'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 주간 예산
CREATE TABLE `weekly_budget` (
  `id`           BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '주간예산 ID',
  `user_id`      BIGINT        NOT NULL                   COMMENT '소유자',
  `amount`       DECIMAL(12,2) NOT NULL                   COMMENT '그 주 예산 한도',
  `start_date`   DATE          NOT NULL                   COMMENT '주 시작일',
  `end_date`     DATE          NOT NULL                   COMMENT '주 종료일',
  `update_count` INT           DEFAULT 0                  COMMENT '그 달 수정 횟수(월 2회 제한)',
  `created_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '주마다 새로 생성',
  `updated_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '주중 변경 시각'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 예산 초과 알림
CREATE TABLE `notification` (
  `id`               BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '알림 ID',
  `user_id`          BIGINT        NOT NULL                   COMMENT '받는 사람',
  `weekly_budget_id` BIGINT        NOT NULL                   COMMENT '어느 주 예산 기준',
  `threshold`        SMALLINT      NOT NULL                   COMMENT '25/50/75/100/125/150 (한 행=한 단계)',
  `current_budget`   DECIMAL(12,2) NULL                       COMMENT '그 시점 예산',
  `spent_money`      DECIMAL(12,2) NULL                       COMMENT '그 시점 지출 합계',
  `ratio`            DECIMAL(5,2)  NULL                       COMMENT 'spent_money / current_budget (%)',
  `is_read`          TINYINT       NOT NULL DEFAULT 0         COMMENT '0=안읽음 / 1=읽음',
  `created_at`       DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '알림 발생 시각',
  UNIQUE KEY `uq_budget_threshold` (`weekly_budget_id`, `threshold`) -- 앱 로직(mexSent 체크)이 이미 중복을 막지만, 동시 요청 충돌용 DB 안전망
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI 월간 레포트 (월 1회 생성 후 저장)
CREATE TABLE `monthly_report` (
  `id`               BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '레포트 ID',
  `user_id`          BIGINT        NOT NULL                   COMMENT '소유자',
  `report_year`      INT           NOT NULL                   COMMENT '대상 연도',
  `report_month`     INT           NOT NULL                   COMMENT '대상 월(1~12)',
  -- 통계 스냅샷 (생성 시점 고정)
  `total_expense`    DECIMAL(12,2) NULL                       COMMENT '그 달 총 지출',
  `prev_expense`     DECIMAL(12,2) NULL                       COMMENT '전월 총 지출',
  `diff_ratio`       DECIMAL(7,2)  NULL                       COMMENT '전월 대비 %, prev=0이면 NULL',
  `success_weeks`    INT           NULL                       COMMENT '예산 성공 주 수(ratio<=100)',
  `total_weeks`      INT           NULL                       COMMENT '그 달에 걸친 주간예산 수',
  `top_category`     VARCHAR(50)   NULL                       COMMENT '가장 많이 쓴 카테고리명',
  `category_json`    TEXT          NULL                       COMMENT '카테고리 breakdown(이름/금액/비율/전월대비) JSON',
  -- AI 생성 텍스트
  `one_liner`        VARCHAR(255)  NULL                       COMMENT '굴비 한줄평',
  `mood`             VARCHAR(20)   NULL                       COMMENT 'hello|warn|happy|sad|hungry|sulk|angry',
  `category_comment` VARCHAR(500)  NULL                       COMMENT '카테고리 분석 코멘트',
  `advice`           VARCHAR(500)  NULL                       COMMENT '다음 달 절약 조언',
  -- 굴비에게 한 마디 (월 1회)
  `user_message`     VARCHAR(255)  NULL                       COMMENT '사용자가 굴비에게 건넨 말',
  `gulbi_reply`      VARCHAR(500)  NULL                       COMMENT '굴비의 응답',
  `replied_at`       DATETIME      NULL                       COMMENT '응답 시각(=한 마디 사용 여부)',
  `generated_at`     DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '레포트 생성 시각',
  UNIQUE KEY `uq_report_user_month` (`user_id`, `report_year`, `report_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Refresh Token 저장
CREATE TABLE refresh_token (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(100) NOT NULL,
  expires_at DATETIME NOT NULL,
  revoked_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uq_refresh_token_hash (token_hash),
  INDEX idx_refresh_token_user_id (user_id),

  CONSTRAINT fk_refresh_token_user
    FOREIGN KEY (user_id) REFERENCES `user`(id)
);

-- ─────────────────────────────────────────────
-- [3] 외래키(FK) 연결
-- ─────────────────────────────────────────────
ALTER TABLE `category`      ADD FOREIGN KEY (`user_id`)          REFERENCES `user` (`id`);
ALTER TABLE `transaction`   ADD FOREIGN KEY (`user_id`)          REFERENCES `user` (`id`);
ALTER TABLE `transaction`   ADD FOREIGN KEY (`category_id`)      REFERENCES `category` (`id`);
ALTER TABLE `weekly_budget` ADD FOREIGN KEY (`user_id`)          REFERENCES `user` (`id`);
ALTER TABLE `notification`  ADD FOREIGN KEY (`user_id`)          REFERENCES `user` (`id`);
ALTER TABLE `notification`  ADD FOREIGN KEY (`weekly_budget_id`) REFERENCES `weekly_budget` (`id`);
ALTER TABLE `monthly_report` ADD FOREIGN KEY (`user_id`)          REFERENCES `user` (`id`);