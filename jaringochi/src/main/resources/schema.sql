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
  `is_active`     TINYINT      NOT NULL DEFAULT 1         COMMENT '1=사용 / 0=숨김(soft delete)',
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
  `threshold`        TINYINT       NOT NULL                   COMMENT '25/50/75/100/125/150 (한 행=한 단계)',
  `current_budget`   DECIMAL(12,2) NULL                       COMMENT '그 시점 예산',
  `spent_money`      DECIMAL(12,2) NULL                       COMMENT '그 시점 지출 합계',
  `ratio`            DECIMAL(5,2)  NULL                       COMMENT 'spent_money / current_budget (%)',
  `is_read`          TINYINT       NOT NULL DEFAULT 0         COMMENT '0=안읽음 / 1=읽음',
  `created_at`       DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '알림 발생 시각'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────
-- [3] 외래키(FK) 연결
-- ─────────────────────────────────────────────
ALTER TABLE `category`      ADD FOREIGN KEY (`user_id`)          REFERENCES `user` (`id`);
ALTER TABLE `transaction`   ADD FOREIGN KEY (`user_id`)          REFERENCES `user` (`id`);
ALTER TABLE `transaction`   ADD FOREIGN KEY (`category_id`)      REFERENCES `category` (`id`);
ALTER TABLE `weekly_budget` ADD FOREIGN KEY (`user_id`)          REFERENCES `user` (`id`);
ALTER TABLE `notification`  ADD FOREIGN KEY (`user_id`)          REFERENCES `user` (`id`);
ALTER TABLE `notification`  ADD FOREIGN KEY (`weekly_budget_id`) REFERENCES `weekly_budget` (`id`);