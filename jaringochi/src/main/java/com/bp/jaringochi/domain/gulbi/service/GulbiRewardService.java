package com.bp.jaringochi.domain.gulbi.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bp.jaringochi.domain.gulbi.dao.GulbiRewardDao;
import com.bp.jaringochi.domain.gulbi.dto.GulbiDrawRequest;
import com.bp.jaringochi.domain.gulbi.dto.GulbiDrawResponse;
import com.bp.jaringochi.domain.gulbi.dto.GulbiReward;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GulbiRewardService {
	
	private static final List<Outfit> OUTFITS = List.of(
		new Outfit("hanbok", "cute traditional Korean hanbok"),
		new Outfit("hoodie", "cozy oversized hoodie"),
		new Outfit("pajama", "soft striped pajamas"),
		new Outfit("school", "neat school uniform"),
		new Outfit("santa", "tiny Santa outfit"),
		new Outfit("raincoat", "yellow raincoat")
	);
	
	private final GmsImageClient gmsImageClient;
	private final GulbiRewardDao gulbiRewardDao;
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	public GulbiRewardService(GmsImageClient gmsImageClient, GulbiRewardDao gulbiRewardDao) {
		this.gmsImageClient = gmsImageClient;
		this.gulbiRewardDao = gulbiRewardDao;
	}
	
	@Transactional
	public GulbiDrawResponse draw(Long userId, Long weeklyBudgetId, GulbiDrawRequest request) {
		
		String currentOutfitKey = "default";
		Outfit outfit = pickOutfitExcept(currentOutfitKey);
		
		Map<String, String> resultImages = new LinkedHashMap<>();
		
		for (Map.Entry<String, GulbiDrawRequest.BaseImage> entry : request.baseImages().entrySet()) {
			String mood = entry.getKey();
			GulbiDrawRequest.BaseImage base = entry.getValue();
			
			String generatedDataUrl = gmsImageClient.dressGulbi(
				base.base64(),
				base.mimeType(),
				outfit.promptName(),
				mood
			);
			
			resultImages.put(mood, generatedDataUrl);
		}
		
		return new GulbiDrawResponse(
			weeklyBudgetId,
			outfit.key(),
			"PENDING",
			resultImages
		);
	}
	
	@Transactional
	public void decide(Long userId, Long weeklyBudgetId, String decision) {
		String normalized = decision == null ? "" : decision.trim().toUpperCase();
		
		if (!normalized.equals("ACCEPT") && !normalized.equals("DECLINE")) {
			throw new IllegalArgumentException("decision은 ACCEPT 또는 DECLINE이어야 합니다.");
		}
		
		GulbiReward reward = gulbiRewardDao.selectRewardForUpdate(userId, weeklyBudgetId);

	    if (reward == null) {
	        throw new IllegalArgumentException("보상 정보를 찾을 수 없습니다.");
	    }

	    if (!"PENDING".equals(reward.getRewardStatus())) {
	        throw new IllegalStateException("선택 대기 중인 보상이 아닙니다.");
	    }

	    if (reward.getRewardOutfitKey() == null || reward.getRewardImagesJson() == null) {
	        throw new IllegalStateException("뽑기 결과가 없습니다.");
	    }

	    if (normalized.equals("ACCEPT")) {
	        gulbiRewardDao.updateUserCurrentOutfit(
	            userId,
	            reward.getRewardOutfitKey(),
	            reward.getRewardImagesJson()
	        );

	        gulbiRewardDao.updateRewardDecision(
	            userId,
	            weeklyBudgetId,
	            "ACCEPTED"
	        );
	        return;
	    }

	    gulbiRewardDao.updateRewardDecision(
	        userId,
	        weeklyBudgetId,
	        "DECLINED"
	    );
	}
	
	private Outfit pickOutfitExcept(String currentOutfitKey) {
		List<Outfit> candidates = OUTFITS.stream()
				.filter(o -> !o.key().equals(currentOutfitKey))
				.toList();
		
		return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
	}
	
	private record Outfit(String key, String promptName) {}
}
