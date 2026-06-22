package com.bp.jaringochi.domain.gulbi.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GulbiReward {
	private Long weeklyBudgetId;
	private Long userId;
	private String rewardOutfitKey;
	private String rewardStatus;
	private String rewardImagesJson;
}
