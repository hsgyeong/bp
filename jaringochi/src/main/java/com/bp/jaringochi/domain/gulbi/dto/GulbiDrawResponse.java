package com.bp.jaringochi.domain.gulbi.dto;

import java.util.Map;

public record GulbiDrawResponse(
	Long weeklyBudgetId,
	String outfitKey,
	String rewardStatus,
	Map<String, String> images
){}
