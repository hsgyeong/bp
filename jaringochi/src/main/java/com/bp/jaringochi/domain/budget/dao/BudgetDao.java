package com.bp.jaringochi.domain.budget.dao;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bp.jaringochi.domain.budget.dto.WeeklyBudget;

@Mapper
public interface BudgetDao {

    // ===== 엔드포인트 =====
    WeeklyBudget selectCurrentWeek(Long userId);          // 4-1. 현재 주 예산
    List<WeeklyBudget> selectRecentWeeks(Long userId);    // 4-2. 최근 4주 (통계 주 화면 공용)
    int insertWeeklyBudget(WeeklyBudget budget);          // 4-3. 등록
    int updateWeeklyBudget(WeeklyBudget budget);          // 4-4. 수정

    // ===== 보조 조회 (검증·중복확인용) =====
    WeeklyBudget selectByWeek(@Param("userId") Long userId,  // 4-3 등록 보조 : 같은 주 중복 검사(409)
                              @Param("startDate") LocalDate startDate);
    WeeklyBudget selectById(Long id);                        // 4-4 수정 보조 : 존재 확인(404) + 소유권(403) + 수정 후 최신본 반환
    
    
    // ==== 알림 관련 추가 ==== //
    // 알림 트리거용: 거래일이 속한 주의 예산(+지출합) 조회 — 없으면 null(예산 안 짠 주)
    WeeklyBudget selectByDate(@Param("userId") Long userId,
                              @Param("date") LocalDate date);
    /*
     
    <참고> selectById / selectByWeek는 왜 필요?
    
	둘 다 GET 엔드포인트가 아니라, 쓰기(등록·수정)의 "비즈니스 규칙"을 위한 내부 조회
	
	- selectById(Long id) - 수정(4-4)용
	4-4에서 수정 전에 그 행을 먼저 꺼내봐야 함:
	
	존재 확인 -> 없으면 404 (BUDGET_NOT_FOUND)
	소유권 확인 -> 남의 예산이면 403 (findOwned)
	수정 후 최신본 반환 -> UPDATE 직후 selectById로 다시 읽어 지출합 포함 객체를 응답
	-> "PUT 했는데 그게 존재하는지/내 건지/수정 결과가 뭔지"를 알려면 id로 단건 조회가 필수.
	
	- selectByWeek(userId, startDate) - 등록(4-3)용
	4-3 명세에 "같은 주 예산이 이미 있으면 409". 이것을 판단하려면 등록 전에 "이 유저가 이 주에 이미 예산을 만들었나?" 를 조회해야 함:
	-> 이 부분이 없으면 같은 주에 예산이 중복 INSERT 됨.
     
     */
    
    
    
}