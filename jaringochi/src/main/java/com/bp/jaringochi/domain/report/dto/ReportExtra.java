package com.bp.jaringochi.domain.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 레포트 부가 지표 — monthly_report.extra_json 의 직렬화 구조.
 * 생성 시점 스냅샷(캐싱). 숫자는 통계/예산으로 계산해 채운다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReportExtra {

    private BigDecimal dailyAvg;        // 하루 평균 지출 (총지출 ÷ 그 달 전체 일수, 원 단위 반올림)
    private Integer noSpendDays;        // 무지출 날 수 (그 달 일수 − 지출 있은 날 수)
    private DayAmount biggestDay;       // 가장 많이 쓴 하루 (날짜+금액, 원본)
    private CategoryDelta savedMost;    // 전월 대비 가장 아낀 카테고리
    private CategoryDelta spentMost;    // 전월 대비 가장 늘어난 카테고리
    private List<WeekStat> weeks;       // 주차별 예산 달성 (완료된 주, 과거→현재)

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DayAmount {
        private LocalDate date;
        private BigDecimal amount;
        public DayAmount(LocalDate date, BigDecimal amount) {
            this.date = date;
            this.amount = amount;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CategoryDelta {
        private String name;
        private BigDecimal diff;        // 전월 대비 증감액 (savedMost는 음수, spentMost는 양수)
        public CategoryDelta(String name, BigDecimal diff) {
            this.name = name;
            this.diff = diff;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class WeekStat {
        private String label;          // "1주차" ...
        private BigDecimal ratio;      // 그 주 지출/예산 %
        private boolean pass;          // ratio <= 100
        public WeekStat(String label, BigDecimal ratio, boolean pass) {
            this.label = label;
            this.ratio = ratio;
            this.pass = pass;
        }
    }
}
