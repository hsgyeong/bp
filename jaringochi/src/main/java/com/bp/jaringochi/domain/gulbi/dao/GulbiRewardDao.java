package com.bp.jaringochi.domain.gulbi.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bp.jaringochi.domain.gulbi.dto.GulbiReward;

@Mapper
public interface GulbiRewardDao {

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
}