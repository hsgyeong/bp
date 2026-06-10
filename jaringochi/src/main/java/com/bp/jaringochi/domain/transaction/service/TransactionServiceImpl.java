package com.bp.jaringochi.domain.transaction.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bp.jaringochi.domain.transaction.dao.TransactionDao;
import com.bp.jaringochi.domain.transaction.dto.Transaction;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;

@Service
public class TransactionServiceImpl implements TransactionService {
	
	@Autowired
	private TransactionDao transactionDao;
	

	@Override
	public List<Transaction> getTransactions(Long userId, 
											 LocalDate startDate, 
											 LocalDate endDate, 
											 Integer type,
											 Long categoryId) {		
		return transactionDao.selectTransactions(userId, startDate, endDate, type, endDate);
	}

	@Override
	public Transaction getTransaction(Long id, Long userId) {
		return transactionDao.selectTransactionById(id, userId);
	}

	@Override
	@Transactional
	public Transaction addTransaction(Long userId, Transaction transaction) {
		validateInput(transaction);
		transaction.setUserId(userId);
		transactionDao.insertTransaction(transaction);
		
		Transaction created = transactionDao.selectTransactionById(transaction.getId(), userId);
		return created;
	}

	@Override
	@Transactional
	public Transaction updateTransaction(Long id, Long userId, Transaction transaction) {
		findOwned(id, userId);
		validateInput(transaction);
		
		transaction.setId(id);
		transaction.setUserId(userId);
		
		transactionDao.updateTransaction(transaction);
		Transaction updated = transactionDao.selectTransactionById(id, userId);
		
		return updated;
	}

	@Override
	public void deleteTransaction(Long id, Long userId) {
		Transaction existing = findOwned(id, userId);
		transactionDao.deleteTransaction(id, userId);
	}
	
	// 존재, 소유권 확인 (해당 id의 거래내역이 실제 존재하는지, 그 거래 내역이 현재 로그인한 사용자의 것인지 확인)
	private Transaction findOwned(Long id, Long userId) {
		Transaction transaction = transactionDao.selectTransactionById(id, userId);
		
		if (transaction == null) {
			throw new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND);
		}
		return transaction;
	}
	
	// 입력값 검증
	private void validateInput(Transaction transaction) {
		if (transaction.getCategoryId() == null
		 || transaction.getAmount() == null
		 || transaction.getAmount().signum() <= 0
		 || transaction.getDate() == null
		 || (transaction.getType() != 1 && transaction.getType() != 2)) {
			 throw new BusinessException(ErrorCode.TRANSACTION_INVALID_INPUT);
		 }
			
	}
	
	 // 변수명
	 // transaction   요청으로 들어온 거래내역 데이터
	 // created       DB에 생성된 후 다시 조회한 거래내역 데이터
	 // updated       수정 후 다시 조회한 거래내역 데이터
	 // existing      수정/삭제 전부터 이미 존재하던 거래내역 데이터

}
