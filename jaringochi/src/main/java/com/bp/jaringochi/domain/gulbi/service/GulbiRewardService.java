package com.bp.jaringochi.domain.gulbi.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bp.jaringochi.domain.gulbi.dao.GulbiRewardDao;
import com.bp.jaringochi.domain.gulbi.dto.GulbiDrawContext;
import com.bp.jaringochi.domain.gulbi.dto.GulbiDrawRequest;
import com.bp.jaringochi.domain.gulbi.dto.GulbiDrawResponse;
import com.bp.jaringochi.domain.gulbi.dto.GulbiReward;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GulbiRewardService {

	private static final List<Outfit> OUTFITS = List.of(
		new Outfit("hanbok",   "cute traditional Korean hanbok"),
		new Outfit("hoodie",   "cozy oversized hoodie"),
		new Outfit("pajama",   "soft striped pajamas"),
		new Outfit("school",   "neat school uniform"),
		new Outfit("santa",    "tiny Santa outfit"),
		new Outfit("raincoat", "yellow raincoat")
	);

	private final GmsImageClient gmsImageClient;
	private final GulbiRewardDao gulbiRewardDao;
	private final GulbiRewardTx gulbiRewardTx;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public GulbiRewardService(GmsImageClient gmsImageClient,
	                          GulbiRewardDao gulbiRewardDao,
	                          GulbiRewardTx gulbiRewardTx) {
		this.gmsImageClient = gmsImageClient;
		this.gulbiRewardDao = gulbiRewardDao;
		this.gulbiRewardTx = gulbiRewardTx;
	}

	/** 뽑기: 자격조회 → 이미지 생성(트랜잭션 밖) → 저장(짧은 트랜잭션). 메서드 자체엔 @Transactional 없음. */
	public GulbiDrawResponse draw(Long userId, Long weeklyBudgetId, GulbiDrawRequest request) {

		// 1) 자격 (단순 조회, 트랜잭션 불필요)
		GulbiDrawContext ctx = gulbiRewardDao.selectDrawContext(userId, weeklyBudgetId);
		validateEligible(ctx);

		// 2) 현재 옷 제외하고 랜덤 선택
		String currentOutfitKey = ctx.getCurrentOutfitKey() == null ? "default" : ctx.getCurrentOutfitKey();
		Outfit outfit = pickOutfitExcept(currentOutfitKey);

		// 3) Gemini 이미지 생성 (느린 외부호출 — 트랜잭션/DB커넥션 밖)
		Map<String, String> images = generate(request, outfit);

		// 4) PENDING 저장 (여기만 트랜잭션)
		String imagesJson = writeJson(images);
		gulbiRewardTx.persistPending(userId, weeklyBudgetId, outfit.key(), imagesJson);

		return new GulbiDrawResponse(weeklyBudgetId, outfit.key(), "PENDING", images);
	}

	/** I-2: PENDING 보상 이어보기 (이미지 재생성 없이 저장된 결과 반환). 없으면 null. */
	public GulbiDrawResponse getPending(Long userId, Long weeklyBudgetId) {
		GulbiReward r = gulbiRewardDao.selectReward(userId, weeklyBudgetId);
		if (r == null || !"PENDING".equals(r.getRewardStatus()) || r.getRewardImagesJson() == null) {
			return null;
		}
		return new GulbiDrawResponse(
			weeklyBudgetId, r.getRewardOutfitKey(), "PENDING", readJson(r.getRewardImagesJson())
		);
	}

	/** 받기/거절 — 컨트롤러가 직접 호출하므로 @Transactional 정상 동작. */
	@Transactional
	public void decide(Long userId, Long weeklyBudgetId, String decision) {
		String normalized = decision == null ? "" : decision.trim().toUpperCase();
		if (!normalized.equals("ACCEPT") && !normalized.equals("DECLINE")) {
			throw new BusinessException(ErrorCode.REWARD_INVALID_DECISION);
		}

		GulbiReward reward = gulbiRewardDao.selectRewardForUpdate(userId, weeklyBudgetId);
		if (reward == null) {
			throw new BusinessException(ErrorCode.REWARD_NOT_FOUND);
		}
		if (!"PENDING".equals(reward.getRewardStatus())) {
			throw new BusinessException(ErrorCode.REWARD_ALREADY_DECIDED);
		}
		if (reward.getRewardOutfitKey() == null || reward.getRewardImagesJson() == null) {
			throw new BusinessException(ErrorCode.REWARD_NO_DRAW);
		}

		if (normalized.equals("ACCEPT")) {
			gulbiRewardDao.updateUserCurrentOutfit(
				userId, reward.getRewardOutfitKey(), reward.getRewardImagesJson());
			gulbiRewardDao.updateRewardDecision(userId, weeklyBudgetId, "ACCEPTED");
		} else {
			gulbiRewardDao.updateRewardDecision(userId, weeklyBudgetId, "DECLINED");
		}
	}

	// ===== helpers =====

	private void validateEligible(GulbiDrawContext ctx) {
		if (ctx == null) {
			throw new BusinessException(ErrorCode.BUDGET_NOT_FOUND);
		}
		boolean weekEnded = ctx.getEndDate().isBefore(LocalDate.now());
		boolean savedUnderBudget = ctx.getSpentMoney().compareTo(ctx.getAmount()) <= 0;
		if (!weekEnded || !savedUnderBudget) {
			throw new BusinessException(ErrorCode.REWARD_NOT_ELIGIBLE);
		}
		if ("ACCEPTED".equals(ctx.getRewardStatus()) || "DECLINED".equals(ctx.getRewardStatus())) {
			throw new BusinessException(ErrorCode.REWARD_ALREADY_DECIDED);
		}
	}

	private Map<String, String> generate(GulbiDrawRequest request, Outfit outfit) {
		Map<String, String> images = new LinkedHashMap<>();
		var entries = new ArrayList<>(request.baseImages().entrySet());
		if (entries.isEmpty()) return images;
		
		var anchor = entries.get(0);
		GulbiDrawRequest.BaseImage anchorBase = anchor.getValue();
		String anchorDataUrl = gmsImageClient.dressGulbi(
				anchorBase.base64(), anchorBase.mimeType(), outfit.promptName(), anchor.getKey(), null);
		images.put(anchor.getKey(), anchorDataUrl);
		
		String referenceB64 = anchorDataUrl.substring(anchorDataUrl.indexOf(',') + 1);
		
		for (int i = 1; i < entries.size(); i++) {
			var e = entries.get(i);
			GulbiDrawRequest.BaseImage base = e.getValue();
			images.put(e.getKey(), gmsImageClient.dressGulbi(
					base.base64(), base.mimeType(), outfit.promptName(), e.getKey(), referenceB64));
		
		}
		return images;
	}

	private Outfit pickOutfitExcept(String currentOutfitKey) {
		List<Outfit> candidates = OUTFITS.stream()
			.filter(o -> !o.key().equals(currentOutfitKey)).toList();
		return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
	}

	private String writeJson(Map<String, String> images) {
		try {
			return objectMapper.writeValueAsString(images);
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	private Map<String, String> readJson(String json) {
		try {
			return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {});
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}		

	private record Outfit(String key, String promptName) {}
}