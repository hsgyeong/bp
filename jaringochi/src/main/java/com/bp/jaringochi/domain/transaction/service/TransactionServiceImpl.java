package com.bp.jaringochi.domain.transaction.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bp.jaringochi.domain.category.dao.CategoryDao;
import com.bp.jaringochi.domain.category.dto.Category;
import com.bp.jaringochi.domain.transaction.dao.TransactionDao;
import com.bp.jaringochi.domain.transaction.dto.Transaction;
import com.bp.jaringochi.domain.user.dao.UserDao;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

import com.bp.jaringochi.domain.notification.service.NotificationService;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
	
	private final TransactionDao transactionDao;
	private final CategoryDao categoryDao;
	private final NotificationService notificationService;
	private final UserDao userDao;
	private static final Set<String> ALLOWED_SORTS = Set.of(
	        "date_desc",
	        "date_asc",
	        "amount_desc",
	        "amount_asc"
	);
	

	 // 변수명
	 // transaction   요청으로 들어온 거래내역 데이터
	 // created       DB에 생성된 후 다시 조회한 거래내역 데이터
	 // updated       수정 후 다시 조회한 거래내역 데이터
	 // existing      수정/삭제 전부터 이미 존재하던 거래내역 데이터

	@Override
	public List<Transaction> getTransactions(Long userId, 
											 LocalDate startDate, 
											 LocalDate endDate, 
											 Integer type,
											 Long categoryId,
											 String keyword,
	                                         String sort) {	
		validateUser(userId);
		if (type != null && type != 1 && type != 2) {
		    throw new BusinessException(ErrorCode.TRANSACTION_INVALID_INPUT);
		}
		
		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
		    throw new BusinessException(ErrorCode.TRANSACTION_INVALID_INPUT);
		}
		
		// 빈 문자열이면 검색 조건을 적용하지 않도록 null로 바꾼다.
		String normalizedKeyword = null;
	    if (keyword != null && !keyword.trim().isEmpty()) {
	        normalizedKeyword = keyword.trim();
	    }
	    
	    // sort가 비어 있으면 기본 정렬을 사용
	    String normalizedSort = (sort == null || sort.isBlank()) ? "date_desc" : sort;
	    
	    // 허용하지 않은 정렬값은 SQL로 넘기지 않는다.
	    // MyBatis에서 ORDER BY를 안전하게 분기하기 위한 방어 코드
	    if (!ALLOWED_SORTS.contains(normalizedSort)) {
	        throw new BusinessException(ErrorCode.TRANSACTION_INVALID_INPUT);
	    }
	    
		return transactionDao.selectTransactions(userId, startDate, endDate, type, categoryId, normalizedKeyword, normalizedSort);
	}

	@Override
	public Transaction getTransaction(Long userId, Long id) {
		validateUser(userId);
		return findOwned(userId, id);
	}

	@Override
	@Transactional
	public Transaction addTransaction(Long userId, Transaction transaction) {
		validateUser(userId);
	    validateInput(transaction);
	    validateCategory(userId, transaction);

	    transaction.setUserId(userId);
	    transactionDao.insertTransaction(transaction);

	    // 지출이면 임계치 평가 -> 필요 시 알림 생성 (DEC-0011)
	    if (transaction.getType() == 2) {
	        notificationService.evaluateExpense(userId, transaction.getDate());
	    }

	    Transaction created = transactionDao.selectTransactionById(userId, transaction.getId());
	    return created;
	}

	@Override
	@Transactional
	public Transaction updateTransaction(Long userId, Long id, Transaction transaction) {
		validateUser(userId);
		findOwned(userId, id);
		validateInput(transaction);
		validateCategory(userId, transaction);
		
		transaction.setUserId(userId);
		transaction.setId(id);
		
		transactionDao.updateTransaction(transaction);
		Transaction updated = transactionDao.selectTransactionById(userId, id);
		
		return updated;
	}

	@Override
	@Transactional
	public void deleteTransaction(Long userId, Long id) {
		validateUser(userId);
		findOwned(userId, id);
		transactionDao.deleteTransaction(userId, id);
	}
	
	
	// 존재, 소유권 확인 (해당 id의 거래내역이 실제 존재하는지, 그 거래 내역이 현재 로그인한 사용자의 것인지 확인)
	private Transaction findOwned(Long userId, Long id) {
		Transaction transaction = transactionDao.selectTransactionById(userId, id);
		
		if (transaction == null) {
			throw new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND);
		}
		return transaction;
	}
	
	// 입력값 검증
	private void validateInput(Transaction transaction) {
		if (transaction == null
		 || transaction.getCategoryId() == null
		 || transaction.getAmount() == null
		 || transaction.getAmount().signum() <= 0
		 || transaction.getDate() == null
		 || (transaction.getType() != 1 && transaction.getType() != 2)) {
			 throw new BusinessException(ErrorCode.TRANSACTION_INVALID_INPUT);
		 }
			
	}
	
	// 카테고리 유효성 검증
	private void validateCategory(Long userId, Transaction transaction) {
		Category category = categoryDao.selectCategoryById(transaction.getCategoryId());
		
		// 카테고리가 없거나 숨김 처리된 카테고리면 사용할 수 없음
		if (category == null || Boolean.FALSE.equals(category.getIsActive())) {
			throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
		}
		
		// 기본 카테고리는 userId가 null
		// 사용자 커스텀 카테고리는 userId가 현재 로그인 사용자와 같아야 함
		if (category.getUserId() != null && !category.getUserId().equals(userId)) {
			throw new BusinessException(ErrorCode.CATEGORY_FORBIDDEN);
		}
		
		// 카테고리 type과 거래 type이 반드시 같아야 함
		if (category.getType() == null || category.getType() != transaction.getType()) {
			throw new BusinessException(ErrorCode.TRANSACTION_INVALID_INPUT);
		}
	}
	
	// 검증 메서드 추가
	private void validateUser(Long userId) {
	    if (userId == null || userDao.findById(userId) == null) {
	        throw new BusinessException(ErrorCode.USER_UNAUTHORIZED);
	    }
	}

}
