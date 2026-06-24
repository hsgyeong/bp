package com.bp.jaringochi.domain.gulbi.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

	// 뽑기: 자격조회 → 이미지 생성(트랜잭션 밖) → 저장(짧은 트랜잭션). 메서드 자체엔 @Transactional 없음. 
	public GulbiDrawResponse draw(Long userId, Long weeklyBudgetId, GulbiDrawRequest request) {

		// 자격 (단순 조회, 트랜잭션 불필요)
		GulbiDrawContext ctx = gulbiRewardDao.selectDrawContext(userId, weeklyBudgetId);
		validateEligible(ctx);

		// 3) Gemini 이미지 생성 (느린 외부호출 — 트랜잭션/DB커넥션 밖)
		GenerateResult result = generate(request);
		
		// 4) PENDING 저장 (여기만 트랜잭션)
		String imagesJson = writeJson(result.images());
		gulbiRewardTx.persistPending(userId, weeklyBudgetId, result.outfitName(), imagesJson);

		return new GulbiDrawResponse(weeklyBudgetId, result.outfitName(), "PENDING", result.images());
	}

	/** I-2: PENDING 보상 이어보기 (이미지 재생성 없이 저장된 결과 반환). 없으면 null. */
	public GulbiDrawResponse getPending(Long userId, Long weeklyBudgetId) {
		GulbiReward r = gulbiRewardDao.selectReward(userId, weeklyBudgetId);
		// PENDING이 아니거나 저장된 이미지가 없으면 보여줄 게 없음 → null
		if (r == null || !"PENDING".equals(r.getRewardStatus()) || r.getRewardImagesJson() == null) {
			return null;
		}
		// 저장돼 있던 JSON을 다시 맵으로 풀어 응답 구성
		return new GulbiDrawResponse(
			weeklyBudgetId, r.getRewardOutfitKey(), "PENDING", readJson(r.getRewardImagesJson())
		);
	}

	/** 받기/거절 — 컨트롤러가 직접 호출하므로 @Transactional 정상 동작. */
	@Transactional
	public void decide(Long userId, Long weeklyBudgetId, String decision) {
		// 입력값 정규화 후 ACCEPT/DECLINE만 허용. 그 외엔 400
		String normalized = decision == null ? "" : decision.trim().toUpperCase();
		if (!normalized.equals("ACCEPT") && !normalized.equals("DECLINE")) {
			throw new BusinessException(ErrorCode.REWARD_INVALID_DECISION);
		}

		// 동시 처리 충돌 방지를 위해 FOR UPDATE 락을 걸고 보상 행을 조회
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
			// 받기: 유저의 '현재 착용 옷'과 무드별 이미지를 이번 결과로 갱신 → 홈 굴비에 반영
			gulbiRewardDao.updateUserCurrentOutfit(
				userId, reward.getRewardOutfitKey(), reward.getRewardImagesJson());
			gulbiRewardDao.updateRewardDecision(userId, weeklyBudgetId, "ACCEPTED");
		} else {
			// 거절: 외형은 그대로 두고 보상만 DECLINED로 잠금
			gulbiRewardDao.updateRewardDecision(userId, weeklyBudgetId, "DECLINED");
		}
	}

	// ===== helpers =====

	// 뽑기 자격 검사: 끝난 주 + 지출≤예산 + 아직 미결정이어야 통과
	private void validateEligible(GulbiDrawContext ctx) {
		if (ctx == null) {
			throw new BusinessException(ErrorCode.BUDGET_NOT_FOUND);
		}
		boolean weekEnded = ctx.getEndDate().isBefore(LocalDate.now());					// 주가 끝났는가
		boolean savedUnderBudget = ctx.getSpentMoney().compareTo(ctx.getAmount()) <= 0;	// 절약 성공인가
		if (!weekEnded || !savedUnderBudget) {
			throw new BusinessException(ErrorCode.REWARD_NOT_ELIGIBLE);
		}
		// 이미 받기/거절한 주는 다시 뽑을 수 없음
		if ("ACCEPTED".equals(ctx.getRewardStatus()) || "DECLINED".equals(ctx.getRewardStatus())) {
			throw new BusinessException(ErrorCode.REWARD_ALREADY_DECIDED);
		}
	}

	// images 맵 → JSON 문자열(저장용). 실패 시 500
	private String writeJson(Map<String, String> images) {
		try {
			return objectMapper.writeValueAsString(images);
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	// JSON 문자열 → images 맵(조회용). 순서 보존 위해 LinkedHashMap으로 역직렬화. 실패 시 500
	private Map<String, String> readJson(String json) {
		try {
			return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {});
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}		
	
	private record GenerateResult(Map<String, String> images, String outfitName) {}
	
	
	private GenerateResult generate(GulbiDrawRequest request) {
		// 무드 → 결과 이미지(data URL) 맵. 입력 순서 보존을 위해 LinkedHashMap.
		Map<String, String> images = new LinkedHashMap<>();
		// 프론트가 보낸 무드별 원본 이미지들을 리스트로(인덱스 접근 위해)
		var entries = new ArrayList<>(request.baseImages().entrySet());
		// 보낸 무드가 하나도 없으면 빈 결과 + 기본 옷 이름 반환
		if (entries.isEmpty()) {
			return new GenerateResult(images, "랜덤 옷");
		}
		
		// 앵커 : 먼저 순차로 생성 (나머지가 이 이미지를 참조함)
		Map.Entry<String, GulbiDrawRequest.BaseImage> anchor = entries.get(0);
		GulbiDrawRequest.BaseImage anchorBase = anchor.getValue();
		GmsImageClient.DressResult a;
		
		try {
			// 앵커는 다른 무드의 기준이 되기 때문에 반드시 먼저 순차로 생성
			a = gmsImageClient.dressGulbi(
					anchorBase.base64(), anchorBase.mimeType(), anchor.getKey(), null);
		} catch (Exception ex) {
			throw new BusinessException(ErrorCode.REWARD_IMAGE_GENERATION_FAILED, ex);
		}
		// 앵커 결과 이미지를 맵에 저장
		images.put(anchor.getKey(), a.dataUrl());
		
		String outfitName = (a.outfitName() == null || a.outfitName().isBlank()) ? "랜덤 옷" : a.outfitName().trim();
		// DB 컬럼 길이 보호 (50자를 넘으면 잘라냄)
		if (outfitName.length() > 50) {
			outfitName = outfitName.substring(0, 50);
		}
		
		final String referenceB64 = a.dataUrl().substring(a.dataUrl().indexOf(',') + 1);
		
		// 나머지 무드 : 서로 독립으로 동시에 호출해 시간 단축. 동시에 3개까지
		ExecutorService pool = Executors.newFixedThreadPool(3);
		try {
			List<String> moodKeys = new ArrayList<>();	// 순서 보존용
			List<Future<String>> futures = new ArrayList<>();
			
			// i=1부터(앵커 제외) 무드별 작업을 만들어 풀에 제출 → 백그라운드에서 동시에 실행 시작.
			for (int i = 1; i < entries.size(); i++) {
				Map.Entry<String, GulbiDrawRequest.BaseImage> e = entries.get(i);
				final String mood = e.getKey();			// 익명 클래스에서 쓰려면 final
				final GulbiDrawRequest.BaseImage base = e.getValue();
				
				moodKeys.add(mood);
				
				Callable<String> task = new Callable<String>() {

					@Override
					public String call() throws Exception {
						return gmsImageClient.dressGulbi(
								base.base64(), base.mimeType(), mood, referenceB64).dataUrl();
					}
					
				};
				futures.add(pool.submit(task));
			}
			// 입력 순서대로 결과 모으기 (get은 그 작업이 끝날 때까지 기다림)
			for (int i = 0; i < futures.size(); i++) {
				try {
					images.put(moodKeys.get(i), futures.get(i).get());
				} catch (Exception ex) {
					throw new BusinessException(ErrorCode.REWARD_IMAGE_GENERATION_FAILED, ex);
				}
			}
		} finally {
			pool.shutdown(); // 스레드 정리
		}		
		return new GenerateResult(images, outfitName);
	}

}