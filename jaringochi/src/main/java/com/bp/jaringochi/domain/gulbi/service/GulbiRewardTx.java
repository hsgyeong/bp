package com.bp.jaringochi.domain.gulbi.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bp.jaringochi.domain.gulbi.dao.GulbiRewardDao;
import com.bp.jaringochi.domain.gulbi.dto.GulbiReward;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;

@Service
public class GulbiRewardTx {
	private final GulbiRewardDao gulbiRewardDao;
	
	public GulbiRewardTx(GulbiRewardDao gulbiRewardDao) {
		this.gulbiRewardDao = gulbiRewardDao;
	}
	
	// 뽑기 결과 저장 (짧은 트랜잭션) 저장 직전 락 잡고 자격 재확인
	@Transactional
	public void persistPending(Long userId, Long weeklyBudgetId, String outfutKey, String imagesJson) {
		GulbiReward reward = gulbiRewardDao.selectRewardForUpdate(userId, weeklyBudgetId);
		if (reward == null) {
			throw new BusinessException(ErrorCode.BUDGET_NOT_FOUND);
		}
		if ("ACCEPTED".equals(reward.getRewardStatus()) || "DECLINED".equals(reward.getRewardStatus())) {
			throw new BusinessException(ErrorCode.REWARD_ALREADY_DECIDED);
		}
		gulbiRewardDao.updateRewardDraw(userId, weeklyBudgetId, outfutKey, imagesJson);
	}
}
