package com.bp.jaringochi.domain.statistics.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/*
DTO = 데이터의 "모양" -> 응답 구조가 다르면 여러 개 (이 경우엔 3개)
StatisticsSummary는 나머지 둘과 다른 API 응답. 
다른 것과 함께 묶으면 하나가 응답이 있으면 나머지는 null이 나와 응답마다 의미 없는 빈 필드가 노출됨 
CategoryStatistics가 List<CategoryStatItem>을 품는 부모-자식 관계라, 한 클래스로는 표현이 안 됨.
*/

@Getter
@Setter
public class StatisticsSummary {
    private BigDecimal income;    // 수입 합 (SQL)
    private BigDecimal expense;   // 지출 합 (SQL)
    private BigDecimal balance;   // income - expense (서비스)
}
