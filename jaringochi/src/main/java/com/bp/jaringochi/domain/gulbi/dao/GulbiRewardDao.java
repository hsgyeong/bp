package com.bp.jaringochi.domain.gulbi.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bp.jaringochi.domain.gulbi.dto.GulbiDrawContext;
import com.bp.jaringochi.domain.gulbi.dto.GulbiReward;

@Mapper
public interface GulbiRewardDao {

    // 뽑기 자격 판정용: 예산/지출/종료일/보상상태 + 유저 현재 옷
    GulbiDrawContext selectDrawContext(
        @Param("userId") Long userId,
        @Param("weeklyBudgetId") Long weeklyBudgetId
    );

    // 뽑기 결과 저장(PENDING) — 이게 있어야 decide가 동작
    int updateRewardDraw(
        @Param("userId") Long userId,
        @Param("weeklyBudgetId") Long weeklyBudgetId,
        @Param("outfitKey") String outfitKey,
        @Param("imagesJson") String imagesJson
    );

    GulbiReward selectRewardForUpdate(
        @Param("userId") Long userId,
        @Param("weeklyBudgetId") Long weeklyBudgetId
    );

    int updateUserCurrentOutfit(
        @Param("userId") Long userId,
        @Param("outfitKey") String outfitKey,
        @Param("imagesJson") String imagesJson
    );

    int updateRewardDecision(
        @Param("userId") Long userId,
        @Param("weeklyBudgetId") Long weeklyBudgetId,
        @Param("rewardStatus") String rewardStatus
    );
    
    // 읽기 전용(이어보기). selectRewardForUpdate 는 FOR UPDATE 라 쓰기 경로 전용.
    GulbiReward selectReward(
        @Param("userId") Long userId,
        @Param("weeklyBudgetId") Long weeklyBudgetId
    );
}